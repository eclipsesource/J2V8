package com.eclipsesource.v8;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class V8ParseExceptionTest {

    private V8ParseException exception;

    @Before
    public void setup() {
        exception = createParseException();
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
    public void testV8ParseExceptionGetExceptionMessage() {
        assertEquals("the message", exception.getExceptionMessage());
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
    public void testGetMessageWithNulls() {
        V8ParseException exceptionWithNulls = new V8ParseException(null, 4, null, null, 4, 6);

        assertNotNull(exceptionWithNulls.getMessage());
    }

    @Test
    public void testGetMessage() {
        String result = "filename.js:4: the message\nline of JS\n    ^^";

        assertEquals(result, exception.getMessage());
    }

    @Test
    public void testToString() {
        String result = "filename.js:4: the message\nline of JS\n    ^^";

        assertEquals(result, exception.toString());
    }

    private V8ParseException createParseException() {
        return new V8ParseException("filename.js", 4, "the message", "line of JS", 4, 6);
    }
}
