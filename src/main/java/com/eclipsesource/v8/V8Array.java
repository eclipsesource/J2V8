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

public class V8Array extends V8Object {

    protected V8Array() {

    }

    public V8Array(final V8 v8) {
        super(v8);
        v8.checkThread();
    }

    @Override
    protected V8Value createTwin(final int newHandle) {
        return new V8Array(v8, newHandle);
    }

    @Override
    public V8Array twin() {
        return (V8Array) super.twin();
    }

    protected V8Array(final V8 v8, final int objectHandle) {
        super(v8, objectHandle);
    }

    @Override
    protected void initialize(final long runtimePtr, final int objectHandle) {
        v8.initNewV8Array(runtimePtr, objectHandle);
        v8.addObjRef();
        released = false;
    }

    public int length() {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetSize(v8.getV8RuntimePtr(), getHandle());
    }

    public int getType(final int index) {
        v8.checkThread();
        checkReleaesd();
        return v8.getType(v8.getV8RuntimePtr(), getHandle(), index);
    }

    public int getType() {
        v8.checkThread();
        checkReleaesd();
        return v8.getArrayType(v8.getV8RuntimePtr(), getHandle());
    }

    public int getType(final int index, final int length) {
        v8.checkThread();
        checkReleaesd();
        return v8.getType(v8.getV8RuntimePtr(), getHandle(), index, length);
    }

