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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

public class V8PropertyMapTest {

    @Test
    public void testPropertyMapContainsSelf() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("foo", map);

        assertNotNull(map.get("foo"));
        assertEquals(map, map.get("foo"));
        assertSame(map, map.get("foo"));
    }

    @Test
    public void testPropertyMapContainsSelfComputeHash() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("foo", map);

        assertEquals(map.hashCode(), map.get("foo").hashCode());
    }

    @Test
    public void testPropertyMapHashIsConstant() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        int hashCode = map.hashCode();

        map.put("foo", map);

        assertEquals(hashCode, map.hashCode());
    }

    @Test
    public void testPropertyMapWithNulls() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("foo", null);

        assertNull(map.get("foo"));
    }

    @Test
    public void testPropertyMapWithMultiNulls() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("foo", null);
        map.put("bar", null);
        map.put("baz", null);

        assertNull(map.get("foo"));
        assertNull(map.get("bar"));
        assertNull(map.get("baz"));
        assertEquals(3, map.size());
    }

    @Test
    public void testPropertyMapSize() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("bar", new Object());
        map.put("foo", null);

        assertEquals(2, map.size());
    }

    @Test
    public void testPropertyMapIsEmpty() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        assertTrue(map.isEmpty());
    }

    @Test
    public void testPropertyMapIsNotEmpty() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("foo", null);

        assertFalse(map.isEmpty());
    }

    @Test
    public void testPropertyMapClear() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("bar", new Object());
        map.put("foo", null);

        map.clear();

        assertEquals(0, map.size());
    }

    @Test
    public void testPropertyMapRemove() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("foo", null);

        assertNull(map.remove("foo"));

        assertEquals(0, map.size());
    }

    @Test
    public void testPropertyMapContainsNullValue() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("foo", null);

        assertTrue(map.containsValue(null));
    }

    @Test
    public void testPropertyMapDoesNotContainNullValue() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("foo", new Object());

        assertFalse(map.containsValue(null));
    }

    @Test
    public void testPropertyMapGetValues() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("foo", map);
        map.put("bar", null);
        map.put("baz", null);

        Collection<Object> values = map.values();

        assertEquals(3, values.size());
        values.contains(map);
        values.contains(null);
    }

    @Test
    public void testPropertyMapGetValuesEmpty() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        Collection<Object> values = map.values();

        assertEquals(0, values.size());
    }

    @Test
    public void testPropertyMapGetKeys() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("foo", map);
        map.put("bar", null);
        map.put("baz", null);

        Set<String> keys = map.keySet();

        assertEquals(3, keys.size());
        keys.contains("foo");
        keys.contains("bar");
        keys.contains("baz");
    }

    @Test
    public void testPropertyMapGetKeysEmpty() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        Set<String> keys = map.keySet();

        assertEquals(0, keys.size());
    }

    @Test
    public void testPropertyMapPutDuplicate() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("foo", map);
        map.put("foo", null);

        assertEquals(1, map.size());
        assertNull(map.get("foo"));
    }

    @Test
    public void testPropertyMapPutDuplicateReverse() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.put("foo", null);
        map.put("foo", map);

        assertEquals(1, map.size());
        assertEquals(map, map.get("foo"));
    }

    @Test
    public void testPropertyMapPutAll() {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("one", 1);
        items.put("two", 2);
        items.put("three", 3);
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.putAll(items);

        assertEquals(3, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
    }

    @Test
    public void testPropertyMapPutAllWithNull() {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("one", 1);
        items.put("two", 2);
        items.put("three", 3);
        items.put("null", null);
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        map.putAll(items);

        assertEquals(4, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
        assertNull(map.get("null"));
    }

    @Test
    public void testPropertyMapPutAllReplacesWithNull() {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("one", 1);
        items.put("two", 2);
        items.put("three", 3);
        items.put("null", null);
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("null", "foo");

        map.putAll(items);

        assertEquals(4, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
        assertNull(map.get("null"));
    }

    @Test
    public void testPropertyMapPutAllReplacesNull() {
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("one", 1);
        items.put("two", 2);
        items.put("three", 3);
        items.put("null", "foo");
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("null", null);

        map.putAll(items);

        assertEquals(4, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
        assertEquals("foo", map.get("null"));
    }

    @Test
    public void testCombinePropertyMap() {
        Map<String, Object> items = new V8PropertyMap<Object>();
        items.put("one", 1);
        items.put("two", 2);
        items.put("three", 3);
        items.put("null", "foo");
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("null", null);

        map.putAll(items);

        assertEquals(4, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
        assertEquals("foo", map.get("null"));
    }

    @Test
    public void testPropertyMapEntrySetWithNulls() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("foo", null);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Set<Entry<String, Object>> entrySet = map.entrySet();

        assertEquals(4, entrySet.size());
        entrySet.contains(null);
        entrySet.contains(1);
        entrySet.contains(2);
        entrySet.contains(3);
    }

    @Test
    public void testPropertyMapEntrySetWithSelf() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();
        map.put("foo", null);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("self", map);

        Set<Entry<String, Object>> entrySet = map.entrySet();

        assertEquals(5, entrySet.size());
        entrySet.contains(null);
        entrySet.contains(1);
        entrySet.contains(2);
        entrySet.contains(3);
        entrySet.contains(map);
    }

    @Test
    public void testPropertyMapEntrySetEmpty() {
        V8PropertyMap<Object> map = new V8PropertyMap<Object>();

        Set<Entry<String, Object>> entrySet = map.entrySet();

        assertEquals(0, entrySet.size());
    }
}
