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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.debug.DebugHandler.DebugEvent;
import com.eclipsesource.v8.debug.mirror.Frame;
import com.eclipsesource.v8.debug.mirror.FunctionMirror;
import com.eclipsesource.v8.debug.mirror.Scope;
import com.eclipsesource.v8.debug.mirror.SourceLocation;
import com.eclipsesource.v8.debug.mirror.ValueMirror;

public class FrameTest {

    private static String script = "    // 1  \n"
            + "function foo(a, b, c)  { // 2  \n"
            + "  var x = 7;             // 3  \n"
            + "  var y = x + 1;         // 4  \n"
            + "  var z = { 'foo' : 3 }; // 5  \n"
            + "}                        // 6  \n"
            + "foo(1,2,'yes');          // 7 \n";
    private Object        result;
    private V8            v8;
    private DebugHandler  debugHandler;
    private BreakHandler  breakHandler;

    @Before
    public void setup() {
        V8.setFlags("--expose-debug-as=" + DebugHandler.DEBUG_OBJECT_NAME);
        v8 = V8.createV8Runtime();
        debugHandler = new DebugHandler(v8);
        debugHandler.setScriptBreakpoint("script", 5);
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
    public void testGetFunctionMirror() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                result = frame.getFunction();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertEquals("foo", ((FunctionMirror) result).getName());
        ((FunctionMirror) result).release();
    }

    @Test
    public void testGetSourceLocation() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                result = frame.getSourceLocation();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertEquals(5, ((SourceLocation) result).getLine());
        assertEquals("script", ((SourceLocation) result).getScriptName());
        assertEquals(0, ((SourceLocation) result).getColumn());
    }

    @Test
    public void testGetLocalCount() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                result = frame.getLocalCount();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertEquals(3, result);
    }

    @Test
    public void testGetArgumentCount() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                result = frame.getArgumentCount();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertEquals(3, result);
    }

    @Test
    public void testGetScopeCount() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                result = frame.getScopeCount();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertEquals(3, result);
    }

    @Test
    public void testGetScope() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                Scope scope0 = frame.getScope(0);
                Scope scope1 = frame.getScope(1);
                result = (scope0 != null) && (scope1 != null);
                scope0.release();
                scope1.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetLocalNames() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                int argumentCount = frame.getLocalCount();
                String local1 = frame.getLocalName(0);
                String local2 = frame.getLocalName(1);
                String local3 = frame.getLocalName(2);
                result = argumentCount == 3;
                result = (Boolean) result && local1.equals("x");
                result = (Boolean) result && local2.equals("y");
                result = (Boolean) result && local3.equals("z");
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetArgumentNames() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                int argumentCount = frame.getArgumentCount();
                String arg1 = frame.getArgumentName(0);
                String arg2 = frame.getArgumentName(1);
                String arg3 = frame.getArgumentName(2);
                result = argumentCount == 3;
                result = (Boolean) result && arg1.equals("a");
                result = (Boolean) result && arg2.equals("b");
                result = (Boolean) result && arg3.equals("c");
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetArgumentValues() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                int argumentCount = frame.getArgumentCount();
                ValueMirror arg1 = frame.getArgumentValue(0);
                ValueMirror arg2 = frame.getArgumentValue(1);
                ValueMirror arg3 = frame.getArgumentValue(2);
                result = argumentCount == 3;
                result = (Boolean) result && arg1.getValue().equals(1);
                result = (Boolean) result && arg2.getValue().equals(2);
                result = (Boolean) result && arg3.getValue().equals("yes");
                arg1.release();
                arg2.release();
                arg3.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetLocalValues() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                int argumentCount = frame.getLocalCount();
                ValueMirror local1 = frame.getLocalValue(0);
                ValueMirror local2 = frame.getLocalValue(1);
                ValueMirror local3 = frame.getLocalValue(2);
                result = argumentCount == 3;
                result = (Boolean) result && local1.getValue().equals(7);
                result = (Boolean) result && local2.getValue().equals(8);
                V8Object z = (V8Object) local3.getValue();
                result = (Boolean) result && (z.getInteger("foo") == 3);
                local1.release();
                local2.release();
                local3.release();
                z.release();
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
                EventData arg3 = (EventData) invocation.getArguments()[2];
                V8Object arg4 = (V8Object) invocation.getArguments()[3];
                handler.onBreak(arg1, arg2, arg3, arg4);
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));
    }

}
