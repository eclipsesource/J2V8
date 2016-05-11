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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.eclipsesource.v8.debug.BreakEventTest;
import com.eclipsesource.v8.debug.DebugHandlerTest;
import com.eclipsesource.v8.debug.ExecutionStateTest;
import com.eclipsesource.v8.debug.FrameTest;
import com.eclipsesource.v8.debug.MirrorTest;
import com.eclipsesource.v8.debug.ScopeTest;
import com.eclipsesource.v8.debug.ScriptBreakPointTest;
import com.eclipsesource.v8.utils.V8ExecutorTest;
import com.eclipsesource.v8.utils.V8MapTest;
import com.eclipsesource.v8.utils.V8ObjectUtilsTest;
import com.eclipsesource.v8.utils.V8PropertyMapTest;

@RunWith(Suite.class)
// V8RuntimeNotLoadedTest must be run first. This is because we need to test when the natives are not loaded
// and once the V8 class is loaded we cannot unload it.
@SuiteClasses({ V8RuntimeNotLoadedTest.class, LibraryLoaderTest.class, V8ObjectTest.class, V8Test.class, V8ArrayTest.class, V8JSFunctionCallTest.class,
        V8CallbackTest.class, V8ScriptCompilationExceptionTest.class, V8ScriptExecutionExceptionTest.class, V8ObjectUtilsTest.class, V8TypedArraysTest.class,
        NullScriptExecuteTest.class, V8MultiThreadTest.class, V8LockerTest.class, V8ExecutorTest.class, V8MapTest.class, V8PropertyMapTest.class,
        DebugHandlerTest.class, ExecutionStateTest.class, FrameTest.class, ScopeTest.class, ScriptBreakPointTest.class, MirrorTest.class, BreakEventTest.class, NodeJSTest.class })
public class AllTests {

}
