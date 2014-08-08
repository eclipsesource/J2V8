package com.eclipsesource.v8;


public class V8Array {

    private static int v8ArrayInstanceCounter = 1;

    private V8         v8;
    private int        arrayHandle;

    public V8Array(final V8 v8) {
        this.v8 = v8;
        this.v8.checkThread();
        arrayHandle = v8ArrayInstanceCounter++;
        this.v8._initNewV8Array(v8.getV8RuntimeHandle(), arrayHandle);
        this.v8.addObjRef();
    }

    public void release() {
        v8.checkThread();
        v8._releaseArray(v8.getV8RuntimeHandle(), arrayHandle);
        v8.releaseObjRef();
    }

    public int getSize() {
        v8.checkThread();
        return 0;
    }

    public int getValueType(final int index) {
        v8.checkThread();
        return 0;
    }

    public int getInteger(final int index) {
        v8.checkThread();
        return 0;
    }

    public boolean getBoolean(final int index) {
        v8.checkThread();
        return false;
    }

    public double getDouble(final int index) {
        v8.checkThread();
        return 0.0;
    }

    public String getString(final int index) {
        v8.checkThread();
        return null;
    }

    public V8Array getArray(final int index) {
        v8.checkThread();
        return null;
    }

    public V8Object getObject(final int index) {
        v8.checkThread();
        return null;
    }

    public void add(final int value) {
        v8.checkThread();
    }

    public void add(final boolean value) {
        v8.checkThread();
    }

    public void add(final double value) {
        v8.checkThread();
    }

    public void add(final String value) {
        v8.checkThread();
    }

    public void addObject(final V8Object value) {
        v8.checkThread();
    }

    public void addArray(final int size, final V8Array value) {
        v8.checkThread();
    }

}