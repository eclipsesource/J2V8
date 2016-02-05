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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;

public class ScriptBreakPointTest {

    private V8            v8;
    private DebugHandler  handler;

    @Before
    public void setup() {
        V8.setFlags("--expose-debug-as=" + DebugHandler.DEBUG_OBJECT_NAME);
        v8 = V8.createV8Runtime();
        handler = new DebugHandler(v8);
    }

    @After
    public void tearDown() {
        handler.release();
        try {
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released.");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testGetBreakpointNumber() {
        int breakpointID_0 = handler.setScriptBreakpoint("script", 3);
        int breakpointID_1 = handler.setScriptBreakpoint("script", 4);

        ScriptBreakPoint breakpoint_0 = handler.getScriptBreakPoint(breakpointID_0);
        ScriptBreakPoint breakpoint_1 = handler.getScriptBreakPoint(breakpointID_1);

        assertEquals(breakpointID_0, breakpoint_0.getBreakPointNumber());
        assertEquals(breakpointID_1, breakpoint_1.getBreakPointNumber());
        breakpoint_0.release();
        breakpoint_1.release();
    }

    @Test
    public void testGetLineNumber() {
        int breakpointID = handler.setScriptBreakpoint("script", 3);

        ScriptBreakPoint breakpoint = handler.getScriptBreakPoint(breakpointID);

        assertEquals(3, breakpoint.getLine());
        breakpoint.release();
    }
}
