package com.eclipsesource.v8;

/**
 * Simple runnable for use with an {@link ConcurrentV8} instance.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public interface ConcurrentV8Runnable {
    void run(final V8 v8) throws Exception;
}
