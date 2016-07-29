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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * V8ArrayBuffers represent ArrayBuffers from V8, but are backed by a
 * java.nio.ByteBuffer. This means that any data stored in a TypedArray
 * can be accessed by the java.nio.ByteBuffer. This significantly improves
 * performance of data access from Java to JavaScript.
 *
 * V8ArrayBuffers can either be constructed in Java, or returned from
 * JavaScript.
 *
 */
public class V8ArrayBuffer extends V8Value {

    private ByteBuffer byteBuffer;

    /**
     * Creates a new V8ArrayBuffer on a given V8Runtime with a
     * given capacity.
     *
     * @param v8 The runtime on which to create the ArrayBuffer
     * @param capacity The capacity of the buffer
     */
    public V8ArrayBuffer(final V8 v8, final int capacity) {
        super(v8);
        initialize(v8.getV8RuntimePtr(), capacity);
        byteBuffer = v8.createV8ArrayBufferBackingStore(v8.getV8RuntimePtr(), objectHandle, capacity);
        byteBuffer.order(ByteOrder.nativeOrder());
    }

    V8ArrayBuffer(final V8 v8, final ByteBuffer byteBuffer) {
        super(v8);
        initialize(v8.getV8RuntimePtr(), byteBuffer.capacity());
        this.byteBuffer = byteBuffer;
        byteBuffer.order(ByteOrder.nativeOrder());
    }

    @Override
    protected void initialize(final long runtimePtr, final Object data) {
        v8.checkThread();
        int capacity = (Integer) data;
        objectHandle = v8.initNewV8ArrayBuffer(v8.getV8RuntimePtr(), capacity);
        released = false;
        addObjectReference(objectHandle);
    }

    @Override
    protected V8Value createTwin() {
        return new V8ArrayBuffer(v8, byteBuffer);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.V8Object#twin()
     */
    @Override
    public V8ArrayBuffer twin() {
        return (V8ArrayBuffer) super.twin();
    }

    /**
     * Returns the backing store used for this ArrayBuffer.
     *
     * @return The backing store used for this ArrayBuffer.
     */
    public ByteBuffer getBackingStore() {
        v8.checkReleased();
        v8.checkThread();
        return byteBuffer;
    }

}
