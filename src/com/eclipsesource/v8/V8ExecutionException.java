
package com.eclipsesource.v8;

@SuppressWarnings("serial")
public class V8ExecutionException extends V8RuntimeException {

    public V8ExecutionException(final String message) {
        super(message);
    }

    public V8ExecutionException(final Exception source) {
        super(source);
    }
}
