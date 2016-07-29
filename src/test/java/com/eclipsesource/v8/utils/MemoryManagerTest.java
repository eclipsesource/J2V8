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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

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
                throw new IllegalStateException("V8Runtimes not properly released.");
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
}
