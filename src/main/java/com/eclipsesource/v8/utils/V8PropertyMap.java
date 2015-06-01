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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * A custom map is needed because the existing HashMaps
 * do not self containment, and Hashtables do not
 * allow nulls as values.
 */
class V8PropertyMap<V> implements Map<String, V> {

    private Hashtable<String, V> map   = new Hashtable<String, V>();
    private Set<String>          nulls = new HashSet<String>();

    @Override
    public int size() {
        return map.size() + nulls.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty() && nulls.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key) || nulls.contains(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        if ((value == null) && !nulls.isEmpty()) {
            return true;
        } else if (value == null) {
            return false;
        }
        return map.containsValue(value);
    }

    @Override
    public V get(final Object key) {
        if (nulls.contains(key)) {
            return null;
        }
        return map.get(key);
    }

    @Override
    public V put(final String key, final V value) {
        if (value == null) {
            if (map.containsKey(key)) {
                map.remove(key);
            }
            nulls.add(key);
            return null;
        }
        if (nulls.contains(key)) {
            nulls.remove(key);
        }
        return map.put(key, value);
    }

    @Override
    public V remove(final Object key) {
        if (nulls.contains(key)) {
            nulls.remove(key);
            return null;
        }
        return map.remove(key);
    }

    @Override
    public void putAll(final Map<? extends String, ? extends V> m) {
        for (Entry<? extends String, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
        nulls.clear();
    }

    @Override
    public Set<String> keySet() {
        HashSet<String> result = new HashSet<String>(map.keySet());
        result.addAll(nulls);
        return result;
    }

    @Override
    public Collection<V> values() {
        ArrayList<V> result = new ArrayList<V>(map.values());
        for (int i = 0; i < nulls.size(); i++) {
            result.add(null);
        }
        return result;
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        HashSet<Entry<String, V>> result = new HashSet<Map.Entry<String, V>>(map.entrySet());
        for (String nullKey : nulls) {
            result.add(new SimpleEntry<String, V>(nullKey, null));
        }
        return result;
    }

}
