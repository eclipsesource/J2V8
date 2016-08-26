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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8JSFunctionCallTest {

    private V8     v8;
    private Object result;

    @Before
    public void seutp() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testHandleReleasedReceiver() {
        V8Object object = v8.executeObjectScript("var x = { a: function() { return 10; } }; x;");
        V8Function function = (V8Function) object.get("a");
        object.release();
        V8Array parameters = new V8Array(v8);
        try {
            function.call(object, parameters);
        } finally {
            parameters.release();
            function.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testHandleReleasedParameters() {
        V8Object object = v8.executeObjectScript("var x = { a: function() { return 10; } }; x;");
        V8Function function = (V8Function) object.get("a");
        V8Array parameters = new V8Array(v8);
        parameters.release();
        try {
            function.call(object, parameters);
        } finally {
            object.release();
            function.release();
        }
    }

    @Test
    public void testGetFunction() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");

        V8Object result = v8.getObject("add");

        assertTrue(result instanceof V8Function);
        result.release();
    }

    @Test
    public void testCallFunction() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Function function = (V8Function) v8.getObject("add");
        V8Array parameters = new V8Array(v8).push(7).push(8);

        Object result = function.call(v8, parameters);

        assertEquals(15, result);
        function.release();
        parameters.release();
    }

    @Test
    public void testCallFunctionNullParameters() {
        v8.executeVoidScript("function call() {return true;}");
        V8Function function = (V8Function) v8.getObject("call");

        boolean result = (Boolean) function.call(v8, null);

        assertTrue(result);
        function.release();
    }

    @Test
    public void testCallFunctionNullReceiver() {
        v8.executeVoidScript("function call() {return this;}");
        V8Function function = (V8Function) v8.getObject("call");

        Object result = function.call(null, null);

        assertEquals(v8, result);
        function.release();
        ((Releasable) result).release();
    }

    @Test
    public void testCallFunctionOnUndefined() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Function function = (V8Function) v8.getObject("add");
        V8Array parameters = new V8Array(v8).push(7).push(8);

        Object result = function.call(new V8Object.Undefined(), parameters);

        assertEquals(15, result);
        function.release();
        parameters.release();
    }

    @Test
    public void testFunctionScope() {
        v8.executeVoidScript("function say() { return this.name + ' say meow!'} ");
        V8Function function = (V8Function) v8.getObject("say");
        V8Object ginger = new V8Object(v8).add("name", "ginger");
        V8Object felix = new V8Object(v8).add("name", "felix");

        Object result1 = function.call(ginger, null);
        Object result2 = function.call(felix, null);

        assertEquals("ginger say meow!", result1);
        assertEquals("felix say meow!", result2);
        function.release();
        ginger.release();
        felix.release();
    }

    @Test
    public void testIntFunction() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        int result = v8.executeIntegerFunction("add", parameters);

        assertEquals(15, result);
        parameters.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testIntegerFunctionNotInteger() {
        v8.executeVoidScript("function add(x, y) {return 'bar';}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeIntegerFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testIntegerFunctionNoReturn() {
        v8.executeVoidScript("function add(x, y) {;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeIntegerFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test
    public void testDoubleFunctionCall() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(1.1);
        parameters.push(2.2);

        double result = v8.executeDoubleFunction("add", parameters);

        assertEquals(3.3, result, 0.000001);
        parameters.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testDoubleFunctionNotDouble() {
        v8.executeVoidScript("function add(x, y) {return 'bar';}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeDoubleFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testDoubleFunctionNoReturn() {
        v8.executeVoidScript("function add(x, y) {;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeDoubleFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test
    public void testStringFunctionCall() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.push("hello, ");
        parameters.push("world!");

        String result = v8.executeStringFunction("add", parameters);

        assertEquals("hello, world!", result);
        parameters.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testStringFunctionNotString() {
        v8.executeVoidScript("function add(x, y) {return 7;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeStringFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testStringFunctionNoReturn() {
        v8.executeVoidScript("function add(x, y) {;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeStringFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testBooleanFunctionNotBoolean() {
        v8.executeVoidScript("function add(x, y) {return 'bar';}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeBooleanFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testBooleanFunctionNoReturn() {
        v8.executeVoidScript("function add(x, y) {;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeBooleanFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test
    public void testBooleanFunctionCall() {
        v8.executeVoidScript("function add(x, y) {return x&&y;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(true);
        parameters.push(true);

        boolean result = v8.executeBooleanFunction("add", parameters);

        assertTrue(result);
        parameters.release();
    }

    @Test
    public void testArrayFunctionCall() {
        v8.executeVoidScript("function add(a,b,c,d) {return [a,b,c,d];}");
        V8Array parameters = new V8Array(v8);
        parameters.push(true);
        parameters.push(false);
        parameters.push(7);
        parameters.push("foo");

        V8Array result = v8.executeArrayFunction("add", parameters);

        assertTrue(result.getBoolean(0));
        assertFalse(result.getBoolean(1));
        assertEquals(7, result.getInteger(2));
        assertEquals("foo", result.getString(3));
        parameters.release();
        result.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayFunctionNotArray() {
        v8.executeVoidScript("function add(x, y) {return 7;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeArrayFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test
    public void testObjectFunctionCall() {
        v8.executeVoidScript("function getPerson(first, last, age) {return {'first':first, 'last':last, 'age':age};}");
        V8Array parameters = new V8Array(v8);
        parameters.push("John");
        parameters.push("Smith");
        parameters.push(7);

        V8Object result = v8.executeObjectFunction("getPerson", parameters);

        assertEquals("John", result.getString("first"));
        assertEquals("Smith", result.getString("last"));
        assertEquals(7, result.getInteger("age"));
        parameters.release();
        result.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testObjectFunctionNotObject() {
        v8.executeVoidScript("function add(x, y) {return 7;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        try {
            v8.executeObjectFunction("add", parameters);
        } finally {
            parameters.release();
        }
    }

    @Test
    public void testFunctionCallWithObjectReturn() {
        v8.executeVoidScript("function getPerson(first, last, age) {return {'first':first, 'last':last, 'age':age};}");
        V8Array parameters = new V8Array(v8);
        parameters.push("John");
        parameters.push("Smith");
        parameters.push(7);

        Object result = v8.executeFunction("getPerson", parameters);

        assertTrue(result instanceof V8Object);
        V8Object v8Object = (V8Object) result;
        assertEquals("John", v8Object.getString("first"));
        assertEquals("Smith", v8Object.getString("last"));
        assertEquals(7, v8Object.getInteger("age"));
        parameters.release();
        v8Object.release();
    }

    @Test
    public void testFunctionCallWithIntegerReturn() {
        v8.executeVoidScript("function getAge(first, last, age) {return age;}");
        V8Array parameters = new V8Array(v8);
        parameters.push("John");
        parameters.push("Smith");
        parameters.push(7);

        Object result = v8.executeFunction("getAge", parameters);

        assertTrue(result instanceof Integer);
        assertEquals(7, result);
        parameters.release();
    }

    @Test
    public void testFunctionCallWithDoubleReturn() {
        v8.executeVoidScript("function getFoo() {return 33.3;}");

        Object result = v8.executeFunction("getFoo", null);

        assertEquals(33.3, (Double) result, 0.000001);
    }

    @Test
    public void testFunctionCallWithStringReturn() {
        v8.executeVoidScript("function getFoo() {return 'bar';}");

        Object result = v8.executeFunction("getFoo", null);

        assertEquals("bar", result);
    }

    @Test
    public void testFunctionCallWithBooleanReturn() {
        v8.executeVoidScript("function getFoo() {return true;}");

        Object result = v8.executeFunction("getFoo", null);

        assertTrue((Boolean) result);
    }

    @Test
    public void testFunctionCallWithNullReturn() {
        v8.executeVoidScript("function getFoo() {return null;}");

        Object result = v8.executeFunction("getFoo", null);

        assertNull(result);
    }

    @Test
    public void testFunctionCallWithUndefinedReturn() {
        v8.executeVoidScript("function getFoo() {return undefined;}");

        Object result = v8.executeFunction("getFoo", null);

        assertEquals(V8.getUndefined(), result);
    }

    @Test
    public void testFunctionCallWithArrayReturn() {
        v8.executeVoidScript("function getFoo() {return [1,2,3];}");

        Object result = v8.executeFunction("getFoo", null);

        assertTrue(result instanceof V8Array);
        V8Array v8Array = (V8Array) result;
        assertEquals(3, v8Array.length());
        assertEquals(1, v8Array.get(0));
        assertEquals(2, v8Array.get(1));
        assertEquals(3, v8Array.get(2));
        v8Array.release();
    }

    @Test
    public void testFunctionCallWithNoReturn() {
        v8.executeVoidScript("function getAge(first, last, age) {}");

        Object result = v8.executeFunction("getAge", null);

        assertEquals(V8.getUndefined(), result);
    }

    @Test
    public void testVoidFunctionCall() {
        v8.executeVoidScript("function setPerson(first, last, age) {person = {'first':first, 'last':last, 'age':age};}");
        V8Array parameters = new V8Array(v8);
        parameters.push("John");
        parameters.push("Smith");
        parameters.push(7);

        v8.executeVoidFunction("setPerson", parameters);
        V8Object result = v8.getObject("person");

        assertEquals("John", result.getString("first"));
        assertEquals("Smith", result.getString("last"));
        assertEquals(7, result.getInteger("age"));
        parameters.release();
        result.release();
    }

    @Test
    public void testIntFunctionCallNoParameters() {
        v8.executeVoidScript("function foo() {return 7;}");
        V8Array parameters = new V8Array(v8);

        int result = v8.executeIntegerFunction("foo", parameters);

        assertEquals(7, result);
        parameters.release();
    }

    @Test
    public void testDoubleFunctionCallNoParameters() {
        v8.executeVoidScript("function foo() {return 7.2;}");
        V8Array parameters = new V8Array(v8);

        double result = v8.executeDoubleFunction("foo", parameters);

        assertEquals(7.2, result, 0.0000001);
        parameters.release();
    }

    @Test
    public void testStringFunctionCallNoParameters() {
        v8.executeVoidScript("function foo() {return 'hello';}");
        V8Array parameters = new V8Array(v8);

        String result = v8.executeStringFunction("foo", parameters);

        assertEquals("hello", result);
        parameters.release();
    }

    @Test
    public void testBooleanFunctionCallNoParameters() {
        v8.executeVoidScript("function foo() {return true;}");
        V8Array parameters = new V8Array(v8);

        boolean result = v8.executeBooleanFunction("foo", parameters);

        assertTrue(result);
        parameters.release();
    }

    @Test
    public void testArrayFunctionCallNoParameters() {
        v8.executeVoidScript("function foo() {return [];}");
        V8Array parameters = new V8Array(v8);

        V8Array result = v8.executeArrayFunction("foo", parameters);

        assertEquals(0, result.length());
        parameters.release();
        result.release();
    }

    @Test
    public void testObjectFunctionCallNoParameters() {
        v8.executeVoidScript("function foo() {return {bar:8};}");
        V8Array parameters = new V8Array(v8);

        V8Object result = v8.executeObjectFunction("foo", parameters);

        assertEquals(8, result.getInteger("bar"));
        parameters.release();
        result.release();
    }

    @Test
    public void testVoidFunctionCallNoParameters() {
        v8.executeVoidScript("function foo() {x=7;}");
        V8Array parameters = new V8Array(v8);

        v8.executeVoidFunction("foo", parameters);

        assertEquals(7, v8.getInteger("x"));
        parameters.release();
    }

    @Test
    public void testIntFunctionCallNullParameters() {
        v8.executeVoidScript("function foo() {return 7;}");

        int result = v8.executeIntegerFunction("foo", null);

        assertEquals(7, result);
    }

    @Test
    public void testDoubleFunctionCallNullParameters() {
        v8.executeVoidScript("function foo() {return 7.1;}");

        double result = v8.executeDoubleFunction("foo", null);

        assertEquals(7.1, result, 0.000001);
    }

    @Test
    public void testStringFunctionCallNullParameters() {
        v8.executeVoidScript("function foo() {return 'hello';}");

        String result = v8.executeStringFunction("foo", null);

        assertEquals("hello", result);
    }

    @Test
    public void testBooleanFunctionCallNullParameters() {
        v8.executeVoidScript("function foo() {return true;}");

        boolean result = v8.executeBooleanFunction("foo", null);

        assertTrue(result);
    }

    @Test
    public void testArrayFunctionCallNullParameters() {
        v8.executeVoidScript("function foo() {return [1,2];}");

        V8Array result = v8.executeArrayFunction("foo", null);

        assertEquals(2, result.length());
        result.release();
    }

    @Test
    public void testObjectFunctionCallNullParameters() {
        v8.executeVoidScript("function foo() {return {a:'b'};}");

        V8Object result = v8.executeObjectFunction("foo", null);

        assertEquals("b", result.getString("a"));
        result.release();
    }

    @Test
    public void testVoidFunctionCallNullParameters() {
        v8.executeVoidScript("function foo() {x=7;}");

        v8.executeVoidFunction("foo", null);

        assertEquals(7, v8.getInteger("x"));
    }

    @Test
    public void testIntFunctionCallOnObject() {
        v8.executeVoidScript("function add(x, y) {return x + y;}");
        v8.executeVoidScript("adder = {};");
        v8.executeVoidScript("adder.addFuction = add;");
        V8Object object = v8.getObject("adder");

        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);
        int result = object.executeIntegerFunction("addFuction", parameters);
        parameters.release();

        assertEquals(15, result);
        object.release();
    }

    @Test
    public void testDoubleFunctionCallOnObject() {
        v8.executeVoidScript("function add(x, y) {return x + y;}");
        v8.executeVoidScript("adder = {};");
        v8.executeVoidScript("adder.addFuction = add;");
        V8Object object = v8.getObject("adder");

        V8Array parameters = new V8Array(v8);
        parameters.push(7.1);
        parameters.push(8.1);
        double result = object.executeDoubleFunction("addFuction", parameters);
        parameters.release();

        assertEquals(15.2, result, 0.000001);
        object.release();
    }

    @Test
    public void testStringFunctionCallOnObject() {
        v8.executeVoidScript("function add(x, y) {return x + y;}");
        v8.executeVoidScript("adder = {};");
        v8.executeVoidScript("adder.addFuction = add;");
        V8Object object = v8.getObject("adder");

        V8Array parameters = new V8Array(v8);
        parameters.push("hello, ");
        parameters.push("world!");
        String result = object.executeStringFunction("addFuction", parameters);
        parameters.release();

        assertEquals("hello, world!", result);
        object.release();
    }

    @Test
    public void testBooleanFunctionCallOnObject() {
        v8.executeVoidScript("function add(x, y) {return x && y;}");
        v8.executeVoidScript("adder = {};");
        v8.executeVoidScript("adder.addFuction = add;");
        V8Object object = v8.getObject("adder");

        V8Array parameters = new V8Array(v8);
        parameters.push(true);
        parameters.push(false);
        boolean result = object.executeBooleanFunction("addFuction", parameters);
        parameters.release();

        assertFalse(result);
        object.release();
    }

    @Test
    public void testArrayFunctionCallOnObject() {
        v8.executeVoidScript("function add(x, y) {return [x,y];}");
        v8.executeVoidScript("adder = {};");
        v8.executeVoidScript("adder.addFuction = add;");
        V8Object object = v8.getObject("adder");

        V8Array parameters = new V8Array(v8);
        parameters.push(true);
        parameters.push(false);
        V8Array result = object.executeArrayFunction("addFuction", parameters);
        parameters.release();

        assertFalse(result.getBoolean(1));
        assertTrue(result.getBoolean(0));
        result.release();
        object.release();
    }

    @Test
    public void testObjectFunctionCallOnObject() {
        v8.executeVoidScript("function getPoint(x, y) {return {'x':x, 'y':y};}");
        v8.executeVoidScript("pointer = {};");
        v8.executeVoidScript("pointer.pointGetter = getPoint;");
        V8Object object = v8.getObject("pointer");

        V8Array parameters = new V8Array(v8);
        parameters.push(8);
        parameters.push(9);
        V8Object result = object.executeObjectFunction("pointGetter", parameters);
        parameters.release();

        assertEquals(8, result.getInteger("x"));
        assertEquals(9, result.getInteger("y"));
        result.release();
        object.release();
    }

    @Test
    public void testVoidFunctionCallOnObject() {
        v8.executeVoidScript("pointer = {'x':0,'y':0};");
        v8.executeVoidScript("function setPoint(x, y) {pointer.x = x;pointer.y=y;}");
        v8.executeVoidScript("pointer.pointSetter = setPoint;");
        V8Object object = v8.getObject("pointer");

        V8Array parameters = new V8Array(v8);
        parameters.push(8);
        parameters.push(9);
        object.executeVoidFunction("pointSetter", parameters);
        parameters.release();

        assertEquals(8, object.getInteger("x"));
        assertEquals(9, object.getInteger("y"));
        object.release();
    }

    @Test
    public void testStringParameter() {
        v8.executeVoidScript("function countLength(str) {return str.length;}");

        V8Array parameters = new V8Array(v8);
        parameters.push("abcdefghijklmnopqrstuvwxyz");

        assertEquals(26, v8.executeIntegerFunction("countLength", parameters));
        parameters.release();
    }

    @Test
    public void testObjectParameter() {
        V8Object obj1 = new V8Object(v8);
        V8Object obj2 = new V8Object(v8);
        obj1.add("first", "John");
        obj1.add("last", "Smith");
        obj1.add("age", 7);
        obj2.add("first", "Tim");
        obj2.add("last", "Jones");
        obj2.add("age", 8);
        V8Array parameters = new V8Array(v8);
        parameters.push(obj1);
        parameters.push(obj2);

        v8.executeVoidScript("function add(p1, p2) {return p1.age + p2['age'];}");
        int result = v8.executeIntegerFunction("add", parameters);

        assertEquals(15, result);
        obj1.release();
        obj2.release();
        parameters.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteJSFunction_InvalidArg() {
        v8.executeVoidScript("function add(p1, p2) {return p1 + p2;}");

        v8.executeJSFunction("add", new Object(), 8);
    }

    @Test
    public void testExecuteJSFunction_VarArgs() {
        v8.executeVoidScript("function add() {return arguments.length;}");

        int result = (Integer) v8.executeJSFunction("add", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertEquals(10, result);
    }

    @Test
    public void testExecuteJSFunction_Integer() {
        v8.executeVoidScript("function add(p1, p2) {return p1 + p2;}");

        int result = (Integer) v8.executeJSFunction("add", 7, 8);

        assertEquals(15, result);
    }

    @Test
    public void testExecuteJSFunction_Float() {
        v8.executeVoidScript("function add(p1, p2) {return p1 + p2;}");

        double result = (Double) v8.executeJSFunction("add", 3.1f, 2.2f);

        assertEquals(5.3, result, 0.00001);
    }

    @Test
    public void testExecuteJSFunction_Double() {
        v8.executeVoidScript("function add(p1, p2) {return p1 + p2;}");

        double result = (Double) v8.executeJSFunction("add", 3.1d, 2.2d);

        assertEquals(5.3, result, 0.00001);
    }

    @Test
    public void testExecuteJSFunction_IntegerSingleParam() {
        v8.executeVoidScript("function add(p1) {return p1;}");

        int result = (Integer) v8.executeJSFunction("add", 7);

        assertEquals(7, result);
    }

    @Test
    public void testExecuteJSFunction_BooleanSingleParam() {
        v8.executeVoidScript("function add(p1) {return p1;}");

        boolean result = (Boolean) v8.executeJSFunction("add", false);

        assertFalse(result);
    }

    @Test
    public void testExecuteJSFunction_String() {
        v8.executeVoidScript("function add(p1, p2) {return p1 + p2;}");

        String result = (String) v8.executeJSFunction("add", "seven", "eight");

        assertEquals("seveneight", result);
    }

    @Test
    public void testExecuteJSFunction_V8Object() {
        V8Object object = new V8Object(v8).add("first", 7).add("second", 8);
        v8.executeVoidScript("function add(p1) {return p1.first + p1.second;}");

        int result = (Integer) v8.executeJSFunction("add", object);

        assertEquals(15, result);
        object.release();
    }

    @Test
    public void testExecuteJSFunction_V8Array() {
        V8Object array = new V8Array(v8).push(7).push(8);
        v8.executeVoidScript("function add(p1) {return p1[0] + p1[1];}");

        int result = (Integer) v8.executeJSFunction("add", array);

        assertEquals(15, result);
        array.release();
    }

    @Test
    public void testExecuteJSFunction_Mixed() {
        V8Object array = new V8Array(v8).push(7).push(8);
        V8Object object = new V8Object(v8).add("first", 7).add("second", 8);
        v8.executeVoidScript("function add(p1, p2) {return p1[0] + p1[1] + p2.first + p2.second ;}");

        int result = (Integer) v8.executeJSFunction("add", array, object);

        assertEquals(30, result);
        object.release();
        array.release();
    }

    @Test
    public void testExecuteJSFunction_Function() {
        v8.executeVoidScript("function add(p1, p2) {return p1();}");
        V8Function function = new V8Function(v8, new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                return 7;
            }
        });

        int result = (Integer) v8.executeJSFunction("add", function);

        assertEquals(7, result);
        function.release();
    }

    @Test
    public void testExecuteJSFunction_null() {
        v8.executeVoidScript("function test(p1) { if (!p1) { return 'passed';} }");

        String result = (String) v8.executeJSFunction("test", new Object[] { null });

        assertEquals("passed", result);
    }

    @Test
    public void testExecuteJSFunction_nullArray() {
        v8.executeVoidScript("function test() { return 'passed';}");

        String result = (String) v8.executeJSFunction("test", (Object[]) null);

        assertEquals("passed", result);
    }

    @Test
    public void testExecuteJSFunction_NoParameters() {
        v8.executeVoidScript("function test() { return 'passed';}");

        String result = (String) v8.executeJSFunction("test");

        assertEquals("passed", result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteJSFunction_UndefinedReceiver() {
        v8.executeVoidScript("function test() { }");
        V8Object undefined = new V8Object.Undefined();

        undefined.executeJSFunction("test", (Object[]) null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteFunction_UndefinedReceiver() {
        v8.executeVoidScript("function test() { }");
        V8Object undefined = new V8Object.Undefined();

        undefined.executeFunction("test", null);
    }

    @Test
    public void testExecuteJSFunction_undefined() {
        v8.executeVoidScript("function test(p1) { if (!p1) { return 'passed';} }");

        String result = (String) v8.executeJSFunction("test", V8.getUndefined());

        assertEquals("passed", result);
    }

    @Test
    public void testCallFunctionWithEmojiParamter() {
        V8Function v8Function = new V8Function(v8, new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                return parameters.get(0);
            }
        });

        V8Array paramters = new V8Array(v8).push("👄");
        Object result = v8Function.call(null, paramters);

        assertEquals("👄", result);
        paramters.release();
        v8Function.release();
    }

    @Test
    public void testCreateV8Function() {
        V8Function function = new V8Function(v8, new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                result = "passed";
                return null;
            }
        });
        function.call(null, null);

        assertEquals("passed", result);
        function.release();
    }

    @Test
    public void testCreateV8Function_CalledFromJS() {
        v8.executeScript("function doSomething(callback) { callback(); }");
        V8Function function = new V8Function(v8, new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                result = "passed";
                return null;
            }
        });
        V8Array parameters = new V8Array(v8).push(function);
        v8.executeVoidFunction("doSomething", parameters);
        function.release();
        parameters.release();

        assertEquals("passed", result);
    }

    @Test
    public void testCreateV8Function_CalledFromJS_AfterFunctionReleased() {
        v8.executeScript("function doSomething(callback) { callback(); }");
        V8Function function = new V8Function(v8, new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                result = "passed";
                return null;
            }
        });
        V8Array parameters = new V8Array(v8).push(function);
        function.release();
        v8.executeVoidFunction("doSomething", parameters);
        parameters.release();

        assertEquals("passed", result);
    }

}
