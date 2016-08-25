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
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.eclipsesource.v8.utils.typedarrays.ArrayBuffer;

public class ArrayBufferTest {

    @Test
    public void testCreateArrayBufferCapacity() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(8);

        assertEquals(8, arrayBuffer.getByteBuffer().limit());
    }

    @Test
    public void testCreateArrayBuffer_ByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        ArrayBuffer arrayBuffer = new ArrayBuffer(buffer);

        assertSame(buffer, arrayBuffer.getByteBuffer());
    }

    @Test
    public void testCreateArrayBufferWithByteArray() {
        byte[] bytes = new byte[] {1,2,3,4,5,6,7,8};
        ArrayBuffer arrayBuffer = new ArrayBuffer(bytes);

        assertEquals(8, arrayBuffer.getByteBuffer().limit());
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1, arrayBuffer.getByteBuffer().get((i)));
        }
    }

    @Test
    public void testGetByte() {
        byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        ArrayBuffer arrayBuffer = new ArrayBuffer(bytes);

        assertEquals(8, arrayBuffer.getByteBuffer().limit());
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1, arrayBuffer.getByte(i));
        }
    }

    @Test
    public void testGetByteUnsigned() {
        byte[] bytes = new byte[] { 127, (byte) 129, (byte) 255, (byte) 256 };
        ArrayBuffer arrayBuffer = new ArrayBuffer(bytes);

        assertEquals(127, arrayBuffer.getUnsignedByte(0));
        assertEquals(129, arrayBuffer.getUnsignedByte(1));
        assertEquals(255, arrayBuffer.getUnsignedByte(2));
        assertEquals(0, arrayBuffer.getUnsignedByte(3));
    }

    @Test
    public void testPutByte() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(3);
        arrayBuffer.put(0, (byte) 1);
        arrayBuffer.put(1, (byte) 2);
        arrayBuffer.put(2, (byte) 3);

        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, arrayBuffer.getByteBuffer().get((i)));
        }
    }

    @Test
    public void testLimit() {
        ArrayBuffer buffer = new ArrayBuffer(100);

        assertEquals(100, buffer.limit());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutByte_IndexOutOfBoundsx() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(3);
        arrayBuffer.put(3, (byte) 3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutByte_IndexOutOfBoundsxNegative() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(3);
        arrayBuffer.put(-1, (byte) 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayBufferRequiresDirectBuffer() {
        new ArrayBuffer(ByteBuffer.allocate(8));
    }

}
