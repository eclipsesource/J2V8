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

import com.eclipsesource.v8.V8Object;

/**
 * Represents JavaScript 'Function' Mirrors
 */
public class FunctionMirror extends ObjectMirror {

    FunctionMirror(final V8Object v8Object) {
        super(v8Object);
    }

    /**
     * Returns the name of this Function.
     *
     * @return The name of this function
     */
    public String getName() {
        return v8Object.executeStringFunction("name", null);
    }

    @Override
    public boolean isFunction() {
        return true;
    }

}
