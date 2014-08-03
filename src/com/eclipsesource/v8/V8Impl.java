package com.eclipsesource.v8;

import java.util.Collection;

class V8Impl extends V8 {

    Thread thread = null;

    static {
        System.loadLibrary("j2v8"); // Load native library at runtime
    }

    V8Impl() {
        thread = Thread.currentThread();
        _createIsolate();
    }

    @Override
    public void release() {
        checkThread();
        _release();
    }

    @Override
    public boolean contains(final String key) {
        checkThread();
        return _contains(key);
    }

    @Override
    public Collection<String> getKeys() {
        checkThread();
        return _getKeys();
    }

    @Override
    public int getType(final String key) {
        checkThread();
        return _getType(key);
    }

    @Override
    public int getInteger(final String key) {
        checkThread();
        return _getInteger(key);
    }

    @Override
    public boolean getBoolean(final String key) {
        checkThread();
        return false;
    }

    @Override
    public double getDouble(final String key) {
        checkThread();
        return 0;
    }

    @Override
    public String getString(final String key) {
        checkThread();
        return null;
    }

    @Override
    public V8Array getArray(final String key) {
        checkThread();
        return null;
    }

    @Override
    public V8Object getObject(final String key) {
        checkThread();
        return null;
    }

    @Override
    public V8Array createParameterList(final int size) {
        checkThread();
        return null;
    }

    @Override
    public int executeIntFunction(final String name, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return 0;
    }

    @Override
    public double executeDoubleFunction(final String name, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return 0;
    }

    @Override
    public String executeStringFunction(final String name, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public boolean executeBooleanFunction(final String name, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return false;
    }

    @Override
    public V8Array executeArrayFunction(final String name, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public V8Object executeObjectFunction(final String name, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public void executeVoidFunction(final String name, final V8Array parameters) throws V8RuntimeException {
        checkThread();

    }

    @Override
    public void add(final String key, final int value) {
        checkThread();

    }

    @Override
    public void add(final String key, final boolean value) {
        checkThread();
    }

    @Override
    public void add(final String key, final double value) {
        checkThread();
    }

    @Override
    public void add(final String key, final String value) {
        checkThread();
    }

    @Override
    public V8Object addObject(final String key) {
        checkThread();
        return null;
    }

    @Override
    public V8Array addArray(final String key, final int size) {
        checkThread();
        return null;
    }

    @Override
    public void registerJavaMethod(final Object object, final String methodName, final Class<?>[] parameterTypes) {
        checkThread();
    }

    @Override
    public int executeIntScript(final String script, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return 0;
    }

    @Override
    public double executeDoubleScript(final String script, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return 0;
    }

    @Override
    public String executeStringScript(final String script, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public boolean executeBooleanScript(final String script, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return false;
    }

    @Override
    public V8Array executeArrayScript(final String script, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public V8Object executeObjectScript(final String script, final V8Array parameters) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public void executeVoidScript(final String script, final V8Array parameters) throws V8RuntimeException {
        checkThread();
    }

    private void checkThread() {
        if (thread != Thread.currentThread()) {
            throw new Error("Invalid V8 thread access.");
        }

    }

    private native void _createIsolate();

    public native void _release();

    public native boolean _contains(final String key);

    public native Collection<String> _getKeys();

    public native int _getType(final String key);

    public native int _getInteger(final String key);

    public native boolean _getBoolean(final String key);

    public native double _getDouble(final String key);

    public native String _getString(final String key);

    public native V8Array _getArray(final String key);

    public native V8Object _getObject(final String key);

    public native V8Array _createParameterList(final int size);

    public native int _executeIntFunction(final String name, final V8Array parameters) throws V8RuntimeException;

    public native double _executeDoubleFunction(final String name, final V8Array parameters) throws V8RuntimeException;

    public native String _executeStringFunction(final String name, final V8Array parameters) throws V8RuntimeException;

    public native boolean _executeBooleanFunction(final String name, final V8Array parameters)
            throws V8RuntimeException;

    public native V8Array _executeArrayFunction(final String name, final V8Array parameters) throws V8RuntimeException;

    public native V8Object _executeObjectFunction(final String name, final V8Array parameters)
            throws V8RuntimeException;

    public native void _executeVoidFunction(final String name, final V8Array parameters) throws V8RuntimeException;

    public native void _add(final String key, final int value);

    public native void _add(final String key, final boolean value);

    public native void _add(final String key, final double value);

    public native void _add(final String key, final String value);

    public native V8Object _addObject(final String key);

    public native V8Array _addArray(final String key, final int size);

    public native void _registerJavaMethod(final Object object, final String methodName, final Class<?>[] parameterTypes);

    public native int _executeIntScript(final String script, final V8Array parameters) throws V8RuntimeException;

    public native double _executeDoubleScript(final String script, final V8Array parameters) throws V8RuntimeException;

    public native String _executeStringScript(final String script, final V8Array parameters) throws V8RuntimeException;

    public native boolean _executeBooleanScript(final String script, final V8Array parameters)
            throws V8RuntimeException;

    public native V8Array _executeArrayScript(final String script, final V8Array parameters) throws V8RuntimeException;

    public native V8Object _executeObjectScript(final String script, final V8Array parameters)
            throws V8RuntimeException;

    public native void _executeVoidScript(final String script, final V8Array parameters) throws V8RuntimeException;
}
