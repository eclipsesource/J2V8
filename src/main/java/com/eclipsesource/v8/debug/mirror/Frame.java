/*******************************************************************************
 * Copyright (c) 2016 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8.debug.mirror;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;

/**
 * Represents a single stack frame accessible from the
 * current execution state.
 */
public class Frame extends Mirror {

    private static final String IS_STRING      = "isString";
    private static final String IS_ARRAY       = "isArray";
    private static final String IS_BOOLEAN     = "isBoolean";
    private static final String IS_NUMBER      = "isNumber";
    private static final String IS_OBJECT      = "isObject";
    private static final String LOCAL_VALUE    = "localValue";
    private static final String IS_VALUE       = "isValue";
    private static final String SCOPE          = "scope";
    private static final String ARGUMENT_VALUE = "argumentValue";
    private static final String ARGUMENT_NAME  = "argumentName";
    private static final String LOCAL_COUNT    = "localCount";
    private static final String ARGUMENT_COUNT = "argumentCount";
    private static final String SCOPE_COUNT    = "scopeCount";
    private static final String LOCAL_NAME     = "localName";

    public Frame(final V8Object v8Object) {
        super(v8Object);
    }

    /**
     * Returns the number of accessible scopes from this stack frame.
     *
     * @return The number of accessible scopes
     */
    public int getScopeCount() {
        return v8Object.executeIntegerFunction(SCOPE_COUNT, null);
    }

    /**
     * Returns the number of arguments to this frame.
     *
     * @return The number of arguments passed to this frame.
     */
    public int getArgumentCount() {
        return v8Object.executeIntegerFunction(ARGUMENT_COUNT, null);
    }

    /**
     * Returns the name of the argument at the given index.
     *
     * @param index The index of the argument name to return.
     * @return The name of argument at the given index.
     */
    public String getArgumentName(final int index) {
        V8Array parameters = new V8Array(v8Object.getRuntime());
        parameters.push(index);
        try {
            return v8Object.executeStringFunction(ARGUMENT_NAME, parameters);
        } finally {
            parameters.release();
        }
    }

    /**
     * Returns the value of the argument at the given index.
     *
     * @param index The index of the argument value to return.
     * @return The value of argument at the given index.
     */
    public ValueMirror getArgumentValue(final int index) {
        V8Array parameters = new V8Array(v8Object.getRuntime());
        parameters.push(index);
        V8Object result = null;
        try {
            result = v8Object.executeObjectFunction(ARGUMENT_VALUE, parameters);
            if (!isValue(result)) {
                throw new IllegalStateException("Argument value is not a ValueMirror.");
            }
            return new ValueMirror(result);
        } finally {
            parameters.release();
            if (result != null) {
                result.release();
            }
        }
    }

    /**
     * Returns the value of the local variable at the given index.
     *
     * @param index The index of the local to return.
     * @return The value of local at the given index.
     */
    public ValueMirror getLocalValue(final int index) {
        V8Array parameters = new V8Array(v8Object.getRuntime());
        parameters.push(index);
        V8Object result = null;
        try {
            result = v8Object.executeObjectFunction(LOCAL_VALUE, parameters);
            if (!isValue(result)) {
                throw new IllegalStateException("Local value is not a ValueMirror.");
            }
            return createMirror(result);
        } finally {
            parameters.release();
            if (result != null) {
                result.release();
            }
        }
    }

    private boolean isValue(final V8Object mirror) {
        try {
            return mirror.executeBooleanFunction(IS_VALUE, null);
        } catch (V8ResultUndefined e) {
            return false;
        }
    }

    private boolean isObject(final V8Object mirror) {
        try {
            return mirror.executeBooleanFunction(IS_OBJECT, null);
        } catch (V8ResultUndefined e) {
            return false;
        }
    }

    private boolean isNumber(final V8Object mirror) {
        try {
            return mirror.executeBooleanFunction(IS_NUMBER, null);
        } catch (V8ResultUndefined e) {
            return false;
        }
    }

    private boolean isBoolean(final V8Object mirror) {
        try {
            return mirror.executeBooleanFunction(IS_BOOLEAN, null);
        } catch (V8ResultUndefined e) {
            return false;
        }
    }

    private boolean isArray(final V8Object mirror) {
        try {
            return mirror.executeBooleanFunction(IS_ARRAY, null);
        } catch (V8ResultUndefined e) {
            return false;
        }
    }

    private boolean isString(final V8Object mirror) {
        try {
            return mirror.executeBooleanFunction(IS_STRING, null);
        } catch (V8ResultUndefined e) {
            return false;
        }
    }

    private ValueMirror createMirror(final V8Object mirror) {
        if (isArray(mirror)) {
            return new ArrayMirror(mirror);
        }
        if (isObject(mirror)) {
            return new ObjectMirror(mirror);
        }
        if (isString(mirror)) {
            return new StringMirror(mirror);
        }
        if (isNumber(mirror)) {
            return new NumberMirror(mirror);
        }
        if (isBoolean(mirror)) {
            return new BooleanMirror(mirror);
        }
        return new ValueMirror(mirror);
    }

    /**
     * Returns the number of local variables in this frame.
     *
     * @return The number of local variables accessible from this stack frame.
     */
    public int getLocalCount() {
        return v8Object.executeIntegerFunction(LOCAL_COUNT, null);
    }

    /**
     * Returns the name of the local variable at the given index.
     *
     * @param index The index of the local variable name to return.
     * @return The name of local variable at the given index.
     */
    public String getLocalName(final int index) {
        V8Array parameters = new V8Array(v8Object.getRuntime());
        parameters.push(index);
        try {
            return v8Object.executeStringFunction(LOCAL_NAME, parameters);
        } finally {
            parameters.release();
        }
    }

    /**
     * Returns the scope at a given index.
     *
     * @param index The index
     * @return The scope
     */
    public Scope getScope(final int index) {
        V8Array parameters = new V8Array(v8Object.getRuntime());
        parameters.push(index);
        V8Object scope = null;
        try {
            scope = v8Object.executeObjectFunction(SCOPE, parameters);
            return new Scope(scope);
        } finally {
            parameters.release();
            if (scope != null) {
                scope.release();
            }
        }
    }

    @Override
    public boolean isFrame() {
        return true;
    }

}