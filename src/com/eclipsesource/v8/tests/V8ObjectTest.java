package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    /*** Add Primitives ***/
    @Test
    public void testAddString() {
        V8Object v8Object = new V8Object(v8);

        v8Object.add("hello", "world");

        assertEquals("world", v8Object.getString("hello"));
        v8Object.release();
    }

    @Test
    public void testAddInt() {
        V8Object v8Object = new V8Object(v8);

        v8Object.add("hello", 7);

        assertEquals(7, v8Object.getInteger("hello"));
        v8Object.release();
    }

    @Test
    public void testAddDouble() {
        V8Object v8Object = new V8Object(v8);

        v8Object.add("hello", 3.14159);

        assertEquals(3.14159, v8Object.getDouble("hello"), 0.000001);
        v8Object.release();
    }

    @Test
    public void testAddBoolean() {
        V8Object v8Object = new V8Object(v8);

        v8Object.add("hello", true);

        assertTrue(v8Object.getBoolean("hello"));
        v8Object.release();
    }

    @Test
    public void testObjectChangedFromJS() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("world", "hello");
        v8.add("object", v8Object);

        v8.executeVoidScript("object.world = 'goodbye'");

        assertEquals("goodbye", v8Object.getString("world"));
        v8Object.release();
    }

    @Test
    public void testObjectChangedFromAPI() {
        v8.executeVoidScript("object = {world : 'goodbye'}");

        V8Object v8Object = v8.getObject("object");
        v8Object.add("world", "hello");

        assertEquals("hello", v8Object.getString("world"));
        v8Object.release();
    }

    /*** Add Object ***/
    @Test
    public void testAddObject() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", true);

        v8.add("foo", v8Object);

        V8Object foo = v8.getObject("foo");
        assertTrue(foo.getBoolean("hello"));
        foo.release();
        v8Object.release();
    }

    @Test
    public void testAddObjectWithString() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "world");

        v8.add("foo", v8Object);

        String result = v8.executeStringScript("foo.hello");
        assertEquals("world", result);
        v8Object.release();
    }

    @Test
    public void testAddObjectWithBoolean() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("boolean", false);

        v8.add("foo", v8Object);

        boolean result = v8.executeBooleanScript("foo.boolean");
        assertFalse(result);
        v8Object.release();
    }

    @Test
    public void testAddObjectWithInt() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("integer", 75);

        v8.add("foo", v8Object);

        int result = v8.executeIntScript("foo.integer");
        assertEquals(75, result);
        v8Object.release();
    }

    @Test
    public void testAddObjectWithDouble() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("double", 75.5);

        v8.add("foo", v8Object);

        double result = v8.executeDoubleScript("foo.double");
        assertEquals(75.5, result, 0.000001);
        v8Object.release();
    }

    @Test
    public void testAddObjectToObject() {
        V8Object v8Object = new V8Object(v8);
        V8Object nested = new V8Object(v8);
        nested.add("foo", "bar");
        v8Object.add("nested", nested);
        v8.add("foo", v8Object);

        String result = v8.executeStringScript("foo.nested.foo");

        assertEquals("bar", result);
        v8Object.release();
        nested.release();
    }

    /*** Add Array ***/
    @Test
    public void testAddArrayToObject() {
        V8Object v8Object = new V8Object(v8);
        V8Array array = new V8Array(v8);
        v8Object.add("array", array);
        v8.add("foo", v8Object);

        V8Array result = v8.executeArrayScript("foo.array");

        assertNotNull(result);
        v8Object.release();
        array.release();
        result.release();
    }
}
