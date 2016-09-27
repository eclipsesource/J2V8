/*******************************************************************************
 * Copyright (c) 2016 Brandon Sanders
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brandon Sanders - initial API and implementation and/or initial documentation
 ******************************************************************************/
package com.eclipsesource.v8;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/**
 * Utilities for translating individual Java objects to and from V8.
 *
 * This class differs from {@link com.eclipsesource.v8.utils.V8ObjectUtils}
 * in that it bridges individual Java objects to and from a V8 runtime,
 * not entire lists or arrays of objects.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public final class V8JavaObjectUtils {
//Private//////////////////////////////////////////////////////////////////////

    /**
     * Super hax0r map used when comparing primitives and their boxed
     * counterparts.
     */
    private static final Map<Class<?>, Class<?>> BOXED_PRIMITIVE_MAP = new HashMap<Class<?>, Class<?>>() {
        @Override public Class<?> get(Object classy) {
            if (containsKey(classy)) {
                return super.get(classy);
            } else {
                return (Class<?>) classy;
            }
        }
    }; static {
        BOXED_PRIMITIVE_MAP.put(boolean.class, Boolean.class);
        BOXED_PRIMITIVE_MAP.put(short.class, Short.class);
        BOXED_PRIMITIVE_MAP.put(int.class, Integer.class);
        BOXED_PRIMITIVE_MAP.put(long.class, Long.class);
        BOXED_PRIMITIVE_MAP.put(float.class, Float.class);
        BOXED_PRIMITIVE_MAP.put(double.class, Double.class);
    }

    /**
     * Returns true if the passed object is primitive in respect to V8.
     */
    private static boolean isBasicallyPrimitive(Object object) {
        return object instanceof V8Value ||
                object instanceof String ||
                object instanceof Boolean ||
                object instanceof Short ||
                object instanceof Integer ||
                object instanceof Long ||
                object instanceof Float ||
                object instanceof Double;
    }

    /**
     * List of {@link V8Value}s held by this class or one of its delegates.
     */
    private static final List<WeakReference<V8Value>> v8Resources = new ArrayList<WeakReference<V8Value>>();

    /**
     * Lightweight invocation handler for translating certain V8 functions to
     * Java functional interfaces.
     */
    private static class V8FunctionInvocationHandler implements InvocationHandler {
        private final V8Object receiver;
        private final V8Function function;

        @Override protected void finalize() {
            try {
                super.finalize();
            } catch (Throwable t) { }

            if (!receiver.isReleased()) {
                receiver.release();
            }

            if (!function.isReleased()) {
                function.release();
            }
        }

        public V8FunctionInvocationHandler(V8Object receiver, V8Function function) {
            this.receiver = receiver.twin();
            this.function = function.twin();
            v8Resources.add(new WeakReference<V8Value>(this.receiver));
            v8Resources.add(new WeakReference<V8Value>(this.function));
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                V8Array v8Args = translateJavaArgumentsToJavascript(args, V8JavaObjectUtils.getRuntimeSarcastically(receiver));
                Object obj = function.call(receiver, v8Args);
                if (!v8Args.isReleased()) {
                    v8Args.release();
                }

                if (obj instanceof V8Object) {
                    V8Object v8Obj = ((V8Object) obj);
                    if (!v8Obj.isUndefined()) {
                        Object ret = V8JavaCache.identifierToV8ObjectMap.get(v8Obj.get(JAVA_OBJECT_HANDLE_ID).toString()).get();
                        v8Obj.release();
                        return ret;
                    } else {
                        v8Obj.release();
                        return null;
                    }
                } else {
                    return obj;
                }
            } catch (Throwable t) {
                throw t;
            }
        }

        public String toString() {
            return function.toString();
        }
    }

