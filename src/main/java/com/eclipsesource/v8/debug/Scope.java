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
import com.eclipsesource.v8.V8Object;

/**
 * Represents a JavaScope scope accessible from the current stack frame
 * during debug break.
 *
 */
public class Scope implements Releasable {

    /**
     * Represents the different types of scopes available.
     */
    public static enum ScopeType {
        Global(0), Local(1), With(2), Closure(3), Catch(4), Block(5), Script(6);
        int index;

        private ScopeType(final int index) {
            this.index = index;
        }
    }

    private V8Object v8Object;

    Scope(final V8Object v8Object) {
        this.v8Object = v8Object.twin();
    }

    /**
     * Returns the type of this scope.
     *
     * @return The type of scope.
     */
    public ScopeType getType() {
        return ScopeType.values()[v8Object.executeIntegerFunction("scopeType", null)];
    }

    @Override
    public void release() {
        if ((v8Object != null) && !v8Object.isReleased()) {
            v8Object.release();
            v8Object = null;
        }
    }

}