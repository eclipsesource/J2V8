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
package com.eclipsesource.v8.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ConcurrentModificationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.ReferenceHandler;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

public class MemoryManagerTest {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testMemoryManagerReleasesObjects() {
        MemoryManager memoryManager = new MemoryManager(v8);

        new V8Object(v8);
        memoryManager.release();

        assertEquals(0, v8.getObjectReferenceCount());
    }

    @Test
    public void testObjectIsReleased() {
        MemoryManager memoryManager = new MemoryManager(v8);

        V8Object object = new V8Object(v8);
        memoryManager.release();

        assertTrue(object.isReleased());
    }

    @Test
    public void testMemoryManagerReleasesFunctions() {
        MemoryManager memoryManager = new MemoryManager(v8);

        v8.executeScript("(function() {})");
        memoryManager.release();

        assertEquals(0, v8.getObjectReferenceCount());
    }

    @Test
    public void testMemoryReferenceCount0() {
        MemoryManager memoryManager = new MemoryManager(v8);

        assertEquals(0, memoryManager.getObjectReferenceCount());
    }

    @Test
    public void testMemoryReferenceCount0_AfterRemove() {
        MemoryManager memoryManager = new MemoryManager(v8);

        new V8Object(v8).release();

        assertEquals(0, memoryManager.getObjectReferenceCount());
    }

    @Test
    public void testMemoryReferenceCount() {
        MemoryManager memoryManager = new MemoryManager(v8);

        v8.executeScript("(function() {})");
        assertEquals(1, memoryManager.getObjectReferenceCount());
        memoryManager.release();

        assertEquals(0, v8.getObjectReferenceCount());
    }

    @Test
    public void testMemoryManagerReleasesReturnedObjects() {
        MemoryManager memoryManager = new MemoryManager(v8);

        v8.executeScript("foo = {}; foo");

        assertEquals(1, v8.getObjectReferenceCount());
        memoryManager.release();
        assertEquals(0, v8.getObjectReferenceCount());
    }

    @Test
    public void testReleasedMemoryManagerDoesTrackObjects() {
        MemoryManager memoryManager = new MemoryManager(v8);

        memoryManager.release();
        V8Object object = new V8Object(v8);

        assertEquals(1, v8.getObjectReferenceCount());
        object.release();
    }

    @Test
    public void testNestedMemoryManagers() {
        MemoryManager memoryManager1 = new MemoryManager(v8);
        MemoryManager memoryManager2 = new MemoryManager(v8);

        new V8Object(v8);
        memoryManager2.release();
        new V8Object(v8);

        assertEquals(1, v8.getObjectReferenceCount());
        memoryManager1.release();
        assertEquals(0, v8.getObjectReferenceCount());
    }

    @Test
    public void testNestedMemoryManagerHasProperObjectCount() {
        MemoryManager memoryManager1 = new MemoryManager(v8);

        new V8Object(v8);
        MemoryManager memoryManager2 = new MemoryManager(v8);
        new V8Object(v8);

        assertEquals(2, memoryManager1.getObjectReferenceCount());
        assertEquals(1, memoryManager2.getObjectReferenceCount());
        memoryManager2.release();

        assertEquals(1, memoryManager1.getObjectReferenceCount());
        memoryManager1.release();
    }

    @Test
    public void testNestedMemoryManager_ReverseReleaseOrder() {
        MemoryManager memoryManager1 = new MemoryManager(v8);

        new V8Object(v8);
        MemoryManager memoryManager2 = new MemoryManager(v8);
        new V8Object(v8);

        assertEquals(2, memoryManager1.getObjectReferenceCount());
        assertEquals(1, memoryManager2.getObjectReferenceCount());
        memoryManager1.release();

        assertEquals(0, memoryManager2.getObjectReferenceCount());
        memoryManager2.release();
    }

    @Test(expected = IllegalStateException.class)
    public void testMemoryManagerReleased_CannotCallGetObjectReferenceCount() {
        MemoryManager memoryManager = new MemoryManager(v8);

        memoryManager.release();

        memoryManager.getObjectReferenceCount();
    }

    @Test
    public void testCanReleaseTwice() {
        MemoryManager memoryManager = new MemoryManager(v8);

        memoryManager.release();
        memoryManager.release();
    }

    @Test
    public void testIsReleasedTrue() {
        MemoryManager memoryManager = new MemoryManager(v8);

        memoryManager.release();

        assertTrue(memoryManager.isReleased());
    }

    @Test
    public void testIsReleasedFalse() {
        MemoryManager memoryManager = new MemoryManager(v8);

        assertFalse(memoryManager.isReleased());
    }

    @Test
    public void testPersistObject() {
        MemoryManager memoryManager = new MemoryManager(v8);

        V8Object object = new V8Object(v8);
        memoryManager.persist(object);
        memoryManager.release();

        assertFalse(object.isReleased());
        object.release();
    }

    @Test
    public void testPersistNonManagedObject() {
        V8Object object = new V8Object(v8);
        MemoryManager memoryManager = new MemoryManager(v8);

        memoryManager.persist(object);
        memoryManager.release();

        assertFalse(object.isReleased());
        object.release();
    }

    @Test
    public void testTwins() {
        MemoryManager memoryManager = new MemoryManager(v8);

        V8Object object = new V8Object(v8);
        object.twin();

        assertEquals(2, memoryManager.getObjectReferenceCount());
        memoryManager.release();
    }

    @Test
    public void testTwinsReleaseOne() {
        MemoryManager memoryManager = new MemoryManager(v8);

        V8Object object = new V8Object(v8);
        object.twin();
        object.release();

        assertEquals(1, memoryManager.getObjectReferenceCount());
        memoryManager.release();
    }

    @Test
    public void testGetObjectTwice() {
        v8.executeVoidScript("foo = {}");
        MemoryManager memoryManager = new MemoryManager(v8);

        V8Object foo1 = v8.getObject("foo");
        v8.getObject("foo").release();

        assertEquals(1, memoryManager.getObjectReferenceCount());
        memoryManager.release();
        assertTrue(foo1.isReleased());
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCallPersistOnReleasedManager() {
        MemoryManager memoryManager = new MemoryManager(v8);

        V8Object object = new V8Object(v8);
        memoryManager.release();
        memoryManager.persist(object);
    }

    MemoryManager memoryManager;

    @Test
    public void testExceptionDuringReleaseDoesNotReleaseMemoryManager() {
        memoryManager = new MemoryManager(v8);
        ReferenceHandler handler = new ReferenceHandler() {

            @Override
            public void v8HandleDisposed(final V8Value object) {
                // Throws CME
                memoryManager.persist(object);
            }

            @Override
            public void v8HandleCreated(final V8Value object) {
            }
        };
        v8.addReferenceHandler(handler);

        new V8Object(v8);
        try {
            memoryManager.release();
        } catch (ConcurrentModificationException e) {

        }

        assertFalse(memoryManager.isReleased());

        v8.removeReferenceHandler(handler);
        memoryManager.release();
        assertTrue(memoryManager.isReleased());
    }

}
