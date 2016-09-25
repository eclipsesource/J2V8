package com.eclipsesource.v8;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic base for proxying static and instance Java methods.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
abstract class V8JavaMethodProxy {
//Private//////////////////////////////////////////////////////////////////////

    private final String name;
    private final List<Method> methodSignatures = new ArrayList<Method>();

//Public///////////////////////////////////////////////////////////////////////

    public V8JavaMethodProxy(String name) {
        this.name = name;
    }

    /**
     * Associates a new Java method signature with this proxy.
     *
     * @param method Method signature to add.
     */
    public void addMethodSignature(Method method) {
        methodSignatures.add(method);
    }

    /**
     * @return Unmodifiable list of all possible argument signatures (methods)
     *         for the Java method represented by this proxy.
     */
    public List<Method> getMethodSignatures() {
        return Collections.unmodifiableList(methodSignatures);
    }

    /**
     * @return Name of the Java method represented by this proxy.
     */
    public String getMethodName() {
        return name;
    }
}
