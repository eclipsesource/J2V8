package com.eclipsesource.v8;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class V8WeakReferenceHandlerTest {
    private V8 v8;

    @Before
    public void setup() {
        V8.setFlags("--expose_gc");
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            v8.close();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    static class WeakReferenceObserver implements WeakReferenceHandler {
        boolean collected = false;
        V8Value ref;

        WeakReferenceObserver(V8Value ref) {
            this.ref = ref;
        }

        @Override
        public void v8WeakReferenceCollected(V8Value weakRef) {
            if (weakRef == ref) {
                collected = true;
            } else {
                throw new IllegalStateException("v8WeakReferenceCollected() called with an unexpected reference");
            }
        }
    }

    @Test
    public void testReferenceCollectedCalled() {
        final V8Object o = v8.executeObjectScript(
                "let a = [];" +
                "a.push({foo: \"bar\"});"+
                "a[0];");

        WeakReferenceObserver observer = new WeakReferenceObserver(o);

        o.setWeak(observer);

        Assert.assertFalse(observer.collected);
        v8.executeVoidScript("a = null");
        v8.executeVoidScript("gc()");
        Assert.assertTrue(observer.collected);
    }

    @Test
    public void testReferenceCollectedNotCalled() {
        final V8Object o = v8.executeObjectScript(
                "let a = [];" +
                "a.push({foo: \"bar\"});"+
                "a[0];");

        WeakReferenceObserver observer = new WeakReferenceObserver(o);

        o.setWeak(observer);

        Assert.assertFalse(observer.collected);
        v8.executeVoidScript("gc()");
        Assert.assertFalse(observer.collected);
    }
}
