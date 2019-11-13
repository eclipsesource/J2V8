package com.eclipsesource.v8.inspector;

public interface V8InspectorDelegate {
    public abstract void onResponse(String message);

    public abstract void waitFrontendMessageOnPause();
}
