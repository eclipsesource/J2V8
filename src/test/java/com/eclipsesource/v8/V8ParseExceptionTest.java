package com.eclipsesource.v8;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class V8ParseExceptionTest {

    private V8ParseException exception;

    private V8               v8;

    String                   script = "x = [1,2,3];\n"
                                            + "y = 0;\n"
                                            + "\n"
                                            + "//A JS Script that has a compile error, int should be var\n"
                                            + "for (int i = 0; i < x.length; i++) {\n"
                                            + "  y = y + x[i];\n"
                                            + "}";

    @Before
    public void seutp() {
        exception = createParseException();
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
    public void testV8ParseExceptionGetFileName() {
        assertEquals("filename.js", exception.getFileName());
    }

    @Test
    public void testV8ParseExceptionGetLineNumber() {
        assertEquals(4, exception.getLineNumber());
    }

    @Test
    public void testV8ParseExceptionGetMessage() {
        assertEquals("the message", exception.getMessage());
    }

    @Test
    public void testV8ParseExceptionGetSourceLine() {
        assertEquals("line of JS", exception.getSourceLine());
    }

    @Test
    public void testV8ParseExceptionGetStartColumn() {
        assertEquals(4, exception.getStartColumn());
    }

    @Test
    public void testV8ParseExceptionGetEndColumn() {
        assertEquals(6, exception.getEndColumn());
    }

    @Test
    public void testGetSytaxErrorWithNulls() {
        V8ParseException exceptionWithNulls = new V8ParseException(null, 4, null, null, 4, 6);

        assertNotNull(exceptionWithNulls.getSyntaxError());
    }

    @Test
    public void testGetSyntaxError() {
        String result = "filename.js:4: the message\nline of JS\n    ^^";

        assertEquals(result, exception.getSyntaxError());
    }

    @Test
    public void testToString() {
        String result = "filename.js:4: the message\nline of JS\n    ^^\nthe message";

        assertEquals(result, exception.toString());
    }

    @Test
    public void testToStringWithNull() {
        V8ParseException exceptionWithNulls = new V8ParseException(null, 4, null, null, 4, 6);

        assertNotNull(exceptionWithNulls.toString());
    }

    private V8ParseException createParseException() {
        return new V8ParseException("filename.js", 4, "the message", "line of JS", 4, 6);
    }

    @Test
    public void testV8ParseExceptionCreated() {
        try {
            v8.executeVoidScript(script, "file", 0);
        } catch (V8ParseException e) {
            assertEquals("file", e.getFileName());
            assertEquals(5, e.getLineNumber());
            assertEquals("for (int i = 0; i < x.length; i++) {", e.getSourceLine());
            assertEquals(9, e.getStartColumn());
            assertEquals(10, e.getEndColumn());
            assertEquals("SyntaxError: Unexpected identifier", e.getMessage());
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test
    public void testV8ParseExceptionCreatedUndefinedFile() {
        try {
            v8.executeVoidScript(script);
        } catch (V8ParseException e) {
            assertEquals("undefined", e.getFileName());
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test
    public void testV8ParseException() {
        try {
            v8.executeVoidScript("'a");
        } catch (Exception e) {
            assertEquals("SyntaxError: Unexpected token ILLEGAL", e.getMessage());
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test
    public void testV8ParseExceptionUnexpectedEnd() {
        try {
            v8.executeVoidScript("for (i");
        } catch (V8ParseException e) {
            assertEquals("SyntaxError: Unexpected end of input", e.getMessage());
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test(expected = V8ParseException.class)
    public void testParseExceptionForVoidScript() {
        v8.executeVoidScript(script);
    }

    @Test(expected = V8ParseException.class)
    public void testParseExceptionForIntScript() {
        v8.executeIntScript(script);
    }

    @Test(expected = V8ParseException.class)
    public void testParseExceptionForDoubleScript() {
        v8.executeDoubleScript(script);
    }

    @Test(expected = V8ParseException.class)
    public void testParseExceptionForBooleanScript() {
        v8.executeBooleanScript(script);
    }

    @Test(expected = V8ParseException.class)
    public void testParseExceptionForStringScript() {
        v8.executeStringScript(script);
    }

    @Test(expected = V8ParseException.class)
    public void testParseExceptionForObjectScript() {
        v8.executeObjectScript(script);
    }

    @Test(expected = V8ParseException.class)
    public void testParseExceptionForArrayScript() {
        v8.executeArrayScript(script);
    }
}
