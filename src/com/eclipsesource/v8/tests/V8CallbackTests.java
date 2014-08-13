package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V8CallbackTests {

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

    public class ObjNoParameters {
        boolean methodCalled = false;

        public void voidMethod() {
            methodCalled = true;
        }

        public boolean isMethodCalled() {
            return methodCalled;
        }
    }

    @Test
    public void testVoidMethodCalledFromVoidScript() {
        ObjNoParameters obj = new ObjNoParameters();
        v8.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[0]);

        v8.executeVoidScript("foo();");

        assertTrue(obj.isMethodCalled());
    }

    @Test
    public void testFunctionCallOnJSObject() {
        ObjNoParameters obj = new ObjNoParameters();
        V8Object v8Object = new V8Object(v8);
        v8Object.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[0]);

        v8Object.executeVoidFunction("foo", null);

        assertTrue(obj.isMethodCalled());
        v8Object.release();
    }

    @Test
    public void testVoidMethodCalledFromIntScript() {
        ObjNoParameters obj = new ObjNoParameters();
        v8.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[0]);

        v8.executeIntScript("foo();1");

        assertTrue(obj.isMethodCalled());
    }

    @Test
    public void testVoidMethodCalledFromDoubleScript() {
        ObjNoParameters obj = new ObjNoParameters();
        v8.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[0]);

        v8.executeDoubleScript("foo();1.1");

        assertTrue(obj.isMethodCalled());
    }

    @Test
    public void testVoidMethodCalledFromStringScript() {
        ObjNoParameters obj = new ObjNoParameters();
        v8.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[0]);

        v8.executeStringScript("foo();'test'");

        assertTrue(obj.isMethodCalled());
    }

    @Test
    public void testVoidMethodCalledFromArrayScript() {
        ObjNoParameters obj = new ObjNoParameters();
        v8.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[0]);

        v8.executeArrayScript("foo();[]").release();

        assertTrue(obj.isMethodCalled());
    }

    @Test
    public void testVoidMethodCalledFromObjectScript() {
        ObjNoParameters obj = new ObjNoParameters();
        v8.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[0]);

        v8.executeObjectScript("foo(); bar={}; bar;").release();

        assertTrue(obj.isMethodCalled());
    }

    public class ObjParameters {
        boolean methodCalled = false;
        int     a;
        double  b;
        boolean c;
        String  d;

        public void voidMethod(final int a, final double b, final boolean c, final String d) {
            methodCalled = true;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public boolean isMethodCalled() {
            return methodCalled;
        }

        public int getA() {
            return a;
        }

        public double getB() {
            return b;
        }

        public boolean getC() {
            return c;
        }

        public String getD() {
            return d;
        }
    }

    @Test
    public void testVoidMethodCalledWithParameters() {
        ObjParameters obj = new ObjParameters();
        v8.registerJavaMethod(obj, "voidMethod", "foo", new Class<?>[] { Integer.TYPE, Double.TYPE, Boolean.TYPE,
                String.class });

        v8.executeVoidScript("foo(1,1.1, false, 'string');");

        assertTrue(obj.isMethodCalled());
        assertEquals(1, obj.getA());
        assertEquals(1.1, obj.getB(), 0.000001);
        assertFalse(obj.getC());
        assertEquals("string", obj.getD());
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
}
