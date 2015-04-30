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

import java.lang.reflect.Method;

public class V8Object extends V8Value {

    protected V8Object() {

    }

    protected V8Object(final V8 v8, final int objectHandle) {
        if (v8 == null) {
            this.v8 = (V8) this;
        }
        this.objectHandle = objectHandle;
        released = false;
    }

    public V8Object(final V8 v8) {
        this(v8, false);
    }

    protected V8Object(final V8 v8, final boolean unlock) {
        if (unlock) {
            V8.lock.unlockRead();
        }
        try {
            this.v8 = v8;
            v8.checkThread();
            objectHandle = v8ObjectInstanceCounter++;
            initialize(v8.getV8RuntimeHandle(), objectHandle);
        } finally {
            if (unlock) {
                V8.lock.lockRead();
            }
        }
    }

    public boolean contains(final String key) {
        v8.checkThread();
        checkReleaesd();
        return v8.contains(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public String[] getKeys() {
        v8.checkThread();
        checkReleaesd();
        return v8.getKeys(v8.getV8RuntimeHandle(), objectHandle);
    }

    public int getType(final String key) throws V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        return v8.getType(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public int getInteger(final String key) throws V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        return v8.getInteger(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public boolean getBoolean(final String key) throws V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        return v8.getBoolean(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public double getDouble(final String key) throws V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        return v8.getDouble(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public String getString(final String key) throws V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        return v8.getString(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public V8Array getArray(final String key) throws V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        Object result = v8.get(v8.getV8RuntimeHandle(), V8_ARRAY, objectHandle, key);
        if ((result == null) || (result instanceof V8Array)) {
            return (V8Array) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Object getObject(final String key) throws V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        Object result = v8.get(v8.getV8RuntimeHandle(), V8_OBJECT, objectHandle, key);
        if ((result == null) || (result instanceof V8Object)) {
            return (V8Object) result;
        }
        throw new V8ResultUndefined();
    }

    public int executeIntegerFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8.executeIntegerFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public double executeDoubleFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8.executeDoubleFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public String executeStringFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8.executeStringFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public boolean executeBooleanFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8.executeBooleanFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public V8Array executeArrayFunction(final String name, final V8Array parameters) {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        Object result = v8.executeFunction(v8.getV8RuntimeHandle(), V8_ARRAY, objectHandle, name, parametersHandle);
        if (result instanceof V8Array) {
            return (V8Array) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Object executeObjectFunction(final String name, final V8Array parameters) {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        Object result = v8.executeFunction(v8.getV8RuntimeHandle(), V8_OBJECT, objectHandle, name, parametersHandle);
        if (result instanceof V8Object) {
            return (V8Object) result;
        }
        throw new V8ResultUndefined();
    }

    public Object executeFunction(final String name, final V8Array parameters) {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8.executeFunction(v8.getV8RuntimeHandle(), V8_OBJECT, objectHandle, name, parametersHandle);
    }

    public void executeVoidFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException {
        v8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        v8.executeVoidFunction(v8.getV8RuntimeHandle(), objectHandle, name, parametersHandle);
    }

    public V8Object add(final String key, final int value) {
        v8.checkThread();
        checkReleaesd();
        v8.add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        return this;
    }

    public V8Object add(final String key, final boolean value) {
        v8.checkThread();
        checkReleaesd();
        v8.add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        return this;
    }

    public V8Object add(final String key, final double value) {
        v8.checkThread();
        checkReleaesd();
        v8.add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        return this;
    }

    public V8Object add(final String key, final String value) {
        v8.checkThread();
        checkReleaesd();
        if (value == null) {
            v8.addNull(v8.getV8RuntimeHandle(), objectHandle, key);
        } else if (value.equals(V8.getUndefined())) {
            v8.addUndefined(v8.getV8RuntimeHandle(), objectHandle, key);
        } else {
            v8.add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        }
        return this;
    }

    public V8Object add(final String key, final V8Value value) {
        v8.checkThread();
        checkReleaesd();
        if (value == null) {
            v8.addNull(v8.getV8RuntimeHandle(), objectHandle, key);
        } else if (value.equals(V8.getUndefined())) {
            v8.addUndefined(v8.getV8RuntimeHandle(), objectHandle, key);
        } else {
            v8.addObject(v8.getV8RuntimeHandle(), objectHandle, key, value.getHandle());
        }
        return this;
    }

    public V8Object addUndefined(final String key) {
        v8.checkThread();
        checkReleaesd();
        v8.addUndefined(v8.getV8RuntimeHandle(), objectHandle, key);
        return this;
    }

    public V8Object setPrototype(final V8Object value) {
        v8.checkThread();
        checkReleaesd();
        v8.setPrototype(v8.getV8RuntimeHandle(), objectHandle, value.getHandle());
        return this;
    }

    public V8Object registerJavaMethod(final JavaCallback callback, final String jsFunctionName) {
        v8.checkThread();
        checkReleaesd();
        v8.registerCallback(callback, getHandle(), jsFunctionName);
        return this;
    }

    public V8Object registerJavaMethod(final JavaVoidCallback callback, final String jsFunctionName) {
        v8.checkThread();
        checkReleaesd();
        v8.registerVoidCallback(callback, getHandle(), jsFunctionName);
        return this;
    }

    public V8Object registerJavaMethod(final Object object, final String methodName, final String jsFunctionName,
            final Class<?>[] parameterTypes) {
        v8.checkThread();
        checkReleaesd();
        try {
            Method method = object.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            v8.registerCallback(object, method, getHandle(), jsFunctionName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        }
        return this;
    }

    @Override
    public String toString() {
        v8.checkThread();
        checkReleaesd();
        return executeStringFunction("toString", null);
    }

    static class Undefined extends V8Object {

        public Undefined() {
        }

        @Override
        public boolean isUndefined() {
            return true;
        }

        @Override
        public boolean isReleased() {
            return true;
        }

        @Override
        public void release() {
        }

        @Override
        public String toString() {
            return "undefined";
        }

        @Override
        public boolean equals(final Object that) {
            if ((that instanceof V8Object) && ((V8Object) that).isUndefined()) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 919;
        }

        @Override
        public V8Object add(final String key, final boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8 getRutime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final V8Value value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object addUndefined(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array executeArrayFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean executeBooleanFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double executeDoubleFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeIntegerFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object executeObjectFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String executeStringFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void executeVoidFunction(final String name, final V8Array parameters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array getArray(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBoolean(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDouble(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getHandle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInteger(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object getObject(final String key) throws V8ResultUndefined {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(final String key) throws V8ResultUndefined {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final String key) throws V8ResultUndefined {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaCallback callback, final String jsFunctionName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaVoidCallback callback, final String jsFunctionName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final Object object, final String methodName, final String jsFunctionName, final Class<?>[] parameterTypes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object setPrototype(final V8Object value) {
            throw new UnsupportedOperationException();
        }

    }

}
