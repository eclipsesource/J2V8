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
 * The Int16Array typed array represents an array of twos-complement
 * 16-bit signed integers in the platform byte order.
 */
public class Int16Array extends TypedArray {

    /**
     * Creates an Int16Array projected onto the given ByteBuffer.
     *
     * @param buffer The ByteBuffer on which the array is projected on.
     */
    public Int16Array(final ByteBuffer buffer) {
        super(buffer);
    }

    /**
     * Creates a Int16Array projected onto the given ArrayBuffer.
     *
     * @param arrayBuffer The ArrayBuffer on which the array is projected on.
     */
    public Int16Array(final ArrayBuffer arrayBuffer) {
        this(arrayBuffer.getByteBuffer());
    }

    /**
     * Returns the 16-bit signed integer at the given index
     *
     * @param index The index at which to return the value.
     * @return The 16-bit integer at the index.
     */
    public short get(final int index) {
        return buffer.asShortBuffer().get(index);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#length()
     */
    @Override
    public int length() {
        return buffer.asShortBuffer().limit();
    }

    /**
     * Puts a 16-bit integer at a particular index.
     *
     * @param index The index at which to place the value.
     * @param value The 16-bit integer to put into buffer.
     */
    public void put(final int index, final short value) {
        buffer.asShortBuffer().put(index, value);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#getType()
     */
    @Override
    public int getType() {
        return V8Value.INT_16_ARRAY;
    }

}
