/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.engine.util.DetachedJson;

/**
 * A chunk of detached rows, shown to scripts as a read-only array: a row becomes a script object only when it is
 * read. The row kernels and emit take its Java rows as they are, without converting them.
 */
public final class RowChunk extends ScriptableObject {
	private static final long serialVersionUID = 1L;
	private final transient List<Object> rows;
	private final transient Object[] views;
	private transient BaseFunction toJson;

	public RowChunk(List<Object> rows, Scriptable scope) {
		this.rows = rows;
		this.views = new Object[rows.size()];
		var top = ScriptableObject.getTopLevelScope(scope);
		setParentScope(top);
		setPrototype(ScriptableObject.getArrayPrototype(top));
	}

	/** The detached rows of this chunk, never to be changed by a caller. */
	public List<Object> rows() {
		return rows;
	}

	@Override public String getClassName() {
		return "Array";
	}

	@Override public Object get(int index, Scriptable start) {
		if (index < 0 || index >= views.length) return NOT_FOUND;
		var view = views[index];
		if (view == null && rows.get(index) != null) {
			try {
				view = views[index] = DetachedJson.toScriptable(rows.get(index), getParentScope());
			} catch (JSONException e) {
				throw Context.throwAsScriptRuntimeEx(e);
			}
		}
		return view;
	}

	@Override public boolean has(int index, Scriptable start) {
		return index >= 0 && index < views.length;
	}

	@Override public Object get(String name, Scriptable start) {
		if ("length".equals(name)) return (double) views.length;
		if ("toJSON".equals(name)) return toJson();
		return super.get(name, start);
	}

	// JSON.stringify writes an array only for a native array: give it one
	private Object toJson() {
		if (toJson == null) {
			var scope = getParentScope();
			toJson = new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
				private static final long serialVersionUID = 1L;
				@Override public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
					var items = new Object[views.length];
					for (int i = 0; i < items.length; i++) items[i] = RowChunk.this.get(i, RowChunk.this);
					return cx.newArray(scope, items);
				}
			};
		}
		return toJson;
	}

	@Override public boolean has(String name, Scriptable start) {
		return "length".equals(name) || "toJSON".equals(name) || super.has(name, start);
	}

	@Override public void put(int index, Scriptable start, Object value) {
		throw readOnly();
	}

	@Override public void put(String name, Scriptable start, Object value) {
		throw readOnly();
	}

	@Override public void delete(int index) {
		throw readOnly();
	}

	@Override public void delete(String name) {
		throw readOnly();
	}

	@Override public Object[] getIds() {
		var ids = new Object[views.length];
		for (int i = 0; i < ids.length; i++) ids[i] = i;
		return ids;
	}

	private static RuntimeException readOnly() {
		return Context.reportRuntimeError("The rows of a chunk are read-only: copy them to change them.");
	}
}
