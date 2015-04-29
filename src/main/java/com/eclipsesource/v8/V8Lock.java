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

final class V8Lock {

    private int readers       = 0;
    private int writers       = 0;
    private int writeRequests = 0;

    public synchronized void lockRead() {
        while ((writers > 0) || (writeRequests > 0)) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        readers++;
    }

    public synchronized void unlockRead() {
        readers--;
        notifyAll();
    }

    public synchronized void lockWrite() {
        writeRequests++;

        while ((readers > 0) || (writers > 0)) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        writeRequests--;
        writers++;
    }

    public synchronized void unlockWrite() {
        writers--;
        notifyAll();
    }

    protected int getReaderCount() {
        return readers;
    }

    protected int getWriterCount() {
        return writers;
    }

    protected int getWriterRequestCount() {
        return writeRequests;
    }
}
