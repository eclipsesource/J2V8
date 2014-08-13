package com.eclipsesource.v8;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class V8 extends V8Object {

    private static int v8InstanceCounter;
    private int        methodReferenceCounter = 0;

    private int        v8RuntimeHandle;
    Thread             thread = null;
    long               objectReferences = 0;

    class MethodDescriptor {
        Object object;
        Method method;
    }

    Map<Integer, MethodDescriptor> functions = new HashMap<>();

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

    public void registerCallback(final Object object, final Method method, final int methodType, final int objectHandle,
            final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.object = object;
        methodDescriptor.method = method;
        int methodID = methodReferenceCounter++;
        functions.put(methodID, methodDescriptor);
        _registerJavaMethod(getV8RuntimeHandle(), objectHandle, jsFunctionName, methodID, V8Object.VOID);
    }

    protected void callVoidJavaMethod(final int methodID, final V8Array parameters) {
        MethodDescriptor methodDescriptor = functions.get(methodID);
        int size = parameters == null ? 0 : parameters.getSize();
        Object[] args = new Object[size];
        for (int i = 0; i < size; i++) {
            args[i] = getArrayItem(parameters, i);
        }
        try {
            methodDescriptor.method.invoke(methodDescriptor.object, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            // TODO: Handle exception
        } finally {
            for (int i = 0; i < size; i++) {
                if (args[i] instanceof V8Array) {
                    ((V8Array) args[i]).release();
                } else if (args[i] instanceof V8Object) {
                    ((V8Object) args[i]).release();
                }
            }
        }
    }

    private Object getArrayItem(final V8Array array, final int index) {
        int type = array.getType(index);
        switch (type) {
            case INTEGER:
                return array.getInteger(index);
            case DOUBLE:
                return array.getDouble(index);
            case BOOLEAN:
                return array.getBoolean(index);
            case STRING:
                return array.getString(index);
            case V8_ARRAY:
                return array.getArray(index);
            case V8_OBJECT:
                return array.getObject(index);
        }
        return null;
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

    protected native String[] _getKeys(int v8RuntimeHandle, int objectHandle);

    protected native int _getInteger(int v8RuntimeHandle, int objectHandle, final String key);

    protected native boolean _getBoolean(int v8RuntimeHandle, int objectHandle, final String key);

    protected native double _getDouble(int v8RuntimeHandle, int objectHandle, final String key);

    protected native String _getString(int v8RuntimeHandle, int objectHandle, final String key);

    protected native void _getArray(int v8RuntimeHandle, int objectHandle, String key, int resultHandle);

    protected native void _getObject(int v8RuntimeHandle, final int objectHandle, final String key,  final int resultObjectHandle);

    protected native int _executeIntFunction(int v8RuntimeHandle, int objectHandle, String name, int parametersHandle);

    protected native double _executeDoubleFunction(int v8RuntimeHandle, int objectHandle, String name,
            int parametersHandle);

    protected native String _executeStringFunction(int v8RuntimeHandle2, int handle, String name, int parametersHandle);

    protected native boolean _executeBooleanFunction(int v8RuntimeHandle2, int handle, String name, int parametersHandle);

    protected native void _executeArrayFunction(int v8RuntimeHandle, int objectHandle, String name,
            int parametersHandle, int resultHandle);

    protected native void _executeObjectFunction(int v8RuntimeHandle, int objectHandle, final String name,
            final int parametersHandle, int resultHandle);

    protected native void _executeVoidFunction(int v8RuntimeHandle, int objectHandle, final String name,
            final int parametersHandle);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _addObject(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final boolean value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final double value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final String value);

    protected native void _addArray(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _registerJavaMethod(int v8RuntimeHandle, final int objectHandle, final String functionName,
            final int methodID, final int methodType);

    protected native void _initNewV8Array(int v8RuntimeHandle, int arrayHandle);

    protected native void _releaseArray(int v8RuntimeHandle, int arrayHandle);

    protected native int _arrayGetSize(int v8RuntimeHandle, int arrayHandle);

    protected native int _arrayGetInteger(int v8RuntimeHandle, int arrayHandle, int index);

    protected native boolean _arrayGetBoolean(int v8RuntimeHandle, int arrayHandle, int index);

    protected native double _arrayGetDouble(int v8RuntimeHandle, int arrayHandle, int index);

    protected native String _arrayGetString(int v8RuntimeHandle, int arrayHandle, int index);

    protected native void _arrayGetObject(final int v8RuntimeHandle, final int arrayHandle, final int index,
            final int resultHandle);

    protected native void _arrayGetArray(int v8RuntimeHandle, int arrayHandle, int index, int resultHandle);

    protected native void _addArrayIntItem(int v8RuntimeHandle, int arrayHandle, int value);

    protected native void _addArrayBooleanItem(int v8RuntimeHandle, int arrayHandle, boolean value);

    protected native void _addArrayDoubleItem(int v8RuntimeHandle, int arrayHandle, double value);

    protected native void _addArrayStringItem(int v8RuntimeHandle, int arrayHandle, String value);

    protected native void _addArrayArrayItem(int v8RuntimeHandle, int arrayHandle, int value);

    protected native void _addArrayObjectItem(int v8RuntimeHandle, int arrayHandle, int value);

    protected native int _getType(int v8RuntimeHandle, int objectHandle, final String key);

    protected native int _getType(int v8RuntimeHandle, int objectHandle, final int index);

    public void addObjRef() {
        objectReferences++;
    }

    public void releaseObjRef() {
        objectReferences--;
    }

}
