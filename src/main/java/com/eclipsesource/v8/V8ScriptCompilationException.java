package com.eclipsesource.v8;

@SuppressWarnings("serial")
public class V8ScriptCompilationException extends V8RuntimeException {

    private String fileName;
    private int    lineNumber;
    private String message;
    private String sourceLine;
    private int    startColumn;
    private int    endColumn;

    public V8ScriptCompilationException(final String fileName, final int lineNumber,
            final String message, final String sourceLine, final int startColumn, final int endColumn) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.message = message;
        this.sourceLine = sourceLine;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public String getSourceLine() {
        return sourceLine;
    }

    public String getSyntaxError() {
        StringBuilder result = new StringBuilder();
        result.append(createMessageLine());
        result.append('\n');
        result.append(createMessageDetails());
        return result.toString();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getSyntaxError());
        result.append('\n');
        result.append(getMessage());
        return result.toString();
    }

    @Override
    public String getMessage() {
        return message;
    }

    private String createMessageLine() {
        return fileName + ":" + lineNumber + ": " + message;
    }

    private String createMessageDetails() {
        StringBuilder result = new StringBuilder();
        result.append(sourceLine);
        result.append('\n');
        result.append(createCharSequence(startColumn, ' '));
        result.append(createCharSequence(endColumn - startColumn, '^'));
        return result.toString();
    }

    private char[] createCharSequence(final int length, final char c) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = c;
        }
        return result;
    }
}
