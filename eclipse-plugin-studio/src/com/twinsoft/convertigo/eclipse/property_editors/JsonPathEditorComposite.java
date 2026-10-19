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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.fasterxml.jackson.databind.JsonNode;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.JsonTree;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.NativeJson;
import com.twinsoft.convertigo.engine.json.JsonPath;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

/** Picks a JSON path in the JSON text the step read (Read JSON value), at its last execution in the Studio. */
public class JsonPathEditorComposite extends AbstractDialogComposite {
	private JsonTree tree;
	private Text path;
	private Label value;
	private boolean updating = false;

	public JsonPathEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		setLayout(new GridLayout(2, false));
		Step step = (Step) cellEditor.databaseObjectTreeObject.getObject();
		NativeStepOutput sample = NativeJson.getSample(step);
		String input = sample == null ? null : sample.getInput();

		Label help = new Label(this, SWT.WRAP);
		help.setText(input != null
				? "Pick the value to read in the JSON '" + step.getName() + "' read at its last execution in the Studio. Paths: $ the whole value, .key, ['key'], [index], .* and [*] (the array of the values found)."
				: "Type a JSON path: $ the whole value, .key, ['key'], [index], .* and [*] (the array of the values found). Execute the sequence once in the Studio to pick it in the JSON '" + step.getName() + "' reads.");
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		gd.widthHint = 760;
		help.setLayoutData(gd);

		tree = new JsonTree(this, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.heightHint = 380;
		tree.setLayoutData(gd);
		tree.setSelectionListener(p -> {
			if (!updating) {
				path.setText(JsonTree.toJsonPath(null, p));
			}
		});

		new Label(this, SWT.NONE).setText("JSON path:");
		path = new Text(this, SWT.BORDER | SWT.SINGLE);
		path.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		path.addModifyListener(e -> showValue());

		value = new Label(this, SWT.WRAP);
		value.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		Object current = cellEditor.getEditorData();
		String text = current == null || current.toString().isEmpty() ? "$" : current.toString();
		updating = true;
		try {
			if (input != null) {
				tree.setJson("$", input);
				if (JsonPath.isJsonPath(text)) {
					tree.select(JsonPath.parse(text).getSegments());
				}
			} else {
				tree.setJson("", null);
			}
			path.setText(text);
		} catch (IllegalArgumentException e) {
			path.setText(text);
		} finally {
			updating = false;
		}
		showValue();
	}

	private void showValue() {
		String text = path.getText();
		try {
			JsonPath.parse(text);
			JsonNode node = tree.getSelectedNode();
			value.setText(node == null ? "" : "Value: " + abbreviate(node.isContainerNode() ? (node.isArray() ? "array of " : "object of ") + node.size() : node.toString()));
		} catch (IllegalArgumentException e) {
			value.setText("Invalid JSON path: " + e.getMessage());
		}
		layout(true);
	}

	private static String abbreviate(String s) {
		return s.length() > 200 ? s.substring(0, 200) + "…" : s;
	}

	@Override
	public Object getValue() {
		return path.getText();
	}
}
