package com.eclipsesource.v8;

public class V8Array extends V8Object {

    public V8Array(final V8 v8) {
        super(v8);
        V8.checkThread();
    }

    @Override
    protected void initialize(final int runtimeHandle, final int objectHandle) {
        v8._initNewV8Array(runtimeHandle, objectHandle);
    }

    public int length() {
        V8.checkThread();
        return v8._arrayGetSize(v8.getV8RuntimeHandle(), getHandle());
    }

    public int getType(final int index) {
        V8.checkThread();
        return v8._getType(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public int getInteger(final int index) {
        V8.checkThread();
        return v8._arrayGetInteger(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public boolean getBoolean(final int index) {
        V8.checkThread();
        return v8._arrayGetBoolean(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public double getDouble(final int index) {
        V8.checkThread();
        return v8._arrayGetDouble(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public String getString(final int index) {
        V8.checkThread();
        return v8._arrayGetString(v8.getV8RuntimeHandle(), getHandle(), index);
    }

    public int[] getInts(final int index, final int length) {
        V8.checkThread();
        return v8._arrayGetInts(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public double[] getDoubles(final int index, final int length) {
        V8.checkThread();
        return v8._arrayGetDoubles(v8.getV8RuntimeHandle(), getHandle(), index, length);
    }

    public V8Array getArray(final int index) {
        V8.checkThread();
        V8Array result = new V8Array(v8);
        try {
            v8._arrayGetArray(v8.getV8RuntimeHandle(), getHandle(), index, result.getHandle());
        } catch (Exception e) {
            result.release();
            throw e;
        }
        return result;
    }

    public V8Object getObject(final int index) {
        V8.checkThread();
        V8Object result = new V8Object(v8);
        try {
            v8._arrayGetObject(v8.getV8RuntimeHandle(), getHandle(), index, result.getHandle());
        } catch (Exception e) {
            result.release();
            throw e;
        }
        return result;
    }

    public V8Array push(final int value) {
        V8.checkThread();
        v8._addArrayIntItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final boolean value) {
        V8.checkThread();
        v8._addArrayBooleanItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final double value) {
        V8.checkThread();
        v8._addArrayDoubleItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final String value) {
        V8.checkThread();
        v8._addArrayStringItem(v8.getV8RuntimeHandle(), getHandle(), value);
        return this;
    }

    public V8Array push(final V8Object value) {
        V8.checkThread();
        v8._addArrayObjectItem(v8.getV8RuntimeHandle(), getHandle(), value.getHandle());
        return this;
    }

    public V8Array push(final V8Array value) {
        V8.checkThread();
        v8._addArrayArrayItem(v8.getV8RuntimeHandle(), getHandle(), value.getHandle());
        return this;
    }

    public V8Array pushUndefined() {
        V8.checkThread();
        v8._addArrayUndefinedItem(v8.getV8RuntimeHandle(), getHandle());
        return this;
    }

}