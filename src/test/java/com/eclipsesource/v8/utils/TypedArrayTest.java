package com.eclipsesource.v8.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8TypedArray;
import com.eclipsesource.v8.V8Value;

public class TypedArrayTest {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Runtime();
    }

    @After
    public void tearDown() {
        if (v8 != null) {
            v8.close();
        }
        if (V8.getActiveRuntimes() != 0) {
            throw new IllegalStateException("V8Runtimes not properly released");
        }
    }

    @Test
    public void testGetV8TypedArray() {
        TypedArray typedArray = new TypedArray(v8, new ArrayBuffer(v8, ByteBuffer.allocateDirect(8)), V8Value.INT_8_ARRAY, 0, 8);

        V8TypedArray v8TypedArray = typedArray.getV8TypedArray();

        assertNotNull(v8TypedArray);
        v8TypedArray.close();
    }

    @Test
    public void testV8TypedArrayAvailable() {
        TypedArray typedArray = new TypedArray(v8, new ArrayBuffer(v8, ByteBuffer.allocateDirect(8)), V8Value.INT_8_ARRAY, 0, 8);

        assertTrue(typedArray.isAvailable());
    }

}
