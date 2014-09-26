package com.eclipsesource.v8;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.eclipsesource.v8.utils.tests.V8ObjectUtilsTest;

@RunWith(Suite.class)
// V8RuntimeNotLoadedTest must be run first. This is because we need to test when the natives are not loaded
// and once the V8 class is loaded we cannot unload it.
@SuiteClasses({ V8RuntimeNotLoadedTest.class, V8ObjectTest.class, V8Test.class, V8ArrayTest.class, V8JSFunctionCallTest.class,
        V8CallbackTest.class, V8ParseExceptionTest.class, V8ObjectUtilsTest.class })
public class AllTests {

}
