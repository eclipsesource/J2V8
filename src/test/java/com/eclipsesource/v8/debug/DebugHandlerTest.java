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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;

public class DebugHandlerTest {

    private static String script = "// 1 \n"
            + "function foo() {     // 2 \n"
            + "  var x = 7;         // 3 \n"
            + "  var y = x + 1;     // 4 \n"
            + "}                    // 5 \n"
            + "foo();               // 6 \n";
    private V8            v8;
    private Object        result = null;

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
                throw new IllegalStateException("V8Runtimes not properly released.");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testCreateDebugHandler() {
        DebugHandler handler = new DebugHandler(v8);

        assertNotNull(handler);
        handler.release();
    }

    @Test
    public void testSetBreakpoint() {
        DebugHandler handler = new DebugHandler(v8);
        handler.setScriptBreakpoint("script", 3);
        handler.addBreakHandler(new BreakHandler() {

            @Override
            public void onBreak(final int event, final ExecutionState state, final V8Object eventData, final V8Object data) {
                result = event == 1 ? Boolean.TRUE : Boolean.FALSE;
            }
        });
        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
        handler.release();
    }

    @Test
    public void testSetBreakpointByFunction() {
        DebugHandler handler = new DebugHandler(v8);
        v8.executeScript(script, "script", 0);
        V8Function function = (V8Function) v8.get("foo");
        handler.setBreakpoint(function);
        handler.addBreakHandler(new BreakHandler() {

            @Override
            public void onBreak(final int event, final ExecutionState state, final V8Object eventData, final V8Object data) {
                result = event == 1 ? Boolean.TRUE : Boolean.FALSE;
            }
        });

        function.call(null, null);

        assertTrue((Boolean) result);
        handler.release();
        function.release();
    }

}
