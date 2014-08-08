package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;

import static org.junit.Assert.assertEquals;

public class V8ArrayTests {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = new V8();
    }

    @After
    public void tearDown() {
        try {
            v8.release();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testCreateAndReleaseArray() {
        for (int i = 0; i < 10000; i++) {
            V8Array v8Array = new V8Array(v8);
            v8Array.release();
        }
    }

    @Test
    public void testArraySize() {
        V8Array array = v8.executeArrayScript("foo = [1,2,3]; foo");

        assertEquals(3, array.getSize());
        array.release();
    }

    @Test
    public void testArraySizeZero() {
        V8Array array = v8.executeArrayScript("foo = []; foo");

        assertEquals(0, array.getSize());
        array.release();
    }

}
