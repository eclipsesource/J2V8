
package com.eclipsesource.v8;

public class PersistentV8Object implements V8Object {

    private static int v8ObjectInstanceCounter = 1;

    private V8         v8;
    private int        objectHandle;

    protected PersistentV8Object() {
        v8 = (V8) this;
        objectHandle = 0;
    }

    public PersistentV8Object(final V8 v8) {
        this.v8 = v8;
        this.v8.checkThread();
        objectHandle = v8ObjectInstanceCounter++;
        this.v8._initNewV8Object(v8.getV8RuntimeHandle(), objectHandle);
        this.v8.addObjRef();
    }

    public int getHandle() {
        return objectHandle;
    }

    @Override
    public void release() {
        v8.checkThread();
        v8._release(v8.getV8RuntimeHandle(), objectHandle);
        v8.releaseObjRef();
    }

    @Override
    public boolean contains(final String key) {
        v8.checkThread();
        return v8._contains(v8.getV8RuntimeHandle(), objectHandle, key);
    }

    @Override
    public String[] getKeys() {
        v8.checkThread();
        return v8._getKeys(v8.getV8RuntimeHandle());
    }

    @Override
    public int getType(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return 0;
    }

    @Override
    public int getInteger(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getInteger(v8.getV8RuntimeHandle(), key);
    }

    @Override
    public boolean getBoolean(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getBoolean(v8.getV8RuntimeHandle(), key);
    }

    @Override
    public double getDouble(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getDouble(v8.getV8RuntimeHandle(), key);
    }

    @Override
    public String getString(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return v8._getString(v8.getV8RuntimeHandle(), key);
    }

    @Override
    public V8Array getArray(final String key) throws V8ResultUndefined {
        v8.checkThread();
        return null;
    }

    @Override
    public V8Object getObject(final String key) throws V8ResultUndefined {
        v8.checkThread();
        PersistentV8Object result = new PersistentV8Object(v8);
        v8._getObject(v8.getV8RuntimeHandle(), objectHandle, key, result.getHandle());
        return result;
    }

    @Override
    public V8Array createParameterList(final int size) {
        v8.checkThread();
        return null;
    }

    @Override
    public int executeIntFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        return v8._executeIntFunction(v8.getV8RuntimeHandle(), name, null);
    }

    @Override
    public double executeDoubleFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        return v8._executeDoubleFunction(v8.getV8RuntimeHandle(), name, null);
    }

    @Override
    public String executeStringFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        return v8._executeStringFunction(v8.getV8RuntimeHandle(), name, null);
    }

    @Override
    public boolean executeBooleanFunction(final String name, final V8Array parameters) throws V8ExecutionException,
    V8ResultUndefined {
        v8.checkThread();
        return v8._executeBooleanFunction(v8.getV8RuntimeHandle(), name, null);
    }

    @Override
    public V8Array executeArrayFunction(final String name, final V8Array parameters) throws V8ExecutionException, V8ResultUndefined {
        v8.checkThread();
        return null;
    }

    @Override
    public V8Object executeObjectFunction(final String name, final V8Array parameters) throws V8ExecutionException,
    V8ResultUndefined {
        v8.checkThread();
        return null;
    }

    @Override
    public void executeVoidFunction(final String name, final V8Array parameters) throws V8ExecutionException {
        v8.checkThread();
        v8._executeVoidFunction(v8.getV8RuntimeHandle(), name, null);
    }

    @Override
    public void add(final String key, final int value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), key, value);
    }

    @Override
    public void add(final String key, final boolean value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), key, value);
    }

    @Override
    public void add(final String key, final double value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), key, value);
    }

    @Override
    public void add(final String key, final String value) {
        v8.checkThread();
        v8._add(v8.getV8RuntimeHandle(), key, value);
    }

    @Override
    public V8Object addObject(final String key) {
        v8.checkThread();
        return null;
    }

    @Override
    public V8Array addArray(final String key, final int size) {
        v8.checkThread();
        return null;
    }

    @Override
    public void registerJavaMethod(final Object object, final String methodName, final Class<?>[] parameterTypes) {
        v8.checkThread();
    }

}
