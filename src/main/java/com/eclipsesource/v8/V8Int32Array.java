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

/**
 * A representation of a JS Int32Array in Java. An Int32Array is a typed array
 * in which each value is a 32-bit integer. The typed array is simply a 'view' onto
 * a back buffer.
 */
public class V8Int32Array extends V8Array {

    /**
     * Create a new V8Int32Array from a specified ArrayBuffer, offset and size. An
     * V8Int32Array is a typed array where each value is a 32-bit integer. The
     * typed array is backed by the V8ArrayBuffer.
     *
     * @param v8 The V8Runtime on which to create this Int32Array
     * @param buffer The buffer used to back the typed array
     * @param offset The offset into the buffer at which to start the the array
     * @param size The size of the typed array
     */
    public V8Int32Array(final V8 v8, final V8ArrayBuffer buffer, final int offset, final int size) {
        super(v8, new V8Int32ArrayData(buffer, offset, size));
    }

    private V8Int32Array(final V8 v8) {
        super(v8);
    }

    /**
     * Provide access to the underlying ByteBuffer used for this TypedArray.
     * The V8ArrayBuffer must be released.
     *
     * @return The V8ArrayBuffer used to back this TypedArray.
     */
    public V8ArrayBuffer getBuffer() {
        return (V8ArrayBuffer) get("buffer");
    }

    @Override
    protected long initialize(final long runtimePtr, final Object data) {
        v8.checkThread();
        if (data == null) {
            return super.initialize(runtimePtr, data);
        }
        V8Int32ArrayData arrayData = (V8Int32ArrayData) data;
        long handle = v8.initNewV8Int32Array(runtimePtr, arrayData.buffer.objectHandle, arrayData.offset, arrayData.size);
        v8.addObjRef();
        released = false;
        return handle;
    }

    @Override
    protected V8Value createTwin() {
        return new V8Int32Array(v8);
    }

    private static class V8Int32ArrayData {
        private V8ArrayBuffer buffer;
        private int           offset;
        private int           size;

        public V8Int32ArrayData(final V8ArrayBuffer buffer, final int offset, final int size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
    }

}
