package com.eclipsesource.v8.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.V8;

import static org.junit.Assert.assertNotNull;

public class V8Tests {

    private V8 v8;

    @Before
    public void seutp() {
        v8 = V8.createV8Isolate();
    }

    @After
    public void tearDown() {
        v8.release();
    }

    @Test
    public void testV8Setup() {
        assertNotNull(v8);
    }

}
