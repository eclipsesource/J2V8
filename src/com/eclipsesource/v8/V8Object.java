package com.eclipsesource.v8;

import java.util.Collection;

public interface V8Object {

    public final int INTEGER       = 1;
    public final int DOUBLE        = 2;
    public final int BOOLEAN       = 3;
    public final int STRING        = 4;
    public final int INTEGER_ARRAY = 5;
    public final int DOUBLE_ARRAY  = 6;
    public final int BOOLEAN_ARRAY = 7;
    public final int STRING_ARRAY  = 8;
    public final int V8_ARRAY      = 9;
    public final int V8_OBJECT     = 10;

    public void release();

    public boolean contains(String key);

    public Collection<String> getKeys();

    public int getType(String key) throws V8ResultUndefined;

    public int getInteger(String key) throws V8ResultUndefined;

    public boolean getBoolean(String key) throws V8ResultUndefined;

    public double getDouble(String key) throws V8ResultUndefined;

    public String getString(String key) throws V8ResultUndefined;

    public V8Array getArray(String key) throws V8ResultUndefined;

    public V8Object getObject(String key) throws V8ResultUndefined;

    public V8Array createParameterList(int size);

    public int executeIntFunction(String name, V8Array parameters) throws V8ExecutionException, V8ResultUndefined;

    public double executeDoubleFunction(String name, V8Array parameters) throws V8ExecutionException, V8ResultUndefined;

    public String executeStringFunction(String name, V8Array parameters) throws V8ExecutionException, V8ResultUndefined;

    public boolean executeBooleanFunction(String name, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public V8Array executeArrayFunction(String name, V8Array parameters) throws V8ExecutionException, V8ResultUndefined;

    public V8Object executeObjectFunction(String name, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public void executeVoidFunction(String name, V8Array parameters) throws V8ExecutionException;

    public void add(String key, int value);

    public void add(String key, boolean value);

    public void add(String key, double value);

    public void add(String key, String value);

    public V8Object addObject(String key);

    public V8Array addArray(String key, int size);

    public void registerJavaMethod(final Object object, final String methodName, final Class<?>[] parameterTypes);

}