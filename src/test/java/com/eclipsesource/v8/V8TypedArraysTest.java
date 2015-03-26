package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class V8TypedArraysTest {

    private V8 v8;

    @Before
    public void seutp() {
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

    @Test
    public void testArrayBuffer() {
        V8Value result = (V8Value) v8.executeScript("var buf = new ArrayBuffer(100);  buf;");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testInt8Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Int8Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testInt16Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Int16Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testUint8Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Uint8Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testUint16Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Uint16Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testInt32Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Int32Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testUInt32Array() {
        V8Value result = (V8Value) v8.executeScript("var ints = new Uint32Array(); ints");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testFloat32Array() {
        V8Value result = (V8Value) v8.executeScript("var floats = new Float32Array(); floats");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testFloat64Array() {
        V8Value result = (V8Value) v8.executeScript("var floats = new Float64Array(); floats");

        assertNotNull(result);
        result.release();
    }

    @Test
    public void testIntArrayLength() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var ints = new Int32Array(buf);\n");
        int arrayLength = v8.executeIntScript("ints.length;"); // 4 bytes for each element

        assertEquals(25, arrayLength);
    }

    @Test
    public void testIntArrayByteLength() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var ints = new Int32Array(buf);\n");
        int arrayLength = v8.executeIntScript("ints.byteLength;"); // 4 bytes for each element

        assertEquals(100, arrayLength);
    }
}
