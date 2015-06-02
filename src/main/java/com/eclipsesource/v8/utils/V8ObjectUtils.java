/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

public class V8ObjectUtils {

    private static final Object IGNORE = new Object();

    public static Map<String, ? super Object> toMap(final V8Object object) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return toMap(object, cache);
        } finally {
            cache.release();
        }
    }

    private static Map<String, ? super Object> toMap(final V8Object object, final V8Map<Object> cache) {
        if (object == null) {
            return Collections.emptyMap();
        }
        if (cache.containsKey(object)) {
            return (Map<String, ? super Object>) cache.get(object);
        }
        Map<String, ? super Object> result = new V8PropertyMap<Object>();
        cache.put(object, result);
        String[] keys = object.getKeys();
        for (String key : keys) {
            Object value = getValue(object, key, cache);
            if (value != IGNORE) {
                result.put(key, value);
            }
        }
        return result;
    }

    public static List<? super Object> toList(final V8Array array) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return toList(array, cache);
        } finally {
            cache.release();
        }
    }

    private static List<? super Object> toList(final V8Array array, final V8Map<Object> cache) {
        if (array == null) {
            return Collections.emptyList();
        }
        if (cache.containsKey(array)) {
            return (List<? super Object>) cache.get(array);
        }
        List<? super Object> result = new ArrayList<Object>();
        cache.put(array, result);
        for (int i = 0; i < array.length(); i++) {
            Object value = getValue(array, i, cache);
            if (value != IGNORE) {
                result.add(getValue(array, i, cache));
            }
        }
        return result;
    }

    public static Object getTypedArray(final V8Array array, final int arrayType, final Object result) {
        int length = array.length();
        if (arrayType == V8Value.INTEGER) {
            int[] intArray = (int[]) result;
            if ((intArray == null) || (intArray.length < length)) {
                intArray = new int[length];
            }
            array.getIntegers(0, length, intArray);
            return intArray;
        } else if (arrayType == V8Value.DOUBLE) {
            double[] doubleArray = (double[]) result;
            if ((doubleArray == null) || (doubleArray.length < length)) {
                doubleArray = new double[length];
            }
            array.getDoubles(0, length, doubleArray);
            return doubleArray;
        } else if (arrayType == V8Value.BOOLEAN) {
            boolean[] booleanArray = (boolean[]) result;
            if ((booleanArray == null) || (booleanArray.length < length)) {
                booleanArray = new boolean[length];
            }
            array.getBooleans(0, length, booleanArray);
            return booleanArray;
        } else if (arrayType == V8Value.STRING) {
            String[] stringArray = (String[]) result;
            if ((stringArray == null) || (stringArray.length < length)) {
                stringArray = new String[length];
            }
            array.getStrings(0, length, stringArray);
            return stringArray;
        }
        throw new RuntimeException("Unsupported bulk load type: " + arrayType);

    }

    public static Object getTypedArray(final V8Array array, final int arrayType) {
        int length = array.length();
        if (arrayType == V8Value.INTEGER) {
            return array.getIntegers(0, length);
        } else if (arrayType == V8Value.DOUBLE) {
            return array.getDoubles(0, length);
        } else if (arrayType == V8Value.BOOLEAN) {
            return array.getBooleans(0, length);
        } else if (arrayType == V8Value.STRING) {
            return array.getStrings(0, length);
        }
        throw new RuntimeException("Unsupported bulk load type: " + arrayType);
    }

    public static V8Object toV8Object(final V8 v8, final Map<String, ? extends Object> map) {
        Map<Object, V8Object> cache = new Hashtable<Object, V8Object>();
        try {
            return toV8Object(v8, map, cache).twin();
        } finally {
            for (V8Object v8Object : cache.values()) {
                v8Object.release();
            }
        }
    }

    private static V8Object toV8Object(final V8 v8, final Map<String, ? extends Object> map, final Map<Object, V8Object> cache) {
        if (cache.containsKey(map)) {
            return cache.get(map);
        }
        V8Object result = new V8Object(v8);
        cache.put(map, result);
        try {
            for (Entry<String, ? extends Object> entry : map.entrySet()) {
                setValue(v8, result, entry.getKey(), entry.getValue(), cache);
            }
        } catch (IllegalStateException e) {
            result.release();
            throw e;
        }
        return result;
    }

    public static V8Array toV8Array(final V8 v8, final List<? extends Object> list) {
        Map<Object, V8Object> cache = new Hashtable<Object, V8Object>();
        try {
            return toV8Array(v8, list, cache).twin();
        } finally {
            for (V8Object v8Object : cache.values()) {
                v8Object.release();
            }
        }
    }

    private static V8Array toV8Array(final V8 v8, final List<? extends Object> list, final Map<Object, V8Object> cache) {
        if (cache.containsKey(new ListWrapper(list))) {
            return (V8Array) cache.get(new ListWrapper(list));
        }
        V8Array result = new V8Array(v8);
        cache.put(new ListWrapper(list), result);
        try {
            for (int i = 0; i < list.size(); i++) {
                Object value = list.get(i);
                pushValue(v8, result, value, cache);
            }
        } catch (IllegalStateException e) {
            result.release();
            throw e;
        }

        return result;
    }

    public static Object getV8Result(final V8 v8, final Object value) {
        if (value == null) {
            return null;
        }
        Map<Object, V8Object> cache = new Hashtable<Object, V8Object>();
        try {
            Object result = getV8Result(v8, value, cache);
            if ( result instanceof V8Object ) {
                return ((V8Object) result).twin();
            }
            return result;
        } finally {
            for (V8Object v8Object : cache.values()) {
                v8Object.release();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object getV8Result(final V8 v8, final Object value, final Map<Object, V8Object> cache) {
        if (cache.containsKey(value)) {
            return cache.get(value);
        }
        if (value instanceof Map<?, ?>) {
            return toV8Object(v8, (Map<String, ? extends Object>) value, cache);
        } else if (value instanceof List<?>) {
            return toV8Array(v8, (List<? extends Object>) value, cache);
        }
        return value;
    }

    public static void pushValue(final V8 v8, final V8Array result, final Object value) {
        Map<Object, V8Object> cache = new Hashtable<Object, V8Object>();
        try {
            pushValue(v8, result, value, cache);
        } finally {
            for (V8Object v8Object : cache.values()) {
                v8Object.release();
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void pushValue(final V8 v8, final V8Array result, final Object value, final Map<Object, V8Object> cache) {
        if (value == null) {
            result.pushUndefined();
        } else if (value instanceof Integer) {
            result.push((Integer) value);
        } else if (value instanceof Long) {
            result.push((int) (long) (Long) value);
        } else if (value instanceof Double) {
            result.push((Double) value);
        } else if (value instanceof Float) {
            result.push((Float) value);
        } else if (value instanceof String) {
            result.push((String) value);
        } else if (value instanceof Boolean) {
            result.push((Boolean) value);
        } else if (value instanceof Map) {
            V8Object object = toV8Object(v8, (Map) value, cache);
            result.push(object);
        } else if (value instanceof List) {
            V8Array array = toV8Array(v8, (List) value, cache);
            result.push(array);
        } else {
            throw new IllegalStateException("Unsupported Object of type: " + value.getClass());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setValue(final V8 v8, final V8Object result, final String key, final Object value, final Map<Object, V8Object> cache) {
        if (value == null) {
            result.addUndefined(key);
        } else if (value instanceof Integer) {
            result.add(key, (Integer) value);
        } else if (value instanceof Long) {
            result.add(key, (int) (long) (Long) value);
        } else if (value instanceof Double) {
            result.add(key, (Double) value);
        } else if (value instanceof Float) {
            result.add(key, (Float) value);
        } else if (value instanceof String) {
            result.add(key, (String) value);
        } else if (value instanceof Boolean) {
            result.add(key, (Boolean) value);
        } else if (value instanceof Map) {
            V8Object object = toV8Object(v8, (Map) value, cache);
            result.add(key, object);
        } else if (value instanceof List) {
            V8Array array = toV8Array(v8, (List) value, cache);
            result.add(key, array);
        } else {
            throw new IllegalStateException("Unsupported Object of type: " + value.getClass());
        }
    }

    public static Object getValue(final V8Array array, final int index) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return getValue(array, index, cache);
        } finally {
            cache.release();
        }
    }

    private static Object getValue(final V8Array array, final int index, final V8Map<Object> cache) {
        int valueType = array.getType(index);
        switch (valueType) {
            case V8Value.INTEGER:
                return array.getInteger(index);
            case V8Value.DOUBLE:
                return array.getDouble(index);
            case V8Value.BOOLEAN:
                return array.getBoolean(index);
            case V8Value.STRING:
                return array.getString(index);
            case V8Value.V8_FUNCTION:
                return IGNORE;
            case V8Value.V8_ARRAY:
                V8Array arrayValue = array.getArray(index);
                try {
                    return toList(arrayValue, cache);
                } finally {
                    if (arrayValue instanceof V8Array) {
                        arrayValue.release();
                    }
                }
            case V8Value.V8_OBJECT:
                V8Object objectValue = array.getObject(index);
                try {
                    return toMap(objectValue, cache);
                } finally {
                    if (objectValue instanceof V8Object) {
                        objectValue.release();
                    }
                }
            case V8Value.NULL:
                return null;
            case V8Value.UNDEFINED:
                return V8.getUndefined();
            default:
                throw new IllegalStateException("Cannot find type for index: " + index);
        }
    }

    public static Object getValue(final V8Object object, final String key) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return getValue(object, key, cache);
        } finally {
            cache.release();
        }
    }

    private static Object getValue(final V8Object object, final String key, final V8Map<Object> cache) {
        int valueType = object.getType(key);
        switch (valueType) {
            case V8Value.INTEGER:
                return object.getInteger(key);
            case V8Value.DOUBLE:
                return object.getDouble(key);
            case V8Value.BOOLEAN:
                return object.getBoolean(key);
            case V8Value.STRING:
                return object.getString(key);
            case V8Value.V8_FUNCTION:
                return IGNORE;
            case V8Value.V8_ARRAY:
                V8Array array = object.getArray(key);
                try {
                    return toList(array, cache);
                } finally {
                    if (array instanceof V8Array) {
                        array.release();
                    }
                }
            case V8Value.V8_OBJECT:
                V8Object child = object.getObject(key);
                try {
                    return toMap(child, cache);
                } finally {
                    if (child instanceof V8Object) {
                        child.release();
                    }
                }
            case V8Value.NULL:
                return null;
            case V8Value.UNDEFINED:
                return V8.getUndefined();
            default:
                throw new IllegalStateException("Cannot find type for key: " + key);
        }
    }

    private V8ObjectUtils() {

    }

    static class ListWrapper {
        private List<? extends Object> list;

        public ListWrapper(final List<? extends Object> list) {
            this.list = list;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ListWrapper) {
                return ((ListWrapper) obj).list == list;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(list);
        }
    }
}
