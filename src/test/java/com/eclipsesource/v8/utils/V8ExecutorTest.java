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
package com.eclipsesource.v8.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ScriptCompilationException;
import com.eclipsesource.v8.V8ScriptException;
import com.eclipsesource.v8.V8ScriptExecutionException;

public class V8ExecutorTest {

    private boolean passed = false;
    private String  result = "";

    @After
    public void tearDown() {
        if (V8.getActiveRuntimes() != 0) {
            throw new IllegalStateException("V8Runtimes not properly released");
        }
    }

    @Test
    public void testNestedExecutorExecution() {
        V8 runtime = V8.createV8Runtime();
        V8Executor executor = new V8Executor("");
        executor.start();
        V8Object key = new V8Object(runtime);
        runtime.registerV8Executor(key, executor);
        key.release();
        runtime.release();
    }

    @Test
    public void testGetNestedExecutor() {
        V8 runtime = V8.createV8Runtime();
        V8Executor executor = new V8Executor("");
        V8Object key = new V8Object(runtime);

        runtime.registerV8Executor(key, executor);

        assertEquals(executor, runtime.getExecutor(key));
        key.release();
        runtime.release();
    }

    @Test
    public void testGetMissingExecutor() {
        V8 runtime = V8.createV8Runtime();
        V8Executor executor = new V8Executor("");
        V8Object key = new V8Object(runtime);
        runtime.registerV8Executor(key, executor);
        V8Object anotherKey = new V8Object(runtime);

        V8Executor result = runtime.getExecutor(anotherKey);

        assertNull(result);
        key.release();
        anotherKey.release();
        runtime.release();
    }

    @Test
    public void testRemoveNestedExecutor() {
        V8 runtime = V8.createV8Runtime();
        V8Executor executor = new V8Executor("");
        V8Object key = new V8Object(runtime);
        runtime.registerV8Executor(key, executor);

        V8Executor result = runtime.removeExecutor(key);

        assertEquals(executor, result);
        assertNull(runtime.getExecutor(key));
        key.release();
        runtime.release();
    }

    @Test
    public void testTerminateNestedExecutors() {
        V8 runtime = V8.createV8Runtime();
        V8Executor executor = new V8Executor("while (true){}");
        V8Object key = new V8Object(runtime);
        runtime.registerV8Executor(key, executor);

        runtime.shutdownExecutors(false);

        assertTrue(runtime.getExecutor(key).isShuttingDown());
        key.release();
        runtime.release();
    }

    @Test
    public void testForceTerminateNestedExecutors() {
        V8 runtime = V8.createV8Runtime();
        V8Executor executor = new V8Executor("while (true){}");
        executor.start();
        V8Object key = new V8Object(runtime);
        runtime.registerV8Executor(key, executor);

        runtime.shutdownExecutors(true);

        assertTrue(runtime.getExecutor(key).isShuttingDown());
        key.release();
        runtime.release();
    }

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
        executor.forceTermination();
        executor.start();
        executor.join();

