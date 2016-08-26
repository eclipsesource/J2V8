/*******************************************************************************
 * Copyright (c) 2016 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8TypedArraysTest {

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

    @Test
    public void testArrayBuffer() {
        V8Value result = (V8Value) v8.executeScript("var buf = new ArrayBuffer(100); buf;");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testInt8Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Int8Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testInt16Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Int16Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testUint8Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Uint8Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testUint16Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Uint16Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testInt32Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Int32Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testUInt32Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Uint32Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testFloat32Array() {
        V8Value result = (V8Value) v8.executeScript("var floats = new Float32Array(); floats");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testFloat64Array() {
        V8Value result = (V8Value) v8.executeScript("var floats = new Float64Array(); floats");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testTypedArrayLength() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int32Array(buf); ints[0] = 7; ints");

        assertEquals(1, result.length());
        result.release();
    }

    @Test
    public void testGetTypedArrayValue() {
        int result = v8.executeIntegerScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints[0]");

        assertEquals(7, result);
    }

    @Test
    public void testGetTypedArrayIntValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(7, result.get(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayUsingKeys() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(7, result.getInteger("0"));
        result.release();
    }

    @Test
    public void testGetTypedArrayIntType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(V8Value.INTEGER, result.getType(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayFloatType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var floats = new Float32Array(buf); floats[0] = 7.7; floats");

        assertEquals(V8Value.DOUBLE, result.getType(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayIntArrayType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(V8Value.INT_16_ARRAY, result.getType());
        result.release();
    }

    @Test
    public void testGetTypedArrayUInt8Type() {
        v8.registerJavaMethod(new JavaVoidCallback() {

            @Override
            public void invoke(final V8Object receiver, final V8Array parameters) {
                assertEquals(V8Value.V8_TYPED_ARRAY, parameters.getType(0));
            }
        }, "javaMethod");
        v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Uint8ClampedArray(buf); ints[0] = 7; javaMethod(ints);");
    }

    @Test
    public void testAccessSignedValueFromUnsignedByte_Greater128() {
        V8TypedArray array = (V8TypedArray) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Uint8ClampedArray(buf); bytes[0] = 240; bytes[1] = 7; bytes");
        ByteBuffer byteBuffer = array.getByteBuffer();

        short result = (short) (byteBuffer.get() & 0xFF);

        assertEquals(240, result);
        array.release();
    }

    @Test
    public void testAccessSignedValueFromUnsignedByte_Less128() {
        V8TypedArray array = (V8TypedArray) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Uint8Array(buf); bytes[0] = 20; bytes[1] = 7; bytes");
        ByteBuffer byteBuffer = array.getByteBuffer();

        short result = (short) (byteBuffer.get() & 0xFF);

        assertEquals(20, result);
        array.release();
    }

    @Test
    public void testInt8IsByteArray() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Int8Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.BYTE, result.getType());
        result.release();
    }

    @Test
    public void testInt8_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Int8Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.INT_8_ARRAY, result.getType());
        result.release();
    }

    @Test
    public void testUint8Clamped_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Uint8ClampedArray(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.UNSIGNED_INT_8_CLAMPED_ARRAY, result.getType());
        result.release();
    }

    @Test
    public void testUint8Array_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Uint8Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.UNSIGNED_INT_8_ARRAY, result.getType());
        result.release();
    }

    @Test
    public void testInt16Array_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Int16Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.INT_16_ARRAY, result.getType());
        assertEquals(4, result.length());
        result.release();
    }

    @Test
    public void testUnsignedInt16Array_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Uint16Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.UNSIGNED_INT_16_ARRAY, result.getType());
        assertEquals(4, result.length());
        result.release();
    }

    @Test
    public void testInt32Array_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Int32Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.INT_32_ARRAY, result.getType());
        assertEquals(2, result.length());
        result.release();
    }

    @Test
    public void testUInt32Array_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Uint32Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.UNSIGNED_INT_32_ARRAY, result.getType());
        assertEquals(2, result.length());
        result.release();
    }

    @Test
    public void testFloat32Array_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Float32Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.FLOAT_32_ARRAY, result.getType());
        assertEquals(2, result.length());
        result.release();
    }

    @Test
    public void testFloat64Array_GetType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Float64Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.FLOAT_64_ARRAY, result.getType());
        assertEquals(1, result.length());
        result.release();
    }

    @Test
    public void testGetBytesFromTypedArray() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var bytes = new Int8Array(buf); bytes[0] = 1; bytes[1] = 256; bytes");

        assertEquals(V8Value.BYTE, result.getType());
        assertEquals(1, result.getByte(0));
        assertEquals(0, result.getByte(1));
        result.release();
    }

    @Test
    public void testGetTypedArrayFloatArrayType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var floats = new Float32Array(buf); floats[0] = 7.7; floats[1] = 7; floats");

        assertEquals(V8Value.FLOAT_32_ARRAY, result.getType());
        result.release();
    }

    @Test
    public void testGetTypedArrayType32BitValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int32Array(buf); ints[0] = 255; ints");

        assertEquals(255, result.get(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayType16BitValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 255; ints");

        assertEquals(255, result.get(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayType32BitFloatValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var floats = new Float32Array(buf); floats[0] = 255.5; floats");

        assertEquals(255.5, result.getDouble(0), 0.00001);
        result.release();
    }

    @Test
    public void testGetTypedArrayType64BitFloatValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var floats = new Float64Array(buf); floats[0] = 255.5; floats");

        assertEquals(255.5, result.getDouble(0), 0.00001);
        result.release();
    }

    @Test
    public void testGetTypedRangeArrayValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(100); var ints = new Int32Array(buf); for(var i = 0; i < 25; i++) {ints[i] = i;}; ints");

        assertEquals(25, result.length());
        int[] ints = result.getIntegers(0, 25);
        for (int i = 0; i < ints.length; i++) {
            assertEquals(i, ints[i]);
        }
        result.release();
    }

    @Test
    public void testGetTypedArrayGetKeys() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints[0] = 255; ints[1] = 17; ints");

        assertEquals(2, result.getKeys().length);
        assertEquals("0", result.getKeys()[0]);
        assertEquals("1", result.getKeys()[1]);
        result.release();
    }

    @Test
    public void testAddTypedArrayInteger() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        array.add("0", 7);
        array.add("1", 17);

        assertEquals(2, array.length());
        assertEquals(7, array.getInteger(0));
        assertEquals(17, array.getInteger(1));
        array.release();
    }

    @Test
    public void testAddTypedArrayFloat() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var floats = new Float32Array(buf); floats");

        array.add("0", 7.7);
        array.add("1", 17.7);

        assertEquals(2, array.length());
        assertEquals(7.7, array.getDouble(0), 0.000001);
        assertEquals(17.7, array.getDouble(1), 0.000001);
        array.release();
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushIntToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushIntToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushFloatToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7.7);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushFloatToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7.7);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushBooleanToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(true);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushBooleanToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(true);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushStringToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push("foo");
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushStringToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push("foo");
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushUndefinedToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.pushUndefined();
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushUndefinedToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.pushUndefined();
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushNullToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push((V8Object) null);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushNullToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push((V8Object) null);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushV8ObjectToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Object obj = new V8Object(v8);

        try {
            array.push(obj);
        } finally {
            array.release();
            obj.release();
        }
    }

    @Test
    public void testCannotPushV8ObjectToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Object obj = new V8Object(v8);

        try {
            array.push(obj);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
            obj.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushV8ArrayToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Array obj = new V8Array(v8);

        try {
            array.push(obj);
        } finally {
            array.release();
            obj.release();
        }
    }

    @Test
    public void testCannotPushV8ArrayToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Array obj = new V8Array(v8);

        try {
            array.push(obj);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
            obj.release();
        }
        fail("Expected failure");
    }

    @Test
    public void testIntArrayLength() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var ints = new Int32Array(buf);\n");
        int arrayLength = v8.executeIntegerScript("ints.length;"); // 4 bytes for each element

        assertEquals(25, arrayLength);
    }

    @Test
    public void testIntArrayByteLength() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var ints = new Int32Array(buf);\n");
        int arrayLength = v8.executeIntegerScript("ints.byteLength;"); // 4 bytes for each element

        assertEquals(100, arrayLength);
    }

    @Test
    public void testGetTypedArray() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n");

        int type = v8.getType("intsArray");

        assertEquals(V8Value.V8_TYPED_ARRAY, type);
    }

    @Test
    public void testGetTypedArray_IntegerType() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }

    @Test
    public void testGetTypedArray_DoubleType() {
        v8.executeVoidScript("var buf = new ArrayBuffer(80);\n"
                + "var doublesArray = new Float64Array(buf);");

        V8Array doublesArray = (V8Array) v8.get("doublesArray");

        assertEquals(V8Value.DOUBLE, doublesArray.getType());
        doublesArray.release();
    }

    @Test
    public void testGetTypedArray_IntegerTypeAfterNull() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = null;\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }

    @Test
    public void testGetTypedArray_IntegerTypeAfterUndefined() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = undefined;\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }

    @Test
    public void testGetTypedArray_IntegerTypeAfterFloat() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = 7.4;\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }

    @Test
    public void testGetIntBufferFromInt32Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);

        IntBuffer intBuffer = v8Int32Array.getByteBuffer().asIntBuffer();

        assertEquals(intBuffer, buffer.getBackingStore().asIntBuffer());
        buffer.release();
        v8Int32Array.release();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidOffset_Int32Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        try {
            new V8TypedArray(v8, buffer, V8Value.INTEGER, 1, 2);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLength_Int32Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        try {
            new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 3);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLengthNegative_Int32Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        try {
            new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, -1);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLengthWithOffset_Int32Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 12);
        try {
            new V8TypedArray(v8, buffer, V8Value.INTEGER, 4, 3);
        } finally {
            buffer.release();
        }
    }

    @Test
    public void testGetArrayBuffer_Int32Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);

        V8ArrayBuffer result = v8Int32Array.getBuffer();

        assertEquals(result, buffer);
        result.release();
        buffer.release();
        v8Int32Array.release();
    }

    @Test
    public void testUseAccessedArrayBuffer_Int32Array() {
        V8TypedArray array = (V8TypedArray) v8.executeScript("\n"
                + "var buffer = new ArrayBuffer(8);"
                + "var array = new Int32Array(buffer);"
                + "array[0] = 1; array[1] = 7;"
                + "array;");

        V8ArrayBuffer buffer = array.getBuffer();

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        assertEquals(1, intBuffer.get(0));
        assertEquals(7, intBuffer.get(1));
        buffer.release();
        array.release();
    }

    @Test
    public void testCreateIntegerTypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);

        v8Int32Array.add("0", 7);
        v8Int32Array.add("1", 8);

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        assertEquals(7, intBuffer.get());
        assertEquals(8, intBuffer.get());
        buffer.release();
        v8Int32Array.release();
    }

    @Test
    public void testUpdateInt32TypedArrayInJavaScript() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);
        v8.add("v8Int32Array", v8Int32Array);

        v8.executeVoidScript("v8Int32Array[0] = 7; v8Int32Array[1] = 9;");

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        assertEquals(7, intBuffer.get());
        assertEquals(9, intBuffer.get());
        buffer.release();
        v8Int32Array.release();
    }

    @Test
    public void testAccessInt32TypedArrayInJavaScript() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);
        v8.add("v8Int32Array", v8Int32Array);

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        intBuffer.put(4);
        intBuffer.put(8);

        assertEquals(4, v8.executeIntegerScript("v8Int32Array[0];"));
        assertEquals(8, v8.executeIntegerScript("v8Int32Array[1];"));
        buffer.release();
        v8Int32Array.release();
    }

    @Test
    public void testWriteInt32TypedArrayFromArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);
        v8.add("v8Int32Array", v8Int32Array);

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        intBuffer.put(4);
        intBuffer.put(8);

        assertEquals(4, v8Int32Array.get(0));
        assertEquals(8, v8Int32Array.get(1));
        buffer.release();
        v8Int32Array.release();
    }

    @Test
    public void testInt32TypedArray_BufferReleased() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);
        buffer.release();
        v8.add("v8Int32Array", v8Int32Array);

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        intBuffer.put(4);
        intBuffer.put(8);

        assertEquals(4, v8Int32Array.get(0));
        assertEquals(8, v8Int32Array.get(1));
        v8Int32Array.release();
    }

    @Test
    public void testInt32TypedArray_Length() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);
        buffer.release();
        v8.add("v8Int32Array", v8Int32Array);

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        intBuffer.put(4);
        intBuffer.put(8);

        assertEquals(2, v8Int32Array.length());
        v8Int32Array.release();
    }

    @Test
    public void testInt32TypedArray_CustomLength() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 1);
        buffer.release();
        v8.add("v8Int32Array", v8Int32Array);

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        intBuffer.put(4);
        intBuffer.put(8);

        assertEquals(1, v8Int32Array.length());
        v8Int32Array.release();
    }

    @Test
    public void testInt32TypedArray_CustomOffset() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 4, 1);
        buffer.release();
        v8.add("v8Int32Array", v8Int32Array);

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        intBuffer.put(4);
        intBuffer.put(8);

        assertEquals(1, v8Int32Array.length());
        assertEquals(8, v8Int32Array.get(0));
        v8Int32Array.release();
    }

    @Test
    public void testInt32TypedArray_Twin() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);
        V8Array twinArray = v8Int32Array.twin();

        assertTrue(twinArray instanceof V8TypedArray);
        assertEquals(v8Int32Array, twinArray);
        v8Int32Array.release();
        twinArray.release();
        buffer.release();
    }

    @Test
    public void testInt32TypedArray_TwinHasSameValues() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int32Array1 = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);
        V8TypedArray v8Int32Array2 = (V8TypedArray) v8Int32Array1.twin();

        v8Int32Array1.add("0", 7);
        v8Int32Array1.add("1", 8);

        assertEquals(7, v8Int32Array2.get(0));
        assertEquals(8, v8Int32Array2.get(1));
        v8Int32Array1.release();
        v8Int32Array2.release();
        buffer.release();
    }

    @Test
    public void testGetInt32TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        V8Array array = (V8Array) v8.executeScript("new Int32Array(buf);");

        assertTrue(array instanceof V8TypedArray);
        assertEquals(V8Value.INTEGER, array.getType());
        array.release();
        buffer.release();
    }

    @Test
    public void testCreateV8Int32ArrayFromJSArrayBuffer() {
        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.executeScript(""
                + "var buffer = new ArrayBuffer(8)\n"
                + "var array = new Int32Array(buffer);\n"
                + "array[0] = 7; array[1] = 9;\n"
                + "buffer;");

        V8TypedArray array = new V8TypedArray(v8, buffer, V8Value.INTEGER, 0, 2);

        assertEquals(7, array.get(0));
        assertEquals(9, array.get(1));
        array.release();
        buffer.release();
    }

    @Test
    public void testGetByteBuffer_Int8Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);

        ByteBuffer intBuffer = v8Int8Array.getByteBuffer();

        assertEquals(intBuffer, buffer.getBackingStore());
        buffer.release();
        v8Int8Array.release();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLength_Int8Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        try {
            new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 9);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLengthNegative_Int8Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        try {
            new V8TypedArray(v8, buffer, V8Value.BYTE, 0, -1);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLengthWithOffset_Int8Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 12);
        try {
            new V8TypedArray(v8, buffer, V8Value.BYTE, 4, 11);
        } finally {
            buffer.release();
        }
    }

    @Test
    public void testGetArrayBuffer_Int8Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);

        V8ArrayBuffer result = v8Int8Array.getBuffer();

        assertEquals(result, buffer);
        result.release();
        buffer.release();
        v8Int8Array.release();
    }

    @Test
    public void testUseAccessedArrayBuffer_Int8Array() {
        V8TypedArray array = (V8TypedArray) v8.executeScript("\n"
                + "var buffer = new ArrayBuffer(8);"
                + "var array = new Int8Array(buffer);"
                + "array[0] = 1; array[1] = 7;"
                + "array;");

        V8ArrayBuffer buffer = array.getBuffer();

        ByteBuffer byteBuffer = buffer.getBackingStore();
        assertEquals(1, byteBuffer.get(0));
        assertEquals(7, byteBuffer.get(1));
        buffer.release();
        array.release();
    }

    @Test
    public void testCreateByteTypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 8);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateInt8TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.INT_8_ARRAY, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 8);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        assertEquals(V8Value.INT_8_ARRAY, typedArray.getType());
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateUInt8TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.UNSIGNED_INT_8_ARRAY, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 8);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        assertEquals(V8Value.UNSIGNED_INT_8_ARRAY, typedArray.getType());
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateUInt8ClampedTypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.UNSIGNED_INT_8_CLAMPED_ARRAY, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 270);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        assertEquals(V8Value.UNSIGNED_INT_8_CLAMPED_ARRAY, typedArray.getType());
        assertEquals(7, byteBuffer.get());
        assertEquals(255, byteBuffer.get() & 0xFF); // Java does not have Unsigned Bytes
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateInt16TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.INT_16_ARRAY, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 8);

        ShortBuffer byteBuffer = buffer.getBackingStore().asShortBuffer();
        assertEquals(V8Value.INT_16_ARRAY, typedArray.getType());
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateUInt16TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.UNSIGNED_INT_16_ARRAY, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 8);

        ShortBuffer byteBuffer = buffer.getBackingStore().asShortBuffer();
        assertEquals(V8Value.UNSIGNED_INT_16_ARRAY, typedArray.getType());
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateInt32TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.INT_32_ARRAY, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 8);

        IntBuffer byteBuffer = buffer.getBackingStore().asIntBuffer();
        assertEquals(V8Value.INT_32_ARRAY, typedArray.getType());
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateUInt32TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.UNSIGNED_INT_32_ARRAY, 0, 2);

        typedArray.add("0", 7);
        typedArray.add("1", 8);

        IntBuffer byteBuffer = buffer.getBackingStore().asIntBuffer();
        assertEquals(V8Value.UNSIGNED_INT_32_ARRAY, typedArray.getType());
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateFloat32TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.FLOAT_32_ARRAY, 0, 2);

        assertEquals(V8Value.FLOAT_32_ARRAY, typedArray.getType());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testAccess32BitFloatFromTypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.FLOAT_32_ARRAY, 0, 2);

        typedArray.add("0", 3.14);

        assertEquals(V8Value.FLOAT_32_ARRAY, typedArray.getType());
        assertEquals(3.14, buffer.getBackingStore().asFloatBuffer().get(), 0.0001);
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testCreateFloat64TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.FLOAT_64_ARRAY, 0, 1);

        assertEquals(V8Value.FLOAT_64_ARRAY, typedArray.getType());
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testAccess64BitFloatFromTypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray typedArray = new V8TypedArray(v8, buffer, V8Value.FLOAT_64_ARRAY, 0, 1);

        typedArray.add("0", 3.14159265359);

        assertEquals(V8Value.FLOAT_64_ARRAY, typedArray.getType());
        assertEquals(3.14159265359, buffer.getBackingStore().asDoubleBuffer().get(), 0.000000000001);
        buffer.release();
        typedArray.release();
    }

    @Test
    public void testUpdateInt8TypedArrayInJavaScript() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);
        v8.add("v8Int8Array", v8Int8Array);

        v8.executeVoidScript("v8Int8Array[0] = 7; v8Int8Array[1] = 9;");

        ByteBuffer byteBuffer = buffer.getBackingStore();
        assertEquals(7, byteBuffer.get());
        assertEquals(9, byteBuffer.get());
        buffer.release();
        v8Int8Array.release();
    }

    @Test
    public void testAccessInt8TypedArrayInJavaScript() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 2);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);
        v8.add("v8Int8Array", v8Int8Array);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        byteBuffer.put((byte) 4);
        byteBuffer.put((byte) 8);

        assertEquals(4, v8.executeIntegerScript("v8Int8Array[0];"));
        assertEquals(8, v8.executeIntegerScript("v8Int8Array[1];"));
        buffer.release();
        v8Int8Array.release();
    }

    @Test
    public void testWriteInt8TypedArrayFromArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);
        v8.add("v8Int8Array", v8Int8Array);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        byteBuffer.put((byte) 4);
        byteBuffer.put((byte) 8);

        assertEquals(4, v8Int8Array.get(0));
        assertEquals(8, v8Int8Array.get(1));
        buffer.release();
        v8Int8Array.release();
    }

    @Test
    public void testInt8TypedArray_BufferReleased() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);
        buffer.release();
        v8.add("v8Int8Array", v8Int8Array);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        byteBuffer.put((byte) 4);
        byteBuffer.put((byte) 8);

        assertEquals(4, v8Int8Array.get(0));
        assertEquals(8, v8Int8Array.get(1));
        v8Int8Array.release();
    }

    @Test
    public void testInt8TypedArray_Length() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);
        buffer.release();
        v8.add("v8Int8Array", v8Int8Array);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        byteBuffer.put((byte) 4);
        byteBuffer.put((byte) 8);

        assertEquals(2, v8Int8Array.length());
        v8Int8Array.release();
    }

    @Test
    public void testInt8TypedArray_CustomLength() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 1);
        buffer.release();
        v8.add("v8Int8Array", v8Int8Array);

        buffer.getBackingStore();

        assertEquals(1, v8Int8Array.length());
        v8Int8Array.release();
    }

    @Test
    public void testInt8TypedArray_CustomOffset() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 1, 1);
        buffer.release();
        v8.add("v8Int8Array", v8Int8Array);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        byteBuffer.put((byte) 4);
        byteBuffer.put((byte) 8);

        assertEquals(1, v8Int8Array.length());
        assertEquals(8, v8Int8Array.get(0));
        v8Int8Array.release();
    }

    @Test
    public void testInt8TypedArray_Twin() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        V8TypedArray v8Int8Array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 4);
        V8Array twinArray = v8Int8Array.twin();

        assertTrue(twinArray instanceof V8TypedArray);
        assertEquals(V8Value.BYTE, twinArray.getType());
        assertEquals(v8Int8Array, twinArray);
        v8Int8Array.release();
        twinArray.release();
        buffer.release();
    }

    @Test
    public void testInt8TypedArray_TwinHasSameValues() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        V8TypedArray v8Int8Array1 = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 4);
        V8TypedArray v8Int8Array2 = (V8TypedArray) v8Int8Array1.twin();

        v8Int8Array1.add("0", 7);
        v8Int8Array1.add("1", 8);

        assertEquals(7, v8Int8Array2.get(0));
        assertEquals(8, v8Int8Array2.get(1));
        v8Int8Array1.release();
        v8Int8Array2.release();
        buffer.release();
    }

    @Test
    public void testGetInt8TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        V8Array array = (V8Array) v8.executeScript("new Int8Array(buf);");

        assertTrue(array instanceof V8TypedArray);
        array.release();
        buffer.release();
    }

    @Test
    public void testCreateV8Int8ArrayFromJSArrayBuffer() {
        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.executeScript(""
                + "var buffer = new ArrayBuffer(8)\n"
                + "var array = new Int8Array(buffer);\n"
                + "array[0] = 7; array[1] = 9;\n"
                + "buffer;");

        V8TypedArray array = new V8TypedArray(v8, buffer, V8Value.BYTE, 0, 2);

        assertEquals(7, array.get(0));
        assertEquals(9, array.get(1));
        array.release();
        buffer.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_Boolean() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.BOOLEAN, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_String() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.STRING, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_V8Array() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.V8_ARRAY, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_V8Object() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.V8_OBJECT, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_V8ArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.V8_ARRAY_BUFFER, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_V8Function() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.V8_FUNCTION, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_Null() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.NULL, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWithIllegalType_Undefined() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        try {
            new V8TypedArray(v8, buffer, V8Value.UNDEFINED, 0, 10);
        } finally {
            buffer.release();
        }
    }

    @Test
    public void testInt8ArrayIs1Byte() {
        assertEquals(1, V8TypedArray.getStructureSize(V8Value.INT_8_ARRAY));
    }

    @Test
    public void testUInt8ArrayIs1Byte() {
        assertEquals(1, V8TypedArray.getStructureSize(V8Value.UNSIGNED_INT_8_ARRAY));
    }

    @Test
    public void testUInt8ClampedArrayIs1Byte() {
        assertEquals(1, V8TypedArray.getStructureSize(V8Value.UNSIGNED_INT_8_CLAMPED_ARRAY));
    }

    @Test
    public void testInt16ArrayIs2Bytes() {
        assertEquals(2, V8TypedArray.getStructureSize(V8Value.INT_16_ARRAY));
    }

    @Test
    public void testUInt16ArrayIs2Bytes() {
        assertEquals(2, V8TypedArray.getStructureSize(V8Value.UNSIGNED_INT_16_ARRAY));
    }

    @Test
    public void testInt32ArrayIs4Bytes() {
        assertEquals(4, V8TypedArray.getStructureSize(V8Value.INT_32_ARRAY));
    }

    @Test
    public void testUInt32ArrayIs4Bytes() {
        assertEquals(4, V8TypedArray.getStructureSize(V8Value.UNSIGNED_INT_32_ARRAY));
    }

    @Test
    public void testFloat32ArrayIs4Bytes() {
        assertEquals(4, V8TypedArray.getStructureSize(V8Value.FLOAT_32_ARRAY));
    }

    @Test
    public void testFloat64ArrayIs8Bytes() {
        assertEquals(8, V8TypedArray.getStructureSize(V8Value.FLOAT_64_ARRAY));
    }

}
