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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8Object.Undefined;

public class V8ObjectTest {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            if (v8 != null) {
                v8.close();
            }
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testV8ValueNull_StringRepresentation() {
        assertEquals("Null", V8Value.getStringRepresentation(0));
    }

    @Test
    public void testV8ValueInteger_StringRepresentation() {
        assertEquals("Integer", V8Value.getStringRepresentation(1));
    }

    @Test
    public void testV8ValueDouble_StringRepresentation() {
        assertEquals("Double", V8Value.getStringRepresentation(2));
    }

    @Test
    public void testV8ValueBoolean_StringRepresentation() {
        assertEquals("Boolean", V8Value.getStringRepresentation(3));
    }

    @Test
    public void testV8ValueString_StringRepresentation() {
        assertEquals("String", V8Value.getStringRepresentation(4));
    }

    @Test
    public void testV8ValueV8Array_StringRepresentation() {
        assertEquals("V8Array", V8Value.getStringRepresentation(5));
    }

    @Test
    public void testV8ValueV8Object_StringRepresentation() {
        assertEquals("V8Object", V8Value.getStringRepresentation(6));
    }

    @Test
    public void testV8ValueV8Function_StringRepresentation() {
        assertEquals("V8Function", V8Value.getStringRepresentation(7));
    }

    @Test
    public void testV8ValueV8TypedArray_StringRepresentation() {
        assertEquals("V8TypedArray", V8Value.getStringRepresentation(8));
    }

    @Test
    public void testV8ValueByte_StringRepresentation() {
        assertEquals("Byte", V8Value.getStringRepresentation(9));
    }

    @Test
    public void testV8ValueV8ArrayBuffer_StringRepresentation() {
        assertEquals("V8ArrayBuffer", V8Value.getStringRepresentation(10));
    }

    @Test
    public void testV8ValueUInt8_StringRepresentation() {
        assertEquals("UInt8Array", V8Value.getStringRepresentation(11));
    }

    @Test
    public void testV8ValueUInt8Clamped_StringRepresentation() {
        assertEquals("UInt8ClampedArray", V8Value.getStringRepresentation(12));
    }

    @Test
    public void testV8ValueInt16_StringRepresentation() {
        assertEquals("Int16Array", V8Value.getStringRepresentation(13));
    }

    @Test
    public void testV8ValueUInt16_StringRepresentation() {
        assertEquals("UInt16Array", V8Value.getStringRepresentation(14));
    }

    @Test
    public void testV8ValueUInt32_StringRepresentation() {
        assertEquals("UInt32Array", V8Value.getStringRepresentation(15));
    }

    @Test
    public void testV8ValueFloat32_StringRepresentation() {
        assertEquals("Float32Array", V8Value.getStringRepresentation(16));
    }