        assertNull(executor.getResult());
    }

    @Test
    public void testTerminateLongRunningThread() throws InterruptedException {
        V8Executor executor = new V8Executor("while(true){}");
        executor.start();
        Thread.sleep(1000);
        executor.forceTermination();
        executor.join();

        // We should not wait forever
    }

    @Test
    public void testTerminateHasException() throws InterruptedException {
        V8Executor executor = new V8Executor("while(true){}");
        executor.start();
        Thread.sleep(1000);
        executor.forceTermination();
        executor.join();

        assertTrue(executor.hasException());
    }

    @Test
    public void testTerminateAfterExecution() throws InterruptedException {
        V8Executor executor = new V8Executor("'fooBar'");
        executor.start();
        executor.join();
        executor.forceTermination();

        assertEquals("fooBar", executor.getResult());
    }

    @Test
    public void testHasException() throws InterruptedException {
        V8Executor executor = new V8Executor("(function() {throw 'foo';})();");
        executor.start();
        executor.join();
        executor.forceTermination();

        assertNull(executor.getResult());
        assertTrue(executor.hasException());
    }

    @Test
    public void testGetExecutionException() throws InterruptedException {
        V8Executor executor = new V8Executor("(function() {throw 'foo';})();");
        executor.start();
        executor.join();
        executor.forceTermination();

        assertTrue(executor.getException() instanceof V8ScriptExecutionException);
        assertEquals("foo", ((V8ScriptExecutionException) executor.getException()).getJSMessage());
    }

    @Test
    public void testGetParseException() throws InterruptedException {
        V8Executor executor = new V8Executor("'a");
        executor.start();
        executor.join();
        executor.forceTermination();

        assertTrue(executor.getException() instanceof V8ScriptCompilationException);
    }

    @Test
    public void testExceptionHasCorrectLineNumber() throws InterruptedException {
        V8Executor executor = new V8Executor("'a");
        executor.start();
        executor.join();
        executor.forceTermination();

        assertEquals(1, ((V8ScriptException) executor.getException()).getLineNumber());
    }

    @Test
    public void testLongRunningExecutorWithMessageHandler() throws InterruptedException {
        V8Executor executor = new V8Executor("messageHandler = function(e) { postMessage(e); }", true, "messageHandler") {
            @Override
            protected void setup(final V8 runtime) {
                runtime.registerJavaMethod(V8ExecutorTest.this, "postMessage", "postMessage", new Class<?>[] { Object[].class });
            }
        };
        executor.start();
        executor.postMessage("");
        Thread.sleep(500);
        executor.forceTermination();
        executor.join();

        assertTrue(passed);
    }

    @Test
    public void testPostEmptyMessageToLongRunningTask() throws InterruptedException {
        V8Executor executor = new V8Executor("messageHandler = function(e) { postMessage(e); }", true, "messageHandler") {
            @Override
            protected void setup(final V8 runtime) {
                runtime.registerJavaMethod(V8ExecutorTest.this, "postMessage", "postMessage", new Class<?>[] { Object[].class });
            }
        };
        executor.start();
        executor.postMessage();
        Thread.sleep(500);
        executor.forceTermination();
        executor.join();

        assertEquals("", result);
    }

    @Test
    public void testLongRunningExecutorWithMultiPartMessage() throws InterruptedException {
        V8Executor executor = new V8Executor("messageHandler = function(e) { postMessage(e[0], e[1]); }", true, "messageHandler") {
            @Override
            protected void setup(final V8 runtime) {
                runtime.registerJavaMethod(V8ExecutorTest.this, "postMessage", "postMessage", new Class<?>[] { Object[].class });
            }
        };
        executor.start();
        executor.postMessage("1", "3");
        Thread.sleep(500);
        executor.forceTermination();
        executor.join();

        assertEquals("13", result);
    }

    @Test
    public void testLongRunningExecutorWithSeveralMessages() throws InterruptedException {
        V8Executor executor = new V8Executor("messageHandler = function(e) { postMessage(e); }", true, "messageHandler") {
            @Override
            protected void setup(final V8 runtime) {
                runtime.registerJavaMethod(V8ExecutorTest.this, "postMessage", "postMessage", new Class<?>[] { Object[].class });
            }
        };
        executor.start();
        executor.postMessage("1");
        executor.postMessage("2");
        executor.postMessage("3");
        Thread.sleep(500);
        executor.forceTermination();
        executor.join();

        assertEquals("123", result);
    }

    @Test
    public void testLongRunningExecutorWithNoMessage() throws InterruptedException {
        V8Executor executor = new V8Executor("messageHandler = function(e) { postMessage(e); }", true, "messageHandler") {
            @Override
            protected void setup(final V8 runtime) {
                runtime.registerJavaMethod(V8ExecutorTest.this, "postMessage", "postMessage", new Class<?>[] { Object[].class });
            }
        };
        executor.start();
        Thread.sleep(500);
        executor.forceTermination();
        executor.join();

        assertFalse(passed);
    }

    @Test
    public void testShutdownDoesNotTerminateLongRunningTask() throws InterruptedException {
        V8Executor executor = new V8Executor("while(true)", true, "messageHandler");
        executor.shutdown();
        Thread.sleep(1000);
        assertFalse(executor.hasTerminated());
        executor.forceTermination();
    }

    @Test
    public void testIsTerminating() {
        V8Executor executor = new V8Executor("");

        executor.forceTermination();

        assertTrue(executor.isTerminating());
    }

    @Test
    public void testIsNotTerminating() {
        V8Executor executor = new V8Executor("");

        assertFalse(executor.isTerminating());
        assertFalse(executor.isShuttingDown());
    }

    public void postMessage(final Object... s) {
        passed = true;
        for (Object element : s) {
            result = result + element;
        }
    }

}
