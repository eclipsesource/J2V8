package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class V8ObjectTest {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = new V8();
    }

    @After
    public void tearDown() {
        v8.release();
    }

    @Test
    public void testCreateReleaseObject() {
        for (int i = 0; i < 1000; i++) {
            V8Object persistentV8Object = new V8Object(v8);
            persistentV8Object.release();
        }
    }

    @Test
    public void testDoesNotReleaseObject() {
        try {
            new V8Object(v8);
            v8.release();
        } catch (IllegalStateException e) {
            v8 = new V8();
            return;
        }
        fail("Illegal State Exception not thrown.");
    }

    @Test
    public void testGetV8Object() {
        v8.executeVoidScript("foo = {key: 'value'}");

        V8Object object = v8.getObject("foo");

        assertTrue(object.contains("key"));
        assertFalse(object.contains("noKey"));
        object.release();
    }

    @Test
    public void testGetMultipleV8Object() {
        v8.executeVoidScript("foo = {key: 'value'}; " + "bar={key : 'value'}");

        V8Object fooObject = v8.getObject("foo");
        V8Object barObject = v8.getObject("bar");

        assertTrue(fooObject.contains("key"));
        assertFalse(fooObject.contains("noKey"));
        assertTrue(barObject.contains("key"));
        assertFalse(barObject.contains("noKey"));

        fooObject.release();
        barObject.release();
    }

    @Test
    public void testGetNestedV8Object() {
        v8.executeVoidScript("foo = {nested: {key : 'value'}}");

        for (int i = 0; i < 1000; i++) {
            V8Object fooObject = v8.getObject("foo");
            V8Object nested = fooObject.getObject("nested");

            assertTrue(fooObject.contains("nested"));
            assertTrue(nested.contains("key"));
            assertFalse(nested.contains("noKey"));
            fooObject.release();
            nested.release();
        }
    }

    /*** Get Primitives ***/
    @Test
    public void testGetIntegerV8Object() {
        v8.executeVoidScript("foo = {bar: 7}");

        V8Object foo = v8.getObject("foo");

        assertEquals(7, foo.getInteger("bar"));
        foo.release();
    }

    @Test
    public void testNestedInteger() {
        v8.executeVoidScript("foo = {bar: {key:6}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals(6, bar.getInteger("key"));
        foo.release();
        bar.release();
    }

    @Test
    public void testGetDoubleV8Object() {
        v8.executeVoidScript("foo = {bar: 7.1}");

        V8Object foo = v8.getObject("foo");

        assertEquals(7.1, foo.getDouble("bar"), 0.0001);
        foo.release();
    }

    @Test
    public void testNestedDouble() {
        v8.executeVoidScript("foo = {bar: {key:6.1}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals(6.1, bar.getDouble("key"), 0.0001);
        foo.release();
        bar.release();
    }

    @Test
    public void testGetBooleanV8Object() {
        v8.executeVoidScript("foo = {bar: false}");

        V8Object foo = v8.getObject("foo");

        assertFalse(foo.getBoolean("bar"));
        foo.release();
    }

    @Test
    public void testNestedBoolean() {
        v8.executeVoidScript("foo = {bar: {key:true}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertTrue(bar.getBoolean("key"));
        foo.release();
        bar.release();
    }

    @Test
    public void testGetStringV8Object() {
        v8.executeVoidScript("foo = {bar: 'string'}");

        V8Object foo = v8.getObject("foo");

        assertEquals("string", foo.getString("bar"));
        foo.release();
    }

    @Test
    public void testNestedString() {
        v8.executeVoidScript("foo = {bar: {key:'string'}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals("string", bar.getString("key"));
        foo.release();
        bar.release();
    }

    /*** Execute Object Script ***/
    @Test
    public void testObjectScript() {
        v8.executeVoidScript("function foo() { return { x : 7 }} ");

        V8Object result = v8.executeObjectFunction("foo", null);

        assertEquals(7, result.getInteger("x"));
        result.release();
    }
}
