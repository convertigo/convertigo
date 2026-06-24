/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by the Free Software Foundation; either
 * version  3  of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class JsonInfoPropertyDescriptor extends PropertyDescriptor {

	private final String dialogTitle;

	public JsonInfoPropertyDescriptor(Object id, String displayName, String dialogTitle) {
		super(id, displayName);
		this.dialogTitle = dialogTitle == null || dialogTitle.isBlank() ? displayName : dialogTitle;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		var editor = new JsonInfoCellEditor(parent, dialogTitle);
		if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
		return editor;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		if (isLabelProviderSet()) {
			return super.getLabelProvider();
		}
		return new LabelProvider() {
			@Override
			public String getText(Object element) {
				return compactSummary(element);
			}
		};
	}

	public static void openDialog(Shell shell, String title, Object value) {
		new JsonInfoDialog(shell, title, value).open();
	}

	private static class JsonInfoCellEditor extends DialogCellEditor {

		private final String dialogTitle;

		JsonInfoCellEditor(Composite parent, String dialogTitle) {
			super(parent);
			this.dialogTitle = dialogTitle;
		}

		@Override
		protected void updateContents(Object value) {
			var label = getDefaultLabel();
			if (label != null) {
				label.setText(compactSummary(value));
			}
		}

		@Override
		protected Object openDialogBox(Control cellEditorWindow) {
			var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			openDialog(shell, dialogTitle, getValue());
			return getValue();
		}
	}

	private static class JsonInfoDialog extends Dialog {

		private final String title;
		private final Object value;

		JsonInfoDialog(Shell parentShell, String title, Object value) {
			super(parentShell);
			this.title = title;
			this.value = value;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(title);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			var area = (Composite) super.createDialogArea(parent);
			area.setLayout(new GridLayout(1, false));

			var summary = new Label(area, SWT.NONE);
			summary.setText(compactSummary(value));
			summary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			var sash = new SashForm(area, SWT.HORIZONTAL);
			sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			var tree = new Tree(sash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
			fillTree(tree, value);

			var text = new Text(sash, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			text.setEditable(false);
			text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
			text.setText(prettyJson(value));

			sash.setWeights(new int[] { 40, 60 });
			return area;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
		}

		@Override
		protected Point getInitialSize() {
			return new Point(900, 650);
		}

		@Override
		protected boolean isResizable() {
			return true;
		}
	}

	public static String compactSummary(Object value) {
		var parsed = parseJson(value);
		if (parsed == null) {
			var text = value == null ? "" : String.valueOf(value).trim();
			return text.length() > 120 ? text.substring(0, 117) + "..." : text;
		}
		return schemaLabel("schema", parsed);
	}

	private static String prettyJson(Object value) {
		var parsed = parseJson(value);
		try {
			if (parsed instanceof JSONObject json) {
				return json.toString(2);
			}
			if (parsed instanceof JSONArray array) {
				return array.toString(2);
			}
		} catch (Exception e) {
		}
		return value == null ? "" : String.valueOf(value);
	}

	private static Object parseJson(Object value) {
		if (value instanceof JSONObject || value instanceof JSONArray) {
			return value;
		}
		if (value == null) {
			return null;
		}
		var text = String.valueOf(value).trim();
		if (text.isEmpty()) {
			return null;
		}
		try {
			if (text.startsWith("{")) {
				return new JSONObject(text);
			}
			if (text.startsWith("[")) {
				return new JSONArray(text);
			}
		} catch (Exception e) {
		}
		return null;
	}

	private static void fillTree(Tree tree, Object value) {
		var parsed = parseJson(value);
		if (parsed == null) {
			var item = new TreeItem(tree, SWT.NONE);
			item.setText("value");
			return;
		}
		var count = new int[] { 0 };
		var root = new TreeItem(tree, SWT.NONE);
		root.setText(schemaLabel("schema", parsed));
		addChildren(root, parsed, count);
		expandToLevel(root, 2);
	}

	private static void addChildren(TreeItem parent, Object value, int[] count) {
		if (count[0] > 1000) {
			var item = new TreeItem(parent, SWT.NONE);
			item.setText("...");
			return;
		}
		if (value instanceof JSONObject json) {
			var properties = json.optJSONObject("properties");
			if (properties != null) {
				for (var key : keys(properties)) {
					var childValue = properties.opt(key);
					var item = new TreeItem(parent, SWT.NONE);
					item.setText(schemaLabel(key, childValue));
					count[0]++;
					addChildren(item, childValue, count);
				}
				return;
			}
			if (json.has("items")) {
				var childValue = json.opt("items");
				var item = new TreeItem(parent, SWT.NONE);
				item.setText(schemaLabel("[0]", childValue));
				count[0]++;
				addChildren(item, childValue, count);
				return;
			}
			for (var key : keys(json)) {
				if ("type".equals(key) || "description".equals(key)) {
					continue;
				}
				var childValue = json.opt(key);
				if (childValue instanceof JSONObject || childValue instanceof JSONArray) {
					var item = new TreeItem(parent, SWT.NONE);
					item.setText(schemaLabel(key, childValue));
					count[0]++;
					addChildren(item, childValue, count);
				}
			}
		} else if (value instanceof JSONArray array) {
			for (int i = 0; i < array.length() && i < 100; i++) {
				var childValue = array.opt(i);
				var item = new TreeItem(parent, SWT.NONE);
				item.setText(schemaLabel("[" + i + "]", childValue));
				count[0]++;
				addChildren(item, childValue, count);
			}
		}
	}

	private static String schemaLabel(String name, Object value) {
		return name + " " + schemaType(value);
	}

	private static String schemaType(Object value) {
		if (value instanceof JSONObject json) {
			var type = json.optString("type", "");
			if ("array".equals(type)) {
				return "array<" + schemaType(json.opt("items")) + ">";
			}
			if (!type.isBlank()) {
				return type;
			}
			if (json.has("properties")) {
				return "object";
			}
			if (json.has("items")) {
				return "array";
			}
			return "object";
		}
		if (value instanceof JSONArray array) {
			return "array[" + array.length() + "]";
		}
		if (value == null || JSONObject.NULL.equals(value)) {
			return "null";
		}
		return value.getClass().getSimpleName().toLowerCase();
	}

	private static List<String> keys(JSONObject json) {
		var keys = new ArrayList<String>();
		for (Iterator<?> it = json.keys(); it.hasNext();) {
			keys.add(String.valueOf(it.next()));
		}
		return keys;
	}

	private static void expandToLevel(TreeItem item, int level) {
		if (level <= 0) {
			return;
		}
		item.setExpanded(true);
		for (var child : item.getItems()) {
			expandToLevel(child, level - 1);
		}
	}
}
