package com.eclipsesource.v8.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class V8ObjectUtils {

    public static Map<String, ? super Object> asMap(final V8Object object) {
        Map<String, ? super Object> result = new HashMap<>();
        String[] keys = object.getKeys();
        for (String key : keys) {
            result.put(key, getValue(object, key));
        }
        return result;
    }

    public static List<? super Object> asList(final V8Array array) {
        List<? super Object> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(getValue(array, "" + i));
        }
        return result;
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
                    return asList(array);
                } finally {
                    array.release();
                }
            case V8Object.V8_OBJECT:
                V8Object child = object.getObject(key);
                try {
                    return asMap(child);
                } finally {
                    child.release();
                }
            case V8Object.VOID:
                return null;
            default:
                throw new IllegalArgumentException("Cannot find type for key: " + key);
        }
    }

    private V8ObjectUtils() {

    }
}
