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
package com.eclipsesource.v8.debug;

import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * Represents a single stack frame accessible from the
 * current execution state.
 */
public class Frame implements Releasable {

    private static final String LOCAL_COUNT    = "localCount";
    private static final String ARGUMENT_COUNT = "argumentCount";
    private static final String SCOPE_COUNT    = "scopeCount";

    private V8Object v8Object;

    Frame(final V8Object v8Object) {
        this.v8Object = v8Object.twin();
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
     * Returns the number of local variables in this frame.
     *
     * @return The number of local variables accessible from this stack frame.
     */
    public int getLocalCount() {
        return v8Object.executeIntegerFunction(LOCAL_COUNT, null);
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
            scope = v8Object.executeObjectFunction("scope", parameters);
            return new Scope(scope);
        } finally {
            parameters.release();
            if (scope != null) {
                scope.release();
            }
        }
    }

    @Override
    public void release() {
        if ((v8Object != null) && !v8Object.isReleased()) {
            v8Object.release();
            v8Object = null;
        }
    }

}