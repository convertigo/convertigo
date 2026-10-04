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

package com.twinsoft.convertigo.beans.flow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;

class FlowVirtualProjector {

	private FlowVirtualProjector() {
	}

	static List<DatabaseObject> childrenOf(Flow flow) {
		try {
			return childrenFromResponse(flow, new FlowEngineBridge().describeTree(flow));
		} catch (Exception e) {
			Engine.logBeans.warn("Unable to describe Flow tree for " + flow.getQName(), e);
			return errorChildren(flow, "flow", e);
		}
	}

	static List<DatabaseObject> childrenOf(FlowEngine flowEngine) {
		try {
			return childrenFromResponse(flowEngine, new FlowEngineBridge().describeTree(flowEngine));
		} catch (Exception e) {
			Engine.logBeans.warn("Unable to describe FlowEngine tree for " + flowEngine.getQName(), e);
			return errorChildren(flowEngine, "engine", e);
		}
	}

	private static List<DatabaseObject> childrenFromResponse(DatabaseObject parent, JSONObject response) {
		var children = new ArrayList<DatabaseObject>();
		if (response == null) {
			return children;
		}
		if (!response.optBoolean("ok", false)) {
			var error = response.optJSONObject("error");
			var message = error == null ? response.toString() : error.optString("message", error.toString());
			Engine.logBeans.warn("Flow virtual tree response failed for " + parent.getQName() + ": " + message);
			children.add(new FlowVirtualObject(parent, "error", "error", "error", "error", "Flow tree error", message));
			return children;
		}
		var array = response.optJSONArray("children");
		if (array == null) {
			return children;
		}
		var seen = new HashSet<String>();
		var names = new HashSet<String>();
		for (var i = 0; i < array.length(); i++) {
			var child = array.optJSONObject(i);
			if (child != null && seen.add(virtualObjectKey(child))) {
				children.add(toVirtualObject(parent, child, i, names));
			}
		}
		return children;
	}

	private static FlowVirtualObject toVirtualObject(DatabaseObject parent, JSONObject source, int order, HashSet<String> names) {
		var name = FlowVirtualObject.safeName(source.optString("name", "item"));
		// A presentation sibling may refer to another source branch, and different
		// source names may normalize to the same DBO name. Keep QNames unambiguous
		// without reserving business keys or changing their source mutation paths.
		if (!names.add(name)) {
			var base = name + "_" + Integer.toUnsignedString(virtualObjectKey(source).hashCode(), 36);
			name = base;
			for (int i = 2; !names.add(name); i++) name = base + "_" + i;
		}
		var object = new FlowVirtualObject(parent,
				name,
				source.optString("kind", ""),
				source.optString("type", ""),
				source.optString("path", ""),
				source.optString("summary", ""),
				source.optString("definition", ""));
		object.setVirtualOrder(order);
		object.setVirtualInfo(source.optString("info", ""));
		var children = source.optJSONArray("children");
		if (children != null) {
			var seen = new HashSet<String>();
			var childNames = new HashSet<String>();
			for (var i = 0; i < children.length(); i++) {
				var child = children.optJSONObject(i);
				if (child != null && seen.add(virtualObjectKey(child))) {
					object.addVirtualChild(toVirtualObject(object, child, i, childNames));
				}
			}
		}
		return object;
	}

	static FlowVirtualObject projectedObject(DatabaseObject parent, JSONObject source, int order) {
		if (source == null) return null;
		var names = new HashSet<String>();
		// A targeted refresh must allocate names against the existing siblings,
		// exactly as a complete projection does, excluding the replaced node itself.
		if (parent instanceof FlowVirtualObject virtualParent) {
			for (var sibling : virtualParent.getDatabaseObjectChildren()) {
				if (!(sibling instanceof FlowVirtualObject virtual)
						|| !virtualObjectKey(source).equals(virtualObjectKey(virtual))) {
					names.add(sibling.getName());
				}
			}
		}
		return toVirtualObject(parent, source, order, names);
	}

	private static String virtualObjectKey(FlowVirtualObject source) {
		return source.getVirtualPath() + "\u0000" + source.getVirtualKind() + "\u0000" + source.getVirtualType();
	}

	private static String virtualObjectKey(JSONObject source) {
		return source.optString("path", "") + "\u0000"
				+ source.optString("kind", "") + "\u0000"
				+ source.optString("type", "");
	}

	private static List<DatabaseObject> errorChildren(DatabaseObject parent, String target, Exception e) {
		var children = new ArrayList<DatabaseObject>();
		children.add(new FlowVirtualObject(parent, "error", "error", target, target + ".error",
				"Unable to describe " + target + " tree", e.getMessage()));
		return children;
	}
}
