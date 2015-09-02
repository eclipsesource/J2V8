/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Contributors:
 * EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

/**
 * An exception used to indicate that a script failed to compile.
 */
@SuppressWarnings("serial")
public class V8ScriptCompilationException extends V8ScriptException {

    V8ScriptCompilationException(final String fileName, final int lineNumber,
                                 final String message, final String sourceLine, final int startColumn, final int endColumn) {
        super(fileName, lineNumber, message, sourceLine, startColumn, endColumn, null, null);
    }

}
