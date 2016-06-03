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

import java.nio.IntBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8Int32ArrayTest {

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
    public void testCreateInt32TypedArray() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 8);
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 2);

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
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 2);
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
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 2);
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
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 2);
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
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 2);
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
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 2);
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
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 1);
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
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 4, 1);
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
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        V8Int32Array v8Int32Array = new V8Int32Array(v8, buffer, 0, 300);
        V8Array twinArray = v8Int32Array.twin();

        assertTrue(twinArray instanceof V8Int32Array);
        assertEquals(v8Int32Array, twinArray);
        v8Int32Array.release();
        twinArray.release();
        buffer.release();
    }

    @Test
    public void testInt32TypedArray_TwinHasSameValues() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 4);
        V8Int32Array v8Int32Array1 = new V8Int32Array(v8, buffer, 0, 300);
        V8Int32Array v8Int32Array2 = (V8Int32Array) v8Int32Array1.twin();

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

        assertTrue(array instanceof V8Int32Array);
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

        V8Int32Array array = new V8Int32Array(v8, buffer, 0, 2);

        assertEquals(7, array.get(0));
        assertEquals(9, array.get(1));
        array.release();
        buffer.release();
    }

}
