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

import static com.eclipsesource.v8.V8Value.BOOLEAN;
import static com.eclipsesource.v8.V8Value.DOUBLE;
import static com.eclipsesource.v8.V8Value.INTEGER;
import static com.eclipsesource.v8.V8Value.NULL;
import static com.eclipsesource.v8.V8Value.STRING;
import static com.eclipsesource.v8.V8Value.UNDEFINED;
import static com.eclipsesource.v8.V8Value.V8_ARRAY;
import static com.eclipsesource.v8.V8Value.V8_FUNCTION;
import static com.eclipsesource.v8.V8Value.V8_OBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testDoNotReleaseArrayReference() {
        V8 _v8 = V8.createV8Runtime();
        new V8Array(_v8);
        _v8.release();
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
        V8Array array = v8.executeArrayScript("[1,2,3];");

        assertEquals(3, array.length());
        array.release();
    }

    @Test
    public void testArraySizeZero() {
        V8Array array = v8.executeArrayScript("[];");

        assertEquals(0, array.length());
        array.release();
    }

    /*** Undefined ***/
    @Test
    public void testUndefinedObjectProperty() {
        V8Array result = v8.getArray("array");

        assertTrue(result.isUndefined());
    }

    @Test
    public void testObjectUndefinedEqualsArrayUndefined() {
        assertEquals(new V8Object.Undefined(), new V8Array.Undefined());
    }

    @Test
    public void testObjectUndefinedHashCodeEqualsArrayUndefinedHashCode() {
        assertEquals(new V8Object.Undefined().hashCode(), new V8Array.Undefined().hashCode());
    }

    @Test
    public void testUndefinedEqual() {
        V8Array undefined1 = v8.getArray("foo");
        V8Array undefined2 = v8.getArray("bar");

        assertEquals(undefined1, undefined2);
    }

    @Test
    public void testStaticUndefined() {
        V8Array undefined = v8.getArray("foo");

        assertEquals(undefined, V8.getUndefined());
    }

    @Test
    public void testUndefinedHashCodeEquals() {
        V8Array undefined1 = v8.getArray("foo");
        V8Array undefined2 = v8.getArray("bar");

        assertEquals(undefined1.hashCode(), undefined2.hashCode());
    }

    @Test
    public void testUndefinedToString() {
        V8Array undefined = v8.getArray("object");

        assertEquals("undefined", undefined.toString());
    }

    @Test
    public void testUndefinedRelease() {
        V8Array undefined = v8.getArray("object");

        undefined.release();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetByteUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getByte(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBytesUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getBytes(0, 10);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBytesUndefined2() {
        V8Array undefined = v8.getArray("array");

        undefined.getBytes(0, 10, new byte[10]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddIntUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.add("foo", 7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddBooleanUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.add("foo", false);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddStringUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.add("foo", "bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddDoubleUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.add("foo", 7.7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddObjectUndefined() {
        V8Array undefined = v8.getArray("array");
        V8Object object = new V8Object(v8);

        try {
            undefined.add("foo", object);
        } finally {
            object.release();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddArrayUndefined() {
        V8Array undefined = v8.getArray("array");
        V8Array array = new V8Array(v8);

        try {
            undefined.add("foo", array);
        } finally {
            array.release();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddUndefinedUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.addUndefined("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContainsUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.contains("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteIntFunctionUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.executeIntegerFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteBooleanFunctionUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.executeBooleanFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteDoubleFunctionUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.executeDoubleFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteStringFunctionUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.executeStringFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteObjectFunctionUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.executeObjectFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteArrayFunctionUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.executeArrayFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteVoidFunctionUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.executeVoidFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetIntegerUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getInteger("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBooleanUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getBoolean("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDoubleUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getDouble("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStringUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getString("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetObjectUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getObject("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetArrayUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getArray("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetKeysUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getKeys();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetTypeKeyUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getType("bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetTypeIndexUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getType(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetPrototype() {
        V8Array undefined = v8.getArray("array");
        V8Object prototype = new V8Object(v8);

        try {
            undefined.setPrototype(prototype);
        } finally {
            prototype.release();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterJavaMethod() {
        V8Array undefined = v8.getArray("array");

        undefined.registerJavaMethod(mock(JavaCallback.class), "name");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterVoidJavaMethod() {
        V8Array undefined = v8.getArray("array");

        undefined.registerJavaMethod(mock(JavaVoidCallback.class), "name");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterAnyJavaMethod() {
        V8Array undefined = v8.getArray("array");

        undefined.registerJavaMethod(new Object(), "toString", "toString", new Class<?>[0], false);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetIntegerIndexUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getInteger(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBooleanIndexUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getBoolean(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDoubleIndexUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getDouble(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStringIndexUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getString(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetObjectIndexUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getObject(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetArrayIndexUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getArray(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushIntUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.push(7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushBooleanUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.push(false);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushDoubleUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.push(7.7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushStringUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.push("bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushV8ObjectUndefined() {
        V8Array undefined = v8.getArray("array");
        V8Object object = new V8Object(v8);

        try {
            undefined.push(object);
        } finally {
            object.release();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushV8ArrayUndefined() {
        V8Array undefined = v8.getArray("array");
        V8Array array = new V8Array(v8);

        try {
            undefined.push(array);
        } finally {
            array.release();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushUndefinedUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.pushUndefined();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetIntsUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getIntegers(0, 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetInts2Undefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getIntegers(0, 1, new int[1]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDoublesUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getDoubles(0, 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDoubles2Undefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getDoubles(0, 1, new double[1]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBooleansUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getBooleans(0, 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBooleans2Undefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getBooleans(0, 1, new boolean[1]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStringsUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getStrings(0, 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStrings2Undefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getStrings(0, 1, new String[1]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLengthUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.length();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetTypeUndefined() {
        V8Array undefined = v8.getArray("array");

        undefined.getType();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetIndex() {
        V8Array undefined = v8.getArray("array");

        undefined.get(7);
    }

    @Test
    public void testGetIsInteger() {
        V8Array array = v8.executeArrayScript("foo = [7]");

        Object result = array.get(0);

        assertTrue(result instanceof Integer);
        assertEquals(7, result);
        array.release();
    }

    @Test
    public void testGetIsDouble() {
        V8Array array = v8.executeArrayScript("foo = [7.7]");

        Object result = array.get(0);

        assertTrue(result instanceof Double);
        assertEquals(7.7, result);
        array.release();
    }

    @Test
    public void testGetIsString() {
        V8Array array = v8.executeArrayScript("foo = ['bar']");

        Object result = array.get(0);

        assertTrue(result instanceof String);
        assertEquals("bar", result);
        array.release();
    }

    @Test
    public void testGetIsBoolean() {
        V8Array array = v8.executeArrayScript("foo = [true]");

        Object result = array.get(0);

        assertTrue(result instanceof Boolean);
        assertEquals(true, result);
        array.release();
    }

    @Test
    public void testGetIsObject() {
        V8Array array = v8.executeArrayScript("foo = [{}]");

        Object result = array.get(0);

        assertTrue(result instanceof V8Object);
        array.release();
        ((Releasable) result).release();
    }

    @Test
    public void testGetIsArray() {
        V8Array array = v8.executeArrayScript("foo = [[]]");

        Object result = array.get(0);

        assertTrue(result instanceof V8Array);
        array.release();
        ((Releasable) result).release();
    }

    @Test
    public void testGetIsNull() {
        V8Array array = v8.executeArrayScript("foo = [null]");

        Object result = array.get(0);

        assertNull(result);
        array.release();
    }

    @Test
    public void testGetIsUndefined() {
        V8Array array = v8.executeArrayScript("foo = []");

        Object result = array.get(0);

        assertEquals(V8.getUndefined(), result);
        array.release();
    }

    @Test
    public void testGetIsFunction() {
        V8Array array = v8.executeArrayScript("foo = [function(){}]");

        Object result = array.get(0);

        assertTrue(result instanceof V8Function);
        array.release();
        ((Releasable) result).release();
    }

    /*** Null ***/
    @Test
    public void testNullStrinsgInArray() {
        V8Array array = v8.executeArrayScript("x = [null]; x;");

        assertNull(array.getString(0));
        array.release();
    }

    @Test
    public void testIsNull() {
        V8Array array = v8.executeArrayScript("x = [null]; x;");

        assertEquals(NULL, array.getType(0));
        array.release();
    }

    @Test
    public void testGetNullInArray() {
        V8Array array = v8.executeArrayScript("x = [null]; x;");

        assertNull(array.getObject(0));
        array.release();
    }

    @Test
    public void testAddNullAsObject() {
        V8Array array = new V8Array(v8).push((V8Object) null);

        assertNull(array.getObject(0));
        array.release();
    }

    @Test
    public void testAddNullAsString() {
        V8Array array = new V8Array(v8).push((String) null);

        assertNull(array.getObject(0));
        array.release();
    }

    @Test
    public void testAddNullAsArray() {
        V8Array array = new V8Array(v8).push((V8Array) null);

        assertNull(array.getArray(0));
        array.release();
    }

    /*** Get Byte ***/
    @Test
    public void testGetIntegerAsByte() {
        V8Array array = v8.executeArrayScript("foo = [3]");

        byte result = array.getByte(0);

        assertEquals(3, result);
        array.release();
    }

    @Test
    public void testGetIntegerAsByte_Overflow() {
        V8Array array = v8.executeArrayScript("foo = [256]");

        byte result = array.getByte(0);

        assertEquals(0, result);
        array.release();
    }

    /*** Get Int ***/
    @Test
    public void testArrayGetInt() {
        V8Array array = v8.executeArrayScript("[1,2,8];");

        assertEquals(1, array.getInteger(0));
        assertEquals(2, array.getInteger(1));
        assertEquals(8, array.getInteger(2));
        array.release();
    }

    @Test
    public void testArrayGetIntFromDouble() {
        V8Array array = v8.executeArrayScript("[1.1, 2.2];");

        assertEquals(1, array.getInteger(0));
        assertEquals(2, array.getInteger(1));
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetIntWrongType() {
        V8Array array = v8.executeArrayScript("['string'];");

        try {
            array.getInteger(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetIntIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("[];");
        try {
            array.getInteger(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetIntChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo;");

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
        V8Array array = v8.executeArrayScript("[true,false,false];");

        assertTrue(array.getBoolean(0));
        assertFalse(array.getBoolean(1));
        assertFalse(array.getBoolean(2));
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetBooleanWrongType() {
        V8Array array = v8.executeArrayScript("['string'];");

        try {
            array.getBoolean(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetBooleanIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("[];");
        try {
            array.getBoolean(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetBooleanChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo;");

        v8.executeVoidScript("foo[0] = true");
        assertTrue(array.getBoolean(0));
        array.release();
    }

    /*** Get Double ***/
    @Test
    public void testArrayGetDouble() {
        V8Array array = v8.executeArrayScript("[3.1,4.2,5.3];");

        assertEquals(3.1, array.getDouble(0), 0.00001);
        assertEquals(4.2, array.getDouble(1), 0.00001);
        assertEquals(5.3, array.getDouble(2), 0.00001);
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetDoubleWrongType() {
        V8Array array = v8.executeArrayScript("['string'];");

        try {
            array.getDouble(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetDoubleIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("[];");
        try {
            array.getDouble(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetDoubleChangeValue() {
        V8Array array = v8.executeArrayScript("foo = []; foo;");

        v8.executeVoidScript("foo[0] = 3.14159");
        assertEquals(3.14159, array.getDouble(0), 0.000001);
        array.release();
    }

    /*** Get String ***/
    @Test
    public void testArrayGetString() {
        V8Array array = v8.executeArrayScript("['first','second','third'];");

        assertEquals("first", array.getString(0));
        assertEquals("second", array.getString(1));
        assertEquals("third", array.getString(2));
        array.release();
    }

    @Test
    public void testArrayGetString_Unicode() {
        V8Array array = v8.executeArrayScript("['🎉','🌞','💐'];");

        assertEquals("🎉", array.getString(0));
        assertEquals("🌞", array.getString(1));
        assertEquals("💐", array.getString(2));
        array.release();
    }

    @Test
    public void testArrayGetStrings_Unicode() {
        V8Array array = v8.executeArrayScript("['🎉','🌞','💐'];");

        String[] result = array.getStrings(0, 3);
        assertEquals("🎉", result[0]);
        assertEquals("🌞", result[1]);
        assertEquals("💐", result[2]);
        array.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetStringWrongType() {
        V8Array array = v8.executeArrayScript("[42];");

        try {
            array.getString(0);
        } finally {
            array.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetStringIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("[];");
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
        V8Array array = v8.executeArrayScript("[{name : 'joe', age : 38 }];");

        V8Object object = array.getObject(0);
        assertEquals("joe", object.getString("name"));
        assertEquals(38, object.getInteger("age"));
        array.release();
        object.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testArrayGetObjectWrongType() {
        V8Array array = v8.executeArrayScript("[42];");

        try {
            array.getObject(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetObjectIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("[];");
        V8Object result = array.getObject(0);

        assertTrue(result.isUndefined());

        array.release();
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
        V8Array array = v8.executeArrayScript("[[1,2,3],['first','second'],[true]];");

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
        V8Array array = v8.executeArrayScript("[42];");

        try {
            array.getArray(0);
        } finally {
            array.release();
        }
    }

    @Test
    public void testArrayGetArrayIndexOutOfBounds() {
        V8Array array = v8.executeArrayScript("[];");

        V8Array result = array.getArray(0);

        assertTrue(result.isUndefined());
        array.release();
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
        V8Array array = v8.executeArrayScript("['a', 3, 3.1, true];");

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
        assertEquals(UNDEFINED, array.getType(5));

        array.release();
    }

    @Test
    public void testAddToExistingArray() {
        V8Array array = v8.executeArrayScript("[1,2,3,,5];");

        array.push(false);

        assertEquals(6, array.length());
        assertFalse(array.getBoolean(5));
        array.release();
    }

    @Test
    public void testSparseArrayLength() {
        V8Array array = v8.executeArrayScript("x = []; x[0] = 'foo'; x[100] = 'bar'; x['boo'] = 'baz'; x");

        assertEquals(101, array.length());
        array.release();
    }

    @Test
    public void testAddUndefined() {
        V8Array v8Array = new V8Array(v8).pushUndefined();

        assertEquals(1, v8Array.length());
        assertEquals(UNDEFINED, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testAddNull() {
        V8Array v8Array = new V8Array(v8).pushNull();

        assertEquals(1, v8Array.length());
        assertEquals(NULL, v8Array.getType(0));
        assertNull(v8Array.getObject(0));
        v8Array.release();
    }

    @Test
    public void testGetNull() {
        V8Array v8Array = v8.executeArrayScript("[null];");

        assertEquals(NULL, v8Array.getType(0));
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

        assertEquals(INTEGER, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeDouble() {
        V8Array v8Array = new V8Array(v8);

        v8Array.push(1.1);

        assertEquals(DOUBLE, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeString() {
        V8Array v8Array = new V8Array(v8);

        v8Array.push("String");

        assertEquals(STRING, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeBoolean() {
        V8Array v8Array = new V8Array(v8);

        v8Array.push(false);

        assertEquals(BOOLEAN, v8Array.getType(0));
        v8Array.release();
    }

    @Test
    public void testGetTypeArray() {
        V8Array v8Array = new V8Array(v8);
        V8Array value = new V8Array(v8);

        v8Array.push(value);

        assertEquals(V8_ARRAY, v8Array.getType(0));
        v8Array.release();
        value.release();
    }

    @Test
    public void testGetTypeFunction() {
        v8.executeVoidScript("var foo = function() {};");
        V8Object function = v8.getObject("foo");

        V8Array v8Array = new V8Array(v8).push(function);

        assertEquals(V8_FUNCTION, v8Array.getType(0));
        v8Array.release();
        function.release();
    }

    @Test
    public void testGetTypeObject() {
        V8Array v8Array = new V8Array(v8);
        V8Object value = new V8Object(v8);

        v8Array.push(value);

        assertEquals(V8_OBJECT, v8Array.getType(0));
        v8Array.release();
        value.release();
    }

    @Test
    public void testGetTypeIndexOutOfBounds() {
        V8Array v8Array = new V8Array(v8);

        int result = v8Array.getType(0);

        assertEquals(UNDEFINED, result);
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
    public void testGetTypeRangeOfInts() {
        V8Array a = v8.executeArrayScript("[1,2,3,4,5];");

        int result = a.getType(0, 5);

        assertEquals(INTEGER, result);
        a.release();
    }

    @Test
    public void testGetTypeRangeOfDoubles() {
        V8Array a = v8.executeArrayScript("[1.1,2.2,3.3,4.4,5.5];");

        int result = a.getType(1, 3);

        assertEquals(DOUBLE, result);
        a.release();
    }

    @Test
    public void testGetTypeRangeOfStrings() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 1, 2];");

        int result = a.getType(0, 3);

        assertEquals(STRING, result);
        a.release();
    }

    @Test
    public void testGetTypeRangeOfBooleans() {
        V8Array a = v8.executeArrayScript("[1, false, true, false, 2];");

        int result = a.getType(1, 3);

        assertEquals(BOOLEAN, result);
        a.release();
    }

    @Test
    public void testGetTypeRangeOfUndefined() {
        V8Array a = v8.executeArrayScript("[1, undefined, undefined, undefined, 2];");

        int result = a.getType(1, 3);

        assertEquals(UNDEFINED, result);
        a.release();
    }

    @Test
    public void testGetTypeRangeOfArrays() {
        V8Array a = v8.executeArrayScript("[1, [1], [false], ['string'], 2];");

        int result = a.getType(1, 3);

        assertEquals(V8_ARRAY, result);
        a.release();
    }

    @Test
    public void testGetTypeRangeOfObjects() {
        V8Array a = v8.executeArrayScript("[1, {foo:1}, {foo:false}, {foo:'string'}, 2];");

        int result = a.getType(1, 3);

        assertEquals(V8_OBJECT, result);
        a.release();
    }

    @Test
    public void testGetTypeSubRangeOfInts() {
        V8Array a = v8.executeArrayScript("[1,2,3,4,5];");

        int result = a.getType(4, 1);

        assertEquals(INTEGER, result);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetMixedTypeRangeThrowsUndefinedException() {
        V8Array a = v8.executeArrayScript("[1, false, true, false, 2];");

        try {
            a.getType(0, 5);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetTypeRangeSizeZeroThrowsUndefinedException() {
        V8Array a = v8.executeArrayScript("[1, false, true, false, 2];");

        try {
            a.getType(0, 0);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetTypeOutOfBoundsThrowsUndefinedException() {
        V8Array a = v8.executeArrayScript("[1, false, true, false, 2];");

        try {
            a.getType(5, 0);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetArrayOfInts() {
        V8Array a = v8.executeArrayScript("[1,2,3,4,5];");

        int[] result = a.getIntegers(0, 5);

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
        V8Array a = v8.executeArrayScript("[1,2,3,4,5];");

        int[] result = a.getIntegers(4, 1);

        assertEquals(1, result.length);
        assertEquals(5, result[0]);
        a.release();
    }

    @Test
    public void testGetSubArrayOfInts2() {
        V8Array a = v8.executeArrayScript("[1,2,3,4,5];");

        int[] result = a.getIntegers(3, 2);

        assertEquals(2, result.length);
        assertEquals(4, result[0]);
        assertEquals(5, result[1]);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetIntsWithoutInts() {
        V8Array a = v8.executeArrayScript("[1,'a',3,4,5];");

        try {
            a.getIntegers(0, 5);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetIntsWithDoubles() {
        V8Array a = v8.executeArrayScript("[1,1.1,3,4,5];");

        int[] ints = a.getIntegers(0, 5);

        assertEquals(5, ints.length);
        assertEquals(1, ints[0]);
        assertEquals(1, ints[1]);
        assertEquals(3, ints[2]);
        assertEquals(4, ints[3]);
        assertEquals(5, ints[4]);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetSubArrayOfIntsOutOfBounds() {
        V8Array a = v8.executeArrayScript("[1,2,3,4,5];");

        try {
            a.getIntegers(3, 3);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetArrayOfDoubles() {
        V8Array a = v8.executeArrayScript("[1.1,2.1,3.1,4.1,5.1];");

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
        V8Array a = v8.executeArrayScript("[1.1,2.1,3.1,4.1,5.1];");

        double[] result = a.getDoubles(4, 1);

        assertEquals(1, result.length);
        assertEquals(5.1, result[0], 0.000001);
        a.release();
    }

    @Test
    public void testGetSubArrayOfDoubles2() {
        V8Array a = v8.executeArrayScript("[1.1,2.1,3.1,4.1,5.1];");

        double[] result = a.getDoubles(3, 2);

        assertEquals(2, result.length);
        assertEquals(4.1, result[0], 0.000001);
        assertEquals(5.1, result[1], 0.000001);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetDoublesWithoutDoubles() {
        V8Array a = v8.executeArrayScript("[1.1,'a',3.1,4.1,5.1];");

        try {
            a.getIntegers(0, 5);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetSubArrayOfDoublesOutOfBounds() {
        V8Array a = v8.executeArrayScript("[1.1,2.1,3.1,4.1,5.1];");

        try {
            a.getIntegers(3, 3);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetArrayOfBooleans() {
        V8Array a = v8.executeArrayScript("[true, false, true, true, false];");

        boolean[] result = a.getBooleans(0, 5);

        assertEquals(5, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertTrue(result[2]);
        assertTrue(result[3]);
        assertFalse(result[4]);
        a.release();
    }

    @Test
    public void testGetArrayOfBytes() {
        V8Array a = v8.executeArrayScript("[0, 1, 2, 3, 256];");

        byte[] result = a.getBytes(0, 5);

        assertEquals(5, result.length);
        assertEquals(0, result[0]);
        assertEquals(1, result[1]);
        assertEquals(2, result[2]);
        assertEquals(3, result[3]);
        assertEquals(0, result[4]);
        a.release();
    }

    @Test
    public void testGetSubArrayOfBooleans() {
        V8Array a = v8.executeArrayScript("[true, false, true, true, false];");

        boolean[] result = a.getBooleans(4, 1);

        assertEquals(1, result.length);
        assertFalse(result[0]);
        a.release();
    }

    @Test
    public void testGetSubArrayOfBooleans2() {
        V8Array a = v8.executeArrayScript("[true, false, true, true, false];");

        boolean[] result = a.getBooleans(3, 2);

        assertEquals(2, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetBooleansWithoutBooleans() {
        V8Array a = v8.executeArrayScript("[true, 'a', false, false, true];");

        try {
            a.getBooleans(0, 5);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetSubArrayOfBooleansOutOfBounds() {
        V8Array a = v8.executeArrayScript("[true, true, true, true, false];");

        try {
            a.getBooleans(3, 3);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetArrayOfStrings() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd', 'e'];");

        String[] result = a.getStrings(0, 5);

        assertEquals(5, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("d", result[3]);
        assertEquals("e", result[4]);
        a.release();
    }

    @Test
    public void testGetSubArrayOfStrings() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd', 'e'];");

        String[] result = a.getStrings(4, 1);

        assertEquals(1, result.length);
        assertEquals("e", result[0]);
        a.release();
    }

    @Test
    public void testGetSubArrayOfStrings2() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd', 'e'];");

        String[] result = a.getStrings(3, 2);

        assertEquals(2, result.length);
        assertEquals("d", result[0]);
        assertEquals("e", result[1]);
        a.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetStringsWithoutStrings() {
        V8Array a = v8.executeArrayScript("['a', 7, 'c', 'd', 'e'];");

        try {
            a.getStrings(0, 5);
        } finally {
            a.release();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetSubArrayOfStringsOutOfBounds() {
        V8Array a = v8.executeArrayScript("['a', 7, 'c', 'd', 'e']");

        try {
            a.getStrings(3, 3);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetArrayTypeInt() {
        V8Array a = v8.executeArrayScript("[1,2,3,4]");

        int type = a.getType();

        assertEquals(V8Value.INTEGER, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeDouble() {
        V8Array a = v8.executeArrayScript("[1.1,2.2,3.3,4.4]");

        int type = a.getType();

        assertEquals(V8Value.DOUBLE, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeDoubleSingleValue() {
        V8Array a = v8.executeArrayScript("[0.1]");

        int type = a.getType();

        assertEquals(V8Value.DOUBLE, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeDoubleMixed() {
        V8Array a = v8.executeArrayScript("[0, 0.1]");

        int type = a.getType();

        assertEquals(V8Value.DOUBLE, type);
        a.release();
    }

    public void testGetArrayTypeDoubleWithInts1() {
        V8Array a = v8.executeArrayScript("[1.1,2,3.3,4]");

        int type = a.getType();

        assertEquals(V8Value.DOUBLE, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeDoubleWithInts2() {
        V8Array a = v8.executeArrayScript("[1,2,3.3,4.4]");

        int type = a.getType();

        assertEquals(V8Value.DOUBLE, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeString() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd']");

        int type = a.getType();

        assertEquals(V8Value.STRING, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeBoolean() {
        V8Array a = v8.executeArrayScript("[true, false, false, true]");

        int type = a.getType();

        assertEquals(V8Value.BOOLEAN, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeObject() {
        V8Array a = v8.executeArrayScript("[{}, {}, {foo:'bar'}]");

        int type = a.getType();

        assertEquals(V8Value.V8_OBJECT, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeArray() {
        V8Array a = v8.executeArrayScript("[[], [1,2,3], []]");

        int type = a.getType();

        assertEquals(V8Value.V8_ARRAY, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeObjectWithArray1() {
        V8Array a = v8.executeArrayScript("[{}, []]");

        int type = a.getType();

        assertEquals(V8Value.V8_OBJECT, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeObjectWithArray2() {
        V8Array a = v8.executeArrayScript("[[], {}]");

        int type = a.getType();

        assertEquals(V8Value.V8_OBJECT, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeUndefined() {
        V8Array a = v8.executeArrayScript("[false, 1, true, 0]");

        int type = a.getType();

        assertEquals(V8Value.UNDEFINED, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeUndefined2() {
        V8Array a = v8.executeArrayScript("['false', false]");

        int type = a.getType();

        assertEquals(V8Value.UNDEFINED, type);
        a.release();
    }

    @Test
    public void testGetArrayTypeEmpty() {
        V8Array a = v8.executeArrayScript("['false', false]");

        int type = a.getType();

        assertEquals(V8Value.UNDEFINED, type);
        a.release();
    }

    @Test
    public void testGetIntsSameSizeArray() {
        V8Array a = v8.executeArrayScript("[1,2,3,4]");
        int[] result = new int[4];

        int size = a.getIntegers(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test
    public void testGetIntsBiggerArray() {
        V8Array a = v8.executeArrayScript("[1,2,3,4]");
        int[] result = new int[40];

        int size = a.getIntegers(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetIntsSmallerArray() {
        V8Array a = v8.executeArrayScript("[1,2,3,4]");
        int[] result = new int[3];

        try {
            a.getIntegers(0, 4, result);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetIntsPopulatesArray() {
        V8Array a = v8.executeArrayScript("[1,2,3,4]");
        int[] result = new int[4];

        a.getIntegers(0, 4, result);

        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        a.release();
    }

    @Test
    public void testGetDoubleSameSizeArray() {
        V8Array a = v8.executeArrayScript("[1,2.2,3.3,4]");
        double[] result = new double[4];

        int size = a.getDoubles(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test
    public void testGetDoublesBiggerArray() {
        V8Array a = v8.executeArrayScript("[1.1,2.2,3,4]");
        double[] result = new double[40];

        int size = a.getDoubles(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetDoublesSmallerArray() {
        V8Array a = v8.executeArrayScript("[1,2,3.3,4.4]");
        double[] result = new double[3];

        try {
            a.getDoubles(0, 4, result);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetDoublesPopulatesArray() {
        V8Array a = v8.executeArrayScript("[1.1,2.2,3.3,4]");
        double[] result = new double[4];

        a.getDoubles(0, 4, result);

        assertEquals(1.1, result[0], 0.000001);
        assertEquals(2.2, result[1], 0.000001);
        assertEquals(3.3, result[2], 0.000001);
        assertEquals(4, result[3], 0.000001);
        a.release();
    }

    @Test
    public void testGetBooleanSameSizeArray() {
        V8Array a = v8.executeArrayScript("[true, false, false, true]");
        boolean[] result = new boolean[4];

        int size = a.getBooleans(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test
    public void testGetBytesSameSizeArray() {
        V8Array a = v8.executeArrayScript("[0, 1, 2, 3]");
        byte[] result = new byte[4];

        int size = a.getBytes(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test
    public void testGetBooleanBiggerArray() {
        V8Array a = v8.executeArrayScript("[false, false, false, true]");
        boolean[] result = new boolean[40];

        int size = a.getBooleans(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test
    public void testGetBytesBiggerArray() {
        V8Array a = v8.executeArrayScript("[0, 1, 2, 3]");
        byte[] result = new byte[40];

        int size = a.getBytes(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetBooleanSmallerArray() {
        V8Array a = v8.executeArrayScript("[true, true, false, false]");
        boolean[] result = new boolean[3];

        try {
            a.getBooleans(0, 4, result);
        } finally {
            a.release();
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetBytesSmallerArray() {
        V8Array a = v8.executeArrayScript("[0, 1, 2, 3]");
        byte[] result = new byte[3];

        try {
            a.getBytes(0, 4, result);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetBooleanPopulatesArray() {
        V8Array a = v8.executeArrayScript("[true, false, false, true]");
        boolean[] result = new boolean[4];

        a.getBooleans(0, 4, result);

        assertTrue(result[0]);
        assertFalse(result[1]);
        assertFalse(result[2]);
        assertTrue(result[3]);
        a.release();
    }

    @Test
    public void testGetBytesPopulatesArray() {
        V8Array a = v8.executeArrayScript("[0, 1, 2, 256]");
        byte[] result = new byte[4];

        a.getBytes(0, 4, result);

        assertEquals(0, result[0]);
        assertEquals(1, result[1]);
        assertEquals(2, result[2]);
        assertEquals(0, result[3]);
        a.release();
    }

    @Test
    public void testGetStringSameSizeArray() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd']");
        String[] result = new String[4];

        int size = a.getStrings(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test
    public void testGetStringBiggerArray() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd']");
        String[] result = new String[40];

        int size = a.getStrings(0, 4, result);

        assertEquals(4, size);
        a.release();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetStringSmallerArray() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd']");
        String[] result = new String[3];

        try {
            a.getStrings(0, 4, result);
        } finally {
            a.release();
        }
    }

    @Test
    public void testGetStringPopulatesArray() {
        V8Array a = v8.executeArrayScript("['a', 'b', 'c', 'd']");
        String[] result = new String[4];

        a.getStrings(0, 4, result);

        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("d", result[3]);
        a.release();
    }

    @Test
    public void testUndefinedNotReleased() {
        com.eclipsesource.v8.V8Array.Undefined undefined = new V8Array.Undefined();
        undefined.release();

        assertFalse(undefined.isReleased());
    }

}