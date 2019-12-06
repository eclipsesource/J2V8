package com.eclipsesource.v8.inspector;

import java.util.ArrayList;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ScriptExecutionException;

public class V8Inspector {

    private V8                runtime;
    private long              inspectorPtr         = 0;
    private boolean           waitingForConnection = true;
    private ArrayList<V8InspectorScript> scripts;

    public static V8Inspector createV8Inspector(final V8 runtime, final V8InspectorDelegate inspectorDelegate, final String contextName) {
        return new V8Inspector(runtime, inspectorDelegate, contextName);
    }

    public static V8Inspector createV8Inspector(final V8 runtime, final V8InspectorDelegate inspectorDelegate) {
        return new V8Inspector(runtime, inspectorDelegate, null);
    }

    public void dispatchProtocolMessage(final String protocolMessage) {
        try {
            runtime.dispatchProtocolMessage(inspectorPtr, protocolMessage);
            if (waitingForConnection) {
                V8Object json = runtime.executeObjectScript("JSON.parse(`" + protocolMessage + "`)");
                if (json.getString("method").equals("Runtime.runIfWaitingForDebugger")) {
                    waitingForConnection = false;
                    runtime.schedulePauseOnNextStatement(inspectorPtr, "");
                    executeScripts();
                }
            }
        } catch (V8ScriptExecutionException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addScript(final V8InspectorScript script) {
        scripts.add(script);
    }

    protected V8Inspector(final V8 runtime, final V8InspectorDelegate inspectorDelegate, final String contextName) {
        this.runtime = runtime;
        inspectorPtr = runtime.createInspector(inspectorDelegate, contextName);
        scripts = new ArrayList<V8InspectorScript>();
    }

    private void executeScripts() {
        for (final V8InspectorScript script : scripts) {
            runtime.executeScript(script.getScriptContent(), script.getScriptName(), 0);
        }
    }

}
