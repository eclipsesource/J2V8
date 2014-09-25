package com.eclipsesource.v8;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.eclipsesource.v8.utils.tests.V8ObjectUtilsTest;

@RunWith(Suite.class)
@SuiteClasses({ V8ObjectTest.class, V8Test.class, V8ArrayTest.class, V8JSFunctionCallTest.class,
    V8CallbackTest.class, V8ParseExceptionTest.class, V8ObjectUtilsTest.class })
public class AllTests {

}
