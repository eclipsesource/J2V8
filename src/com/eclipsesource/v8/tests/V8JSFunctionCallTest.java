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
    public void testSimpleFunctionCall() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Array parameters = new V8Array(v8);
        parameters.add(7);
        parameters.add(8);

        int result = v8.executeIntFunction("add", parameters);

        assertEquals(15, result);
        parameters.release();
    }

    @Test
    public void testSimpleFunctionCallNoParameters() {
        v8.executeVoidScript("function add() {return 7;}");
        V8Array parameters = new V8Array(v8);

        int result = v8.executeIntFunction("add", parameters);

        assertEquals(7, result);
        parameters.release();
    }

    @Test
    public void testSimpleFunctionCallNullParameters() {
        v8.executeVoidScript("function add() {return 7;}");

        int result = v8.executeIntFunction("add", null);

        assertEquals(7, result);
    }

    @Test
    public void testFunctionCallOnObject() {
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
    public void testStringParameter() {
        v8.executeVoidScript("function countLength(str) {return str.length;}");

        V8Array parameters = new V8Array(v8);
        parameters.add("abcdefghijklmnopqrstuvwxyz");

        assertEquals(26, v8.executeIntFunction("countLength", parameters));
        parameters.release();
    }

}
