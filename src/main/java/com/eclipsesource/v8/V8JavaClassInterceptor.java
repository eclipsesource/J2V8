package com.eclipsesource.v8;

/**
 * Represents a class that intercepts a Java object of a known type
 * and translates it into a set of key-value pairs before injecting
 * it into a V8 context.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public interface V8JavaClassInterceptor<T> {

    /**
     * Returns the body of the JS constructor function that this
     * interceptor works with.
     *
     * All constructor scripts should declare two functions, {@code onJ2V8Inject} and
     * {@code onJ2V8Withdraw}, which accept a single variable, {@code context}. These
     * functions will be called when a Java object is injected and extracted from V8,
     * respectively. The {@code context} variable will contain all of the top-level
     * properties that were set onto the {@code context} variable provided to this
     * class's {@link #onInject(V8JavaClassInterceptorContext, Object)} and
     * {@link #onExtract(V8JavaClassInterceptorContext, Object)} methods.
     *
     * @return Body of a JS constructor function that this interceptor
     *         will be used with.
     */
    String getConstructorScriptBody();

    /**
     * Called when an intercepted class is injected into the V8 context.
     *
     * Use this method to extract data from the passed object and inject it
     * into the V8 context; do not worry about translating values, as J2V8
     * will handle that for you.
     *
     * @param context Context variable to set all injected properties onto.
     * @param object Object being injected.
     */
    void onInject(V8JavaClassInterceptorContext context, T object);

    /**
     * Called when an intercepted class is extracted from the V8 context.
     *
     * Use this method to extract data from the V8 context and inject it
     * into the passed object; do not worry about translation values, as
     * J2V8 will have already handled that for you.
     *
     * @param context Context variable to extract properties from.
     * @param object Object to inject all properties into.
     */
    void onExtract(V8JavaClassInterceptorContext context, T object);
}
