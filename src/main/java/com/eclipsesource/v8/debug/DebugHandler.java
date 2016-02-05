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

import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;

/**
 * The entry point for the Debug API. The debug API is a Java API
 * that exposes V8's JavaScript API.
 *
 * The API requires that V8 be initialized with the runtime flag
 * '--expose-debug-as=__j2v8_debug_handler'.
 */
public class DebugHandler implements Releasable {

    public static enum DebugEvent {
        Undefined(0), Break(1), Exception(2), NewFunction(3), BeforeCompile(4), AfterCompile(5), CompileError(6), PromiseError(7), AsyncTaskEvent(8);
        int index;

        DebugEvent(final int index) {
            this.index = index;
        }
    }

    public static String DEBUG_OBJECT_NAME = "__j2v8_Debug";

    private static final String DEBUG_BREAK_HANDLER            = "__j2v8_debug_handler";
    private static final String SET_SCRIPT_BREAK_POINT_BY_NAME = "setScriptBreakPointByName";
    private static final String SET_BREAK_POINT                = "setBreakPoint";
    private static final String SET_LISTENER                   = "setListener";
    private static final String V8_DEBUG_OBJECT                = "Debug";

    private V8                 runtime;
    private V8Object           debugObject;
    private List<BreakHandler> breakHandlers = null;

    /**
     * Creates the Debug Handler for a particular V8 runtime.
     * Before the runtime was created, V8.setFlags("expose-debug-as=__j2v8_debug_handler");
     * must be called.
     *
     * @param runtime The runtime on which to create the Debug Handler.
     */
    public DebugHandler(final V8 runtime) {
        this.runtime = runtime;
        setupDebugObject(runtime);
        setupBreakpointHandler();
    }

    /**
     * Adds a handler to be notified when a breakpoint is hit.
     *
     * @param hanlder The handler to notify.
     */
    public void addBreakHandler(final BreakHandler hanlder) {
        runtime.getLocker().checkThread();
        if (breakHandlers == null) {
            breakHandlers = new ArrayList<BreakHandler>();
        }
        breakHandlers.add(hanlder);
    }

    /**
     * Removes a handler from the list of breakpoint handlers.
     * If the handler is not present in the list, the list is unchanged.
     *
     * @param handler The handler to remove.
     */
    public void removeBreakHandler(final BreakHandler handler) {
        runtime.getLocker().checkThread();
        breakHandlers.remove(handler);
    }

    /**
     * Registers a function breakpoint. When the JavaScript function
     * is invoked, the breakpoint will be 'hit'.
     *
     * @param function The function on which to register the breakpoint.
     */
    public void setBreakpoint(final V8Function function) {
        V8Array parameters = new V8Array(runtime);
        parameters.push(function);
        try {
            debugObject.executeVoidFunction(SET_BREAK_POINT, parameters);
        } finally {
            parameters.release();
        }
    }

    /**
     * Registers a breakpoint given a scriptID and line number. The breakpoint
     * will be 'hit' when the script is executed and the given line is reached.
     *
     * @param scriptID The ID of the script on which to set the breakpoint.
     * @param lineNumber The line number on which to set the breakpoint.
     */
    public void setScriptBreakpoint(final String scriptID, final int lineNumber) {
        V8Array parameters = new V8Array(runtime);
        parameters.push(scriptID);
        parameters.push(lineNumber);
        try {
            debugObject.executeVoidFunction(SET_SCRIPT_BREAK_POINT_BY_NAME, parameters);
        } finally {
            parameters.release();
        }
    }

    @Override
    public void release() {
        debugObject.release();
    }

    private void setupDebugObject(final V8 runtime) {
        V8Object outerDebug = runtime.getObject(DEBUG_OBJECT_NAME);
        try {
            debugObject = outerDebug.getObject(V8_DEBUG_OBJECT);
        } finally {
            outerDebug.release();
        }
    }

    private void setupBreakpointHandler() {
        BreakpointHandler handler = new BreakpointHandler();
        debugObject.registerJavaMethod(handler, DEBUG_BREAK_HANDLER);
        V8Function debugHandler = null;
        V8Array parameters = null;
        try {
            debugHandler = (V8Function) debugObject.getObject(DEBUG_BREAK_HANDLER);
            parameters = new V8Array(runtime).push(debugHandler);
            debugObject.executeFunction(SET_LISTENER, parameters);
        } finally {
            if ((debugHandler != null) && !debugHandler.isReleased()) {
                debugHandler.release();
            }
            if ((parameters != null) && !parameters.isReleased()) {
                parameters.release();
            }
        }
    }

    private class BreakpointHandler implements JavaVoidCallback {

        @Override
        public void invoke(final V8Object receiver, final V8Array parameters) {
            if ((parameters == null) || parameters.isUndefined()) {
                return;
            }
            int event = parameters.getInteger(0);
            if (event != 1) {
                return;
            }
            for (BreakHandler handler : breakHandlers) {
                invokeHandler(parameters, event, handler);
            }
        }

        private void invokeHandler(final V8Array parameters, final int event, final BreakHandler handler) {
            V8Object execState = null;
            V8Object eventData = null;
            V8Object data = null;
            ExecutionState state = null;
            try {
                execState = parameters.getObject(1);
                eventData = parameters.getObject(2);
                data = parameters.getObject(3);
                state = new ExecutionState(execState);
                handler.onBreak(DebugEvent.values()[event], state, eventData, data);
            } finally {
                safeRelease(execState);
                safeRelease(eventData);
                safeRelease(data);
                safeRelease(state);
            }
        }

        private void safeRelease(final Releasable object) {
            if ((object != null)) {
                object.release();
            }
        }

    }

}