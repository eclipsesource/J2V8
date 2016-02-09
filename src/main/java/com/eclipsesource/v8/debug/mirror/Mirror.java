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

import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8Object;

/**
 * A mirror is used to represent a copy (mirror) of a runtime object
 * during a debug session.
 *
 * Mirror hierarchy:
 *  - Mirror
 *     - ValueMirror
 *       - UndefinedMirror
 *       - NullMirror
 *       - NumberMirror
 *       - StringMirror
 *       - ObjectMirror
 *         - FunctionMirror
 *           - UnresolvedFunctionMirror
 *         - ArrayMirror
 *         - DateMirror
 *         - RegExpMirror
 *         - ErrorMirror
 *         - PromiseMirror
 *     - PropertyMirror
 *     - InternalPropertyMirror
 *     - FrameMirror
 *     - ScriptMirror
 */
public class Mirror implements Releasable {

    private static final String IS_UNDEFINED = "isUndefined";

    protected V8Object v8Object;

    Mirror(final V8Object v8Object) {
        this.v8Object = v8Object.twin();
    }

    /**
     * Returns true if this mirror object points to the type 'undefined'.
     * False otherwise.
     *
     * @return True iff this mirror object points to an 'undefined' type.
     */
    public boolean isUndefined() {
        return v8Object.executeBooleanFunction(IS_UNDEFINED, null);
    }

    /**
     * Returns true if this mirror object points to a 'value' type.
     *
     * @return True iff this mirror object points to a 'value' type.
     */
    public boolean isValue() {
        return false;
    }

    /**
     * Returns true if this mirror object points to 'null'.
     *
     * @return True iff this mirror object points to a 'null'.
     */
    public boolean isNull() {
        return false;
    }

    /**
     * Returns true if this mirror object points to a 'boolean' type.
     *
     * @return True iff this mirror object points to a 'boolean' type.
     */
    public boolean isBoolean() {
        return false;
    }

    /**
     * Returns true if this mirror object points to a 'number' type.
     *
     * @return True iff this mirror object points to a 'number' type.
     */
    public boolean isNumber() {
        return false;
    }

    /**
     * Returns true if this mirror object points to a 'String' type.
     *
     * @return True iff this mirror object points to a 'String' type.
     */
    public boolean isString() {
        return false;
    }

    /**
     * Returns true if this mirror object points to an 'Object' type.
     *
     * @return True iff this mirror object points to an 'Object' type.
     */
    public boolean isObject() {
        return false;
    }

    /**
     * Returns true if this mirror object points to a 'Function' type.
     *
     * @return True iff this mirror object points to a 'Function' type.
     */
    public boolean isFunction() {
        return false;
    }

    /**
     * Returns true if this mirror object points to an 'Array' type.
     *
     * @return True iff this mirror object points to an 'Array' type.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Returns true if this mirror object points to a 'Function' type.
     *
     * @return True iff this mirror object points to a 'Function' type.
     */
    public boolean isFrame() {
        return false;
    }

    @Override
    public void release() {
        if ((v8Object != null) && !v8Object.isReleased()) {
            v8Object.release();
            v8Object = null;
        }
    }
}