    @Test
    public void testV8ValueUndefined_StringRepresentation() {
        assertEquals("Undefined", V8Value.getStringRepresentation(99));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testV8ValueIllegal_StringRepresentation() {
        V8Value.getStringRepresentation(17);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testV8ValueUndefined_StringRepresentation_deprecated() {
        assertEquals("Undefined", V8Value.getStringRepresentaion(99));
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testV8ValueIllegal_StringRepresentation_deprecated() {
        V8Value.getStringRepresentaion(17);
    }

    @Test
    public void testCreateReleaseObject() {
        for (int i = 0; i < 1000; i++) {
            V8Object persistentV8Object = new V8Object(v8);
            persistentV8Object.close();
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testReleaseRuntimeDoesNotReleaseObject() {
        try {
            new V8Object(v8);
            v8.close();
        } catch (IllegalStateException e) {
            v8 = V8.createV8Runtime();
            return;
        }
        fail("Illegal State Exception not thrown.");
    }

    @Test
    public void testToStringWorksOnReleasedV8Object() {
        V8Object v8Object = new V8Object(v8);

        v8Object.close();

        assertEquals("[Object released]", v8Object.toString());
    }

    @Test
    public void testToStringWorksOnReleasedV8Function() {
        V8Object v8Object = new V8Function(v8);

        v8Object.close();

        assertEquals("[Function released]", v8Object.toString());
    }

    @Test
    public void testToStringWorksOnReleasedV8Array() {
        V8Object v8Object = new V8Array(v8);

        v8Object.close();

        assertEquals("[Array released]", v8Object.toString());
    }

    @Test
    public void testToStringWorksOnReleasedV8Runtime_V8Object() {
        @SuppressWarnings("resource")
        V8Object v8Object = new V8Object(v8);
        v8.release(false);

        assertEquals("[Object released]", v8Object.toString());
        v8 = V8.createV8Runtime();
    }

    @Test
    public void testToStringWorksOnReleasedV8Runtime_V8Function() {
        @SuppressWarnings("resource")
        V8Object v8Object = new V8Function(v8);
        v8.release(false);

        assertEquals("[Function released]", v8Object.toString());
        v8 = V8.createV8Runtime();
    }

    @Test
    public void testToStringWorksOnReleasedV8Runtime_V8Array() {
        @SuppressWarnings("resource")
        V8Object v8Object = new V8Array(v8);
        v8.release(false);

        assertEquals("[Array released]", v8Object.toString());
        v8 = V8.createV8Runtime();
    }

    @Test(expected = IllegalStateException.class)
    public void testAccessReleasedObjectThrowsException2() {
        V8Object v8Object = new V8Object(v8);

        v8Object.close();

        v8Object.add("foo", "bar");
    }

    @Test
    public void testGetIsInteger() {
        V8Object object = v8.executeObjectScript("foo = {key: 7}");

        Object result = object.get("key");

        assertTrue(result instanceof Integer);
        assertEquals(7, result);
        object.close();
    }

    @Test
    public void testGetIsDouble() {
        V8Object object = v8.executeObjectScript("foo = {key: 7.7}");

        Object result = object.get("key");

        assertTrue(result instanceof Double);
        assertEquals(7.7, result);
        object.close();
    }

    @Test
    public void testGetIsString() {
        V8Object object = v8.executeObjectScript("foo = {key: 'bar'}");

        Object result = object.get("key");

        assertTrue(result instanceof String);
        assertEquals("bar", result);
        object.close();
    }

    @Test
    public void testGetIsBoolean() {
        V8Object object = v8.executeObjectScript("foo = {key: true}");

        Object result = object.get("key");

        assertTrue(result instanceof Boolean);
        assertEquals(true, result);
        object.close();
    }

    @Test
    public void testGetIsObject() {
        V8Object object = v8.executeObjectScript("foo = {key: {}}");

        Object result = object.get("key");

        assertTrue(result instanceof V8Object);
        object.close();
        ((Releasable) result).release();
    }

    @Test
    public void testGetIsArray() {
        V8Object object = v8.executeObjectScript("foo = {key: []}");

        Object result = object.get("key");

        assertTrue(result instanceof V8Array);
        object.close();
        ((Releasable) result).release();
    }

    @Test
    public void testGetIsNull() {
        V8Object object = v8.executeObjectScript("foo = {key: null}");

        Object result = object.get("key");

        assertNull(result);
        object.close();
    }

    @Test
    public void testGetIsUndefined() {
        V8Object object = v8.executeObjectScript("foo = {}");

        Object result = object.get("key");

        assertEquals(V8.getUndefined(), result);
        object.close();
    }

    @Test
    public void testGetIsFunction() {
        V8Object object = v8.executeObjectScript("foo = {key: function(){}}");

        Object result = object.get("key");

        assertTrue(result instanceof V8Function);
        object.close();
        ((Releasable) result).release();
    }

    @Test
    public void testGetV8Object() {
        v8.executeVoidScript("foo = {key: 'value'}");

        V8Object object = v8.getObject("foo");

        assertTrue(object.contains("key"));
        assertFalse(object.contains("noKey"));
        object.close();
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

        fooObject.close();
        barObject.close();
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
            fooObject.close();
            nested.close();
        }
    }

    /*** Get Array ***/
    @Test
    public void testGetV8ArrayV8Object() {
        v8.executeVoidScript("foo = {array : [1,2,3]}");

        V8Object object = v8.getObject("foo");
        V8Array array = object.getArray("array");

        assertEquals(3, array.length());
        assertEquals(1, array.getInteger(0));
        assertEquals(2, array.getInteger(1));
        assertEquals(3, array.getInteger(2));
        array.close();
        object.close();
    }

    /*** Get Primitives ***/
    @Test
    public void testGetIntegerV8Object() {
        v8.executeVoidScript("foo = {bar: 7}");

        V8Object foo = v8.getObject("foo");

        assertEquals(7, foo.getInteger("bar"));
        foo.close();
    }

    @Test
    public void testNestedInteger() {
        v8.executeVoidScript("foo = {bar: {key:6}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals(6, bar.getInteger("key"));
        foo.close();
        bar.close();
    }

    @Test
    public void testGetDoubleV8Object() {
        v8.executeVoidScript("foo = {bar: 7.1}");

        V8Object foo = v8.getObject("foo");

        assertEquals(7.1, foo.getDouble("bar"), 0.0001);
        foo.close();
    }

    @Test
    public void testNestedDouble() {
        v8.executeVoidScript("foo = {bar: {key:6.1}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals(6.1, bar.getDouble("key"), 0.0001);
        foo.close();
        bar.close();
    }

    @Test
    public void testGetBooleanV8Object() {
        v8.executeVoidScript("foo = {bar: false}");

        V8Object foo = v8.getObject("foo");

        assertFalse(foo.getBoolean("bar"));
        foo.close();
    }

    @Test
    public void testNestedBoolean() {
        v8.executeVoidScript("foo = {bar: {key:true}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertTrue(bar.getBoolean("key"));
        foo.close();
        bar.close();
    }

    @Test
    public void testGetStringV8Object() {
        v8.executeVoidScript("foo = {bar: 'string'}");

        V8Object foo = v8.getObject("foo");

        assertEquals("string", foo.getString("bar"));
        foo.close();
    }

    @Test
    public void testNestedString() {
        v8.executeVoidScript("foo = {bar: {key:'string'}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals("string", bar.getString("key"));
        foo.close();
        bar.close();
    }

    /*** Execute Object Function ***/
    @Test
    public void testObjectScript() {
        v8.executeVoidScript("function foo() { return { x : 7 }} ");

        V8Object result = v8.executeObjectFunction("foo", null);

        assertEquals(7, result.getInteger("x"));
        result.close();
    }

    /*** Add Primitives ***/
    @Test
    public void testAddString() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "world");

        assertEquals("world", v8Object.getString("hello"));
        v8Object.close();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetStringNotFound() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "string");

        try {
            v8Object.getString("goodbye");
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetStringNotString() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", 7);

        try {
            v8Object.getString("hello");
        } finally {
            v8Object.close();
        }
    }

    @Test
    public void testAddInt() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", 7);

        assertEquals(7, v8Object.getInteger("hello"));
        v8Object.close();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetIntegerNotFound() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", 7);

        try {
            v8Object.getInteger("goodbye");
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetIntegerNotInteger() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "string");

        try {
            v8Object.getInteger("hello");
        } finally {
            v8Object.close();
        }
    }

    @Test
    public void testAddDouble() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", 3.14159);

        assertEquals(3.14159, v8Object.getDouble("hello"), 0.000001);
        v8Object.close();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetDoubleNotFound() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", 7.7);

        try {
            v8Object.getDouble("goodbye");
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetDoubleNotDouble() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "string");

        try {
            v8Object.getDouble("hello");
        } finally {
            v8Object.close();
        }
    }

    @Test
    public void testAddBoolean() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", true);

        assertTrue(v8Object.getBoolean("hello"));
        v8Object.close();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetBooleanNotFound() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", false);

        try {
            v8Object.getBoolean("goodbye");
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetBooleanNotBoolean() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "string");

        try {
            v8Object.getBoolean("hello");
        } finally {
            v8Object.close();
        }
    }

    @Test
    public void testObjectChangedFromJS() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "world");
        v8.add("object", v8Object);

        v8.executeVoidScript("object.world = 'goodbye'");

        assertEquals("goodbye", v8Object.getString("world"));
        v8Object.close();
    }

    @Test
    public void testObjectChangedFromAPI() {
        v8.executeVoidScript("object = {world : 'goodbye'}");

        V8Object v8Object = v8.getObject("object").add("world", "hello");

        assertEquals("hello", v8Object.getString("world"));
        v8Object.close();
    }

    @Test
    public void testAddUndefined() {
        V8Object v8Object = new V8Object(v8);
        v8Object.addUndefined("foo");

        assertEquals("foo", v8Object.getKeys()[0]);
        assertEquals(UNDEFINED, v8Object.getType("foo"));
        v8Object.close();
    }

    @Test
    public void testAddNull() {
        V8Object v8Object = new V8Object(v8);
        v8Object.addNull("foo");

        assertEquals("foo", v8Object.getKeys()[0]);
        assertEquals(NULL, v8Object.getType("foo"));
        assertNull(v8Object.getObject("foo"));
        v8Object.close();
    }

    @Test
    public void testGetUndefined() {
        V8Object v8Object = v8.executeObjectScript("x = {a : undefined}; x;");

        assertEquals(UNDEFINED, v8Object.getType("a"));
        v8Object.close();
    }

    @Test
    public void testUndefinedNotReleased() {
        Undefined undefined = new V8Object.Undefined();
        undefined.close();

        assertFalse(undefined.isReleased());
    }

    /*** Add Object ***/
    @Test
    public void testAddObject() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", true);

        v8.add("foo", v8Object);

        V8Object foo = v8.getObject("foo");
        assertTrue(foo.getBoolean("hello"));
        foo.close();
        v8Object.close();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetObjectNotObject() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", 7);

        try {
            v8Object.getObject("hello");
        } finally {
            v8Object.close();
        }
    }

