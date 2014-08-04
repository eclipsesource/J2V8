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
    public void testResultUndefinedException() {
        v8.executeIntScript("");
    }

    @Test(expected = V8ResultUndefined.class)
    public void testResultUndefinedExceptionForWrongReturnType() {
        v8.executeIntScript("'test'");
    }

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

}