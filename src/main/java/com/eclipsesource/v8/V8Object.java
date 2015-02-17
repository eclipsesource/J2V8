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
        this.v8 = v8;
        V8.checkThread();
        objectHandle = v8ObjectInstanceCounter++;
        initialize(v8.getV8RuntimeHandle(), objectHandle);
    }

    public boolean contains(final String key) {
        V8.checkThread();
        checkReleaesd();
        return v8._contains(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public String[] getKeys() {
        V8.checkThread();
        checkReleaesd();
        return v8._getKeys(v8.getV8RuntimeHandle(), objectHandle);
    }

    public int getType(final String key) throws V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        return v8._getType(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public int getInteger(final String key) throws V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        return v8._getInteger(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public boolean getBoolean(final String key) throws V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        return v8._getBoolean(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public double getDouble(final String key) throws V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        return v8._getDouble(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public String getString(final String key) throws V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        return v8._getString(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public V8Array getArray(final String key) throws V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        Object result = v8._get(v8.getV8RuntimeHandle(), V8_ARRAY, objectHandle, key);
        if ((result == null) || (result instanceof V8Array)) {
            return (V8Array) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Object getObject(final String key) throws V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        Object result = v8._get(v8.getV8RuntimeHandle(), V8_OBJECT, objectHandle, key);
        if ((result == null) || (result instanceof V8Object)) {
            return (V8Object) result;
        }
        throw new V8ResultUndefined();
    }

    public int executeIntFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8._executeIntFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public double executeDoubleFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8._executeDoubleFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public String executeStringFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8._executeStringFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public boolean executeBooleanFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException,
            V8ResultUndefined {
        V8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8._executeBooleanFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public V8Array executeArrayFunction(final String name, final V8Array parameters) {
        V8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        Object result = v8._executeFunction(v8.getV8RuntimeHandle(), V8_ARRAY, objectHandle, name, parametersHandle);
        if (result instanceof V8Array) {
            return (V8Array) result;
        }
        throw new V8ResultUndefined();
    }

    public V8Object executeObjectFunction(final String name, final V8Array parameters) {
        V8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        Object result = v8._executeFunction(v8.getV8RuntimeHandle(), V8_OBJECT, objectHandle, name, parametersHandle);
        if (result instanceof V8Object) {
            return (V8Object) result;
        }
        throw new V8ResultUndefined();
    }

    public void executeVoidFunction(final String name, final V8Array parameters) throws V8ScriptExecutionException {
        V8.checkThread();
        checkReleaesd();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        v8._executeVoidFunction(v8.getV8RuntimeHandle(), objectHandle, name, parametersHandle);
    }

    public V8Object add(final String key, final int value) {
        V8.checkThread();
        checkReleaesd();
        v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        return this;
    }

    public V8Object add(final String key, final boolean value) {
        V8.checkThread();
        checkReleaesd();
        v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        return this;
    }

    public V8Object add(final String key, final double value) {
        V8.checkThread();
        checkReleaesd();
        v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        return this;
    }

    public V8Object add(final String key, final String value) {
        V8.checkThread();
        checkReleaesd();
        if (value == null) {
            v8._addNull(v8.getV8RuntimeHandle(), objectHandle, key);
        } else if (value.equals(V8.getUndefined())) {
            v8._addUndefined(v8.getV8RuntimeHandle(), objectHandle, key);
        } else {
            v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
        }
        return this;
    }

    public V8Object add(final String key, final V8Value value) {
        V8.checkThread();
        checkReleaesd();
        if (value == null) {
            v8._addNull(v8.getV8RuntimeHandle(), objectHandle, key);
        } else if (value.equals(V8.getUndefined())) {
            v8._addUndefined(v8.getV8RuntimeHandle(), objectHandle, key);
        } else {
            v8._addObject(v8.getV8RuntimeHandle(), objectHandle, key, value.getHandle());
        }
        return this;
    }

    public V8Object addUndefined(final String key) {
        V8.checkThread();
        checkReleaesd();
        v8._addUndefined(v8.getV8RuntimeHandle(), objectHandle, key);
        return this;
    }

    public V8Object setPrototype(final V8Object value) {
        V8.checkThread();
        checkReleaesd();
        v8._setPrototype(v8.getV8RuntimeHandle(), objectHandle, value.getHandle());
        return this;
    }

    public V8Object registerJavaMethod(final JavaCallback callback, final String jsFunctionName) {
        V8.checkThread();
        checkReleaesd();
        v8.registerCallback(callback, getHandle(), jsFunctionName);
        return this;
    }

    public V8Object registerJavaMethod(final JavaVoidCallback callback, final String jsFunctionName) {
        V8.checkThread();
        checkReleaesd();
        v8.registerVoidCallback(callback, getHandle(), jsFunctionName);
        return this;
    }

    public V8Object registerJavaMethod(final Object object, final String methodName, final String jsFunctionName,
            final Class<?>[] parameterTypes) {
        V8.checkThread();
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
        V8.checkThread();
        checkReleaesd();
        return executeStringFunction("toString", null);
    }

    static class Undefined extends V8Object {

        @Override
        public boolean isUndefined() {
            V8.checkThread();
            return true;
        }

        @Override
        public boolean isReleased() {
            V8.checkThread();
            return true;
        }

        @Override
        public void release() {
            V8.checkThread();
        }

        @Override
        public String toString() {
            return "undefined";
        }

        @Override
        public boolean equals(final Object that) {
            V8.checkThread();
            if ((that instanceof V8Object) && ((V8Object) that).isUndefined()) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            V8.checkThread();
            return 919;
        }

        @Override
        public V8Object add(final String key, final boolean value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final double value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final int value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final String value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object add(final String key, final V8Value value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object addUndefined(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array executeArrayFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean executeBooleanFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public double executeDoubleFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int executeIntFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object executeObjectFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String executeStringFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public void executeVoidFunction(final String name, final V8Array parameters) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Array getArray(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBoolean(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDouble(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getHandle() {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInteger(final String key) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getKeys() {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object getObject(final String key) throws V8ResultUndefined {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(final String key) throws V8ResultUndefined {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public int getType(final String key) throws V8ResultUndefined {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaCallback callback, final String jsFunctionName) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final JavaVoidCallback callback, final String jsFunctionName) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object registerJavaMethod(final Object object, final String methodName, final String jsFunctionName, final Class<?>[] parameterTypes) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

        @Override
        public V8Object setPrototype(final V8Object value) {
            V8.checkThread();
            throw new UnsupportedOperationException();
        }

    }

}
