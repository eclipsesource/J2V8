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
 * A mirror is used to represent a copy (mirror) of a runtime object
 * during a debug session.
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

    @Override
    public void release() {
        if ((v8Object != null) && !v8Object.isReleased()) {
            v8Object.release();
            v8Object = null;
        }
    }
}
