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

    static V8Lock               lock                   = new V8Lock();
    private static volatile int v8InstanceCounter      = 0;
    private static List<V8>     runtimes               = new ArrayList<>();
    private static Runnable     debugHandler           = null;
    private static Thread       debugThread            = null;

    private Thread              thread                 = null;
    private int                 methodReferenceCounter = 0;
    private int                 v8RuntimeHandle;
    private boolean             debugEnabled           = false;
    long                        objectReferences       = 0;

    private static boolean      nativeLibraryLoaded    = false;
    private static Error        nativeLoadError        = null;
    private static Exception    nativeLoadException    = null;
    private static V8Value      undefined              = new V8Object.Undefined();
    private static Object       invalid                = new Object();

    class MethodDescriptor {
        Object           object;
        Method           method;
        JavaCallback     callback;
        JavaVoidCallback voidCallback;
    }

    @Override
    public boolean equals(final Object that) {
        return this == that;
    };

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    };

    Map<Integer, MethodDescriptor> functions = new HashMap<>();

    private static void load(final String tmpDirectory) {
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

    public synchronized static V8 createV8Runtime() {
        return createV8Runtime(null, null);
    }

    public synchronized static V8 createV8Runtime(final String globalAlias) {
        return createV8Runtime(globalAlias, null);
    }

    public synchronized static V8 createV8Runtime(final String globalAlias, final String tempDirectory) {
        if (!nativeLibraryLoaded) {
            load(tempDirectory);
        }
        checkNativeLibraryLoaded();
        if (debugThread == null) {
            debugThread = Thread.currentThread();
        }
        lock.lockWrite();
        try {
            V8 runtime = new V8(globalAlias);
            runtime.thread = Thread.currentThread();
            runtimes.add(runtime);
            return runtime;
        } finally {
            lock.unlockWrite();
        }
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
        V8.checkDebugThread();
        debugEnabled = enableDebugSupport(getHandle(), port, waitForConnection);
        return debugEnabled;
    }

    public boolean enableDebugSupport(final int port) {
        V8.checkDebugThread();
        debugEnabled = enableDebugSupport(getV8RuntimeHandle(), port, false);
        return debugEnabled;
    }

    public void disableDebugSupport() {
        V8.checkDebugThread();
        disableDebugSupport(getV8RuntimeHandle());
        debugEnabled = false;
    }

    public static void processDebugMessages() {
        V8.checkDebugThread();
        for (V8 v8 : runtimes) {
            v8.processDebugMessages(v8.getV8RuntimeHandle());
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

    public void terminateExecution() {
        terminateExecution(v8RuntimeHandle);
    }

    public void release(final boolean reportMemoryLeaks) {
        checkThread();
        if (debugEnabled) {
            disableDebugSupport();
        }
        lock.lockWrite();
        try {
            runtimes.remove(this);
            _releaseRuntime(v8RuntimeHandle);
            if (reportMemoryLeaks && (objectReferences > 0)) {
                throw new IllegalStateException(objectReferences + " Object(s) still exist in runtime");
            }
        } finally {
            lock.unlockWrite();
        }
    }

    public int executeIntegerScript(final String script) {
        return executeIntegerScript(script, null, 0);
    }

    public int executeIntegerScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeIntegerScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    public double executeDoubleScript(final String script) {
        return executeDoubleScript(script, null, 0);
    }

    public double executeDoubleScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeDoubleScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    public String executeStringScript(final String script) {
        return executeStringScript(script, null, 0);
    }

    public String executeStringScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeStringScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    public boolean executeBooleanScript(final String script) {
        return executeBooleanScript(script, null, 0);
    }

    public boolean executeBooleanScript(final String script, final String scriptName, final int lineNumber) {
        checkThread();
        checkScript(script);
        return executeBooleanScript(v8RuntimeHandle, script, scriptName, lineNumber);
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
        return executeScript(getV8RuntimeHandle(), UNKNOWN, script, scriptName, lineNumber);
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
        executeVoidScript(v8RuntimeHandle, script, scriptName, lineNumber);
    }

    void checkThread() {
        if ((thread != null) && (thread != Thread.currentThread())) {
            throw new Error("Invalid V8 thread access.");
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

    void registerCallback(final Object object, final Method method, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.object = object;
        methodDescriptor.method = method;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        registerJavaMethod(getV8RuntimeHandle(), objectHandle, jsFunctionName, methodID, isVoidMethod(method));
    }

    void registerVoidCallback(final JavaVoidCallback callback, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.voidCallback = callback;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        registerJavaMethod(getV8RuntimeHandle(), objectHandle, jsFunctionName, methodID, true);
    }

    void registerCallback(final JavaCallback callback, final int objectHandle, final String jsFunctionName) {
        MethodDescriptor methodDescriptor = new MethodDescriptor();
        methodDescriptor.callback = callback;
        int methodID = methodReferenceCounter++;
        getFunctionRegistry().put(methodID, methodDescriptor);
        registerJavaMethod(getV8RuntimeHandle(), objectHandle, jsFunctionName, methodID, false);
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

    protected Object callObjectJavaMethod(final int methodID, final V8Array parameters) throws Throwable {
        lock.unlockRead();
        try {
            MethodDescriptor methodDescriptor = getFunctionRegistry().get(methodID);
            if (methodDescriptor.callback != null) {
                return checkResult(methodDescriptor.callback.invoke(parameters));
            }
            boolean hasVarArgs = methodDescriptor.method.isVarArgs();
            Object[] args = getArgs(methodDescriptor, parameters, hasVarArgs);
            checkArgs(args);
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
        } finally {
            lock.lockRead();
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
        lock.unlockRead();
        try {
            MethodDescriptor methodDescriptor = getFunctionRegistry().get(methodID);
            if (methodDescriptor.voidCallback != null) {
                methodDescriptor.voidCallback.invoke(parameters);
                return;
            }
            boolean hasVarArgs = methodDescriptor.method.isVarArgs();
            Object[] args = getArgs(methodDescriptor, parameters, hasVarArgs);
            checkArgs(args);
            try {
                methodDescriptor.method.invoke(methodDescriptor.object, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw e;
            } finally {
                releaseArguments(args, hasVarArgs);
            }
        } finally {
            lock.lockRead();
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

    protected void initNewV8Object(final int v8RuntimeHandle, final int objectHandle) {
        lock.lockRead();
        try {
            _initNewV8Object(v8RuntimeHandle, objectHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected int executeIntegerScript(final int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber) {
        lock.lockRead();
        try {
            return _executeIntegerScript(v8RuntimeHandle, script, scriptName, lineNumber);
        } finally {
            lock.unlockRead();
        }
    }

    protected double executeDoubleScript(final int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber) {
        lock.lockRead();
        try {
            return _executeDoubleScript(v8RuntimeHandle, script, scriptName, lineNumber);
        } finally {
            lock.unlockRead();
        }
    }

    protected String executeStringScript(final int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber) {
        lock.lockRead();
        try {
            return _executeStringScript(v8RuntimeHandle, script, scriptName, lineNumber);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean executeBooleanScript(final int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber) {
        lock.lockRead();
        try {
            return _executeBooleanScript(v8RuntimeHandle, script, scriptName, lineNumber);
        } finally {
            lock.unlockRead();
        }
    }

    protected Object executeScript(final int v8RuntimeHandle, final int expectedType, final String script, final String scriptName, final int lineNumber) {
        lock.lockRead();
        try {
            return _executeScript(v8RuntimeHandle, expectedType, script, scriptName, lineNumber);
        } finally {
            lock.unlockRead();
        }
    }

    protected void executeVoidScript(final int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber) {
        lock.lockRead();
        try {
            _executeVoidScript(v8RuntimeHandle, script, scriptName, lineNumber);
        } finally {
            lock.unlockRead();
        }
    }

    protected void release(final int v8RuntimeHandle, final int objectHandle) {
        lock.lockRead();
        try {
            _release(v8RuntimeHandle, objectHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean contains(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            return _contains(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected String[] getKeys(final int v8RuntimeHandle, final int objectHandle) {
        lock.lockRead();
        try {
            return _getKeys(v8RuntimeHandle, objectHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected int getInteger(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            return _getInteger(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean getBoolean(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            return _getBoolean(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected double getDouble(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            return _getDouble(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected String getString(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            return _getString(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected Object get(final int v8RuntimeHandle, final int expectedType, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            return _get(v8RuntimeHandle, expectedType, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected int executeIntegerFunction(final int v8RuntimeHandle, final int objectHandle, final String name, final int parametersHandle) {
        lock.lockRead();
        try {
            return _executeIntegerFunction(v8RuntimeHandle, objectHandle, name, parametersHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected double executeDoubleFunction(final int v8RuntimeHandle, final int objectHandle, final String name, final int parametersHandle) {
        lock.lockRead();
        try {
            return _executeDoubleFunction(v8RuntimeHandle, objectHandle, name, parametersHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected String executeStringFunction(final int v8RuntimeHandle, final int handle, final String name, final int parametersHandle) {
        lock.lockRead();
        try {
            return _executeStringFunction(v8RuntimeHandle, handle, name, parametersHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean executeBooleanFunction(final int v8RuntimeHandle, final int handle, final String name, final int parametersHandle) {
        lock.lockRead();
        try {
            return _executeBooleanFunction(v8RuntimeHandle, handle, name, parametersHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected Object executeFunction(final int v8RuntimeHandle, final int expectedType, final int objectHandle, final String name, final int parametersHandle) {
        lock.lockRead();
        try {
            return _executeFunction(v8RuntimeHandle, expectedType, objectHandle, name, parametersHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected void executeVoidFunction(final int v8RuntimeHandle, final int objectHandle, final String name, final int parametersHandle) {
        lock.lockRead();
        try {
            _executeVoidFunction(v8RuntimeHandle, objectHandle, name, parametersHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean equals(final int v8RuntimeHandle, final int objectHandle, final int that) {
        lock.lockRead();
        try {
            return _equals(v8RuntimeHandle, objectHandle, that);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean strictEquals(final int v8RuntimeHandle, final int objectHandle, final int that) {
        lock.lockRead();
        try {
            return _strictEquals(v8RuntimeHandle, objectHandle, that);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean sameValue(final int v8RuntimeHandle, final int objectHandle, final int that) {
        lock.lockRead();
        try {
            return _sameValue(v8RuntimeHandle, objectHandle, that);
        } finally {
            lock.unlockRead();
        }
    }

    protected int identityHash(final int v8RuntimeHandle, final int objectHandle) {
        lock.lockRead();
        try {
            return _identityHash(v8RuntimeHandle, objectHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected void add(final int v8RuntimeHandle, final int objectHandle, final String key, final int value) {
        lock.lockRead();
        try {
            _add(v8RuntimeHandle, objectHandle, key, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addObject(final int v8RuntimeHandle, final int objectHandle, final String key, final int value) {
        lock.lockRead();
        try {
            _addObject(v8RuntimeHandle, objectHandle, key, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void add(final int v8RuntimeHandle, final int objectHandle, final String key, final boolean value) {
        lock.lockRead();
        try {
            _add(v8RuntimeHandle, objectHandle, key, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void add(final int v8RuntimeHandle, final int objectHandle, final String key, final double value) {
        lock.lockRead();
        try {
            _add(v8RuntimeHandle, objectHandle, key, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void add(final int v8RuntimeHandle, final int objectHandle, final String key, final String value) {
        lock.lockRead();
        try {
            _add(v8RuntimeHandle, objectHandle, key, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addUndefined(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            _addUndefined(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addNull(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            _addNull(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected void registerJavaMethod(final int v8RuntimeHandle, final int objectHandle, final String functionName, final int methodID, final boolean voidMethod) {
        lock.lockRead();
        try {
            _registerJavaMethod(v8RuntimeHandle, objectHandle, functionName, methodID, voidMethod);
        } finally {
            lock.unlockRead();
        }
    }

    protected void initNewV8Array(final int v8RuntimeHandle, final int arrayHandle) {
        lock.lockRead();
        try {
            _initNewV8Array(v8RuntimeHandle, arrayHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected int arrayGetSize(final int v8RuntimeHandle, final int arrayHandle) {
        lock.lockRead();
        try {
            return _arrayGetSize(v8RuntimeHandle, arrayHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected int arrayGetInteger(final int v8RuntimeHandle, final int arrayHandle, final int index) {
        lock.lockRead();
        try {
            return _arrayGetInteger(v8RuntimeHandle, arrayHandle, index);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean arrayGetBoolean(final int v8RuntimeHandle, final int arrayHandle, final int index) {
        lock.lockRead();
        try {
            return _arrayGetBoolean(v8RuntimeHandle, arrayHandle, index);
        } finally {
            lock.unlockRead();
        }
    }

    protected double arrayGetDouble(final int v8RuntimeHandle, final int arrayHandle, final int index) {
        lock.lockRead();
        try {
            return _arrayGetDouble(v8RuntimeHandle, arrayHandle, index);
        } finally {
            lock.unlockRead();
        }
    }

    protected String arrayGetString(final int v8RuntimeHandle, final int arrayHandle, final int index) {
        lock.lockRead();
        try {
            return _arrayGetString(v8RuntimeHandle, arrayHandle, index);
        } finally {
            lock.unlockRead();
        }
    }

    protected Object arrayGet(final int v8RuntimeHandle, final int expectedType, final int arrayHandle, final int index) {
        lock.lockRead();
        try {
            return _arrayGet(v8RuntimeHandle, expectedType, arrayHandle, index);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addArrayIntItem(final int v8RuntimeHandle, final int arrayHandle, final int value) {
        lock.lockRead();
        try {
            _addArrayIntItem(v8RuntimeHandle, arrayHandle, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addArrayBooleanItem(final int v8RuntimeHandle, final int arrayHandle, final boolean value) {
        lock.lockRead();
        try {
            _addArrayBooleanItem(v8RuntimeHandle, arrayHandle, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addArrayDoubleItem(final int v8RuntimeHandle, final int arrayHandle, final double value) {
        lock.lockRead();
        try {
            _addArrayDoubleItem(v8RuntimeHandle, arrayHandle, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addArrayStringItem(final int v8RuntimeHandle, final int arrayHandle, final String value) {
        lock.lockRead();
        try {
            _addArrayStringItem(v8RuntimeHandle, arrayHandle, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addArrayObjectItem(final int v8RuntimeHandle, final int arrayHandle, final int value) {
        lock.lockRead();
        try {
            _addArrayObjectItem(v8RuntimeHandle, arrayHandle, value);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addArrayUndefinedItem(final int v8RuntimeHandle, final int arrayHandle) {
        lock.lockRead();
        try {
            _addArrayUndefinedItem(v8RuntimeHandle, arrayHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected void addArrayNullItem(final int v8RuntimeHandle, final int arrayHandle) {
        lock.lockRead();
        try {
            _addArrayNullItem(v8RuntimeHandle, arrayHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected int getType(final int v8RuntimeHandle, final int objectHandle, final String key) {
        lock.lockRead();
        try {
            return _getType(v8RuntimeHandle, objectHandle, key);
        } finally {
            lock.unlockRead();
        }
    }

    protected int getType(final int v8RuntimeHandle, final int objectHandle, final int index) {
        lock.lockRead();
        try {
            return _getType(v8RuntimeHandle, objectHandle, index);
        } finally {
            lock.unlockRead();
        }
    }

    protected int getArrayType(final int v8RuntimeHandle, final int objectHandle) {
        lock.lockRead();
        try {
            return _getArrayType(v8RuntimeHandle, objectHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected int getType(final int v8RuntimeHandle, final int objectHandle, final int index, final int length) {
        lock.lockRead();
        try {
            return _getType(v8RuntimeHandle, objectHandle, index, length);
        } finally {
            lock.unlockRead();
        }
    }

    protected void setPrototype(final int v8RuntimeHandle, final int objectHandle, final int prototypeHandle) {
        lock.lockRead();
        try {
            _setPrototype(v8RuntimeHandle, objectHandle, prototypeHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean enableDebugSupport(final int v8RuntimeHandle, final int port, final boolean waitForConnection) {
        lock.lockRead();
        try {
            return _enableDebugSupport(v8RuntimeHandle, port, waitForConnection);
        } finally {
            lock.unlockRead();
        }
    }

    protected void disableDebugSupport(final int v8RuntimeHandle) {
        lock.lockRead();
        try {
            _disableDebugSupport(v8RuntimeHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected void processDebugMessages(final int v8RuntimeHandle) {
        lock.lockRead();
        try {
            _processDebugMessages(v8RuntimeHandle);
        } finally {
            lock.unlockRead();
        }
    }

    protected int[] arrayGetIntegers(final int v8RuntimeHandle, final int objectHandle, final int index, final int length) {
        lock.lockRead();
        try {
            return _arrayGetIntegers(v8RuntimeHandle, objectHandle, index, length);
        } finally {
            lock.unlockRead();
        }
    }

    protected double[] arrayGetDoubles(final int v8RuntimeHandle, final int objectHandle, final int index, final int length) {
        lock.lockRead();
        try {
            return _arrayGetDoubles(v8RuntimeHandle, objectHandle, index, length);
        } finally {
            lock.unlockRead();
        }
    }

    protected boolean[] arrayGetBooleans(final int v8RuntimeHandle, final int objectHandle, final int index, final int length) {
        lock.lockRead();
        try {
            return _arrayGetBooleans(v8RuntimeHandle, objectHandle, index, length);
        } finally {
            lock.unlockRead();
        }
    }

    protected String[] arrayGetStrings(final int v8RuntimeHandle, final int objectHandle, final int index, final int length) {
        lock.lockRead();
        try {
            return _arrayGetStrings(v8RuntimeHandle, objectHandle, index, length);
        } finally {
            lock.unlockRead();
        }
    }

    protected int arrayGetIntegers(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, final int[] resultArray) {
        lock.lockRead();
        try {
            return _arrayGetIntegers(v8RuntimeHandle, objectHandle, index, length, resultArray);
        } finally {
            lock.unlockRead();
        }
    }

    protected int arrayGetDoubles(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, final double[] resultArray) {
        lock.lockRead();
        try {
            return _arrayGetDoubles(v8RuntimeHandle, objectHandle, index, length, resultArray);
        } finally {
            lock.unlockRead();
        }
    }

    protected int arrayGetBooleans(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, final boolean[] resultArray) {
        lock.lockRead();
        try {
            return _arrayGetBooleans(v8RuntimeHandle, objectHandle, index, length, resultArray);
        } finally {
            lock.unlockRead();
        }
    }

    protected int arrayGetStrings(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, final String[] resultArray) {
        lock.lockRead();
        try {
            return _arrayGetStrings(v8RuntimeHandle, objectHandle, index, length, resultArray);
        } finally {
            lock.unlockRead();
        }
    }

    protected void terminateExecution(final int v8RuntimeHandle) {
        lock.lockRead();
        try {
            _terminateExecution(v8RuntimeHandle);
        } finally {
            lock.unlockRead();
        }
    }

    private native void _initNewV8Object(int v8RuntimeHandle, int objectHandle);

    private native void _releaseRuntime(int v8RuntimeHandle);

    private native void _createIsolate(int v8RuntimeHandle, String globalAlias);

    private native int _executeIntegerScript(int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber);

    private native double _executeDoubleScript(int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber);

    private native String _executeStringScript(int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber);

    private native boolean _executeBooleanScript(int v8RuntimeHandle, final String script, final String scriptName, final int lineNumber);

    private native Object _executeScript(int v8RuntimeHandle, int expectedType, String script, String scriptName, int lineNumber);

    private native void _executeVoidScript(int v8RuntimeHandle, String script, String scriptName, int lineNumber);

    private native void _release(int v8RuntimeHandle, int objectHandle);

    private native boolean _contains(int v8RuntimeHandle, int objectHandle, final String key);

    private native String[] _getKeys(int v8RuntimeHandle, int objectHandle);

    private native int _getInteger(int v8RuntimeHandle, int objectHandle, final String key);

    private native boolean _getBoolean(int v8RuntimeHandle, int objectHandle, final String key);

    private native double _getDouble(int v8RuntimeHandle, int objectHandle, final String key);

    private native String _getString(int v8RuntimeHandle, int objectHandle, final String key);

    private native Object _get(int v8RuntimeHandle, int expectedType, final int objectHandle, final String key);

    private native int _executeIntegerFunction(int v8RuntimeHandle, int objectHandle, String name, int parametersHandle);

    private native double _executeDoubleFunction(int v8RuntimeHandle, int objectHandle, String name, int parametersHandle);

    private native String _executeStringFunction(int v8RuntimeHandle2, int handle, String name, int parametersHandle);

    private native boolean _executeBooleanFunction(int v8RuntimeHandle2, int handle, String name, int parametersHandle);

    private native Object _executeFunction(int v8RuntimeHandle, int expectedType, int objectHandle, String name, int parametersHandle);

    private native void _executeVoidFunction(int v8RuntimeHandle, int objectHandle, final String name, final int parametersHandle);

    private native boolean _equals(int v8RuntimeHandle, int objectHandle, int that);

    private native boolean _strictEquals(int v8RuntimeHandle, int objectHandle, int that);

    private native boolean _sameValue(int v8RuntimeHandle, int objectHandle, int that);

    private native int _identityHash(int v8RuntimeHandle, int objectHandle);

    private native void _add(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    private native void _addObject(int v8RuntimeHandle, int objectHandle, final String key, final int value);

    private native void _add(int v8RuntimeHandle, int objectHandle, final String key, final boolean value);

    private native void _add(int v8RuntimeHandle, int objectHandle, final String key, final double value);

    private native void _add(int v8RuntimeHandle, int objectHandle, final String key, final String value);

    private native void _addUndefined(int v8RuntimeHandle, int objectHandle, final String key);

    private native void _addNull(int v8RuntimeHandle, int objectHandle, final String key);

    private native void _registerJavaMethod(int v8RuntimeHandle, final int objectHandle, final String functionName, final int methodID, final boolean voidMethod);

    private native void _initNewV8Array(int v8RuntimeHandle, int arrayHandle);

    private native int _arrayGetSize(int v8RuntimeHandle, int arrayHandle);

    private native int _arrayGetInteger(int v8RuntimeHandle, int arrayHandle, int index);

    private native boolean _arrayGetBoolean(int v8RuntimeHandle, int arrayHandle, int index);

    private native double _arrayGetDouble(int v8RuntimeHandle, int arrayHandle, int index);

    private native String _arrayGetString(int v8RuntimeHandle, int arrayHandle, int index);

    private native Object _arrayGet(int v8RuntimeHandle, int expectedType, int arrayHandle, int index);

    private native void _addArrayIntItem(int v8RuntimeHandle, int arrayHandle, int value);

    private native void _addArrayBooleanItem(int v8RuntimeHandle, int arrayHandle, boolean value);

    private native void _addArrayDoubleItem(int v8RuntimeHandle, int arrayHandle, double value);

    private native void _addArrayStringItem(int v8RuntimeHandle, int arrayHandle, String value);

    private native void _addArrayObjectItem(int v8RuntimeHandle, int arrayHandle, int value);

    private native void _addArrayUndefinedItem(int v8RuntimeHandle, int arrayHandle);

    private native void _addArrayNullItem(int v8RuntimeHandle, int arrayHandle);

    private native int _getType(int v8RuntimeHandle, int objectHandle, final String key);

    private native int _getType(int v8RuntimeHandle, int objectHandle, final int index);

    private native int _getArrayType(int v8RuntimeHandle, int objectHandle);

    private native void _setPrototype(int v8RuntimeHandle, int objectHandle, int prototypeHandle);

    private native int _getType(int v8RuntimeHandle, int objectHandle, final int index, final int length);

    private native boolean _enableDebugSupport(int v8RuntimeHandle, int port, boolean waitForConnection);

    private native void _disableDebugSupport(int v8RuntimeHandle);

    private native void _processDebugMessages(int v8RuntimeHandle);

    private native double[] _arrayGetDoubles(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    private native int[] _arrayGetIntegers(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    private native boolean[] _arrayGetBooleans(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    private native String[] _arrayGetStrings(final int v8RuntimeHandle, final int objectHandle, final int index, final int length);

    private native int _arrayGetIntegers(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, int[] resultArray);

    private native int _arrayGetDoubles(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, double[] resultArray);

    private native int _arrayGetBooleans(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, boolean[] resultArray);

    private native int _arrayGetStrings(final int v8RuntimeHandle, final int objectHandle, final int index, final int length, String[] resultArray);

    private native void _terminateExecution(final int v8RuntimeHandle);

    void addObjRef() {
        objectReferences++;
    }

    void releaseObjRef() {
        objectReferences--;
    }

}
