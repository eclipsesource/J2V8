package com.eclipsesource.v8;


public interface V8Array {

    public void release();

    public int getSize();

    public int getValueType(int index);

    public int getInteger(int index);

    public boolean getBoolean(int index);

    public double getDouble(int index);

    public String getString(int index);

    public V8Array getArray(int index);

    public V8Object getObject(int index);

    public void add(int value);

    public void add(boolean value);

    public void add(double value);

    public void add(String value);

    public V8Object addObject();

    public V8Array addArray(int size);

}