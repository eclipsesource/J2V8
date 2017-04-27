/*******************************************************************************
 * Copyright (c) 2016 Brandon Sanders
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brandon Sanders - initial API and implementation and/or initial documentation
 ******************************************************************************/
package com.eclipsesource.v8;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class V8JavaAdapterTest {
//Setup classes////////////////////////////////////////////////////////////////
    private interface Baz {
        Foo doFooInterface(Foo foo);
    }

    private interface Bar {
        int doInterface(int args);
    }

    private static final class Foo {
        public int i;
        public Foo(int i) { this.i = i; }
        public static int doStatic() { return 9001; }
        public int doInstance(int i) { return this.i + i; }
        public int doInstance(int i, int i2) { return this.i + (i * i2); }
        public void add(Foo foo) { this.i += foo.i; }
        public void add(Bar bar) { this.i = bar.doInterface(this.i); }
        public void addBaz(Baz baz) { this.i = baz.doFooInterface(this).getI(); }
        public String doString(String s) { return s; }
        public int getI() { return i; }
        public Foo copy() { return new Foo(i); }
        public int doArray(int[] a) {
            int ret = 0;
            for (int i = 0; i < a.length; i++) {
                ret += a[i];
            }
            return ret;
        }
        public int doNArray(int[][] a) {
            int ret = 0;
            for (int i = 0; i < a.length; i++) {
                ret += doArray(a[i]);
            }
            return ret;
        }
        public int doVarargs(int a, int b, int... c) {
            int ret = a + b;
            for (int i = 0; i < c.length; i++) {
                ret += c[i];
            }
            return ret;
        }
    }

    private static final class InterceptableFoo {
        public int i;
        public InterceptableFoo(int i) { this.i = i; }
        public void add(int i) { this.i += i; }
        public int getI() { return i; }
        public void setI(int i) { this.i = i; }
    }

    private static final class FooInterceptor implements V8JavaClassInterceptor<InterceptableFoo> {

        @Override public String getConstructorScriptBody() {
            return "var i = 0;\n" +
                    "this.getI = function() { return i; };\n" +
                    "this.setI = function(other) { i = other; };\n" +
                    "this.add = function(other) { i = i + other; };\n" +
                    "this.onJ2V8Inject = function(context) { i = context.get(\"i\"); };\n" +
                    "this.onJ2V8Extract = function(context) { context.set(\"i\", i); };";
        }

        @Override public void onInject(V8JavaClassInterceptorContext context, InterceptableFoo object) {
            context.set("i", object.i);
        }

        @Override public void onExtract(V8JavaClassInterceptorContext context, InterceptableFoo object) {
            object.i = V8JavaObjectUtils.widenNumber(context.get("i"), Integer.class);
        }
    }

    private static final class Fooey {
        public int i = 0;
        public Fooey(int i) { this.i = i; }
        public void doInstance(InterceptableFoo foo) { this.i += foo.getI(); }
        public void setI(int i) { this.i = i; }
        public int getI() { return i; }
    }

//Tests////////////////////////////////////////////////////////////////////////

    private V8 v8;

    @Before
    public void setup() {
        v8 = V8.createV8Runtime();
        V8JavaAdapter.injectClass(Foo.class, v8);
    }

    @After
    public void teardown() {
        V8JavaObjectUtils.releaseV8Resources(v8);
        V8JavaCache.removeGarbageCollectedJavaObjects();
        v8.release(true);
    }

    @Test
    public void shouldInjectObjects() {
        V8JavaAdapter.injectObject("bar", new Bar() {
            @Override public int doInterface(int args) {
                return args * 2;
            }
        }, v8);
        Assert.assertEquals(10, v8.executeIntegerScript("bar.doInterface(5);"));
        V8JavaAdapter.injectObject("bar", new Bar() {
            @Override public int doInterface(int args) {
                return args * 4;
            }
        }, v8);
        Assert.assertEquals(20, v8.executeIntegerScript("bar.doInterface(5);"));
    }

    @Test
    public void shouldInjectClasses() {
        int i = new Random().nextInt(1000);
        Assert.assertEquals(i, v8.executeIntegerScript(String.format("var x = new Foo(%d).$release(); x.getI();", i)));
    }

    @Test
    public void shouldHandleStaticInvocations() {
        Assert.assertEquals(9001, v8.executeIntegerScript("Foo.doStatic();"));
    }

    @Test
    public void shouldHandleInstanceInvocations() {
        Assert.assertEquals(3344, v8.executeIntegerScript("var x = new Foo(3300).$release(); x.doInstance(44);"));
        Assert.assertEquals(9000, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.doInstance(3000, 2);"));
    }

    @Test
    public void shouldHandleComplexArguments() {
        Assert.assertEquals(3344, v8.executeIntegerScript("var x = new Foo(3300).$release(); x.add(new Foo(44).$release()); x.getI();"));
    }

    @Test
    public void shouldHandleFunctionalArguments() {
        Assert.assertEquals(1500, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.add(function(i) { return i / 2; }); x.getI();"));
        Assert.assertEquals(1500, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.addBaz(function(foo) { return new Foo(foo.getI() / 2).$release(); }); x.getI();"));
    }

    @Test
    public void shouldHandleComplexReturnTypes() {
        int i = new Random().nextInt(1000);
        Assert.assertEquals(i, v8.executeIntegerScript(String.format("var x = new Foo(%d).$release(); x.copy().getI();", i)));
    }

    @Test
    public void shouldHandleStringArguments() {
        Assert.assertEquals("aStringArgument", v8.executeStringScript("var x = new Foo(9001).$release(); x.doString(\"aStringArgument\");"));
    }

    @Test
    public void shouldHandleObjectArrays() {
        V8JavaAdapter.injectObject("objectArray", new String[] {"Hello", "World"}, v8);
        Assert.assertEquals("Hello", v8.executeStringScript("objectArray.get(0)"));
        Assert.assertEquals("World", v8.executeStringScript("objectArray.get(1)"));
    }

    @Test
    public void shouldInterceptClasses() {
        V8JavaAdapter.injectClass(InterceptableFoo.class, new FooInterceptor(), v8);
        Assert.assertEquals(3344, v8.executeIntegerScript("var x = new InterceptableFoo(0).$release(); x.add(3344); x.getI();"));
        V8JavaAdapter.injectObject("bazz", new Fooey(3000), v8);
        Assert.assertEquals(9001, v8.executeIntegerScript("x.setI(6001); bazz.doInstance(x); bazz.getI();"));
        InterceptableFoo foo = new InterceptableFoo(4444);
        V8JavaAdapter.injectObject("foobar", foo, v8);
        Assert.assertEquals(8888, v8.executeIntegerScript("foobar.add(4444); foobar.getI();"));
        Assert.assertEquals(8888, v8.executeIntegerScript("bazz.setI(0); bazz.doInstance(foobar); bazz.getI();"));
        Assert.assertEquals(8888, foo.getI());
    }

    @Test
    public void shouldHandleVarargs() {
        Assert.assertEquals(30, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.doVarargs(10, 20);"));
        Assert.assertEquals(60, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.doVarargs(10, 20, 30);"));
        Assert.assertEquals(100, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.doVarargs(10, 20, 30, 40);"));
    }

    @Test
    public void shouldHandleArrays() {
        Assert.assertEquals(30, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.doArray([10, 15, 5]);"));
        Assert.assertEquals(70, v8.executeIntegerScript("var x = new Foo(3000).$release(); x.doNArray([[10, 15], [20, 25]]);"));
    }
}