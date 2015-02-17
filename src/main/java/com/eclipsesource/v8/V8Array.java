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
        V8.checkThread();
    }

    @Override
    protected void initialize(final int runtimeHandle, final int objectHandle) {
        v8._initNewV8Array(runtimeHandle, objectHandle);
        v8.addObjRef();
        released = false;
    }

    public int length() {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetSize(v8.getV8RuntimeHandle(), getHandle());
    }

    public int getType(final int index) {
        V8.checkThread();
        checkReleaesd();
        return v8._getType(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public int getType() {
        V8.checkThread();
        checkReleaesd();
        return v8._getArrayType(v8.getV8RuntimeHandle(), getHandle());
    }

    public int getType(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._getType(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public int getInteger(final int index) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetInteger(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public boolean getBoolean(final int index) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetBoolean(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public double getDouble(final int index) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetDouble(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public String getString(final int index) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetString(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public int[] getInts(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetInts(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public int getInts(final int index, final int length, final int[] resultArray) {
        V8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8._arrayGetInts(v8.getV8RuntimeHandle(), getHandle(), index, length, resultArray);
    }

    public double[] getDoubles(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetDoubles(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public int getDoubles(final int index, final int length, final double[] resultArray) {
        V8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8._arrayGetDoubles(v8.getV8RuntimeHandle(), getHandle(), index, length, resultArray);
    }

    public boolean[] getBooleans(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetBooleans(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public int getBooleans(final int index, final int length, final boolean[] resultArray) {
        V8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8._arrayGetBooleans(v8.getV8RuntimeHandle(), getHandle(), index, length, resultArray);
    }

    public String[] getStrings(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetStrings(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public int getStrings(final int index, final int length, final String[] resultArray) {
        V8.checkThread();
        checkReleaesd();
        if (length > resultArray.length) {
            throw new IndexOutOfBoundsException();
        }
        return v8._arrayGetStrings(v8.getV8RuntimeHandle(), getHandle(), index, length, resultArray);
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
            case V8_OBJECT:
                return getObject(index);
        }
        return null;
    }

    public V8Array getArray(final int index) {
        V8.checkThread();
        checkReleaesd();
        Object result = v8._arrayGet(v8.getV8RuntimeHandle(), V8_ARRAY, objectHandle, index);
        if ((result == null) || (result instanceof V8Array)) {
            return (V8Array) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Object getObject(final int index) {
        V8.checkThread();
        checkReleaesd();
        Object result = v8._arrayGet(v8.getV8RuntimeHandle(), V8_OBJECT, objectHandle, index);
        if ((result == null) || (result instanceof V8Object)) {
            return (V8Object) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Array push(final int value) {
        V8.checkThread();
        checkReleaesd();
        v8._addArrayIntItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final boolean value) {
        V8.checkThread();
        checkReleaesd();
        v8._addArrayBooleanItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final double value) {
        V8.checkThread();
        checkReleaesd();
        v8._addArrayDoubleItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final String value) {
        V8.checkThread();
        checkReleaesd();
        if( value == null ) {
            v8._addArrayNullItem(v8.getV8RuntimeHandle(), getHandle());
        } else if ( value.equals(V8.getUndefined())) {
            v8._addArrayUndefinedItem(v8.getV8RuntimeHandle(), getHandle());
        } else {
            v8._addArrayStringItem(v8.getV8RuntimeHandle(), getHandle(), value);
        }
        return this;
    }

    public V8Array push(final V8Value value) {
        V8.checkThread();
        checkReleaesd();
        if (value == null) {
            v8._addArrayNullItem(v8.getV8RuntimeHandle(), getHandle());
        } else if (value.equals(V8.getUndefined())) {
            v8._addArrayUndefinedItem(v8.getV8RuntimeHandle(), getHandle());
        } else {
            v8._addArrayObjectItem(v8.getV8RuntimeHandle(), getHandle(), value.getHandle());
        }
        return this;
    }

    public V8Array pushUndefined() {
        V8.checkThread();
        checkReleaesd();
        v8._addArrayUndefinedItem(v8.getV8RuntimeHandle(), getHandle());
        return this;
    }

    static class Undefined extends V8Array {

        @Override
        public boolean isUndefined() {
            V8.checkThread();
            return true;
        }

        @Override
        public boolean isReleased() {
            V8.checkThread();
            return true;
        }

        @Override
        public void release() {
            V8.checkThread();
        }

        @Override
        public String toString() {
            return "undefined";
        }

        @Override
        public boolean equals(final Object that) {
            V8.checkThread();
            if ((that instanceof V8Object) && ((V8Object) that).isUndefined()) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            V8.checkThread();
            return 919;
        }

        @Override
        public V8Object add(final String key, final boolean value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final double value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final int value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final String value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final V8Value value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object addUndefined(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array executeArrayFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean executeBooleanFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public double executeDoubleFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeIntFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object executeObjectFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String executeStringFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public void executeVoidFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array getArray(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBoolean(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDouble(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getHandle() {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInteger(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getKeys() {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object getObject(final String key) throws V8ResultUndefined {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(final String key) throws V8ResultUndefined {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final String key) throws V8ResultUndefined {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaCallback callback, final String jsFunctionName) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaVoidCallback callback, final String jsFunctionName) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final Object object, final String methodName, final String jsFunctionName, final Class<?>[] parameterTypes) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object setPrototype(final V8Object value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array getArray(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBoolean(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean[] getBooleans(final int index, final int length) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBooleans(final int index, final int length, final boolean[] resultArray) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDouble(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public double[] getDoubles(final int index, final int length) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getDoubles(final int index, final int length, final double[] resultArray) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInteger(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int[] getInts(final int index, final int length) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInts(final int index, final int length, final int[] resultArray) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object getObject(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getStrings(final int index, final int length) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStrings(final int index, final int length, final String[] resultArray) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType() {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final int index) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final int index, final int length) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int length() {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final boolean value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final double value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final int value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final String value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array push(final V8Value value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array pushUndefined() {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

    }

}