package com.eclipsesource.v8.inspector;

public class V8InspectorScript {
    private String scriptContent = null;
    private String scriptName      = null;

    public V8InspectorScript(final String scriptContent, final String scriptName) {
        this.scriptContent = scriptContent;
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getScriptContent() {
        return scriptContent;
    }
}
