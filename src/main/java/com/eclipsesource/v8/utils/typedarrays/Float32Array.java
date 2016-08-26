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
 * The Float32Array typed array represents an array of 32-bit floating
 * point numbers.
 */
public class Float32Array extends TypedArray {

    /**
     * Creates a Float32Array projected onto the given ByteBuffer.
     *
     * @param buffer The ByteBuffer on which the array is projected on.
     */
    public Float32Array(final ByteBuffer buffer) {
        super(buffer);
    }

    /**
     * Creates a Float32Array projected onto the given ArrayBuffer.
     *
     * @param arrayBuffer The ArrayBuffer on which the array is projected on.
     */
    public Float32Array(final ArrayBuffer arrayBuffer) {
        this(arrayBuffer.getByteBuffer());
    }

    /**
     * Returns the floating point (Float32) value at a given index.
     *
     * @param index The index at which to return the value.
     * @return The Float32 value at the given index.
     */
    public float get(final int index) {
        return buffer.asFloatBuffer().get(index);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#length()
     */
    @Override
    public int length() {
        return buffer.asFloatBuffer().limit();
    }

    /**
     * Puts a Float32 value at a particular index.
     *
     * @param index The index at which to place the value.
     * @param value The Float32 value to place into buffer.
     */
    public void put(final int index, final float value) {
        buffer.asFloatBuffer().put(index, value);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.utils.typedarrays.TypedArray#getType()
     */
    @Override
    public int getType() {
        return V8Value.FLOAT_32_ARRAY;
    }

}
