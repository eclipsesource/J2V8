/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Contributors:
 * EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8.utils;

import com.eclipsesource.v8.Reference;

import java.io.*;
import java.nio.channels.FileChannel;

public class LibraryLoader {

    public static void loadLibrary(File tempDirectory) throws IOException {
        tempDirectory.mkdirs();
        String libraryName = OS.getCurrentOS().getType().getSharedLibraryPrefix() + "j2v8-" + Reference.V8_VERSION + "-" + Reference.J2V8_VERSION + "-" + OS.getCurrentOS().getType().getShortName() + "-" + OS.getCurrentOS().getArch().getName() + "." + OS.getCurrentOS().getType().getSharedLibraryExtension();
        File libraryFile = new File(tempDirectory, libraryName);
        if (!isExtracted()) {
            if (!extractFromIDE(libraryName, libraryFile)) {
                extractFromResources(libraryName, libraryFile);
            }
        }
        System.load(libraryFile.getAbsolutePath());
    }

    private static boolean isExtracted() {
        return false;
    }

    private static boolean extractFromIDE(String libraryName, File destFile) throws IOException {
        File srcFile = new File("native/lib/", libraryName);
        if (!srcFile.exists()) {
            return false;
        } else {
            if (destFile.exists()) {
                destFile.delete();
            }
            copyFile(srcFile, destFile);
            return true;
        }
    }

    private static void extractFromResources(String libraryName, File destFile) throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }
        InputStream is = LibraryLoader.class.getResourceAsStream(libraryName); //$NON-NLS-1$
        if (is != null) {
            FileOutputStream os = new FileOutputStream(destFile);
            copy(is, os);
            os.close();
            is.close();
            destFile.setExecutable(true);
        } else {
            throw new IllegalStateException("Library " + libraryName + " doesn't exist! Maybe the current OS is not supported!");
        }
    }

    public static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destFile).getChannel();
        if (source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        destination.close();
    }

}
