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

package com.twinsoft.convertigo.eclipse.views.jsonpicker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A tree of a JSON value, built as its nodes are expanded. Each item knows its path from the root (keys and
 * indexes), from which the JSON picker writes a JavaScript expression or a JSON path.
 */
public class JsonTree extends Composite {
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Pattern identifier = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
	/** items shown under an array or object before a "more" item */
	private static final int MAX_CHILDREN = 500;
	private static final String DUMMY = "…";

	private final Tree tree;
	private String rootName = "";
	private Consumer<List<Object>> selectionListener;

	public JsonTree(Composite parent, int style) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
		tree = new Tree(this, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | style);
		tree.addListener(SWT.Expand, e -> fill((TreeItem) e.item));
		tree.addListener(SWT.Selection, e -> {
			if (selectionListener != null) {
				selectionListener.accept(getSelectedPath());
			}
		});
	}

	public Tree getTree() {
		return tree;
	}

	public void setSelectionListener(Consumer<List<Object>> selectionListener) {
		this.selectionListener = selectionListener;
	}

	/** shows a JSON text under a root item; false if the text is not JSON */
	public boolean setJson(String rootName, String json) {
		tree.removeAll();
		this.rootName = rootName == null ? "" : rootName;
		if (json == null || json.isEmpty()) {
			return false;
		}
		JsonNode node;
		try {
			node = mapper.readTree(json);
		} catch (Exception e) {
			TreeItem item = new TreeItem(tree, SWT.NONE);
			item.setText("(not JSON) " + (json.length() > 200 ? json.substring(0, 200) + DUMMY : json));
			return false;
		}
		TreeItem root = new TreeItem(tree, SWT.NONE);
		root.setData(new Node(node, new ArrayList<>()));
		root.setText(label(this.rootName, node));
		addDummy(root, node);
		fill(root);
		root.setExpanded(true);
		return true;
	}

	/** expands the items down to a path, and selects its item */
	public void select(List<Object> path) {
		if (tree.getItemCount() == 0) {
			return;
		}
		TreeItem item = tree.getItem(0);
		for (Object segment : path) {
			fill(item);
			item.setExpanded(true);
			TreeItem next = null;
			for (TreeItem child : item.getItems()) {
				if (child.getData() instanceof Node n && !n.path.isEmpty() && segment.equals(n.path.get(n.path.size() - 1))) {
					next = child;
					break;
				}
			}
			if (next == null) {
				break;
			}
			item = next;
		}
		tree.setSelection(item);
		tree.showItem(item);
	}

	/** the path of the selected item: keys (String) and indexes (Integer), empty for the root */
	public List<Object> getSelectedPath() {
		TreeItem[] selection = tree.getSelection();
		if (selection.length == 1 && selection[0].getData() instanceof Node n) {
			return n.path;
		}
		return new ArrayList<>();
	}

	public JsonNode getSelectedNode() {
		TreeItem[] selection = tree.getSelection();
		if (selection.length == 1 && selection[0].getData() instanceof Node n) {
			return n.node;
		}
		return null;
	}

	private static class Node {
		final JsonNode node;
		final List<Object> path;
		boolean filled = false;

		Node(JsonNode node, List<Object> path) {
			this.node = node;
			this.path = path;
		}
	}

	private void addDummy(TreeItem item, JsonNode node) {
		if (node.isContainerNode() && node.size() > 0) {
			new TreeItem(item, SWT.NONE).setText(DUMMY);
		}
	}

	private void fill(TreeItem item) {
		if (!(item.getData() instanceof Node n) || n.filled) {
			return;
		}
		n.filled = true;
		for (TreeItem child : item.getItems()) {
			child.dispose();
		}
		int count = 0;
		if (n.node.isArray()) {
			for (int i = 0; i < n.node.size() && count < MAX_CHILDREN; i++, count++) {
				addChild(item, n, i, n.node.get(i));
			}
		} else if (n.node.isObject()) {
			for (Iterator<Map.Entry<String, JsonNode>> it = n.node.fields(); it.hasNext() && count < MAX_CHILDREN; count++) {
				Map.Entry<String, JsonNode> entry = it.next();
				addChild(item, n, entry.getKey(), entry.getValue());
			}
		}
		if (n.node.size() > count) {
			new TreeItem(item, SWT.NONE).setText(DUMMY + " " + (n.node.size() - count) + " more");
		}
	}

	private void addChild(TreeItem parent, Node n, Object segment, JsonNode value) {
		List<Object> path = new ArrayList<>(n.path);
		path.add(segment);
		TreeItem child = new TreeItem(parent, SWT.NONE);
		child.setData(new Node(value, path));
		child.setText(label(segment instanceof Integer ? "[" + segment + "]" : segment.toString(), value));
		addDummy(child, value);
	}

	private static String label(String name, JsonNode node) {
		String prefix = name.isEmpty() ? "" : name + ": ";
		if (node.isArray()) {
			return prefix + "[" + node.size() + "]";
		}
		if (node.isObject()) {
			return prefix + "{" + node.size() + "}";
		}
		String text = node.isTextual() ? '"' + node.asText() + '"' : node.toString();
		return prefix + (text.length() > 200 ? text.substring(0, 200) + DUMMY : text);
	}

	/** the JavaScript expression of a path under a variable: rows[0].name, rows[0]["a b"] */
	public static String toJavaScript(String variable, List<Object> path) {
		StringBuilder sb = new StringBuilder(variable == null ? "" : variable);
		for (Object segment : path) {
			if (segment instanceof Integer) {
				sb.append('[').append(segment).append(']');
			} else if (identifier.matcher(segment.toString()).matches()) {
				sb.append('.').append(segment);
			} else {
				sb.append("[\"").append(segment.toString().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"]");
			}
		}
		return sb.toString();
	}

	/** the JSON path of a path under a variable: $.rows[0].name, $.rows[0]['a b'] */
	public static String toJsonPath(String variable, List<Object> path) {
		StringBuilder sb = new StringBuilder("$");
		if (variable != null && !variable.isEmpty()) {
			sb.append('.').append(variable);
		}
		for (Object segment : path) {
			if (segment instanceof Integer) {
				sb.append('[').append(segment).append(']');
			} else if (identifier.matcher(segment.toString()).matches()) {
				sb.append('.').append(segment);
			} else {
				sb.append("['").append(segment.toString().replace("'", "\\'")).append("']");
			}
		}
		return sb.toString();
	}

	/** the path of a simple JavaScript expression under a variable (variable.a[0]["b"]), or null for another expression */
	public static List<Object> parseJavaScript(String variable, String expression) {
		if (expression == null || variable == null) {
			return null;
		}
		String s = expression.trim();
		if (!s.startsWith(variable)) {
			return null;
		}
		s = s.substring(variable.length());
		List<Object> path = new ArrayList<>();
		java.util.regex.Matcher m = Pattern.compile("\\G(?:\\.([A-Za-z_$][A-Za-z0-9_$]*)|\\[(\\d+)\\]|\\[\"((?:[^\"\\\\]|\\\\.)*)\"\\])").matcher(s);
		int end = 0;
		while (m.find()) {
			if (m.group(1) != null) {
				path.add(m.group(1));
			} else if (m.group(2) != null) {
				path.add(Integer.valueOf(m.group(2)));
			} else {
				path.add(m.group(3).replace("\\\"", "\"").replace("\\\\", "\\"));
			}
			end = m.end();
		}
		return end == s.length() ? path : null;
	}
}
