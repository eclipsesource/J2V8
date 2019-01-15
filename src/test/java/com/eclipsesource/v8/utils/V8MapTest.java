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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

public class V8MapTest {
    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        if (v8 != null) {
            v8.close();
        }
        if (V8.getActiveRuntimes() != 0) {
            throw new IllegalStateException("V8Runtimes not properly released");
        }
    }

    @Test
    public void testCreateMap() {
        new V8Map<String>().close();
    }

    @Test
    public void testReleaseEmptyMap() {
        V8Map<String> map = new V8Map<String>();

        map.close();
    }

    @Test
    public void testSizeEmpty() {
        V8Map<String> map = new V8Map<String>();

        assertEquals(0, map.size());
        map.close();
    }

    @Test
    public void testIsEmpty() {
        V8Map<String> map = new V8Map<String>();

        assertTrue(map.isEmpty());
        map.close();
    }

    @Test
    public void testPutUndefined() {
        V8Map<Object> map = new V8Map<Object>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, V8.getUndefined());

        assertEquals(V8.getUndefined(), map.get(v1));
        v1.close();
        map.close();
    }

    @Test
    public void testIsNotEmpty() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");
        v1.close();

        assertFalse(map.isEmpty());
        map.close();
    }

    @Test
    public void testSize() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        V8Object v2 = new V8Object(v8);
        V8Object v3 = new V8Object(v8);
        map.put(v1, "foo");
        map.put(v2, "bar");
        map.put(v3, "baz");

        assertEquals(3, map.size());
        v1.close();
        v2.close();
        v3.close();
        map.close();
    }

    @Test
    public void testClear() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        V8Object v2 = new V8Object(v8);
        V8Object v3 = new V8Object(v8);
        map.put(v1, "foo");
        map.put(v2, "bar");
        map.put(v3, "baz");

        map.clear();
        assertEquals(0, map.size());
        v1.close();
        v2.close();
        v3.close();
        map.close();
    }

    @Test
    public void testAddDuplicateKey() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");
        map.put(v1, "bar");

        assertEquals("bar", map.get(v1));
        v1.close();
        map.close();
    }

    @Test
    public void testGet() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");
        V8Object v8Object = v8.getObject("x");
        map.put(v8Object, "foo");

        assertEquals("foo", map.get(v8Object));

        v8Object.close();
        map.close();
    }

    @Test
    public void testGetMissing() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");
        V8Object v8Object = v8.getObject("x");

        assertNull(map.get(v8Object));

        v8Object.close();
        map.close();
    }

    @Test
    public void testContainsKey() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");
        V8Object v8Object = v8.getObject("x");
        map.put(v8Object, "foo");

        assertTrue(map.containsKey(v8Object));

        v8Object.close();
        map.close();
    }

    @Test
    public void testDoesNotContainKey() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");
        V8Object v8Object = v8.getObject("x");

        assertFalse(map.containsKey(v8Object));

        v8Object.close();
        map.close();
    }

    @Test
    public void testContainsValue() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");
        V8Object v8Object = v8.getObject("x");
        map.put(v8Object, "foo");

        assertTrue(map.containsValue("foo"));

        v8Object.close();
        map.close();
    }

    @Test
    public void testDoesNotContainValue() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");

        assertFalse(map.containsValue("foo"));

        map.close();
    }

    @Test
    public void testRemove() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");
        V8Object v8Object = v8.getObject("x");
        map.put(v8Object, "foo");

        assertEquals("foo", map.remove(v8Object));

        v8Object.close();
        map.close();
    }

    @Test
    public void testRemoveMissing() {
        V8Map<String> map = new V8Map<String>();
        v8.executeVoidScript("var x = {}");
        V8Object v8Object = v8.getObject("x");

        assertNull(map.remove(v8Object));

        v8Object.close();
        map.close();
    }

    @Test
    public void testReleaseMapReleasesKeys() {
        V8Object v8Object = new V8Object(v8);
        V8Map<String> map = new V8Map<String>();
        map.put(v8Object, "foo");
        v8Object.close();

        map.close();
    }

    @Test
    public void testRemoveKeyReleasesKey() {
        V8Map<String> map = new V8Map<String>();
        v8.executeScript("var x = {}");
        V8Object v8Object = v8.getObject("x");
        map.put(v8Object, "foo");

        map.remove(v8Object);

        v8Object.close();
        map.close();
    }

    @Test
    public void testAddItemStoresACopy() {
        V8 v8 = V8.createV8Runtime();
        V8Object v8Object = new V8Object(v8);
        V8Map<String> map = new V8Map<String>();
        map.put(v8Object, "foo");
        v8Object.close();

        try {
            v8.release(true);
            map.close();
        } catch (IllegalStateException e) {
            return;
        }
        fail("Exception expected due to handle leak.");
    }

    @Test
    public void testKeyset() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");

        Set<V8Value> keySet = map.keySet();

        assertEquals(1, keySet.size());
        assertEquals(v1, keySet.iterator().next());
        v1.close();
        map.close();
    }

    @Test
    public void testEntrySet() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");

        Set<Entry<V8Value, String>> entrySet = map.entrySet();

        assertEquals(1, entrySet.size());
        assertEquals(v1, entrySet.iterator().next().getKey());
        assertEquals("foo", entrySet.iterator().next().getValue());
        v1.close();
        map.close();
    }

    @Test
    public void testValues() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");

        Collection<String> values = map.values();

        assertEquals(1, values.size());
        assertEquals("foo", values.iterator().next());
        v1.close();
        map.close();
    }

    @Test
    public void testKeysetNotReleased() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");
        v1.close();

        Set<V8Value> keySet = map.keySet();

        assertEquals(1, keySet.size());
        assertFalse(keySet.iterator().next().isReleased());
        map.close();
    }

    @Test
    public void testAddAll() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");
        v1.close();
        V8Map<String> newMap = new V8Map<String>();
        V8Object v2 = new V8Object(v8);
        newMap.put(v2, "bar");
        v2.close();

        newMap.putAll(map);

        map.close();
        assertEquals(2, newMap.size());
        newMap.close();
    }

    @Test
    public void testAddAllWithDuplicates() {
        V8Map<String> map = new V8Map<String>();
        V8Object v1 = new V8Object(v8);
        map.put(v1, "foo");
        V8Map<String> newMap = new V8Map<String>();
        newMap.put(v1, "bar");
        v1.close();

        newMap.putAll(map);

        map.close();
        assertEquals(1, newMap.size());
        assertEquals("foo", newMap.values().iterator().next());
        newMap.close();
    }

}
