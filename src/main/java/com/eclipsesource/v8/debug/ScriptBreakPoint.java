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
 * Represents a BreakPoint.
 */
public class ScriptBreakPoint implements Releasable {

    private static final String LINE   = "line";
    private static final String NUMBER = "number";

    private V8Object v8Object;

    ScriptBreakPoint(final V8Object v8Object) {
        this.v8Object = v8Object.twin();
    }

    /**
     * Returns the ID of this breakpoint.
     *
     * @return The ID (breakpoint number) of this breakpoint.
     */
    public int getBreakPointNumber() {
        return v8Object.executeIntegerFunction(NUMBER, null);
    }

    /**
     * Returns the line number of this breakpoint.
     *
     * @return The line number of this breakpoint.
     */
    public int getLine() {
        return v8Object.executeIntegerFunction(LINE, null);
    }

    @Override
    public void release() {
        if ((v8Object != null) && !v8Object.isReleased()) {
            v8Object.release();
            v8Object = null;
        }
    }
}
