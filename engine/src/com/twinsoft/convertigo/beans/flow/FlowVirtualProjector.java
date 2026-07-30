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
		for (var i = 0; i < array.length(); i++) {
			var child = array.optJSONObject(i);
			if (child != null && seen.add(virtualObjectKey(child))) {
				children.add(toVirtualObject(parent, child, i));
			}
		}
		return children;
	}

	private static FlowVirtualObject toVirtualObject(DatabaseObject parent, JSONObject source, int order) {
		var object = new FlowVirtualObject(parent,
				source.optString("name", "item"),
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
			for (var i = 0; i < children.length(); i++) {
				var child = children.optJSONObject(i);
				if (child != null && seen.add(virtualObjectKey(child))) {
					object.addVirtualChild(toVirtualObject(object, child, i));
				}
			}
		}
		return object;
	}

	static FlowVirtualObject projectedObject(DatabaseObject parent, JSONObject source, int order) {
		return source == null ? null : toVirtualObject(parent, source, order);
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
