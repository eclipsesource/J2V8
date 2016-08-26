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
 * The Float64Array typed array represents an array of 64-bit floating
 * point numbers.
 */
public class Float64Array extends TypedArray {

    /**
     * Creates a Float64Array projected onto the given ByteBuffer.
     *
     * @param buffer The ByteBuffer on which the array is projected on.
     */
    public Float64Array(final ByteBuffer buffer) {
        super(buffer);
    }

    /**
     * Creates a Float64Array projected onto the given ArrayBuffer.
     *
     * @param arrayBuffer The ArrayBuffer on which the array is projected on.
     */
    public Float64Array(final ArrayBuffer arrayBuffer) {
        this(arrayBuffer.getByteBuffer());
    }

    /**
     * Returns the floating point (Float64) value at a given index.
     *
     * @param index The index at which to return the value.
     * @return The Double (Float64) value at the given index.
     */
    public double get(final int index) {
        return buffer.asDoubleBuffer().get(index);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#length()
     */
    @Override
    public int length() {
        return buffer.asDoubleBuffer().limit();
    }

    /**
     * Puts a Double (Float64) value at a particular index.
     *
     * @param index The index at which to place the value.
     * @param value The Double to put into the buffer.
     */
    public void put(final int index, final double value) {
        buffer.asDoubleBuffer().put(index, value);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#getType()
     */
    @Override
    public int getType() {
        return V8Value.FLOAT_64_ARRAY;
    }

}
