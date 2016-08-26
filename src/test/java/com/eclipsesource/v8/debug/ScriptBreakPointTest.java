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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.debug.DebugHandler.DebugEvent;

public class ScriptBreakPointTest {

    private static String script = "// 1 \n"
            + "function foo() {     // 2 \n"
            + "  var x = 7;         // 3 \n"
            + "  var y = x + 1;     // 4 \n"
            + "}                    // 5 \n"
            + "foo();               // 6 \n";
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
                throw new IllegalStateException("V8Runtimes not properly released");
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

    @Test
    public void testFalseConditionDoesntTriggerBreak() {
        DebugHandler handler = new DebugHandler(v8);
        int breakPointID = handler.setScriptBreakpoint("script", 3);
        ScriptBreakPoint breakPoint = handler.getScriptBreakPoint(breakPointID);
        breakPoint.setCondition("false");
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(0)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));
        breakPoint.release();
        handler.release();
    }

    @Test
    public void testTrueConditionTriggersBreak() {
        DebugHandler handler = new DebugHandler(v8);
        int breakPointID = handler.setScriptBreakpoint("script", 3);
        ScriptBreakPoint breakPoint = handler.getScriptBreakPoint(breakPointID);
        breakPoint.setCondition("true;");
        BreakHandler breakHandler = mock(BreakHandler.class);
        handler.addBreakHandler(breakHandler);

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(1)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));
        breakPoint.release();
        handler.release();
    }

    @Test
    public void testGetCondition() {
        DebugHandler handler = new DebugHandler(v8);
        int breakPointID = handler.setScriptBreakpoint("script", 3);
        ScriptBreakPoint breakPoint = handler.getScriptBreakPoint(breakPointID);
        breakPoint.setCondition("x=7;");

        String result = breakPoint.getCondition();

        assertEquals("x=7;", result);
        breakPoint.release();
        handler.release();
    }

    @Test
    public void testGetNoConditionReturnsUndefined() {
        DebugHandler handler = new DebugHandler(v8);
        int breakPointID = handler.setScriptBreakpoint("script", 3);
        ScriptBreakPoint breakPoint = handler.getScriptBreakPoint(breakPointID);

        String result = breakPoint.getCondition();

        assertEquals("undefined", result);
        breakPoint.release();
        handler.release();
    }
}
