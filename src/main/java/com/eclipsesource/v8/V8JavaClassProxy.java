package com.eclipsesource.v8;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Represents a proxy of a Java class for use within a V8 javascript context.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
final class V8JavaClassProxy implements JavaCallback {
//Private//////////////////////////////////////////////////////////////////////

    //Class represented by this proxy.
    private final Class<?> classy;
    private final V8JavaClassInterceptor interceptor;

    //Intercepted contexts owned by this proxy.
    private final Map<String, V8JavaClassInterceptorContext> interceptContexts = new HashMap<String, V8JavaClassInterceptorContext>();

    //Methods owned by this proxy.
    private final Map<String, V8JavaStaticMethodProxy> staticMethods = new HashMap<String, V8JavaStaticMethodProxy>();
    private final Map<String, V8JavaInstanceMethodProxy> instanceMethods = new HashMap<String, V8JavaInstanceMethodProxy>();

    //Instances of this proxy created from JS. Used to control garbage collection.
    private final List<Object> jsObjects = new ArrayList<Object>(); {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                // When the application exits, make sure no instances remained.
                if (jsObjects.size() > 0) {
                    System.err.println(jsObjects.size() + " instance(s) of " + classy.getName() +
                                       " were created from JavaScript and not released via $release.");
                }
            }
        }));
    }

//Protected////////////////////////////////////////////////////////////////////

    /**
     * @return All static methods associated with the class this proxy represents.
     */
    List<V8JavaStaticMethodProxy> getStaticMethods() {
        return new ArrayList<V8JavaStaticMethodProxy>(staticMethods.values());
    }

