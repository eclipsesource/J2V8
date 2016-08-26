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

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8ArrayBufferTest {

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
    public void testCreateV8ArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 100);
        v8.add("buffer", buffer);

        V8Value result = (V8Value) v8.executeScript("var ints = new Int8Array(buffer); ints");
        assertNotNull(result);
        result.release();
        buffer.release();
    }

    @Test
    public void testArrayBufferType() {
        V8Array container = (V8Array) v8.executeScript("var buf = new ArrayBuffer(100); var container = [buf]; container");

        assertEquals(V8Value.V8_ARRAY_BUFFER, container.getType(0));
        container.release();
    }

    @Test
    public void testGetArrayBufferIsV8ArrayBuffer() {
        V8Value result = (V8Value) v8.executeScript("var buf = new ArrayBuffer(100);  buf;");

        assertTrue(result instanceof V8ArrayBuffer);
        result.release();
    }

    @Test
    public void testTypedArrayLength_WithArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);
        V8Array result = (V8Array) v8.executeScript("var ints = new Int32Array(buf); ints[0] = 7; ints");

        assertEquals(1, result.length());
        result.release();
        buffer.release();
    }

    @Test
    public void testAccessArrayBuffer_Int8ArrayView() {
        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int8Array(buf); ints[0] = 7; buf");

        ByteBuffer byteBuffer = buffer.getBackingStore();

        assertEquals(4, byteBuffer.limit());
        assertEquals(7, byteBuffer.get(0));
        buffer.release();
    }

    @Test
    public void testAccessArrayBuffer_Int32ArrayView() {
        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int32Array(buf); ints[0] = 7; buf");

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();

        assertEquals(1, intBuffer.limit());
        assertEquals(7, intBuffer.get(0));
        buffer.release();
    }

    @Test
    public void testAccessArrayBuffer_Int16ArrayView() {
        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(4); var shorts = new Int16Array(buf); shorts[0] = 7; buf");

        ShortBuffer shortBuffer = buffer.getBackingStore().asShortBuffer();

        assertEquals(2, shortBuffer.limit());
        assertEquals(7, shortBuffer.get(0));
        buffer.release();
    }

    @Test
    public void testAccessArrayBuffer_Float32rrayView() {
        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(4); var floats = new Float32Array(buf); floats[0] = 7.7; buf");

        FloatBuffer floatBuffer = buffer.getBackingStore().asFloatBuffer();

        assertEquals(1, floatBuffer.limit());
        assertEquals(7.7, floatBuffer.get(0), 0.00001);
        buffer.release();
    }

    @Test
    public void testAccessArrayBuffer_Float64ArrayView() {
        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(8); var floats = new Float64Array(buf); floats[0] = 7.7; buf");

        DoubleBuffer doubleBuffer = buffer.getBackingStore().asDoubleBuffer();

        assertEquals(1, doubleBuffer.limit());
        assertEquals(7.7, doubleBuffer.get(0), 0.00001);
        buffer.release();
    }

    @Test
    public void testGetTypedArrayValue_WithArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        int result = v8.executeIntegerScript("var ints = new Int16Array(buf); ints[0] = 7; ints[0]");

        assertEquals(7, result);
        buffer.release();
    }

    @Test
    public void testGetTypedArrayIntValue_WithArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        V8Array result = (V8Array) v8.executeScript("var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(7, result.get(0));
        result.release();
        buffer.release();
    }

    @Test
    public void testGetTypedArrayUsingKeys_WithArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        V8Array result = (V8Array) v8.executeScript("var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(7, result.getInteger("0"));
        result.release();
        buffer.release();
    }

    @Test
    public void testGetTypedArrayType32BitValue_FromBackingStore() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        v8.executeVoidScript("var ints = new Int32Array(buf); ints[0] = 255;");

        assertEquals(255, buffer.getBackingStore().asIntBuffer().get(0));
        buffer.release();
    }

    @Test
    public void testGetTypedArrayType16BitValue_FromBackingStore() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        v8.executeVoidScript("var ints = new Int16Array(buf); ints[0] = 255;");

        assertEquals(255, buffer.getBackingStore().asShortBuffer().get(0));
        buffer.release();
    }

    @Test
    public void testGetTypedArrayType32BitFloatValue_FromBackingStore() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        v8.add("buf", buffer);

        v8.executeVoidScript("var floats = new Float32Array(buf); floats[0] = 255.5;");

        assertEquals(255.5, buffer.getBackingStore().asFloatBuffer().get(0), 0.00001);
        buffer.release();
    }

    @Test
    public void testGetTypedArrayType64BitFloatValue_FromBackingStore() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        v8.add("buf", buffer);

        v8.executeVoidScript("var floats = new Float64Array(buf); floats[0] = 255.5;");

        assertEquals(255.5, buffer.getBackingStore().asDoubleBuffer().get(0), 0.00001);
        buffer.release();
    }

    @Test
    public void testGetTypedRangeArrayValue_FromBackingStore() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 100);
        v8.add("buf", buffer);

        v8.executeVoidScript("var ints = new Int32Array(buf); for(var i = 0; i < 25; i++) {ints[i] = i;};");

        IntBuffer intBuffer = buffer.getBackingStore().asIntBuffer();
        assertEquals(25, intBuffer.limit());
        for (int i = 0; i < intBuffer.limit(); i++) {
            assertEquals(i, intBuffer.get(i));
        }
        buffer.release();
    }

    @Test
    public void testAddTypedArrayIntegerToBackingStore() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        v8.add("buf", buffer);

        V8Array array = (V8Array) v8.executeScript("var ints = new Int32Array(buf); ints");

        buffer.getBackingStore().asIntBuffer().put(0, 7);
        buffer.getBackingStore().asIntBuffer().put(1, 17);

        assertEquals(2, array.length());
        assertEquals(7, array.getInteger(0));
        assertEquals(17, array.getInteger(1));
        array.release();
        buffer.release();
    }

    @Test
    public void testAddTypedArrayFloatToBackingStore() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        v8.add("buf", buffer);

        V8Array array = (V8Array) v8.executeScript("var floats = new Float32Array(buf); floats");

        buffer.getBackingStore().asFloatBuffer().put(0, 7.7f);
        buffer.getBackingStore().asFloatBuffer().put(1, 17.7f);

        assertEquals(2, array.length());
        assertEquals(7.7, array.getDouble(0), 0.000001);
        assertEquals(17.7, array.getDouble(1), 0.000001);
        array.release();
        buffer.release();
    }

    @Test
    public void testUseCustomByteBuffer_Int32Array() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, byteBuffer);
        v8.add("buf", buffer);

        V8Array array = (V8Array) v8.executeScript("var ints = new Int32Array(buf); ints");

        buffer.getBackingStore().asIntBuffer().put(0, 7);
        buffer.getBackingStore().asIntBuffer().put(1, 17);

        assertEquals(2, array.length());
        assertEquals(7, array.getInteger(0));
        assertEquals(17, array.getInteger(1));
        array.release();
        buffer.release();
    }

    @Test
    public void testUseCustomByteBuffer_Float32Array() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, byteBuffer);
        v8.add("buf", buffer);

        V8Array array = (V8Array) v8.executeScript("var floats = new Float32Array(buf); floats");

        buffer.getBackingStore().asFloatBuffer().put(0, 7.7f);
        buffer.getBackingStore().asFloatBuffer().put(1, 17.7f);

        assertEquals(2, array.length());
        assertEquals(7.7, array.getDouble(0), 0.000001);
        assertEquals(17.7, array.getDouble(1), 0.000001);
        array.release();
        buffer.release();
    }

    @Test
    public void shareDirectBufferBetweenArrayBuffers() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
        V8ArrayBuffer buffer1 = new V8ArrayBuffer(v8, byteBuffer);
        V8ArrayBuffer buffer2 = new V8ArrayBuffer(v8, byteBuffer);
        V8TypedArray array1 = new V8TypedArray(v8, buffer1, V8Value.INT_32_ARRAY, 0, 2);
        V8TypedArray array2 = new V8TypedArray(v8, buffer1, V8Value.INT_32_ARRAY, 0, 2);

        array1.add("0", 7).add("1", 9);

        assertEquals(7, array2.get(0));
        assertEquals(9, array2.get(1));
        array1.release();
        array2.release();
        buffer1.release();
        buffer2.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteBufferMustBeDirectBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        new V8ArrayBuffer(v8, byteBuffer);
    }

    @Test
    public void getArrayBuffer() {
        v8.executeVoidScript("var buffer = new ArrayBuffer(8);");

        V8ArrayBuffer buffer = (V8ArrayBuffer) v8.get("buffer");

        assertNotNull(buffer);
        buffer.release();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetBackingStoreV8Released() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        v8.release();

        try {
            buffer.getBackingStore();
        } finally {
            v8 = V8.createV8Runtime();
        }
    }
}
