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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class V8LockerTest {

    private boolean passed = false;

    @Test
    public void testAcquireOnCreation() {
        V8Locker v8Locker = new V8Locker();

        v8Locker.checkThread();
    }

    @Test
    public void testAcquireLocker() {
        V8Locker v8Locker = new V8Locker();
        v8Locker.release();
        v8Locker.acquire();

        v8Locker.checkThread();
    }

    @Test
    public void testHasLock() {
        V8Locker v8Locker = new V8Locker();

        assertTrue(v8Locker.hasLock());
    }

    @Test
    public void testDoesNotHasLock() {
        V8Locker v8Locker = new V8Locker();
        v8Locker.release();

        assertFalse(v8Locker.hasLock());
    }

    @Test
    public void testThreadLocked() throws InterruptedException {
        final V8Locker v8Locker = new V8Locker();
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    v8Locker.checkThread();
                } catch (Error e) {
                    assertEquals("Invalid V8 thread access", e.getMessage());
                    passed = true;
                }
            }
        });
        t.start();
        t.join();

        assertTrue(passed);
    }

    @Test
    public void testCannotUseReleasedLocker() {
        V8Locker v8Locker = new V8Locker();
        v8Locker.release();

        try {
            v8Locker.checkThread();
        } catch (Error e) {
            assertEquals("Invalid V8 thread access", e.getMessage());
            return;
        }
        fail("Expected exception");
    }

}