//Public///////////////////////////////////////////////////////////////////////

    public V8JavaClassProxy(Class<?> classy, V8JavaClassInterceptor interceptor) {
        this.classy = classy;
        this.interceptor = interceptor;

        // TODO: Do we want to cache methods from non-final classes to reduce
        //       the memory footprint of multiple classes with a common base?

        // Get all public methods for the given class.
        for (Method m : classy.getMethods()) {
            // We want to ignore any methods from the base object class for now since that
            // will take up excess memory for potentially unused features.
            if (!m.getDeclaringClass().equals(Object.class)) {
                if (Modifier.isStatic(m.getModifiers())) {
                    if (staticMethods.containsKey(m.getName())) {
                        staticMethods.get(m.getName()).addMethodSignature(m);
                    } else {
                        V8JavaStaticMethodProxy methodProxy = new V8JavaStaticMethodProxy(m.getName());
                        methodProxy.addMethodSignature(m);
                        staticMethods.put(m.getName(), methodProxy);
                    }
                } else {
                    if (instanceMethods.containsKey(m.getName())) {
                        instanceMethods.get(m.getName()).addMethodSignature(m);
                    } else {
                        V8JavaInstanceMethodProxy methodProxy = new V8JavaInstanceMethodProxy(m.getName());
                        methodProxy.addMethodSignature(m);
                        instanceMethods.put(m.getName(), methodProxy);
                    }
                }
            }
        }
    }

    /**
     * Returns the {@link V8JavaClassInterceptor} associated with this class.
     *
     * @return The {@link V8JavaClassInterceptor} associated with this class,
     *         or null if none exists.
     */
    public V8JavaClassInterceptor getInterceptor() {
        return interceptor;
    }

    /**
     * Updates an {@link V8Object}'s state to match that of it's associated
     * {@link V8JavaClassInterceptor}.
     *
     * This method will do nothing if the specified V8Object does not have a
     * Java object handle or Java class interceptor handle.
     *
     * @param jsObject V8Object to restore from Java.
     */
    public void writeInjectedInterceptor(V8Object jsObject) {
        Object obj = jsObject.get(V8JavaObjectUtils.JAVA_CLASS_INTERCEPTOR_CONTEXT_HANDLE_ID);
        if (obj instanceof V8Value && ((V8Value) obj).isUndefined()) {
            ((V8Value) obj).release();
            return;
        }
        String interceptorAddress = String.valueOf(obj);

        obj = jsObject.get(V8JavaObjectUtils.JAVA_OBJECT_HANDLE_ID);
        if (obj instanceof V8Value && ((V8Value) obj).isUndefined()) {
            ((V8Value) obj).release();
            return;
        }
        String objectAddress = String.valueOf(obj);

        Object javaObject = V8JavaCache.identifierToV8ObjectMap.get(objectAddress).get();
        V8JavaClassInterceptorContext context = interceptContexts.get(interceptorAddress);

        if (javaObject != null && context != null) {
            // Invoke the injection callback if present.
            Object function = jsObject.get("onJ2V8Inject");
            if (function instanceof V8Function) {
                // Despite being unchecked, we can guarantee that this is correct so long as the provided
                // interceptor is of the correct type. TODO: Maybe we could add an assert on the interceptor type?
                try {
                    interceptor.onInject(context, classy.cast(javaObject));
                } catch (Exception e) {
                    e.printStackTrace();
                    if (function instanceof V8Value) {
                        ((V8Value) function).release();
                    }
                    return;
                }
                V8Array args = V8JavaObjectUtils.translateJavaArgumentsToJavascript(new Object[] {context}, V8JavaObjectUtils.getRuntimeSarcastically(jsObject));
                ((V8Function) function).call(jsObject, args);
                args.release();
            }

            // Clean up.
            if (function instanceof V8Value) {
                ((V8Value) function).release();
            }
        } else {
            System.err.println("omigod");
        }
    }

    /**
     * Restores the Java state of a {@link V8Object} that was intercepted
     * by an {@link V8JavaClassInterceptor}.
     *
     * This method will do nothing if the specified V8Object does not have a
     * Java object handle or Java class interceptor handle.
     *
     * @param jsObject V8Object to restore to Java.
     */
    public void readInjectedInterceptor(V8Object jsObject) {
        Object obj = jsObject.get(V8JavaObjectUtils.JAVA_CLASS_INTERCEPTOR_CONTEXT_HANDLE_ID);
        if (obj instanceof V8Value && ((V8Value) obj).isUndefined()) {
            ((V8Value) obj).release();
            return;
        }
        String interceptorAddress = String.valueOf(obj);

        obj = jsObject.get(V8JavaObjectUtils.JAVA_OBJECT_HANDLE_ID);
        if (obj instanceof V8Value && ((V8Value) obj).isUndefined()) {
            ((V8Value) obj).release();
            return;
        }
        String objectAddress = String.valueOf(obj);

        Object javaObject = V8JavaCache.identifierToV8ObjectMap.get(objectAddress).get();
        V8JavaClassInterceptorContext context = interceptContexts.get(interceptorAddress);

        if (javaObject != null && context != null) {
            // Invoke the injection callback if present.
            Object function = jsObject.get("onJ2V8Extract");
            if (function instanceof V8Function) {
                V8Array args = V8JavaObjectUtils.translateJavaArgumentsToJavascript(new Object[] {context}, V8JavaObjectUtils.getRuntimeSarcastically(jsObject));
                ((V8Function) function).call(jsObject, args);
                args.release();

                // Despite being unchecked, we can guarantee that this is correct so long as the provided
                // interceptor is of the correct type. TODO: Maybe we could add an assert on the interceptor type?
                try {
                    interceptor.onExtract(context, classy.cast(javaObject));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Clean up.
            if (function instanceof V8Value) {
                ((V8Value) function).release();
            }
        } else {
            System.err.println("omigod");
        }
    }

    /**
     * Attaches a Java object to a JS object, treating the JS object as if it
     * were a proxy for the Java object.
     *
     * @param javaObject Java object to attach.
     * @param jsObject JS object to attach to.
     *
     * @return String identifier for the final java script object.
     *
     * @throws IllegalArgumentException If the passed object is not an instance of the class this proxy represents.
     */
    public String attachJavaObjectToJsObject(Object javaObject, V8Object jsObject) throws IllegalArgumentException {
        if (javaObject.getClass().equals(classy)) {
            // Register its methods as properties on itself if it doesn't have an interceptor.
            if (interceptor == null) {
                for (String m : instanceMethods.keySet()) {
                    jsObject.registerJavaMethod(instanceMethods.get(m).getCallbackForInstance(javaObject), m);
                }

            // Otherwise, register the interceptor's callback information.
            } else {
                String interceptorAddress = "CICHID" + UUID.randomUUID().toString().replaceAll("-", "");
                jsObject.add(V8JavaObjectUtils.JAVA_CLASS_INTERCEPTOR_CONTEXT_HANDLE_ID, interceptorAddress);
                V8JavaClassInterceptorContext context = new V8JavaClassInterceptorContext();
                interceptContexts.put(interceptorAddress, context);

                // Invoke the injection callback if present.
                Object function = jsObject.get("onJ2V8Inject");
                if (function instanceof V8Function) {
                    // Despite being unchecked, we can guarantee that this is correct so long as the provided
                    // interceptor is of the correct type. TODO: Maybe we could add an assert on the interceptor type?
                    interceptor.onInject(context, classy.cast(javaObject));
                    V8Array args = V8JavaObjectUtils.translateJavaArgumentsToJavascript(new Object[] {context}, V8JavaObjectUtils.getRuntimeSarcastically(jsObject));
                    ((V8Function) function).call(jsObject, args);
                    args.release();
                }

                // Clean up.
                if (function instanceof V8Value) {
                    ((V8Value) function).release();
                }
            }

            //Register the object's handle.
            String instanceAddress = "OHID" + UUID.randomUUID().toString().replaceAll("-", "");
            jsObject.add(V8JavaObjectUtils.JAVA_OBJECT_HANDLE_ID, instanceAddress);
            WeakReference<Object> reference = new WeakReference<Object>(javaObject);
            V8JavaCache.identifierToV8ObjectMap.put(instanceAddress, reference);
            V8JavaCache.v8ObjectToIdentifierMap.put(javaObject, instanceAddress);

            //Add a handle to the object on the V8 context.
            V8JavaObjectUtils.getRuntimeSarcastically(jsObject).add(instanceAddress, jsObject);

            return instanceAddress;
        } else {
            throw new IllegalArgumentException(String.format("Cannot attach Java object of type [%s] using proxy for type [%s]",
                                                             javaObject.getClass().getName(), classy.getName()));
        }
    }

    /**
     * Creates a new Java object representing the type associated with this proxy.
     *
     * @param receiver Java Script object that will represent the Java object.
     * @param parameters Parameters to use when constructing the Java object.
     */
    @Override public Object invoke(V8Object receiver, V8Array parameters) {
        //Attempt to discover a matching constructor for the arguments we've been passed.
        Object[] coercedArguments = null;
        Constructor coercedConstructor = null;
        for (Constructor constructor : classy.getConstructors()) {
            try {
                coercedArguments = V8JavaObjectUtils.translateJavascriptArgumentsToJava(constructor.isVarArgs(), constructor.getParameterTypes(), parameters, receiver);
                coercedConstructor = constructor;
                break;
            } catch (IllegalArgumentException e) {

            }
        }

        if (coercedArguments == null) {
            throw new IllegalArgumentException("No constructor exists for " + classy.getName() + " with specified arguments.");
        }

        try {
            final Object instance = coercedConstructor.newInstance(coercedArguments);
            attachJavaObjectToJsObject(instance, receiver);

            // TODO: Is this the best way to handle cleanup of Java objects for garbage collection?
            // Give it the ability to release itself.
            jsObjects.add(instance);
            receiver.registerJavaMethod(new JavaCallback() {
                @Override public Object invoke(V8Object receiver, V8Array parameters) {
                    // Dispose of any references to allow for garbage collection.
                    jsObjects.remove(instance);
                    return receiver;
                }
            }, "$release");
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Constructor received invalid arguments!");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Constructor received invalid arguments!");
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Constructor received invalid arguments!");
        }

        return null;
    }
}