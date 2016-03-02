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

/**
 * Represents a JS Script location.
 */
public class SourceLocation {

    private final String scriptName;
    private final int    position;
    private final int    line;
    private final int    column;
    private final int    start;
    private final int    end;

    /**
     * Represents a JS Script Source Location
     * @param scriptName The name of the script
     * @param position The position in the script
     * @param line The line number
     * @param column The column number
     * @param start The start of this location
     * @param end The end of this location
     */
    public SourceLocation(final String scriptName, final int position, final int line, final int column, final int start, final int end) {
        this.scriptName = scriptName;
        this.position = position;
        this.line = line;
        this.column = column;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return scriptName + " : " + position + " : " + line + " : " + column + " : " + start + " : " + end;
    }

    /**
     * Returns the name of the script for this SourceLocation.
     * @return The name of the script
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Returns the position of this SourceLocation.
     * @return The position of this SourceLocation.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Returns the line number of this SourceLocation.
     * @return The line number of this SourceLocation.
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number of this SourceLocation.
     * @return The column number of this SourceLocation.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the start of this SourceLocation.
     * @return The start of this SourceLocation.
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the end of this SourceLocation.
     * @return The end of this SourceLocation.
     */
    public int getEnd() {
        return end;
    }

}
