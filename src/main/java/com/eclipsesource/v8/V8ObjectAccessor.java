package com.eclipsesource.v8;

public interface V8ObjectAccessor {

    public V8ObjectAccessor getObject(final String key);

    public V8ArrayAccessor getArray(final String key);

    public String getString(final String key);

    public double getDouble(final String key);

    public boolean getBoolean(final String key);

    public int getInteger(final String key);

    public int getType(final String key);

    public String[] getKeys();
}
