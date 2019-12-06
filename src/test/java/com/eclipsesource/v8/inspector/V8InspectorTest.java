package com.eclipsesource.v8.inspector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8ScriptExecutionException;

public class V8InspectorTest {

    V8                  v8;
    V8                  spyV8;
    V8InspectorDelegate inspectorDelegate;
    V8Inspector         inspector;

    @Before
    public void setup() {
        v8 = V8.createV8Runtime();
        spyV8 = spy(v8);
        inspectorDelegate = mock(V8InspectorDelegate.class);
        inspector = V8Inspector.createV8Inspector(spyV8, inspectorDelegate, "test");
    }

    @After
    public void tearDown() {
        try {
            reset(spyV8);
            if (inspector != null) {
                inspector = null;
            }
            if (v8 != null) {
                v8.close();
            }
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private void startInspector() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                // resume Debugger
                inspector.dispatchProtocolMessage("{\"id\":9,\"method\":\"Debugger.resume\"}");
                return null;
            }

        }).when(inspectorDelegate).waitFrontendMessageOnPause();

        // Standart Chrome DevTool protocol messages
        inspector.dispatchProtocolMessage("{\"id\":1,\"method\":\"Profiler.enable\"}");
        inspector.dispatchProtocolMessage("{\"id\":2,\"method\":\"Runtime.enable\"}");
        inspector.dispatchProtocolMessage("{\"id\":3,\"method\":\"Debugger.enable\",\"params\":{\"maxScriptsCacheSize\":10000000}}");
        inspector.dispatchProtocolMessage("{\"id\":4,\"method\":\"Debugger.setPauseOnExceptions\",\"params\":{\"state\":\"uncaught\"}}");
        inspector.dispatchProtocolMessage("{\"id\":5,\"method\":\"Debugger.setAsyncCallStackDepth\",\"params\":{\"maxDepth\":32}}");
        inspector.dispatchProtocolMessage("{\"id\":6,\"method\":\"Runtime.getIsolateId\"}");
        inspector.dispatchProtocolMessage("{\"id\":7,\"method\":\"Debugger.setBlackboxPatterns\",\"params\":{\"patterns\":[]}}");
        inspector.dispatchProtocolMessage("{\"id\":8,\"method\":\"Runtime.runIfWaitingForDebugger\"}");
    }

    @Test
    public void testDelegateOnResponseCalled() {
        inspector.addScript(new V8InspectorScript("console.log('foo')", "app.js"));

        startInspector();

        verify(inspectorDelegate, atLeast(8)).onResponse(any(String.class));
    }

    @Test
    public void testSchedulePauseOnNextStatementCalled() {
        inspector.addScript(new V8InspectorScript("console.log('foo')", "app.js"));

        startInspector();

        verify(spyV8).schedulePauseOnNextStatement(any(Long.class), eq(""));
    }

    @Test
    public void testExecuteScriptCalledFromInspector() {
        inspector.addScript(new V8InspectorScript("console.log('foo')", "app.js"));
        inspector.addScript(new V8InspectorScript("var bar = 'baz';", "foo.js"));

        startInspector();

        verify(spyV8).executeScript("console.log('foo')", "app.js", 0);
        verify(spyV8).executeScript("var bar = 'baz';", "foo.js", 0);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testThrowsErrorOnScriptExecution() {
        inspector.addScript(new V8InspectorScript("var bar = baz;", "foo.js"));

        try {
            startInspector();
        } catch (V8ScriptExecutionException e) {
            assertNotNull(e);
            assertEquals("foo.js", e.getFileName());
            assertEquals(1, e.getLineNumber());
            assertEquals("ReferenceError: baz is not defined", e.getJSMessage());
            throw e;
        }
    }

}
