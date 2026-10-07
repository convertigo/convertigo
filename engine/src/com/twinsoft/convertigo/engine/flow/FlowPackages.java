/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.flow;

import java.util.function.Supplier;

import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.lc.type.impl.factory.WeakReferenceFactory;

import com.twinsoft.convertigo.engine.ProjectClassLoader;

/**
 * The Java packages of a Flow call: the runtimes of the Flow engine are shared by all the projects, and Rhino binds
 * Packages (and java, javax, org, com, edu, net) to the class loader of the thread that creates a scope. A runtime is
 * created with the packages of the engine, a call gets those of the generation of the class path of its project (see
 * {@link ProjectClassLoader}) and gives back those of the engine, so that a runtime never keeps a generation.
 * <p>
 * Rhino also keeps what it learns of a Java class in caches of the scope: the type information (made weak here) and
 * the members of the classes (ClassCache), from which the classes of the generation are removed after the call.
 */
final class FlowPackages {
	/** The names Rhino defines for the Java packages in a scope. */
	static final String[] ROOTS = { "Packages", "java", "javax", "org", "com", "edu", "net" };
	/** In a scope: the packages of the engine, given back after each call. */
	private static final String ENGINE_ROOTS = "__flowEnginePackages";
	/** In a scope: the identifier of the generation of the call, empty for the engine. */
	static final String GENERATION_ID = "__flowGenerationId";

	private FlowPackages() {
	}

	/**
	 * @return the standard objects of a new runtime, with the packages of the engine whatever the calling thread: Rhino
	 *         creates them at their first use, with the class loader of the thread at that time
	 */
	static ScriptableObject createScope(Context cx) {
		return withEngineLoader(() -> {
			var scope = new NativeObject();
			// before the standard objects, which keep it: the type information of a class does not keep it
			new WeakReferenceFactory().associate(scope);
			cx.initStandardObjects(scope);
			captureEngineRoots(scope);
			return scope;
		});
	}

	private static <T> T withEngineLoader(Supplier<T> creation) {
		var thread = Thread.currentThread();
		var previous = thread.getContextClassLoader();
		// the libraries of the workspace, then the engine
		var engine = com.twinsoft.convertigo.engine.Engine.getEngineClassLoader();
		thread.setContextClassLoader(engine != null ? engine : FlowPackages.class.getClassLoader());
		try {
			return creation.get();
		} finally {
			thread.setContextClassLoader(previous);
		}
	}

	/** Keeps the packages of the engine of a new runtime, given back after each call. */
	private static void captureEngineRoots(Scriptable scope) {
		var roots = new Object[ROOTS.length];
		for (int i = 0; i < ROOTS.length; i++) {
			roots[i] = ScriptableObject.getProperty(scope, ROOTS[i]);
		}
		if (scope instanceof ScriptableObject object) {
			object.defineProperty(ENGINE_ROOTS, roots, ScriptableObject.DONTENUM | ScriptableObject.READONLY);
		}
	}

	/**
	 * The call uses the packages of a generation; nothing changes for the engine (null) or a runtime without the
	 * packages of the engine.
	 */
	static void bind(Context cx, Scriptable scope, ClassLoader generation) {
		var engineRoots = engineRoots(scope);
		if (engineRoots == null) {
			return;
		}
		if (generation == null || !(engineRoots[0] instanceof Function packages)) {
			restore(scope);
			return;
		}
		var top = packages.construct(cx, scope, new Object[] { Context.javaToJS(generation, scope) });
		scope.put(ROOTS[0], scope, top);
		for (int i = 1; i < ROOTS.length; i++) {
			scope.put(ROOTS[i], scope, ScriptableObject.getProperty(top, ROOTS[i]));
		}
		scope.put(GENERATION_ID, scope, generation instanceof ProjectClassLoader projectGeneration ? projectGeneration.getId() : "");
	}

	/** Gives back the packages of the engine. */
	static void restore(Scriptable scope) {
		restore(scope, null);
	}

	/** Gives back the packages of the engine, and forgets the classes of the generation of the call. */
	static void restore(Scriptable scope, ClassLoader generation) {
		var engineRoots = engineRoots(scope);
		if (engineRoots == null) {
			return;
		}
		for (int i = 0; i < ROOTS.length; i++) {
			scope.put(ROOTS[i], scope, engineRoots[i]);
		}
		scope.put(GENERATION_ID, scope, "");
		if (generation != null) {
			forget(scope, generation);
		}
	}

	private static java.lang.reflect.Field classTable;
	private static java.lang.reflect.Field cacheKeyClass;

	/** Removes the members of the classes of a generation from the cache of the scope, else all of them. */
	private static void forget(Scriptable scope, ClassLoader generation) {
		ClassCache cache;
		try {
			cache = ClassCache.get(scope);
		} catch (Exception e) {
			return;
		}
		try {
			if (classTable == null) {
				var table = ClassCache.class.getDeclaredField("classTable");
				table.setAccessible(true);
				var key = Class.forName("org.mozilla.javascript.ClassCache$CacheKey").getDeclaredField("cls");
				key.setAccessible(true);
				cacheKeyClass = key;
				classTable = table;
			}
			if (classTable.get(cache) instanceof java.util.Map<?, ?> table) {
				var key = cacheKeyClass;
				table.keySet().removeIf(entry -> {
					try {
						return key.get(entry) instanceof Class<?> cls && cls.getClassLoader() == generation;
					} catch (IllegalAccessException e) {
						return true;
					}
				});
			}
		} catch (Exception | LinkageError e) {
			// another version of Rhino: everything is learnt again
			cache.clearCaches();
		}
	}

	private static Object[] engineRoots(Scriptable scope) {
		return scope instanceof ScriptableObject object && object.has(ENGINE_ROOTS, object)
				&& object.get(ENGINE_ROOTS, object) instanceof Object[] roots ? roots : null;
	}
}
