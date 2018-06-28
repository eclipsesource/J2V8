/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
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
import java.io.PrintWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LibraryLoaderTest {

    private String osName;
    private String vendor;
    private String arch;

    private Field releaseFilesField;
    private String[] releaseFiles;

    static void makeFinalStaticAccessible(Field field) {
        field.setAccessible(true);

        try {
            // on certain JVMs this is not present and will throw the exceptions below (e.g. the Android Dalvik VM)
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
        catch (NoSuchFieldException e) {}
        catch (IllegalAccessException e) {}
    }

    @Before
    public void setup() throws Exception {
        osName = System.getProperty("os.name");
        vendor = System.getProperty("java.specification.vendor");
        arch = System.getProperty("os.arch");

        Class<?> vendorClass = PlatformDetector.Vendor.class;
        releaseFilesField = vendorClass.getDeclaredField("LINUX_OS_RELEASE_FILES");
        makeFinalStaticAccessible(releaseFilesField);

        releaseFiles = (String[])releaseFilesField.get(null);
    }

    @After
    public void tearDown() throws Exception {
        System.setProperty("os.name", osName);
        System.setProperty("java.specification.vendor", vendor);
        System.setProperty("os.arch", arch);

        releaseFilesField.set(null, releaseFiles);
    }

    @Test
    public void testAndroidLibNameStructure() throws Exception {
        System.setProperty("os.name", "Android");
        System.setProperty("java.specification.vendor", "...");
        System.setProperty("os.arch", "x64");

        performTests(Platform.ANDROID, null, ".so");

        System.setProperty("os.name", "...");
        System.setProperty("java.specification.vendor", "Android");
        System.setProperty("os.arch", "x64");

        performTests(Platform.ANDROID, null, ".so");
    }

    @Test
    public void testLinuxLibNameStructure() throws Exception {

        // skip this test on android
        if (PlatformDetector.OS.isAndroid())
            return;

        System.setProperty("os.name", "Linux");
        System.setProperty("java.specification.vendor", "OSS");
        System.setProperty("os.arch", "x64");

        final String os_release_test_path = "./test-mockup-os-release";
        final String test_vendor = "linux_vendor";

        // mock /etc/os-release file
        releaseFilesField.set(null, new String[] { os_release_test_path });

        PrintWriter out = new PrintWriter(os_release_test_path);
        out.println(
            "NAME=The-Linux-Vendor\n" +
            "VERSION=\"towel_42\"\n" +
            "ID=" + test_vendor + "\n" +
            "VERSION_ID=42\n"
        );
        out.close();

        performTests(Platform.LINUX, test_vendor, ".so");
    }

    @Test
    public void testMacOSXLibNameStructure() throws Exception {
        System.setProperty("os.name", "MacOSX");
        System.setProperty("java.specification.vendor", "Apple");
        System.setProperty("os.arch", "x64");

        performTests(Platform.MACOSX, null, ".dylib");
    }

    @Test
    public void testWindowsLibNameStructure() throws Exception {
        System.setProperty("os.name", "Windows");
        System.setProperty("java.specification.vendor", "Microsoft");
        System.setProperty("os.arch", "x64");

        performTests(Platform.WINDOWS, null, ".dll");
    }

    private void performTests(String expectedOsName, String expectedVendor, String expectedLibExtension) {
        // API calls
        String libName = LibraryLoader.computeLibraryShortName(true);
        String[] parts = libName.split("-");

        // test assertions
        int i = 0;
        int expectedParts = expectedVendor != null ? 4 : 3;
        assertEquals(expectedParts, parts.length);
        assertEquals("j2v8", parts[i++]);
        if (expectedVendor != null)
            assertEquals(expectedVendor, parts[i++]);
        assertEquals(expectedOsName, parts[i++]);
        assertEquals("x86_64", parts[i++]);

        // API calls
        libName = LibraryLoader.computeLibraryShortName(false);
        parts = libName.split("-");

        // test assertions
        assertEquals(3, parts.length);
        assertEquals("j2v8", parts[0]);
        assertEquals(expectedOsName, parts[1]);
        assertEquals("x86_64", parts[2]);

        // API calls
        libName = LibraryLoader.computeLibraryFullName(false);

        // test assertions
        assertTrue(libName.startsWith("libj2v8"));
        assertTrue(libName.endsWith(expectedLibExtension));
    }
}
