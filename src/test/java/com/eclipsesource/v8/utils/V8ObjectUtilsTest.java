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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8ArrayBuffer;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8TypedArray;
import com.eclipsesource.v8.V8Value;

public class V8ObjectUtilsTest {
    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            if (v8 != null) {
                v8.close();
            }
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            throw e;
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
        object.close();
    }

    @Test
    public void testCreateMapWithFunction() {
        V8Object object = v8.executeObjectScript("x = {a : function() {return 1;}, b : 'foo'}; x;");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(1, map.size());
        assertEquals("foo", map.get("b"));
        object.close();
    }

    @Test
    public void testCreateListWithFunction() {
        V8Array array = v8.executeArrayScript("x = [function() {return 1;}, 'foo']; x;");

        List<Object> list = V8ObjectUtils.toList(array);

        assertEquals(1, list.size());
        assertEquals("foo", list.get(0));
        array.close();
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
        object.close();
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
        object.close();
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
        object.close();
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
        object.close();
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
        object.close();
    }

    @Test
    public void testCreateListFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [1,2,3]; x");

        List<? super Object> list = V8ObjectUtils.toList(array);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        array.close();
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
        array.close();
    }

    @Test
    public void testCreateListWithNullFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [null]; x");

        List<? super Object> list = V8ObjectUtils.toList(array);

        assertEquals(1, list.size());
        assertNull(list.get(0));
        array.close();
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
        array.close();
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
        object.close();
    }

    @Test
    public void testCreateMapWithNulls() {
        V8Object object = v8.executeObjectScript("x = {a:null}; x;");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object);

        assertEquals(1, map.size());
        assertNull(map.get("a"));
        object.close();
    }

    @Test
    public void testCreateV8ObjectFromStringMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("first", "john");
        map.put("last", "smith");

        int size = registerAndRelease("result", map);

        assertEquals(2, size);
        assertEquals("john", v8.executeStringScript("result.first"));
        assertEquals("smith", v8.executeStringScript("result['last']"));
    }

    @Test
    public void testCreateV8ObjectFromIntegerMap() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 1);
        map.put("b", 3);

        int size = registerAndRelease("result", map);

        assertEquals(2, size);
        assertEquals(1, v8.executeIntegerScript("result.a"));
        assertEquals(3, v8.executeIntegerScript("result['b']"));
    }

    @Test
    public void testCreateV8ObjectFromLongMap() {
        Map<String, Long> map = new HashMap<String, Long>();
        map.put("a", 1L);
        map.put("b", 3L);

        int size = registerAndRelease("result", map);

        assertEquals(2, size);
        assertEquals(1, v8.executeIntegerScript("result.a"));
        assertEquals(3, v8.executeIntegerScript("result['b']"));
    }

    @Test
    public void testCreateV8ObjectFromLongTimestampMap() {
        Map<String, Long> map = new HashMap<String, Long>();
        map.put("a", 1477486236000L);

        int size = registerAndRelease("result", map);

        assertEquals(1, size);
        assertEquals(1477486236000L, (long) v8.executeDoubleScript("result.a"));
    }

    @Test
    public void testCreateV8ObjectFromMaxLongMap() {
        Map<String, Long> map = new HashMap<String, Long>();
        map.put("a", Long.MAX_VALUE);
        map.put("b", Long.MIN_VALUE);

        int size = registerAndRelease("result", map);

        assertEquals(2, size);
        assertEquals(Long.MAX_VALUE, (long) v8.executeDoubleScript("result.a"));
        assertEquals(Long.MIN_VALUE, (long) v8.executeDoubleScript("result.b"));
    }

    @Test
    public void testCreateV8ObjectFromDoubleMap() {
        Map<String, Double> map = new HashMap<String, Double>();
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
        Map<String, Float> map = new HashMap<String, Float>();
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
        Map<String, Boolean> map = new HashMap<String, Boolean>();
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
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        map.put("a", true);
        map.put("b", null);

        int size = registerAndRelease("result", map);

        assertTrue(v8.executeBooleanScript("result.a"));
        assertTrue(v8.executeBooleanScript("typeof result.b === 'undefined'"));
        assertEquals(2, size);
    }

    @Test
    public void testCreateV8ObjectFromMixedMap() {
        Map<String, Object> map = new HashMap<String, Object>();
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
        object.close();
    }

    @Test
    public void testCreateV8ArrayFromIntegerList() {
        List<Integer> list = new ArrayList<Integer>();
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
        List<Long> list = new ArrayList<Long>();
        list.add((long) 1);

        int size = registerAndRelease("result", list);

        assertEquals(1, size);
        assertEquals(1, v8.executeIntegerScript("result[0]"));
    }

    @Test
    public void testCreateV8ArrayFromMaxValueLong() {
        List<Long> list = new ArrayList<Long>();
        list.add(Long.MAX_VALUE);

        int size = registerAndRelease("result", list);

        assertEquals(1, size);
        assertEquals(Long.MAX_VALUE, v8.executeDoubleScript("result[0]"), 0.0000001);
    }

    @Test
    public void testCreateV8ArrayFromFloatList() {
        List<Float> list = new ArrayList<Float>();
        list.add(1.1f);

        int size = registerAndRelease("result", list);

        assertEquals(1, size);
        assertEquals(1.1, v8.executeDoubleScript("result[0]"), 0.0000001);
    }

    @Test
    public void testCreateV8ArrayFromDoubleList() {
        List<Double> list = new ArrayList<Double>();
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
        List<Boolean> list = new ArrayList<Boolean>();
        list.add(true);
        list.add(false);

        int size = registerAndRelease("result", list);

        assertEquals(2, size);
        assertTrue(v8.executeBooleanScript("result[0]"));
        assertFalse(v8.executeBooleanScript("result[1]"));
    }

    @Test
    public void testCreateV8ArrayFromStringList() {
        List<String> list = new ArrayList<String>();
        list.add("hello");
        list.add("world");

        int size = registerAndRelease("result", list);

        assertEquals(2, size);
        assertEquals("hello", v8.executeStringScript("result[0]"));
        assertEquals("world", v8.executeStringScript("result[1]"));
    }

    @Test
    public void testCreateV8ArrayWithNullValues() {
        List<String> list = new ArrayList<String>();
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
        List<Object> list = new ArrayList<Object>();
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
        List<Map<String, Integer>> list = new ArrayList<Map<String, Integer>>();
        Map<String, Integer> m1 = new HashMap<String, Integer>();
        m1.put("Sadie", 7);
        m1.put("Lily", 5);
        m1.put("Maggie", 3);
        Map<String, Integer> m2 = new HashMap<String, Integer>();
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
        List<List<Integer>> matrix = new ArrayList<List<Integer>>();
        List<Integer> l1 = new ArrayList<Integer>();
        l1.add(1);
        l1.add(2);
        l1.add(3);
        List<Integer> l2 = new ArrayList<Integer>();
        l2.add(4);
        l2.add(5);
        l2.add(6);
        List<Integer> l3 = new ArrayList<Integer>();
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
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        List<String> l1 = new ArrayList<String>();
        l1.add("first");
        l1.add("second");
        l1.add("third");
        List<String> l2 = new ArrayList<String>();
        l2.add("a");
        l2.add("b");
        l2.add("c");
        List<String> l3 = new ArrayList<String>();
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
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", new Date());

        registerAndRelease("result", map);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateV8ArrayWithInvalidContents() {
        List<Object> list = new ArrayList<Object>();
        list.add(new Date());

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
        array.close();
    }

    @Test
    public void testGetDoubleValueFromArray() {
        V8Array array = v8.executeArrayScript("[1.2,2.2,3.3,4.4,5.5]");

        assertEquals(4.4, V8ObjectUtils.getValue(array, 3));
        array.close();
    }

    @Test
    public void testGetStringValueFromArray() {
        V8Array array = v8.executeArrayScript("['string']");

        assertEquals("string", V8ObjectUtils.getValue(array, 0));
        array.close();
    }

    @Test
    public void testGetBooleanValueFromArray() {
        V8Array array = v8.executeArrayScript("[true, false]");

        assertEquals(Boolean.TRUE, V8ObjectUtils.getValue(array, 0));
        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetMapValueFromArray() {
        V8Array array = v8.executeArrayScript("[{a:'b', c:'d'}]");

        assertTrue(V8ObjectUtils.getValue(array, 0) instanceof Map<?, ?>);
        assertEquals("b", ((Map<String, Object>) V8ObjectUtils.getValue(array, 0)).get("a"));
        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetArrayValueFromArray() {
        V8Array array = v8.executeArrayScript("[[1,2,3]]");

        assertTrue(V8ObjectUtils.getValue(array, 0) instanceof List<?>);
        assertEquals(1, ((List<Object>) V8ObjectUtils.getValue(array, 0)).get(0));
        array.close();
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
    public void testGetV8ResultNull() {
        Object result = V8ObjectUtils.getV8Result(v8, null);

        assertNull(result);
    }

    @Test
    public void testGetV8ResultUndefined() {
        Object result = V8ObjectUtils.getV8Result(v8, V8.getUndefined());

        assertEquals(V8.getUndefined(), result);
    }

    @Test
    public void testGetV8ResultTrue() {
        Object result = V8ObjectUtils.getV8Result(v8, Boolean.TRUE);

        assertEquals(true, result);
    }

    @Test
    public void testGetV8ResultObject() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        Object result = V8ObjectUtils.getV8Result(v8, map);

        assertTrue(result instanceof V8Object);
        assertEquals("bar", ((V8Object) result).getString("foo"));
        ((V8Object) result).close();
    }

    @Test
    public void testGetV8ResultArray() {
        List<Object> list = new ArrayList<Object>();
        list.add(1);
        list.add("two");
        Object result = V8ObjectUtils.getV8Result(v8, list);

        assertTrue(result instanceof V8Array);
        assertEquals(2, ((V8Array) result).length());
        assertEquals(1, ((V8Array) result).getInteger(0));
        assertEquals("two", ((V8Array) result).getString(1));
        ((V8Object) result).close();
    }

    @Test
    public void testPushInteger() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, 7);

        assertEquals(7, array.getInteger(0));
        array.close();
    }

    @Test
    public void testPushDouble() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, 7.8);

        assertEquals(7.8, array.getDouble(0), 0.000001);
        array.close();
    }

    @Test
    public void testPushBoolean() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, true);

        assertTrue(array.getBoolean(0));
        array.close();
    }

    @Test
    public void testPushString() {
        V8Array array = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, "string");

        assertEquals("string", array.getString(0));
        array.close();
    }

    @Test
    public void testPushMap() {
        V8Array array = new V8Array(v8);
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");

        V8ObjectUtils.pushValue(v8, array, map);

        V8Object result = array.getObject(0);
        assertEquals("bar", result.getString("foo"));
        result.close();
        array.close();
    }

    @Test
    public void testPushList() {
        V8Array array = new V8Array(v8);
        List<String> list = new ArrayList<String>();
        list.add("one");

        V8ObjectUtils.pushValue(v8, array, list);

        V8Array result = array.getArray(0);
        assertEquals("one", result.getString(0));
        result.close();
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
    }

    @Test
    public void testPopulateFromExistingByteArray() {
        V8Array array = v8.executeArrayScript("[0, 1, 2, 256]");
        byte[] result = new byte[1000];

        V8ObjectUtils.getTypedArray(array, V8Value.BYTE, result);

        assertEquals(1000, result.length);
        assertEquals(0, result[0]);
        assertEquals(1, result[1]);
        assertEquals(2, result[2]);
        assertEquals(0, result[3]);
        array.close();
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
        array.close();
    }

    @Test
    public void testPopulateFromNonExistingByteArray() {
        V8Array array = v8.executeArrayScript("[0, 1, 2, 256]");

        byte[] result = (byte[]) V8ObjectUtils.getTypedArray(array, V8Value.BYTE, null);

        assertEquals(4, result.length);
        assertEquals(0, result[0]);
        assertEquals(1, result[1]);
        assertEquals(2, result[2]);
        assertEquals(0, result[3]);
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
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
        array.close();
    }

    @Test
    public void testListContainsSelf() {
        List<Object> list = new ArrayList<Object>();
        list.add(list);

        V8Array v8Array = V8ObjectUtils.toV8Array(v8, list);
        V8Object self = v8Array.getObject(0);

        assertEquals(v8Array, self);
        self.close();
        v8Array.close();
    }

    @Test
    public void testMapContainsSelf() {
        Map<String, Object> map = new Hashtable<String, Object>();
        map.put("self", map);
        map.put("self2", map);
        map.put("self3", map);

        V8Object v8Object = V8ObjectUtils.toV8Object(v8, map);
        V8Object self = v8Object.getObject("self");

        assertEquals(v8Object, self);
        assertEquals(3, v8Object.getKeys().length);
        v8Object.close();
        self.close();
    }

    //@Test
    public void testParentChildMap() {
        Map<String, Object> parent = new Hashtable<String, Object>();
        Map<String, Object> child = new Hashtable<String, Object>();
        parent.put("child", child);
        child.put("parent", parent);

        V8Object v8Object = V8ObjectUtils.toV8Object(v8, parent);
        V8Object v8_child = v8Object.getObject("child");
        V8Object v8_parent = v8_child.getObject("parent");

        assertEquals(v8Object, v8_parent);
        v8Object.close();
        v8_child.close();
        v8_parent.close();
    }

    @Test
    public void testV8ArrayContainsSelf() {
        V8Array v8Array = new V8Array(v8);
        v8Array.push(v8Array);

        List<Object> list = V8ObjectUtils.toList(v8Array);

        assertEquals(list, list.get(0));
        v8Array.close();
    }

    @Test
    public void testV8ObjectContainsSelf() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("self", v8Object);

        Map<String, Object> map = V8ObjectUtils.toMap(v8Object);

        assertEquals(map, map.get("self"));
        assertEquals(map.hashCode(), map.get("self").hashCode());
        v8Object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBackpointer() {
        V8Object parent = v8.executeObjectScript("var parent = {};\n"
                + "var child = {parent : parent};\n"
                + "parent.child = child\n;"
                + "parent;");

        Map<String, Object> map = V8ObjectUtils.toMap(parent);

        assertEquals(map, ((Map<String, Object>) map.get("child")).get("parent"));
        parent.close();
    }

    @Test
    public void testCloneV8Object() {
        V8Object list = v8.executeObjectScript("var l = [{first:'ian', last:'bull'}, {first:'sadie', last:'bull'}]; l;");

        V8Object v8Object = V8ObjectUtils.toV8Object(v8, V8ObjectUtils.toMap(list));

        v8.add("l2", v8Object);
        v8.executeBooleanScript("JSON.stringify(l) === JSON.stringify(l2);");
        list.close();
        v8Object.close();
    }

    @Test
    public void testCloneV8Array() {
        V8Array list = v8.executeArrayScript("var l = [{first:'ian', last:'bull'}, {first:'sadie', last:'bull'}]; l;");

        V8Array v8Object = V8ObjectUtils.toV8Array(v8, V8ObjectUtils.toList(list));

        v8.add("l2", v8Object);
        v8.executeBooleanScript("JSON.stringify(l) === JSON.stringify(l2);");
        list.close();
        v8Object.close();
    }

    @Test
    public void testCloneV8ObjectsWithCircularStructure() {
        V8Object parent = v8.executeObjectScript("var parent = {};\n"
                + "var child = {parent : parent};\n"
                + "parent.child = child\n;"
                + "parent;");

        V8Object v8Object = V8ObjectUtils.toV8Object(v8, V8ObjectUtils.toMap(parent));

        assertEquals(1, v8Object.getKeys().length);
        assertEquals("child", v8Object.getKeys()[0]);
        parent.close();
        v8Object.close();
    }

    @Test
    public void testEqualSiblings() {
        V8Object parent = v8.executeObjectScript("var parent = {};\n"
                + "var child = {parent : parent};\n"
                + "parent.child1 = child\n;"
                + "parent.child2 = child\n;"
                + "parent;");

        Map<String, Object> map = V8ObjectUtils.toMap(parent);

        assertEquals((map.get("child2")), (map.get("child1")));
        parent.close();
    }

    @Test
    public void testArrayTypedArrayValue_ArrayBuffer() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var root = [buf];\n"
                + "root;\n");

        V8ArrayBuffer result = ((ArrayBuffer) V8ObjectUtils.getValue(root, 0)).getV8ArrayBuffer();

        assertEquals(100, result.limit());
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Int8Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int8Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = [intsArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((byte) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Int8ArrayWithoutBackingStore() {
        V8Array root = v8.executeArrayScript(""
                + "var intsArray = new Int8Array(24);\n"
                + "var root = [intsArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(24, result.length());
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Uint8Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint8Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = [intsArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((short) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Uint8ClampedArray() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var int8ClampedArray = new Uint8ClampedArray(buf);\n"
                + "int8ClampedArray[0] = 16;\n"
                + "var root = [int8ClampedArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((short) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Int16Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int16Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = [intsArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(50, result.length());
        assertEquals((short) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Uint16Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint16Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = [intsArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(50, result.length());
        assertEquals(16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Int32Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = [intsArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals(16, result.get(0));
        root.close();
        result.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Uint32Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint32Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = [intsArray]\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals((long) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Float32Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var floatsArray = new Float32Array(buf);\n"
                + "floatsArray[0] = 16.2;\n"
                + "var root = [floatsArray];"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals(16.2, (Float) result.get(0), 0.00001);
        root.close();
        result.close();
    }

    @Test
    public void testArrayTypedArrayValue_Float64Array() {
        V8Array root = v8.executeArrayScript("var buf = new ArrayBuffer(80);\n"
                + "var floatsArray = new Float64Array(buf);\n"
                + "floatsArray[0] = 16.2;\n"
                + "var root = [floatsArray];\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, 0)).getV8TypedArray();

        assertEquals(10, result.length());
        assertEquals(16.2, (Double) result.get(0), 0.0001);
        root.close();
        result.close();
    }

    @Test
    public void testArrayBufferAsProperty() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var root = { 'items' : buf };\n"
                + "root;\n");

        V8ArrayBuffer result = ((ArrayBuffer) V8ObjectUtils.getValue(root, "items")).getV8ArrayBuffer();

        assertEquals(100, result.limit());
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Int8Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int8Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = { 'items' : intsArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((byte) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Uint8Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint8Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = { 'items' : intsArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((short) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Uint8ClampedArray() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var int8ClampedArray = new Uint8ClampedArray(buf);\n"
                + "int8ClampedArray[0] = 16;\n"
                + "var root = { 'items' : int8ClampedArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((short) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Int16Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int16Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = { 'items' : intsArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(50, result.length());
        assertEquals((short) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Uint16Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint16Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = { 'items' : intsArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(50, result.length());
        assertEquals(16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Int32Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = { 'items' : intsArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals(16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Uint32Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint32Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "var root = { 'items' : intsArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals((long) 16, result.get(0));
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Float32Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(100);\n"
                + "var floatsArray = new Float32Array(buf);\n"
                + "floatsArray[0] = 16.2;\n"
                + "var root = { 'items' : floatsArray };"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals(16.2, (Float) result.get(0), 0.00001);
        root.close();
        result.close();
    }

    @Test
    public void testTypedArrayValue_Float64Array() {
        V8Object root = v8.executeObjectScript("var buf = new ArrayBuffer(80);\n"
                + "var floatsArray = new Float64Array(buf);\n"
                + "floatsArray[0] = 16.2;\n"
                + "var root = { 'items' : floatsArray };\n"
                + "root;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(root, "items")).getV8TypedArray();

        assertEquals(10, result.length());
        assertEquals(16.2, (Double) result.get(0), 0.0001);
        root.close();
        result.close();
    }


    @Test
    public void testTypedArrayInMap() {
        TypedArray int8Array = new TypedArray(v8, new ArrayBuffer(v8, ByteBuffer.allocateDirect(8)), V8Value.INT_8_ARRAY, 0, 8);
        V8TypedArray v8TypedArray = int8Array.getV8TypedArray();
        V8ArrayBuffer v8ArrayBuffer = v8TypedArray.getBuffer();
        v8ArrayBuffer.put(0, (byte) 7);
        v8TypedArray.close();
        v8ArrayBuffer.close();
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("array", int8Array);

        V8Object object = V8ObjectUtils.toV8Object(v8, map);

        V8TypedArray v8Array = (V8TypedArray) object.get("array");
        assertEquals((byte) 7, v8Array.get(0));
        assertEquals(V8Value.INT_8_ARRAY, v8Array.getType());
        v8Array.close();
        object.close();
    }

    @Test
    public void testGetTypedArray() {
        TypedArray int8Array = new TypedArray(v8, new ArrayBuffer(v8, ByteBuffer.allocateDirect(8)), V8Value.INT_8_ARRAY, 0, 8);
        V8TypedArray v8TypedArray = int8Array.getV8TypedArray();
        V8ArrayBuffer v8ArrayBuffer = v8TypedArray.getBuffer();
        v8ArrayBuffer.put(0, (byte) 7);
        v8TypedArray.close();
        v8ArrayBuffer.close();

        V8TypedArray v8Array = (V8TypedArray) V8ObjectUtils.getV8Result(v8, int8Array);

        assertEquals((byte) 7, v8Array.get(0));
        assertEquals(V8Value.INT_8_ARRAY, v8Array.getType());
        v8Array.close();
    }

    @Test
    public void testByteBufferInMap() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(v8, ByteBuffer.allocateDirect(8));
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("buffer", arrayBuffer);

        V8Object object = V8ObjectUtils.toV8Object(v8, map);

        V8ArrayBuffer v8ArrayBuffer = (V8ArrayBuffer) object.get("buffer");
        assertEquals(arrayBuffer.getV8ArrayBuffer().setWeak(), v8ArrayBuffer);
        v8ArrayBuffer.close();
        object.close();
    }

    @Test
    public void testArrayBufferInList() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(v8, ByteBuffer.allocateDirect(8));
        List<Object> list = new ArrayList<Object>();
        list.add(arrayBuffer);

        V8Array array = V8ObjectUtils.toV8Array(v8, list);

        V8ArrayBuffer v8ArrayBuffer = (V8ArrayBuffer) array.get(0);
        assertEquals(arrayBuffer.getV8ArrayBuffer().setWeak(), v8ArrayBuffer);
        v8ArrayBuffer.close();
        array.close();
    }

    @Test
    public void testGetArrayBuffer() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(v8, ByteBuffer.allocateDirect(8));

        V8ArrayBuffer v8ArrayBuffer = (V8ArrayBuffer) V8ObjectUtils.getV8Result(v8, arrayBuffer);

        assertEquals(arrayBuffer.getV8ArrayBuffer().setWeak(), v8ArrayBuffer);
        v8ArrayBuffer.close();
    }

    @Test
    public void testGetArrayBufferNotReleased() {
        ArrayBuffer arrayBuffer = new ArrayBuffer(v8, ByteBuffer.allocateDirect(8));

        V8ArrayBuffer v8ArrayBuffer = (V8ArrayBuffer) V8ObjectUtils.getV8Result(v8, arrayBuffer);

        assertFalse(v8ArrayBuffer.isReleased());
        v8ArrayBuffer.close();
    }

    @Test
    public void testTypedArrayInList() {
        TypedArray int8Array = new TypedArray(v8, new ArrayBuffer(v8, ByteBuffer.allocateDirect(8)), V8Value.INT_8_ARRAY, 0, 8);
        V8TypedArray v8TypedArray = int8Array.getV8TypedArray();
        V8ArrayBuffer v8ArrayBuffer = v8TypedArray.getBuffer();
        v8ArrayBuffer.put(0, (byte) 7);
        v8TypedArray.close();
        v8ArrayBuffer.close();
        List<Object> list = new ArrayList<Object>();
        list.add(int8Array);

        V8Array array = V8ObjectUtils.toV8Array(v8, list);

        V8Array v8Array = (V8Array) array.get(0);
        assertEquals((byte) 7, v8Array.get(0));
        assertEquals(V8Value.INT_8_ARRAY, v8Array.getType());
        v8Array.close();
        array.close();
    }

    @Test
    public void testPushV8ArrayToArray() {
        V8Array array = new V8Array(v8);
        V8Array child = new V8Array(v8);

        V8ObjectUtils.pushValue(v8, array, child);

        V8Object result = (V8Object) array.get(0);

        assertEquals(child, result);
        array.close();
        result.close();
        child.close();
    }

    @Test
    public void testPushV8ObjectToArray() {
        V8Array array = new V8Array(v8);
        V8Object child = new V8Object(v8);

        V8ObjectUtils.pushValue(v8, array, child);

        V8Object result = (V8Object) array.get(0);

        assertEquals(child, result);
        array.close();
        result.close();
        child.close();
    }

    @Test
    public void testPushV8TypedArrayToArray() {
        V8Array array = new V8Array(v8);
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        V8Object child = new V8TypedArray(v8, buffer, V8Value.INT_8_ARRAY, 0, 10);

        V8ObjectUtils.pushValue(v8, array, child);

        V8Object result = (V8Object) array.get(0);

        assertEquals(child, result);
        array.close();
        result.close();
        child.close();
        buffer.close();
    }

    @Test
    public void testV8ArrayBufferInMap() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("buffer", buffer);

        V8Object v8Object = V8ObjectUtils.toV8Object(v8, map);

        V8Value result = (V8Value) v8Object.get("buffer");
        assertEquals(buffer, result);
        buffer.close();
        result.close();
        v8Object.close();
    }

    @Test
    public void testV8ArrayBufferInList() {
        V8ArrayBuffer buffer = new V8ArrayBuffer(v8, 10);
        List<Object> list = new ArrayList<Object>();
        list.add(buffer);

        V8Array v8Array = V8ObjectUtils.toV8Array(v8, list);

        V8Value result = (V8Value) v8Array.get(0);
        assertEquals(buffer, result);
        buffer.close();
        result.close();
        v8Array.close();
    }

    @Test
    public void testIntegerTypeAdapter_Map() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.INTEGER) {

            @Override
            public Object adapt(final Object value) {
                return 7;
            }
        });

        assertEquals(4, map.size());
        assertEquals(7, map.get("a"));
        assertEquals(3.14, (Double) map.get("b"), 0.00001);
        assertEquals(false, map.get("c"));
        assertEquals("string", map.get("d"));

        object.close();
    }

    @Test
    public void testDoubleTypeAdapter_Map() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.DOUBLE) {

            @Override
            public Object adapt(final Object value) {
                return 7.7;
            }
        });

        assertEquals(4, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(7.7, (Double) map.get("b"), 0.00001);
        assertEquals(false, map.get("c"));
        assertEquals("string", map.get("d"));

        object.close();
    }

    @Test
    public void testBooleanTypeAdapter_Map() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.BOOLEAN) {

            @Override
            public Object adapt(final Object value) {
                return true;
            }
        });

        assertEquals(4, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(3.14, (Double) map.get("b"), 0.00001);
        assertEquals(true, map.get("c"));
        assertEquals("string", map.get("d"));

        object.close();
    }

    @Test
    public void testStringTypeAdapter_Map() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.STRING) {

            @Override
            public Object adapt(final Object value) {
                return "foo";
            }
        });

        assertEquals(4, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(3.14, (Double) map.get("b"), 0.00001);
        assertEquals(false, map.get("c"));
        assertEquals("foo", map.get("d"));

        object.close();
    }

    @Test
    public void testObjectTypeAdapter_Map() {
        V8Object object = v8.executeObjectScript("x = {b:{a:{}}}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return "object";
            }
        });

        assertEquals(1, map.size());
        assertEquals("object", map.get("b"));

        object.close();
    }

    @Test
    public void testObjectNullTypeAdapter_Map() {
        V8Object object = v8.executeObjectScript("x = {b:{a:{}}}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return null;
            }
        });

        assertEquals(1, map.size());
        assertNull(map.get("b"));

        object.close();
    }

    @Test
    public void testObjectUndefinedTypeAdapter_Map() {
        V8Object object = v8.executeObjectScript("x = {b:{a:{}}}; x");

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return V8.getUndefined();
            }
        });

        assertEquals(1, map.size());
        assertEquals(V8.getUndefined(), map.get("b"));

        object.close();
    }

    @Test
    public void testIntegerTypeAdapter_List() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = V8ObjectUtils.toList(array, new SingleTypeAdapter(V8Value.INTEGER) {

            @Override
            public Object adapt(final Object value) {
                return 7;
            }
        });

        assertEquals(4, list.size());
        assertEquals(7, list.get(0));
        assertEquals(3.14, (Double) list.get(1), 0.00001);
        assertEquals(false, list.get(2));
        assertEquals("string", list.get(3));

        array.close();
    }

    @Test
    public void testDoubleTypeAdapter_List() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = V8ObjectUtils.toList(array, new SingleTypeAdapter(V8Value.DOUBLE) {

            @Override
            public Object adapt(final Object value) {
                return 7.7;
            }
        });

        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(7.7, (Double) list.get(1), 0.00001);
        assertEquals(false, list.get(2));
        assertEquals("string", list.get(3));

        array.close();
    }

    @Test
    public void testBooleanTypeAdapter_List() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = V8ObjectUtils.toList(array, new SingleTypeAdapter(V8Value.BOOLEAN) {

            @Override
            public Object adapt(final Object value) {
                return true;
            }
        });

        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3.14, (Double) list.get(1), 0.00001);
        assertEquals(true, list.get(2));
        assertEquals("string", list.get(3));

        array.close();
    }

    @Test
    public void testStringTypeAdapter_List() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = V8ObjectUtils.toList(array, new SingleTypeAdapter(V8Value.STRING) {

            @Override
            public Object adapt(final Object value) {
                return "foo";
            }
        });

        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3.14, (Double) list.get(1), 0.00001);
        assertEquals(false, list.get(2));
        assertEquals("foo", list.get(3));

        array.close();
    }

    @Test
    public void testObjectTypeAdapter_List() {
        V8Array array = v8.executeArrayScript("x = [{a:{}}]; x");

        List<? super Object> list = V8ObjectUtils.toList(array, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return "object";
            }
        });

        assertEquals(1, list.size());
        assertEquals("object", list.get(0));

        array.close();
    }

    @Test
    public void testObjectNullTypeAdapter_List() {
        V8Array array = v8.executeArrayScript("x = [{a:{}}]; x");

        List<? super Object> list = V8ObjectUtils.toList(array, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return null;
            }
        });

        assertEquals(1, list.size());
        assertNull(list.get(0));

        array.close();
    }

    @Test
    public void testObjectUndefinedTypeAdapter_List() {
        V8Array array = v8.executeArrayScript("x = [{a:{}}]; x");

        List<? super Object> list = V8ObjectUtils.toList(array, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return V8.getUndefined();
            }
        });

        assertEquals(1, list.size());
        assertEquals(V8.getUndefined(), list.get(0));

        array.close();
    }

    @Test
    public void testCreateFunctionMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {a:function() {return 'a'}, b:function(){return 'b'}}; x");
        MemoryManager memoryManager = new MemoryManager(v8);

        Map<String, ? super Object> map = V8ObjectUtils.toMap(object, new SingleTypeAdapter(V8Value.V8_FUNCTION) {

            @Override
            public Object adapt(final Object value) {
                return ((V8Function) value).twin();
            }
        });

        assertEquals(2, map.size());
        assertEquals("a", ((V8Function) map.get("a")).call(null, null));
        assertEquals("b", ((V8Function) map.get("b")).call(null, null));

        memoryManager.release();
        object.close();
    }

    @Test
    public void testCreateFunctionListFromV8Object() {
        V8Array object = v8.executeArrayScript("x = [function() {return 'a'}, function(){return 'b'}]; x");
        MemoryManager memoryManager = new MemoryManager(v8);

        List<? super Object> list = V8ObjectUtils.toList(object, new SingleTypeAdapter(V8Value.V8_FUNCTION) {

            @Override
            public Object adapt(final Object value) {
                return ((V8Function) value).twin();
            }
        });

        assertEquals(2, list.size());
        assertEquals("a", ((V8Function) list.get(0)).call(null, null));
        assertEquals("b", ((V8Function) list.get(1)).call(null, null));

        memoryManager.release();
        object.close();
    }

    @Test
    public void testGetValue_Integer() {
        int result = (Integer) V8ObjectUtils.getValue(7);

        assertEquals(7, result);
    }

    @Test
    public void testGetValue_Double() {
        double result = (Double) V8ObjectUtils.getValue(3.14);

        assertEquals(3.14, result, 0.00001);
    }

    @Test
    public void testGetValue_Boolean() {
        boolean result = (Boolean) V8ObjectUtils.getValue(false);

        assertFalse(result);
    }

    @Test
    public void testGetValue_String() {
        String result = (String) V8ObjectUtils.getValue("foo");

        assertEquals("foo", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateIntegerMapFromV8Object_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:2, c:3}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMapWithFunction_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a : function() {return 1;}, b : 'foo'}; x;");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(1, map.size());
        assertEquals("foo", map.get("b"));
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateListWithFunction_GetValue() {
        V8Array array = v8.executeArrayScript("x = [function() {return 1;}, 'foo']; x;");

        List<Object> list = (List<Object>) V8ObjectUtils.getValue(array);

        assertEquals(1, list.size());
        assertEquals("foo", list.get(0));
        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDoubleMapFromV8Object_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:1.1, b:2.2, c:3.3, d:4.4}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(4, map.size());
        assertEquals(1.1, (Double) map.get("a"), 0.000001);
        assertEquals(2.2, (Double) map.get("b"), 0.000001);
        assertEquals(3.3, (Double) map.get("c"), 0.000001);
        assertEquals(4.4, (Double) map.get("d"), 0.000001);
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateStringMapFromV8Object_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:'foo', b:'bar', c:'baz', d:'boo'}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(4, map.size());
        assertEquals("foo", map.get("a"));
        assertEquals("bar", map.get("b"));
        assertEquals("baz", map.get("c"));
        assertEquals("boo", map.get("d"));
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateBooleanMapFromV8Object_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:true, b:1==1, c:false, d:1!=1}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(4, map.size());
        assertTrue((Boolean) map.get("a"));
        assertTrue((Boolean) map.get("b"));
        assertFalse((Boolean) map.get("c"));
        assertFalse((Boolean) map.get("d"));
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMixedMapFromV8Object_GetValue() {
        V8Object object = v8.executeObjectScript("x = {boolean:true, integer:1, double:3.14159, string:'hello'}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(4, map.size());
        assertTrue((Boolean) map.get("boolean"));
        assertEquals(1, (int) (Integer) map.get("integer"));
        assertEquals(3.14159, (Double) map.get("double"), 0.0000001);
        assertEquals("hello", map.get("string"));
        object.close();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCreateNestedMapFromV8Object_GetValue() {
        V8Object object = v8.executeObjectScript("x = { name : { first :'john', last: 'smith'}, age: 7}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(2, map.size());
        assertEquals(7, map.get("age"));
        assertEquals("john", ((Map) map.get("name")).get("first"));
        assertEquals("smith", ((Map) map.get("name")).get("last"));
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateListFromV8Array_GetValue() {
        V8Array array = v8.executeArrayScript("x = [1,2,3]; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateListWithUndefinedFromV8Array_GetValue() {
        V8Array array = v8.executeArrayScript("x = [1,2,3]; x[9] = 10; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array);

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
        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateListWithNullFromV8Array_GetValue() {
        V8Array array = v8.executeArrayScript("x = [null]; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array);

        assertEquals(1, list.size());
        assertNull(list.get(0));
        array.close();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCreateMatrixFromV8Array_GetValue() {
        V8Array array = v8.executeArrayScript("x = [[1,2,3],[true,false,true],['this','that','other']]; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array);

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
        array.close();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCreateMapWithLists_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:[1,2,3], b:[4,5,6]}; x;");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(2, map.size());
        assertEquals(1, ((List) map.get("a")).get(0));
        assertEquals(2, ((List) map.get("a")).get(1));
        assertEquals(3, ((List) map.get("a")).get(2));
        assertEquals(4, ((List) map.get("b")).get(0));
        assertEquals(5, ((List) map.get("b")).get(1));
        assertEquals(6, ((List) map.get("b")).get(2));
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMapWithNulls_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:null}; x;");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object);

        assertEquals(1, map.size());
        assertNull(map.get("a"));
        object.close();
    }

    @Test
    public void testNullObjectGivesNull_GetValue() {
        Object result = V8ObjectUtils.getValue(null);

        assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testV8ArrayContainsSelf_GetValue() {
        V8Array v8Array = new V8Array(v8);
        v8Array.push(v8Array);

        List<Object> list = (List<Object>) V8ObjectUtils.getValue(v8Array);

        assertEquals(list, list.get(0));
        v8Array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testV8ObjectContainsSelf_GetValue() {
        V8Object v8Object = new V8Object(v8);
        v8Object.add("self", v8Object);

        Map<String, Object> map = (Map<String, Object>) V8ObjectUtils.getValue(v8Object);

        assertEquals(map, map.get("self"));
        assertEquals(map.hashCode(), map.get("self").hashCode());
        v8Object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBackpointer_GetValue() {
        V8Object parent = v8.executeObjectScript("var parent = {};\n"
                + "var child = {parent : parent};\n"
                + "parent.child = child\n;"
                + "parent;");

        Map<String, Object> map = (Map<String, Object>) V8ObjectUtils.getValue(parent);

        assertEquals(map, ((Map<String, Object>) map.get("child")).get("parent"));
        parent.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCloneV8Array_GetValue() {
        V8Array list = v8.executeArrayScript("var l = [{first:'ian', last:'bull'}, {first:'sadie', last:'bull'}]; l;");

        V8Array v8Object = V8ObjectUtils.toV8Array(v8, (List<? extends Object>) V8ObjectUtils.getValue(list));

        v8.add("l2", v8Object);
        v8.executeBooleanScript("JSON.stringify(l) === JSON.stringify(l2);");
        list.close();
        v8Object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCloneV8ObjectsWithCircularStructure_GetValue() {
        V8Object parent = v8.executeObjectScript("var parent = {};\n"
                + "var child = {parent : parent};\n"
                + "parent.child = child\n;"
                + "parent;");

        V8Object v8Object = V8ObjectUtils.toV8Object(v8, (Map<String, ? extends Object>) V8ObjectUtils.getValue(parent));

        assertEquals(1, v8Object.getKeys().length);
        assertEquals("child", v8Object.getKeys()[0]);
        parent.close();
        v8Object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEqualSiblings_GetValue() {
        V8Object parent = v8.executeObjectScript("var parent = {};\n"
                + "var child = {parent : parent};\n"
                + "parent.child1 = child\n;"
                + "parent.child2 = child\n;"
                + "parent;");

        Map<String, Object> map = (Map<String, Object>) V8ObjectUtils.getValue(parent);

        assertEquals((map.get("child2")), (map.get("child1")));
        parent.close();
    }

    @Test
    public void testTypedArrayGetValue_Int8Array() {
        V8Array intsArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int8Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "intsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(intsArray)).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((byte) 16, result.get(0));
        intsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Int8ArrayWithoutBackingStore() {
        V8Array intsArray = v8.executeArrayScript(""
                + "var intsArray = new Int8Array(24);\n"
                + "intsArray");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(intsArray)).getV8TypedArray();

        assertEquals(24, result.length());
        intsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Uint8Array() {
        V8Array intsArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint8Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "intsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(intsArray)).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((short) 16, result.get(0));
        intsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Uint8ClampedArray() {
        V8Array int8ClampedArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var int8ClampedArray = new Uint8ClampedArray(buf);\n"
                + "int8ClampedArray[0] = 16;\n"
                + "int8ClampedArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(int8ClampedArray)).getV8TypedArray();

        assertEquals(100, result.length());
        assertEquals((short) 16, result.get(0));
        int8ClampedArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Int16Array() {
        V8Array intsArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int16Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "intsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(intsArray)).getV8TypedArray();

        assertEquals(50, result.length());
        assertEquals((short) 16, result.get(0));
        intsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Uint16Array() {
        V8Array intsArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint16Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "intsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(intsArray)).getV8TypedArray();

        assertEquals(50, result.length());
        assertEquals(16, result.get(0));
        intsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Int32Array() {
        V8Array intsArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "intsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(intsArray)).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals(16, result.get(0));
        intsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Uint32Array() {
        V8Array intsArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Uint32Array(buf);\n"
                + "intsArray[0] = 16;\n"
                + "intsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(intsArray)).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals(16L, result.get(0));
        intsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Float32Array() {
        V8Array floatsArray = v8.executeArrayScript("var buf = new ArrayBuffer(100);\n"
                + "var floatsArray = new Float32Array(buf);\n"
                + "floatsArray[0] = 16.2;\n"
                + "floatsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(floatsArray)).getV8TypedArray();

        assertEquals(25, result.length());
        assertEquals(16.2, (Float) result.get(0), 0.00001);
        floatsArray.close();
        result.close();
    }

    @Test
    public void testTypedArrayGetValue_Float64Array() {
        V8Array floatsArray = v8.executeArrayScript("var buf = new ArrayBuffer(80);\n"
                + "var floatsArray = new Float64Array(buf);\n"
                + "floatsArray[0] = 16.2;\n"
                + "floatsArray;\n");

        V8TypedArray result = ((TypedArray) V8ObjectUtils.getValue(floatsArray)).getV8TypedArray();

        assertEquals(10, result.length());
        assertEquals(16.2, (Double) result.get(0), 0.0001);
        floatsArray.close();
        result.close();
    }

    @Test
    public void testArrayBufferGetValue() {
        V8ArrayBuffer buf = (V8ArrayBuffer) v8.executeScript("var buf = new ArrayBuffer(100);\n"
                + "buf;\n");

        V8ArrayBuffer result = ((ArrayBuffer) V8ObjectUtils.getValue(buf)).getV8ArrayBuffer();

        assertEquals(100, result.limit());
        buf.close();
        result.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIntegerTypeAdapter_Map_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.INTEGER) {

            @Override
            public Object adapt(final Object value) {
                return 7;
            }
        });

        assertEquals(4, map.size());
        assertEquals(7, map.get("a"));
        assertEquals(3.14, (Double) map.get("b"), 0.00001);
        assertEquals(false, map.get("c"));
        assertEquals("string", map.get("d"));

        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoubleTypeAdapter_Map_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.DOUBLE) {

            @Override
            public Object adapt(final Object value) {
                return 7.7;
            }
        });

        assertEquals(4, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(7.7, (Double) map.get("b"), 0.00001);
        assertEquals(false, map.get("c"));
        assertEquals("string", map.get("d"));

        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBooleanTypeAdapter_Map_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.BOOLEAN) {

            @Override
            public Object adapt(final Object value) {
                return true;
            }
        });

        assertEquals(4, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(3.14, (Double) map.get("b"), 0.00001);
        assertEquals(true, map.get("c"));
        assertEquals("string", map.get("d"));

        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStringTypeAdapter_Map_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:1, b:3.14, c:false, d:'string'}; x");

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.STRING) {

            @Override
            public Object adapt(final Object value) {
                return "foo";
            }
        });

        assertEquals(4, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(3.14, (Double) map.get("b"), 0.00001);
        assertEquals(false, map.get("c"));
        assertEquals("foo", map.get("d"));

        object.close();
    }

    @Test
    public void testObjectTypeAdapter_Map_GetValue() {
        V8Object object = v8.executeObjectScript("x = {b:{a:{}}}; x");

        Object result = V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return "object";
            }
        });

        assertEquals("object", result);

        object.close();
    }

    @Test
    public void testObjectNullTypeAdapter_Map_GetValue() {
        V8Object object = v8.executeObjectScript("x = {b:{a:{}}}; x");

        Object result = V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return null;
            }
        });

        assertNull(result);

        object.close();
    }

    @Test
    public void testObjectUndefinedTypeAdapter_Map_GetValue() {
        V8Object object = v8.executeObjectScript("x = {b:{a:{}}}; x");

        @SuppressWarnings("resource")
        V8Value result = (V8Value) V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return V8.getUndefined();
            }
        });

        assertTrue(result.isUndefined());

        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIntegerTypeAdapter_List_GetValue() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array, new SingleTypeAdapter(V8Value.INTEGER) {

            @Override
            public Object adapt(final Object value) {
                return 7;
            }
        });

        assertEquals(4, list.size());
        assertEquals(7, list.get(0));
        assertEquals(3.14, (Double) list.get(1), 0.00001);
        assertEquals(false, list.get(2));
        assertEquals("string", list.get(3));

        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoubleTypeAdapter_List_GetValue() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array, new SingleTypeAdapter(V8Value.DOUBLE) {

            @Override
            public Object adapt(final Object value) {
                return 7.7;
            }
        });

        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(7.7, (Double) list.get(1), 0.00001);
        assertEquals(false, list.get(2));
        assertEquals("string", list.get(3));

        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBooleanTypeAdapter_List_GetValue() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array, new SingleTypeAdapter(V8Value.BOOLEAN) {

            @Override
            public Object adapt(final Object value) {
                return true;
            }
        });

        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3.14, (Double) list.get(1), 0.00001);
        assertEquals(true, list.get(2));
        assertEquals("string", list.get(3));

        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStringTypeAdapter_List_GetValue() {
        V8Array array = v8.executeArrayScript("x = [1, 3.14, false, 'string']; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array, new SingleTypeAdapter(V8Value.STRING) {

            @Override
            public Object adapt(final Object value) {
                return "foo";
            }
        });

        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3.14, (Double) list.get(1), 0.00001);
        assertEquals(false, list.get(2));
        assertEquals("foo", list.get(3));

        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testObjectTypeAdapter_List_GetValue() {
        V8Array array = v8.executeArrayScript("x = [{a:{}}]; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return "object";
            }
        });

        assertEquals(1, list.size());
        assertEquals("object", list.get(0));

        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testObjectNullTypeAdapter_List_GetValue() {
        V8Array array = v8.executeArrayScript("x = [{a:{}}]; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return null;
            }
        });

        assertEquals(1, list.size());
        assertNull(list.get(0));

        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testObjectUndefinedTypeAdapter_List_GetValue() {
        V8Array array = v8.executeArrayScript("x = [{a:{}}]; x");

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(array, new SingleTypeAdapter(V8Value.V8_OBJECT) {

            @Override
            public Object adapt(final Object value) {
                return V8.getUndefined();
            }
        });

        assertEquals(1, list.size());
        assertEquals(V8.getUndefined(), list.get(0));

        array.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateFunctionMapFromV8Object_GetValue() {
        V8Object object = v8.executeObjectScript("x = {a:function() {return 'a'}, b:function(){return 'b'}}; x");
        MemoryManager memoryManager = new MemoryManager(v8);

        Map<String, ? super Object> map = (Map<String, ? super Object>) V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.V8_FUNCTION) {

            @Override
            public Object adapt(final Object value) {
                return ((V8Function) value).twin();
            }
        });

        assertEquals(2, map.size());
        assertEquals("a", ((V8Function) map.get("a")).call(null, null));
        assertEquals("b", ((V8Function) map.get("b")).call(null, null));

        memoryManager.release();
        object.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateFunctionListFromV8Object_GetValue() {
        V8Array object = v8.executeArrayScript("x = [function() {return 'a'}, function(){return 'b'}]; x");
        MemoryManager memoryManager = new MemoryManager(v8);

        List<? super Object> list = (List<? super Object>) V8ObjectUtils.getValue(object, new SingleTypeAdapter(V8Value.V8_FUNCTION) {

            @Override
            public Object adapt(final Object value) {
                return ((V8Function) value).twin();
            }
        });

        assertEquals(2, list.size());
        assertEquals("a", ((V8Function) list.get(0)).call(null, null));
        assertEquals("b", ((V8Function) list.get(1)).call(null, null));

        memoryManager.release();
        object.close();
    }

    private int registerAndRelease(final String name, final List<? extends Object> list) {
        V8Array array = V8ObjectUtils.toV8Array(v8, list);
        v8.add(name, array);
        int size = array.length();
        array.close();
        return size;
    }

    private int registerAndRelease(final String name, final Map<String, ? extends Object> map) {
        V8Object object = V8ObjectUtils.toV8Object(v8, map);
        v8.add(name, object);
        int size = getNumberOfProperties(object);
        object.close();
        return size;
    }

    private int getNumberOfProperties(final V8Object object) {
        return object.getKeys().length;
    }

}
