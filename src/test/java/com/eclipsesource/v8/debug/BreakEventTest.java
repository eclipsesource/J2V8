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

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.debug.DebugHandler.DebugEvent;

public class BreakEventTest {

    private static String script = "// 0 \n"
            + "function foo() {     // 1 \n"
            + "  var x = 7;         // 2 \n"
            + "  var y = x + 1;     // 3 \n"
            + "}                    // 4 \n"
            + "foo();               // 5 \n";
    private V8            v8;
    private Object        result = false;

    @Before
    public void setup() {
        V8.setFlags("--expose-debug-as=" + DebugHandler.DEBUG_OBJECT_NAME);
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
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
    public void testGetSourceLine() {
        DebugHandler handler = new DebugHandler(v8);
        handler.setScriptBreakpoint("script", 3);
        handler.addBreakHandler(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent type, final ExecutionState state, final EventData eventData, final V8Object data) {
                result = ((BreakEvent) eventData).getSourceLine() == 3;
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
        handler.release();
    }

    @Test
    public void testGetSourceColumn() {
        DebugHandler handler = new DebugHandler(v8);
        handler.setScriptBreakpoint("script", 3);
        handler.addBreakHandler(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent type, final ExecutionState state, final EventData eventData, final V8Object data) {
                result = (((BreakEvent) eventData).getSourceColumn() == 2);
                result = (Boolean) result && ((BreakEvent) eventData).getSourceLineText().equals("  var y = x + 1;     // 3 ");
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
        handler.release();
    }

    @Test
    public void testGetSourceLineText() {
        DebugHandler handler = new DebugHandler(v8);
        handler.setScriptBreakpoint("script", 3);
        handler.addBreakHandler(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent type, final ExecutionState state, final EventData eventData, final V8Object data) {
                result = ((BreakEvent) eventData).getSourceLineText().equals("  var y = x + 1;     // 3 ");
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
        handler.release();
    }
}
