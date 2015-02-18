/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.awt.Rectangle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class V8CallbackTest {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released.");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    public interface ICallback {

        public void voidMethodNoParameters();

        public void voidMethodWithParameters(final int a, final double b, final boolean c, final String d, V8Object e);

        public void voidMethodWithObjectParameters(final Integer a);

        public void voidMethodWithArrayParameter(final V8Array array);

        public void voidMethodWithObjectParameter(final V8Object object);

        public void voidMethodWithStringParameter(final String string);

        public int intMethodNoParameters();

        public Integer integerMethod();

        public int intMethodWithParameters(final int x, final int b);

        public int intMethodWithArrayParameter(final V8Array array);

        public double doubleMethodNoParameters();

        public float floatMethodNoParameters();

        public double doubleMethodWithParameters(final double x, final double y);

        public boolean booleanMethodNoParameters();

        public boolean booleanMethodWithArrayParameter(final V8Array array);

        public String stringMethodNoParameters();

        public String stringMethodWithArrayParameter(final V8Array array);

        public V8Object v8ObjectMethodNoParameters();

        public V8Object v8ObjectMethodWithObjectParameter(final V8Object object);

        public V8Array v8ArrayMethodNoParameters();

        public V8Array v8ArrayMethodWithStringParameter(final String string);

        public Object objectMethodNoParameter();

        public void voidMethodVarArgs(final Object... args);

        public void voidMethodVarArgsAndOthers(int x, int y, final Object... args);

    }

    @Test
    public void testVoidMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testIntMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).intMethodNoParameters();
    }

    @Test
    public void testIntMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(7).when(callback).intMethodNoParameters();
        v8.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        int result = v8.executeIntScript("foo();");

        assertEquals(7, result);
    }

    @Test
    public void testIntegerMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(8).when(callback).integerMethod();
        v8.registerJavaMethod(callback, "integerMethod", "foo", new Class<?>[0]);

        int result = v8.executeIntScript("foo();");

        assertEquals(8, result);
    }

    @Test
    public void testDoubleMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).doubleMethodNoParameters();
    }

    @Test
    public void testDoubleMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(3.14159).when(callback).doubleMethodNoParameters();
        v8.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8.executeDoubleScript("foo();");

        assertEquals(3.14159, result, 0.0000001);
    }

    @Test
    public void testFloatMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(3.14159f).when(callback).floatMethodNoParameters();
        v8.registerJavaMethod(callback, "floatMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8.executeDoubleScript("foo();");

        assertEquals(3.14159f, result, 0.0000001);
    }

    @Test
    public void testBooleanMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).booleanMethodNoParameters();
    }

    @Test
    public void testBooleanMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(true).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("foo();");

        assertTrue(result);
    }

    @Test
    public void testStringMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).stringMethodNoParameters();
    }

    @Test
    public void testCallbackWithUndefinedInParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo(undefined);");

        verify(callback).voidMethodWithObjectParameter(new V8Object.Undefined());
    }

    @Test
    public void testCallbackWithNullInParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo(null);");

        verify(callback).voidMethodWithObjectParameter(null);
    }

    @Test
    public void testCallbackWithEmptyParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo();");

        verify(callback).voidMethodWithObjectParameter(new V8Object.Undefined());
    }

    @Test
    public void testStringMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn("bar").when(callback).stringMethodNoParameters();
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        String result = v8.executeStringScript("foo();");

        assertEquals("bar", result);
    }

    @Test
    public void testStringMethodCalledFromScriptWithNull() {
        ICallback callback = mock(ICallback.class);
        doReturn(null).when(callback).stringMethodNoParameters();
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("foo() === null");

        assertTrue(result);
    }

    @Test
    public void testV8ObjectMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).v8ObjectMethodNoParameters();
    }

    @Test
    public void testV8ObjectMethodReturnsUndefined() {
        ICallback callback = mock(ICallback.class);
        doReturn(V8.getUndefined()).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("typeof foo() === 'undefined'");

        assertTrue(result);
    }

    @Test
    public void testV8ObjectMethodReturnsNull() {
        ICallback callback = mock(ICallback.class);
        doReturn(null).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("foo() === null");

        assertTrue(result);
    }

    @Test
    public void testV8ObjectMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        V8Object object = new V8Object(v8);
        object.add("name", "john");
        doReturn(object).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        V8Object result = v8.executeObjectScript("foo();");

        assertEquals("john", result.getString("name"));
        result.release();
    }

    @Test
    public void testV8ObjectMethodReleasesResults() {
        ICallback callback = mock(ICallback.class);
        V8Object object = new V8Object(v8);
        doReturn(object).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        assertTrue(object.isReleased());
    }

    @Test
    public void testV8ArrayMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).v8ArrayMethodNoParameters();
    }

    @Test
    public void testV8ArrayMethodReturnsUndefined() {
        ICallback callback = mock(ICallback.class);
        doReturn(new V8Array.Undefined()).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("typeof foo() === 'undefined'");

        assertTrue(result);
    }

    @Test
    public void testV8ArrayMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        V8Array array = new V8Array(v8);
        array.push("john");
        doReturn(array).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        V8Array result = v8.executeArrayScript("foo();");

        assertEquals("john", result.getString(0));
        result.release();
    }

    @Test
    public void testV8ArrayMethodReleasesResults() {
        ICallback callback = mock(ICallback.class);
        V8Array object = new V8Array(v8);
        doReturn(object).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        assertTrue(object.isReleased());
    }

    @Test
    public void testVoidFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8Object.executeVoidFunction("foo", null);

        verify(callback).voidMethodNoParameters();
        v8Object.release();
    }

    @Test
    public void testVoidFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8Array.executeVoidFunction("foo", null);

        verify(callback).voidMethodNoParameters();
        v8Array.release();
    }

    @Test
    public void testIntFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(99).when(callback).intMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        int result = v8Object.executeIntFunction("foo", null);

        verify(callback).intMethodNoParameters();
        assertEquals(99, result);
        v8Object.release();
    }

    @Test
    public void testIntFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(99).when(callback).intMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        int result = v8Array.executeIntFunction("foo", null);

        verify(callback).intMethodNoParameters();
        assertEquals(99, result);
        v8Array.release();
    }

    @Test
    public void testDoubleFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(99.9).when(callback).doubleMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8Object.executeDoubleFunction("foo", null);

        verify(callback).doubleMethodNoParameters();
        assertEquals(99.9, result, 0.000001);
        v8Object.release();
    }

    @Test
    public void testDoubleFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(99.9).when(callback).doubleMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8Array.executeDoubleFunction("foo", null);

        verify(callback).doubleMethodNoParameters();
        assertEquals(99.9, result, 0.000001);
        v8Array.release();
    }

    @Test
    public void testBooleanFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(false).when(callback).booleanMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8Object.executeBooleanFunction("foo", null);

        verify(callback).booleanMethodNoParameters();
        assertFalse(result);
        v8Object.release();
    }

    @Test
    public void testBooleanFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(false).when(callback).booleanMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8Array.executeBooleanFunction("foo", null);

        verify(callback).booleanMethodNoParameters();
        assertFalse(result);
        v8Array.release();
    }

    @Test
    public void testStringFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn("mystring").when(callback).stringMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        String result = v8Object.executeStringFunction("foo", null);

        verify(callback).stringMethodNoParameters();
        assertEquals("mystring", result);
        v8Object.release();
    }

    @Test
    public void testStringFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn("mystring").when(callback).stringMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        String result = v8Array.executeStringFunction("foo", null);

        verify(callback).stringMethodNoParameters();
        assertEquals("mystring", result);
        v8Array.release();
    }

    @Test
    public void testV8ObjectFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeObjectScript("x = {first:'bob'}; x")).when(callback).v8ObjectMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        V8Object result = v8Object.executeObjectFunction("foo", null);

        verify(callback).v8ObjectMethodNoParameters();
        assertEquals("bob", result.getString("first"));
        v8Object.release();
        result.release();
    }

    @Test
    public void testV8ObjectFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeObjectScript("x = {first:'bob'}; x")).when(callback).v8ObjectMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        V8Object result = v8Array.executeObjectFunction("foo", null);

        verify(callback).v8ObjectMethodNoParameters();
        assertEquals("bob", result.getString("first"));
        v8Array.release();
        result.release();
    }

    @Test
    public void testV8ArrayFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeArrayScript("x = ['a','b','c']; x")).when(callback).v8ArrayMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        V8Array result = v8Object.executeArrayFunction("foo", null);

        verify(callback).v8ArrayMethodNoParameters();
        assertEquals(3, result.length());
        assertEquals("a", result.getString(0));
        assertEquals("b", result.getString(1));
        assertEquals("c", result.getString(2));
        v8Object.release();
        result.release();
    }

    @Test
    public void testV8ArrayFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeArrayScript("x = ['a','b','c']; x")).when(callback).v8ArrayMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        V8Array result = v8Array.executeArrayFunction("foo", null);

        verify(callback).v8ArrayMethodNoParameters();
        assertEquals(3, result.length());
        assertEquals("a", result.getString(0));
        assertEquals("b", result.getString(1));
        assertEquals("c", result.getString(2));
        v8Array.release();
        result.release();
    }

    @Test
    public void testVoidMethodCalledFromIntScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeIntScript("foo();1");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromDoubleScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeDoubleScript("foo();1.1");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromStringScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeStringScript("foo();'test'");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromArrayScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeArrayScript("foo();[]").release();

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromObjectScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeObjectScript("foo(); bar={}; bar;").release();

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Double.TYPE,
                Boolean.TYPE, String.class, V8Object.class });

        v8.executeVoidScript("foo(1,1.1, false, 'string', undefined);");

        verify(callback).voidMethodWithParameters(1, 1.1, false, "string", new V8Object.Undefined());
    }

    @Test
    public void testIntMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Integer>() {

            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                int x = (int) args[0];
                int y = (int) args[1];
                return x + y;
            }

        }).when(callback).intMethodWithParameters(anyInt(), anyInt());
        v8.registerJavaMethod(callback, "intMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Integer.TYPE });

        int result = v8.executeIntScript("foo(8,7);");

        verify(callback).intMethodWithParameters(8, 7);
        assertEquals(15, result);
    }

    @Test
    public void testDoubleMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Double>() {

            @Override
            public Double answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                double x = (double) args[0];
                double y = (double) args[1];
                return x + y;
            }

        }).when(callback).doubleMethodWithParameters(anyInt(), anyInt());
        v8.registerJavaMethod(callback, "doubleMethodWithParameters", "foo",
                new Class<?>[] { Double.TYPE, Double.TYPE });

        double result = v8.executeDoubleScript("foo(8.3,7.1);");

        verify(callback).doubleMethodWithParameters(8.3, 7.1);
        assertEquals(15.4, result, 0.000001);
    }

    @Test
    public void testVoidMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                assertEquals(1, args.length);
                assertEquals(1, ((V8Array) args[0]).getInteger(0));
                assertEquals(2, ((V8Array) args[0]).getInteger(1));
                assertEquals(3, ((V8Array) args[0]).getInteger(2));
                assertEquals(4, ((V8Array) args[0]).getInteger(3));
                assertEquals(5, ((V8Array) args[0]).getInteger(4));
                return null;
            }
        }).when(callback).voidMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "voidMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        v8.executeVoidScript("foo([1,2,3,4,5]);");
    }

    @Test
    public void testIntMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int arrayLength = ((V8Array) args[0]).length();
                int result = 0;
                for (int i = 0; i < arrayLength; i++) {
                    result += ((V8Array) args[0]).getInteger(i);
                }
                return result;
            }
        }).when(callback).intMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "intMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        int result = v8.executeIntScript("foo([1,2,3,4,5]);");

        assertEquals(15, result);
    }

    @Test
    public void testBooleanMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int arrayLength = ((V8Array) args[0]).length();
                int result = 0;
                for (int i = 0; i < arrayLength; i++) {
                    result += ((V8Array) args[0]).getInteger(i);
                }
                return result > 10;
            }
        }).when(callback).booleanMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "booleanMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        boolean result = v8.executeBooleanScript("foo([1,2,3,4,5]);");

        assertTrue(result);
    }

    @Test
    public void testStringMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int arrayLength = ((V8Array) args[0]).length();
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < arrayLength; i++) {
                    result.append(((V8Array) args[0]).getString(i));
                }
                return result.toString();
            }
        }).when(callback).stringMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "stringMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        String result = v8.executeStringScript("foo(['a', 'b', 'c', 'd', 'e']);");

        assertEquals("abcde", result);
    }

    @Test
    public void testArrayMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<V8Array>() {
            @Override
            public V8Array answer(final InvocationOnMock invocation) {
                V8Array result = new V8Array(v8);
                String arg = (String) invocation.getArguments()[0];
                String[] split = arg.split(" ");
                for (String string : split) {
                    result.push(string);
                }
                return result;
            }
        }).when(callback).v8ArrayMethodWithStringParameter(any(String.class));
        v8.registerJavaMethod(callback, "v8ArrayMethodWithStringParameter", "foo", new Class<?>[] { String.class });

        V8Array result = v8.executeArrayScript("foo('hello world how are you');");

        assertEquals(5, result.length());
        assertEquals("hello", result.getString(0));
        assertEquals("world", result.getString(1));
        assertEquals("how", result.getString(2));
        assertEquals("are", result.getString(3));
        assertEquals("you", result.getString(4));
        result.release();
    }

    @Test
    public void testVoidMethodCalledWithObjectParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                assertEquals(1, args.length);
                assertEquals("john", ((V8Object) args[0]).getString("first"));
                assertEquals("smith", ((V8Object) args[0]).getString("last"));
                assertEquals(7, ((V8Object) args[0]).getInteger("age"));
                return null;
            }
        }).when(callback).voidMethodWithObjectParameter(any(V8Object.class));
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo({first:'john', last:'smith', age:7});");

        verify(callback).voidMethodWithObjectParameter(any(V8Object.class));
    }

    @Test
    public void testObjectMethodCalledWithObjectParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                V8Object parameter = ((V8Object) args[0]);
                V8Object result = new V8Object(v8);
                result.add("first", parameter.getString("last"));
                result.add("last", parameter.getString("first"));
                return result;
            }
        }).when(callback).v8ObjectMethodWithObjectParameter(any(V8Object.class));
        v8.registerJavaMethod(callback, "v8ObjectMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        V8Object result = v8.executeObjectScript("foo({first:'john', last:'smith'});");

        assertEquals("smith", result.getString("first"));
        assertEquals("john", result.getString("last"));
        result.release();
    }

    @Test(expected = RuntimeException.class)
    public void testVoidMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test
    public void testCatchJavaExceptionInJS() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'My Runtime Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testCatchJavaExceptionInJSWithoutMessage() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException()).when(callback).voidMethodNoParameters();
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'Unhandled Java Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testNonVoidCallbackCatchJavaExceptionInJS() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'My Runtime Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testNonVoidCallbackCatchJavaExceptionInJSWithoutMessage() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException()).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'Unhandled Java Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testJSCatchWillCatchJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        doNothing().when(callback).voidMethodWithStringParameter(anyString());
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});
        v8.registerJavaMethod(callback, "voidMethodWithStringParameter", "bar", new Class<?>[] { String.class });

        v8.executeVoidScript("try {foo();} catch (e) {bar();}");

        // Runtime exception should not be thrown
    }

    @Test
    public void testJSCatchWillCatchJavaException2() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        doNothing().when(callback).voidMethodWithStringParameter(anyString());
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});
        v8.registerJavaMethod(callback, "voidMethodWithStringParameter", "bar", new Class<?>[] { String.class });

        v8.executeVoidScript("try {foo();} catch (e) {}");

        // Runtime exception should not be thrown
    }

    @Test(expected = RuntimeException.class)
    public void testIntMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).intMethodNoParameters();
        v8.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testDoubleMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).doubleMethodNoParameters();
        v8.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testBooleanMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testStringMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).stringMethodNoParameters();
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testV8ObjectMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testV8ArrayMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test
    public void testVoidMethodCallWithMissingObjectArgs() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo()");

        verify(callback).voidMethodWithObjectParameter(new V8Array.Undefined());
    }

    @Test
    public void testObjectMethodReturnsInteger() {
        ICallback callback = mock(ICallback.class);
        doReturn(7).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        int result = v8.executeIntFunction("foo", null);

        assertEquals(7, result);
    }

    @Test
    public void testObjectMethodReturnsBoolean() {
        ICallback callback = mock(ICallback.class);
        doReturn(true).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        boolean result = v8.executeBooleanFunction("foo", null);

        assertTrue(result);
    }

    @Test
    public void testObjectMethodReturnsDouble() {
        ICallback callback = mock(ICallback.class);
        doReturn(7.7).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        double result = v8.executeDoubleFunction("foo", null);

        assertEquals(7.7, result, 0.000001);
    }

    @Test
    public void testObjectMethodReturnsString() {
        ICallback callback = mock(ICallback.class);
        doReturn("foobar").when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        String result = v8.executeStringFunction("foo", null);

        assertEquals("foobar", result);
    }

    @Test
    public void testObjectMethodReturnsV8Object() {
        ICallback callback = mock(ICallback.class);
        doReturn(new V8Object(v8).add("foo", "bar")).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        V8Object result = v8.executeObjectFunction("foo", null);

        assertEquals("bar", result.getString("foo"));
        result.release();
    }

    @Test
    public void testObjectMethodReturnsV8Array() {
        ICallback callback = mock(ICallback.class);
        doReturn(new V8Array(v8).push(1).push("a")).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        V8Array result = v8.executeArrayFunction("foo", null);

        assertEquals(2, result.length());
        assertEquals(1, result.getInteger(0));
        assertEquals("a", result.getString(1));
        result.release();
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testObjectMethodReturnsIncompatibleType() {
        ICallback callback = mock(ICallback.class);
        doReturn(new Rectangle()).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        v8.executeVoidScript("foo()");
    }

    @Test
    public void testVarArgParametersString() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, 'foo', 'bar')");

        verify(callback).voidMethodVarArgsAndOthers(1, 2, "foo", "bar");
    }

    @Test
    public void testVarArgParametersObject() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, {}, {foo:'bar'})");

        verify(callback).voidMethodVarArgsAndOthers(eq(1), eq(2), any(V8Object.class), any(V8Object.class));
    }

    @Test
    public void testVarArgParametersArray() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, [], [1,2,3])");

        verify(callback).voidMethodVarArgsAndOthers(eq(1), eq(2), any(V8Object.class), any(V8Object.class));
    }

    @Test
    public void testVarArgParametersInts() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, 3, 4)");

        verify(callback).voidMethodVarArgsAndOthers(1, 2, 3, 4);
    }

    @Test
    public void testVarArgParametersMissing() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2)");

        verify(callback).voidMethodVarArgsAndOthers(1, 2);
    }

    @Test
    public void testMissingParamters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Double.TYPE,
                Boolean.TYPE, String.class, V8Object.class });

        v8.executeVoidScript("foo()");

        verify(callback).voidMethodWithParameters(0, 0d, false, null, new V8Object.Undefined());
    }

    @Test
    public void testSomeMissingParamters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Double.TYPE,
                Boolean.TYPE, String.class, V8Object.class });

        v8.executeVoidScript("foo(1,2)");

        verify(callback).voidMethodWithParameters(1, 2d, false, null, new V8Object.Undefined());
    }

    @Test
    public void testMissingParamtersWithVarArgs() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1)");

        verify(callback).voidMethodVarArgsAndOthers(1, 0);
    }

    @Test
    public void testMissingParamtersWithObjectParameters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameters", "foo", new Class<?>[] { Integer.class });

        v8.executeVoidScript("foo(1)");

        verify(callback).voidMethodWithObjectParameters(1);
    }

    @Test
    public void testMissingParamtersWithMissingObjectParameters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameters", "foo", new Class<?>[] { Integer.class });

        v8.executeVoidScript("foo()");

        verify(callback).voidMethodWithObjectParameters(0);
    }

    @Test
    public void testRegisterJavaCallback() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidScript("foo()");

        verify(callback).invoke(any(V8Array.class));
    }

    @Test
    public void testRegisterJavaCallbackExecuteFunction() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidFunction("foo", null);

        verify(callback).invoke(any(V8Array.class));
    }

    @Test
    public void testInvokeCallbackWithParameters() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");
        V8Object object = new V8Object(v8).add("foo", "bar");
        V8Array array = new V8Array(v8).push(1).push(2).push(3);
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push("test");
        parameters.push(3.14159);
        parameters.push(true);
        parameters.push(object);
        parameters.push(array);
        doAnswer(constructAnswer(parameters, null)).when(callback).invoke(any(V8Array.class));

        v8.executeVoidFunction("foo", parameters);
        parameters.release();
        object.release();
        array.release();
    }

    @Test
    public void testRegisterJavaVoidCallback() {
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidScript("foo()");

        verify(callback).invoke(any(V8Array.class));
    }

    @Test
    public void testRegisterJavaVoidCallbackExecuteFunction() {
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidFunction("foo", null);

        verify(callback).invoke(any(V8Array.class));
    }

    @Test
    public void testInvokeVoidCallbackWithParameters() {
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        v8.registerJavaMethod(callback, "foo");
        V8Object object = new V8Object(v8).add("foo", "bar");
        V8Array array = new V8Array(v8).push(1).push(2).push(3);
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push("test");
        parameters.push(3.14159);
        parameters.push(true);
        parameters.push(object);
        parameters.push(array);
        doAnswer(constructAnswer(parameters, null)).when(callback).invoke(any(V8Array.class));

        v8.executeVoidFunction("foo", parameters);
        parameters.release();
        object.release();
        array.release();
    }

    @Test
    public void testInvokeCallbackWithReturnValue() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");
        doAnswer(constructAnswer(null, 77)).when(callback).invoke(any(V8Array.class));

        int result = v8.executeIntFunction("foo", null);

        assertEquals(77, result);
    }

    private Answer<Object> constructAnswer(final V8Array parameters, final Object result) {
        return new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (parameters != null) {
                    assertEquals(parameters.length(), ((V8Array) args[0]).length());
                    for (int i = 0; i < args.length; i++) {
                        assertEquals(parameters.get(i), ((V8Array) args[0]).get(i));
                    }
                }
                return result;
            }
        };
    }

}
