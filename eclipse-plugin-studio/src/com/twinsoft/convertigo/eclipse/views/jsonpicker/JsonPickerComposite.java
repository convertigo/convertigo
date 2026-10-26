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

import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

/**
 * The JSON picker: the steps before a step that give JSON (native steps, JSON to XML steps), the JSON each one
 * gave at its last execution in the Studio, and the JavaScript expression (and JSON path) of the value picked in it.
 * It is the JSON counterpart of the XPath source picker.
 */
public class JsonPickerComposite extends Composite {
	private final Step step;
	private final List<Step> steps = new ArrayList<>();
	private Table table;
	private JsonTree tree;
	private Label info;
	private Text expression;
	private Label jsonPath;
	private boolean updating = false;

	/** the steps before step, and step itself with includeStep (to look at its own JSON) */
	public JsonPickerComposite(Composite parent, Step step, String purpose, boolean includeStep) {
		super(parent, SWT.NONE);
		this.step = step;
		steps.addAll(NativeJson.getPreviousJsonSteps(step));
		if (includeStep && NativeJson.isJsonProducer(step)) {
			steps.add(step);
		}
		create(purpose);
	}

	public JsonPickerComposite(Composite parent, Step step, String purpose) {
		this(parent, step, purpose, false);
	}

	private void create(String purpose) {
		setLayout(new GridLayout(2, false));

		Label title = new Label(this, SWT.WRAP);
		title.setText("Pick a value in the JSON of a step of '" + step.getSequence().getName() + "'"
				+ (purpose != null ? " for " + purpose : "")
				+ ". The samples are the JSON of the last execution of the sequence in the Studio.");
		title.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		SashForm sash = new SashForm(this, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		table = new Table(sash, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		for (String[] c : new String[][] { { "Step", "170" }, { "Variable", "110" }, { "Last sample", "110" } }) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(c[0]);
			column.setWidth(Integer.parseInt(c[1]));
		}
		for (Step s : steps) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setData(s);
			item.setImage(ViewImageProvider.getImageFromCache(MySimpleBeanInfo.getIconName(s, BeanInfo.ICON_COLOR_16x16), s));
			item.setText(new String[] { s.getName(), NativeJson.getVariableName(s), describe(NativeJson.getSample(s)) });
		}
		table.addListener(SWT.Selection, e -> showStep(selectedStep(), null));

		tree = new JsonTree(sash, SWT.NONE);
		tree.setSelectionListener(path -> {
			Step s = selectedStep();
			if (s != null && !updating) {
				setExpressionText(JsonTree.toJavaScript(NativeJson.getVariableName(s), path), path, s);
			}
		});
		sash.setWeights(new int[] { 38, 62 });

		info = new Label(this, SWT.WRAP);
		info.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		new Label(this, SWT.NONE).setText("Expression:");
		expression = new Text(this, SWT.BORDER | SWT.SINGLE);
		expression.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		expression.addModifyListener(e -> {
			if (!updating) {
				jsonPath.setText(jsonPathOf(expression.getText()));
			}
		});

		new Label(this, SWT.NONE).setText("JSON path:");
		jsonPath = new Label(this, SWT.NONE);
		jsonPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		if (steps.isEmpty()) {
			info.setText("No step before '" + step.getName() + "' gives JSON: the expression can use any variable of the sequence.");
		}
	}

	private static String describe(NativeStepOutput sample) {
		if (sample == null) {
			return "none";
		}
		if (sample.getNote() != null && sample.getJson().isEmpty()) {
			return sample.getNote();
		}
		return sample.getCount() >= 0 ? sample.getCount() + (sample.getJson().startsWith("[") ? " items" : " keys") : "value";
	}

	private Step selectedStep() {
		TableItem[] selection = table.getSelection();
		return selection.length == 1 ? (Step) selection[0].getData() : null;
	}

	private void showStep(Step s, List<Object> path) {
		if (s == null) {
			return;
		}
		NativeStepOutput sample = NativeJson.getSample(s);
		String variable = NativeJson.getVariableName(s);
		updating = true;
		try {
			if (sample == null || sample.getJson().isEmpty()) {
				tree.setJson(variable, null);
				info.setText(sample == null
						? "'" + s.getName() + "' has no sample yet: execute the sequence in the Studio to see its JSON. Its value is in the variable '" + variable + "'."
						: "'" + s.getName() + "' writes its rows in stream into the response: see them in the JSON output of the sequence.");
			} else {
				tree.setJson(variable, sample.getJson());
				info.setText("'" + s.getName() + "' gave " + describe(sample) + " in '" + variable + "'"
						+ (sample.isTruncated() ? " (sample cut at " + NativeStepOutput.MAX_LENGTH + " characters)" : "") + ".");
			}
			if (path != null) {
				tree.select(path);
			}
		} finally {
			updating = false;
		}
		if (path == null) {
			setExpressionText(variable, new ArrayList<>(), s);
		}
		layout(true, true);
	}

	private void setExpressionText(String text, List<Object> path, Step s) {
		updating = true;
		try {
			expression.setText(text);
			jsonPath.setText(JsonTree.toJsonPath(NativeJson.getVariableName(s), path));
		} finally {
			updating = false;
		}
	}

	private String jsonPathOf(String text) {
		for (Step s : steps) {
			String variable = NativeJson.getVariableName(s);
			List<Object> path = JsonTree.parseJavaScript(variable, text);
			if (path != null) {
				return JsonTree.toJsonPath(variable, path);
			}
		}
		return "(a JavaScript expression)";
	}

	/** selects the step and the value of an expression written by the picker */
	public void setExpression(String text) {
		expression.setText(text == null ? "" : text);
		for (int i = steps.size() - 1; i >= 0; i--) {
			Step s = steps.get(i);
			List<Object> path = JsonTree.parseJavaScript(NativeJson.getVariableName(s), text);
			if (path != null) {
				table.setSelection(i);
				showStep(s, path);
				updating = true;
				expression.setText(text);
				updating = false;
				jsonPath.setText(JsonTree.toJsonPath(NativeJson.getVariableName(s), path));
				return;
			}
		}
		jsonPath.setText(jsonPathOf(text));
	}

	/** selects a step of the picker, to show its JSON */
	public void selectStep(Step s) {
		int i = steps.indexOf(s);
		if (i >= 0) {
			table.setSelection(i);
			showStep(s, null);
		}
	}

	public String getExpression() {
		return expression.getText();
	}
}
