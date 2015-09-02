/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Contributors:
 * EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

/**
 * A base class for all V8 resources. V8 resources must
 * be released. The rules for releasing resources is as
 * follows:
 * <p>
 * 1. If you created it, you must release it, with one exception;
 * if the object is being passed pack via a return statement,
 * the system will release it for you.
 * <p>
 * 2. If the system created it, you don’t need to worry about it,
 * with one caveat; if the object was returned to you as a
 * result of a method call, you must release it.
 */
abstract public class V8Value implements Releasable {

    public static final int NULL = 0;
    public static final int UNKNOWN = 0;
    public static final int INTEGER = 1;
    public static final int DOUBLE = 2;
    public static final int BOOLEAN = 3;
    public static final int STRING = 4;
    public static final int V8_ARRAY = 5;
    public static final int V8_OBJECT = 6;
    public static final int V8_FUNCTION = 7;
    public static final int UNDEFINED = 99;

    protected static int v8ObjectInstanceCounter = 1;
    protected V8 v8;
    protected int objectHandle;
    protected boolean released = true;

    protected V8Value() {
        super();
    }

    protected V8Value(final V8 v8) {
        this.v8 = v8;
        objectHandle = v8.v8ObjectInstanceCounter++;
    }

    protected V8Value(final V8 v8, final int objectHandle) {
        if (v8 == null) {
            this.v8 = (V8) this;
        } else {
            this.v8 = v8;
            v8.addObjRef();
        }
        this.objectHandle = objectHandle;
        released = false;
    }

    protected void initialize(final long runtimePtr, final int objectHandle) {
        v8.initNewV8Object(runtimePtr, objectHandle);
        v8.addObjRef();
        released = false;
    }

    /**
     * Determines if this value is undefined.
     *
     * @return Returns true if the value is undefined, false otherwise
     */
    public boolean isUndefined() {
        return false;
    }

    /**
     * Gets the runtime this Value was created on.
     *
     * @return Returns the V8 runtime this value is associated with.
     */
    public V8 getRutime() {
        return v8;
    }

    /**
     * Creates a new Java object pointing at the same V8 Value
     * as this. If the value is mutated (by adding new members or
     * changing existing ones) then both the original and twin
     * will be updated. Twins are .equal and .strict equals, but
     * not == in Java.
     * <p>
     * Twins must be released separately since they have their own
     * native resources.
     *
     * @return A new Java object pointing at the same V8 Value
     * as this.
     */
    public V8Value twin() {
        if (isUndefined()) {
            return this;
        }
        v8.checkThread();
        v8.checkReleaesd();
        int twinHandle = v8.v8ObjectInstanceCounter++;
        v8.createTwin(this, twinHandle);
        return createTwin(twinHandle);
    }

    /**
     * Releases the native resources associated with this V8Value.
     */
    @Override
    public void release() {
        v8.checkThread();
        if (!released) {
            released = true;
            v8.release(v8.getV8RuntimePtr(), objectHandle);
            v8.releaseObjRef();
        }
    }

    /**
     * Determine if the native resources have been released. Once released
     * a V8 Value can no longer be used.
     *
     * @return Returns true if this object has been released, false otherwise.
     */
    public boolean isReleased() {
        return released;
    }

    /**
     * Performs a JS === on the parameter and the receiver.
     *
     * @return Returns true iff this === that
     */
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

    protected int getHandle() {
        checkReleaesd();
        return objectHandle;
    }

    protected abstract V8Value createTwin(int twinObjectHandle);

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object that) {
        return strictEquals(that);
    }

    /**
     * Performs a JS == on the parameter and the receiver.
     *
     * @return Returns true iff this == that
     */
    public boolean jsEquals(final Object that) {
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

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        v8.checkThread();
        checkReleaesd();
        return v8.identityHash(v8.getV8RuntimePtr(), getHandle());
    }

    protected void checkReleaesd() {
        if (released) {
            throw new IllegalStateException("Object released");
        }
    }

}