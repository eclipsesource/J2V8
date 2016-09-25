package com.eclipsesource.v8.engine;

import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static org.junit.Assert.*;

public class J2V8ScriptEngineTest {

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
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("J2V8");

        Integer val = (Integer) engine.eval("2 * 2");
        assertNotNull(val);
        assertEquals(4, val.intValue());
    }
}
