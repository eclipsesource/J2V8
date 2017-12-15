/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LibraryLoaderTest {

    private String osName;
    private String vendor;
    private String arch;

    @Before
    public void setup() {
        osName = System.getProperty("os.name");
        vendor = System.getProperty("java.specification.vendor");
        arch = System.getProperty("os.arch");
    }

    @After
    public void tearDown() {
        System.setProperty("os.name", osName);
        System.setProperty("java.specification.vendor", vendor);
        System.setProperty("os.arch", arch);
    }

    @Test
    public void testGetOSMac() {
        System.setProperty("os.name", "Mac OS X");

        assertEquals("macosx", LibraryLoader.getOS());
    }

    @Test
    public void testGetOSLinux() {
        System.setProperty("os.name", "Linux");

        assertEquals("linux", LibraryLoader.getOS());
    }

    @Test
    public void testGetOSWindows() {
        System.setProperty("os.name", "Windows");

        assertEquals("win32", LibraryLoader.getOS());
    }

    @Test
    public void testGetOSAndroid() {
        System.setProperty("os.name", "Linux");
        System.setProperty("java.specification.vendor", "The Android Project");

        assertEquals("android", LibraryLoader.getOS());
    }

    @Test
    public void testGetOSFileExtensionNativeClient() {
        System.setProperty("os.name", "naclthe android project");
        System.setProperty("java.specification.vendor", "The Android Project");

        assertEquals("so", LibraryLoader.getOSFileExtension());
    }

    @Test
    public void testGetArchxNaCl() {
        System.setProperty("os.arch", "nacl");

        assertEquals("armv7l", LibraryLoader.getArchSuffix());
    }
    
    @Test
    public void testGetArchaarch64() {
        System.setProperty("os.arch", "aarch64");

        assertEquals("armv7l", LibraryLoader.getArchSuffix());
    }

    @Test
    public void testGetArchx86() {
        System.setProperty("os.arch", "x86");

        assertEquals("x86", LibraryLoader.getArchSuffix());
    }

    @Test
    public void testGetArchx86_64() {
        System.setProperty("os.arch", "x86_64");

        assertEquals("x86_64", LibraryLoader.getArchSuffix());
    }

    @Test
    public void testGetArchx64FromAmd64() {
        System.setProperty("os.arch", "amd64");

        assertEquals("x86_64", LibraryLoader.getArchSuffix());
    }

    @Test
    public void testGetArcharmv7l() {
        System.setProperty("os.arch", "armv7l");

        assertEquals("armv7l", LibraryLoader.getArchSuffix());
    }

    @Test
    public void test686isX86() {
        System.setProperty("os.arch", "i686");

        assertEquals("x86", LibraryLoader.getArchSuffix());
    }

}
