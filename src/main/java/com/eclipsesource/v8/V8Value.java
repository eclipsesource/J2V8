/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

abstract public class V8Value {

    public static final int NULL        = 0;
    public static final int UNKNOWN     = 0;
    public static final int INTEGER     = 1;
    public static final int DOUBLE      = 2;
    public static final int BOOLEAN     = 3;
    public static final int STRING      = 4;
    public static final int V8_ARRAY    = 5;
    public static final int V8_OBJECT   = 6;
    public static final int V8_FUNCTION = 7;
    public static final int UNDEFINED   = 99;

    protected static int    v8ObjectInstanceCounter = 1;
    protected V8            v8;
    protected int           objectHandle;
    protected boolean       released                = true;

    public V8Value() {
        super();
    }

    protected void initialize(final long runtimePtr, final int objectHandle) {
        v8.initNewV8Object(runtimePtr, objectHandle);
        v8.addObjRef();
        released = false;
    }

    public boolean isUndefined() {
        return false;
    }

    public int getHandle() {
        return objectHandle;
    }

    public V8 getRutime() {
        return v8;
    }

    public void release() {
        v8.checkThread();
        if ( !released ) {
            released = true;
            v8.release(v8.getV8RuntimePtr(), objectHandle);
            v8.releaseObjRef();
        }
    }

    @Override
    public boolean equals(final Object that) {
        v8.checkThread();
        checkReleaesd();
        if (that == this) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (!(that instanceof V8Value)) {
            return false;
        }
        if (isUndefined() && ((V8Value) that).isUndefined()) {
            return true;
        }
        if (((V8Value) that).isUndefined()) {
            return false;
        }
        return v8.equals(v8.getV8RuntimePtr(), getHandle(), ((V8Value) that).getHandle());
    }

    public boolean strictEquals(final Object that) {
        v8.checkThread();
        checkReleaesd();
        if (that == this) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (!(that instanceof V8Value)) {
            return false;
        }
        if (isUndefined() && ((V8Value) that).isUndefined()) {
            return true;
        }
        if (((V8Value) that).isUndefined()) {
            return false;
        }
        return v8.strictEquals(v8.getV8RuntimePtr(), getHandle(), ((V8Value) that).getHandle());
    }

    public boolean sameValue(final Object that) {
        v8.checkThread();
        checkReleaesd();
        if (that == this) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (!(that instanceof V8Value)) {
            return false;
        }
        if (isUndefined() && ((V8Value) that).isUndefined()) {
            return true;
        }
        if (((V8Value) that).isUndefined()) {
            return false;
        }
        return v8.sameValue(v8.getV8RuntimePtr(), getHandle(), ((V8Value) that).getHandle());
    }

    @Override
    public int hashCode() {
        v8.checkThread();
        checkReleaesd();
        return v8.identityHash(v8.getV8RuntimePtr(), getHandle());
    }

    public boolean isReleased() {
        return released;
    }

    @Override
    public abstract String toString();

    protected void checkReleaesd() {
        if (released) {
            throw new IllegalStateException("Object released");
        }
    }

}