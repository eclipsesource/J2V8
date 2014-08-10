package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import static org.junit.Assert.assertEquals;

public class V8JSFunctionCallTest {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = new V8();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testIntFunction() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.add(7);
        parameters.add(8);

        int result = v8.executeIntFunction("add", parameters);

        assertEquals(15, result);
        parameters.release();
    }

    @Test
    public void testDoubleFunctionCall() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.add(1.1);
        parameters.add(2.2);

        double result = v8.executeDoubleFunction("add", parameters);

        assertEquals(3.3, result, 0.000001);
        parameters.release();
    }

    @Test
    public void testStringFunctionCall() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.add("hello, ");
        parameters.add("world!");

        String result = v8.executeStringFunction("add", parameters);

        assertEquals("hello, world!", result);
        parameters.release();
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
    public void testIntFunctionCallOnObject() {
        v8.executeVoidScript("function add(x, y) {return x + y;}");
        v8.executeVoidScript("adder = {};");
        v8.executeVoidScript("adder.addFuction = add;");
        V8Object object = v8.getObject("adder");

        V8Array parameters = new V8Array(v8);
        parameters.add(7);
        parameters.add(8);
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
        parameters.add(7.1);
        parameters.add(8.1);
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
        parameters.add("hello, ");
        parameters.add("world!");
        String result = object.executeStringFunction("addFuction", parameters);
        parameters.release();

        assertEquals("hello, world!", result);
        object.release();
    }

    @Test
    public void testStringParameter() {
        v8.executeVoidScript("function countLength(str) {return str.length;}");

        V8Array parameters = new V8Array(v8);
        parameters.add("abcdefghijklmnopqrstuvwxyz");

        assertEquals(26, v8.executeIntFunction("countLength", parameters));
        parameters.release();
    }

}
