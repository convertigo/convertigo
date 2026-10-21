/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.lc.type.impl.factory.WeakReferenceFactory;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** Shared reflection metadata, isolated by project class-loader generation. Never retain request scopes. */
public final class RhinoClassCache {
	private static final WeakHashMap<ClassLoader, WeakReference<ClassCache>> caches = new WeakHashMap<>();
	private static final Object key = associationKey();
	private RhinoClassCache() { }
	private static Object associationKey() {
		try {
			var field = ClassCache.class.getDeclaredField("AKEY");
			field.setAccessible(true);
			return field.get(null);
		} catch (ReflectiveOperationException | RuntimeException e) { return null; }
	}

	public static ScriptableObject newScope(Context cx) {
		if (key == null || !EnginePropertiesManager.getPropertyAsBoolean(PropertyName.JAVASCRIPT_SHARED_CLASS_CACHE)) {
			return cx.initStandardObjects();
		}
		var loader = Thread.currentThread().getContextClassLoader();
		ClassCache cache;
		synchronized (caches) {
			var reference = caches.get(loader);
			cache = reference == null ? null : reference.get();
			if (cache == null) {
				// ClassCache.associate changes its associatedScope: associate once to an empty neutral scope.
				// Metadata must never close over a request's context, session, credentials or variables.
				var neutral = new NativeObject();
				new WeakReferenceFactory().associate(neutral);
				cache = new ClassCache();
				cache.associate(neutral);
				cx.initStandardObjects(neutral);
				caches.put(loader, new WeakReference<>(cache));
			}
		}
		var scope = new NativeObject();
		new WeakReferenceFactory().associate(scope);
		scope.associateValue(key, cache);
		cx.initStandardObjects(scope);
		return scope;
	}
}