    @Test
    public void testAddObjectWithString() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", "world");

        v8.add("foo", v8Object);

        String result = v8.executeStringScript("foo.hello");
        assertEquals("world", result);
        v8Object.close();
    }

    @Test
    public void testAddObjectWithBoolean() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("boolean", false);

        v8.add("foo", v8Object);

        boolean result = v8.executeBooleanScript("foo.boolean");
        assertFalse(result);
        v8Object.close();
    }

    @Test
    public void testAddObjectWithInt() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("integer", 75);

        v8.add("foo", v8Object);

        int result = v8.executeIntegerScript("foo.integer");
        assertEquals(75, result);
        v8Object.close();
    }

    @Test
    public void testAddObjectWithDouble() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("double", 75.5);

        v8.add("foo", v8Object);

        double result = v8.executeDoubleScript("foo.double");
        assertEquals(75.5, result, 0.000001);
        v8Object.close();
    }

    @Test
    public void testAddObjectToObject() {
        V8Object nested = new V8Object(v8);
        nested.add("foo", "bar");
        V8Object v8Object = new V8Object(v8);
        v8Object.add("nested", nested);
        v8.add("foo", v8Object);

        String result = v8.executeStringScript("foo.nested.foo");

        assertEquals("bar", result);
        v8Object.close();
        nested.close();
    }

    /*** Add Array ***/
    @Test
    public void testAddArrayToObject() {
        V8Array array = new V8Array(v8);
        V8Object v8Object = new V8Object(v8);
        v8Object.add("array", array);
        v8.add("foo", v8Object);

        V8Array result = v8.executeArrayScript("foo.array");

        assertNotNull(result);
        v8Object.close();
        array.close();
        result.close();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetArrayNotArray() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("hello", 7);

        try {
            v8Object.getArray("hello");
        } finally {
            v8Object.close();
        }
    }

    /*** Undefined ***/
    @Test
    public void testUndefinedObjectProperty() {
        V8Object result = v8.getObject("object");

        assertTrue(result.isUndefined());
    }

    @Test
    public void testUndefinedEqual() {
        V8Object undefined1 = v8.getObject("foo");
        V8Object undefined2 = v8.getObject("bar");

        assertEquals(undefined1, undefined2);
    }

    @Test
    public void testUndefinedNotEquals() {
        V8Object undefined = v8.getObject("foo");
        V8Object object = new V8Object(v8);

        assertNotEquals(undefined, object);
        assertNotEquals(object, undefined);
        object.close();
    }

    @Test
    public void testStaticUndefined() {
        V8Object undefined = v8.getObject("foo");

        assertEquals(undefined, V8.getUndefined());
    }

    @Test
    public void testUndefinedHashCodeEquals() {
        V8Object undefined1 = v8.getObject("foo");
        V8Object undefined2 = v8.getObject("bar");

        assertEquals(undefined1.hashCode(), undefined2.hashCode());
    }

    @Test
    public void testUndefinedToString() {
        V8Object undefined = v8.getObject("object");

        assertEquals("undefined", undefined.toString());
    }

    @Test
    public void testUndefinedRelease() {
        V8Object undefined = v8.getObject("object");

        undefined.close();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddIntUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.add("foo", 7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddBooleanUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.add("foo", false);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddStringUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.add("foo", "bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddDoubleUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.add("foo", 7.7);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddObjectUndefined() {
        V8Object undefined = v8.getObject("object");
        V8Object object = new V8Object(v8);

        try {
            undefined.add("foo", object);
        } finally {
            object.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddArrayUndefined() {
        V8Object undefined = v8.getObject("object");
        V8Array array = new V8Array(v8);

        try {
            undefined.add("foo", array);
        } finally {
            array.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddUndefinedUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.addUndefined("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContainsUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.contains("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteIntFunctionUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.executeIntegerFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteBooleanFunctionUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.executeBooleanFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteDoubleFunctionUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.executeDoubleFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteStringFunctionUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.executeStringFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteObjectFunctionUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.executeObjectFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteArrayFunctionUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.executeArrayFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteVoidFunctionUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.executeVoidFunction("foo", null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetIntegerUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getInteger("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetBooleanUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getBoolean("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDoubleUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getDouble("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStringUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getString("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetObjectUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getObject("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetArrayUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getArray("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetKeysUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getKeys();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetTypeUndefined() {
        V8Object undefined = v8.getObject("object");

        undefined.getType("bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetPrototype() {
        V8Object undefined = v8.getObject("object");
        V8Object prototype = new V8Object(v8);

        try {
            undefined.setPrototype(prototype);
        } finally {
            prototype.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterJavaMethod() {
        V8Object undefined = v8.getObject("object");

        undefined.registerJavaMethod(mock(JavaCallback.class), "name");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterVoidJavaMethod() {
        V8Object undefined = v8.getObject("object");

        undefined.registerJavaMethod(mock(JavaVoidCallback.class), "name");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterAnyJavaMethod() {
        V8Object undefined = v8.getObject("object");

        undefined.registerJavaMethod(new Object(), "toString", "toString", new Class<?>[0]);
    }

    @Test
    public void testAddUndefinedAsObject() {
        V8Object object = new V8Object(v8);
        object.add("foo", V8.getUndefined());

        assertEquals(V8.getUndefined(), object.getObject("foo"));
        object.close();
    }

    @Test
    public void testAddUndefinedIsUndefined() {
        V8Object object = new V8Object(v8);
        object.add("foo", V8.getUndefined());

        assertEquals(UNDEFINED, object.getType("foo"));
        object.close();
    }

    /*** Null ***/
    @Test
    public void testStringIsNull() {
        v8.add("nullString", (V8Object) null);

        assertNull(v8.getString("nullString"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTypeNull() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.getType(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContainsNull() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.contains(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNullKey() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.get(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNullKey_Integer() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.getInteger(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNullKey_String() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.getString(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNullKey_Double() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.getDouble(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNullKey_Boolean() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.getBoolean(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNullKey_Object() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.getObject(null);
        } finally {
            v8Object.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNullKey_Array() {
        V8Object v8Object = new V8Object(v8);
        try {
            v8Object.getArray(null);
        } finally {
            v8Object.close();
        }
    }

    @Test
    public void testStringScript() {
        assertNull(v8.executeStringScript("null;"));
    }

    @Test
    public void testIsNull() {
        V8Object v8Object = v8.executeObjectScript("x = {a : null}; x;");

        assertEquals(NULL, v8Object.getType("a"));
        v8Object.close();
    }

    @Test
    public void testGetNull() {
        V8Object v8Object = v8.executeObjectScript("x = {a : null}; x;");

        assertNull(v8Object.getObject("a"));
        v8Object.close();
    }

    @Test
    public void testAddNullAsObject() {
        V8Object object = new V8Object(v8);
        object.add("foo", (V8Object) null);

        assertNull(object.getObject("foo"));
        object.close();
    }

    @Test
    public void testAddNullAsString() {
        V8Object object = new V8Object(v8);
        object.add("foo", (String) null);

        assertNull(object.getObject("foo"));
        object.close();
    }

    @Test
    public void testAddNullAsArray() {
        V8Object object = new V8Object(v8);
        object.add("foo", (V8Array) null);

        assertNull(object.getArray("foo"));
        object.close();
    }

    /*** Test Types ***/
    @Test
    public void testGetTypeInt() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("key", 1);

        assertEquals(INTEGER, v8Object.getType("key"));
        v8Object.close();
    }

    @Test
    public void testGetTypeDouble() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("key", 1.1);

        assertEquals(DOUBLE, v8Object.getType("key"));
        v8Object.close();
    }

    @Test
    public void testGetTypeString() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("key", "String");

        assertEquals(STRING, v8Object.getType("key"));
        v8Object.close();
    }

    @Test
    public void testGetTypeBoolean() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("key", false);

        assertEquals(BOOLEAN, v8Object.getType("key"));
        v8Object.close();
    }

    @Test
    public void testGetTypeArray() {
        V8Array value = new V8Array(v8);
        V8Object v8Object = new V8Object(v8);
        v8Object.add("key", value);

        assertEquals(V8_ARRAY, v8Object.getType("key"));
        v8Object.close();
        value.close();
    }

    @Test
    public void testGetTypeObject() {
        V8Object value = new V8Object(v8);
        V8Object v8Object = new V8Object(v8);
        v8Object.add("key", value);

        assertEquals(V8_OBJECT, v8Object.getType("key"));
        v8Object.close();
        value.close();
    }

    @Test
    public void testGetTypeFunction() {
        v8.executeVoidScript("var foo = function() {};");
        V8Object function = v8.getObject("foo");
        V8Object v8Object = new V8Object(v8);
        v8Object.add("key", function);

        int type = v8Object.getType("key");

        assertEquals(V8_FUNCTION, type);
        v8Object.close();
        function.close();
    }

    @Test
    public void testGetKeysOnObject() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("integer", 1).add("double", 1.1).add("boolean", true)
                        .add("string", "hello, world!");

        String[] keys = v8Object.getKeys();

        assertEquals(4, keys.length);
        V8Test.arrayContains(keys, "integer", "double", "boolean", "string");
        v8Object.close();
    }

    @Test
    public void testGetTypeKeyDoesNotExist() {
        V8Object v8Object = new V8Object(v8);

        int result = v8Object.getType("key");

        assertEquals(UNDEFINED, result);
        v8Object.close();
    }

    @Test
    public void testUnaccessibleMethod() {
        final boolean[] called = new boolean[] { false };
        Runnable r = new Runnable() {

            @Override
            public void run() {
                called[0] = true;
            }
        };
        v8.registerJavaMethod(r, "run", "run", new Class<?>[0]);

        v8.executeVoidFunction("run", null);

        assertTrue(called[0]);
    }

    /*** Manipulate Prototypes ***/
    @Test
    public void testSetPrototypeOfObject() {
        v8.executeVoidScript("function Mammal(){}; Mammal.prototype.breathe=function(){return 'breathe';};");
        V8Object mammal = v8.executeObjectScript("new Mammal();");
        V8Object cat = new V8Object(v8);
        cat.setPrototype(mammal);

        v8.add("cat", cat);

        assertTrue(v8.executeBooleanScript("cat instanceof Mammal"));
        assertEquals("breathe", cat.executeStringFunction("breathe", null));
        cat.close();
        mammal.close();
    }

    @Test
    public void testGetKeysDoesNotIncludePrototypeKeys() {
        v8.executeVoidScript("function Mammal(){}; Mammal.prototype.breathe=function(){return 'breathe';};");
        V8Object mammal = v8.executeObjectScript("new Mammal();");
        V8Object cat = new V8Object(v8);
        cat.setPrototype(mammal);

        String[] keys = cat.getKeys();
        Object object = cat.get("breathe");
        assertTrue(object instanceof V8Function);

        assertEquals(0, keys.length);
        cat.close();
        mammal.close();
        ((Releasable) object).release();
    }

    @Test
    public void testChangePrototypeAfterCreation() {
        v8.executeVoidScript("function Mammal(){};");
        V8Object mammal = v8.executeObjectScript("new Mammal();");
        V8Object cat = new V8Object(v8);
        v8.add("cat", cat);
        v8.add("mammal", mammal);
        assertFalse(v8.executeBooleanScript("cat instanceof Mammal"));

        cat.setPrototype(mammal);

        assertTrue(v8.executeBooleanScript("cat instanceof Mammal"));
        assertTrue(v8.executeBooleanScript("cat.__proto__ === mammal"));
        cat.close();
        mammal.close();
    }

    @Test
    public void testChangePrototypePropertiesAfterCreation() {
        v8.executeVoidScript("function Mammal(){};");
        V8Object mammal = v8.executeObjectScript("new Mammal();");
        V8Object cat = new V8Object(v8);
        cat.setPrototype(mammal);
        v8.add("cat", cat);
        assertFalse(v8.executeBooleanScript("'breathe' in cat"));

        v8.executeVoidScript("Mammal.prototype.breathe=function(){return 'breathe';};");

        assertTrue(v8.executeBooleanScript("'breathe' in cat"));
        cat.close();
        mammal.close();
    }

    /*** Equals ***/
    @Test
    public void testEquals() {
        v8.executeVoidScript("o = {}");
        V8Object o1 = v8.executeObjectScript("o");
        V8Object o2 = v8.executeObjectScript("o");

        assertEquals(o1, o2);
        assertNotSame(o1, o2);

        o1.close();
        o2.close();
    }

    @Test
    public void testEqualsPassByReference() {
        v8.executeVoidScript("o = {}");
        v8.executeVoidScript("function ident(x){return x;}");
        V8Object o1 = v8.executeObjectScript("o");
        V8Array parameters = new V8Array(v8);
        parameters.push(o1);
        V8Object o2 = v8.executeObjectFunction("ident", parameters);

        assertEquals(o1, o2);
        assertNotSame(o1, o2);

        o1.close();
        o2.close();
        parameters.close();
    }

    @Test
    public void testEqualsDifferenceReference() {
        v8.executeVoidScript("a = {}; b=a;");
        v8.executeVoidScript("function ident(x){return x;}");
        V8Object o1 = v8.executeObjectScript("a");
        V8Object o2 = v8.executeObjectScript("b");

        assertEquals(o1, o2);
        assertNotSame(o1, o2);

        o1.close();
        o2.close();
    }

    @Test
    public void testEqualHash() {
        v8.executeVoidScript("o = {}");
        V8Object o1 = v8.executeObjectScript("o");
        V8Object o2 = v8.executeObjectScript("o");

        assertEquals(o1.hashCode(), o2.hashCode());

        o1.close();
        o2.close();
    }

    @Test
    public void testNotEquals() {
        v8.executeVoidScript("a = {}; b = {};");
        V8Object o1 = v8.executeObjectScript("a");
        V8Object o2 = v8.executeObjectScript("b");

        assertNotEquals(o1, o2);

        o1.close();
        o2.close();
    }

    @Test
    public void testNotEqualsNull() {
        v8.executeVoidScript("a = {};");
        V8Object o1 = v8.executeObjectScript("a");

        assertNotEquals(o1, null);

        o1.close();
    }

    @Test
    public void testNotEqualsNull2() {
        v8.executeVoidScript("a = {};");
        V8Object o1 = v8.executeObjectScript("a");

        assertNotEquals(null, o1);

        o1.close();
    }

    @Test
    public void testNotEqualHash() {
        v8.executeVoidScript("a = {}; b = {};");
        V8Object o1 = v8.executeObjectScript("a");
        V8Object o2 = v8.executeObjectScript("b");

        assertNotEquals(o1.hashCode(), o2.hashCode());

        o1.close();
        o2.close();
    }

    @Test
    public void testHashStable() {
        V8Object a = v8.executeObjectScript("a = []; a");
        int hash1 = a.hashCode();
        int hash2 = a.add("1", true).add("2", false).add("3", 123).hashCode();

        assertEquals(hash1, hash2);
        a.close();
    }

    @Test
    public void testFunctionToString() {
        String result = "function (){\n  1+2;\n}";
        String script = "var func = " + result + "\n"
                + "func;\n";
        V8Object function = v8.executeObjectScript(script);

        assertEquals(result, function.toString());
        function.close();
    }

    @Test
    public void testDateToString() {
        V8Object a = v8.executeObjectScript("new Date(2014, 9, 1, 10, 0, 0, 0)");

        assertTrue(a.toString().startsWith("Wed Oct 01 2014 10:00:00"));
        a.close();
    }

    @Test
    public void testArrayToString() {
        V8Object a = v8.executeObjectScript("x = [1,2,3]; x;");

        assertEquals("1,2,3", a.toString());
        a.close();
    }

    @Test
    public void testToString() {
        V8Object a = v8.executeObjectScript("x = {a:'b'}; x;");

        assertEquals("[object Object]", a.toString());
        a.close();
    }

    public void runMe(final Object o) {
        assertNotNull(o.toString());
    }

    @Test
    public void testToStringInCallback() {
        V8Object a = v8.executeObjectScript("x = [1, 'test', false]; x;");
        v8.registerJavaMethod(this, "runMe", "runMe", new Class<?>[] { Object.class });

        v8.executeVoidScript("runMe(x);");
        a.close();
    }

    @Test
    public void testV8ObjectTwinEqual() {
        V8Object v8Object = new V8Object(v8);

        V8Object twin = v8Object.twin();

        assertNotSame(v8Object, twin);
        assertTrue(v8Object.equals(twin));
        assertTrue(twin.equals(v8Object));
        v8Object.close();
        twin.close();
    }

    @Test
    public void testV8ObjectTwinStrictEquals() {
        V8Object v8Object = new V8Object(v8);

        V8Object twin = v8Object.twin();

        assertNotSame(v8Object, twin);
        assertTrue(v8Object.strictEquals(twin));
        assertTrue(twin.strictEquals(v8Object));
        v8Object.close();
        twin.close();
    }

    @Test
    public void testV8ObjectTwinSameHashCode() {
        V8Object v8Object = new V8Object(v8);

        V8Object twin = v8Object.twin();

        assertEquals(v8Object.hashCode(), twin.hashCode());
        v8Object.close();
        twin.close();
    }

    @Test
    public void testTwinIsObject() {
        V8Object v8Object = new V8Object(v8);

        V8Object twin = v8Object.twin();

        assertTrue(twin instanceof V8Object);
        v8Object.close();
        twin.close();
    }

    @Test
    public void testTwinIsArray() {
        V8Array v8Object = new V8Array(v8);

        V8Array twin = v8Object.twin();

        assertTrue(twin instanceof V8Array);
        v8Object.close();
        twin.close();
    }

    @Test
    public void testTwinIsArrayBuffer() {
        V8ArrayBuffer arrayBuffer = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(100);  buf;");

        V8ArrayBuffer twin = arrayBuffer.twin();

        assertTrue(twin instanceof V8ArrayBuffer);
        arrayBuffer.close();
        twin.close();
    }

    @Test
    public void testArrayBufferTwinHasSameBackingStore() {
        V8ArrayBuffer arrayBuffer = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(100);  buf;");

        V8ArrayBuffer twin = arrayBuffer.twin();

        assertEquals(twin.byteBuffer, arrayBuffer.byteBuffer);
        arrayBuffer.close();
        twin.close();
    }

    @Test
    public void testTwinIsFunction() {
        v8.executeVoidScript("function add(x, y) {return x+y;}");
        V8Function v8Object = (V8Function) v8.getObject("add");

        V8Function twin = v8Object.twin();

        assertTrue(twin instanceof V8Function);
        v8Object.close();
        twin.close();
    }

    @Test
    public void testTwinIsUndefined() {
        V8Object v8Object = (V8Object) V8.getUndefined();

        V8Value twin = v8Object.twin();

        assertTrue(twin.isUndefined());
        v8Object.close();
        twin.close();
    }

    @Test
    public void testReleaseTwinDoesNotReleaseOriginal() {
        V8Object v8Object = new V8Object(v8);
        V8Value twin = v8Object.twin();

        twin.close();

        assertFalse(v8Object.isReleased());
        v8Object.close();
    }

    @Test
    public void testReleaseObjectDoesNotReleaseTwin() {
        V8Object v8Object = new V8Object(v8);
        V8Value twin = v8Object.twin();

        v8Object.close();

        assertFalse(twin.isReleased());
        twin.close();
    }

    @Test
    public void testTwinMimicsObject() {
        V8Object v8Object = new V8Object(v8);
        V8Object twin = v8Object.twin();

        v8Object.add("foo", "bar");

        assertEquals("bar", twin.getString("foo"));
        v8Object.close();
        twin.close();
    }

    @Test
    public void testUnicodeValue() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("foo", "\uD83C\uDF89");

        assertEquals("\uD83C\uDF89", v8Object.get("foo"));

        v8Object.close();
    }

    @Test
    public void testUnicodeValue_Char() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("foo", "\uD83C\uDF89");

        assertEquals("🎉", v8Object.get("foo"));

        v8Object.close();
    }

    @Test
    public void testUnicodeValue_SetChar() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("foo", "🎉");

        assertEquals("\uD83C\uDF89", v8Object.get("foo"));

        v8Object.close();
    }

    @Test
    public void testUnicodeKey() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("\uD83C\uDF89", "foo");

        assertEquals("foo", v8Object.get("\uD83C\uDF89"));

        v8Object.close();
    }

    @Test
    public void testUnicodeKeyWithChar() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("\uD83C\uDF89", "foo");

        assertEquals("foo", v8Object.get("🎉"));

        v8Object.close();
    }

    @Test
    public void testUnicodeKeyGetKeys() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("\uD83C\uDF89", "foo");
        assertEquals("🎉", v8Object.getKeys()[0]);

        v8Object.close();
    }

    @Test
    public void testSetUnicodeKeyWithChar() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("🎉", "foo");

        assertEquals("foo", v8Object.get("\uD83C\uDF89"));

        v8Object.close();
    }

    @Test
    public void testGetType_V8Object() {
        V8Object object = new V8Object(v8);

        assertEquals(V8Value.V8_OBJECT, object.getV8Type());
        object.close();
    }

    @Test
    public void testGetType_V8Array() {
        V8Array array = new V8Array(v8);

        assertEquals(V8Value.V8_ARRAY, array.getV8Type());
        array.close();
    }

    @Test
    public void testGetType_V8Function() {
        V8Function function = new V8Function(v8, mock(JavaCallback.class));

        assertEquals(V8Value.V8_FUNCTION, function.getV8Type());
        function.close();
    }

    @Test
    public void testGetType_TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 64);
        V8Array array = new V8TypedArray(v8, buffer, V8Value.INT_8_ARRAY, 0, 8);

        assertEquals(V8Value.V8_TYPED_ARRAY, array.getV8Type());
        array.close();
        buffer.close();
    }

    @Test
    public void testGetType_Undefined() {
        assertEquals(V8Value.UNDEFINED, V8.getUndefined().getV8Type());
    }

    @SuppressWarnings("resource")
    @Test
    public void testWeakReferenceReducesObjectCount() {
        new V8Object(v8).setWeak();

        assertEquals(0, v8.getObjectReferenceCount());
    }

    @SuppressWarnings("resource")
    @Test
    public void testSetWeakMakesObjectWeak() {
        V8Value object = new V8Object(v8).setWeak();

        assertTrue(object.isWeak());
    }

    @SuppressWarnings("resource")
    @Test
    public void testClearWeakMakesObjectWeak() {
        V8Value object = new V8Object(v8).setWeak().clearWeak();

        assertFalse(object.isWeak());
        object.close();
    }

    @Test
    public void testConstructorNameFunction() {
        v8.executeVoidScript("function Foo() {} var foo = new Foo();");
        V8Object object = (V8Object) v8.get("foo");

        assertEquals(object.getConstructorName(), "Foo");

        object.close();
    }

    @Test
    public void testConstructorNameObject() {
        v8.executeVoidScript("var foo = new Object()");
        V8Object object = (V8Object) v8.get("foo");

        assertEquals(object.getConstructorName(), "Object");

        object.close();
    }

    @Test
    public void testConstructorNameObject_2() {
        v8.executeVoidScript("var foo = {}");
        V8Object object = (V8Object) v8.get("foo");

        assertEquals(object.getConstructorName(), "Object");

        object.close();
    }

    @Test
    public void testConstructorNameArray() {
        v8.executeVoidScript("var foo = []");
        V8Object object = (V8Object) v8.get("foo");

        assertEquals(object.getConstructorName(), "Array");

        object.close();
    }

    @Test
    public void testConstructorNameTypedArray() {
        v8.executeVoidScript("var foo = new Uint8Array()");
        V8Object object = (V8Object) v8.get("foo");

        assertEquals(object.getConstructorName(), "Uint8Array");

        object.close();
    }

    @Test
    public void testConstructorNameArrayBuffer() {
        v8.executeVoidScript("var foo = new ArrayBuffer()");
        V8ArrayBuffer object = (V8ArrayBuffer) v8.get("foo");

        assertEquals(object.getConstructorName(), "ArrayBuffer");

        object.close();
    }

    @Test
    public void testConstructorNameMap() {
        v8.executeVoidScript("var foo = new Map()");
        V8Object object = (V8Object) v8.get("foo");

        assertEquals(object.getConstructorName(), "Map");

        object.close();
    }

}
