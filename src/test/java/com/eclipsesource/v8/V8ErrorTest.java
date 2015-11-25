package com.eclipsesource.v8;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class V8ErrorTest {

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

    public interface ICallback {

        public void callback(V8Object context);

        public void objectCallback(V8Object context);

    }

    @Test
    public void testThrowMyCustomError() {
        v8.executeScript("var myError = new Error();");
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                V8Object context = (V8Object) args[0];
                final V8Object myError = context.getObject("myError");

                myError.throwException();
                return null;
            }
        }).when(callback).callback(any(V8Object.class));
        v8.registerJavaMethod(callback, "callback", "foo", new Class<?>[] { V8Object.class }, true);

        boolean result = v8.executeBooleanScript("\n"
                + "var result = false;"
                + "try {\n"
                + "  foo();\n"
                + "} catch (e) {\n"
                + "  result = myError === e;\n"
                + "}\n"
                + "result;");

        assertTrue(result);
    }

    @Test
    public void testThrowV8ErrorTypeObject() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                V8Object exception = new V8Object(v8);
                try {
                    throw new V8Error(exception);
                } finally {
                    exception.release();
                }
            }
        }).when(callback).callback(any(V8Object.class));
        v8.registerJavaMethod(callback, "callback", "foo", new Class<?>[] { V8Object.class }, true);

        boolean result = v8.executeBooleanScript("\n"
                + "var result = false;"
                + "try {\n"
                + "  foo();\n"
                + "} catch (e) {\n"
                + "  result = typeof e === 'object'\n"
                + "}\n"
                + "result;");

        assertTrue(result);
    }

    @Test
    public void testThrowV8ErrorIsMyError() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                V8Object exception = new V8Object(v8); // Get MyError
                exception.add("MyObject", "IsMyError");
                try {
                    throw new V8Error(exception);
                } finally {
                    exception.release();
                }
            }
        }).when(callback).callback(any(V8Object.class));
        v8.registerJavaMethod(callback, "callback", "foo", new Class<?>[] { V8Object.class }, true);

        boolean result = v8.executeBooleanScript("\n"
                + "var result = false;"
                + "try {\n"
                + "  foo();\n"
                + "} catch (e) {\n"
                + "  result = e.MyObject === 'IsMyError'\n"
                + "}\n"
                + "result;");

        assertTrue(result);
    }

    @Test
    public void testObjectCallbackThrowV8ErrorIsMyError() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                V8Object exception = new V8Object(v8); // Get MyError
                exception.add("MyObject", "IsMyError");
                try {
                    throw new V8Error(exception);
                } finally {
                    exception.release();
                }
            }
        }).when(callback).objectCallback(any(V8Object.class));
        v8.registerJavaMethod(callback, "objectCallback", "foo", new Class<?>[] { V8Object.class }, true);

        boolean result = v8.executeBooleanScript("\n"
                + "var result = false;"
                + "try {\n"
                + "  foo();\n"
                + "} catch (e) {\n"
                + "  result = e.MyObject === 'IsMyError'\n"
                + "}\n"
                + "result;");

        assertTrue(result);
    }

}
