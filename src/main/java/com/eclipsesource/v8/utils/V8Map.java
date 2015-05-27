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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8Value;

public class V8Map<V> implements Map<V8Value, V>, Releasable {

    private Map<V8Value, V>       map;
    private Map<V8Value, V8Value> twinMap;

    public V8Map() {
        map = new HashMap<V8Value, V>();
        twinMap = new HashMap<V8Value, V8Value>();
    }

    @Override
    public void release() {
        this.clear();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(final Object key) {
        return map.get(key);
    }

    @Override
    public V put(final V8Value key, final V value) {
        this.remove(key);
        V8Value twin = key.twin();
        twinMap.put(twin, twin);
        return map.put(twin, value);
    }

    @Override
    public V remove(final Object key) {
        V result = map.remove(key);
        V8Value twin = twinMap.remove(key);
        if (twin != null) {
            twin.release();
        }
        return result;
    }

    @Override
    public void putAll(final Map<? extends V8Value, ? extends V> m) {
        for (java.util.Map.Entry<? extends V8Value, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
        for (V8Value V8Value : twinMap.keySet()) {
            V8Value.release();
        }
        twinMap.clear();
    }

    @Override
    public Set<V8Value> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<java.util.Map.Entry<V8Value, V>> entrySet() {
        return map.entrySet();
    }

}
