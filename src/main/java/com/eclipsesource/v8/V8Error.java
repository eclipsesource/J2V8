package com.eclipsesource.v8;

public class V8Error extends Error implements Releasable {

    private static final long serialVersionUID = 1L;
    private V8Object          object;

    public V8Error(final V8Object object) {
        this.object = object.twin();
    }

    protected V8Object getObject() {
        return object;
    }

    protected long getObjectHandle() {
        return object.getHandle();
    }

    @Override
    public void release() {
        if (!object.isReleased()) {
            object.release();
        }
    }

}
