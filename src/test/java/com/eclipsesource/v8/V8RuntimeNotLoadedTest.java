/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.net.URLClassLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.eclipsesource.v8.V8RuntimeNotLoadedTest.SeparateClassloaderTestRunner;

// A separate class loaded must be used since we don't want these tests to interfere
// with other tests.
@RunWith(SeparateClassloaderTestRunner.class)
public class V8RuntimeNotLoadedTest {

    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    private String              existingLibraryPath;

    @Before
    public void before() throws Exception {
        existingLibraryPath = System.getProperty(JAVA_LIBRARY_PATH);
        setLibraryPath("");
    }

    @After
    public void after() throws Exception {
        setLibraryPath(existingLibraryPath);
    }

    @Test
    public void testJ2V8NotEnabled() {
        assertFalse(V8.isLoaded());
    }

    @Test(expected = IllegalStateException.class)
    public void testJ2V8CannotCreateRuntime() {
        String oldValue = System.getProperty("os.arch");
        System.setProperty("os.arch", "unknown");
        try {
            V8.createV8Runtime();
        } finally {
            System.setProperty("os.arch", oldValue);
        }
    }

    private static void setLibraryPath(final String path) throws Exception {
        System.setProperty(JAVA_LIBRARY_PATH, path);

        // set sys_paths to null so that java.library.path will be reevalueted next time it is needed
        final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
        sysPathsField.setAccessible(true);
        sysPathsField.set(null, null);
    }

    public static class SeparateClassloaderTestRunner extends BlockJUnit4ClassRunner {

        public SeparateClassloaderTestRunner(final Class<?> clazz) throws InitializationError {
            super(getFromTestClassloader(clazz));
        }

        private static Class<?> getFromTestClassloader(final Class<?> clazz) throws InitializationError {
            try {
                ClassLoader testClassLoader = new TestClassLoader();
                return Class.forName(clazz.getName(), true, testClassLoader);
            } catch (ClassNotFoundException e) {
                throw new InitializationError(e);
            }
        }

        public static class TestClassLoader extends URLClassLoader {
            public TestClassLoader() {
                super(((URLClassLoader) getSystemClassLoader()).getURLs());
            }

            @Override
            public Class<?> loadClass(final String name) throws ClassNotFoundException {
                if (name.startsWith("com.eclipsesource.v8")) {
                    return super.findClass(name);
                }
                return super.loadClass(name);
            }
        }
    }
}
