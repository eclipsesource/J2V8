package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
    public void testTypedArrayLength() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int32Array(buf); ints[0] = 7; ints");

        assertEquals(1, result.length());
        result.release();
    }

    @Test
    public void testGetTypedArrayValue() {
        int result = v8.executeIntegerScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints[0]");

        assertEquals(7, result);
    }

    @Test
    public void testGetTypedArrayIntValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(7, result.get(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayUsingKeys() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(7, result.getInteger("0"));
        result.release();
    }

    @Test
    public void testGetTypedArrayIntType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(V8Value.INTEGER, result.getType(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayFloatType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var floats = new Float32Array(buf); floats[0] = 7.7; floats");

        assertEquals(V8Value.DOUBLE, result.getType(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayIntArrayType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 7; ints");

        assertEquals(V8Value.INTEGER, result.getType());
        result.release();
    }

    @Test
    public void testGetTypedArrayUInt8Type() {
        v8.registerJavaMethod(new JavaVoidCallback() {

            @Override
            public void invoke(final V8Object receiver, final V8Array parameters) {
                assertEquals(V8Value.V8_TYPED_ARRAY, parameters.getType(0));
            }
        }, "javaMethod");
        v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Uint8ClampedArray(buf); ints[0] = 7; javaMethod(ints);");
    }

    @Test
    public void testGetTypedArrayFloatArrayType() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var floats = new Float32Array(buf); floats[0] = 7.7; floats[1] = 7; floats");

        assertEquals(V8Value.DOUBLE, result.getType());
        result.release();
    }

    @Test
    public void testGetTypedArrayType32BitValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int32Array(buf); ints[0] = 255; ints");

        assertEquals(255, result.get(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayType16BitValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var ints = new Int16Array(buf); ints[0] = 255; ints");

        assertEquals(255, result.get(0));
        result.release();
    }

    @Test
    public void testGetTypedArrayType32BitFloatValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(4); var floats = new Float32Array(buf); floats[0] = 255.5; floats");

        assertEquals(255.5, result.getDouble(0), 0.00001);
        result.release();
    }

    @Test
    public void testGetTypedArrayType64BitFloatValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var floats = new Float64Array(buf); floats[0] = 255.5; floats");

        assertEquals(255.5, result.getDouble(0), 0.00001);
        result.release();
    }

    @Test
    public void testGetTypedRangeArrayValue() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(100); var ints = new Int32Array(buf); for(var i = 0; i < 25; i++) {ints[i] = i;}; ints");

        assertEquals(25, result.length());
        int[] ints = result.getIntegers(0, 25);
        for (int i = 0; i < ints.length; i++) {
            assertEquals(i, ints[i]);
        }
        result.release();
    }

    @Test
    public void testGetTypedArrayGetKeys() {
        V8Array result = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints[0] = 255; ints[1] = 17; ints");

        assertEquals(2, result.getKeys().length);
        assertEquals("0", result.getKeys()[0]);
        assertEquals("1", result.getKeys()[1]);
        result.release();
    }

    @Test
    public void testAddTypedArrayInteger() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        array.add("0", 7);
        array.add("1", 17);

        assertEquals(2, array.length());
        assertEquals(7, array.getInteger(0));
        assertEquals(17, array.getInteger(1));
        array.release();
    }

    @Test
    public void testAddTypedArrayFloat() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var floats = new Float32Array(buf); floats");

        array.add("0", 7.7);
        array.add("1", 17.7);

        assertEquals(2, array.length());
        assertEquals(7.7, array.getDouble(0), 0.000001);
        assertEquals(17.7, array.getDouble(1), 0.000001);
        array.release();
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushIntToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushIntToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushFloatToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7.7);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushFloatToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(7.7);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushBooleanToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(true);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushBooleanToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push(true);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushStringToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push("foo");
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushStringToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push("foo");
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushUndefinedToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.pushUndefined();
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushUndefinedToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.pushUndefined();
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushNullToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push((V8Object) null);
        } finally {
            array.release();
        }
    }

    @Test
    public void testCannotPushNullToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");

        try {
            array.push((V8Object) null);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushV8ObjectToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Object obj = new V8Object(v8);

        try {
            array.push(obj);
        } finally {
            array.release();
            obj.release();
        }
    }

    @Test
    public void testCannotPushV8ObjectToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Object obj = new V8Object(v8);

        try {
            array.push(obj);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
            obj.release();
        }
        fail("Expected failure");
    }

    @Test(expected = V8RuntimeException.class)
    public void testCannotPushV8ArrayToTypedArray() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Array obj = new V8Array(v8);

        try {
            array.push(obj);
        } finally {
            array.release();
            obj.release();
        }
    }

    @Test
    public void testCannotPushV8ArrayToTypedArray_CheckMessage() {
        V8Array array = (V8Array) v8.executeScript("var buf = new ArrayBuffer(8); var ints = new Int32Array(buf); ints");
        V8Array obj = new V8Array(v8);

        try {
            array.push(obj);
        } catch (Exception e) {
            assertEquals("Cannot push to a Typed Array.", e.getMessage());
            return;
        } finally {
            array.release();
            obj.release();
        }
        fail("Expected failure");
    }

    @Test
    public void testIntArrayLength() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var ints = new Int32Array(buf);\n");
        int arrayLength = v8.executeIntegerScript("ints.length;"); // 4 bytes for each element

        assertEquals(25, arrayLength);
    }

    @Test
    public void testIntArrayByteLength() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var ints = new Int32Array(buf);\n");
        int arrayLength = v8.executeIntegerScript("ints.byteLength;"); // 4 bytes for each element

        assertEquals(100, arrayLength);
    }

    @Test
    public void testGetTypedArray() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n");

        int type = v8.getType("intsArray");

        assertEquals(V8Value.V8_TYPED_ARRAY, type);
    }

    @Test
    public void testGetTypedArray_IntegerType() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }

    @Test
    public void testGetTypedArray_DoubleType() {
        v8.executeVoidScript("var buf = new ArrayBuffer(80);\n"
                + "var doublesArray = new Float64Array(buf);");

        V8Array doublesArray = (V8Array) v8.get("doublesArray");

        assertEquals(V8Value.DOUBLE, doublesArray.getType());
        doublesArray.release();
    }

    @Test
    public void testGetTypedArray_IntegerTypeAfterNull() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = null;\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }

    @Test
    public void testGetTypedArray_IntegerTypeAfterUndefined() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = undefined;\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }

    @Test
    public void testGetTypedArray_IntegerTypeAfterFloat() {
        v8.executeVoidScript("var buf = new ArrayBuffer(100);\n"
                + "var intsArray = new Int32Array(buf);\n"
                + "intsArray[0] = 7.4;\n");

        V8Array intsArray = (V8Array) v8.get("intsArray");

        assertEquals(V8Value.INTEGER, intsArray.getType());
        intsArray.release();
    }
}
