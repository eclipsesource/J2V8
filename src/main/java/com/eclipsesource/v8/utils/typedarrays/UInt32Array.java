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

import com.eclipsesource.v8.V8Value;

/**
 * The Uint32Array typed array represents an array of 32-bit unsigned
 * integers in the platform byte order.
 */
public class UInt32Array extends TypedArray {

    /**
     * Creates an UInt32Array projected onto the given ByteBuffer.
     *
     * @param buffer The ByteBuffer on which the array is projected on.
     */
    public UInt32Array(final ByteBuffer buffer) {
        super(buffer);
    }

    /**
     * Creates a UInt32Array projected onto the given ArrayBuffer.
     *
     * @param arrayBuffer The ArrayBuffer on which the array is projected on.
     */
    public UInt32Array(final ArrayBuffer arrayBuffer) {
        this(arrayBuffer.getByteBuffer());
    }

    /**
     * Returns the 32-bit unsigned integer at the given index.
     *
     * @param index The index at which to return the value.
     * @return The 32-bit unsigned integer at the index.
     */
    public long get(final int index) {
        return 0x00000000FFFFFFFF & buffer.asIntBuffer().get(index);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#length()
     */
    @Override
    public int length() {
        return buffer.asIntBuffer().limit();
    }

    /**
     * Puts a 32-bit unsigned integer at a particular index.
     *
     * @param index The index at which to place the value.
     * @param value The 32-bit unsigned integer to put into buffer.
     */
    public void put(final int index, final long value) {
        buffer.asIntBuffer().put(index, (int) (0x00000000FFFFFFFF & value));
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#getType()
     */
    @Override
    public int getType() {
        return V8Value.UNSIGNED_INT_32_ARRAY;
    }

}
