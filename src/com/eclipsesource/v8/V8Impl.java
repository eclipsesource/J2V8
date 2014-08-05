package com.eclipsesource.v8;

import java.util.Collection;

class V8Impl extends V8 {

    private static int v8InstanceCounter;

    private int        handle;
    Thread thread = null;

    static {
        System.loadLibrary("j2v8"); // Load native library at runtime
    }

    V8Impl() {
        thread = Thread.currentThread();
        handle = v8InstanceCounter++;
        _createIsolate(handle);
    }

    @Override
    public void release() {
        checkThread();
        _release(handle);
    }

    @Override
    public boolean contains(final String key) {
        checkThread();
        return false;
    }

    @Override
    public Collection<String> getKeys() {
        checkThread();
        return null;
    }

    @Override
    public int getType(final String key) {
        checkThread();
        return 0;
    }

    @Override
    public int getInteger(final String key) {
        checkThread();
        return 0;
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
        return _executeIntFunction(handle, name, parameters);
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
        _add(handle, key, value);
    }

    @Override
    public void add(final String key, final boolean value) {
        checkThread();
    }

    @Override
    public void add(final String key, final double value) {
        checkThread();
        _add(handle, key, value);
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
    public int executeIntScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeIntScript(handle, script);
    }

    @Override
    public double executeDoubleScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeDoubleScript(handle, script);
    }

    @Override
    public String executeStringScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeStringScript(handle, script);
    }

    @Override
    public boolean executeBooleanScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeBooleanScript(handle, script);
    }

    @Override
    public V8Array executeArrayScript(final String script) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public V8Object executeObjectScript(final String script) throws V8RuntimeException {
        checkThread();
        return null;
    }

    @Override
    public void executeVoidScript(final String script) throws V8RuntimeException {
        checkThread();
        _executeVoidScript(handle, script);
    }

    private void checkThread() {
        if (thread != Thread.currentThread()) {
            throw new Error("Invalid V8 thread access.");
        }

    }

    private native void _createIsolate(int handle);

    private native void _release(int handle);

    private native boolean _contains(int handle, final String key);

    private native Collection<String> _getKeys(int handle);

    private native int _getType(int handle, final String key);

    private native int _getInteger(int handle, final String key);

    private native boolean _getBoolean(int handle, final String key);

    private native double _getDouble(int handle, final String key);

    private native String _getString(int handle, final String key);

    private native V8Array _getArray(int handle, final String key);

    private native V8Object _getObject(int handle, final String key);

    private native V8Array _createParameterList(int handle, final int size);

    private native int _executeIntFunction(int handle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    private native double _executeDoubleFunction(int handle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    private native String _executeStringFunction(int handle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    private native boolean _executeBooleanFunction(int handle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    private native V8Array _executeArrayFunction(int handle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    private native V8Object _executeObjectFunction(int handle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    private native void _executeVoidFunction(int handle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    private native void _add(int handle, final String key, final int value);

    private native void _add(int handle, final String key, final boolean value);

    private native void _add(int handle, final String key, final double value);

    private native void _add(int handle, final String key, final String value);

    private native V8Object _addObject(int handle, final String key);

    private native V8Array _addArray(int handle, final String key, final int size);

    private native void _registerJavaMethod(int handle, final Object object, final String methodName,
            final Class<?>[] parameterTypes);

    private native int _executeIntScript(int handle, final String script) throws V8RuntimeException;

    private native double _executeDoubleScript(int handle, final String script) throws V8RuntimeException;

    private native String _executeStringScript(int handle, final String script) throws V8RuntimeException;

    private native boolean _executeBooleanScript(int handle, final String script) throws V8RuntimeException;

    private native V8Array _executeArrayScript(int handle, final String script) throws V8RuntimeException;

    private native V8Object _executeObjectScript(int handle, final String script) throws V8RuntimeException;

    private native void _executeVoidScript(int handle, final String script) throws V8RuntimeException;
}
