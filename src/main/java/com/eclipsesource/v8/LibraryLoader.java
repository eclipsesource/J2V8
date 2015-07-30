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
package com.eclipsesource.v8;

import java.io.*;

class LibraryLoader {


    static void loadLibrary(File tempDirectory) throws IOException {
        String libraryName = OS.getCurrentOS().getType().getSharedLibraryPrefix() + "j2v8-" + Reference.V8_VERSION + "-" + Reference.J2V8_VERSION + "-" + OS.getCurrentOS().getType().getShortName() + "-" + OS.getCurrentOS().getArch().getName() + "." + OS.getCurrentOS().getType().getSharedLibraryExtension();
        System.out.println(libraryName);
        //File libraryFile = new File(tempDirectory, libraryName);
        //if (!isExtracted()) {
        //    extract(libraryName, libraryFile);
        //}
        //System.load(libraryFile.getAbsolutePath());
    }

    private static boolean isExtracted() {
        return false;
    }

    private static void extract(String resourceName, File outFile) throws IOException {
        if (outFile.exists()) {
            outFile.delete();
        }
        InputStream is = LibraryLoader.class.getResourceAsStream(resourceName); //$NON-NLS-1$
        if (is != null) {
            FileOutputStream os = new FileOutputStream(outFile);
            copy(is, os);
            os.close();
            is.close();
            chmod("755", outFile.getAbsolutePath());
        } else {
            throw new IllegalStateException("could not get resource " + resourceName);
        }
    }

    private static void chmod(String permision, String path) throws IOException {
        if (OS.getCurrentOS().getType() != OS.Type.WINDOWS) {
            try {
                Runtime.getRuntime().exec(new String[]{"chmod", permision, path}).waitFor(); //$NON-NLS-1$
            } catch (InterruptedException e) {
            }
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

}
