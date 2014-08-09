package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V8ArrayTests {

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
    public void testCreateAndReleaseArray() {
        for (int i = 0; i < 10000; i++) {
            V8Array v8Array = new V8Array(v8);
            v8Array.release();
        }
    }

    @Test
    public void testArraySize() {
        V8Array array = v8.executeArrayScript("foo = [1,2,3]; foo");

        assertEquals(3, array.getSize());
        array.release();
    }

    @Test
    public void testArraySizeZero() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        assertEquals(0, array.getSize());
        array.release();
    }

    /*** Get Int ***/
    @Test
    public void testArrayGetInt() {
        V8Array array = v8.executeArrayScript("foo = [1,2,8]; foo");

        assertEquals(1, array.getInteger(0));
        assertEquals(2, array.getInteger(1));
        assertEquals(8, array.getInteger(2));
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetIntWrongType() {
        V8Array array = v8.executeArrayScript("foo = ['string']; foo");

        try {
            array.getInteger(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetIntIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("foo = []; foo");
        try {
            array.getInteger(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetIntChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        v8.executeVoidScript("foo[0] = 1");
        assertEquals(1, array.getInteger(0));
        array.release();
    }

    @Test
    public void testLargeArrayGetInt() {
        V8Array array = v8.executeArrayScript("foo = []; for ( var i = 0; i < 10000; i++) {foo[i] = i;}; foo");

        assertEquals(10000, array.getSize());
        for (int i = 0; i < 10000; i++) {
            assertEquals(i, array.getInteger(i));
        }
        array.release();
    }

    /*** Get Boolean ***/
    @Test
    public void testArrayGetBoolean() {
        V8Array array = v8.executeArrayScript("foo = [true,false,false]; foo");

        assertTrue(array.getBoolean(0));
        assertFalse(array.getBoolean(1));
        assertFalse(array.getBoolean(2));
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetBooleanWrongType() {
        V8Array array = v8.executeArrayScript("foo = ['string']; foo");

        try {
            array.getBoolean(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetBooleanIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("foo = []; foo");
        try {
            array.getBoolean(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetBooleanChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        v8.executeVoidScript("foo[0] = true");
        assertTrue(array.getBoolean(0));
        array.release();
    }

    /*** Get Double ***/
    @Test
    public void testArrayGetDouble() {
        V8Array array = v8.executeArrayScript("foo = [3.1,4.2,5.3]; foo");

        assertEquals(3.1, array.getDouble(0), 0.00001);
        assertEquals(4.2, array.getDouble(1), 0.00001);
        assertEquals(5.3, array.getDouble(2), 0.00001);
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetDoubleWrongType() {
        V8Array array = v8.executeArrayScript("foo = ['string']; foo");

        try {
            array.getDouble(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetDoubleIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("foo = []; foo");
        try {
            array.getDouble(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetDoubleChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        v8.executeVoidScript("foo[0] = 3.14159");
        assertEquals(3.14159, array.getDouble(0), 0.000001);
        array.release();
    }

    /*** Get String ***/
    @Test
    public void testArrayGetString() {
        V8Array array = v8.executeArrayScript("foo = ['first','second','third']; foo");

        assertEquals("first", array.getString(0));
        assertEquals("second", array.getString(1));
        assertEquals("third", array.getString(2));
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetStringWrongType() {
        V8Array array = v8.executeArrayScript("foo = [42]; foo");

        try {
            array.getString(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetStringIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("foo = []; foo");
        try {
            array.getString(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetStringChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        v8.executeVoidScript("foo[0] = 'test'");
        assertEquals("test", array.getString(0));
        array.release();
    }

    /**** Object Array ****/
    @Test
    public void testArrayGetObject() {
        V8Array array = v8.executeArrayScript("foo = [{name : 'joe', age : 38 }]; foo");

        V8Object object = array.getObject(0);
        assertEquals("joe", object.getString("name"));
        assertEquals(38, object.getInteger("age"));
        array.release();
        object.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetObjectWrongType() {
        V8Array array = v8.executeArrayScript("foo = [42]; foo");

        try {
            array.getObject(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetObjectIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("foo = []; foo");
        try {
            array.getObject(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetObjectChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        v8.executeVoidScript("foo[0] = {foo:'bar'}");
        V8Object obj = array.getObject(0);
        assertEquals("bar", obj.getString("foo"));
        array.release();
        obj.release();
    }

    /**** Mixed Array ****/
    @Test
    public void testMixedArray() {
        V8Array array = v8.executeArrayScript("foo = ['a', 3, 3.1, true]; foo");

        assertEquals(4, array.getSize());
        assertEquals("a", array.getString(0));
        assertEquals(3, array.getInteger(1));
        assertEquals(3.1, array.getDouble(2), 0.00001);
        assertTrue(array.getBoolean(3));
        array.release();
    }
}
