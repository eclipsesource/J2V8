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
package com.eclipsesource.v8.utils.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.V8ObjectUtils;

public class V8ObjectUtilsTest {
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
    public void testCreateIntegerMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:2, c:3}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        object.release();
    }

    @Test
    public void testCreateDoubleMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {a:1.1, b:2.2, c:3.3, d:4.4}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(4, map.size());
        assertEquals(1.1, (Double) map.get("a"), 0.000001);
        assertEquals(2.2, (Double) map.get("b"), 0.000001);
        assertEquals(3.3, (Double) map.get("c"), 0.000001);
        assertEquals(4.4, (Double) map.get("d"), 0.000001);
        object.release();
    }

    @Test
    public void testCreateStringMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {a:'foo', b:'bar', c:'baz', d:'boo'}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(4, map.size());
        assertEquals("foo", map.get("a"));
        assertEquals("bar", map.get("b"));
        assertEquals("baz", map.get("c"));
        assertEquals("boo", map.get("d"));
        object.release();
    }

    @Test
    public void testCreateBooleanMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {a:true, b:1==1, c:false, d:1!=1}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(4, map.size());
        assertTrue((Boolean) map.get("a"));
        assertTrue((Boolean) map.get("b"));
        assertFalse((Boolean) map.get("c"));
        assertFalse((Boolean) map.get("d"));
        object.release();
    }

    @Test
    public void testCreateMixedMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {boolean:true, integer:1, double:3.14159, string:'hello'}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(4, map.size());
        assertTrue((Boolean) map.get("boolean"));
        assertEquals(1, (int) (Integer) map.get("integer"));
        assertEquals(3.14159, (Double) map.get("double"), 0.0000001);
        assertEquals("hello", map.get("string"));
        object.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateNestedMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = { name : { first :'john', last: 'smith'}, age: 7}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(2, map.size());
        assertEquals(7, map.get("age"));
        assertEquals("john", ((Map) map.get("name")).get("first"));
        assertEquals("smith", ((Map) map.get("name")).get("last"));
        object.release();
    }

    @Test
    public void testCreateListFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [1,2,3]; x");

        List<? super Object> list = V8ObjectUtils.toList(array);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        array.release();
    }

    @Test
    public void testCreateListWithUndefinedFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [1,2,3]; x[9] = 10; x");

        List<? super Object> list = V8ObjectUtils.toList(array);

        assertEquals(10, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertTrue(((V8Value) list.get(3)).isUndefined());
        assertTrue(((V8Value) list.get(4)).isUndefined());
        assertTrue(((V8Value) list.get(5)).isUndefined());
        assertTrue(((V8Value) list.get(6)).isUndefined());
        assertTrue(((V8Value) list.get(7)).isUndefined());
        assertTrue(((V8Value) list.get(8)).isUndefined());
        assertEquals(10, list.get(9));
        array.release();
    }

    @Test
    public void testCreateListWithNullFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [null]; x");

        List<? super Object> list = V8ObjectUtils.toList(array);

        assertEquals(1, list.size());
        assertNull(list.get(0));
        array.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateMatrixFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [[1,2,3],[true,false,true],['this','that','other']]; x");

        List<? super Object> list = V8ObjectUtils.toList(array);

        assertEquals(3, list.size());
        assertEquals(3, ((List) list.get(0)).size());
        assertEquals(3, ((List) list.get(1)).size());
        assertEquals(3, ((List) list.get(2)).size());
        assertEquals(1, ((List) list.get(0)).get(0));
        assertEquals(2, ((List) list.get(0)).get(1));
        assertEquals(3, ((List) list.get(0)).get(2));
        assertTrue((Boolean) ((List) list.get(1)).get(0));
        assertFalse((Boolean) ((List) list.get(1)).get(1));
        assertTrue((Boolean) ((List) list.get(1)).get(2));
        assertEquals("this", ((List) list.get(2)).get(0));
        assertEquals("that", ((List) list.get(2)).get(1));
        assertEquals("other", ((List) list.get(2)).get(2));
        array.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateMapWithLists() {
        V8Object object = v8.executeObjectScript("x = {a:[1,2,3], b:[4,5,6]}; x;");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(2, map.size());
        assertEquals(1, ((List) map.get("a")).get(0));
        assertEquals(2, ((List) map.get("a")).get(1));
        assertEquals(3, ((List) map.get("a")).get(2));
        assertEquals(4, ((List) map.get("b")).get(0));
        assertEquals(5, ((List) map.get("b")).get(1));
        assertEquals(6, ((List) map.get("b")).get(2));
        object.release();
    }

    @Test
    public void testCreateMapWithNulls() {
        V8Object object = v8.executeObjectScript("x = {a:null}; x;");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(1, map.size());
        assertNull(map.get(0));
        object.release();
    }

    @Test
    public void testCreateV8ObjectFromStringMap() {
        Map<String, String> map = new HashMap<>();
        map.put("first", "john");
        map.put("last", "smith");

        int size = registerAndRelease("result", map);

        assertEquals(2, size);
        assertEquals("john", v8.executeStringScript("result.first"));
        assertEquals("smith", v8.executeStringScript("result['last']"));
    }

    @Test
    public void testCreateV8ObjectFromIntegerMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 3);

        int size = registerAndRelease("result", map);

        assertEquals(2, size);
        assertEquals(1, v8.executeIntegerScript("result.a"));
        assertEquals(3, v8.executeIntegerScript("result['b']"));
    }

    @Test
    public void testCreateV8ObjectFromLongMap() {
        Map<String, Long> map = new HashMap<>();
        map.put("a", 1L);
        map.put("b", 3L);

        int size = registerAndRelease("result", map);

        assertEquals(2, size);
        assertEquals(1, v8.executeIntegerScript("result.a"));
        assertEquals(3, v8.executeIntegerScript("result['b']"));
    }

    @Test
    public void testCreateV8ObjectFromDoubleMap() {
        Map<String, Double> map = new HashMap<>();
        map.put("a", 1.1);
        map.put("b", 3.14159);
        map.put("c", 4.999);

        int size = registerAndRelease("result", map);

        assertEquals(3, size);
        assertEquals(1.1, v8.executeDoubleScript("result.a"), 0.000001);
        assertEquals(3.14159, v8.executeDoubleScript("result['b']"), 0.000001);
        assertEquals(4.999, v8.executeDoubleScript("result['c']"), 0.000001);
    }

    @Test
    public void testCreateV8ObjectFromFloatMap() {
        Map<String, Float> map = new HashMap<>();
        map.put("a", 1.1f);
        map.put("b", 3.14159f);
        map.put("c", 4.999f);

        int size = registerAndRelease("result", map);

        assertEquals(3, size);
        assertEquals(1.1, v8.executeDoubleScript("result.a"), 0.000001);
        assertEquals(3.14159, v8.executeDoubleScript("result['b']"), 0.000001);
        assertEquals(4.999, v8.executeDoubleScript("result['c']"), 0.000001);
    }

    @Test
    public void testCreateV8ObjectFromBooleanMap() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("a", true);
        map.put("b", true);
        map.put("c", false);

        int size = registerAndRelease("result", map);

        assertEquals(3, size);
        assertTrue(v8.executeBooleanScript("result.a"));
        assertTrue(v8.executeBooleanScript("result['b']"));
        assertFalse(v8.executeBooleanScript("result['c']"));
    }

    @Test
    public void testCreatev8ObjectWithNulls() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("a", true);
        map.put("b", null);

        int size = registerAndRelease("result", map);

        assertTrue(v8.executeBooleanScript("result.a"));
        assertTrue(v8.executeBooleanScript("typeof result.b === 'undefined'"));
        assertEquals(2, size);
    }

    @Test
    public void testCreateV8ObjectFromMixedMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("boolean", true);
        map.put("integer", 7);
        map.put("double", 3.14159);
        map.put("String", "hello");
        map.put("undefined", null);

        int size = registerAndRelease("result", map);

        assertEquals(5, size);
        V8Object object = v8.getObject("result");
        assertTrue(object.getBoolean("boolean"));
        assertEquals(7, object.getInteger("integer"));
        assertEquals(3.14159, object.getDouble("double"), 0.000001);
        assertEquals("hello", object.getString("String"));
        assertEquals(V8Value.UNDEFINED, object.getType("undefined"));
        object.release();
    }

    @Test
    public void testCreateV8ArrayFromIntegerList() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(1);
        for (int i = 2; i < 10; i++) {
            list.add(list.get(i - 1) + list.get(i - 2));
        }

        int size = registerAndRelease("result", list);

        assertEquals(10, size);
        assertEquals(1, v8.executeIntegerScript("result[0]"));
        assertEquals(1, v8.executeIntegerScript("result[1]"));
        assertEquals(2, v8.executeIntegerScript("result[2]"));
        assertEquals(3, v8.executeIntegerScript("result[3]"));
        assertEquals(5, v8.executeIntegerScript("result[4]"));
        assertEquals(8, v8.executeIntegerScript("result[5]"));
        assertEquals(13, v8.executeIntegerScript("result[6]"));
        assertEquals(21, v8.executeIntegerScript("result[7]"));
        assertEquals(34, v8.executeIntegerScript("result[8]"));
        assertEquals(55, v8.executeIntegerScript("result[9]"));
    }

    @Test
    public void testCreateV8ArrayFromLongList() {
        List<Long> list = new ArrayList<>();
        list.add((long) 1);

        int size = registerAndRelease("result", list);

        assertEquals(1, size);
        assertEquals(1, v8.executeIntegerScript("result[0]"));
    }

    @Test
    public void testCreateV8ArrayFromFloatList() {
        List<Float> list = new ArrayList<>();
        list.add(1.1f);

        int size = registerAndRelease("result", list);

        assertEquals(1, size);
        assertEquals(1.1, v8.executeDoubleScript("result[0]"), 0.0000001);
    }

    @Test
    public void testCreateV8ArrayFromDoubleList() {
        List<Double> list = new ArrayList<>();
        list.add(3.14159);
        list.add(4.1);
        list.add(5.3);

        int size = registerAndRelease("result", list);

        assertEquals(3, size);
        assertEquals(3.14159, v8.executeDoubleScript("result[0]"), 0.000001);
        assertEquals(4.1, v8.executeDoubleScript("result[1]"), 0.000001);
        assertEquals(5.3, v8.executeDoubleScript("result[2]"), 0.000001);
    }

    @Test
    public void testCreateV8ArrayFromBooleanList() {
        List<Boolean> list = new ArrayList<>();
        list.add(true);
        list.add(false);

        int size = registerAndRelease("result", list);

        assertEquals(2, size);
        assertTrue(v8.executeBooleanScript("result[0]"));
        assertFalse(v8.executeBooleanScript("result[1]"));
    }

    @Test
    public void testCreateV8ArrayFromStringList() {
        List<String> list = new ArrayList<>();
        list.add("hello");
        list.add("world");

        int size = registerAndRelease("result", list);

        assertEquals(2, size);
        assertEquals("hello", v8.executeStringScript("result[0]"));
        assertEquals("world", v8.executeStringScript("result[1]"));
    }

    @Test
    public void testCreateV8ArrayWithNullValues() {
        List<String> list = new ArrayList<>();
        list.add("hello");
        list.add(null);
        list.add("world");

        int size = registerAndRelease("result", list);

        assertEquals(3, size);
        assertEquals("hello", v8.executeStringScript("result[0]"));
        assertTrue(v8.executeBooleanScript("typeof result[1] === 'undefined'"));
        assertEquals("world", v8.executeStringScript("result[2]"));
    }

    @Test
    public void testCreateV8AraryFromMixedList() {
        List<Object> list = new ArrayList<>();
        list.add("string");
        list.add(7);
        list.add(3.14159);
        list.add(true);
        list.add(null);

        int size = registerAndRelease("result", list);

        assertEquals(5, size);
        assertEquals("string", v8.executeStringScript("result[0]"));
        assertEquals(7, v8.executeIntegerScript("result[1]"));
        assertEquals(3.14159, v8.executeDoubleScript("result[2]"), 0.000001);
        assertTrue(v8.executeBooleanScript("result[3]"));
        assertTrue(v8.executeBooleanScript("typeof result[4] === 'undefined'"));
    }

    @Test
    public void testCreateV8ArrayOfMaps() {
        List<Map<String, Integer>> list = new ArrayList<>();
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("Sadie", 7);
        m1.put("Lily", 5);
        m1.put("Maggie", 3);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("Ian", 38);
        list.add(m1);
        list.add(m2);

        int size = registerAndRelease("result", list);

        assertEquals(2, size);
        assertEquals(7, v8.executeIntegerScript("result[0].Sadie"));
        assertEquals(5, v8.executeIntegerScript("result[0].Lily"));
        assertEquals(3, v8.executeIntegerScript("result[0].Maggie"));
        assertEquals(38, v8.executeIntegerScript("result[1].Ian"));
    }

    @Test
    public void testCreatev8ArrayFromMatrix() {
        List<List<Integer>> matrix = new ArrayList<>();
        List<Integer> l1 = new ArrayList<>();
        l1.add(1);
        l1.add(2);
        l1.add(3);
        List<Integer> l2 = new ArrayList<>();
        l2.add(4);
        l2.add(5);
        l2.add(6);
        List<Integer> l3 = new ArrayList<>();
        l3.add(7);
        l3.add(8);
        l3.add(9);
        matrix.add(l1);
        matrix.add(l2);
        matrix.add(l3);

        int size = registerAndRelease("result", matrix);
        assertEquals(3, size);
        assertEquals(3, v8.executeIntegerScript("result[0].length"));
        assertEquals(3, v8.executeIntegerScript("result[1].length"));
        assertEquals(3, v8.executeIntegerScript("result[2].length"));
        assertEquals(1, v8.executeIntegerScript("result[0][0]"));
        assertEquals(2, v8.executeIntegerScript("result[0][1]"));
        assertEquals(3, v8.executeIntegerScript("result[0][2]"));
        assertEquals(4, v8.executeIntegerScript("result[1][0]"));
        assertEquals(5, v8.executeIntegerScript("result[1][1]"));
        assertEquals(6, v8.executeIntegerScript("result[1][2]"));
        assertEquals(7, v8.executeIntegerScript("result[2][0]"));
        assertEquals(8, v8.executeIntegerScript("result[2][1]"));
        assertEquals(9, v8.executeIntegerScript("result[2][2]"));
    }

    @Test
    public void testCreateV8ObjectFromMapOfLists() {
        Map<String, List<String>> map = new HashMap<>();
        List<String> l1 = new ArrayList<>();
        l1.add("first");
        l1.add("second");
        l1.add("third");
        List<String> l2 = new ArrayList<>();
        l2.add("a");
        l2.add("b");
        l2.add("c");
        List<String> l3 = new ArrayList<>();
        l3.add("dog");
        l3.add("cat");
        map.put("numbers", l1);
        map.put("letters", l2);
        map.put("animals", l3);

        int size = registerAndRelease("result", map);

        assertEquals(3, size);
        assertEquals("first", v8.executeStringScript("result.numbers[0]"));
        assertEquals("second", v8.executeStringScript("result.numbers[1]"));
        assertEquals("third", v8.executeStringScript("result.numbers[2]"));
        assertEquals("a", v8.executeStringScript("result.letters[0]"));
        assertEquals("b", v8.executeStringScript("result.letters[1]"));
        assertEquals("c", v8.executeStringScript("result.letters[2]"));
        assertEquals("dog", v8.executeStringScript("result.animals[0]"));
        assertEquals("cat", v8.executeStringScript("result.animals[1]"));
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateV8ObjectWithInvalidContents() {
        Map<String, Object> map = new HashMap<>();
        map.put("first", new Rectangle());

        registerAndRelease("result", map);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateV8ArrayWithInvalidContents() {
        List<Object> list = new ArrayList<>();
        list.add(new Rectangle());

        registerAndRelease("result", list);
    }

    @Test
    public void testNullObjectGivesEmptyMap() {
        Map<String, ? super Object> map = V8ObjectUtils.toMap(null);

        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void testNullArrayGivesEmptyMap() {
        List<? super Object> list = V8ObjectUtils.toList(null);

        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void testGetIntValueFromArray() {
        V8Array array = v8.executeArrayScript("[1,2,3,4,5]");

        assertEquals(1, V8ObjectUtils.getValue(array, 0));
        array.release();
    }

    @Test
    public void testGetDoubleValueFromArray() {
        V8Array array = v8.executeArrayScript("[1.2,2.2,3.3,4.4,5.5]");

        assertEquals(4.4, V8ObjectUtils.getValue(array, 3));
        array.release();
    }

    @Test
    public void testGetStringValueFromArray() {
        V8Array array = v8.executeArrayScript("['string']");

        assertEquals("string", V8ObjectUtils.getValue(array, 0));
        array.release();
    }

    @Test
    public void testGetBooleanValueFromArray() {
        V8Array array = v8.executeArrayScript("[true, false]");

        assertEquals(Boolean.TRUE, V8ObjectUtils.getValue(array, 0));
        array.release();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetMapValueFromArray() {
        V8Array array = v8.executeArrayScript("[{a:'b', c:'d'}]");

        assertTrue(V8ObjectUtils.getValue(array, 0) instanceof Map<?, ?>);
        assertEquals("b", ((Map<String, Object>) V8ObjectUtils.getValue(array, 0)).get("a"));
        array.release();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetArrayValueFromArray() {
        V8Array array = v8.executeArrayScript("[[1,2,3]]");

        assertTrue(V8ObjectUtils.getValue(array, 0) instanceof List<?>);
        assertEquals(1, ((List<Object>) V8ObjectUtils.getValue(array, 0)).get(0));
        array.release();
    }

    @Test
    public void testGetV8ResultInteger() {
        Object result = V8ObjectUtils.getV8Result(v8, new Integer(77));

        assertEquals(77, result);
    }

    @Test
    public void testGetV8ResultDouble() {
        Object result = V8ObjectUtils.getV8Result(v8, new Double(77.7));

        assertEquals(77.7, result);
    }

    @Test
    public void testGetV8ResultFloat() {
        Object result = V8ObjectUtils.getV8Result(v8, new Float(77.7));

        assertEquals(77.7f, result);
    }

    @Test
    public void testGetV8ResultString() {
        Object result = V8ObjectUtils.getV8Result(v8, "Seven");

        assertEquals("Seven", result);
    }

    @Test
    public void testGetV8ResultTrue() {
        Object result = V8ObjectUtils.getV8Result(v8, Boolean.TRUE);

        assertEquals(true, result);
    }

    @Test
    public void testGetV8ResultObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        Object result = V8ObjectUtils.getV8Result(v8, map);

        assertTrue(result instanceof V8Object);
        assertEquals("bar", ((V8Object) result).getString("foo"));
        ((V8Object) result).release();
    }

    @Test
    public void testGetV8ResultArray() {
        List<Object> list = new ArrayList<>();
        list.add(1);
        list.add("two");
        Object result = V8ObjectUtils.getV8Result(v8, list);

        assertTrue(result instanceof V8Array);
        assertEquals(2, ((V8Array) result).length());
        assertEquals(1, ((V8Array) result).getInteger(0));
        assertEquals("two", ((V8Array) result).getString(1));
        ((V8Object) result).release();
    }

    @Test
    public void testPushInteger() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, 7);

        assertEquals(7, array.getInteger(0));
        array.release();
    }

    @Test
    public void testPushDouble() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, 7.8);

        assertEquals(7.8, array.getDouble(0), 0.000001);
        array.release();
    }

    @Test
    public void testPushBoolean() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, true);

        assertTrue(array.getBoolean(0));
        array.release();
    }

    @Test
    public void testPushString() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, "string");

        assertEquals("string", array.getString(0));
        array.release();
    }

    @Test
    public void testPushMap() {
        V8Array array = new V8Array(v8);
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");

        V8ObjectUtils.pushValue(v8, array, map);

        V8Object result = array.getObject(0);
        assertEquals("bar", result.getString("foo"));
        result.release();
        array.release();
    }

    @Test
    public void testPushList() {
        V8Array array = new V8Array(v8);
        List<String> list = new ArrayList<>();
        list.add("one");

        V8ObjectUtils.pushValue(v8, array, list);

        V8Array result = array.getArray(0);
        assertEquals("one", result.getString(0));
        result.release();
        array.release();
    }

    @Test
    public void testPopulateFromIntegerArray() {
        V8Array array = v8.executeArrayScript("[1,2,3,4]");

        int[] result = (int[]) V8ObjectUtils.getTypedArray(array, V8Value.INTEGER);

        assertEquals(4, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromDoubleArray() {
        V8Array array = v8.executeArrayScript("[1,2,3,4.4]");

        double[] result = (double[]) V8ObjectUtils.getTypedArray(array, V8Value.DOUBLE);

        assertEquals(4, result.length);
        assertEquals(1.0, result[0], 0.000001);
        assertEquals(2.0, result[1], 0.000001);
        assertEquals(3.0, result[2], 0.000001);
        assertEquals(4.4, result[3], 0.000001);
        array.release();
    }

    @Test
    public void testPopulateFromBooleanArray() {
        V8Array array = v8.executeArrayScript("[true, false, false, true]");

        boolean[] result = (boolean[]) V8ObjectUtils.getTypedArray(array, V8Value.BOOLEAN);

        assertEquals(4, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertFalse(result[2]);
        assertTrue(result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromStringArray() {
        V8Array array = v8.executeArrayScript("['one', 'two', 'three', 'four']");

        String[] result = (String[]) V8ObjectUtils.getTypedArray(array, V8Value.STRING);

        assertEquals(4, result.length);
        assertEquals("one", result[0]);
        assertEquals("two", result[1]);
        assertEquals("three", result[2]);
        assertEquals("four", result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromExistingIntArray() {
        V8Array array = v8.executeArrayScript("[1,2,3,4,5]");
        int[] result = new int[1000];

        V8ObjectUtils.getTypedArray(array, V8Value.INTEGER, result);

        assertEquals(1000, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        assertEquals(5, result[4]);
        array.release();
    }

    @Test
    public void testPopulateFromNonExistingIntArray() {
        V8Array array = v8.executeArrayScript("[1,2,3,4,5]");

        int[] result = (int[]) V8ObjectUtils.getTypedArray(array, V8Value.INTEGER, null);

        assertEquals(5, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        assertEquals(5, result[4]);
        array.release();
    }

    @Test
    public void testPopulateFromSmallExistingIntArray() {
        V8Array array = v8.executeArrayScript("[1,2,3,4,5]");
        int[] result = new int[4];

        result = (int[]) V8ObjectUtils.getTypedArray(array, V8Value.INTEGER, null);

        assertEquals(5, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        assertEquals(5, result[4]);
        array.release();
    }

    @Test
    public void testPopulateFromExistingDoubleArray() {
        V8Array array = v8.executeArrayScript("[1.1,2.2,3,4,5]");
        double[] result = new double[1000];

        V8ObjectUtils.getTypedArray(array, V8Value.DOUBLE, result);

        assertEquals(1000, result.length);
        assertEquals(1.1, result[0], 0.000001);
        assertEquals(2.2, result[1], 0.000001);
        assertEquals(3, result[2], 0.000001);
        assertEquals(4, result[3], 0.000001);
        assertEquals(5, result[4], 0.000001);
        array.release();
    }

    @Test
    public void testPopulateFromNonExistingDoubleArray() {
        V8Array array = v8.executeArrayScript("[1.1,2.2,3,4,5]");

        double[] result = (double[]) V8ObjectUtils.getTypedArray(array, V8Value.DOUBLE, null);

        assertEquals(5, result.length);
        assertEquals(1.1, result[0], 0.000001);
        assertEquals(2.2, result[1], 0.000001);
        assertEquals(3, result[2], 0.000001);
        assertEquals(4, result[3], 0.000001);
        assertEquals(5, result[4], 0.000001);
        array.release();
    }

    @Test
    public void testPopulateFromSmallExistingDoubleArray() {
        V8Array array = v8.executeArrayScript("[1.1,2.2,3,4,5.5]");
        double[] result = new double[4];

        result = (double[]) V8ObjectUtils.getTypedArray(array, V8Value.DOUBLE, null);

        assertEquals(5, result.length);
        assertEquals(1.1, result[0], 0.000001);
        assertEquals(2.2, result[1], 0.000001);
        assertEquals(3, result[2], 0.000001);
        assertEquals(4, result[3], 0.000001);
        assertEquals(5.5, result[4], 0.000001);
        array.release();
    }

    @Test
    public void testPopulateFromExistingBooleanArray() {
        V8Array array = v8.executeArrayScript("[true, true, false, false]");
        boolean[] result = new boolean[1000];

        V8ObjectUtils.getTypedArray(array, V8Value.BOOLEAN, result);

        assertEquals(1000, result.length);
        assertTrue(result[0]);
        assertTrue(result[1]);
        assertFalse(result[2]);
        assertFalse(result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromNonExistingBooleanArray() {
        V8Array array = v8.executeArrayScript("[true, true, false, false]");

        boolean[] result = (boolean[]) V8ObjectUtils.getTypedArray(array, V8Value.BOOLEAN, null);

        assertEquals(4, result.length);
        assertTrue(result[0]);
        assertTrue(result[1]);
        assertFalse(result[2]);
        assertFalse(result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromSmallExistingBooleanArray() {
        V8Array array = v8.executeArrayScript("[true, true, false, false]");
        boolean[] result = new boolean[4];

        result = (boolean[]) V8ObjectUtils.getTypedArray(array, V8Value.BOOLEAN, null);

        assertEquals(4, result.length);
        assertTrue(result[0]);
        assertTrue(result[1]);
        assertFalse(result[2]);
        assertFalse(result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromExistingStringArray() {
        V8Array array = v8.executeArrayScript("['a', 'b', 'c', 'z']");
        String[] result = new String[1000];

        V8ObjectUtils.getTypedArray(array, V8Value.STRING, result);

        assertEquals(1000, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("z", result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromNonExistingStringArray() {
        V8Array array = v8.executeArrayScript("['a', 'b', 'c', 'z']");

        String[] result = (String[]) V8ObjectUtils.getTypedArray(array, V8Value.STRING, null);

        assertEquals(4, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("z", result[3]);
        array.release();
    }

    @Test
    public void testPopulateFromSmallExistingStringArray() {
        V8Array array = v8.executeArrayScript("['a', 'b', 'c', 'z']");
        String[] result = new String[4];

        result = (String[]) V8ObjectUtils.getTypedArray(array, V8Value.STRING, null);

        assertEquals(4, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("z", result[3]);
        array.release();
    }

    private int registerAndRelease(final String name, final List<? extends Object> list) {
        V8Array array = V8ObjectUtils.toV8Array(v8, list);
        v8.add(name, array);
        int size = array.length();
        array.release();
        return size;
    }

    private int registerAndRelease(final String name, final Map<String, ? extends Object> map) {
        V8Object object = V8ObjectUtils.toV8Object(v8, map);
        v8.add(name, object);
        int size = getNumberOfProperties(object);
        object.release();
        return size;
    }

    private int getNumberOfProperties(final V8Object object) {
        return object.getKeys().length;
    }

}
