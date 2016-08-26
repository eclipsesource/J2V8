/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8ScriptExecutionExceptionTest {

    private V8ScriptExecutionException exception;
    private String                     errorFunction         = "function myFunction() {\n"
                                                                     + "undefined.toString();\n"
                                                                     + "}\n";
    private String                     undefinedAccessScript = "x=undefined;\n"
                                                                     + "function called( y ) {\n"
                                                                     + " y.toString()\n"
                                                                     + "}\n"
                                                                     + "\n"
                                                                     + "called( x );\n";

    private V8                         v8;

    @Before
    public void seutp() {
        exception = createV8ScriptExecutionException();
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testV8ScriptExecutionExceptionGetFileName() {
        assertEquals("filename.js", exception.getFileName());
    }

    @Test
    public void testV8ScriptExecutionExceptionGetLineNumber() {
        assertEquals(4, exception.getLineNumber());
    }

    @Test
    public void testV8ScriptExecutionExceptionGetMessage() {
        assertEquals("the message", exception.getJSMessage());
    }

    @Test
    public void testV8ScriptExecutionExceptionGetSourceLine() {
        assertEquals("line of JS", exception.getSourceLine());
    }

    @Test
    public void testV8ScriptExecutionExceptionnGetStartColumn() {
        assertEquals(4, exception.getStartColumn());
    }

    @Test
    public void testV8ScriptExecutionExceptionGetEndColumn() {
        assertEquals(6, exception.getEndColumn());
    }

    @Test
    public void testV8ScriptExecutionExceptionGetStacktrace() {
        assertEquals("stack", exception.getJSStackTrace());
    }

    @Test
    public void testV8ScriptExecutionExceptionGetCause() {
        assertNotNull(exception.getCause());
        assertEquals("cause", exception.getCause().getMessage());
    }

    @Test
    public void testToString() {
        String result = "filename.js:4: the message\nline of JS\n    ^^\nstack\ncom.eclipsesource.v8.V8ScriptExecutionException";

        assertEquals(result, exception.toString());
    }

    @Test
    public void testToStringWithNull() {
        V8ScriptExecutionException exceptionWithNulls = new V8ScriptExecutionException(null, 4, null, null, 4, 6, null, null);

        assertNotNull(exceptionWithNulls.toString());
    }

    public void voidCallbackWithException() {
        ((Object) null).toString();
    }

    public Object callbackWithException() {
        ((Object) null).toString();
        return null;
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testExceptionInVoidJavaCall() {
        try {
            v8.registerJavaMethod(this, "voidCallbackWithException", "voidCallback", new Class<?>[] {});
            v8.executeVoidScript("voidCallback()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("Unhandled Java Exception", e.getJSMessage());
            assertTrue(e.getCause() instanceof NullPointerException);
            throw e;
        }
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testExceptionInJavaCall() {
        try {
            v8.registerJavaMethod(this, "callbackWithException", "callback", new Class<?>[] {});
            v8.executeVoidScript("callback()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("Unhandled Java Exception", e.getJSMessage());
            assertTrue(e.getCause() instanceof NullPointerException);
            throw e;
        }
    }

    private V8ScriptExecutionException createV8ScriptExecutionException() {
        return new V8ScriptExecutionException("filename.js", 4, "the message", "line of JS", 4, 6, "stack", new RuntimeException("cause"));
    }

    @Test
    public void testV8ScriptExecutionExceptionCreated() {
        try {
            v8.executeVoidScript(undefinedAccessScript, "file", 0);
        } catch (V8ScriptExecutionException e) {
            assertEquals("file", e.getFileName());
            assertEquals(3, e.getLineNumber());
            assertEquals(" y.toString()", e.getSourceLine());
            assertEquals(2, e.getStartColumn());
            assertEquals(3, e.getEndColumn());
            assertEquals("TypeError: Cannot read property 'toString' of undefined", e.getJSMessage());
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExecutionExceptionIntScript() {
        v8.executeIntegerScript(undefinedAccessScript + "1;", "file", 0);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExecutionExceptionBooleanScript() {
        v8.executeBooleanScript(undefinedAccessScript + "true;", "file", 0);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExecutionExceptionStringScript() {
        v8.executeStringScript(undefinedAccessScript + "'string';", "file", 0);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExecutionExceptionDoubleScript() {
        v8.executeDoubleScript(undefinedAccessScript + "1.1;", "file", 0);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExecutionExceptionArrayScript() {
        v8.executeArrayScript(undefinedAccessScript + "[];", "file", 0);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExecutionExceptionObjectScript() {
        v8.executeObjectScript(undefinedAccessScript + "{};", "file", 0);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExceptionVoidCall() {
        v8.executeVoidScript(errorFunction);

        v8.executeVoidFunction("myFunction", null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExceptionIntCall() {
        v8.executeVoidScript(errorFunction);

        v8.executeIntegerFunction("myFunction", null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExceptionBooleanCall() {
        v8.executeVoidScript(errorFunction);

        v8.executeBooleanFunction("myFunction", null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExceptionDoubleCall() {
        v8.executeVoidScript(errorFunction);

        v8.executeDoubleFunction("myFunction", null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExceptionStringCall() {
        v8.executeVoidScript(errorFunction);

        v8.executeStringFunction("myFunction", null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExceptionObjectCall() {
        v8.executeVoidScript(errorFunction);

        v8.executeObjectFunction("myFunction", null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ScriptExceptionArrayCall() {
        v8.executeVoidScript(errorFunction);

        v8.executeArrayFunction("myFunction", null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testV8ThrowsException() {
        v8.executeVoidScript("throw 'problem';");
    }
}
