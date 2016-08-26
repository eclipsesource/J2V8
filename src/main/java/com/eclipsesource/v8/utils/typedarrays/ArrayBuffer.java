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
package com.eclipsesource.v8.utils.typedarrays;

import java.nio.ByteBuffer;

/**
 * A wrapper class for java.nio.ByteBuffer. This class provides some convenience methods
 * for working with the ByteBuffer. Furthermore, this class can be converted to a
 * V8ByteBuffer using V8ObjectUtils.
 */
public class ArrayBuffer {

    private ByteBuffer byteBuffer;

    /**
     * Create a new ArrayBuffer with an initial capacity.
     *
     * @param capacity The capacity of this ByteBuffer.
     */
    public ArrayBuffer(final int capacity) {
        byteBuffer = ByteBuffer.allocateDirect(capacity);
    }

    /**
     * Create a new ArrayBuffer from a byte array. The array buffer will be allocated with the same
     * size as the byte array, and the contents of the byte array will be copied to the ArrayBuffer.
     *
     * @param src The byte array from which the ArrayBuffer will be initialized.
     */
    public ArrayBuffer(final byte[] src) {
        byteBuffer = ByteBuffer.allocateDirect(src.length);
        byteBuffer.put(src, 0, src.length);
    }

    /**
     * Create a new ArrayBuffer with the given ByteBuffer as the backing store. The ByteBuffer must
     * be created as a DirectBuffer.
     *
     * @param byteBuffer The ByteBuffer to back this ArrayBuffer.
     */
    public ArrayBuffer(final ByteBuffer byteBuffer) {
        this.byteBuffer = validateByteBuffer(byteBuffer);
    }

    private ByteBuffer validateByteBuffer(final ByteBuffer byteBuffer) {
        if (!byteBuffer.isDirect()) {
            throw new IllegalArgumentException("ByteBuffer must be a allocated as a direct ByteBuffer");
        }
        return byteBuffer;
    }

    /**
     * Returns the ByteBuffer backing this ArrayBuffer.
     *
     * @return The ByteBuffer backing this ArrayBuffer.
     */
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Returns the byte at a given index.
     *
     * @param index The index at which to return the byte.
     * @return The byte at the given index.
     */
    public byte getByte(final int index) {
        return byteBuffer.get(index);
    }

    /**
     * Returns the byte at a given index as an unsigned integer.
     *
     * @param index The index at which to return the byte.
     * @return The unsigned byte at the given index.
     */
    public short getUnsignedByte(final int index) {
        return (short) (0xFF & byteBuffer.get(index));
    }

    /**
     * Puts a byte into the ByteBuffer at the given index.
     *
     * @param index The index at which to put the byte.
     * @param value The value to put at the index.
     */
    public void put(final int index, final byte value) {
        byteBuffer.put(index, value);
    }

    /**
     * Returns this ArrayBuffers limit.
     *
     * @return This ArrayBuffers limit.
     */
    public int limit() {
        return byteBuffer.limit();
    }

}
