
package com.eclipsesource.v8;

public class V8Object {

    public final int   INTEGER                 = 1;
    public final int   DOUBLE                  = 2;
    public final int   BOOLEAN                 = 3;
    public final int   STRING                  = 4;
    public final int   INTEGER_ARRAY           = 5;
    public final int   DOUBLE_ARRAY            = 6;
    public final int   BOOLEAN_ARRAY           = 7;
    public final int   STRING_ARRAY            = 8;
    public final int   V8_ARRAY                = 9;
    public final int   V8_OBJECT               = 10;

    private static int v8ObjectInstanceCounter = 1;

    private V8         v8;
    private int        objectHandle;

    protected V8Object() {
        v8 = (V8) this;
        objectHandle = 0;
    }

    public V8Object(final V8 v8) {
        this.v8 = v8;
        this.v8.checkThread();
        objectHandle = v8ObjectInstanceCounter++;
        this.v8._initNewV8Object(v8.getV8RuntimeHandle(), objectHandle);
        this.v8.addObjRef();
    }

    public int getHandle() {
        return objectHandle;
    }

    public void release() {
        v8.checkThread();
        v8._release(v8.getV8RuntimeHandle(), objectHandle);
        v8.releaseObjRef();
    }

    public boolean contains(final String key) {
        v8.checkThread();
        return v8._contains(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public String[] getKeys() {
        v8.checkThread();
        return v8._getKeys(v8.getV8RuntimeHandle());
    }

    public int getType(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return 0;
    }

    public int getInteger(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getInteger(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public boolean getBoolean(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getBoolean(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public double getDouble(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getDouble(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public String getString(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getString(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    public V8Array getArray(final String key) throws V8ResultUndefined {
        v8.checkThread();
        V8Array result = new V8Array(v8);
        try {
            v8._getArray(v8.getV8RuntimeHandle(), getHandle(), key, result.getHandle());
        } catch (Exception e) {
            result.release();
            throw e;
        }
        return result;
    }

    public V8Object getObject(final String key) throws V8ResultUndefined {
        v8.checkThread();
        V8Object result = new V8Object(v8);
        try {
            v8._getObject(v8.getV8RuntimeHandle(), objectHandle, key, result.getHandle());
        } catch (Exception e) {
            result.release();
            throw e;
        }
        return result;
    }

    public V8Array createParameterList(final int size) {
        v8.checkThread();
        return null;
    }

    public int executeIntFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        int parametersHandle = parameters == null ? -1 : parameters.getHandle();
        return v8._executeIntFunction(v8.getV8RuntimeHandle(), getHandle(), name, parametersHandle);
    }

    public double executeDoubleFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        return v8._executeDoubleFunction(v8.getV8RuntimeHandle(), name, null);
    }

    public String executeStringFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        return v8._executeStringFunction(v8.getV8RuntimeHandle(), name, null);
    }

    public boolean executeBooleanFunction(final String name, final V8Array parameters) throws V8ExecutionException,
    V8ResultUndefined {
        v8.checkThread();
        return v8._executeBooleanFunction(v8.getV8RuntimeHandle(), name, null);
    }

    public V8Array executeArrayFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        return null;
    }

    public V8Object executeObjectFunction(final String name, final V8Array parameters) throws V8ExecutionException,
    V8ResultUndefined {
        v8.checkThread();
        V8Object result = new V8Object(v8);
        try {
            v8._executeObjectFunction(v8.getV8RuntimeHandle(), objectHandle, name, null, result.getHandle());
        } catch (Exception e) {
            result.release();
            throw e;
        }
        return result;
    }

    public void executeVoidFunction(final String name, final V8Array parameters) throws V8ExecutionException {
        v8.checkThread();
        v8._executeVoidFunction(v8.getV8RuntimeHandle(), name, null);
    }

    public void add(final String key, final int value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
    }

    public void add(final String key, final boolean value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
    }

    public void add(final String key, final double value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
    }

    public void add(final String key, final String value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), objectHandle, key, value);
    }

    public void add(final String key, final V8Object value) {
        v8.checkThread();
        v8._addObject(v8.getV8RuntimeHandle(), objectHandle, key, value.getHandle());
    }

    public void add(final String key, final V8Array value) {
        v8.checkThread();
        v8._addArray(v8.getV8RuntimeHandle(), objectHandle, key, value.getHandle());
    }

    public void registerJavaMethod(final Object object, final String methodName, final Class<?>[] parameterTypes) {
        v8.checkThread();
    }

}
