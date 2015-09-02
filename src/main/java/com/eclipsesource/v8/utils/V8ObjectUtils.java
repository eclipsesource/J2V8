/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Contributors:
 * EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8.utils;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

import java.util.*;
import java.util.Map.Entry;

/**
 * A set of static helper methods to convert V8Objects / V8Arrays to
 * java.util Maps and Lists and back again. These conversions
 * perform a deep copy.
 */
public class V8ObjectUtils {

    private static final Object IGNORE = new Object();

    /**
     * Creates a Map<String, Object> from a V8Object using a deep copy. All elements
     * in the V8Object are released after they are accessed. However, the root
     * object itself is not released.
     *
     * @param object The root of the V8Object graph.
     * @return A map representing a deep copy of the V8Object rooted at 'object'.
     */
    public static Map<String, ? super Object> toMap(final V8Object object) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return toMap(object, cache);
        } finally {
            cache.release();
        }
    }

    /**
     * Creates a List<Object> from a V8Array using a deep copy. All elements
     * in the V8Array are released after they are accessed. However, the root
     * array itself is not released.
     *
     * @param array The root of the V8Array graph.
     * @return A list representing a deep copy of the V8Array rooted at 'array'.
     */
    public static List<? super Object> toList(final V8Array array) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return toList(array, cache);
        } finally {
            cache.release();
        }
    }

    /**
     * Populates a Java array from a V8Array. The type of the array must be specified.
     * Currently, only INTEGER, DOUBLE, BOOLEAN and STRING are supported.
     * The V8Array must only contain elements of type 'arrayType'. The result
     * can be optionally passed in as a parameter.
     * <p>
     * This method will use J2V8's bulk array copy making it faster than iterating over
     * all the elements in the array.
     *
     * @param array     The V8Array to convert to a Java Array.
     * @param arrayType The type of the V8Array to convert.
     * @param result    The array to use as the result. If null, a new array will be created.
     * @return A Java array representing a V8Array.
     */
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

    /**
     * Creates a Java array from a V8Array. The type of the Array must be specified.
     * Currently, only INTEGER, DOUBLE, BOOLEAN and STRING are supported.
     * The V8Array must only contain elements of type 'arrayType'.
     * <p>
     * This method will use J2V8's bulk array copy making it faster than iterating over
     * all the elements in the array.
     *
     * @param array     The V8Array to convert to a Java Array.
     * @param arrayType The type of the V8Array to convert.
     * @return A Java array representing a V8Array.
     */
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

    /**
     * Creates a V8Object from a java.util.Map. This is a deep copy, so if the map
     * contains other maps (or lists) they will also be converted.
     *
     * @param v8  The runtime on which to create the result.
     * @param map The map to convert to a V8Object.
     * @return A V8Object representing the map.
     */
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

    /**
     * Creates a V8Array from a java.util.List. This is a deep copy, so if the list
     * contains other lists (or maps) they will also be converted.
     *
     * @param v8   The runtime on which to create the result.
     * @param list The list to convert to a V8Array.
     * @return A V8Array representing the list.
     */
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

    /**
     * Returns an object usable with a V8 Runtime which represents
     * the parameter 'value'. If 'value' is an Integer, Boolean, Double
     * or String, then 'value' is simply returned as these are directly
     * usable on V8. If 'value' is a map / list, then it's converted to
     * a V8Object / V8Array first.
     * <p>
     * If the result is a V8Value, it must be released.
     *
     * @param v8    The runtime on which to create V8Values.
     * @param value The value to convert to an object usable with V8
     * @return An object which can be used directly with a V8 runtime.
     */
    public static Object getV8Result(final V8 v8, final Object value) {
        if (value == null) {
            return null;
        }
        Map<Object, V8Object> cache = new Hashtable<Object, V8Object>();
        try {
            Object result = getV8Result(v8, value, cache);
            if (result instanceof V8Object) {
                return ((V8Object) result).twin();
            }
            return result;
        } finally {
            for (V8Object v8Object : cache.values()) {
                v8Object.release();
            }
        }
    }

    /**
     * Pushes a Java Object to a V8Array by first converting it to a V8Value if needed.
     * If the value is a boxed primitive, then the primitive will be pushed. If the object
     * is a Map / List then a deep copy will be performed, converting the object to a
     * V8Object / V8Array first.
     *
     * @param v8    The runtime on which to create any needed V8Values.
     * @param array The array to push the elements to.
     * @param value The value to push to the array.
     */
    public static void pushValue(final V8 v8, final V8Array array, final Object value) {
        Map<Object, V8Object> cache = new Hashtable<Object, V8Object>();
        try {
            pushValue(v8, array, value, cache);
        } finally {
            for (V8Object v8Object : cache.values()) {
                v8Object.release();
            }
        }
    }

    /**
     * Gets a Java Object representing the value at the given index in the V8Array.
     * If the value is a primitive (int, boolean or double) then a boxed instance
     * is returned. If the value is a String, then a String is returned. If
     * the value is a V8Object or V8Array, then a Map or List is returned.
     *
     * @param array The array on which to lookup the value. The array is not
     *              released.
     * @param index The index whose element to lookup.
     * @return A Java Object representing the value at a given index.
     */
    public static Object getValue(final V8Array array, final int index) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return getValue(array, index, cache);
        } finally {
            cache.release();
        }
    }

    /**
     * Gets a Java Object representing the value with the given key in the V8Object.
     * If the value is a primitive (int, boolean or double) then a boxed instance
     * is returned. If the value is a String, then a String is returned. If
     * the value is a V8Object or V8Array, then a Map or List is returned.
     *
     * @param object The object on which to lookup the value. The object is not
     *               released.
     * @param key    The key to use to lookup the value.
     * @return A Java Object representing the value at a given key.
     */
    public static Object getValue(final V8Object object, final String key) {
        V8Map<Object> cache = new V8Map<Object>();
        try {
            return getValue(object, key, cache);
        } finally {
            cache.release();
        }
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void pushValue(final V8 v8, final V8Array result, final Object value, final Map<Object, V8Object> cache) {
        if (value == null) {
            result.pushUndefined();
        } else if (value instanceof Integer) {
            result.push((Integer) value);
        } else if (value instanceof Long) {
            result.push(new Double((Long) value));
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

    @SuppressWarnings({"unchecked", "rawtypes"})
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