//Public///////////////////////////////////////////////////////////////////////

    /**
     * Variable name used when attaching a Java object ID to a JS object.
     */
    public static final String JAVA_OBJECT_HANDLE_ID = "____JavaObjectHandleID____";

    /**
     * Variable name used when attaching an interceptor context to a JS object.
     */
    public static final String JAVA_CLASS_INTERCEPTOR_CONTEXT_HANDLE_ID = "____JavaClassInterceptorContextHandleID____";

    /**
     * Attempts to convert the given array into it's primitive counterpart.
     *
     * @param array Array to convert.
     * @param type Boxed type of the array to convert.
     *
     * @return Primitive version of the given array, or the original array
     *         if no primitive type matched the passed type.
     */
    public static Object toPrimitiveArray(Object[] array, Class<?> type) {
        if (Boolean.class.equals(type)) {
            boolean[] ret = new boolean[array.length];
            for (int i = 0; i < array.length; i++) {
                ret[i] = (Boolean) array[i];
            }
            return ret;
        } else if (Byte.class.equals(type)) {
            byte[] ret = new byte[array.length];
            for (int i = 0; i < array.length; i++) {
                ret[i] = (Byte) array[i];
            }
            return ret;
        } else if (Short.class.equals(type)) {
            short[] ret = new short[array.length];
            for (int i = 0; i < array.length; i++) {
                ret[i] = (Short) array[i];
            }
            return ret;
        } else if (Integer.class.equals(type)) {
            int[] ret = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                ret[i] = (Integer) array[i];
            }
            return ret;
        } else if (Long.class.equals(type)) {
            long[] ret = new long[array.length];
            for (int i = 0; i < array.length; i++) {
                ret[i] = (Long) array[i];
            }
            return ret;
        } else if (Float.class.equals(type)) {
            float[] ret = new float[array.length];
            for (int i = 0; i < array.length; i++) {
                ret[i] = (Float) array[i];
            }
            return ret;
        } else if (Double.class.equals(type)) {
            double[] ret = new double[array.length];
            for (int i = 0; i < array.length; i++) {
                ret[i] = (Double) array[i];
            }
            return ret;
        }

        return array;
    }

    /**
     * Attempts to widen a given number to work with the specified class.
     *
     * TODO: Surely there's a cleaner way to write this!
     *
     * @param from Number to widen.
     * @param to Class to widen to.
     *
     * @return A widened version of the passed number, or null if no
     *         possible solutions existed for widening.
     */
    @SuppressWarnings("unchecked")
    public static <T> T widenNumber(Object from, Class<T> to) {
        if (from.getClass().equals(to)) {
            return (T) from;
        }

        if (from instanceof Short) {
            if (to == Short.class || to == short.class) {
                return (T) from;
            } else if (to == Integer.class || to == int.class) {
                return (T) new Integer((Short) from);
            } else if (to == Long.class || to == long.class) {
                return (T) new Long((Short) from);
            } else if (to == Float.class || to == float.class) {
                return (T) new Float((Short) from);
            } else if (to == Double.class || to == double.class) {
                return (T) new Double((Short) from);
            }
        } else if (from instanceof Integer) {
            if (to == Integer.class || to == int.class) {
                return (T) from;
            } else if (to == Long.class || to == long.class) {
                return (T) new Long((Integer) from);
            } else if (to == Float.class || to == float.class) {
                return (T) new Float((Integer) from);
            } else if (to == Double.class || to == double.class) {
                return (T) new Double((Integer) from);
            }
        } else if (from instanceof Long) {
            if (to == Long.class || to == long.class) {
                return (T) from;
            } else if (to == Float.class || to == float.class) {
                return (T) new Float((Long) from);
            } else if (to == Double.class || to == double.class) {
                return (T) new Double((Long) from);
            }
        } else if (from instanceof Float) {
            if (to == Float.class || to == float.class) {
                return (T) from;
            } else if (to == Double.class || to == double.class) {
                return (T) new Double((Float) from);
            }
        } else if (from instanceof Double) {
            if (to == Double.class || to == double.class) {
                return (T) from;
            }
        }

        // Welp, find a default.
        return null;
    }

    /**
     * Ultimate hack method to work around a typo in the V8 libraries for
     * Android.
     *
     * TODO: Report upstream and stop using this dorky method.
     */
    public static final V8 getRuntimeSarcastically(V8Value value) {
        try {
            return value.getRuntime();
        } catch (Throwable t) {
            try {
                return (V8) value.getClass().getMethod("getRutime", new Class[0]).invoke(value);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Releases all V8 resources held by this class for a particular runtime.
     *
     * This method should only be called right before a V8 runtime is being
     * released, or else some resources created by this utility class will
     * fail to keep working.
     *
     * @param v8 V8 instance to release resources for.
     *
     * @return Number of resources that were released.
     */
    public static int releaseV8Resources(V8 v8) {
        int released = 0;

        for (Iterator<WeakReference<V8Value>> iterator = v8Resources.iterator(); iterator.hasNext();) {
            V8Value resource = iterator.next().get();
            if (resource != null) {
                if (V8JavaObjectUtils.getRuntimeSarcastically(resource) == v8) {
                    resource.release();
                    iterator.remove();
                    released++;
                }
            } else {
                iterator.remove();
            }
        }

        return released;
    }

    /**
     * Translates a single Java object into an equivalent V8Value.
     *
     * @param javaArgument Java argument to translate.
     * @param v8 V8 runtime that will be receiving the translated Java argument.
     *
     * @return Translated object.
     */
    public static Object translateJavaArgumentToJavascript(Object javaArgument, V8 v8) {
        if (javaArgument != null) {
            if (isBasicallyPrimitive(javaArgument)) {
                return javaArgument;
            } else {
                String key = V8JavaCache.v8ObjectToIdentifierMap.get(javaArgument);
                if (key != null) {
                    V8Object object = (V8Object) v8.get(key);
                    V8JavaCache.cachedV8JavaClasses.get(javaArgument.getClass()).writeInjectedInterceptor(object);
                    return object;
                } else {
                    key = V8JavaAdapter.injectObject(null, javaArgument, v8);
                    return v8.get(key);
                }
            }
        }

        return null;
    }

    /**
     * Translates an array of Java arguments to a V8Array.
     *
     * @param javaArguments Java arguments to translate.
     * @param v8 V8 runtime that will be receiving the translated Java arguments.
     *
     * @return Translated array.
     */
    public static V8Array translateJavaArgumentsToJavascript(Object[] javaArguments, V8 v8) {
        V8Array v8Args = new V8Array(v8);
        for (Object argument : javaArguments) {
            if (argument instanceof V8Value) {
                v8Args.push((V8Value) argument);
            } else if (argument instanceof String) {
                v8Args.push((String) argument);
            } else if (argument instanceof Boolean) {
                v8Args.push((Boolean) argument);
            } else if (argument instanceof Short) {
                v8Args.push((Short) argument);
            } else if (argument instanceof Integer) {
                v8Args.push((Integer) argument);
            } else if (argument instanceof Long) {
                v8Args.push((Long) argument);
            } else if (argument instanceof Float) {
                v8Args.push((Float) argument);
            } else if (argument instanceof Double) {
                v8Args.push((Double) argument);
            } else {
                V8Value translatedJavaArgument = (V8Value) translateJavaArgumentToJavascript(argument, v8);
                v8Args.push(translatedJavaArgument);
                translatedJavaArgument.release();
            }
        }

        return v8Args;
    }

    /**
     * Translates a single element from a V8Array to an Object based on a given Java argument type.
     *
     * It is the responsibility of the caller of this method to invoke {@link V8Value#release()} on
     * any objects passed to this method; this method will not make an effort to release them.
     *
     * @param javaArgumentType Java type that the argument must match.
     * @param argument Argument to translate to Java.
     * @param receiver V8Object receiver that any functional arguments should be tied to.
     *
     * @return Translated Object based on the passed Java types and and Javascript value.
     *
     * @throws IllegalArgumentException if the Javascript value could not be coerced in the types
     *         specified by te passed array of java argument types.
     */
    public static Object translateJavascriptArgumentToJava(Class<?> javaArgumentType, Object argument, V8Object receiver) throws IllegalArgumentException {
        if (argument instanceof V8Value) {
            if (argument instanceof V8Function) {
                if (javaArgumentType.isInterface() && javaArgumentType.getDeclaredMethods().length == 1) {
                    //Create a proxy class for the functional interface that wraps this V8Function.
                    V8FunctionInvocationHandler handler = new V8FunctionInvocationHandler(receiver, (V8Function) argument);
                    return Proxy.newProxyInstance(javaArgumentType.getClassLoader(), new Class[] { javaArgumentType }, handler);
                } else {
                    throw new IllegalArgumentException(
                            "Method was passed V8Function but does not accept a functional interface.");
                }
            } else if (argument instanceof V8Array) {
                if (javaArgumentType.isArray()) {
                    // Perform a single cast up front.
                    V8Array v8Array = (V8Array) argument;

                    // TODO: This logic is almost identical to the varargs manipulation logic. Maybe we can reuse it?
                    Class<?> originalArrayType = javaArgumentType.getComponentType();
                    Class<?> arrayType = originalArrayType;
                    if (BOXED_PRIMITIVE_MAP.containsKey(arrayType)) {
                        arrayType = BOXED_PRIMITIVE_MAP.get(arrayType);
                    }
                    Object[] array = (Object[]) Array.newInstance(arrayType, v8Array.length());

                    for (int i = 0; i < array.length; i++) {
                        // We have to release the value immediately after using it if it's a V8Value.
                        Object arrayElement = v8Array.get(i);
                        try {
                            array[i] = translateJavascriptArgumentToJava(javaArgumentType.getComponentType(),
                                                                         arrayElement, receiver);
                        } catch (IllegalArgumentException e) {
                            throw e;
                        } finally {
                            if (arrayElement instanceof V8Value) {
                                ((V8Value) arrayElement).release();
                            }
                        }
                    }

                    if (BOXED_PRIMITIVE_MAP.containsKey(originalArrayType) && BOXED_PRIMITIVE_MAP.containsValue(arrayType)) {
                        return toPrimitiveArray(array, arrayType);
                    } else {
                        return array;
                    }
                } else {
                    throw new IllegalArgumentException("Method was passed a V8Array but does not accept arrays.");
                }
            } else if (argument instanceof V8Object) {
                try {
                    //Attempt to retrieve a Java object handle.
                    String javaHandle = (String) ((V8Object) argument).get(JAVA_OBJECT_HANDLE_ID);
                    Object javaObject = V8JavaCache.identifierToV8ObjectMap.get(javaHandle).get();

                    if (javaObject != null) {
                        if (javaArgumentType.isAssignableFrom(javaObject.getClass())) {
                            // Check if it's intercepted.
                            V8JavaCache.cachedV8JavaClasses.get(javaObject.getClass()).readInjectedInterceptor(
                                    (V8Object) argument);
                            return javaObject;
                        } else {
                            throw new IllegalArgumentException(
                                    "Argument is Java type but does not match signature for this method.");
                        }
                    } else {
                        V8JavaCache.removeGarbageCollectedJavaObjects();
                        throw new IllegalArgumentException(
                                "Argument has invalid Java object handle or object referenced by handle has aged out.");
                    }
                } catch (NullPointerException e) {
                    throw new IllegalArgumentException(
                            "Argument has invalid Java object handle or object referenced by handle has aged out.");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(
                            "Complex objects can only be passed to Java if they represent Java objects.");
                }
            } else {
                //TODO: Add support for arrays.
                throw new IllegalArgumentException(
                        "Translation of JS to Java arguments is only supported for primitives, objects, arrays and functions.");
            }
        } else {
            if (javaArgumentType.isAssignableFrom(argument.getClass()) ||
                    BOXED_PRIMITIVE_MAP.get(argument.getClass())
                            .isAssignableFrom(BOXED_PRIMITIVE_MAP.get(javaArgumentType))) {
                return argument;
            } else {
                Object widened = widenNumber(argument, javaArgumentType);
                if (widened != null) {
                    return widened;
                } else {
                    throw new IllegalArgumentException(
                            "Primitive argument cannot be coerced to expected parameter type.");
                }
            }
        }
    }

    /**
     * Translates a V8Array of arguments to an Object array based on a set of Java argument types.
     *
     * @param isVarArgs Whether or not the Java parameters list ends in a varargs array.
     * @param javaArgumentTypes Java types that the arguments must match.
     * @param javascriptArguments Arguments to translate to Java.
     * @param receiver V8Object receiver that any functional arguments should be tied to.
     *
     * @return Translated Object array of arguments based on the passed Java types and V8Array.
     *
     * @throws IllegalArgumentException if the V8Array could not be coerced into the types specified
     *         by the passed array of Java argument types.
     */
    public static Object[] translateJavascriptArgumentsToJava(boolean isVarArgs, Class<?>[] javaArgumentTypes, V8Array javascriptArguments, V8Object receiver) throws IllegalArgumentException {
        // Varargs handling.
        if (isVarArgs && javaArgumentTypes.length > 0 &&
            javaArgumentTypes[javaArgumentTypes.length - 1].isArray() &&
            javascriptArguments.length() >= javaArgumentTypes.length - 1) {
            Class<?> originalVarargsType = javaArgumentTypes[javaArgumentTypes.length - 1].getComponentType();
            Class<?> varargsType = originalVarargsType;
            if (BOXED_PRIMITIVE_MAP.containsKey(varargsType)) {
                varargsType = BOXED_PRIMITIVE_MAP.get(varargsType);
            }
            Object[] varargs = (Object[]) Array.newInstance(varargsType, javascriptArguments.length() - javaArgumentTypes.length + 1);
            Object[] returnedArgumentValues = new Object[javaArgumentTypes.length];

            for (int i = 0; i < javascriptArguments.length(); i++) {
                Object argument = javascriptArguments.get(i);

                try {
                    // If we haven't hit the varargs yet, insert normally.
                    if (returnedArgumentValues.length - 1 > i) {
                        returnedArgumentValues[i] =
                                translateJavascriptArgumentToJava(javaArgumentTypes[i],
                                                                  argument, receiver);

                    // Otherwise insert into the varargs.
                    } else {
                        varargs[i - (returnedArgumentValues.length - 1)] =
                                translateJavascriptArgumentToJava(varargsType, argument, receiver);
                    }
                } catch (IllegalArgumentException e) {
                    throw e;
                } finally {
                    if (argument instanceof V8Value) {
                        ((V8Value) argument).release();
                    }
                }
            }

            // Convert any boxed primitives to actual primitives IF the original varargs type was a primitive..
            if (BOXED_PRIMITIVE_MAP.containsKey(originalVarargsType) && BOXED_PRIMITIVE_MAP.containsValue(varargsType)) {
                returnedArgumentValues[returnedArgumentValues.length - 1] = toPrimitiveArray(varargs, varargsType);
            } else {
                returnedArgumentValues[returnedArgumentValues.length - 1] = varargs;
            }

            return returnedArgumentValues;

        // Typical handling.
        } else if (javaArgumentTypes.length == javascriptArguments.length()) {
            Object[] returnedArgumentValues = new Object[javaArgumentTypes.length];

            for (int i = 0; i < javascriptArguments.length(); i++) {
                Object argument = javascriptArguments.get(i);
                try {
                    returnedArgumentValues[i] = translateJavascriptArgumentToJava(javaArgumentTypes[i], argument,
                                                                                  receiver);
                } catch (IllegalArgumentException e) {
                    throw e;
                } finally {
                    if (argument instanceof V8Value) {
                        ((V8Value) argument).release();
                    }
                }
            }

            return returnedArgumentValues;
        } else {
            throw new IllegalArgumentException(
                    "Method arguments size and passed arguments size do not match.");
        }
    }
}
