/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8ScriptCompilationException;
import com.eclipsesource.v8.V8ScriptException;
import com.eclipsesource.v8.V8ScriptExecutionException;
import com.eclipsesource.v8.utils.V8Executor;

public class V8ExecutorTest {

    private boolean passed = false;

    @Test
    public void testSimpleScript() throws InterruptedException {
        V8Executor executor = new V8Executor("'fooBar'");
        executor.start();
        executor.join();

        assertEquals("fooBar", executor.getResult());
        assertFalse(executor.hasException());
    }

    @Test
    public void testNonStringReturnType() throws InterruptedException {
        V8Executor executor = new V8Executor("3+4");
        executor.start();
        executor.join();

        assertEquals("7", executor.getResult());
    }

    @Test
    public void testNoReturn() throws InterruptedException {
        V8Executor executor = new V8Executor("var x = 7;");
        executor.start();
        executor.join();

        assertEquals("undefined", executor.getResult());
    }

    @Test
    public void testNullReturn() throws InterruptedException {
        V8Executor executor = new V8Executor("null;");
        executor.start();
        executor.join();

        assertNull(executor.getResult());
    }

    public void callback() {
        passed = true;
    }

    @Test
    public void testSetup() throws InterruptedException {
        V8Executor executor = new V8Executor("callback()") {
            @Override
            protected void setup(final V8 runtime) {
                runtime.registerJavaMethod(V8ExecutorTest.this, "callback", "callback", new Class<?>[] {});
            }
        };
        executor.start();
        executor.join();

        assertTrue(passed);
    }

    @Test
    public void testTerminateBeforeExecution() throws InterruptedException {
        V8Executor executor = new V8Executor("'fooBar'");
        executor.terminateExecution();
        executor.start();
        executor.join();

        assertNull(executor.getResult());
    }

    @Test
    public void testTerminateLongRunningThread() throws InterruptedException {
        V8Executor executor = new V8Executor("while(true){}");
        executor.start();
        Thread.sleep(1000);
        executor.terminateExecution();
        executor.join();

        // We should not wait forever
    }

    @Test
    public void testTerminateHasException() throws InterruptedException {
        V8Executor executor = new V8Executor("while(true){}");
        executor.start();
        Thread.sleep(1000);
        executor.terminateExecution();
        executor.join();

        assertTrue(executor.hasException());
    }

    @Test
    public void testTerminateAfterExecution() throws InterruptedException {
        V8Executor executor = new V8Executor("'fooBar'");
        executor.start();
        executor.join();
        executor.terminateExecution();

        assertEquals("fooBar", executor.getResult());
    }

    @Test
    public void testHasException() throws InterruptedException {
        V8Executor executor = new V8Executor("(function() {throw 'foo';})();");
        executor.start();
        executor.join();
        executor.terminateExecution();

        assertNull(executor.getResult());
        assertTrue(executor.hasException());
    }

    @Test
    public void testGetExecutionException() throws InterruptedException {
        V8Executor executor = new V8Executor("(function() {throw 'foo';})();");
        executor.start();
        executor.join();
        executor.terminateExecution();

        assertTrue(executor.getException() instanceof V8ScriptExecutionException);
        assertEquals("foo", ((V8ScriptExecutionException) executor.getException()).getJSMessage());
    }

    @Test
    public void testGetParseException() throws InterruptedException {
        V8Executor executor = new V8Executor("'a");
        executor.start();
        executor.join();
        executor.terminateExecution();

        assertTrue(executor.getException() instanceof V8ScriptCompilationException);
    }

    @Test
    public void testExceptionHasCorrectLineNumber() throws InterruptedException {
        V8Executor executor = new V8Executor("'a");
        executor.start();
        executor.join();
        executor.terminateExecution();

        assertEquals(1, ((V8ScriptException) executor.getException()).getLineNumber());
    }

}
