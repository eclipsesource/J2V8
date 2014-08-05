package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8ExecutionException;
import com.eclipsesource.v8.V8ResultUndefined;

import static org.junit.Assert.assertEquals;
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

}