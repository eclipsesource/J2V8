package com.eclipsesource.v8;


public class V8 extends V8Object {

    private static int v8InstanceCounter;

    private int        v8RuntimeHandle;
    Thread             thread = null;
    long               objectReferences = 0;

    static {
        System.loadLibrary("j2v8"); // Load native library at runtime
    }

    public V8() {
        thread = Thread.currentThread();
        v8RuntimeHandle = v8InstanceCounter++;
        _createIsolate(v8RuntimeHandle);
    }

    public int getV8RuntimeHandle() {
        return v8RuntimeHandle;
    }

    @Override
    public void release() {
        checkThread();
        if (objectReferences > 0) {
            throw new IllegalStateException(objectReferences + " Object(s) still exist in runtime");
        }
        _releaseRuntime(v8RuntimeHandle);
    }

    public int executeIntScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeIntScript(v8RuntimeHandle, script);
    }

    public double executeDoubleScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeDoubleScript(v8RuntimeHandle, script);
    }

    public String executeStringScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeStringScript(v8RuntimeHandle, script);
    }

    public boolean executeBooleanScript(final String script) throws V8RuntimeException {
        checkThread();
        return _executeBooleanScript(v8RuntimeHandle, script);
    }

    public V8Array executeArrayScript(final String script) throws V8RuntimeException {
        checkThread();
        V8Array result = new V8Array(this);
        try {
            _executeArrayScript(getV8RuntimeHandle(), script, result.getHandle());
        } catch (Exception e) {
            result.release();
            throw e;
        }
        return result;
    }

    public V8Object executeObjectScript(final String script) throws V8RuntimeException {
        checkThread();
        V8Object result = new V8Object(this);
        try {
            _executeObjectScript(getV8RuntimeHandle(), script, result.getHandle());
        } catch (Exception e) {
            result.release();
            throw e;
        }
        return result;
    }

    public void executeVoidScript(final String script) throws V8RuntimeException {
        checkThread();
        _executeVoidScript(v8RuntimeHandle, script);
    }

    void checkThread() {
        if (thread != Thread.currentThread()) {
            throw new Error("Invalid V8 thread access.");
        }
    }

    protected native void _initExistingV8Object(int v8RuntimeHandle, int parentHandle, String objectKey,
            int objectHandle);

    protected native void _initNewV8Object(int v8RuntimeHandle, int objectHandle);

    protected native void _releaseRuntime(int v8RuntimeHandle);

    protected native void _createIsolate(int v8RuntimeHandle);

    protected native int _executeIntScript(int v8RuntimeHandle, final String script) throws V8RuntimeException;

    protected native double _executeDoubleScript(int v8RuntimeHandle, final String script) throws V8RuntimeException;

    protected native String _executeStringScript(int v8RuntimeHandle, final String script) throws V8RuntimeException;

    protected native boolean _executeBooleanScript(int v8RuntimeHandle, final String script) throws V8RuntimeException;

    protected native void _executeObjectScript(int v8RuntimeHandle, final String script, final int resultHandle)
            throws V8RuntimeException;

    protected native void _executeVoidScript(int v8RuntimeHandle, final String script) throws V8RuntimeException;

    protected native void _executeArrayScript(int v8RuntimeHandle, String script, int resultHandle);

    protected native void _release(int v8RuntimeHandle, int objectHandle);

    protected native boolean _contains(int v8RuntimeHandle, int objectHandle, final String key);

    protected native String[] _getKeys(int v8RuntimeHandle);

    protected native int _getType(int v8RuntimeHandle, final String key);

    protected native int _getInteger(int v8RuntimeHandle, int objectHandle, final String key);

    protected native boolean _getBoolean(int v8RuntimeHandle, int objectHandle, final String key);

    protected native double _getDouble(int v8RuntimeHandle, int objectHandle, final String key);

    protected native String _getString(int v8RuntimeHandle, int objectHandle, final String key);

    protected native V8Array _getArray(int v8RuntimeHandle, final String key);

    protected native void _getObject(int v8RuntimeHandle, final int objectHandle, final String key,  final int resultObjectHandle);

    protected native V8Array _createParameterList(int v8RuntimeHandle, final int size);

    protected native int _executeIntFunction(int v8RuntimeHandle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    protected native double _executeDoubleFunction(int v8RuntimeHandle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    protected native String _executeStringFunction(int v8RuntimeHandle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    protected native boolean _executeBooleanFunction(int v8RuntimeHandle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    protected native V8Array _executeArrayFunction(int v8RuntimeHandle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    protected native void _executeObjectFunction(int v8RuntimeHandle, int objectHandle, final String name,
            final V8Array parameters, int resultHandle) throws V8RuntimeException;

    protected native void _executeVoidFunction(int v8RuntimeHandle, final String name, final V8Array parameters)
            throws V8RuntimeException;

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _addObject(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final boolean value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final double value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final String value);

    protected native void _addArray(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _registerJavaMethod(int v8RuntimeHandle, final Object object, final String methodName,
            final Class<?>[] parameterTypes);

    public native void _initNewV8Array(int v8RuntimeHandle, int arrayHandle);

    public native void _releaseArray(int v8RuntimeHandle, int arrayHandle);

    protected native int _arrayGetSize(int v8RuntimeHandle, int arrayHandle);

    /**
     *
     */
    public void addObjRef() {
        objectReferences++;
    }

    /**
     *
     */
    public void releaseObjRef() {
        objectReferences--;
    }

}
