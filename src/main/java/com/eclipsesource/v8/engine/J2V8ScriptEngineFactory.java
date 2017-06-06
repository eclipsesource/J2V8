package com.eclipsesource.v8.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Factory for <code>J2V8ScriptEngine</code>
 *
 * @author Boris Yakovito
 */
public class J2V8ScriptEngineFactory implements ScriptEngineFactory {

    private static final String ENGINE_NAME = "J2V8";

    private static final String ENGINE_VERSION = "4.5";

    private static final String LANGUAGE_NAME = "ECMAScript";

    private static final String LANGUAGE_VERSION = "ECMA - 262 Edition 6";

    private static final List<String> names = immutableList("J2V8", "j2v8");

    private static final List<String> mimeTypes = immutableList("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript");

    private static final List<String> extensions = immutableList("js");


    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String key) {
        if (key.equals(ScriptEngine.ENGINE)) {
            return getEngineName();
        }
        if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return getEngineVersion();
        }
        if (key.equals(ScriptEngine.LANGUAGE)) {
            return getLanguageName();
        }
        if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return getLanguageVersion();
        }
        if (key.equals(ScriptEngine.NAME)) {
            return getNames().get(0);
        }
        if (key.equals("THREADING")) {
            return "MULTITHREADED";
        }

        return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder ret = new StringBuilder();
        if (obj != null) {
            ret.append(".").append(m);
        }
        ret.append("(");
        for (int i = 0; i < args.length; i++) {
            ret.append(args[i] == null ? "null" : args[i]);
            if (i < args.length - 1) {
                ret.append(",");
            }
        }
        ret.append(")");
        return ret.toString();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "console.log(" + toDisplay + ");";
    }

    @Override
    public String getProgram(String... statements) {
        StringBuilder sb = new StringBuilder();
        for (String statement : statements) {
            sb.append(statement).append(";").append("\n");
        }
        return sb.toString();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new J2V8ScriptEngine(this);
    }

    /**
     * @param globalAlias The name to associate with the global scope.
     * @return
     */
    public ScriptEngine getScriptEngine(final String globalAlias, final String tempDirectory) {
        return new J2V8ScriptEngine(this, globalAlias, tempDirectory);
    }

    private static List<String> immutableList(String... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }
}
