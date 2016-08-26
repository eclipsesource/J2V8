/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.eclipsesource.v8.utils.V8ObjectUtils;
import com.eclipsesource.v8.utils.typedarrays.Float32Array;
import com.eclipsesource.v8.utils.typedarrays.Float64Array;
import com.eclipsesource.v8.utils.typedarrays.Int16Array;
import com.eclipsesource.v8.utils.typedarrays.Int32Array;
import com.eclipsesource.v8.utils.typedarrays.Int8Array;
import com.eclipsesource.v8.utils.typedarrays.UInt16Array;
import com.eclipsesource.v8.utils.typedarrays.UInt32Array;
import com.eclipsesource.v8.utils.typedarrays.UInt8Array;
import com.eclipsesource.v8.utils.typedarrays.UInt8ClampedArray;

public class V8CallbackTest {

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
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    public interface ICallback {

        public void voidMethodNoParameters();

        public void voidMethodWithParameters(final int a, final double b, final boolean c, final String d, V8Object e);

        public void voidMethodWithObjectParameters(final Integer a);

        public void voidMethodWithArrayParameter(final V8Array array);

        public void voidMethodWithObjectParameter(final V8Object object);

        public void voidMethodWithFunctionParameter(final V8Function object);

        public void voidMethodWithStringParameter(final String string);

        public void voidMethodWithIntParameter(final int i);

        public int intMethodNoParameters();

        public Integer integerMethod();

        public int intMethodWithParameters(final int x, final int b);

        public int intMethodWithArrayParameter(final V8Array array);

        public double doubleMethodNoParameters();

        public float floatMethodNoParameters();

        public double doubleMethodWithParameters(final double x, final double y);

        public boolean booleanMethodNoParameters();

        public boolean booleanMethodWithArrayParameter(final V8Array array);

        public String stringMethodNoParameters();

        public String stringMethodWithArrayParameter(final V8Array array);

        public V8Object v8ObjectMethodNoParameters();

        public V8Object v8ObjectMethodWithObjectParameter(final V8Object object);

        public V8Array v8ArrayMethodNoParameters();

        public V8Array v8ArrayMethodWithStringParameter(final String string);

        public Object objectMethodNoParameter();

        public void voidMethodVarArgs(final Object... args);

        public void voidMethodStringVarArgs(final String... args);

        public void voidMethodV8ObjectVarArgs(final V8Object... args);

        public void voidMethodVarArgsAndOthers(int x, int y, final Object... args);

        public void voidMethodVarArgsReceiverAndOthers(V8Object recier, int x, int y, final Object... args);

    }

