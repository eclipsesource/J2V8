package com.eclipsesource.v8.engine;

import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static org.junit.Assert.*;

public class J2V8ScriptEngineTest {

    private ScriptEngine engine;

    @Before
    public void before() throws Exception {
        engine = new ScriptEngineManager().getEngineByName("J2V8");
    }

    @Test
    public void testGetEngineByName() {
        ScriptEngineManager manager = new ScriptEngineManager();

        ScriptEngine engine = manager.getEngineByName("J2V8");
        assertTrue(engine != null);

        engine = manager.getEngineByName("j2v8");
        assertTrue(engine != null);
    }

    @Test
    public void testEvalBasic() throws Exception {
        Integer val = (Integer) engine.eval("2 * 2");
        assertNotNull(val);
        assertEquals(4, val.intValue());
    }

    @Test
    public void testEngineScopeBinding() throws Exception {
        engine.put("var", 21);

        Object val = engine.get("var");
        assertNotNull(val);
        assertTrue(val instanceof Integer);
        assertEquals(21, val);
    }
}
