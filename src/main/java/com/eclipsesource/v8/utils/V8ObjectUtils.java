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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

public class V8ObjectUtils {

    public static Map<String, ? super Object> toMap(final V8Object object) {
        if (object == null) {
            return Collections.emptyMap();
        }
        Map<String, ? super Object> result = new HashMap<>();
        String[] keys = object.getKeys();
        for (String key : keys) {
            result.put(key, getValue(object, key));
        }
        return result;
    }

    public static List<? super Object> toList(final V8Array array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<? super Object> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(getValue(array, i));
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
            array.getInts(0, length, intArray);
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
            return array.getInts(0, length);
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
        V8Object result = new V8Object(v8);
        try {
            for (Entry<String, ? extends Object> entry : map.entrySet()) {
                setValue(v8, result, entry.getKey(), entry.getValue());
            }
        } catch (IllegalStateException e) {
            result.release();
            throw e;
        }
        return result;
    }

    public static V8Array toV8Array(final V8 v8, final List<? extends Object> list) {
        V8Array result = new V8Array(v8);
        try {
            for (int i = 0; i < list.size(); i++) {
                Object value = list.get(i);
                pushValue(v8, result, value);
            }
        } catch (IllegalStateException e) {
            result.release();
            throw e;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static Object getV8Result(final V8 v8, final Object value) {
        if (value instanceof Map<?, ?>) {
            return toV8Object(v8, (Map<String, ? extends Object>) value);
        } else if (value instanceof List<?>) {
            return toV8Array(v8, (List<? extends Object>) value);
        }
        return value;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void pushValue(final V8 v8, final V8Array result, final Object value) {
        if (value == null) {
            result.pushUndefined();
        } else if (value instanceof Integer) {
            result.push((int) value);
        } else if (value instanceof Long) {
            result.push((int) (long) value);
        } else if (value instanceof Double) {
            result.push((double) value);
        } else if (value instanceof Float) {
            result.push((float) value);
        } else if (value instanceof String) {
            result.push((String) value);
        } else if (value instanceof Boolean) {
            result.push((boolean) value);
        } else if (value instanceof Map) {
            V8Object object = toV8Object(v8, (Map) value);
            result.push(object);
            object.release();
        } else if (value instanceof List) {
            V8Array array = toV8Array(v8, (List) value);
            result.push(array);
            array.release();
        } else {
            throw new IllegalStateException("Unsupported Object of type: " + value.getClass());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setValue(final V8 v8, final V8Object result, final String key, final Object value) {
        if (value == null) {
            result.addUndefined(key);
        } else if (value instanceof Integer) {
            result.add(key, (int) value);
        } else if (value instanceof Long) {
            result.add(key, (int) (long) value);
        } else if (value instanceof Double) {
            result.add(key, (double) value);
        } else if (value instanceof Float) {
            result.add(key, (float) value);
        } else if (value instanceof String) {
            result.add(key, (String) value);
        } else if (value instanceof Boolean) {
            result.add(key, (boolean) value);
        } else if (value instanceof Map) {
            V8Object object = toV8Object(v8, (Map) value);
            result.add(key, object);
            object.release();
        } else if (value instanceof List) {
            V8Array array = toV8Array(v8, (List) value);
            result.add(key, array);
            array.release();
        } else {
            throw new IllegalStateException("Unsupported Object of type: " + value.getClass());
        }
    }

    public static Object getValue(final V8Array array, final int index) {
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
            case V8Value.V8_ARRAY:
                V8Array arrayValue = array.getArray(index);
                try {
                    return toList(arrayValue);
                } finally {
                    if (arrayValue instanceof V8Array) {
                        arrayValue.release();
                    }
                }
            case V8Value.V8_OBJECT:
                V8Object objectValue = array.getObject(index);
                try {
                    return toMap(objectValue);
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
            case V8Value.V8_ARRAY:
                V8Array array = object.getArray(key);
                try {
                    return toList(array);
                } finally {
                    if (array instanceof V8Array) {
                        array.release();
                    }
                }
            case V8Value.V8_OBJECT:
                V8Object child = object.getObject(key);
                try {
                    return toMap(child);
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
}