    @Test
    public void testVoidMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testIntMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).intMethodNoParameters();
    }

    @Test
    public void testIntMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(7).when(callback).intMethodNoParameters();
        v8.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        int result = v8.executeIntegerScript("foo();");

        assertEquals(7, result);
    }

    @Test
    public void testIntegerMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(8).when(callback).integerMethod();
        v8.registerJavaMethod(callback, "integerMethod", "foo", new Class<?>[0]);

        int result = v8.executeIntegerScript("foo();");

        assertEquals(8, result);
    }

    @Test
    public void testDoubleMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).doubleMethodNoParameters();
    }

    @Test
    public void testDoubleMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(3.14159).when(callback).doubleMethodNoParameters();
        v8.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8.executeDoubleScript("foo();");

        assertEquals(3.14159, result, 0.0000001);
    }

    @Test
    public void testFloatMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(3.14159f).when(callback).floatMethodNoParameters();
        v8.registerJavaMethod(callback, "floatMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8.executeDoubleScript("foo();");

        assertEquals(3.14159f, result, 0.0000001);
    }

    @Test
    public void testBooleanMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).booleanMethodNoParameters();
    }

    @Test
    public void testBooleanMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn(true).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("foo();");

        assertTrue(result);
    }

    @Test
    public void testStringMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).stringMethodNoParameters();
    }

    @Test
    public void testCallbackWithFunctionInParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("var bar = function() {}; foo(bar);");

        verify(callback).voidMethodWithObjectParameter(isNotNull(V8Function.class));
    }

    @Test
    public void testCallbackWithExplicitFunctionInParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithFunctionParameter", "foo", new Class<?>[] { V8Function.class });

        v8.executeVoidScript("var bar = function() {}; foo(bar);");

        verify(callback).voidMethodWithFunctionParameter(isNotNull(V8Function.class));
    }

    @Test
    public void testCallbackWithUndefinedInParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo(undefined);");

        verify(callback).voidMethodWithObjectParameter(new V8Object.Undefined());
    }

    @Test
    public void testCallbackWithNullInStringParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithStringParameter", "foo", new Class<?>[] { String.class });

        v8.executeVoidScript("foo(null);");

        verify(callback).voidMethodWithStringParameter(null);
    }

    @Test
    public void testCallbackVarArgsWithUndefined() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgs", "foo", new Class<?>[] { Object[].class });

        v8.executeVoidScript("foo(undefined);");

        verify(callback).voidMethodVarArgs(new V8Object.Undefined());
    }

    @Test
    public void testCallbackStringVarArgs() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodStringVarArgs", "foo", new Class<?>[] { String[].class });

        v8.executeVoidScript("foo('bar');");

        verify(callback).voidMethodStringVarArgs(eq("bar"));
    }

    @Test
    public void testCallbackV8ObjectVarArgs() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodV8ObjectVarArgs", "foo", new Class<?>[] { V8Object[].class });

        v8.executeVoidScript("foo({});");

        verify(callback).voidMethodV8ObjectVarArgs(any(V8Object.class));
        verify(callback).voidMethodV8ObjectVarArgs(notNull(V8Object.class));
    }

    @Test
    public void testCallbackV8ArrayVarArgs() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodV8ObjectVarArgs", "foo", new Class<?>[] { V8Object[].class });

        v8.executeVoidScript("foo([]);");

        verify(callback).voidMethodV8ObjectVarArgs(any(V8Array.class));
        verify(callback).voidMethodV8ObjectVarArgs(notNull(V8Array.class));
    }

    @Test
    public void testCallbackWithNullInParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo(null);");

        verify(callback).voidMethodWithObjectParameter(null);
    }

    @Test
    public void testCallbackWithEmptyParameterList() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo();");

        verify(callback).voidMethodWithObjectParameter(new V8Object.Undefined());
    }

    @Test
    public void testStringMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        doReturn("bar").when(callback).stringMethodNoParameters();
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        String result = v8.executeStringScript("foo();");

        assertEquals("bar", result);
    }

    @Test
    public void testStringMethodCalledFromScriptWithNull() {
        ICallback callback = mock(ICallback.class);
        doReturn(null).when(callback).stringMethodNoParameters();
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("foo() === null");

        assertTrue(result);
    }

    @Test
    public void testV8ObjectMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).v8ObjectMethodNoParameters();
    }

    @Test(expected = V8RuntimeException.class)
    public void testReturnReleasedV8ObjectThrowsException() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<V8Object>() {

            @Override
            public V8Object answer(final InvocationOnMock invocation) throws Throwable {
                V8Object result = new V8Object(v8);
                result.release();
                return result;
            }

        }).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeObjectScript("foo();");
    }

    @Test
    public void testV8ObjectMethodReturnsUndefined() {
        ICallback callback = mock(ICallback.class);
        doReturn(V8.getUndefined()).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("typeof foo() === 'undefined'");

        assertTrue(result);
    }

    @Test
    public void testV8ObjectMethodReturnsNull() {
        ICallback callback = mock(ICallback.class);
        doReturn(null).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("foo() === null");

        assertTrue(result);
    }

    @Test
    public void testV8ObjectMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        V8Object object = new V8Object(v8);
        object.add("name", "john");
        doReturn(object).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        V8Object result = v8.executeObjectScript("foo();");

        assertEquals("john", result.getString("name"));
        result.release();
    }

    @Test
    public void testV8ObjectMethodReleasesResults() {
        ICallback callback = mock(ICallback.class);
        V8Object object = new V8Object(v8);
        doReturn(object).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        assertTrue(object.isReleased());
    }

    @Test
    public void testV8ArrayMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).v8ArrayMethodNoParameters();
    }

    @Test
    public void testV8ArrayMethodReturnsUndefined() {
        ICallback callback = mock(ICallback.class);
        doReturn(new V8Array.Undefined()).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8.executeBooleanScript("typeof foo() === 'undefined'");

        assertTrue(result);
    }

    @Test
    public void testV8ArrayMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        V8Array array = new V8Array(v8);
        array.push("john");
        doReturn(array).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        V8Array result = v8.executeArrayScript("foo();");

        assertEquals("john", result.getString(0));
        result.release();
    }

    @Test
    public void testV8TypedArrayMethodCalledFromScriptWithResult() {
        ICallback callback = mock(ICallback.class);
        V8ArrayBuffer arrayBuffer = new V8ArrayBuffer(v8, 100);
        V8TypedArray array = new V8TypedArray(v8, arrayBuffer, V8Value.INTEGER, 0, 25);
        for (int i = 0; i < 25; i++) {
            array.add("" + i, i);
        }
        doReturn(array).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        V8Array result = v8.executeArrayScript("foo();");

        assertTrue(result instanceof V8TypedArray);
        for (int i = 0; i < 25; i++) {
            assertEquals(i, result.getInteger(i));
        }
        arrayBuffer.release();
        result.release();
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Int32Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof Int32Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Int32Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Int8Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof Int8Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Int8Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Int16Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof Int16Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Int16Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Float32Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof Float32Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Float32Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Float64Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof Float64Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Float64Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Uint8Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof UInt8Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Uint8Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Uint8ClampledArray() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof UInt8ClampedArray;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Uint8ClampedArray(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Uint16Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof UInt16Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Uint16Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackWithTypedArray_Uint32Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Boolean invoke(final V8Object receiver, final V8Array parameters) {
                Object result = V8ObjectUtils.getValue(parameters, 0);
                return result instanceof UInt32Array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        Object result = v8.executeScript("callback(new Uint32Array(24));");

        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeCallbackReturnsArrayBuffer() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                V8ArrayBuffer arrayBuffer = new V8ArrayBuffer(v8, 8);
                arrayBuffer.getBackingStore().put((byte) 8);
                return arrayBuffer;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        int result = v8.executeIntegerScript("\n"
                + "var buffer = callback();\n"
                + "new Int8Array(buffer)[0]");

        assertEquals(8, result);
    }

    @Test
    public void testInvokeCallbackWithArrayAsParameter_PassedTypedArray() {
        class MyCallback {
            @SuppressWarnings("unused")
            public int testArray(final V8Array array) {
                return array.length();
            }
        }
        MyCallback callback = new MyCallback();
        v8.registerJavaMethod(callback, "testArray", "testArray", new Class[] { V8Array.class });

        int result = v8.executeIntegerScript("\n"
                + "var array = new Float32Array(5);\n"
                + "for (var i = 0; i < 5; i++) \n"
                + "  array[i] = i / 1000; "
                + "testArray(array);");

        assertEquals(5, result);
    }

    @Test
    public void testInvokeCallbackWithArrayBufferAsParameter() {
        class MyCallback {
            @SuppressWarnings("unused")
            public int testArray(final V8ArrayBuffer arrayBuffer) {
                return arrayBuffer.getBackingStore().limit();
            }
        }
        MyCallback callback = new MyCallback();
        v8.registerJavaMethod(callback, "testArray", "testArray", new Class[] { V8ArrayBuffer.class });

        int result = v8.executeIntegerScript("\n"
                + "var arrayBuffer = new ArrayBuffer(8);\n"
                + "testArray(arrayBuffer);");

        assertEquals(8, result);
    }

    @Test
    public void testInvokeCallbackWithTypedArrayAsParameter() {
        class MyCallback {

            @SuppressWarnings("unused")
            public int testArray(final V8Array array) {
                fail("Test should have invoked the other method.");
                return 0;
            }

            @SuppressWarnings("unused")
            public int testArray(final V8TypedArray array) {
                return array.length();
            }
        }
        MyCallback callback = new MyCallback();
        v8.registerJavaMethod(callback, "testArray", "testArray", new Class[] { V8TypedArray.class });

        int result = v8.executeIntegerScript("\n"
                + "var array = new Float32Array(5);\n"
                + "for (var i = 0; i < 5; i++) \n"
                + "  array[i] = i / 1000; "
                + "testArray(array);");

        assertEquals(5, result);
    }

    @Test
    public void testInvokeCallbackReturnsTypedArray_Int8Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                V8ArrayBuffer arrayBuffer = new V8ArrayBuffer(v8, 8);
                V8TypedArray array = new V8TypedArray(v8, arrayBuffer, V8Value.INT_8_ARRAY, 0, 8);
                array.add("0", 8);
                arrayBuffer.release();
                return array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        int result = v8.executeIntegerScript("\n"
                + "var array = callback();\n"
                + "array[0]");

        assertEquals(8, result);
    }

    @Test
    public void testInvokeCallbackReturnsTypedArray_Int16Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                V8ArrayBuffer arrayBuffer = new V8ArrayBuffer(v8, 8);
                V8TypedArray array = new V8TypedArray(v8, arrayBuffer, V8Value.INT_16_ARRAY, 0, 4);
                array.add("0", 8000);
                arrayBuffer.release();
                return array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        int result = v8.executeIntegerScript("\n"
                + "var array = callback();\n"
                + "array[0]");

        assertEquals(8000, result);
    }

    @Test
    public void testInvokeCallbackReturnsTypedArray_Int32Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                V8ArrayBuffer arrayBuffer = new V8ArrayBuffer(v8, 8);
                V8TypedArray array = new V8TypedArray(v8, arrayBuffer, V8Value.INT_32_ARRAY, 0, 2);
                array.add("0", 800000000);
                arrayBuffer.release();
                return array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        int result = v8.executeIntegerScript("\n"
                + "var array = callback();\n"
                + "array[0]");

        assertEquals(800000000, result);
    }

    @Test
    public void testInvokeCallbackReturnsTypedArray_Float32Array() {
        JavaCallback callback = new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                V8ArrayBuffer arrayBuffer = new V8ArrayBuffer(v8, 8);
                V8TypedArray array = new V8TypedArray(v8, arrayBuffer, V8Value.FLOAT_32_ARRAY, 0, 2);
                array.add("0", 3.14);
                arrayBuffer.release();
                return array;
            }
        };
        v8.registerJavaMethod(callback, "callback");

        float result = (float) v8.executeDoubleScript("\n"
                + "var array = callback();\n"
                + "array[0]");

        assertEquals(3.14, result, 0.1);
    }

    @Test
    public void testV8ArrayMethodReleasesResults() {
        ICallback callback = mock(ICallback.class);
        V8Array object = new V8Array(v8);
        doReturn(object).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        assertTrue(object.isReleased());
    }

    @Test
    public void testVoidFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8Object.executeVoidFunction("foo", null);

        verify(callback).voidMethodNoParameters();
        v8Object.release();
    }

    @Test
    public void testVoidFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8Array.executeVoidFunction("foo", null);

        verify(callback).voidMethodNoParameters();
        v8Array.release();
    }

    @Test
    public void testIntFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(99).when(callback).intMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        int result = v8Object.executeIntegerFunction("foo", null);

        verify(callback).intMethodNoParameters();
        assertEquals(99, result);
        v8Object.release();
    }

    @Test
    public void testIntFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(99).when(callback).intMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[0]);

        int result = v8Array.executeIntegerFunction("foo", null);

        verify(callback).intMethodNoParameters();
        assertEquals(99, result);
        v8Array.release();
    }

    @Test
    public void testDoubleFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(99.9).when(callback).doubleMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8Object.executeDoubleFunction("foo", null);

        verify(callback).doubleMethodNoParameters();
        assertEquals(99.9, result, 0.000001);
        v8Object.release();
    }

    @Test
    public void testDoubleFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(99.9).when(callback).doubleMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[0]);

        double result = v8Array.executeDoubleFunction("foo", null);

        verify(callback).doubleMethodNoParameters();
        assertEquals(99.9, result, 0.000001);
        v8Array.release();
    }

    @Test
    public void testBooleanFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(false).when(callback).booleanMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8Object.executeBooleanFunction("foo", null);

        verify(callback).booleanMethodNoParameters();
        assertFalse(result);
        v8Object.release();
    }

    @Test
    public void testBooleanFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(false).when(callback).booleanMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[0]);

        boolean result = v8Array.executeBooleanFunction("foo", null);

        verify(callback).booleanMethodNoParameters();
        assertFalse(result);
        v8Array.release();
    }

    @Test
    public void testStringFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn("mystring").when(callback).stringMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        String result = v8Object.executeStringFunction("foo", null);

        verify(callback).stringMethodNoParameters();
        assertEquals("mystring", result);
        v8Object.release();
    }

    @Test
    public void testStringFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn("mystring").when(callback).stringMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[0]);

        String result = v8Array.executeStringFunction("foo", null);

        verify(callback).stringMethodNoParameters();
        assertEquals("mystring", result);
        v8Array.release();
    }

    @Test
    public void testV8ObjectFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeObjectScript("x = {first:'bob'}; x")).when(callback).v8ObjectMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        V8Object result = v8Object.executeObjectFunction("foo", null);

        verify(callback).v8ObjectMethodNoParameters();
        assertEquals("bob", result.getString("first"));
        v8Object.release();
        result.release();
    }

    @Test
    public void testV8ObjectFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeObjectScript("x = {first:'bob'}; x")).when(callback).v8ObjectMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[0]);

        V8Object result = v8Array.executeObjectFunction("foo", null);

        verify(callback).v8ObjectMethodNoParameters();
        assertEquals("bob", result.getString("first"));
        v8Array.release();
        result.release();
    }

    @Test
    public void testV8ArrayFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeArrayScript("x = ['a','b','c']; x")).when(callback).v8ArrayMethodNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        V8Array result = v8Object.executeArrayFunction("foo", null);

        verify(callback).v8ArrayMethodNoParameters();
        assertEquals(3, result.length());
        assertEquals("a", result.getString(0));
        assertEquals("b", result.getString(1));
        assertEquals("c", result.getString(2));
        v8Object.release();
        result.release();
    }

    @Test
    public void testV8ArrayFunctionCallOnJSArray() {
        ICallback callback = mock(ICallback.class);
        doReturn(v8.executeArrayScript("x = ['a','b','c']; x")).when(callback).v8ArrayMethodNoParameters();
        V8Array v8Array = new V8Array(v8);
        v8Array.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[0]);

        V8Array result = v8Array.executeArrayFunction("foo", null);

        verify(callback).v8ArrayMethodNoParameters();
        assertEquals(3, result.length());
        assertEquals("a", result.getString(0));
        assertEquals("b", result.getString(1));
        assertEquals("c", result.getString(2));
        v8Array.release();
        result.release();
    }

    @Test
    public void testVoidMethodCalledFromIntScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeIntegerScript("foo();1");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromDoubleScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeDoubleScript("foo();1.1");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromStringScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeStringScript("foo();'test'");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromArrayScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeArrayScript("foo();[]").release();

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledFromObjectScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeObjectScript("foo(); bar={}; bar;").release();

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testVoidMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Double.TYPE,
                Boolean.TYPE, String.class, V8Object.class });

        v8.executeVoidScript("foo(1,1.1, false, 'string', undefined);");

        verify(callback).voidMethodWithParameters(1, 1.1, false, "string", new V8Object.Undefined());
    }

    @Test
    public void testIntMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Integer>() {

            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                int x = (Integer) args[0];
                int y = (Integer) args[1];
                return x + y;
            }

        }).when(callback).intMethodWithParameters(anyInt(), anyInt());
        v8.registerJavaMethod(callback, "intMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Integer.TYPE });

        int result = v8.executeIntegerScript("foo(8,7);");

        verify(callback).intMethodWithParameters(8, 7);
        assertEquals(15, result);
    }

    @Test
    public void testDoubleMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Double>() {

            @Override
            public Double answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                double x = (Double) args[0];
                double y = (Double) args[1];
                return x + y;
            }

        }).when(callback).doubleMethodWithParameters(anyInt(), anyInt());
        v8.registerJavaMethod(callback, "doubleMethodWithParameters", "foo",
                new Class<?>[] { Double.TYPE, Double.TYPE });

        double result = v8.executeDoubleScript("foo(8.3,7.1);");

        verify(callback).doubleMethodWithParameters(8.3, 7.1);
        assertEquals(15.4, result, 0.000001);
    }

    @Test
    public void testVoidMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                assertEquals(1, args.length);
                assertEquals(1, ((V8Array) args[0]).getInteger(0));
                assertEquals(2, ((V8Array) args[0]).getInteger(1));
                assertEquals(3, ((V8Array) args[0]).getInteger(2));
                assertEquals(4, ((V8Array) args[0]).getInteger(3));
                assertEquals(5, ((V8Array) args[0]).getInteger(4));
                return null;
            }
        }).when(callback).voidMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "voidMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        v8.executeVoidScript("foo([1,2,3,4,5]);");
    }

    @Test
    public void testIntMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int arrayLength = ((V8Array) args[0]).length();
                int result = 0;
                for (int i = 0; i < arrayLength; i++) {
                    result += ((V8Array) args[0]).getInteger(i);
                }
                return result;
            }
        }).when(callback).intMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "intMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        int result = v8.executeIntegerScript("foo([1,2,3,4,5]);");

        assertEquals(15, result);
    }

    @Test
    public void testBooleanMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int arrayLength = ((V8Array) args[0]).length();
                int result = 0;
                for (int i = 0; i < arrayLength; i++) {
                    result += ((V8Array) args[0]).getInteger(i);
                }
                return result > 10;
            }
        }).when(callback).booleanMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "booleanMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        boolean result = v8.executeBooleanScript("foo([1,2,3,4,5]);");

        assertTrue(result);
    }

    @Test
    public void testStringMethodCalledWithArrayParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int arrayLength = ((V8Array) args[0]).length();
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < arrayLength; i++) {
                    result.append(((V8Array) args[0]).getString(i));
                }
                return result.toString();
            }
        }).when(callback).stringMethodWithArrayParameter(any(V8Array.class));
        v8.registerJavaMethod(callback, "stringMethodWithArrayParameter", "foo", new Class<?>[] { V8Array.class });

        String result = v8.executeStringScript("foo(['a', 'b', 'c', 'd', 'e']);");

        assertEquals("abcde", result);
    }

    @Test
    public void testArrayMethodCalledWithParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<V8Array>() {
            @Override
            public V8Array answer(final InvocationOnMock invocation) {
                V8Array result = new V8Array(v8);
                String arg = (String) invocation.getArguments()[0];
                String[] split = arg.split(" ");
                for (String string : split) {
                    result.push(string);
                }
                return result;
            }
        }).when(callback).v8ArrayMethodWithStringParameter(any(String.class));
        v8.registerJavaMethod(callback, "v8ArrayMethodWithStringParameter", "foo", new Class<?>[] { String.class });

        V8Array result = v8.executeArrayScript("foo('hello world how are you');");

        assertEquals(5, result.length());
        assertEquals("hello", result.getString(0));
        assertEquals("world", result.getString(1));
        assertEquals("how", result.getString(2));
        assertEquals("are", result.getString(3));
        assertEquals("you", result.getString(4));
        result.release();
    }

    @Test
    public void testVoidMethodCalledWithObjectParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                assertEquals(1, args.length);
                assertEquals("john", ((V8Object) args[0]).getString("first"));
                assertEquals("smith", ((V8Object) args[0]).getString("last"));
                assertEquals(7, ((V8Object) args[0]).getInteger("age"));
                return null;
            }
        }).when(callback).voidMethodWithObjectParameter(any(V8Object.class));
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo({first:'john', last:'smith', age:7});");

        verify(callback).voidMethodWithObjectParameter(any(V8Object.class));
    }

    @Test
    public void testObjectMethodCalledWithObjectParameters() {
        ICallback callback = mock(ICallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                V8Object parameter = ((V8Object) args[0]);
                V8Object result = new V8Object(v8);
                result.add("first", parameter.getString("last"));
                result.add("last", parameter.getString("first"));
                return result;
            }
        }).when(callback).v8ObjectMethodWithObjectParameter(any(V8Object.class));
        v8.registerJavaMethod(callback, "v8ObjectMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        V8Object result = v8.executeObjectScript("foo({first:'john', last:'smith'});");

        assertEquals("smith", result.getString("first"));
        assertEquals("john", result.getString("last"));
        result.release();
    }

    @Test(expected = RuntimeException.class)
    public void testVoidMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test
    public void testCatchJavaExceptionInJS() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'My Runtime Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testCatchJavaExceptionInJSWithoutMessage() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException()).when(callback).voidMethodNoParameters();
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'Unhandled Java Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testNonVoidCallbackCatchJavaExceptionInJS() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'My Runtime Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testNonVoidCallbackCatchJavaExceptionInJSWithoutMessage() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException()).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[] {});

        v8.executeVoidScript("var caught = false; try {foo();} catch (e) {if ( e === 'Unhandled Java Exception' ) caught=true;}");

        assertTrue(v8.getBoolean("caught"));
    }

    @Test
    public void testJSCatchWillCatchJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        doNothing().when(callback).voidMethodWithStringParameter(anyString());
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});
        v8.registerJavaMethod(callback, "voidMethodWithStringParameter", "bar", new Class<?>[] { String.class });

        v8.executeVoidScript("try {foo();} catch (e) {bar('string');}");

        // Runtime exception should not be thrown
    }

    @Test
    public void testJSCatchWillCatchJavaException2() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).voidMethodNoParameters();
        doNothing().when(callback).voidMethodWithStringParameter(anyString());
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[] {});
        v8.registerJavaMethod(callback, "voidMethodWithStringParameter", "bar", new Class<?>[] { String.class });

        v8.executeVoidScript("try {foo();} catch (e) {}");

        // Runtime exception should not be thrown
    }

    @Test(expected = RuntimeException.class)
    public void testIntMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).intMethodNoParameters();
        v8.registerJavaMethod(callback, "intMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testDoubleMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).doubleMethodNoParameters();
        v8.registerJavaMethod(callback, "doubleMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testBooleanMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).booleanMethodNoParameters();
        v8.registerJavaMethod(callback, "booleanMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testStringMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).stringMethodNoParameters();
        v8.registerJavaMethod(callback, "stringMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testV8ObjectMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).v8ObjectMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ObjectMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testV8ArrayMethodThrowsJavaException() {
        ICallback callback = mock(ICallback.class);
        doThrow(new RuntimeException("My Runtime Exception")).when(callback).v8ArrayMethodNoParameters();
        v8.registerJavaMethod(callback, "v8ArrayMethodNoParameters", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (V8ScriptExecutionException e) {
            assertEquals("My Runtime Exception", e.getCause().getMessage());
            throw e;
        }
    }

    @Test
    public void testVoidMethodCallWithMissingObjectArgs() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameter", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo()");

        verify(callback).voidMethodWithObjectParameter(new V8Array.Undefined());
    }

    @Test
    public void testObjectMethodReturnsInteger() {
        ICallback callback = mock(ICallback.class);
        doReturn(7).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        int result = v8.executeIntegerFunction("foo", null);

        assertEquals(7, result);
    }

    @Test
    public void testObjectMethodReturnsBoolean() {
        ICallback callback = mock(ICallback.class);
        doReturn(true).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        boolean result = v8.executeBooleanFunction("foo", null);

        assertTrue(result);
    }

    @Test
    public void testObjectMethodReturnsDouble() {
        ICallback callback = mock(ICallback.class);
        doReturn(7.7).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        double result = v8.executeDoubleFunction("foo", null);

        assertEquals(7.7, result, 0.000001);
    }

    @Test
    public void testObjectMethodReturnsString() {
        ICallback callback = mock(ICallback.class);
        doReturn("foobar").when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        String result = v8.executeStringFunction("foo", null);

        assertEquals("foobar", result);
    }

    @Test
    public void testObjectMethodReturnsV8Object() {
        ICallback callback = mock(ICallback.class);
        doReturn(new V8Object(v8).add("foo", "bar")).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        V8Object result = v8.executeObjectFunction("foo", null);

        assertEquals("bar", result.getString("foo"));
        result.release();
    }

    @Test
    public void testObjectMethodReturnsV8Array() {
        ICallback callback = mock(ICallback.class);
        doReturn(new V8Array(v8).push(1).push("a")).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        V8Array result = v8.executeArrayFunction("foo", null);

        assertEquals(2, result.length());
        assertEquals(1, result.getInteger(0));
        assertEquals("a", result.getString(1));
        result.release();
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testObjectMethodReturnsIncompatibleType() {
        ICallback callback = mock(ICallback.class);
        doReturn(new Date()).when(callback).objectMethodNoParameter();
        v8.registerJavaMethod(callback, "objectMethodNoParameter", "foo", new Class<?>[] {});

        v8.executeVoidScript("foo()");
    }

    @Test
    public void testVarArgParametersString() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, 'foo', 'bar')");

        verify(callback).voidMethodVarArgsAndOthers(1, 2, "foo", "bar");
    }

    @Test
    public void testVarArgParametersObject() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, {}, {foo:'bar'})");

        verify(callback).voidMethodVarArgsAndOthers(eq(1), eq(2), any(V8Object.class), any(V8Object.class));
    }

    @Test
    public void testVarArgParametersArray() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, [], [1,2,3])");

        verify(callback).voidMethodVarArgsAndOthers(eq(1), eq(2), any(V8Object.class), any(V8Object.class));
    }

    @Test
    public void testVarArgParametersInts() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2, 3, 4)");

        verify(callback).voidMethodVarArgsAndOthers(1, 2, 3, 4);
    }

    @Test
    public void testVarArgParametersMissing() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1, 2)");

        verify(callback).voidMethodVarArgsAndOthers(1, 2);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testMissingParamters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Double.TYPE,
                Boolean.TYPE, String.class, V8Object.class });

        v8.executeVoidScript("foo()");
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testSomeMissingParamters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithParameters", "foo", new Class<?>[] { Integer.TYPE, Double.TYPE,
                Boolean.TYPE, String.class, V8Object.class });

        v8.executeVoidScript("foo(1,2)");
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testMissingIntParamtersWithVarArgs() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsAndOthers", "foo", new Class<?>[] { Integer.TYPE,
                Integer.TYPE, Object[].class });

        v8.executeVoidScript("foo(1)");
    }

    @Test
    public void testVarArgsNoReciver() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsReceiverAndOthers", "foo", new Class<?>[] { V8Object.class, Integer.TYPE,
                Integer.TYPE, Object[].class }, false);

        v8.executeVoidScript("foo(undefined, 1, 2);");

        verify(callback).voidMethodVarArgsReceiverAndOthers(new V8Object.Undefined(), 1, 2);
    }

    @Test
    public void testVarArgsWithReciver() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsReceiverAndOthers", "foo", new Class<?>[] { V8Object.class, Integer.TYPE,
                Integer.TYPE, Object[].class }, true);
        V8Array parameters = new V8Array(v8).push(1).push(2);
        doAnswer(constructReflectiveAnswer(v8, parameters, null)).when(callback).voidMethodVarArgsReceiverAndOthers(any(V8Object.class), anyInt(), anyInt());

        v8.executeVoidScript("foo(1, 2);");

        parameters.release();
    }

    @Test
    public void testAvailableVarArgsWithReciver() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsReceiverAndOthers", "foo", new Class<?>[] { V8Object.class, Integer.TYPE,
                Integer.TYPE, Object[].class }, true);
        V8Array parameters = new V8Array(v8).push(1).push(2).push(3).push(4);
        doAnswer(constructReflectiveAnswer(v8, parameters, null)).when(callback).voidMethodVarArgsReceiverAndOthers(any(V8Object.class), anyInt(), anyInt());

        v8.executeVoidScript("foo(1, 2, 3, 4);");

        parameters.release();
    }

    @Test
    public void testAvailableVarArgsWithReciver2() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsReceiverAndOthers", "foo", new Class<?>[] { V8Object.class, Integer.TYPE,
                Integer.TYPE, Object[].class }, true);
        V8Array parameters = new V8Array(v8).push(1).push(2).push(3).push(false);
        doAnswer(constructReflectiveAnswer(v8, parameters, null)).when(callback).voidMethodVarArgsReceiverAndOthers(any(V8Object.class), anyInt(), anyInt());

        v8.executeVoidScript("foo(1, 2, 3, false);");

        parameters.release();
    }

    @Test
    public void testAvailableVarArgsWithNoReciver() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodVarArgsReceiverAndOthers", "foo", new Class<?>[] { V8Object.class, Integer.TYPE,
                Integer.TYPE, Object[].class }, false);
        V8Array parameters = new V8Array(v8).push(v8).push(1).push(2).push(3).push(false);
        doAnswer(constructReflectiveAnswer(null, parameters, null)).when(callback).voidMethodVarArgsReceiverAndOthers(any(V8Object.class), anyInt(), anyInt());

        v8.executeVoidScript("foo(this, 1, 2, 3, false);");

        parameters.release();
    }

    @Test
    public void testMissingParamtersWithObjectParameters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameters", "foo", new Class<?>[] { Integer.class });

        v8.executeVoidScript("foo(1)");

        verify(callback).voidMethodWithObjectParameters(1);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testMissingParamtersWithMissingObjectParameters() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithObjectParameters", "foo", new Class<?>[] { Integer.class });

        v8.executeVoidScript("foo()");
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testCallJavaMethodMissingInt() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithIntParameter", "foo", new Class<?>[] { Integer.TYPE });

        v8.executeVoidScript("foo()");
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testCallJavaMethodNullInt() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithIntParameter", "foo", new Class<?>[] { Integer.TYPE });

        v8.executeVoidScript("foo(null)");
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testCallJavaMethodMissingString() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithStringParameter", "foo", new Class<?>[] { String.class });

        v8.executeVoidScript("foo()");
    }

    @Test
    public void testCallJavaMethodNullString() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithStringParameter", "foo", new Class<?>[] { String.class });

        v8.executeVoidScript("foo(null)");

        verify(callback).voidMethodWithStringParameter(null);
    }

    @Test(expected = V8ScriptExecutionException.class)
    public void testCallJavaMethodNotInt() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodWithIntParameter", "foo", new Class<?>[] { Integer.TYPE });

        v8.executeVoidScript("foo('bar')");
    }

    @Test
    public void testRegisterJavaCallback() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidScript("foo()");

        verify(callback).invoke(any(V8Object.class), any(V8Array.class));
    }

    @Test
    public void testRegisterJavaCallbackExecuteFunction() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidFunction("foo", null);

        verify(callback).invoke(any(V8Object.class), any(V8Array.class));
    }

    @Test
    public void testInvokeCallbackWithParameters() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");
        V8Object object = new V8Object(v8).add("foo", "bar");
        V8Array array = new V8Array(v8).push(1).push(2).push(3);
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push("test");
        parameters.push(3.14159);
        parameters.push(true);
        parameters.push(object);
        parameters.push(array);
        doAnswer(constructAnswer(null, parameters, null)).when(callback).invoke(any(V8Object.class), any(V8Array.class));

        v8.executeVoidFunction("foo", parameters);
        parameters.release();
        object.release();
        array.release();
    }

    @Test
    public void testRegisterJavaVoidCallback() {
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidScript("foo()");

        verify(callback).invoke(any(V8Object.class), any(V8Array.class));
    }

    @Test
    public void testRegisterJavaVoidCallbackExecuteFunction() {
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        v8.registerJavaMethod(callback, "foo");

        v8.executeVoidFunction("foo", null);

        verify(callback).invoke(any(V8Object.class), any(V8Array.class));
    }

    @Test
    public void testInvokeVoidCallbackWithParameters() {
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        v8.registerJavaMethod(callback, "foo");
        V8Object object = new V8Object(v8).add("foo", "bar");
        V8Array array = new V8Array(v8).push(1).push(2).push(3);
        V8Array parameters = new V8Array(v8);
        parameters.push(7);
        parameters.push("test");
        parameters.push(3.14159);
        parameters.push(true);
        parameters.push(object);
        parameters.push(array);
        doAnswer(constructAnswer(null, parameters, null)).when(callback).invoke(any(V8Object.class), any(V8Array.class));

        v8.executeVoidFunction("foo", parameters);
        parameters.release();
        object.release();
        array.release();
    }

    @Test
    public void testInvokeCallbackWithReturnValue() {
        JavaCallback callback = mock(JavaCallback.class);
        v8.registerJavaMethod(callback, "foo");
        doAnswer(constructAnswer(null, null, 77)).when(callback).invoke(any(V8Object.class), any(V8Array.class));

        int result = v8.executeIntegerFunction("foo", null);

        assertEquals(77, result);
    }

    @Test
    public void testInvokeCallbackFunctionUsesReciver() {
        V8Object bar = v8.executeObjectScript("var bar = {}; bar;");
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        bar.registerJavaMethod(callback, "foo");
        doAnswer(constructAnswer(bar, null, 77)).when(callback).invoke(any(V8Object.class), any(V8Array.class));

        bar.executeVoidFunction("foo", null);
        bar.release();
    }

    @Test
    public void testInvokeCallbackOnGlobalFunctionUsesGlobalScopeAsReciver() {
        JavaVoidCallback callback = mock(JavaVoidCallback.class);
        v8.registerJavaMethod(callback, "foo");
        doAnswer(constructAnswer(v8, null, null)).when(callback).invoke(any(V8Object.class), any(V8Array.class));

        v8.executeVoidFunction("foo", null);
    }

    @Test
    public void testInvokeCallbackOnGlobalFunctionUsesGlobalScopeAsReciver2() {
        JavaCallback javaCallback = new JavaCallback() {

            @Override
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                return receiver.executeFunction("testGlobal", null);
            }
        };
        v8.registerJavaMethod(javaCallback, "foo");
        boolean result = (Boolean) v8.executeScript("var global = this;\n"
                + "var testGlobal = function() {return this === global;}; \n"
                + "foo();");

        assertTrue(result);
    }

    private Answer<Object> constructAnswer(final V8Object receiver, final V8Array parameters, final Object result) {
        return new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (parameters != null) {
                    assertEquals(parameters.length(), ((V8Array) args[1]).length());
                    for (int i = 0; i < args.length; i++) {
                        assertEquals(parameters.get(i), ((V8Array) args[1]).get(i));
                    }
                }
                if (receiver != null) {
                    assertEquals(receiver, args[0]);
                }
                return result;
            }
        };
    }

    private Answer<Object> constructReflectiveAnswer(final V8Object receiver, final V8Array parameters, final Object result) {
        return new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                int start = 0;
                if (receiver != null) {
                    assertEquals(receiver, args[start]);
                    start++;
                }
                for (int i = start; i < args.length; i++) {
                    assertEquals(parameters.get(i - start), args[i]);
                }
                return result;
            }
        };
    }

}
