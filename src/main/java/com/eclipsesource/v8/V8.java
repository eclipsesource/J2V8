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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class V8 extends V8Object {

    private static int       v8InstanceCounter;
    private static Thread    thread                 = null;
    private static List<V8>  runtimes               = new ArrayList<>();
    private static Runnable  debugHandler           = null;

    private int              methodReferenceCounter = 0;
    private int              v8RuntimeHandle;
    private boolean          debugEnabled           = false;
    long                     objectReferences       = 0;

    private static boolean   nativeLibraryLoaded    = false;
    private static Error     nativeLoadError        = null;
    private static Exception nativeLoadException    = null;
    private static V8Value   undefined              = new V8Object.Undefined();

    class MethodDescriptor {
        Object           object;
        Method           method;
        JavaCallback     callback;
        JavaVoidCallback voidCallback;
    }

    Map<Integer, MethodDescriptor> functions = new HashMap<>();

    static {
        try {
            System.loadLibrary("j2v8"); // Load native library at runtime
            nativeLibraryLoaded = true;
        } catch (Error e) {
            nativeLoadError = e;
        } catch (Exception e) {
            nativeLoadException = e;
        }
    }

    public static boolean isEnabled() {
        return nativeLibraryLoaded;
    }

    public synchronized static V8 createV8Runtime() {
        return createV8Runtime(null);
    }

    public synchronized static V8 createV8Runtime(final String globalAlias) {
        checkNativeLibraryLoaded();
        if (thread == null) {
            thread = Thread.currentThread();
        }
        V8 runtime = new V8(globalAlias);
        runtimes.add(runtime);
        return runtime;
    }

    private static void checkNativeLibraryLoaded() {
        if (!nativeLibraryLoaded) {
            if (nativeLoadError != null) {
                throw new IllegalStateException("J2V8 native library not loaded.", nativeLoadError);
            } else if (nativeLoadException != null) {
                throw new IllegalStateException("J2V8 native library not loaded.", nativeLoadException);
            } else {
                throw new IllegalStateException("J2V8 native library not loaded.");
            }
        }
    }

    protected V8() {
        this(null);
    }

    protected V8(final String globalAlias) {
        super(null, 0);
        checkThread();
        v8RuntimeHandle = v8InstanceCounter++;
        _createIsolate(v8RuntimeHandle, globalAlias);
    }

    public static V8Value getUndefined() {
        return undefined;
    }

    public boolean enableDebugSupport(final int port, final boolean waitForConnection) {
        checkThread();
        debugEnabled = _enableDebugSupport(getHandle(), port, waitForConnection);
        return debugEnabled;
    }

    public boolean enableDebugSupport(final int port) {
        checkThread();
        debugEnabled = _enableDebugSupport(getV8RuntimeHandle(), port, false);
        return debugEnabled;
    }

    public void disableDebugSupport() {
        checkThread();
        _disableDebugSupport(getV8RuntimeHandle());
        debugEnabled = false;
    }

    public static void processDebugMessages() {
        checkThread();
        for (V8 v8 : runtimes) {
            v8._processDebugMessages(v8.getV8RuntimeHandle());
        }
    }

    public static int getActiveRuntimes() {
        return runtimes.size();
    }

    public static void registerDebugHandler(final Runnable handler) {
        debugHandler = handler;
    }

    public int getV8RuntimeHandle() {
        return v8RuntimeHandle;
    }

    @Override
    public void release() {
        release(true);
    }

    public void release(final boolean reportMemoryLeaks) {
        checkThread();
        if (debugEnabled) {
            disableDebugSupport();
        }
        runtimes.remove(this);
        _releaseRuntime(v8RuntimeHandle);
        if (reportMemoryLeaks && (objectReferences > 0)) {
            throw new IllegalStateException(objectReferences + " Object(s) still exist in runtime");
        }
    }

    public int executeIntScript(final String script) {
        return executeIntScript(script, null, 0);
    }

    public int executeIntScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        return _executeIntScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    public double executeDoubleScript(final String script) {
        return executeDoubleScript(script, null, 0);
    }

    public double executeDoubleScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        return _executeDoubleScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    public String executeStringScript(final String script) {
        return executeStringScript(script, null, 0);
    }

    public String executeStringScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        return _executeStringScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    public boolean executeBooleanScript(final String script) {
        return executeBooleanScript(script, null, 0);
    }

    public boolean executeBooleanScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        return _executeBooleanScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    public V8Array executeArrayScript(final String script) {
        return this.executeArrayScript(script, null, 0);
    }

    public V8Array executeArrayScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        Object result = this.executeScript(script, null, 0);
        if (result instanceof V8Array) {
            return (V8Array) result;
        }
        throw new V8ResultUndefined();
    }

    public Object executeScript(final String script) {
        return this.executeScript(script, null, 0);
    }

    public Object executeScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        return _executeScript(getV8RuntimeHandle(), UNKNOWN, script, scriptName, lineNumber);
    }

    public V8Object executeObjectScript(final String script) {
        return this.executeObjectScript(script, null, 0);
    }

    public V8Object executeObjectScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        Object result = this.executeScript(script, null, 0);
        if (result instanceof V8Object) {
            return (V8Object) result;
        }
        throw new V8ResultUndefined();
    }

    public void executeVoidScript(final String script) {
        this.executeVoidScript(script, null, 0);
    }

    public void executeVoidScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        _executeVoidScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    static void checkThread() {
        if ((thread != null) && (thread != Thread.currentThread())) {
            throw new Error("Invalid V8 thread access.");
        }
    }

    void registerCallback(final Object object, final Method method, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.object = object;
        methodDescriptor.method = method;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        _registerJavaMethod(getV8RuntimeHandle(), objectHandle, jsFunctionName, methodID, isVoidMethod(method));
    }

    void registerVoidCallback(final JavaVoidCallback callback, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.voidCallback = callback;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        _registerJavaMethod(getV8RuntimeHandle(), objectHandle, jsFunctionName, methodID, true);
    }

    void registerCallback(final JavaCallback callback, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.callback = callback;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        _registerJavaMethod(getV8RuntimeHandle(), objectHandle, jsFunctionName, methodID, false);
    }

    private boolean isVoidMethod(final Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(Void.TYPE)) {
            return true;
        }
        return false;
    }

    private Object getDefaultValue(final Class<?> type) {
        if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
            return 0;
        } else if (type.equals(Double.TYPE) || type.equals(Double.class)) {
            return 0d;
        } else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
            return false;
        } else {
            return null;
        }
    }

    protected Object callObjectJavaMethod(final int methodID, final V8Array parameters) throws Throwable {
        MethodDescriptor methodDescriptor = getFunctionRegistry().get(methodID);
        if (methodDescriptor.callback != null) {
            return checkResult(methodDescriptor.callback.invoke(parameters));
        }
        boolean hasVarArgs = methodDescriptor.method.isVarArgs();
        Object[] args = getArgs(methodDescriptor, parameters, hasVarArgs);
        try {
            Object result = methodDescriptor.method.invoke(methodDescriptor.object, args);
            return checkResult(result);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw e;
        } finally {
            releaseArguments(args, hasVarArgs);
        }
    }

    private Object checkResult(final Object result) {
        if (result == null) {
            return result;
        }
        if (result instanceof Float) {
            return ((Float) result).doubleValue();
        }
        if ((result instanceof Integer) || (result instanceof Double) || (result instanceof Boolean)
                || (result instanceof String) || (result instanceof V8Object) || (result instanceof V8Array)) {
            return result;
        }
        throw new V8RuntimeException("Unknown return type: " + result.getClass());
    }

    protected void callVoidJavaMethod(final int methodID, final V8Array parameters) throws Throwable {
        MethodDescriptor methodDescriptor = getFunctionRegistry().get(methodID);
        if (methodDescriptor.voidCallback != null) {
            methodDescriptor.voidCallback.invoke(parameters);
            return;
        }
        boolean hasVarArgs = methodDescriptor.method.isVarArgs();
        Object[] args = getArgs(methodDescriptor, parameters, hasVarArgs);
        try {
            methodDescriptor.method.invoke(methodDescriptor.object, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw e;
        } finally {
            releaseArguments(args, hasVarArgs);
        }
    }

    private void releaseArguments(final Object[] args, final boolean hasVarArgs) {
        if (hasVarArgs && ((args.length > 0) && (args[args.length - 1] instanceof Object[]))) {
            Object[] varArgs = (Object[]) args[args.length - 1];
            for (Object object : varArgs) {
                if (object instanceof V8Object) {
                    ((V8Value) object).release();
                }
            }
        }
        for (Object arg : args) {
            if (arg instanceof V8Object) {
                ((V8Value) arg).release();
            }
        }
    }

    private Object[] getArgs(final MethodDescriptor methodDescriptor, final V8Array parameters, final boolean hasVarArgs) {
        int numberOfParameters = methodDescriptor.method.getParameterTypes().length;
        int varArgIndex = hasVarArgs ? numberOfParameters - 1 : numberOfParameters;
        Object[] args = setDefaultValues(new Object[numberOfParameters], methodDescriptor.method.getParameterTypes());
        List<Object> varArgs = populateParamters(parameters, varArgIndex, args);
        if (hasVarArgs) {
            args[varArgIndex] = varArgs.toArray();
        }
        return args;
    }

    private List<Object> populateParamters(final V8Array parameters, final int varArgIndex, final Object[] args) {
        List<Object> varArgs = new ArrayList<>();
        for (int i = 0; i < parameters.length(); i++) {
            if (i >= varArgIndex) {
                varArgs.add(getArrayItem(parameters, i));
            }
            else {
                args[i] = getArrayItem(parameters, i);
            }
        }
        return varArgs;
    }

    private Object[] setDefaultValues(final Object[] parameters, final Class<?>[] parameterTypes) {
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = getDefaultValue(parameterTypes[i]);
        }
        return parameters;
    }

    private Object getArrayItem(final V8Array array, final int index) {
        try {
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
        } catch (V8ResultUndefined e) {
            // do nothing
        }
        return null;
    }

    protected static void debugMessageReceived() {
        if (debugHandler != null) {
            debugHandler.run();
        }
    }

    private Map<Integer, MethodDescriptor> getFunctionRegistry() {
        if (functions == null) {
            functions = new HashMap<Integer, V8.MethodDescriptor>();
        }
        return functions;
    }

    protected native void _initNewV8Object(int v8RuntimeHandle, int objectHandle);

    protected native void _releaseRuntime(int v8RuntimeHandle);

    protected native void _createIsolate(int v8RuntimeHandle, String globalAlias);

    protected native int _executeIntScript(int v8RuntimeHandle, final String script, final String scriptName,
            final int lineNumber);

    protected native double _executeDoubleScript(int v8RuntimeHandle, final String script, final String scriptName,
            final int lineNumber);

    protected native String _executeStringScript(int v8RuntimeHandle, final String script, final String scriptName,
            final int lineNumber);

    protected native boolean _executeBooleanScript(int v8RuntimeHandle, final String script, final String scriptName,
            final int lineNumber);

    protected native Object _executeScript(int v8RuntimeHandle, int expectedType, String script, String scriptName, int lineNumber);

    protected native void _executeVoidScript(int v8RuntimeHandle, String script, String scriptName, int lineNumber);

    protected native void _release(int v8RuntimeHandle, int objectHandle);

    protected native boolean _contains(int v8RuntimeHandle, int objectHandle, final String key);

    protected native String[] _getKeys(int v8RuntimeHandle, int objectHandle);

    protected native int _getInteger(int v8RuntimeHandle, int objectHandle, final String key);

    protected native boolean _getBoolean(int v8RuntimeHandle, int objectHandle, final String key);

    protected native double _getDouble(int v8RuntimeHandle, int objectHandle, final String key);

    protected native String _getString(int v8RuntimeHandle, int objectHandle, final String key);

    protected native Object _get(int v8RuntimeHandle, int expectedType, final int objectHandle, final String key);

    protected native int _executeIntFunction(int v8RuntimeHandle, int objectHandle, String name, int parametersHandle);

    protected native double _executeDoubleFunction(int v8RuntimeHandle, int objectHandle, String name,
            int parametersHandle);

    protected native String _executeStringFunction(int v8RuntimeHandle2, int handle, String name, int parametersHandle);

    protected native boolean _executeBooleanFunction(int v8RuntimeHandle2, int handle, String name, int parametersHandle);

    protected native Object _executeFunction(int v8RuntimeHandle, int expectedType, int objectHandle, String name, int parametersHandle);

    protected native void _executeVoidFunction(int v8RuntimeHandle, int objectHandle, final String name, final int parametersHandle);

    protected native boolean _equals(int v8RuntimeHandle, int objectHandle, int that);

    protected native boolean _strictEquals(int v8RuntimeHandle, int objectHandle, int that);

    protected native boolean _sameValue(int v8RuntimeHandle, int objectHandle, int that);

    protected native int _identityHash(int v8RuntimeHandle, int objectHandle);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _addObject(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final boolean value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final double value);

    protected native void _add(int v8RuntimeHandle, int objectHandle, final String key, final String value);

    protected native void _addUndefined(int v8RuntimeHandle, int objectHandle, final String key);

    protected native void _addNull(int v8RuntimeHandle, int objectHandle, final String key);

    protected native void _registerJavaMethod(int v8RuntimeHandle, final int objectHandle, final String functionName,
            final int methodID, final boolean voidMethod);

    protected native void _initNewV8Array(int v8RuntimeHandle, int arrayHandle);

    protected native void _releaseArray(int v8RuntimeHandle, int arrayHandle);

    protected native int _arrayGetSize(int v8RuntimeHandle, int arrayHandle);

    protected native int _arrayGetInteger(int v8RuntimeHandle, int arrayHandle, int index);

    protected native boolean _arrayGetBoolean(int v8RuntimeHandle, int arrayHandle, int index);

    protected native double _arrayGetDouble(int v8RuntimeHandle, int arrayHandle, int index);

    protected native String _arrayGetString(int v8RuntimeHandle, int arrayHandle, int index);

    protected native Object _arrayGet(int v8RuntimeHandle, int expectedType, int arrayHandle, int index);

    protected native void _addArrayIntItem(int v8RuntimeHandle, int arrayHandle, int value);

    protected native void _addArrayBooleanItem(int v8RuntimeHandle, int arrayHandle, boolean value);

    protected native void _addArrayDoubleItem(int v8RuntimeHandle, int arrayHandle, double value);

    protected native void _addArrayStringItem(int v8RuntimeHandle, int arrayHandle, String value);

    protected native void _addArrayObjectItem(int v8RuntimeHandle, int arrayHandle, int value);

    protected native void _addArrayUndefinedItem(int v8RuntimeHandle, int arrayHandle);

    protected native void _addArrayNullItem(int v8RuntimeHandle, int arrayHandle);

    protected native int _getType(int v8RuntimeHandle, int objectHandle, final String key);

    protected native int _getType(int v8RuntimeHandle, int objectHandle, final int index);

    protected native int _getArrayType(int v8RuntimeHandle, int objectHandle);

    protected native int _getType(int v8RuntimeHandle, int objectHandle, final int index, final int length);

    protected native void _setPrototype(int v8RuntimeHandle, int objectHandle, int prototypeHandle);

    protected native boolean _enableDebugSupport(int v8RuntimeHandle, int port, boolean waitForConnection);

    protected native void _disableDebugSupport(int v8RuntimeHandle);

    protected native void _processDebugMessages(int v8RuntimeHandle);

    protected native int[] _arrayGetInts(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    protected native double[] _arrayGetDoubles(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    protected native boolean[] _arrayGetBooleans(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    protected native String[] _arrayGetStrings(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    protected native int _arrayGetInts(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, int[] resultArray);

    protected native int _arrayGetDoubles(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, double[] resultArray);

    protected native int _arrayGetBooleans(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, boolean[] resultArray);

    protected native int _arrayGetStrings(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, String[] resultArray);

    void addObjRef() {
        objectReferences++;
    }

    void releaseObjRef() {
        objectReferences--;
    }

}
