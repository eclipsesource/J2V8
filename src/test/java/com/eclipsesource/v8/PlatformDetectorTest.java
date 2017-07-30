/*******************************************************************************
 * Copyright (c) 2017 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 *    Wolfgang Steiner - code separation PlatformDetector/LibraryLoader
 ******************************************************************************/
package com.eclipsesource.v8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PlatformDetectorTest {

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
    public void testGetOSUnknown() {
        System.setProperty("os.name", "???");
        System.setProperty("java.specification.vendor", "???");

        try {
            PlatformDetector.OS.getName();
        } catch (Error e) {
            assertTrue("Expected UnsatisfiedLinkError", e instanceof UnsatisfiedLinkError);
            assertTrue(e.getMessage().startsWith("Unsupported platform/vendor"));
            return;
        }
        fail("Expected exception");
    }

    @Test
    public void testGetOSMac() {
        System.setProperty("os.name", "Mac OS X");
        System.setProperty("java.specification.vendor", "Apple");

        assertEquals("macosx", PlatformDetector.OS.getName());
    }

    @Test
    public void testGetOSLinux() {
        System.setProperty("os.name", "Linux");
        System.setProperty("java.specification.vendor", "OSS");

        assertEquals("linux", PlatformDetector.OS.getName());
    }

    @Test
    public void testGetOSWindows() {
        System.setProperty("os.name", "Windows");
        System.setProperty("java.specification.vendor", "Microsoft");

        assertEquals("windows", PlatformDetector.OS.getName());
    }

    @Test
    public void testGetOSAndroid() {
        System.setProperty("os.name", "Linux");
        System.setProperty("java.specification.vendor", "The Android Project");

        assertEquals("android", PlatformDetector.OS.getName());
    }

    @Test
    public void testGetOSFileExtensionAndroid() {
        System.setProperty("os.name", "naclthe android project");
        System.setProperty("java.specification.vendor", "The Android Project");

        assertEquals("so", PlatformDetector.OS.getLibFileExtension());
    }

    @Test(expected = UnsatisfiedLinkError.class)
    public void testGetArchxNaCl() {
        System.setProperty("os.arch", "nacl");

        PlatformDetector.Arch.getName();
    }
    
    @Test
    public void testGetArchaarch64() {
        System.setProperty("os.arch", "aarch64");

        assertEquals("aarch_64", PlatformDetector.Arch.getName());
    }

    @Test
    public void testGetArchx86() {
        System.setProperty("os.arch", "x86");

        assertEquals("x86_32", PlatformDetector.Arch.getName());
    }

    @Test
    public void testGetArchx86_64() {
        System.setProperty("os.arch", "x86_64");

        assertEquals("x86_64", PlatformDetector.Arch.getName());
    }

    @Test
    public void testGetArchx64FromAmd64() {
        System.setProperty("os.arch", "amd64");

        assertEquals("x86_64", PlatformDetector.Arch.getName());
    }

    @Test(expected = UnsatisfiedLinkError.class)
    public void testGetArcharmv7l() {
        System.setProperty("os.arch", "armv7l");

        PlatformDetector.Arch.getName();
    }

    @Test
    public void test686isX86() {
        System.setProperty("os.arch", "i686");

        assertEquals("x86_32", PlatformDetector.Arch.getName());
    }

    @Test
    public void testVendor_Alpine() {
        if (!isAlpineLinux()) {
            return;
        }

        assertEquals("alpine", PlatformDetector.Vendor.getName());
    }

    private boolean isAlpineLinux() {
        return new File("/etc/alpine-release").exists();
    }
}
