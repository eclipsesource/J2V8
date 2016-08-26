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

import com.eclipsesource.v8.V8TypedArray;

/**
 * An abstract class that represents TypedArrays
 */
public abstract class TypedArray {

    protected ByteBuffer buffer;

    protected TypedArray(final ByteBuffer buffer) {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("ByteBuffer must be a allocated as a direct ByteBuffer");
        }
        if ((buffer.limit() % V8TypedArray.getStructureSize(getType())) != 0) {
            throw new IllegalArgumentException("ByteBuffer must be a allocated as a direct ByteBuffer");
        }
        this.buffer = buffer;
    }

    /**
     * Return the underlying ByteBuffer.
     *
     * @return The underlying ByteBuffer behind this view
     */
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    /**
     * Return the size of this view. The size of the view is determined by the size
     * of the buffer, and the size of the data projected onto it. For example, for a
     * buffer size of 8, and a view representing 16bit integers, the size would be 4.
     *
     * @return The size of this view
     */
    public abstract int length();

    /**
     * Returns the 'Type' of this TypedArray using one of the constants defined in V8Value.
     *
     * @return The 'Type' of this typed array.
     */
    public abstract int getType();
}
