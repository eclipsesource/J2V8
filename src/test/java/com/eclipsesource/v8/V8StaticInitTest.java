package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8StaticInitTest {
    private V8 v8;

    @Before
    public void setup() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released.");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    static {
        V8.v8flags = "--harmony_proxies --expose_gc_as=testGC";
    }

    @Test
    public void testFlags() {
        // V8.v8flags = "--harmony_proxies --expose_gc_as=testGC";
        v8.executeVoidScript("testGC()");
        assertEquals("object", v8.executeStringScript("typeof Proxy"));
    }
}
