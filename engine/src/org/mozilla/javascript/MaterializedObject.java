/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package org.mozilla.javascript;

/**
 * Internal adapter for lazily populated ordinary objects. Rhino 1.9's
 * reflection operations enumerate through this package-private entry point,
 * bypassing the public getIds/getAllIds methods. Keep this adapter with the
 * engine's Rhino loader, never in a project generation. Covered by JSON object
 * reflection tests.
 */
public abstract class MaterializedObject extends NativeObject {
	private static final long serialVersionUID = 1L;

	protected abstract void materialize();

	/**
	 * Inspect a descriptor without allocating a JS descriptor or invoking a getter.
	 */
	public static DescriptorInfo descriptor(Context cx, ScriptableObject value, Object id) {
		return value.getOwnPropertyDescriptor(cx, id);
	}

	@Override
	Object[] getIds(CompoundOperationMap previous, boolean nonEnumerable, boolean symbols) {
		materialize();
		// Populating an empty object can replace its slot map. Enumerate the current
		// map, not the empty compound view opened by the caller before this hook.
		try (var current = startCompoundOp(false)) {
			return super.getIds(current, nonEnumerable, symbols);
		}
	}
}
