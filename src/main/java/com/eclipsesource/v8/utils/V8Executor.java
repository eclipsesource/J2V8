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
package com.eclipsesource.v8.utils;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class V8Executor extends Thread {

    private final String     script;
    private V8               runtime;
    private String           result;
    private volatile boolean terminated         = false;
    private volatile boolean requestTermination = false;
    private Exception        exception          = null;

    public V8Executor(final String script) {
        this.script = script;
    }

    protected void setup(final V8 runtime) {

    }

    public String getResult() {
        return result;
    }

    @Override
    public void run() {
        synchronized (this) {
            runtime = V8.createV8Runtime();
            runtime.registerJavaMethod(new ExecutorTermination(), "__j2v8__checkThreadTerminate");
            setup(runtime);
        }
        try {
            if (!requestTermination) {
                result = runtime.executeScript("__j2v8__checkThreadTerminate();\n" + script, getName(), -1).toString();
            }
        } catch (Exception e) {
            exception = e;
        } finally {
            synchronized (this) {
                if (runtime.getLocker().hasLock()) {
                    runtime.release();
                    runtime = null;
                }
                terminated = true;
            }
        }
    }

    public boolean hasException() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }

    public boolean hasTerminated() {
        return terminated;
    }

    public void terminateExecution() {
        synchronized (this) {
            requestTermination = true;
            if (runtime != null) {
                runtime.terminateExecution();
            }
        }
    }

    class ExecutorTermination implements JavaVoidCallback {
        @Override
        public void invoke(final V8Object receiver, final V8Array parameters) {
            if (requestTermination) {
                throw new RuntimeException("V8Thread Termination.");
            }
        }
    }
}