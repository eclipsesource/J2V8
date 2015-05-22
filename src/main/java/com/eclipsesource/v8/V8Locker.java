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
package com.eclipsesource.v8;

public class V8Locker {

    private Thread thread = null;

    public V8Locker() {
        acquire();
    }

    public synchronized void acquire() {
        if ((thread != null) && (thread != Thread.currentThread())) {
            throw new Error("Invalid V8 thread access.");
        }
        thread = Thread.currentThread();
    }

    public synchronized void release() {
        checkThread();
        thread = null;
    }

    public void checkThread() {
        if ((thread != Thread.currentThread())) {
            throw new Error("Invalid V8 thread access.");
        }
    }

    public boolean hasLock() {
        return thread == Thread.currentThread();
    }

}
