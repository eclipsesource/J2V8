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

    private static Object       lock           = new Object();
    private volatile static int runtimeCounter = 0;
    private static Runnable     debugHandler   = null;
    private static Thread       debugThread    = null;

    private final V8Locker locker;
    private int            methodReferenceCounter = 0;
    private boolean        debugEnabled           = false;
    long                   objectReferences       = 0;
    private long           v8RuntimePtr           = 0;

    private static boolean   nativeLibraryLoaded = false;
    private static Error     nativeLoadError     = null;
    private static Exception nativeLoadException = null;
    private static V8Value   undefined           = new V8Object.Undefined();
    private static Object    invalid             = new Object();

    class MethodDescriptor {
        Object           object;
        Method           method;
        JavaCallback     callback;
        JavaVoidCallback voidCallback;
        boolean          includeReceiver;
    }

    Map<Integer, MethodDescriptor> functions = new HashMap<Integer, MethodDescriptor>();

    private synchronized static void load(final String tmpDirectory) {
        try {
            LibraryLoader.loadLibrary(tmpDirectory);
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

    public static V8 createV8Runtime() {
        return createV8Runtime(null, null);
    }

    public static V8 createV8Runtime(final String globalAlias) {
        return createV8Runtime(globalAlias, null);
    }

    public static V8 createV8Runtime(final String globalAlias, final String tempDirectory) {
        if (!nativeLibraryLoaded) {
            synchronized (lock) {
                if (!nativeLibraryLoaded) {
                    load(tempDirectory);
                }
            }
        }
        checkNativeLibraryLoaded();
        if (debugThread == null) {
            debugThread = Thread.currentThread();
        }
        V8 runtime = new V8(globalAlias);
        synchronized (lock) {
            runtimeCounter++;
        }
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
        locker = new V8Locker();
        checkThread();
        v8RuntimePtr = _createIsolate(globalAlias);
    }

    public static V8Value getUndefined() {
        return undefined;
    }

    public boolean enableDebugSupport(final int port, final boolean waitForConnection) {
        V8.checkDebugThread();
        debugEnabled = enableDebugSupport(getV8RuntimePtr(), port, waitForConnection);
        return debugEnabled;
    }

    public boolean enableDebugSupport(final int port) {
        V8.checkDebugThread();
        debugEnabled = enableDebugSupport(getV8RuntimePtr(), port, false);
        return debugEnabled;
    }

    public static void processDebugMessages(final V8 runtime) {
        runtime.checkThread();
        runtime._processDebugMessages(runtime.getV8RuntimePtr());
    }

    public void disableDebugSupport() {
        V8.checkDebugThread();
        disableDebugSupport(getV8RuntimePtr());
        debugEnabled = false;
    }

    public static int getActiveRuntimes() {
        return runtimeCounter;
    }

    public static void registerDebugHandler(final Runnable handler) {
        debugHandler = handler;
    }

    public long getV8RuntimePtr() {
        return v8RuntimePtr;
    }

    @Override
    public void release() {
        release(true);
    }

    public void terminateExecution() {
        terminateExecution(v8RuntimePtr);
    }

    public void release(final boolean reportMemoryLeaks) {
        if (isReleased()) {
            return;
        }
        checkThread();
        if (debugEnabled) {
            disableDebugSupport();
        }
        synchronized (lock) {
            runtimeCounter--;
        }
        _releaseRuntime(v8RuntimePtr);
        v8RuntimePtr = 0L;
        released = true;
        if (reportMemoryLeaks && (objectReferences > 0)) {
            throw new IllegalStateException(objectReferences + " Object(s) still exist in runtime");
        }
    }

    public int executeIntegerScript(final String script) {
        return executeIntegerScript(script, null, 0);
    }

    public int executeIntegerScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeIntegerScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    void createTwin(final V8Value value, final int twinObjectHandle) {
        checkThread();
        createTwin(v8RuntimePtr, value.getHandle(), twinObjectHandle);
    }

    public double executeDoubleScript(final String script) {
        return executeDoubleScript(script, null, 0);
    }

    public double executeDoubleScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeDoubleScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    public String executeStringScript(final String script) {
        return executeStringScript(script, null, 0);
    }

    public String executeStringScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeStringScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    public boolean executeBooleanScript(final String script) {
        return executeBooleanScript(script, null, 0);
    }

    public boolean executeBooleanScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeBooleanScript(v8RuntimePtr, script, scriptName, lineNumber);
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
        return executeScript(script, null, 0);
    }

    public Object executeScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeScript(getV8RuntimePtr(), UNKNOWN, script, scriptName, lineNumber);
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
        checkScript(script);
        executeVoidScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    public V8Locker getLocker() {
        return locker;
    }

    void checkThread() {
        locker.checkThread();
        if (isReleased()) {
            throw new Error("Runtime disposed error.");
        }
    }

    static void checkDebugThread() {
        if ((debugThread != null) && (debugThread != Thread.currentThread())) {
            throw new Error("Invalid V8 thread access.");
        }
    }

    static void checkScript(final String script) {
        if (script == null) {
            throw new NullPointerException("Script is null");
        }
    }

    void registerCallback(final Object object, final Method method, final int objectHandle, final String jsFunctionName, final boolean includeReceiver) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.object = object;
        methodDescriptor.method = method;
        methodDescriptor.includeReceiver = includeReceiver;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        registerJavaMethod(getV8RuntimePtr(), objectHandle, jsFunctionName, methodID, isVoidMethod(method));
    }

    void registerVoidCallback(final JavaVoidCallback callback, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.voidCallback = callback;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        registerJavaMethod(getV8RuntimePtr(), objectHandle, jsFunctionName, methodID, true);
    }

    void registerCallback(final JavaCallback callback, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.callback = callback;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        registerJavaMethod(getV8RuntimePtr(), objectHandle, jsFunctionName, methodID, false);
    }

    private boolean isVoidMethod(final Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(Void.TYPE)) {
            return true;
        }
        return false;
    }

    private Object getDefaultValue(final Class<?> type) {
        if (type.equals(V8Object.class)) {
            return new V8Object.Undefined();
        } else if (type.equals(V8Array.class)) {
            return new V8Array.Undefined();
        }
        return invalid;
    }

    protected Object callObjectJavaMethod(final int methodID, final V8Object receiver, final V8Array parameters) throws Throwable {
        MethodDescriptor methodDescriptor = getFunctionRegistry().get(methodID);
        if (methodDescriptor.callback != null) {
            return checkResult(methodDescriptor.callback.invoke(receiver, parameters));
        }
        boolean hasVarArgs = methodDescriptor.method.isVarArgs();
        Object[] args = getArgs(receiver, methodDescriptor, parameters, hasVarArgs);
        checkArgs(args);
        try {
            Object result = methodDescriptor.method.invoke(methodDescriptor.object, args);
            return checkResult(result);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
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

    protected void callVoidJavaMethod(final int methodID, final V8Object receiver, final V8Array parameters) throws Throwable {
        MethodDescriptor methodDescriptor = getFunctionRegistry().get(methodID);
        if (methodDescriptor.voidCallback != null) {
            methodDescriptor.voidCallback.invoke(receiver, parameters);
            return;
        }
        boolean hasVarArgs = methodDescriptor.method.isVarArgs();
        Object[] args = getArgs(receiver, methodDescriptor, parameters, hasVarArgs);
        checkArgs(args);
        try {
            methodDescriptor.method.invoke(methodDescriptor.object, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } finally {
            releaseArguments(args, hasVarArgs);
        }
    }

    private void checkArgs(final Object[] args) {
        for (Object argument : args) {
            if (argument == invalid) {
                throw new IllegalArgumentException("argument type mismatch");
            }
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

    private Object[] getArgs(final V8Object receiver, final MethodDescriptor methodDescriptor, final V8Array parameters, final boolean hasVarArgs) {
        int numberOfParameters = methodDescriptor.method.getParameterTypes().length;
        int varArgIndex = hasVarArgs ? numberOfParameters - 1 : numberOfParameters;
        Object[] args = setDefaultValues(new Object[numberOfParameters], methodDescriptor.method.getParameterTypes(), receiver, methodDescriptor.includeReceiver);
        List<Object> varArgs = populateParamters(parameters, varArgIndex, args, methodDescriptor.includeReceiver);
        if (hasVarArgs) {
            args[varArgIndex] = varArgs.toArray();
        }
        return args;
    }

    private List<Object> populateParamters(final V8Array parameters, final int varArgIndex, final Object[] args, final boolean includeReceiver) {
        List<Object> varArgs = new ArrayList<Object>();
        int start = 0;
        if (includeReceiver) {
            start = 1;
        }
        for (int i = start; i < (parameters.length() + start); i++) {
            if (i >= varArgIndex) {
                varArgs.add(getArrayItem(parameters, i - start));
            } else {
                args[i] = getArrayItem(parameters, i - start);
            }
        }
        return varArgs;
    }

    private Object[] setDefaultValues(final Object[] parameters, final Class<?>[] parameterTypes, final V8Object receiver, final boolean includeReceiver) {
        int start = 0;
        if (includeReceiver) {
            start = 1;
            parameters[0] = receiver;
        }
        for (int i = start; i < parameters.length; i++) {
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
                case V8_FUNCTION:
                    return array.getObject(index);
                case UNDEFINED:
                    return V8.getUndefined();
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

    protected void initNewV8Object(final long v8RuntimePtr, final int objectHandle) {
        _initNewV8Object(v8RuntimePtr, objectHandle);
    }

    protected void createTwin(final long v8RuntimePtr, final int objectHandle, final int twinObjectHandle) {
        _createTwin(v8RuntimePtr, objectHandle, twinObjectHandle);
    }

    protected int executeIntegerScript(final long v8RuntimePtr, final String script, final String scriptName, final int lineNumber) {
        return _executeIntegerScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    protected double executeDoubleScript(final long v8RuntimePtr, final String script, final String scriptName, final int lineNumber) {
        return _executeDoubleScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    protected String executeStringScript(final long v8RuntimePtr, final String script, final String scriptName, final int lineNumber) {
        return _executeStringScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    protected boolean executeBooleanScript(final long v8RuntimePtr, final String script, final String scriptName, final int lineNumber) {
        return _executeBooleanScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    protected Object executeScript(final long v8RuntimePtr, final int expectedType, final String script, final String scriptName, final int lineNumber) {
        return _executeScript(v8RuntimePtr, expectedType, script, scriptName, lineNumber);
    }

    protected void executeVoidScript(final long v8RuntimePtr, final String script, final String scriptName, final int lineNumber) {
        _executeVoidScript(v8RuntimePtr, script, scriptName, lineNumber);
    }

    protected void release(final long v8RuntimePtr, final int objectHandle) {
        _release(v8RuntimePtr, objectHandle);
    }

    protected boolean contains(final long v8RuntimePtr, final int objectHandle, final String key) {
        return _contains(v8RuntimePtr, objectHandle, key);
    }

    protected String[] getKeys(final long v8RuntimePtr, final int objectHandle) {
        return _getKeys(v8RuntimePtr, objectHandle);
    }

    protected int getInteger(final long v8RuntimePtr, final int objectHandle, final String key) {
        return _getInteger(v8RuntimePtr, objectHandle, key);
    }

    protected boolean getBoolean(final long v8RuntimePtr, final int objectHandle, final String key) {
        return _getBoolean(v8RuntimePtr, objectHandle, key);
    }

    protected double getDouble(final long v8RuntimePtr, final int objectHandle, final String key) {
        return _getDouble(v8RuntimePtr, objectHandle, key);
    }

    protected String getString(final long v8RuntimePtr, final int objectHandle, final String key) {
        return _getString(v8RuntimePtr, objectHandle, key);
    }

    protected Object get(final long v8RuntimePtr, final int expectedType, final int objectHandle, final String key) {
        return _get(v8RuntimePtr, expectedType, objectHandle, key);
    }

    protected int executeIntegerFunction(final long v8RuntimePtr, final int objectHandle, final String name, final int parametersHandle) {
        return _executeIntegerFunction(v8RuntimePtr, objectHandle, name, parametersHandle);
    }

    protected double executeDoubleFunction(final long v8RuntimePtr, final int objectHandle, final String name, final int parametersHandle) {
        return _executeDoubleFunction(v8RuntimePtr, objectHandle, name, parametersHandle);
    }

    protected String executeStringFunction(final long v8RuntimePtr, final int handle, final String name, final int parametersHandle) {
        return _executeStringFunction(v8RuntimePtr, handle, name, parametersHandle);
    }

    protected boolean executeBooleanFunction(final long v8RuntimePtr, final int handle, final String name, final int parametersHandle) {
        return _executeBooleanFunction(v8RuntimePtr, handle, name, parametersHandle);
    }

    protected Object executeFunction(final long v8RuntimePtr, final int expectedType, final int objectHandle, final String name, final int parametersHandle) {
        return _executeFunction(v8RuntimePtr, expectedType, objectHandle, name, parametersHandle);
    }

    protected Object executeFunction(final long v8RuntimePtr, final int receiverHandle, final int functionHandle, final int parametersHandle) {
        return _executeFunction(v8RuntimePtr, receiverHandle, functionHandle, parametersHandle);
    }

    protected void executeVoidFunction(final long v8RuntimePtr, final int objectHandle, final String name, final int parametersHandle) {
        _executeVoidFunction(v8RuntimePtr, objectHandle, name, parametersHandle);
    }

    protected boolean equals(final long v8RuntimePtr, final int objectHandle, final int that) {
        return _equals(v8RuntimePtr, objectHandle, that);
    }

    protected boolean strictEquals(final long v8RuntimePtr, final int objectHandle, final int that) {
        return _strictEquals(v8RuntimePtr, objectHandle, that);
    }

    protected boolean sameValue(final long v8RuntimePtr, final int objectHandle, final int that) {
        return _sameValue(v8RuntimePtr, objectHandle, that);
    }

    protected int identityHash(final long v8RuntimePtr, final int objectHandle) {
        return _identityHash(v8RuntimePtr, objectHandle);
    }

    protected void add(final long v8RuntimePtr, final int objectHandle, final String key, final int value) {
        _add(v8RuntimePtr, objectHandle, key, value);
    }

    protected void addObject(final long v8RuntimePtr, final int objectHandle, final String key, final int value) {
        _addObject(v8RuntimePtr, objectHandle, key, value);
    }

    protected void add(final long v8RuntimePtr, final int objectHandle, final String key, final boolean value) {
        _add(v8RuntimePtr, objectHandle, key, value);
    }

    protected void add(final long v8RuntimePtr, final int objectHandle, final String key, final double value) {
        _add(v8RuntimePtr, objectHandle, key, value);
    }

    protected void add(final long v8RuntimePtr, final int objectHandle, final String key, final String value) {
        _add(v8RuntimePtr, objectHandle, key, value);
    }

    protected void addUndefined(final long v8RuntimePtr, final int objectHandle, final String key) {
        _addUndefined(v8RuntimePtr, objectHandle, key);
    }

    protected void addNull(final long v8RuntimePtr, final int objectHandle, final String key) {
        _addNull(v8RuntimePtr, objectHandle, key);
    }

    protected void registerJavaMethod(final long v8RuntimePtr, final int objectHandle, final String functionName, final int methodID, final boolean voidMethod) {
        _registerJavaMethod(v8RuntimePtr, objectHandle, functionName, methodID, voidMethod);
    }

    protected void initNewV8Array(final long v8RuntimePtr, final int arrayHandle) {
        _initNewV8Array(v8RuntimePtr, arrayHandle);
    }

    protected int arrayGetSize(final long v8RuntimePtr, final int arrayHandle) {
        return _arrayGetSize(v8RuntimePtr, arrayHandle);
    }

    protected int arrayGetInteger(final long v8RuntimePtr, final int arrayHandle, final int index) {
        return _arrayGetInteger(v8RuntimePtr, arrayHandle, index);
    }

    protected boolean arrayGetBoolean(final long v8RuntimePtr, final int arrayHandle, final int index) {
        return _arrayGetBoolean(v8RuntimePtr, arrayHandle, index);
    }

    protected double arrayGetDouble(final long v8RuntimePtr, final int arrayHandle, final int index) {
        return _arrayGetDouble(v8RuntimePtr, arrayHandle, index);
    }

    protected String arrayGetString(final long v8RuntimePtr, final int arrayHandle, final int index) {
        return _arrayGetString(v8RuntimePtr, arrayHandle, index);
    }

    protected Object arrayGet(final long v8RuntimePtr, final int expectedType, final int arrayHandle, final int index) {
        return _arrayGet(v8RuntimePtr, expectedType, arrayHandle, index);
    }

    protected void addArrayIntItem(final long v8RuntimePtr, final int arrayHandle, final int value) {
        _addArrayIntItem(v8RuntimePtr, arrayHandle, value);
    }

    protected void addArrayBooleanItem(final long v8RuntimePtr, final int arrayHandle, final boolean value) {
        _addArrayBooleanItem(v8RuntimePtr, arrayHandle, value);
    }

    protected void addArrayDoubleItem(final long v8RuntimePtr, final int arrayHandle, final double value) {
        _addArrayDoubleItem(v8RuntimePtr, arrayHandle, value);
    }

    protected void addArrayStringItem(final long v8RuntimePtr, final int arrayHandle, final String value) {
        _addArrayStringItem(v8RuntimePtr, arrayHandle, value);
    }

    protected void addArrayObjectItem(final long v8RuntimePtr, final int arrayHandle, final int value) {
        _addArrayObjectItem(v8RuntimePtr, arrayHandle, value);
    }

    protected void addArrayUndefinedItem(final long v8RuntimePtr, final int arrayHandle) {
        _addArrayUndefinedItem(v8RuntimePtr, arrayHandle);
    }

    protected void addArrayNullItem(final long v8RuntimePtr, final int arrayHandle) {
        _addArrayNullItem(v8RuntimePtr, arrayHandle);
    }

    protected int getType(final long v8RuntimePtr, final int objectHandle, final String key) {
        return _getType(v8RuntimePtr, objectHandle, key);
    }

    protected int getType(final long v8RuntimePtr, final int objectHandle, final int index) {
        return _getType(v8RuntimePtr, objectHandle, index);
    }

    protected int getArrayType(final long v8RuntimePtr, final int objectHandle) {
        return _getArrayType(v8RuntimePtr, objectHandle);
    }

    protected int getType(final long v8RuntimePtr, final int objectHandle, final int index, final int length) {
        return _getType(v8RuntimePtr, objectHandle, index, length);
    }

    protected void setPrototype(final long v8RuntimePtr, final int objectHandle, final int prototypeHandle) {
        _setPrototype(v8RuntimePtr, objectHandle, prototypeHandle);
    }

    protected boolean enableDebugSupport(final long v8RuntimePtr, final int port, final boolean waitForConnection) {
        return _enableDebugSupport(v8RuntimePtr, port, waitForConnection);
    }

    protected void disableDebugSupport(final long v8RuntimePtr) {
        _disableDebugSupport(v8RuntimePtr);
    }

    protected void processDebugMessages(final long v8RuntimePtr) {
        _processDebugMessages(v8RuntimePtr);
    }

    protected int[] arrayGetIntegers(final long v8RuntimePtr, final int objectHandle, final int index, final int length) {
        return _arrayGetIntegers(v8RuntimePtr, objectHandle, index, length);
    }

    protected double[] arrayGetDoubles(final long v8RuntimePtr, final int objectHandle, final int index, final int length) {
        return _arrayGetDoubles(v8RuntimePtr, objectHandle, index, length);
    }

    protected boolean[] arrayGetBooleans(final long v8RuntimePtr, final int objectHandle, final int index, final int length) {
        return _arrayGetBooleans(v8RuntimePtr, objectHandle, index, length);
    }

    protected String[] arrayGetStrings(final long v8RuntimePtr, final int objectHandle, final int index, final int length) {
        return _arrayGetStrings(v8RuntimePtr, objectHandle, index, length);
    }

    protected int arrayGetIntegers(final long v8RuntimePtr, final int objectHandle, final int index, final int length, final int[] resultArray) {
        return _arrayGetIntegers(v8RuntimePtr, objectHandle, index, length, resultArray);
    }

    protected int arrayGetDoubles(final long v8RuntimePtr, final int objectHandle, final int index, final int length, final double[] resultArray) {
        return _arrayGetDoubles(v8RuntimePtr, objectHandle, index, length, resultArray);
    }

    protected int arrayGetBooleans(final long v8RuntimePtr, final int objectHandle, final int index, final int length, final boolean[] resultArray) {
        return _arrayGetBooleans(v8RuntimePtr, objectHandle, index, length, resultArray);
    }

    protected int arrayGetStrings(final long v8RuntimePtr, final int objectHandle, final int index, final int length, final String[] resultArray) {
        return _arrayGetStrings(v8RuntimePtr, objectHandle, index, length, resultArray);
    }

    protected void terminateExecution(final long v8RuntimePtr) {
        _terminateExecution(v8RuntimePtr);
    }

    private native void _initNewV8Object(long v8RuntimePtr, int objectHandle);

    private native void _createTwin(long v8RuntimePtr, int objectHandle, int twinObjectHandle);

    private native void _releaseRuntime(long v8RuntimePtr);

    private native long _createIsolate(String globalAlias);

    private native int _executeIntegerScript(long v8RuntimePtr, final String script, final String scriptName, final int lineNumber);

    private native double _executeDoubleScript(long v8RuntimePtr, final String script, final String scriptName, final int lineNumber);

    private native String _executeStringScript(long v8RuntimePtr, final String script, final String scriptName, final int lineNumber);

    private native boolean _executeBooleanScript(long v8RuntimePtr, final String script, final String scriptName, final int lineNumber);

    private native Object _executeScript(long v8RuntimePtr, int expectedType, String script, String scriptName, int lineNumber);

    private native void _executeVoidScript(long v8RuntimePtr, String script, String scriptName, int lineNumber);

    private native void _release(long v8RuntimePtr, int objectHandle);

    private native boolean _contains(long v8RuntimePtr, int objectHandle, final String key);

    private native String[] _getKeys(long v8RuntimePtr, int objectHandle);

    private native int _getInteger(long v8RuntimePtr, int objectHandle, final String key);

    private native boolean _getBoolean(long v8RuntimePtr, int objectHandle, final String key);

    private native double _getDouble(long v8RuntimePtr, int objectHandle, final String key);

    private native String _getString(long v8RuntimePtr, int objectHandle, final String key);

    private native Object _get(long v8RuntimePtr, int expectedType, final int objectHandle, final String key);

    private native int _executeIntegerFunction(long v8RuntimePtr, int objectHandle, String name, int parametersHandle);

    private native double _executeDoubleFunction(long v8RuntimePtr, int objectHandle, String name, int parametersHandle);

    private native String _executeStringFunction(long v8RuntimePtr2, int handle, String name, int parametersHandle);

    private native boolean _executeBooleanFunction(long v8RuntimePtr2, int handle, String name, int parametersHandle);

    private native Object _executeFunction(long v8RuntimePtr, int expectedType, int objectHandle, String name, int parametersHandle);

    private native Object _executeFunction(long v8RuntimePtr, int receiverHandle, int functionHandle, int parametersHandle);

    private native void _executeVoidFunction(long v8RuntimePtr, int objectHandle, final String name, final int parametersHandle);

    private native boolean _equals(long v8RuntimePtr, int objectHandle, int that);

    private native boolean _strictEquals(long v8RuntimePtr, int objectHandle, int that);

    private native boolean _sameValue(long v8RuntimePtr, int objectHandle, int that);

    private native int _identityHash(long v8RuntimePtr, int objectHandle);

    private native void _add(long v8RuntimePtr, int objectHandle, final String key, final int value);

    private native void _addObject(long v8RuntimePtr, int objectHandle, final String key, final int value);

    private native void _add(long v8RuntimePtr, int objectHandle, final String key, final boolean value);

    private native void _add(long v8RuntimePtr, int objectHandle, final String key, final double value);

    private native void _add(long v8RuntimePtr, int objectHandle, final String key, final String value);

    private native void _addUndefined(long v8RuntimePtr, int objectHandle, final String key);

    private native void _addNull(long v8RuntimePtr, int objectHandle, final String key);

    private native void _registerJavaMethod(long v8RuntimePtr, final int objectHandle, final String functionName, final int methodID, final boolean voidMethod);

    private native void _initNewV8Array(long v8RuntimePtr, int arrayHandle);

    private native int _arrayGetSize(long v8RuntimePtr, int arrayHandle);

    private native int _arrayGetInteger(long v8RuntimePtr, int arrayHandle, int index);

    private native boolean _arrayGetBoolean(long v8RuntimePtr, int arrayHandle, int index);

    private native double _arrayGetDouble(long v8RuntimePtr, int arrayHandle, int index);

    private native String _arrayGetString(long v8RuntimePtr, int arrayHandle, int index);

    private native Object _arrayGet(long v8RuntimePtr, int expectedType, int arrayHandle, int index);

    private native void _addArrayIntItem(long v8RuntimePtr, int arrayHandle, int value);

    private native void _addArrayBooleanItem(long v8RuntimePtr, int arrayHandle, boolean value);

    private native void _addArrayDoubleItem(long v8RuntimePtr, int arrayHandle, double value);

    private native void _addArrayStringItem(long v8RuntimePtr, int arrayHandle, String value);

    private native void _addArrayObjectItem(long v8RuntimePtr, int arrayHandle, int value);

    private native void _addArrayUndefinedItem(long v8RuntimePtr, int arrayHandle);

    private native void _addArrayNullItem(long v8RuntimePtr, int arrayHandle);

    private native int _getType(long v8RuntimePtr, int objectHandle, final String key);

    private native int _getType(long v8RuntimePtr, int objectHandle, final int index);

    private native int _getArrayType(long v8RuntimePtr, int objectHandle);

    private native void _setPrototype(long v8RuntimePtr, int objectHandle, int prototypeHandle);

    private native int _getType(long v8RuntimePtr, int objectHandle, final int index, final int length);

    private native boolean _enableDebugSupport(long v8RuntimePtr, int port, boolean waitForConnection);

    private native void _disableDebugSupport(long v8RuntimePtr);

    private native void _processDebugMessages(long v8RuntimePtr);

    private native double[] _arrayGetDoubles(final long v8RuntimePtr, final int objectHandle, final int index, final int length);

    private native int[] _arrayGetIntegers(final long v8RuntimePtr, final int objectHandle, final int index, final int length);

    private native boolean[] _arrayGetBooleans(final long v8RuntimePtr, final int objectHandle, final int index, final int length);

    private native String[] _arrayGetStrings(final long v8RuntimePtr, final int objectHandle, final int index, final int length);

    private native int _arrayGetIntegers(final long v8RuntimePtr, final int objectHandle, final int index, final int length, int[] resultArray);

    private native int _arrayGetDoubles(final long v8RuntimePtr, final int objectHandle, final int index, final int length, double[] resultArray);

    private native int _arrayGetBooleans(final long v8RuntimePtr, final int objectHandle, final int index, final int length, boolean[] resultArray);

    private native int _arrayGetStrings(final long v8RuntimePtr, final int objectHandle, final int index, final int length, String[] resultArray);

    private native void _terminateExecution(final long v8RuntimePtr);

    void addObjRef() {
        objectReferences++;
    }

    void releaseObjRef() {
        objectReferences--;
    }

}
