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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.debug.DebugHandler.DebugEvent;
import com.eclipsesource.v8.debug.mirror.ArrayMirror;
import com.eclipsesource.v8.debug.mirror.Frame;
import com.eclipsesource.v8.debug.mirror.ValueMirror;

public class MirrorTest {

    private static String script = "      // 1  \n"
            + "function foo(a, b, c)  {   // 2  \n"
            + "  var integer = 7;         // 3  \n"
            + "  var boolean = false;     // 4  \n"
            + "  var obj = { 'foo' : 3 }; // 5  \n"
            + "  var array = [1,2,3];     // 6  \n"
            + "}                          // 7  \n"
            + "foo(1,2,'yes');            // 8  \n";
    private Object        result;
    private V8            v8;
    private DebugHandler  debugHandler;
    private BreakHandler  breakHandler;

    @Before
    public void setup() {
        V8.setFlags("--expose-debug-as=" + DebugHandler.DEBUG_OBJECT_NAME);
        v8 = V8.createV8Runtime();
        debugHandler = new DebugHandler(v8);
        debugHandler.setScriptBreakpoint("script", 6);
        breakHandler = mock(BreakHandler.class);
        debugHandler.addBreakHandler(breakHandler);
    }

    @After
    public void tearDown() {
        try {
            debugHandler.release();
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released.");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testGetNumberValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final V8Object eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror number = frame.getLocalValue(0);
                result = number.isValue() && number.isNumber();
                result = (Boolean) result && number.getValue().equals(7);
                number.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetBooleanValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final V8Object eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror booleanValue = frame.getLocalValue(1);
                result = booleanValue.isValue() && booleanValue.isBoolean();
                result = (Boolean) result && booleanValue.getValue().equals(false);
                booleanValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetObjectValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final V8Object eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror objectValue = frame.getLocalValue(2);
                result = objectValue.isValue() && objectValue.isObject();
                objectValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetArrayValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final V8Object eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror arrayValue = frame.getLocalValue(3);
                result = arrayValue.isValue() && arrayValue.isObject() && arrayValue.isArray();
                result = (Boolean) result && (((ArrayMirror) arrayValue).length() == 3);
                arrayValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    private void handleBreak(final BreakHandler handler) {
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                DebugEvent arg1 = (DebugEvent) invocation.getArguments()[0];
                ExecutionState arg2 = (ExecutionState) invocation.getArguments()[1];
                V8Object arg3 = (V8Object) invocation.getArguments()[2];
                V8Object arg4 = (V8Object) invocation.getArguments()[3];
                handler.onBreak(arg1, arg2, arg3, arg4);
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(V8Object.class), any(V8Object.class));
    }

}
