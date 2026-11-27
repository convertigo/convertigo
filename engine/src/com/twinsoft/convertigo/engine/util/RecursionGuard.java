/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

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

	/** Check without recursion or Xerces' shared child-node list cache. */
	static boolean exceedsDomDepth(Node root, long limit) {
		if (limit <= 0 || root == null) {
			return false;
		}
		Node node = root;
		int depth = 1;
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
}
