/*******************************************************************************
 * Copyright (c) 2016 Alicorn Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alicorn Systems - initial API and implementation and/or initial documentation
 ******************************************************************************/
package com.eclipsesource.v8;

/**
 * Simple runnable for use with an {@link ConcurrentV8} instance.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public interface ConcurrentV8Runnable {
    void run(final V8 v8) throws Exception;
}
