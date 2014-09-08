package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V8JSFunctionCallTest {

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

    @Test
    public void testIntFunction() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push(8);

        int result = v8.executeIntFunction("add", parameters);

        assertEquals(15, result);
        parameters.release();
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

        int result = v8.executeIntFunction("foo", parameters);

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

        assertEquals(0, result.getSize());
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

        int result = v8.executeIntFunction("foo", null);

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

        assertEquals(2, result.getSize());
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
        int result = object.executeIntFunction("addFuction", parameters);
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

        assertEquals(26, v8.executeIntFunction("countLength", parameters));
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
        int result = v8.executeIntFunction("add", parameters);

        assertEquals(15, result);
        obj1.release();
        obj2.release();
        parameters.release();
    }

}
