/*******************************************************************************
 * Copyright (c) 2016 Alicorn Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alicorn Systems - initial API and implementation and/or initial documentation
 ******************************************************************************/
package com.eclipsesource.v8;

/**
 * Wrapper class for an {@link com.eclipsesource.v8.V8} instance that allows
 * a V8 instance to be invoked from across threads without explicitly acquiring
 * or releasing locks.
 *
 * This class does not guarantee the safety of any objects stored in or accessed
 * from the wrapped V8 instance; it only enables callers to interact with a V8
 * instance from any thread. The V8 instance represented by this class should
 * still be treated with thread safety in mind
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public final class ConcurrentV8 {
//Private//////////////////////////////////////////////////////////////////////

    // Wrapped V8 instance, initialized by the default runnable.
    private V8 v8 = null;

//Protected////////////////////////////////////////////////////////////////////

    // Release the V8 runtime when this class is finalized.
    @Override protected void finalize() {
        release();
    }

//Public///////////////////////////////////////////////////////////////////////

    public ConcurrentV8() {
        v8 = V8.createV8Runtime();
        v8.getLocker().release();
    }

    /**
     * Calls {@link #run(ConcurrentV8Runnable)}, quietly handling any thrown
     * exceptions.
     *
     * @see {@link #run(ConcurrentV8Runnable)}.
     */
    public void runQuietly(ConcurrentV8Runnable runny) {
        try {
            run(runny);
        } catch (Throwable t) { }
    }

    /**
     * Runs an {@link ConcurrentV8Runnable} on the V8 thread.
     *
     * <b>Note: </b> This method executes synchronously, not asynchronously;
     * it will not return until the passed {@link ConcurrentV8Runnable} is done
     * executing.
     *
     * @param runny {@link ConcurrentV8Runnable} to run.
     *
     * @throws Exception If the passed runnable throws an exception, this
     *         method will throw that exact exception.
     */
    public synchronized void run(ConcurrentV8Runnable runny) throws Exception {
        try {
            v8.getLocker().acquire();

            try {
                runny.run(v8);
            } catch (Throwable t) {
                v8.getLocker().release();
                if (t instanceof Exception) {
                    throw (Exception) t;
                } else {
                    throw new Exception(t);
                }
            }

            v8.getLocker().release();
        } catch (Throwable t) {
            if (v8 != null && v8.getLocker() != null && v8.getLocker().hasLock()) {
                v8.getLocker().release();
            }

            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new Exception(t);
            }
        }
    }

    /**
     * Releases the underlying {@link V8} instance.
     *
     * This method should be invoked once you're done using this object,
     * otherwise a large amount of garbage could be left on the JVM due to
     * native resources.
     *
     * <b>Note:</b> If this method has already been called once, it
     * will do nothing.
     */
    public void release() {
        if (v8 != null && !v8.isReleased()) {
            // Release the V8 instance from the V8 thread context.
            runQuietly(new ConcurrentV8Runnable() {
                @Override
                public void run(V8 v8) {
                    if (v8 != null && !v8.isReleased()) {
                        v8.release();
                    }
                }
            });
        }
    }
}
