package com.eclipsesource.v8;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Proxies a static method of a Java class and makes it available to the V8 runtime.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
class V8JavaStaticMethodProxy extends V8JavaMethodProxy implements JavaCallback {
//Private//////////////////////////////////////////////////////////////////////

//Public///////////////////////////////////////////////////////////////////////

    public V8JavaStaticMethodProxy(String name) {
        super(name);
    }

    @Override public Object invoke(V8Object receiver, V8Array parameters) {
        //See if a method exists.
        Object[] coercedArguments = null;
        Method coercedMethod = null;
        for (Method method : getMethodSignatures()) {
            try {
                coercedArguments = V8JavaObjectUtils.translateJavascriptArgumentsToJava(method.isVarArgs(), method.getParameterTypes(), parameters, receiver);
                coercedMethod = method;
                break;
            } catch (IllegalArgumentException e) {

            }
        }

        if (coercedArguments == null) {
            throw new IllegalArgumentException("No method exists for specified parameters.");
        }

        //Invoke the method.
        try {
            return V8JavaObjectUtils.translateJavaArgumentToJavascript(coercedMethod.invoke(coercedArguments), V8JavaObjectUtils.getRuntimeSarcastically(receiver));
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Method received invalid arguments!");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Method received invalid arguments!");
        }
    }
}
