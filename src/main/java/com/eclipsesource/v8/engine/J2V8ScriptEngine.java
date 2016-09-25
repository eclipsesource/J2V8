package com.eclipsesource.v8.engine;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;

/**
 * ScriptEngine implementation for J2V8
 *
 * @author Boris Yakovito
 */
public class J2V8ScriptEngine extends AbstractScriptEngine implements Invocable {

    private final ScriptEngineFactory factory;
    private final V8 runtime;

    /**
     * @param factory <code>ScriptEngineFactory</code> for the class to which this <code>ScriptEngine</code> belongs.
     */
    public J2V8ScriptEngine(final ScriptEngineFactory factory) {
        this(factory, null, null);
    }

    /**
     * @param factory       <code>ScriptEngineFactory</code> for the class to which this <code>ScriptEngine</code> belongs.
     * @param globalAlias   The name to associate with the global scope.
     * @param tempDirectory The name of the directory to extract the native
     *                      libraries too.
     */
    public J2V8ScriptEngine(final ScriptEngineFactory factory, final String globalAlias, final String tempDirectory) {
        this.factory = factory;
        this.runtime = V8.createV8Runtime(globalAlias, tempDirectory);
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (thiz == null || !(thiz instanceof V8Object)) {
            throw new IllegalArgumentException("Non-null V8Object instance expected");
        }
        return ((V8Object) thiz).executeJSFunction(name, args);
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return runtime.executeJSFunction(name, args);
    }

    @Override
    public <T> T getInterface(Class<T> clasz) {
        throw new IllegalStateException("Method not implemented. Cannot construct interface type: " + clasz);
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        throw new IllegalStateException("Method not implemented. Cannot construct interface type: " + clasz);
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        return runtime.executeScript(script);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        try {
            return eval(toString(reader), context);
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return this.factory;
    }

    private String toString(Reader reader) throws IOException {
        int read;
        char[] buffer = new char[8192];

        StringBuilder sb = new StringBuilder();
        while ((read = reader.read(buffer)) > 0) {
            sb.append(buffer, 0, read);
        }

        return sb.toString();
    }
}