    public int getInteger(final int index) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetInteger(v8.getV8RuntimePtr(), getHandle(), index);
    }

    public boolean getBoolean(final int index) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetBoolean(v8.getV8RuntimePtr(), getHandle(), index);
    }

    public double getDouble(final int index) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetDouble(v8.getV8RuntimePtr(), getHandle(), index);
    }

    public String getString(final int index) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetString(v8.getV8RuntimePtr(), getHandle(), index);
    }

    public int[] getIntegers(final int index, final int length) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetIntegers(v8.getV8RuntimePtr(), getHandle(), index, length);
    }

    public int getIntegers(final int index, final int length, final int[] resultArray) {
        v8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8.arrayGetIntegers(v8.getV8RuntimePtr(), getHandle(), index, length, resultArray);
    }

    public double[] getDoubles(final int index, final int length) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetDoubles(v8.getV8RuntimePtr(), getHandle(), index, length);
    }

    public int getDoubles(final int index, final int length, final double[] resultArray) {
        v8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8.arrayGetDoubles(v8.getV8RuntimePtr(), getHandle(), index, length, resultArray);
    }

    public boolean[] getBooleans(final int index, final int length) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetBooleans(v8.getV8RuntimePtr(), getHandle(), index, length);
    }

    public int getBooleans(final int index, final int length, final boolean[] resultArray) {
        v8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8.arrayGetBooleans(v8.getV8RuntimePtr(), getHandle(), index, length, resultArray);
    }

    public String[] getStrings(final int index, final int length) {
        v8.checkThread();
        checkReleaesd();
        return v8.arrayGetStrings(v8.getV8RuntimePtr(), getHandle(), index, length);
    }

    public int getStrings(final int index, final int length, final String[] resultArray) {
        v8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8.arrayGetStrings(v8.getV8RuntimePtr(), getHandle(), index, length, resultArray);
    }

    public Object get(final int index) {
        int type = getType(index);
        switch (type) {
            case INTEGER:
                return getInteger(index);
            case DOUBLE:
                return getDouble(index);
            case BOOLEAN:
                return getBoolean(index);
            case STRING:
                return getString(index);
            case V8_ARRAY:
                return getArray(index);
            case V8_FUNCTION:
            case V8_OBJECT:
                return getObject(index);
        }
        return null;
    }

    public V8Array getArray(final int index) {
        v8.checkThread();
        checkReleaesd();
        Object result = v8.arrayGet(v8.getV8RuntimePtr(), V8_ARRAY, objectHandle, index);
        if ((result == null) || (result instanceof V8Array)) {
            return (V8Array) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Object getObject(final int index) {
        v8.checkThread();
        checkReleaesd();
        Object result = v8.arrayGet(v8.getV8RuntimePtr(), V8_OBJECT, objectHandle, index);
        if ((result == null) || (result instanceof V8Object)) {
            return (V8Object) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Array push(final int value) {
        v8.checkThread();
        checkReleaesd();
        v8.addArrayIntItem(v8.getV8RuntimePtr(), getHandle(), value);
        return this;
    }

    public V8Array push(final boolean value) {
        v8.checkThread();
        checkReleaesd();
        v8.addArrayBooleanItem(v8.getV8RuntimePtr(), getHandle(), value);
        return this;
    }

    public V8Array push(final double value) {
        v8.checkThread();
        checkReleaesd();
        v8.addArrayDoubleItem(v8.getV8RuntimePtr(), getHandle(), value);
        return this;
    }

    public V8Array push(final String value) {
        v8.checkThread();
        checkReleaesd();
        if (value == null) {
            v8.addArrayNullItem(v8.getV8RuntimePtr(), getHandle());
        } else if (value.equals(V8.getUndefined())) {
            v8.addArrayUndefinedItem(v8.getV8RuntimePtr(), getHandle());
        } else {
            v8.addArrayStringItem(v8.getV8RuntimePtr(), getHandle(), value);
        }
        return this;
    }

    public V8Array push(final V8Value value) {
        v8.checkThread();
        checkReleaesd();
        if (value == null) {
            v8.addArrayNullItem(v8.getV8RuntimePtr(), getHandle());
        } else if (value.equals(V8.getUndefined())) {
            v8.addArrayUndefinedItem(v8.getV8RuntimePtr(), getHandle());
        } else {
            v8.addArrayObjectItem(v8.getV8RuntimePtr(), getHandle(), value.getHandle());
        }
        return this;
    }

    public V8Array pushNull() {
        v8.checkThread();
        checkReleaesd();
        v8.addArrayNullItem(v8.getV8RuntimePtr(), getHandle());
        return this;
    }

    public V8Array pushUndefined() {
        v8.checkThread();
        checkReleaesd();
        v8.addArrayUndefinedItem(v8.getV8RuntimePtr(), getHandle());
        return this;
    }

    static class Undefined extends V8Array {

        public Undefined() {
        }

        @Override
        public boolean isUndefined() {
            return true;
        }

        @Override
        public boolean isReleased() {
            return true;
        }

        @Override
        public void release() {
        }

        @Override
        public Undefined twin() {
            return (Undefined) super.twin();
        }

        @Override
        public String toString() {
            return "undefined";
        }

        @Override
        public boolean equals(final Object that) {
            if ((that instanceof V8Object) && ((V8Object) that).isUndefined()) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 919;
        }

        @Override
        public V8 getRutime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final V8Value value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object addUndefined(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array executeArrayFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean executeBooleanFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double executeDoubleFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeIntegerFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object executeObjectFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String executeStringFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void executeVoidFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array getArray(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBoolean(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDouble(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getHandle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInteger(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object getObject(final String key) throws V8ResultUndefined {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(final String key) throws V8ResultUndefined {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final String key) throws V8ResultUndefined {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaCallback callback, final String jsFunctionName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaVoidCallback callback, final String jsFunctionName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final Object object, final String methodName, final String jsFunctionName, final Class<?>[] parameterTypes, final boolean includeReceiver) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object setPrototype(final V8Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array getArray(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBoolean(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean[] getBooleans(final int index, final int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBooleans(final int index, final int length, final boolean[] resultArray) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDouble(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double[] getDoubles(final int index, final int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getDoubles(final int index, final int length, final double[] resultArray) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInteger(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int[] getIntegers(final int index, final int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIntegers(final int index, final int length, final int[] resultArray) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object getObject(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getStrings(final int index, final int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStrings(final int index, final int length, final String[] resultArray) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final int index, final int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int length() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final V8Value value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array pushUndefined() {
            throw new UnsupportedOperationException();
        }

    }

}