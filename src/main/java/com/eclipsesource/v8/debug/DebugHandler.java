/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
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

public class DebugHandler implements Releasable {

    public static String DEBUG_EVENTS[]    = {
            "undefined",
            "BREAK",
            "EXCEPTION",
            "NEW_FUNCTION",
            "BEFORE_COMPILE",
            "AFTER_COMPILE",
            "COMPILE_ERROR",
            "PROMISE_ERROR",
            "ASYNC_TASK_EVENT" };
    public static String DEBUG_OBJECT_NAME = "__j2v8_Debug";

    private static String DEBUG_BREAK_HANDLER = "__j2v8_debug_handler";

    private V8                 runtime;
    private V8Object           debugObject;
    private List<BreakHandler> breakHandlers = null;

    public DebugHandler(final V8 runtime) {
        this.runtime = runtime;
        setupDebugObject(runtime);
        setupBreakpointHandler();
    }

    private void setupDebugObject(final V8 runtime) {
        V8Object outerDebug = runtime.getObject(DEBUG_OBJECT_NAME);
        try {
            debugObject = outerDebug.getObject("Debug");
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
            debugObject.executeFunction("setListener", parameters);
        } finally {
            if ((debugHandler != null) && !debugHandler.isReleased()) {
                debugHandler.release();
            }
            if ((parameters != null) && !parameters.isReleased()) {
                parameters.release();
            }
        }
    }

    public void addBreakHandler(final BreakHandler hanlder) {
        runtime.getLocker().checkThread();
        if (breakHandlers == null) {
            breakHandlers = new ArrayList<BreakHandler>();
        }
        breakHandlers.add(hanlder);

    }

    public void removeBreakHandler(final BreakHandler handler) {
        runtime.getLocker().checkThread();
        breakHandlers.remove(handler);
    }

    public void setScriptBreakpoint(final String scriptID, final int lineNumber) {
        V8Array parameters = new V8Array(runtime);
        parameters.push(scriptID);
        parameters.push(lineNumber);
        try {
            debugObject.executeVoidFunction("setScriptBreakPointByName", parameters);
        } finally {
            parameters.release();
        }
    }

    @Override
    public void release() {
        debugObject.release();
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
            try {
                execState = parameters.getObject(1);
                eventData = parameters.getObject(2);
                data = parameters.getObject(3);
                handler.onBreak(event, execState, eventData, data);
            } finally {
                safeRelease(execState);
                safeRelease(eventData);
                safeRelease(data);
            }
        }

        private void safeRelease(final Releasable object) {
            if ((object != null)) {
                object.release();
            }
        }

    }

}