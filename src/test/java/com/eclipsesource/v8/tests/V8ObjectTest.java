package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class V8ObjectTest {

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
    public void testCreateReleaseObject() {
        for (int i = 0; i < 1000; i++) {
            V8Object persistentV8Object = new V8Object(v8);
            persistentV8Object.release();
        }
    }

    @Test
    public void testReleaseRuntimeDoesNotReleaseObject() {
        try {
            new V8Object(v8);
            v8.release();
        } catch (IllegalStateException e) {
            v8 = V8.createV8Runtime();
            return;
        }
        fail("Illegal State Exception not thrown.");
    }

    @Test
    public void testGetV8Object() {
        v8.executeVoidScript("foo = {key: 'value'}");

        V8Object object = v8.getObject("foo");

        assertTrue(object.contains("key"));
        assertFalse(object.contains("noKey"));
        object.release();
    }

    @Test
    public void testGetMultipleV8Object() {
        v8.executeVoidScript("foo = {key: 'value'}; " + "bar={key : 'value'}");

        V8Object fooObject = v8.getObject("foo");
        V8Object barObject = v8.getObject("bar");

        assertTrue(fooObject.contains("key"));
        assertFalse(fooObject.contains("noKey"));
        assertTrue(barObject.contains("key"));
        assertFalse(barObject.contains("noKey"));

        fooObject.release();
        barObject.release();
    }

    @Test
    public void testGetNestedV8Object() {
        v8.executeVoidScript("foo = {nested: {key : 'value'}}");

        for (int i = 0; i < 1000; i++) {
            V8Object fooObject = v8.getObject("foo");
            V8Object nested = fooObject.getObject("nested");

            assertTrue(fooObject.contains("nested"));
            assertTrue(nested.contains("key"));
            assertFalse(nested.contains("noKey"));
            fooObject.release();
            nested.release();
        }
    }

    /*** Get Array ***/
    @Test
    public void testGetV8ArrayV8Object() {
        v8.executeVoidScript("foo = {array : [1,2,3]}");

        V8Object object = v8.getObject("foo");
        V8Array array = object.getArray("array");

        assertEquals(3, array.length());
        assertEquals(1, array.getInteger(0));
        assertEquals(2, array.getInteger(1));
        assertEquals(3, array.getInteger(2));
        array.release();
        object.release();
    }

    /*** Get Primitives ***/
    @Test
    public void testGetIntegerV8Object() {
        v8.executeVoidScript("foo = {bar: 7}");

        V8Object foo = v8.getObject("foo");

        assertEquals(7, foo.getInteger("bar"));
        foo.release();
    }

    @Test
    public void testNestedInteger() {
        v8.executeVoidScript("foo = {bar: {key:6}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals(6, bar.getInteger("key"));
        foo.release();
        bar.release();
    }

    @Test
    public void testGetDoubleV8Object() {
        v8.executeVoidScript("foo = {bar: 7.1}");

        V8Object foo = v8.getObject("foo");

        assertEquals(7.1, foo.getDouble("bar"), 0.0001);
        foo.release();
    }

    @Test
    public void testNestedDouble() {
        v8.executeVoidScript("foo = {bar: {key:6.1}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals(6.1, bar.getDouble("key"), 0.0001);
        foo.release();
        bar.release();
    }

    @Test
    public void testGetBooleanV8Object() {
        v8.executeVoidScript("foo = {bar: false}");

        V8Object foo = v8.getObject("foo");

        assertFalse(foo.getBoolean("bar"));
        foo.release();
    }

    @Test
    public void testNestedBoolean() {
        v8.executeVoidScript("foo = {bar: {key:true}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertTrue(bar.getBoolean("key"));
        foo.release();
        bar.release();
    }

    @Test
    public void testGetStringV8Object() {
        v8.executeVoidScript("foo = {bar: 'string'}");

        V8Object foo = v8.getObject("foo");

        assertEquals("string", foo.getString("bar"));
        foo.release();
    }

    @Test
    public void testNestedString() {
        v8.executeVoidScript("foo = {bar: {key:'string'}}");

        V8Object foo = v8.getObject("foo");
        V8Object bar = foo.getObject("bar");

        assertEquals("string", bar.getString("key"));
        foo.release();
        bar.release();
    }

    /*** Execute Object Function ***/
    @Test
    public void testObjectScript() {
        v8.executeVoidScript("function foo() { return { x : 7 }} ");

        V8Object result = v8.executeObjectFunction("foo", null);

        assertEquals(7, result.getInteger("x"));
        result.release();
    }

    /*** Add Primitives ***/
    @Test
    public void testAddString() {
        V8Object v8Object = new V8Object(v8).add("hello", "world");

        assertEquals("world", v8Object.getString("hello"));
        v8Object.release();
    }

    @Test
    public void testAddInt() {
        V8Object v8Object = new V8Object(v8).add("hello", 7);

        assertEquals(7, v8Object.getInteger("hello"));
        v8Object.release();
    }

    @Test
    public void testAddDouble() {
        V8Object v8Object = new V8Object(v8).add("hello", 3.14159);

        assertEquals(3.14159, v8Object.getDouble("hello"), 0.000001);
        v8Object.release();
    }

    @Test
    public void testAddBoolean() {
        V8Object v8Object = new V8Object(v8).add("hello", true);

        assertTrue(v8Object.getBoolean("hello"));
        v8Object.release();
    }

    @Test
    public void testObjectChangedFromJS() {
        V8Object v8Object = new V8Object(v8).add("hello", "world");
        v8.add("object", v8Object);

        v8.executeVoidScript("object.world = 'goodbye'");

        assertEquals("goodbye", v8Object.getString("world"));
        v8Object.release();
    }

    @Test
    public void testObjectChangedFromAPI() {
        v8.executeVoidScript("object = {world : 'goodbye'}");

        V8Object v8Object = v8.getObject("object").add("world", "hello");

        assertEquals("hello", v8Object.getString("world"));
        v8Object.release();
    }

    /*** Add Object ***/
    @Test
    public void testAddObject() {
        V8Object v8Object = new V8Object(v8).add("hello", true);

        v8.add("foo", v8Object);

        V8Object foo = v8.getObject("foo");
        assertTrue(foo.getBoolean("hello"));
        foo.release();
        v8Object.release();
    }

    @Test
    public void testAddObjectWithString() {
        V8Object v8Object = new V8Object(v8).add("hello", "world");

        v8.add("foo", v8Object);

        String result = v8.executeStringScript("foo.hello");
        assertEquals("world", result);
        v8Object.release();
    }

    @Test
    public void testAddObjectWithBoolean() {
        V8Object v8Object = new V8Object(v8).add("boolean", false);

        v8.add("foo", v8Object);

        boolean result = v8.executeBooleanScript("foo.boolean");
        assertFalse(result);
        v8Object.release();
    }

    @Test
    public void testAddObjectWithInt() {
        V8Object v8Object = new V8Object(v8).add("integer", 75);

        v8.add("foo", v8Object);

        int result = v8.executeIntScript("foo.integer");
        assertEquals(75, result);
        v8Object.release();
    }

    @Test
    public void testAddObjectWithDouble() {
        V8Object v8Object = new V8Object(v8).add("double", 75.5);

        v8.add("foo", v8Object);

        double result = v8.executeDoubleScript("foo.double");
        assertEquals(75.5, result, 0.000001);
        v8Object.release();
    }

    @Test
    public void testAddObjectToObject() {
        V8Object nested = new V8Object(v8).add("foo", "bar");
        V8Object v8Object = new V8Object(v8).add("nested", nested);
        v8.add("foo", v8Object);

        String result = v8.executeStringScript("foo.nested.foo");

        assertEquals("bar", result);
        v8Object.release();
        nested.release();
    }

    /*** Add Array ***/
    @Test
    public void testAddArrayToObject() {
        V8Array array = new V8Array(v8);
        V8Object v8Object = new V8Object(v8).add("array", array);
        v8.add("foo", v8Object);

        V8Array result = v8.executeArrayScript("foo.array");

        assertNotNull(result);
        v8Object.release();
        array.release();
        result.release();
    }

    /*** Object Errors ***/
    @Test(expected = V8ResultUndefined.class)
    public void testUndefinedObjectProperty() {
        v8.getObject("object");
    }

    /*** Test Types ***/
    @Test
    public void testGetTypeInt() {
        V8Object v8Object = new V8Object(v8).add("key", 1);

        assertEquals(V8Object.INTEGER, v8Object.getType("key"));
        v8Object.release();
    }

    @Test
    public void testGetTypeDouble() {
        V8Object v8Object = new V8Object(v8).add("key", 1.1);

        assertEquals(V8Object.DOUBLE, v8Object.getType("key"));
        v8Object.release();
    }

    @Test
    public void testGetTypeString() {
        V8Object v8Object = new V8Object(v8).add("key", "String");

        assertEquals(V8Object.STRING, v8Object.getType("key"));
        v8Object.release();
    }

    @Test
    public void testGetTypeBoolean() {
        V8Object v8Object = new V8Object(v8).add("key", false);

        assertEquals(V8Object.BOOLEAN, v8Object.getType("key"));
        v8Object.release();
    }

    @Test
    public void testGetTypeArray() {
        V8Array value = new V8Array(v8);
        V8Object v8Object = new V8Object(v8).add("key", value);

        assertEquals(V8Object.V8_ARRAY, v8Object.getType("key"));
        v8Object.release();
        value.release();
    }

    @Test
    public void testGetTypeObject() {
        V8Object value = new V8Object(v8);
        V8Object v8Object = new V8Object(v8).add("key", value);

        assertEquals(V8Object.V8_OBJECT, v8Object.getType("key"));
        v8Object.release();
        value.release();
    }

    @Test
    public void testGetKeysOnObject() {
        V8Object v8Object =
                new V8Object(v8).add("integer", 1).add("double", 1.1).add("boolean", true)
                .add("string", "hello, world!");

        String[] keys = v8Object.getKeys();

        assertEquals(4, keys.length);
        V8Test.arrayContains(keys, "integer", "double", "boolean", "string");
        v8Object.release();
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetTypeKeyDoesNotExist() {
        V8Object v8Object = new V8Object(v8);

        try {
            v8Object.getType("key");
        } finally {
            v8Object.release();
        }
    }

    @Test
    public void testUnaccessibleMethod() {
        final boolean[] called = new boolean[] { false };
        Runnable r = new Runnable() {

            @Override
            public void run() {
                called[0] = true;
            }
        };
        v8.registerJavaMethod(r, "run", "run", new Class<?>[0]);

        v8.executeVoidFunction("run", null);

        assertTrue(called[0]);
    }

    /*** Manipulate Prototypes ***/
    @Test
    public void testSetPrototypeOfObject() {
        v8.executeVoidScript("function Mammal(){}; Mammal.prototype.breathe=function(){return 'breathe';};");
        V8Object mammal = v8.executeObjectScript("new Mammal();");
        V8Object cat = new V8Object(v8).setPrototype(mammal);

        v8.add("cat", cat);

        assertTrue(v8.executeBooleanScript("cat instanceof Mammal"));
        assertEquals("breathe", cat.executeStringFunction("breathe", null));
        cat.release();
        mammal.release();
    }

    @Test
    public void testChangePrototypeAfterCreation() {
        v8.executeVoidScript("function Mammal(){};");
        V8Object mammal = v8.executeObjectScript("new Mammal();");
        V8Object cat = new V8Object(v8);
        v8.add("cat", cat);
        v8.add("mammal", mammal);
        assertFalse(v8.executeBooleanScript("cat instanceof Mammal"));

        cat.setPrototype(mammal);

        assertTrue(v8.executeBooleanScript("cat instanceof Mammal"));
        assertTrue(v8.executeBooleanScript("cat.__proto__ === mammal"));
        cat.release();
        mammal.release();
    }

    @Test
    public void testChangePrototypePropertiesAfterCreation() {
        v8.executeVoidScript("function Mammal(){};");
        V8Object mammal = v8.executeObjectScript("new Mammal();");
        V8Object cat = new V8Object(v8).setPrototype(mammal);
        v8.add("cat", cat);
        assertFalse(v8.executeBooleanScript("'breathe' in cat"));

        v8.executeVoidScript("Mammal.prototype.breathe=function(){return 'breathe';};");

        assertTrue(v8.executeBooleanScript("'breathe' in cat"));
        cat.release();
        mammal.release();
    }

    /*** Equals ***/
    @Test
    public void testEquals() {
        v8.executeVoidScript("o = {}");
        V8Object o1 = v8.executeObjectScript("o");
        V8Object o2 = v8.executeObjectScript("o");

        assertEquals(o1, o2);
        assertNotSame(o1, o2);

        o1.release();
        o2.release();
    }

    @Test
    public void testEqualsPassByReference() {
        v8.executeVoidScript("o = {}");
        v8.executeVoidScript("function ident(x){return x;}");
        V8Object o1 = v8.executeObjectScript("o");
        V8Array parameters = new V8Array(v8).push(o1);
        V8Object o2 = v8.executeObjectFunction("ident", parameters);

        assertEquals(o1, o2);
        assertNotSame(o1, o2);

        o1.release();
        o2.release();
        parameters.release();
    }

    @Test
    public void testEqualsDifferenceReference() {
        v8.executeVoidScript("a = {}; b=a;");
        v8.executeVoidScript("function ident(x){return x;}");
        V8Object o1 = v8.executeObjectScript("a");
        V8Object o2 = v8.executeObjectScript("b");

        assertEquals(o1, o2);
        assertNotSame(o1, o2);

        o1.release();
        o2.release();
    }

    @Test
    public void testEqualHash() {
        v8.executeVoidScript("o = {}");
        V8Object o1 = v8.executeObjectScript("o");
        V8Object o2 = v8.executeObjectScript("o");

        assertEquals(o1.hashCode(), o2.hashCode());

        o1.release();
        o2.release();
    }

    @Test
    public void testNotEquals() {
        v8.executeVoidScript("a = {}; b = {};");
        V8Object o1 = v8.executeObjectScript("a");
        V8Object o2 = v8.executeObjectScript("b");

        assertNotEquals(o1, o2);

        o1.release();
        o2.release();
    }

    @Test
    public void testNotEqualsNull() {
        v8.executeVoidScript("a = {};");
        V8Object o1 = v8.executeObjectScript("a");

        assertNotEquals(o1, null);

        o1.release();
    }

    @Test
    public void testNotEqualsNull2() {
        v8.executeVoidScript("a = {};");
        V8Object o1 = v8.executeObjectScript("a");

        assertNotEquals(null, o1);

        o1.release();
    }

    @Test
    public void testNotEqualHash() {
        v8.executeVoidScript("a = {}; b = {};");
        V8Object o1 = v8.executeObjectScript("a");
        V8Object o2 = v8.executeObjectScript("b");

        assertNotEquals(o1.hashCode(), o2.hashCode());

        o1.release();
        o2.release();
    }

    @Test
    public void testHashStable() {
        V8Object a = v8.executeObjectScript("a = []; a");
        int hash1 = a.hashCode();
        int hash2 = a.add("1", true).add("2", false).add("3", 123).hashCode();

        assertEquals(hash1, hash2);
        a.release();
    }

}
