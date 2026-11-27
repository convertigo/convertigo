/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public final class RecursionGuard {
	private RecursionGuard() {
	}

	/** Read once at the start of a protected operation. Non-positive values disable the guard. */
	public static long getLimit() {
		return EnginePropertiesManager.getPropertyAsLong(PropertyName.STACK_LIMIT_PROTECTION);
	}

	static String limitMessage(String operation, long limit) {
		return operation + " exceeds the maximum depth of " + limit + " (Stack limit protection, engine property "
				+ PropertyName.STACK_LIMIT_PROTECTION.getKey() + ")";
	}

	public static void checkDepth(String operation, long limit, int depth) {
		if (limit > 0 && depth > limit) {
			throw new IllegalArgumentException(limitMessage(operation, limit));
		}
	}

	public static void checkDomDepth(Node root, String operation) {
		long limit = getLimit();
		if (exceedsDomDepth(root, limit)) {
			throw new IllegalArgumentException(limitMessage(operation, limit));
		}
	}

	/** Check without recursion or Xerces' shared child-node list cache. */
	static boolean exceedsDomDepth(Node root, long limit) {
		if (limit <= 0 || root == null) {
			return false;
		}
		Node node = root;
		int depth = root.getNodeType() == Node.DOCUMENT_NODE ? 0 : 1;
		while (true) {
			Node child = node.getFirstChild();
			// Leaf text, comments and processing instructions add no nesting.
			if (depth > limit && (node instanceof Element || child != null)) {
				return true;
			}
			if (child != null) {
				node = child;
				depth++;
			} else {
				while (node != root && node.getNextSibling() == null) {
					node = node.getParentNode();
					depth--;
				}
				if (node == root) {
					return false;
				}
				node = node.getNextSibling();
			}
		}
	}

	/** Count nested JSON containers using one cursor per ancestor. */
	static boolean exceedsJsonDepth(Object root, long limit) {
		if (limit <= 0) {
			return false;
		}
		Iterator<?> children = jsonChildren(root);
		if (children == null) {
			return false;
		}
		Deque<Iterator<?>> path = new ArrayDeque<>();
		path.push(children);
		while (!path.isEmpty()) {
			Iterator<?> current = path.peek();
			if (!current.hasNext()) {
				path.pop();
				continue;
			}
			children = jsonChildren(current.next());
			if (children != null) {
				if (path.size() >= limit) {
					return true;
				}
				path.push(children);
			}
		}
		return false;
	}

	private static Iterator<?> jsonChildren(Object value) {
		if (value instanceof JSONObject object) {
			return object.toMap().values().iterator();
		}
		if (value instanceof JSONArray array) {
			return array.stream().iterator();
		}
		return null;
	}
}
