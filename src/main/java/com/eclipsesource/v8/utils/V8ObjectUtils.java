package com.eclipsesource.v8.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class V8ObjectUtils {

    public static Map<String, ? super Object> toMap(final V8Object object) {
        Map<String, ? super Object> result = new HashMap<>();
        String[] keys = object.getKeys();
        for (String key : keys) {
            result.put(key, getValue(object, key));
        }
        return result;
    }

    public static List<? super Object> toList(final V8Array array) {
        List<? super Object> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(getValue(array, "" + i));
        }
        return result;
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
                setValue(v8, result, "" + i, value);
            }
        } catch (IllegalStateException e) {
            result.release();
            throw e;
        }

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setValue(final V8 v8, final V8Object result, final String key, final Object value) {
        if (value == null) {
            result.addUndefined(key);
        } else if (value instanceof Integer) {
            result.add(key, (int) value);
        } else if (value instanceof Double) {
            result.add(key, (double) value);
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

    private static Object getValue(final V8Object object, final String key) {
        int valueType = object.getType(key);
        switch (valueType) {
            case V8Object.INTEGER:
                return object.getInteger(key);
            case V8Object.DOUBLE:
                return object.getDouble(key);
            case V8Object.BOOLEAN:
                return object.getBoolean(key);
            case V8Object.STRING:
                return object.getString(key);
            case V8Object.V8_ARRAY:
                V8Array array = object.getArray(key);
                try {
                    return toList(array);
                } finally {
                    array.release();
                }
            case V8Object.V8_OBJECT:
                V8Object child = object.getObject(key);
                try {
                    return toMap(child);
                } finally {
                    child.release();
                }
            case V8Object.VOID:
                return null;
            default:
                throw new IllegalStateException("Cannot find type for key: " + key);
        }
    }

    private V8ObjectUtils() {

    }
}
