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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class V8LockTest {

    @Test
    public void testGetReadLock() {
        V8Lock v8Lock = new V8Lock();

        v8Lock.lockRead();

        assertEquals(1, v8Lock.getReaderCount());
    }

    @Test
    public void testFreeReadLock() {
        V8Lock v8Lock = new V8Lock();

        v8Lock.lockRead();
        v8Lock.unlockRead();

        assertEquals(0, v8Lock.getReaderCount());
    }

    @Test
    public void testGetWriteLock() {
        V8Lock v8Lock = new V8Lock();

        v8Lock.lockWrite();

        assertEquals(1, v8Lock.getWriterCount());
    }

    @Test
    public void testFreeWriteLock() {
        V8Lock v8Lock = new V8Lock();

        v8Lock.lockWrite();
        v8Lock.unlockWrite();

        assertEquals(0, v8Lock.getWriterCount());
    }

}
