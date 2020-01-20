package com.eclipsesource.v8.inspector;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
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

        // Default Chrome DevTool protocol messages
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
    public void testEmptyContextNameDoesNotThrow() {
        try {
            V8Inspector.createV8Inspector(spyV8, inspectorDelegate);
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void testDelegateOnResponseCalled() {
        startInspector();

        verify(inspectorDelegate, atLeast(8)).onResponse(any(String.class));
    }

    @Test
    public void testSchedulePauseOnNextStatementCalled() {
        startInspector();

        verify(spyV8).schedulePauseOnNextStatement(any(Long.class), eq(""));
    }

    @Test
    public void testDebuggerConnection() {
        inspector.addDebuggerConnectionListener(new DebuggerConnectionListener() {
            @Override
            public void onDebuggerConnected() {
                spyV8.executeScript("console.log('foo')", "app.js", 0);
                spyV8.executeScript("var bar = 'baz';", "foo.js", 0);
            }

            @Override
            public void onDebuggerDisconnected() {
            }
        });

        startInspector();

        verify(spyV8, atLeast(8)).executeObjectScript(any(String.class));
        verify(spyV8, atLeast(10)).executeScript(any(String.class), nullable(String.class), eq(0));
        verify(spyV8, atLeastOnce()).executeScript(eq("console.log('foo')"), eq("app.js"), eq(0));
        verify(spyV8, atLeastOnce()).executeScript(eq("var bar = 'baz';"), eq("foo.js"), eq(0));
    }

    @Test
    public void textComplexProtocolMessageDoesNotThrow() {
        try {
            inspector.addDebuggerConnectionListener(new DebuggerConnectionListener() {
                @Override
                public void onDebuggerConnected() {
                    String protocolMessage = "{\"method\":\"Debugger.setBreakpointByUrl\",\"params\":{\"lineNumber\":0,\"urlRegex\":\"^[^/\\\\]+-\\d+\\.([jJ][sS])$\"},\"id\":8}";
                    inspector.dispatchProtocolMessage(protocolMessage);
                }

                @Override
                public void onDebuggerDisconnected() {
                }
            });
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }
}
