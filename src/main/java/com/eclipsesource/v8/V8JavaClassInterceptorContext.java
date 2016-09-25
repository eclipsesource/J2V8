package com.eclipsesource.v8;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight class for handling contexts for {@link V8JavaClassInterceptor}s.
 *
 * TODO: Maybe we could make it so that instead of a map, intercepted contexts
 * have a predefined schema? This would reduce garbage by a good deal.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public final class V8JavaClassInterceptorContext {
//Private//////////////////////////////////////////////////////////////////////

    private final Map<String, Object> internalContext = new HashMap<String, Object>();

//Public///////////////////////////////////////////////////////////////////////

    /**
     * Sets a property on this intercepted context.
     *
     * @param name Name of the property to set.
     * @param value Value to set it to.
     */
    public void set(String name, Object value) {
        internalContext.put(name, value);
    }

    /**
     * Gets a property from this intercepted context.
     *
     * @param name Name of the property to get.
     *
     * @return Value of the property, or null if it was never set.
     */
    public Object get(String name) {
        return internalContext.get(name);
    }
}
