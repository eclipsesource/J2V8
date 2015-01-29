package com.eclipsesource.v8;

public class LoggingJavaVoidCallback implements LoggableCallback, JavaVoidCallback {

    private JavaVoidCallback delegate;
    private String           jsFunctionName;

    public LoggingJavaVoidCallback(final JavaVoidCallback delegate, final String jsFunctionName) {
        this.delegate = delegate;
        this.jsFunctionName = jsFunctionName;
    }

    @Override
    public void invoke(final V8Array parameters) {
        log(parameters);
        delegate.invoke(parameters);
    }

    private void log(final V8Array parameters) {
        System.out.println(PREFIX + jsFunctionName + "(" + parameters + ")");
    }

}
