package com.eclipsesource.v8;

public interface WeakReferenceHandler {
    void v8WeakReferenceCollected(V8Value weakRef);
}
