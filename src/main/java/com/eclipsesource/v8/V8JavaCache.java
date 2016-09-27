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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Centralized cache for resources created via the {@link V8JavaAdapter}. This class
 * is not meant to be used directly by API consumers; any actions should be performed
 * via the {@link V8JavaAdapter} class.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
final class V8JavaCache {
    /**
     * Cache of Java classes injected into V8 via the {@link V8JavaAdapter}.
     */
    public static final Map<Class<?>, V8JavaClassProxy> cachedV8JavaClasses = new HashMap<Class<?>, V8JavaClassProxy>();

    /**
     * Cache of Java objects created through V8 via a {@link V8JavaClassProxy}.
     *
     * TODO: This cache is shared across V8 runtimes, theoretically allowing cross-runtime sharing of Java objects.
     *       Is this a "feature" or a "bug"?
     */
    public static final Map<String, WeakReference> identifierToV8ObjectMap = new HashMap<String, WeakReference>();
    public static final Map<Object, String> v8ObjectToIdentifierMap = new WeakHashMap<Object, String>();

    /**
     * Removes any Java objects that have been garbage collected from the object cache.
     */
    public static void removeGarbageCollectedJavaObjects() {
        Iterator<Map.Entry<String, WeakReference>> it = identifierToV8ObjectMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, WeakReference> entry = it.next();
            if (entry.getValue().get() == null) {
                it.remove();
            }
        }
    }
}
