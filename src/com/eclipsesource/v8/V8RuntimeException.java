
package com.eclipsesource.v8;

@SuppressWarnings("serial")
public class V8RuntimeException extends RuntimeException {

    public V8RuntimeException(final String message) {
        super(message);
    }

    public V8RuntimeException(final Throwable source) {
        super(source);
    }

}
