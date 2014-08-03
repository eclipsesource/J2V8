package com.eclipsesource.v8;


public abstract class V8 implements V8Object {

    public static V8 createV8Isolate() {
        return new V8Impl();
    }

    public abstract int executeIntScript(String script, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public abstract double executeDoubleScript(String script, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public abstract String executeStringScript(String script, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public abstract boolean executeBooleanScript(String script, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public abstract V8Array executeArrayScript(String script, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public abstract V8Object executeObjectScript(String script, V8Array parameters) throws V8ExecutionException,
            V8ResultUndefined;

    public abstract void executeVoidScript(String script, V8Array parameters) throws V8ExecutionException;

}
