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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.debug.DebugHandler.DebugEvent;
import com.eclipsesource.v8.debug.mirror.Frame;

public class ExecutionStateTest {

    private static String script = "// 1  \n"
            + "function foo() {     // 2  \n"
            + "  bar();             // 3  \n"
            + "  var y = 2 + 1;     // 4  \n"
            + "}                    // 5  \n"
            + "function bar() {     // 6  \n"
            + "  var x = 8;         // 7  \n"
            + "  return x+1;        // 8  \n"
            + "}                    // 9  \n"
            + "foo();               // 10 \n";
    private Object        result;
    private V8            v8;
    private DebugHandler  debugHandler;
    private BreakHandler  breakHandler;

    @Before
    public void setup() {
        V8.setFlags("--expose-debug-as=" + DebugHandler.DEBUG_OBJECT_NAME);
        v8 = V8.createV8Runtime();
        debugHandler = new DebugHandler(v8);
        debugHandler.setScriptBreakpoint("script", 2);
        breakHandler = mock(BreakHandler.class);
        debugHandler.addBreakHandler(breakHandler);
    }

    @After
    public void tearDown() {
        try {
            debugHandler.release();
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testStepNext() {
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ExecutionState state = (ExecutionState) invocation.getArguments()[1];
                state.prepareStep(StepAction.STEP_NEXT);
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(5)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));
    }

    @Test
    public void testStepOut() {
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ExecutionState state = (ExecutionState) invocation.getArguments()[1];
                state.prepareStep(StepAction.STEP_OUT);
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(2)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));
    }

    @Test
    public void testStepIn() {
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ExecutionState state = (ExecutionState) invocation.getArguments()[1];
                state.prepareStep(StepAction.STEP_IN);
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));

        v8.executeScript(script, "script", 0);

        verify(breakHandler, times(8)).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));
    }

    @Test
    public void testGetFrameCount() {
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ExecutionState state = (ExecutionState) invocation.getArguments()[1];
                result = state.getFrameCount();
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));

        v8.executeScript(script, "script", 0);

        assertEquals(2, result);
    }

    @Test
    public void testGetFrame() {
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ExecutionState state = (ExecutionState) invocation.getArguments()[1];
                Frame frame0 = state.getFrame(0);
                Frame frame1 = state.getFrame(1);
                result = (frame0 != null) && (frame1 != null);
                frame0.release();
                frame1.release();
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }
}
