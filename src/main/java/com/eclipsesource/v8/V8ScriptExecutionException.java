
package com.eclipsesource.v8;

@SuppressWarnings("serial")
public class V8ScriptExecutionException extends V8ScriptException {

    public V8ScriptExecutionException(final String fileName,
            final int lineNumber,
            final String message,
            final String sourceLine,
            final int startColumn,
            final int endColumn,
            final String jsStackTrace) {
        this(fileName, lineNumber, message, sourceLine, startColumn, endColumn, jsStackTrace, null);
    }

    public V8ScriptExecutionException(final String fileName,
            final int lineNumber,
            final String message,
            final String sourceLine,
            final int startColumn,
            final int endColumn,
            final String jsStackTrace,
            final Throwable cause) {
        super(fileName, lineNumber, message, sourceLine, startColumn, endColumn, jsStackTrace, cause);
    }

}
