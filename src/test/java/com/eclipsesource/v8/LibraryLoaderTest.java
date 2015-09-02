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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

}
