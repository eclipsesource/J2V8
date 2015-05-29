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

import java.util.LinkedList;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class V8Executor extends Thread {

    private final String         script;
    private V8                   runtime;
    private String               result;
    private volatile boolean     terminated       = false;
    private volatile boolean     shuttingDown     = false;
    private volatile boolean     forceTerminating = false;
    private Exception            exception        = null;
    private LinkedList<String[]> messageQueue     = new LinkedList<String[]>();
    private boolean              longRunning;
    private String               messageHandler;

    public V8Executor(final String script, final boolean longRunning, final String messageHandler) {
        this.script = script;
        this.longRunning = longRunning;
        this.messageHandler = messageHandler;
    }

    public V8Executor(final String script) {
        this(script, false, null);
    }

    protected void setup(final V8 runtime) {

    }

    public String getResult() {
        return result;
    }

    public void postMessage(final String... message) {
        synchronized (this) {
            messageQueue.add(message);
            notify();
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            runtime = V8.createV8Runtime();
            runtime.registerJavaMethod(new ExecutorTermination(), "__j2v8__checkThreadTerminate");
            setup(runtime);
        }
        try {
            if (!forceTerminating) {
                Object scriptResult = runtime.executeScript("__j2v8__checkThreadTerminate();\n" + script, getName(), -1);
                if (scriptResult != null) {
                    result = scriptResult.toString();
                }
                if (scriptResult instanceof Releasable) {
                    ((Releasable) scriptResult).release();
                }
                if (scriptResult instanceof Releasable) {
                    ((Releasable) scriptResult).release();
                }
            }
            while (!forceTerminating && longRunning) {
                synchronized (this) {
                    if (messageQueue.isEmpty() && !shuttingDown) {
                        wait();
                    }
                    if ((messageQueue.isEmpty() && shuttingDown) || forceTerminating) {
                        return;
                    }
                }
                if (!messageQueue.isEmpty()) {
                    String[] message = messageQueue.remove(0);
                    V8Array parameters = new V8Array(runtime);
                    V8Array strings = new V8Array(runtime);
                    try {
                        for (String string : message) {
                            strings.push(string);
                        }
                        parameters.push(strings);
                        runtime.executeVoidFunction(messageHandler, parameters);
                    } finally {
                        strings.release();
                        parameters.release();
                    }
                }
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

    public void forceTermination() {
        synchronized (this) {
            forceTerminating = true;
            shuttingDown = true;
            if (runtime != null) {
                runtime.terminateExecution();
            }
            notify();
        }
    }

    public void shutdown() {
        synchronized (this) {
            shuttingDown = true;
            notify();
        }
    }

    class ExecutorTermination implements JavaVoidCallback {
        @Override
        public void invoke(final V8Object receiver, final V8Array parameters) {
            if (forceTerminating) {
                throw new RuntimeException("V8Thread Termination.");
            }
        }
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public boolean isTerminating() {
        return forceTerminating;
    }
}