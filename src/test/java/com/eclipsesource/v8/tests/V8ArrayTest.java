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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class V8ArrayTest {

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
    public void testGetArrayElementFromProperties() {
        V8Array v8Array = new V8Array(v8).push("1").push(2).push(3.3);

        String result1 = v8Array.getString("0");
        int result2 = v8Array.getInteger("1");
        double result3 = v8Array.getDouble("2");

        assertEquals("1", result1);
        assertEquals(2, result2);
        assertEquals(3.3, result3, 0.000001);
        v8Array.release();
    }

    @Test
    public void testSetArrayElementsWithProperties() {
        V8Array v8Array = new V8Array(v8);

        v8Array.add("0", 1);
        v8Array.add("10", 2);
        v8Array.add("19", 3);
        v8Array.add("bob", 4);

        assertEquals(20, v8Array.length());
        assertEquals(1, v8Array.getInteger(0));
        assertEquals(2, v8Array.getInteger(10));
        assertEquals(3, v8Array.getInteger(19));
        assertEquals(4, v8Array.getInteger("bob"));
        v8Array.release();
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

        assertEquals(3, array.length());
        array.release();
    }

    @Test
    public void testArraySizeZero() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        assertEquals(0, array.length());
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

        assertEquals(10000, array.length());
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

    /**** Get Object ****/
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

    /*** Get Array ***/
    @Test
    public void testArrayGetArray() {
        V8Array array = v8.executeArrayScript("foo = [[1,2,3],['first','second'],[true]]; foo");

        V8Array array1 = array.getArray(0);
        V8Array array2 = array.getArray(1);
        V8Array array3 = array.getArray(2);
        assertEquals(3, array1.length());
        assertEquals(2, array2.length());
        assertEquals(1, array3.length());

        array.release();
        array1.release();
        array2.release();
        array3.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetArrayWrongType() {
        V8Array array = v8.executeArrayScript("foo = [42]; foo");

        try {
            array.getArray(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetArrayIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("foo = []; foo");
        try {
            array.getArray(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetArrayChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        v8.executeVoidScript("foo[0] = [1,2,3]");
        V8Array array1 = array.getArray(0);
        assertEquals(3, array1.length());
        array.release();
        array1.release();
    }

    /**** Mixed Array ****/
    @Test
    public void testMixedArray() {
        V8Array array = v8.executeArrayScript("foo = ['a', 3, 3.1, true]; foo");

        assertEquals(4, array.length());
        assertEquals("a", array.getString(0));
        assertEquals(3, array.getInteger(1));
        assertEquals(3.1, array.getDouble(2), 0.00001);
        assertTrue(array.getBoolean(3));
        array.release();
    }

    /*** Add Primitives ***/
    @Test
    public void testAddInt() {
        V8Array array = new V8Array(v8);

        array.push(7);
        array.push(8);
        array.push(9);

        assertEquals(3, array.length());
        assertEquals(7, array.getInteger(0));
        assertEquals(8, array.getInteger(1));
        assertEquals(9, array.getInteger(2));
        array.release();
    }

    @Test
    public void testAddString() {
        V8Array array = new V8Array(v8);

        array.push("first");
        array.push("second");
        array.push("third");
        array.push("forth");

        assertEquals(4, array.length());
        assertEquals("first", array.getString(0));
        assertEquals("second", array.getString(1));
        assertEquals("third", array.getString(2));
        assertEquals("forth", array.getString(3));
        array.release();
    }

    @Test
    public void testAddDouble() {
        V8Array array = new V8Array(v8);

        array.push(1.1);
        array.push(2.2);
        array.push(3.3);
        array.push(4.9);

        assertEquals(4, array.length());
        assertEquals(1.1, array.getDouble(0), 0.000001);
        assertEquals(2.2, array.getDouble(1), 0.000001);
        assertEquals(3.3, array.getDouble(2), 0.000001);
        assertEquals(4.9, array.getDouble(3), 0.000001);
        array.release();
    }

    @Test
    public void testAddBoolean() {
        V8Array array = new V8Array(v8);

        array.push(true);
        array.push(false);

        assertEquals(2, array.length());
        assertTrue(array.getBoolean(0));
        assertFalse(array.getBoolean(1));
        array.release();
    }

    @Test
    public void testAddMixedValues() {
        V8Array array = new V8Array(v8);

        array.push(true);
        array.push(false);
        array.push(1);
        array.push("string");
        array.push(false);
        array.pushUndefined();

        assertEquals(6, array.length());
        assertTrue(array.getBoolean(0));
        assertFalse(array.getBoolean(1));
        assertEquals(1, array.getInteger(2));
        assertEquals("string", array.getString(3));
        assertFalse(array.getBoolean(4));
        assertEquals(V8Object.UNDEFINED, array.getType(5));

        array.release();
    }

    @Test
    public void testAddToExistingArray() {
        V8Array array = v8.executeArrayScript("foo = [1,2,3,,5]; foo;");

        array.push(false);

        assertEquals(6, array.length());
        assertFalse(array.getBoolean(5));
        array.release();
    }

    @Test
    public void testAddUndefined() {
        V8Array v8Array = new V8Array(v8).pushUndefined();

        assertEquals(1, v8Array.length());
        assertEquals(V8Object.UNDEFINED, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testCreateMatrix() {
        V8Array a1 = new V8Array(v8);
        V8Array a2 = new V8Array(v8);
        V8Array a3 = new V8Array(v8);
        a1.push(1);
        a1.push(2);
        a1.push(3);
        a2.push(4);
        a2.push(5);
        a2.push(6);
        a3.push(7);
        a3.push(8);
        a3.push(9);
        V8Array array = new V8Array(v8);
        array.push(a1);
        array.push(a2);
        array.push(a3);
        V8Array parameters = new V8Array(v8);
        parameters.push(array);

        v8.executeVoidScript("var total = 0; function add(matrix) { for(var i = 0; i < 3; i++) { for (var j = 0; j < 3; j++) { total = total + matrix[i][j]; }}};");
        v8.executeVoidFunction("add", parameters);
        int result = v8.getInteger("total");

        assertEquals(45, result);
        a1.release();
        a2.release();
        a3.release();
        array.release();
        parameters.release();
    }

    @Test
    public void testCreateArrayOfObjects() {
        V8Object obj1 = new V8Object(v8);
        V8Object obj2 = new V8Object(v8);
        obj1.add("first", "John");
        obj1.add("last", "Smith");
        obj1.add("age", 7);
        obj2.add("first", "Tim");
        obj2.add("last", "Jones");
        obj2.add("age", 8);
        V8Array array = new V8Array(v8);
        array.push(obj1);
        array.push(obj2);

        V8Object result1 = array.getObject(0);
        V8Object result2 = array.getObject(1);

        assertEquals("John", result1.getString("first"));
        assertEquals("Smith", result1.getString("last"));
        assertEquals(7, result1.getInteger("age"));
        assertEquals("Tim", result2.getString("first"));
        assertEquals("Jones", result2.getString("last"));
        assertEquals(8, result2.getInteger("age"));
        obj1.release();
        obj2.release();
        array.release();
        result1.release();
        result2.release();
    }

    /*** Test Types ***/
    @Test
    public void testGetTypeInt() {
        V8Array v8Array = new V8Array(v8);

        v8Array.push(1);

        assertEquals(V8Object.INTEGER, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeDouble() {
        V8Array v8Array = new V8Array(v8);

        v8Array.push(1.1);

        assertEquals(V8Object.DOUBLE, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeString() {
        V8Array v8Array = new V8Array(v8);

        v8Array.push("String");

        assertEquals(V8Object.STRING, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeBoolean() {
        V8Array v8Array = new V8Array(v8);

        v8Array.push(false);

        assertEquals(V8Object.BOOLEAN, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeArray() {
        V8Array v8Array = new V8Array(v8);
        V8Array value = new V8Array(v8);

        v8Array.push(value);

        assertEquals(V8Object.V8_ARRAY, v8Array.getType(0));
        v8Array.release();
        value.release();
    }

    @Test
    public void testGetTypeObject() {
        V8Array v8Array = new V8Array(v8);
        V8Object value = new V8Object(v8);

        v8Array.push(value);

        assertEquals(V8Object.V8_OBJECT, v8Array.getType(0));
        v8Array.release();
        value.release();
    }

    @Test
    public void testGetTypeIndexOutOfBounds() {
        V8Array v8Array = new V8Array(v8);

        int result = v8Array.getType(0);

        assertEquals(V8Object.UNDEFINED, result);
        v8Array.release();
    }

    /*** Equals ***/
    @Test
    public void testEqualsArray() {
        v8.executeVoidScript("a = [];");
        V8Array o1 = v8.executeArrayScript("a");
        V8Array o2 = v8.executeArrayScript("a");

        assertEquals(o1, o2);

        o1.release();
        o2.release();
    }

    @Test
    public void testEqualsArrayAndObject() {
        v8.executeVoidScript("a = [];");
        V8Array o1 = v8.executeArrayScript("a");
        V8Object o2 = v8.executeObjectScript("a");

        assertEquals(o1, o2);

        o1.release();
        o2.release();
    }

    @Test
    public void testNotEqualsArray() {
        V8Array a = v8.executeArrayScript("a = []; a");
        V8Array b = v8.executeArrayScript("b = []; b");

        assertNotEquals(a, b);

        a.release();
        b.release();
    }

    @Test
    public void testHashEqualsArray() {
        V8Array a = v8.executeArrayScript("a = []; a");
        V8Array b = v8.executeArrayScript("a");

        assertEquals(a.hashCode(), b.hashCode());

        a.release();
        b.release();
    }

    @Test
    public void testNotEqualsNull() {
        V8Array a = v8.executeArrayScript("a = []; a");

        assertNotEquals(a, null);
        assertNotEquals(null, a);

        a.release();
    }

    @Test
    public void testHashStable() {
        V8Array a = v8.executeArrayScript("a = []; a");
        int hash1 = a.hashCode();
        int hash2 = a.push(true).push(false).push(123).hashCode();

        assertEquals(hash1, hash2);

        a.release();
    }

    @Test
    public void testGetArrayOfInts() {
        V8Array a = v8.executeArrayScript("a = [1,2,3,4,5]; a");

        int[] result = a.getInts(0, 5);

        assertEquals(5, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        assertEquals(5, result[4]);
        a.release();
    }

    @Test
    public void testGetSubArrayOfInts() {
        V8Array a = v8.executeArrayScript("a = [1,2,3,4,5]; a");

        int[] result = a.getInts(4, 1);

        assertEquals(1, result.length);
        assertEquals(5, result[0]);
        a.release();
    }

    @Test
    public void testGetSubArrayOfInts2() {
        V8Array a = v8.executeArrayScript("a = [1,2,3,4,5]; a");

        int[] result = a.getInts(3, 2);

        assertEquals(2, result.length);
        assertEquals(4, result[0]);
        assertEquals(5, result[1]);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetIntsWithoutInts() {
        V8Array a = v8.executeArrayScript("a = [1,'a',3,4,5]; a");

        try {
            a.getInts(0, 5);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetIntsWithDoublesThrowsExceptions() {
        V8Array a = v8.executeArrayScript("a = [1,1.1,3,4,5]; a");

        try {
            a.getInts(0, 5);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetSubArrayOfIntsOutOfBounds() {
        V8Array a = v8.executeArrayScript("a = [1,2,3,4,5]; a");

        try {
            a.getInts(3, 3);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetArrayOfDoubles() {
        V8Array a = v8.executeArrayScript("a = [1.1,2.1,3.1,4.1,5.1]; a");

        double[] result = a.getDoubles(0, 5);

        assertEquals(5, result.length);
        assertEquals(1.1, result[0], 000001);
        assertEquals(2.1, result[1], 000001);
        assertEquals(3.1, result[2], 000001);
        assertEquals(4.1, result[3], 000001);
        assertEquals(5.1, result[4], 000001);
        a.release();
    }

    @Test
    public void testGetSubArrayOfDoubles() {
        V8Array a = v8.executeArrayScript("a = [1.1,2.1,3.1,4.1,5.1]; a");

        double[] result = a.getDoubles(4, 1);

        assertEquals(1, result.length);
        assertEquals(5.1, result[0], 0.000001);
        a.release();
    }

    @Test
    public void testGetSubArrayOfDoubles2() {
        V8Array a = v8.executeArrayScript("a = [1.1,2.1,3.1,4.1,5.1]; a");

        double[] result = a.getDoubles(3, 2);

        assertEquals(2, result.length);
        assertEquals(4.1, result[0], 0.000001);
        assertEquals(5.1, result[1], 0.000001);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetDoublesWithoutDoubles() {
        V8Array a = v8.executeArrayScript("a = [1.1,'a',3.1,4.1,5.1]; a");

        try {
            a.getInts(0, 5);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetSubArrayOfDoublesOutOfBounds() {
        V8Array a = v8.executeArrayScript("a = [1.1,2.1,3.1,4.1,5.1]; a");

        try {
            a.getInts(3, 3);
        } finally {
            a.release();
        }
    }
}