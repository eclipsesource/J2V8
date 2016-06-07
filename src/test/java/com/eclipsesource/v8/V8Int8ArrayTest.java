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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8Int8ArrayTest {

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
    public void testGetByteBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);

        ByteBuffer intBuffer = v8Int8Array.getByteBuffer();

        assertEquals(intBuffer, buffer.getBackingStore());
        buffer.release();
        v8Int8Array.release();
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLength() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        try {
            new V8Int8Array(v8, buffer, 0, 9);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLengthNegative() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        try {
            new V8Int8Array(v8, buffer, 0, -1);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateArrayInvalidLengthWithOffset() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 12);
        try {
            new V8Int8Array(v8, buffer, 4, 11);
        } finally {
            buffer.release();
        }
    }

    @Test
    public void testGetArrayBuffer() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);

        V8ArrayBuffer result = v8Int8Array.getBuffer();

        assertEquals(result, buffer);
        result.release();
        buffer.release();
        v8Int8Array.release();
    }

    @Test
    public void testUseAccessedArrayBuffer() {
        V8Int8Array array = (V8Int8Array) v8.executeScript("\n"
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
    public void testCreateInt8TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);

        v8Int8Array.add("0", 7);
        v8Int8Array.add("1", 8);

        ByteBuffer byteBuffer = buffer.getBackingStore();
        assertEquals(7, byteBuffer.get());
        assertEquals(8, byteBuffer.get());
        buffer.release();
        v8Int8Array.release();
    }

    @Test
    public void testUpdateInt8TypedArrayInJavaScript() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);
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
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);
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
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);
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
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);
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
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 2);
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
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 1);
        buffer.release();
        v8.add("v8Int8Array", v8Int8Array);

        buffer.getBackingStore();

        assertEquals(1, v8Int8Array.length());
        v8Int8Array.release();
    }

    @Test
    public void testInt8TypedArray_CustomOffset() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 1, 1);
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
        V8Int8Array v8Int8Array = new V8Int8Array(v8, buffer, 0, 4);
        V8Array twinArray = v8Int8Array.twin();

        assertTrue(twinArray instanceof V8Int8Array);
        assertEquals(v8Int8Array, twinArray);
        v8Int8Array.release();
        twinArray.release();
        buffer.release();
    }

    @Test
    public void testInt8TypedArray_TwinHasSameValues() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        V8Int8Array v8Int8Array1 = new V8Int8Array(v8, buffer, 0, 4);
        V8Int8Array v8Int8Array2 = (V8Int8Array) v8Int8Array1.twin();

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

        assertTrue(array instanceof V8Int8Array);
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

        V8Int8Array array = new V8Int8Array(v8, buffer, 0, 2);

        assertEquals(7, array.get(0));
        assertEquals(9, array.get(1));
        array.release();
        buffer.release();
    }

}
