/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

/**
 * A V8Value that represents a JavaScript function.
 * JavaScript functions cannot be created in Java, but
 * can be returned as the result of invoking a JS script
 * or JS Function.
 */
public class V8Function extends V8Object {

    protected V8Function(final V8 v8) {
        super(v8);
    }

    @Override
    protected V8Value createTwin() {
        return new V8Function(v8);
    }

    /*
     * (non-Javadoc)
     * @see com.eclipsesource.v8.V8Object#twin()
     */
    @Override
    public V8Function twin() {
        return (V8Function) super.twin();
    }

    /**
     * Invoke the JavaScript function on the current runtime.
     *
     * @param receiver The object on which to call the function on. The
     * receiver will be mapped to 'this' in JavaScript. If receiver is null
     * or undefined, then the V8 runtime will be used instead.
     * @param parameters The parameters passed to the JS Function.
     *
     * @return The result of JavaScript function.
     */
    public Object call(V8Object receiver, final V8Array parameters) {
        v8.checkThread();
        checkReleaesd();
        receiver = receiver != null ? receiver : v8;
        long parametersHandle = parameters == null ? -1 : parameters.getHandle();
        long receiverHandle = receiver.isUndefined() ? v8.objectHandle : receiver.objectHandle;
        return v8.executeFunction(v8.getV8RuntimePtr(), receiverHandle, objectHandle, parametersHandle);
    }
}
