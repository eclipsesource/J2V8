package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

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
        for (int i = 0; i < 10000; i++) {
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

        for (int i = 0; i < 100000; i++) {
            V8Object fooObject = v8.getObject("foo");
            V8Object nested = fooObject.getObject("nested");

            assertTrue(fooObject.contains("nested"));
            assertTrue(nested.contains("key"));
            assertFalse(nested.contains("noKey"));
            fooObject.release();
            nested.release();
        }
    }
}
