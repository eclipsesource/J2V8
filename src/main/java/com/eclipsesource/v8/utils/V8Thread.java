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

import com.eclipsesource.v8.V8;

public class V8Thread extends Thread {

    private final V8Runnable target;
    private V8               runtime;

    public V8Thread(final V8Runnable target) {
        this.target = target;
    }

    @Override
    public void run() {
        runtime = V8.createV8Runtime();
        try {
            target.run(runtime);
        } finally {
            synchronized (this) {
                if (runtime.getLocker().hasLock()) {
                    runtime.release();
                    runtime = null;
                }
            }
        }
    }

    public void terminateExecution() {
        synchronized (this) {
            if (runtime != null) {
                runtime.terminateExecution();
            }
        }
    }
}