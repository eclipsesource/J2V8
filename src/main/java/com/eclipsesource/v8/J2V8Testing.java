package com.eclipsesource.v8;

public class J2V8Testing {

    public J2V8Testing() {
        V8 runtime = V8.createV8Runtime();
        runtime.registerJavaMethod(this::test, "test");
        runtime.registerJavaMethod(this::print, "print");
        runtime.executeVoidScript("test(function() { print('Hello World!'); });");
        runtime.release();
    }

    public void print(V8Object receiver, V8Array parameters) {
        System.out.println(parameters.get(0));
    }

    public void test(V8Object receiver, V8Array parameters) {
        V8Function cb = (V8Function) parameters.get(0);
        cb.call(receiver, null);
        cb.release();
    }

    public static void main(String[] args) {
        new J2V8Testing();
    }


}