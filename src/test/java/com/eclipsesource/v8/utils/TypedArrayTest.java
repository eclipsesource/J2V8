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
package com.eclipsesource.v8.utils;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;
import com.eclipsesource.v8.utils.typedarrays.Float32Array;
import com.eclipsesource.v8.utils.typedarrays.Float64Array;
import com.eclipsesource.v8.utils.typedarrays.Int16Array;
import com.eclipsesource.v8.utils.typedarrays.Int32Array;
import com.eclipsesource.v8.utils.typedarrays.Int8Array;
import com.eclipsesource.v8.utils.typedarrays.TypedArray;
import com.eclipsesource.v8.utils.typedarrays.UInt16Array;
import com.eclipsesource.v8.utils.typedarrays.UInt32Array;
import com.eclipsesource.v8.utils.typedarrays.UInt8Array;
import com.eclipsesource.v8.utils.typedarrays.UInt8ClampedArray;

public class TypedArrayTest {

    @Test
    public void testCreateFloat32ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new Float32Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateFloat64ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new Float64Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateInt32ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new Int32Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateUInt32ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new UInt32Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateInt16ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new Int16Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateUInt16ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new UInt16Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateInt8ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new Int8Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateUInt8ArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new UInt8Array(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testCreateUInt8ClampedArrayFromArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        TypedArray typedArray = new UInt8ClampedArray(arrayBuffer);

        assertEquals(arrayBuffer.getByteBuffer(), typedArray.getByteBuffer());
    }

    @Test
    public void testInt8Array_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(10);
        Int8Array array = new Int8Array(buffer);

        int result = array.length();

        assertEquals(10, result);
    }

    @Test
    public void testUInt8Array_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(10);
        UInt8Array array = new UInt8Array(buffer);

        int result = array.length();

        assertEquals(10, result);
    }

    @Test
    public void testUInt8ClampedArray_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(10);
        UInt8ClampedArray array = new UInt8ClampedArray(buffer);

        int result = array.length();

        assertEquals(10, result);
    }

    @Test
    public void testInt16Array_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Int16Array array = new Int16Array(buffer);

        int result = array.length();

        assertEquals(4, result);
    }

    @Test
    public void testUInt16Array_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt16Array array = new UInt16Array(buffer);

        int result = array.length();

        assertEquals(4, result);
    }

    @Test
    public void testInt32ArrayArray_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Int32Array array = new Int32Array(buffer);

        int result = array.length();

        assertEquals(2, result);
    }

    @Test
    public void testUInt32ArrayArray_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt32Array array = new UInt32Array(buffer);

        int result = array.length();

        assertEquals(2, result);
    }

    @Test
    public void testFloat32ArrayArray_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Float32Array array = new Float32Array(buffer);

        int result = array.length();

        assertEquals(2, result);
    }

    @Test
    public void testFloat64ArrayArray_Length() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Float64Array array = new Float64Array(buffer);

        int result = array.length();

        assertEquals(1, result);
    }

    @Test
    public void testInt8Array_PutValue() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Int8Array array = new Int8Array(buffer);

        array.put(0, (byte) 7);

        assertEquals(7, array.get(0));
    }

    @Test
    public void testUInt8Array_PutValue() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt8Array array = new UInt8Array(buffer);

        array.put(0, (byte) 7);

        assertEquals(7, buffer.get());
    }

    @Test
    public void testUInt8Array_PutValueGreater128() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt8Array array = new UInt8Array(buffer);

        array.put(0, (short) 129);

        assertEquals(129, array.get(0));
    }

    @Test
    public void testUInt8Array_PutValueGreater255() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt8ClampedArray array = new UInt8ClampedArray(buffer);

        array.put(0, (short) 256);

        assertEquals(255, array.get(0));
    }

    @Test
    public void testUInt8Array_PutValueLess0() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt8ClampedArray array = new UInt8ClampedArray(buffer);

        array.put(0, (short) -1);

        assertEquals(0, array.get(0));
    }

    @Test
    public void testInt16Array_PutValue() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Int16Array array = new Int16Array(buffer);

        array.put(0, (short) 10000);

        assertEquals(10000, array.get(0));
    }

    @Test
    public void testInt16Array_PutValueGreaterMaxShort() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt16Array array = new UInt16Array(buffer);

        array.put(0, Short.MAX_VALUE + 1);

        assertEquals(Short.MAX_VALUE + 1, array.get(0));
    }

    @Test
    public void testInt32Array_PutValue() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Int32Array array = new Int32Array(buffer);

        array.put(0, 1000000);

        assertEquals(1000000, array.get(0));
    }

    @Test
    public void testUInt32Array_PutValueGreaterMaxInt() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        UInt32Array array = new UInt32Array(buffer);

        array.put(0, Integer.MAX_VALUE + 1);

        assertEquals(Integer.MAX_VALUE + 1, array.get(0));
    }

    @Test
    public void testFloat32Array_PutValue() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Float32Array array = new Float32Array(buffer);

        array.put(0, 3.14f);

        assertEquals(3.14, array.get(0), 0.0001);
    }

    @Test
    public void testFloat64Array_PutValue() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        Float64Array array = new Float64Array(buffer);

        array.put(0, 3.14159);

        assertEquals(3.14159, array.get(0), 0.0001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWrongSizebuffer_Float32() {
        new Float32Array(ByteBuffer.allocateDirect(7));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWrongSizebuffer_Float64() {
        new Float64Array(ByteBuffer.allocateDirect(7));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWrongSizebuffer_Int32() {
        new Int32Array(ByteBuffer.allocateDirect(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTypedArrayWrongSizebuffer_Int16() {
        new Int16Array(ByteBuffer.allocateDirect(1));
    }

    @Test
    public void testGetType_Float64Array() {
        assertEquals(V8Value.FLOAT_64_ARRAY, new Float64Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_Float32Array() {
        assertEquals(V8Value.FLOAT_32_ARRAY, new Float32Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_Int32Array() {
        assertEquals(V8Value.INT_32_ARRAY, new Int32Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_UInt32Array() {
        assertEquals(V8Value.UNSIGNED_INT_32_ARRAY, new UInt32Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_Int16Array() {
        assertEquals(V8Value.INT_16_ARRAY, new Int16Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_UInt16Array() {
        assertEquals(V8Value.UNSIGNED_INT_16_ARRAY, new UInt16Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_Int8Array() {
        assertEquals(V8Value.INT_8_ARRAY, new Int8Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_UInt8Array() {
        assertEquals(V8Value.UNSIGNED_INT_8_ARRAY, new UInt8Array(ByteBuffer.allocateDirect(8)).getType());
    }

    @Test
    public void testGetType_UInt8ClampledArray() {
        assertEquals(V8Value.UNSIGNED_INT_8_CLAMPED_ARRAY, new UInt8ClampedArray(ByteBuffer.allocateDirect(8)).getType());
    }

}
