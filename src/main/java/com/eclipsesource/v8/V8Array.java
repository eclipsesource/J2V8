package com.eclipsesource.v8;

public class V8Array extends V8Object {

    public V8Array(final V8 v8) {
        this(v8, true);
    }

    protected V8Array(final V8 v8, final boolean initialize) {
        super(v8, initialize);
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

    public double[] getDoubles(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetDoubles(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public boolean[] getBooleans(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetBooleans(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public String[] getStrings(final int index, final int length) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetStrings(v8.getV8RuntimeHandle(), getHandle(), index, length);
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
        return v8._arrayGetArray(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public V8Object getObject(final int index) {
        V8.checkThread();
        checkReleaesd();
        return v8._arrayGetObject(v8.getV8RuntimeHandle(), getHandle(), index);
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
        v8._addArrayStringItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final V8Value value) {
        V8.checkThread();
        v8._addArrayObjectItem(v8.getV8RuntimeHandle(), getHandle(), value.getHandle());
        return this;
    }

    public V8Array pushUndefined() {
        V8.checkThread();
        checkReleaesd();
        v8._addArrayUndefinedItem(v8.getV8RuntimeHandle(), getHandle());
        return this;
    }

}