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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class LibraryLoader {

    static final String SEPARATOR;
    static final String DELIMITER;

    static final String SWT_LIB_DIR = ".j2v8";

    static {
        DELIMITER = System.getProperty("line.separator"); //$NON-NLS-1$
        SEPARATOR = System.getProperty("file.separator"); //$NON-NLS-1$
    }

    private static String computeLibraryShortName() {
        String base = "j2v8";
        String osSuffix = getOS();
        String archSuffix = getArchSuffix();
        return base + "_" + osSuffix + "_" + archSuffix;
    }

    private static String computeLibraryFullName() {
        return "lib" + computeLibraryShortName() + "." + getOSFileExtension();
    }

    static void loadLibrary(final String tempDirectory) {
        if ( isAndroid() ) {
          System.loadLibrary("j2v8");
          return;
        }
        StringBuffer message = new StringBuffer();
        String libShortName = computeLibraryShortName();
        String libFullName = computeLibraryFullName();
        String ideLocation = System.getProperty("user.dir") + SEPARATOR + "jni" + SEPARATOR + computeLibraryFullName();
        String path = null;

        /* Try loading library from java library path */
        if (load(libShortName, message)) {
            return;
        }

        /* Try loading library from the IDE location */
        if (new File(ideLocation).exists()) {
            if (load(ideLocation, message)) {
                return;
            }
        }

        if (tempDirectory != null) {
            path = tempDirectory;
        } else {
            path = System.getProperty("user.home"); //$NON-NLS-1$
        }

        if (extract(path + SEPARATOR + libFullName, libFullName, message)) {
            return;
        }

        /* Failed to find the library */
        throw new UnsatisfiedLinkError("Could not load J2V8 library. Reasons: " + message.toString()); //$NON-NLS-1$
    }

    static boolean load(final String libName, final StringBuffer message) {
        try {
            if (libName.indexOf(SEPARATOR) != -1) {
                System.load(libName);
            } else {
                System.loadLibrary(libName);
            }
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (message.length() == 0) {
                message.append(DELIMITER);
            }
            message.append('\t');
            message.append(e.getMessage());
            message.append(DELIMITER);
        }
        return false;
    }

    static boolean extract(final String fileName, final String mappedName, final StringBuffer message) {
        FileOutputStream os = null;
        InputStream is = null;
        File file = new File(fileName);
        boolean extracted = false;
        try {
            if (file.exists()) {
                file.delete();
            }
            is = LibraryLoader.class.getResourceAsStream("/" + mappedName); //$NON-NLS-1$
            if (is != null) {
                extracted = true;
                int read;
                byte[] buffer = new byte[4096];
                os = new FileOutputStream(fileName);
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.close();
                is.close();
                chmod("755", fileName);
                if (load(fileName, message)) {
                    return true;
                }
            }
        } catch (Throwable e) {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e1) {
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e1) {
            }
            if (extracted && file.exists()) {
                file.delete();
            }
        }
        return false;
    }

    static void chmod(final String permision, final String path) {
        if (isWindows()) {
            return;
        }
        try {
            Runtime.getRuntime().exec(new String[] { "chmod", permision, path }).waitFor(); //$NON-NLS-1$
        } catch (Throwable e) {
        }
    }

    static String getOsName() {
        return System.getProperty("os.name") + System.getProperty("java.specification.vendor");
    }

    static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }

    static boolean isMac() {
        return getOsName().startsWith("Mac");
    }

    static boolean isLinux() {
        return getOsName().startsWith("Linux");
    }

    static boolean isNativeClient() {
        return getOsName().startsWith("nacl");
    }

    static boolean isAndroid() {
        return getOsName().contains("Android");
    }

    static String getArchSuffix() {
        String arch = System.getProperty("os.arch");
        if (arch.equals("i686")) {
            return "x86";
        } else if (arch.equals("amd64")) {
            return "x86_64";
        } else if (arch.equals("nacl")) {
            return "armv7l";
        } else if (arch.equals("aarch64")) {
            return "armv7l";
        }
        return arch;
    }

    static String getOSFileExtension() {
        if (isWindows()) {
            return "dll";
        } else if (isMac()) {
            return "dylib";
        } else if (isLinux()) {
            return "so";
        } else if (isNativeClient()) {
            return "so";
        }
        throw new UnsatisfiedLinkError("Unsupported platform: " + getOsName());
    }

    static String getOS() {
        if (isWindows()) {
            return "win32";
        } else if (isMac()) {
            return "macosx";
        } else if (isLinux() && !isAndroid()) {
            return "linux";
        } else if (isAndroid()) {
            return "android";
        }
        throw new UnsatisfiedLinkError("Unsupported platform: " + getOsName());
    }

}
