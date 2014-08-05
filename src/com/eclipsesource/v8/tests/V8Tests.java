package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8ExecutionException;
import com.eclipsesource.v8.V8ResultUndefined;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class V8Tests {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Isolate();
    }

    @After
    public void tearDown() {
        v8.release();
    }

    @Test
    public void testV8Setup() {
        assertNotNull(v8);
    }

    @Test(expected = Error.class)
    public void testCannotAccessDisposedIsolate() {
        v8.release();
        v8.executeVoidScript("");
    }

    @Test
    public void testSingleThreadAccess() throws InterruptedException {
        final boolean[] result = new boolean[] { false };
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    v8.executeVoidScript("");
                } catch (Error e) {
                    result[0] = e.getMessage().contains("Invalid V8 thread access.");
                }
            }
        });
        t.start();
        t.join();

        assertTrue(result[0]);
    }

    /*** Void Script ***/
    @Test
    public void testSimpleVoidScript() {
        v8.executeVoidScript("function foo() {return 1+1}");

        int result = v8.executeIntFunction("foo", null);

        assertEquals(2, result);
    }

    @Test
    public void testMultipleScriptCallsPermitted() {
        v8.executeVoidScript("function foo() {return 1+1}");
        v8.executeVoidScript("function bar() {return foo() + 1}");

        int foo = v8.executeIntFunction("foo", null);
        int bar = v8.executeIntFunction("bar", null);

        assertEquals(2, foo);
        assertEquals(3, bar);
    }

    @Test(expected = V8ExecutionException.class)
    public void testSyntaxErrorInVoidScript() {
        v8.executeVoidScript("'a");
    }

    /*** Int Script ***/
    @Test
    public void testSimpleIntScript() {
        int result = v8.executeIntScript("1+2;");

        assertEquals(3, result);
    }

    @Test(expected = V8ExecutionException.class)
    public void testSimpleSyntaxError() {
        v8.executeIntScript("return 1+2");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionIntScript() {
        v8.executeIntScript("");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionForWrongReturnTypeIntScript() {
        v8.executeIntScript("'test'");
    }

    /*** Double Script ***/
    @Test
    public void testSimpleDoubleScript() {
        double result = v8.executeDoubleScript("3.14159;");

        assertEquals(3.14159, result, 0.00001);
    }

    @Test(expected = V8ExecutionException.class)
    public void testSimpleSyntaxErrorInDoubleScript() {
        v8.executeDoubleScript("return 1+2");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionDoubleScript() {
        v8.executeDoubleScript("");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionForWrongReturnTypeDoubleScript() {
        v8.executeDoubleScript("'test'");
    }

    public void testDoubleScriptHandlesInts() {
        int result = (int) v8.executeDoubleScript("1");

        assertEquals(1, result);
    }

    /*** Boolean Script ***/
    @Test
    public void testSimpleBooleanScript() {
        boolean result = v8.executeBooleanScript("true");

        assertTrue(result);
    }

    @Test(expected = V8ExecutionException.class)
    public void testSimpleSyntaxErrorInBooleanScript() {
        v8.executeBooleanScript("return 1+2");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionBooleanScript() {
        v8.executeBooleanScript("");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionForWrongReturnTypeBooleanScript() {
        v8.executeBooleanScript("'test'");
    }

    /*** String Script ***/
    @Test
    public void testSimpleStringScript() {
        String result = v8.executeStringScript("'hello, world'");

        assertEquals("hello, world", result);
    }

    @Test(expected = V8ExecutionException.class)
    public void testSimpleSyntaxErrorStringScript() {
        v8.executeStringScript("'a");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionStringScript() {
        v8.executeIntScript("");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionForWrongReturnTypeStringScript() {
        v8.executeStringScript("42");
    }

    /*** Int Function ***/
    @Test
    public void testSimpleIntFunction() {
        v8.executeIntScript("function foo() {return 1+2;}; 42");

        int result = v8.executeIntFunction("foo", null);

        assertEquals(3, result);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedForWrongReturnTypeOfIntFunction() {
        v8.executeIntScript("function foo() {return 'test';}; 42");

        int result = v8.executeIntFunction("foo", null);

        assertEquals(3, result);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedForNoReturnInIntFunction() {
        v8.executeIntScript("function foo() {}; 42");

        int result = v8.executeIntFunction("foo", null);

        assertEquals(3, result);
    }

    /*** String Function ***/
    @Test
    public void testSimpleStringFunction() {
        v8.executeVoidScript("function foo() {return 'hello';}");

        String result = v8.executeStringFunction("foo", null);

        assertEquals("hello", result);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedForWrongReturnTypeOfStringFunction() {
        v8.executeVoidScript("function foo() {return 42;}");

        v8.executeStringFunction("foo", null);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedForNoReturnInStringFunction() {
        v8.executeVoidScript("function foo() {};");

        v8.executeStringFunction("foo", null);
    }

    /*** Add Int ***/
    @Test
    public void testAddInt() {
        v8.add("foo", 42);

        int result = v8.executeIntScript("foo");

        assertEquals(42, result);
    }

    @Test
    public void testAddIntReplaceValue() {
        v8.add("foo", 42);
        v8.add("foo", 43);

        int result = v8.executeIntScript("foo");

        assertEquals(43, result);
    }

    /*** Add Double ***/
    @Test
    public void testAddDouble() {
        v8.add("foo", 3.14159);

        double result = v8.executeDoubleScript("foo");

        assertEquals(3.14159, result, 0.000001);
    }

    @Test
    public void testAddDoubleReplaceValue() {
        v8.add("foo", 42.1);
        v8.add("foo", 43.1);

        double result = v8.executeDoubleScript("foo");

        assertEquals(43.1, result, 0.000001);
    }

    /*** Add String ***/
    @Test
    public void testAddString() {
        v8.add("foo", "hello, world!");

        String result = v8.executeStringScript("foo");

        assertEquals("hello, world!", result);
    }

    @Test
    public void testAddStringReplaceValue() {
        v8.add("foo", "hello");
        v8.add("foo", "world");

        String result = v8.executeStringScript("foo");

        assertEquals("world", result);
    }

    /*** Add Boolean ***/
    @Test
    public void testBooleanString() {
        v8.add("foo", true);

        boolean result = v8.executeBooleanScript("foo");

        assertTrue(result);
    }

    @Test
    public void testAddBooleanReplaceValue() {
        v8.add("foo", true);
        v8.add("foo", false);

        boolean result = v8.executeBooleanScript("foo");

        assertFalse(result);
    }

    @Test
    public void testAddReplaceValue() {
        v8.add("foo", true);
        v8.add("foo", "test");

        String result = v8.executeStringScript("foo");

        assertEquals("test", result);
    }

    /*** Get Int ***/
    @Test
    public void testGetInt() {
        v8.executeVoidScript("x = 7");

        int result = v8.getInteger("x");

        assertEquals(7, result);
    }

    @Test
    public void testGetIntReplaceValue() {
        v8.executeVoidScript("x = 7; x = 8");

        int result = v8.getInteger("x");

        assertEquals(8, result);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetIntWrongType() {
        v8.executeVoidScript("x = 'foo'");

        v8.getInteger("x");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetIntDoesNotExist() {
        v8.executeVoidScript("");

        v8.getInteger("x");
    }

    /*** Get Double ***/
    @Test
    public void testGetDouble() {
        v8.executeVoidScript("x = 3.14159");

        double result = v8.getDouble("x");

        assertEquals(3.14159, result, 0.00001);
    }

    @Test
    public void testGetDoubleReplaceValue() {
        v8.executeVoidScript("x = 7.1; x = 8.1");

        double result = v8.getDouble("x");

        assertEquals(8.1, result, 0.00001);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetDoubleWrongType() {
        v8.executeVoidScript("x = 'foo'");

        v8.getDouble("x");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetDoubleDoesNotExist() {
        v8.executeVoidScript("");

        v8.getDouble("x");
    }

    /*** Get String ***/
    @Test
    public void testGetString() {
        v8.executeVoidScript("x = 'hello'");

        String result = v8.getString("x");

        assertEquals("hello", result);
    }

    @Test
    public void testGetStringReplaceValue() {
        v8.executeVoidScript("x = 'hello'; x = 'world'");

        String result = v8.getString("x");

        assertEquals("world", result);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetStringeWrongType() {
        v8.executeVoidScript("x = 42");

        v8.getString("x");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetStringDoesNotExist() {
        v8.executeVoidScript("");

        v8.getString("x");
    }

    /*** Get Boolean ***/
    @Test
    public void testGetBoolean() {
        v8.executeVoidScript("x = true");

        boolean result = v8.getBoolean("x");

        assertTrue(result);
    }

    @Test
    public void testGetBooleanReplaceValue() {
        v8.executeVoidScript("x = true; x = false");

        boolean result = v8.getBoolean("x");

        assertFalse(result);
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetBooleanWrongType() {
        v8.executeVoidScript("x = 42");

        v8.getBoolean("x");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testGetBooleanDoesNotExist() {
        v8.executeVoidScript("");

        v8.getBoolean("x");
    }
    @Test
    public void testAddGet() {
        v8.add("string", "string");
        v8.add("int", 7);
        v8.add("double", 3.1);
        v8.add("boolean", true);

        assertEquals("string", v8.getString("string"));
        assertEquals(7, v8.getInteger("int"));
        assertEquals(3.1, v8.getDouble("double"), 0.00001);
        assertTrue(v8.getBoolean("boolean"));
    }

    /*** Contains ***/
    @Test
    public void testContainsKey() {
        v8.add("foo", true);

        boolean result = v8.contains("foo");

        assertTrue(result);
    }

    @Test
    public void testContainsKeyFromScript() {
        v8.executeVoidScript("bar = 3");

        assertTrue(v8.contains("bar"));
    }

    @Test
    public void testContainsMultipleKeys() {
        v8.add("true", true);
        v8.add("test", "test");
        v8.add("one", 1);
        v8.add("pi", 3.14);

        assertTrue(v8.contains("true"));
        assertTrue(v8.contains("test"));
        assertTrue(v8.contains("one"));
        assertTrue(v8.contains("pi"));
        assertFalse(v8.contains("bar"));
    }

    @Test
    public void testDoesNotContainsKey() {
        v8.add("foo", true);

        boolean result = v8.contains("bar");

        assertFalse(result);
    }

}