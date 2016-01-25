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
package com.eclipsesource.v8.debug;

import com.eclipsesource.v8.V8Object;

public interface BreakHandler {

    public void onBreak(int event, V8Object state, V8Object eventData, V8Object data);

}
