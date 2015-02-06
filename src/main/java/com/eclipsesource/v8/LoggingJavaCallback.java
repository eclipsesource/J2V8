package com.eclipsesource.v8;

public class LoggingJavaCallback implements LoggableCallback, JavaCallback {

    private JavaCallback delegate;
    private String       jsFunctionName;

    public LoggingJavaCallback(final JavaCallback delegate, final String jsFunctionName) {
        this.delegate = delegate;
        this.jsFunctionName = jsFunctionName;
    }

    @Override
    public Object invoke(final V8Array parameters) {
        log(parameters);
        return delegate.invoke(parameters);
    }

    private void log(final V8Array parameters) {
        System.out.println(PREFIX + jsFunctionName + "(" + parameters + ")");
    }

}
