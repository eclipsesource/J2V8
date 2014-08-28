package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8ExecutionException;
import com.eclipsesource.v8.V8Object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class V8CallbackTest {

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


    public interface ICallback {
        public void voidMethodNoParameters();

        public void voidMethodWithParameters(final int a, final double b, final boolean c, final String d);
    }

    @Test
    public void testVoidMethodCalledFromVoidScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        verify(callback).voidMethodNoParameters();
    }

    @Test
    public void testFunctionCallOnJSObject() {
        ICallback callback = mock(ICallback.class);
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8Object.executeVoidFunction("foo", null);

        verify(callback).voidMethodNoParameters();
        v8Object.release();
    }

    @Test
    public void testVoidMethodCalledFromIntScript() {
        ICallback callback = mock(ICallback.class);
        v8.registerJavaMethod(callback, "voidMethodNoParameters", "foo", new Class<?>[0]);

        v8.executeIntScript("foo();1");

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
                Boolean.TYPE,
                String.class });

        v8.executeVoidScript("foo(1,1.1, false, 'string');");

        verify(callback).voidMethodWithParameters(1, 1.1, false, "string");
    }

    public class ArrayParameters {
        int result = 0;

        public void add(final V8Array array) {
            for (int i = 0; i < array.getSize(); i++) {
                result += array.getInteger(i);
            }
        }

        public int getResult() {
            return result;
        }
    }

    @Test
    public void testVoidMethodCalledWithArrayParameters() {
        ArrayParameters obj = new ArrayParameters();
        v8.registerJavaMethod(obj, "add", "foo", new Class<?>[] { V8Array.class });

        v8.executeVoidScript("foo([1,2,3,4,5]);");
        int result = obj.getResult();

        assertEquals(15, result);
    }

    public class ObjectParameters {
        String result;

        public void add(final V8Object object) {
            result = object.getString("first") + object.getString("last") + object.getInteger("age");
        }

        public String getResult() {
            return result;
        }
    }

    @Test
    public void testVoidMethodCalledWithObjectParameters() {
        ObjectParameters obj = new ObjectParameters();
        v8.registerJavaMethod(obj, "add", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo({first:'john', last:'smith', age:7});");
        String result = obj.getResult();

        assertEquals("johnsmith7", result);
    }

    public class ThrowsSomething {
        public void foo() {
            throw new RuntimeException("My Runtime Exception");
        }
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsJavaException() {
        ThrowsSomething obj = new ThrowsSomething();
        v8.registerJavaMethod(obj, "foo", "foo", new Class<?>[] {});

        try {
            v8.executeVoidScript("foo()");
        } catch (Exception e) {
            assertEquals("My Runtime Exception", e.getMessage());
            throw e;
        }
    }

    public class NoParameters {

        boolean called = false;

        public void add(final V8Object object) {
            called = true;
        }

        public boolean isCalled() {
            return called;
        }
    }

    @Test
    public void testVoidMethodCallWithMissingArgs() {
        NoParameters obj = new NoParameters();
        v8.registerJavaMethod(obj, "add", "foo", new Class<?>[] { V8Object.class });

        v8.executeVoidScript("foo()");

        assertTrue(obj.isCalled());
    }

    @Test(expected = V8ExecutionException.class)
    @Ignore
    public void testIntMethodCallWithMissingArgsThrowsException() {
        NoParameters obj = new NoParameters();
        v8.registerJavaMethod(obj, "add", "foo", new Class<?>[] { V8Object.class });

        try {
            v8.executeIntScript("foo()");
        } catch (V8ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            throw e;
        }
    }
}
