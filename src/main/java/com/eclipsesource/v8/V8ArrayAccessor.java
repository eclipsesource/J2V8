package com.eclipsesource.v8;

public interface V8ArrayAccessor extends V8ObjectAccessor {

    public V8ObjectAccessor getObject(final int index);

    public V8ArrayAccessor getArray(final int index);

    public String getString(final int index);

    public double getDouble(final int index);

    public boolean getBoolean(final int index);

    public int getInteger(final int index);

    public int getType(final int index);

    public int length();

    public String[] getStrings(final int index, final int length);

    public boolean[] getBooleans(final int index, final int length);

    public double[] getDoubles(final int index, final int length);

    public int[] getInts(final int index, final int length);

}
