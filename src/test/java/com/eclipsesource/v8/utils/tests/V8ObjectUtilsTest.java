package com.eclipsesource.v8.utils.tests;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

        Map<String, Object> map = V8ObjectUtils.asMap(object);

        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        object.release();
    }

    @Test
    public void testCreateDoubleMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {a:1.1, b:2.2, c:3.3, d:4.4}; x");

        Map<String, Object> map = V8ObjectUtils.asMap(object);

        assertEquals(4, map.size());
        assertEquals(1.1, (double) map.get("a"), 0.000001);
        assertEquals(2.2, (double) map.get("b"), 0.000001);
        assertEquals(3.3, (double) map.get("c"), 0.000001);
        assertEquals(4.4, (double) map.get("d"), 0.000001);
        object.release();
    }

    @Test
    public void testCreateStringMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {a:'foo', b:'bar', c:'baz', d:'boo'}; x");

        Map<String, Object> map = V8ObjectUtils.asMap(object);

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

        Map<String, Object> map = V8ObjectUtils.asMap(object);

        assertEquals(4, map.size());
        assertTrue((boolean) map.get("a"));
        assertTrue((boolean) map.get("b"));
        assertFalse((boolean) map.get("c"));
        assertFalse((boolean) map.get("d"));
        object.release();
    }

    @Test
    public void testCreateMixedMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = {boolean:true, integer:1, double:3.14159, string:'hello'}; x");

        Map<String, Object> map = V8ObjectUtils.asMap(object);

        assertEquals(4, map.size());
        assertTrue((boolean) map.get("boolean"));
        assertEquals(1, (int) map.get("integer"));
        assertEquals(3.14159, (double) map.get("double"), 0.0000001);
        assertEquals("hello", map.get("string"));
        object.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateNestedMapFromV8Object() {
        V8Object object = v8.executeObjectScript("x = { name : { first :'john', last: 'smith'}, age: 7}; x");

        Map<String, Object> map = V8ObjectUtils.asMap(object);

        assertEquals(2, map.size());
        assertEquals(7, map.get("age"));
        assertEquals("john", ((Map) map.get("name")).get("first"));
        assertEquals("smith", ((Map) map.get("name")).get("last"));
        object.release();
    }

    @Test
    public void testCreateListFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [1,2,3]; x");

        List<Object> list = V8ObjectUtils.asList(array);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        array.release();
    }

    @Test
    public void testCreateListWithNullsFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [1,2,3]; x[9] = 10; x");

        List<Object> list = V8ObjectUtils.asList(array);

        assertEquals(10, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertNull(list.get(3));
        assertNull(list.get(4));
        assertNull(list.get(5));
        assertNull(list.get(6));
        assertNull(list.get(7));
        assertNull(list.get(8));
        assertEquals(10, list.get(9));
        array.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateMatrixFromV8Array() {
        V8Array array = v8.executeArrayScript("x = [[1,2,3],[true,false,true],['this','that','other']]; x");

        List<Object> list = V8ObjectUtils.asList(array);

        assertEquals(3, list.size());
        assertEquals(3, ((List) list.get(0)).size());
        assertEquals(3, ((List) list.get(1)).size());
        assertEquals(3, ((List) list.get(2)).size());
        assertEquals(1, ((List) list.get(0)).get(0));
        assertEquals(2, ((List) list.get(0)).get(1));
        assertEquals(3, ((List) list.get(0)).get(2));
        assertTrue((boolean) ((List) list.get(1)).get(0));
        assertFalse((boolean) ((List) list.get(1)).get(1));
        assertTrue((boolean) ((List) list.get(1)).get(2));
        assertEquals("this", ((List) list.get(2)).get(0));
        assertEquals("that", ((List) list.get(2)).get(1));
        assertEquals("other", ((List) list.get(2)).get(2));
        array.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCreateMapWithLists() {
        V8Object object = v8.executeObjectScript("x = {a:[1,2,3], b:[4,5,6]}; x;");

        Map<String, Object> map = V8ObjectUtils.asMap(object);

        assertEquals(2, map.size());
        assertEquals(1, ((List) map.get("a")).get(0));
        assertEquals(2, ((List) map.get("a")).get(1));
        assertEquals(3, ((List) map.get("a")).get(2));
        assertEquals(4, ((List) map.get("b")).get(0));
        assertEquals(5, ((List) map.get("b")).get(1));
        assertEquals(6, ((List) map.get("b")).get(2));
        object.release();
    }

}
