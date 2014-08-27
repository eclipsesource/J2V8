package com.eclipsesource.v8.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ V8ObjectTest.class, V8Test.class, V8ArrayTest.class, V8JSFunctionCallTest.class,
        V8CallbackTest.class })
public class AllTests {

}
