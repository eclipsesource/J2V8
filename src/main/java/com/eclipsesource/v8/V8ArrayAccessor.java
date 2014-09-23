package com.eclipsesource.v8;

public interface V8ArrayAccessor extends V8ObjectAccessor {

    public V8Object getObject(final int index);

    public V8Array getArray(final int index);

    public String getString(final int index);

    public double getDouble(final int index);

    public boolean getBoolean(final int index);

    public int getInteger(final int index);

    public int getType(final int index);

}
