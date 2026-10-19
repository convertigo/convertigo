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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.JsonStreamRowsStep;
import com.twinsoft.convertigo.beans.steps.JsonTransformStep;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.NativeJson;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

/**
 * Edits a list property of a native JSON step: the value is a JavaScript expression, written by this editor as a
 * JSON array (of names, or of objects with some fields) or null, or kept as an expression of the sequence.
 */
public abstract class JsonListEditorComposite extends AbstractDialogComposite {
	private static final ObjectMapper mapper = new ObjectMapper();

	public static class Field {
		final String key;
		final String label;
		final String[] choices;

		public Field(String key, String label, String[] choices) {
			this.key = key;
			this.label = label;
			this.choices = choices;
		}
	}

	private final Field[] fields;
	private final List<Map<String, String>> rows = new ArrayList<>();
	private final String[] columnNames;
	private Button modeList, modeNull, modeExpression;
	private TableViewer viewer;
	private Button add, remove, up, down;
	private Text expression;
	private Label preview;

	protected JsonListEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor, Field[] fields) {
		super(parent, style, cellEditor);
		this.fields = fields;
		this.columnNames = findColumnNames((Step) cellEditor.databaseObjectTreeObject.getObject());
		create();
		Object value = cellEditor.getEditorData();
		load(value == null ? "" : value.toString());
	}

	private void create() {
		setLayout(new GridLayout(2, false));
		Label help = new Label(this, SWT.WRAP);
		help.setText(cellEditor.propertyDescriptor.getDisplayName() + ": a list written as a JSON array, null, or a JavaScript expression of the sequence."
				+ (columnNames.length > 0 ? " The column names proposed come from the last execution in the Studio." : ""));
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		gd.widthHint = 620;
		help.setLayoutData(gd);

		Composite modes = new Composite(this, SWT.NONE);
		modes.setLayout(new GridLayout(3, false));
		modes.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		modeList = new Button(modes, SWT.RADIO);
		modeList.setText("List");
		modeNull = new Button(modes, SWT.RADIO);
		modeNull.setText("null");
		modeExpression = new Button(modes, SWT.RADIO);
		modeExpression.setText("JavaScript expression");
		for (Button b : new Button[] { modeList, modeNull, modeExpression }) {
			b.addListener(SWT.Selection, e -> refreshState());
		}

		viewer = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		for (Field field : fields) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(field.label);
			column.getColumn().setWidth(fields.length == 1 ? 420 : 150);
			column.setLabelProvider(new ColumnLabelProvider() {
				@SuppressWarnings("unchecked")
				@Override
				public String getText(Object element) {
					return ((Map<String, String>) element).getOrDefault(field.key, "");
				}
			});
			column.setEditingSupport(new FieldEditing(field));
		}
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 220;
		viewer.getTable().setLayoutData(gd);
		viewer.setInput(rows);
		viewer.addSelectionChangedListener(e -> refreshState());

		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayout(new GridLayout(1, true));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		add = button(buttons, "Add", () -> {
			Map<String, String> row = new LinkedHashMap<>();
			if (fields[0].choices != null) {
				row.put(fields[0].key, fields[0].choices[0]);
			}
			rows.add(row);
			viewer.refresh();
			viewer.editElement(row, fields[0].choices != null && fields.length > 1 ? 1 : 0);
		});
		remove = button(buttons, "Remove", () -> {
			int i = viewer.getTable().getSelectionIndex();
			if (i >= 0) {
				rows.remove(i);
			}
		});
		up = button(buttons, "Up", () -> move(-1));
		down = button(buttons, "Down", () -> move(1));

		new Label(this, SWT.NONE).setText("Expression:");
		new Label(this, SWT.NONE);
		expression = new Text(this, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd.heightHint = 60;
		expression.setLayoutData(gd);
		expression.addModifyListener(e -> updatePreview());

		preview = new Label(this, SWT.WRAP);
		preview.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
	}

	private Button button(Composite parent, String text, Runnable action) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		b.addListener(SWT.Selection, e -> {
			action.run();
			viewer.refresh();
			refreshState();
		});
		return b;
	}

	private void move(int delta) {
		int i = viewer.getTable().getSelectionIndex();
		int j = i + delta;
		if (i >= 0 && j >= 0 && j < rows.size()) {
			rows.add(j, rows.remove(i));
			viewer.refresh();
			viewer.getTable().setSelection(j);
		}
	}

	private class FieldEditing extends EditingSupport {
		private final Field field;
		private final String[] items;

		FieldEditing(Field field) {
			super(viewer);
			this.field = field;
			this.items = field.choices != null ? field.choices : "column".equals(field.key) ? columnNames : null;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			if (field.choices != null) {
				ComboBoxCellEditor editor = new ComboBoxCellEditor(viewer.getTable(), items, SWT.READ_ONLY);
				editor.setActivationStyle(ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
				return editor;
			}
			if (items != null && items.length > 0) {
				// the names known, or any name typed
				return new ComboBoxCellEditor(viewer.getTable(), items, SWT.NONE) {
					@Override
					protected Object doGetValue() {
						return ((CCombo) getControl()).getText();
					}

					@Override
					protected void doSetValue(Object value) {
						((CCombo) getControl()).setText(value == null ? "" : value.toString());
					}
				};
			}
			return new TextCellEditor(viewer.getTable());
		}

		@Override
		protected boolean canEdit(Object element) {
			return modeList.getSelection();
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Object getValue(Object element) {
			String value = ((Map<String, String>) element).getOrDefault(field.key, "");
			if (field.choices != null) {
				for (int i = 0; i < items.length; i++) {
					if (items[i].equals(value)) {
						return i;
					}
				}
				return 0;
			}
			return value;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void setValue(Object element, Object value) {
			String text = value instanceof Integer i ? (i >= 0 ? items[i] : "") : value == null ? "" : value.toString();
			if (text.isEmpty()) {
				((Map<String, String>) element).remove(field.key);
			} else {
				((Map<String, String>) element).put(field.key, text);
			}
			viewer.update(element, null);
			refreshState();
		}
	}

	/** the column names known for the rows of the step: the keys of its rows and its schema in the last samples */
	private static String[] findColumnNames(Step step) {
		Set<String> names = new LinkedHashSet<>();
		List<String> expressions = new ArrayList<>();
		if (step instanceof JsonTransformStep transform) {
			expressions.add(transform.getRows());
		}
		if (step instanceof JsonStreamRowsStep stream) {
			expressions.add(stream.getRows());
			expressions.add(stream.getSchema());
		}
		try {
			expressions.add((String) step.getClass().getMethod("getSchema").invoke(step));
		} catch (Exception e) {
			// no schema
		}
		for (Step previous : NativeJson.getPreviousJsonSteps(step)) {
			if (!expressions.contains(NativeJson.getVariableName(previous))) {
				continue;
			}
			NativeStepOutput sample = NativeJson.getSample(previous);
			if (sample == null || sample.getJson().isEmpty()) {
				continue;
			}
			try {
				JsonNode node = mapper.readTree(sample.getJson());
				if (node.isArray()) {
					for (int i = 0; i < Math.min(node.size(), 20); i++) {
						JsonNode item = node.get(i);
						if (item.isObject() && item.has("name") && item.get("name").isTextual() && item.size() > 1 && !item.has("id")) {
							names.add(item.get("name").asText());
						} else if (item.isObject()) {
							item.fieldNames().forEachRemaining(names::add);
						}
					}
				}
			} catch (Exception e) {
				// not JSON
			}
		}
		return names.toArray(new String[names.size()]);
	}

	private void load(String value) {
		rows.clear();
		String v = value.trim();
		boolean list = false;
		if (v.equals("null")) {
			modeNull.setSelection(true);
		} else {
			try {
				JsonNode node = v.isEmpty() ? mapper.createArrayNode() : mapper.readTree(v);
				if (node.isArray()) {
					list = true;
					for (JsonNode item : node) {
						Map<String, String> row = new LinkedHashMap<>();
						if (fields.length == 1 && item.isTextual()) {
							row.put(fields[0].key, item.asText());
						} else if (fields.length > 1 && item.isObject()) {
							for (Iterator<Map.Entry<String, JsonNode>> it = item.fields(); it.hasNext();) {
								Map.Entry<String, JsonNode> e = it.next();
								if (!e.getValue().isValueNode() || !isField(e.getKey())) {
									list = false;
								}
								row.put(e.getKey(), e.getValue().asText());
							}
						} else {
							list = false;
						}
						rows.add(row);
					}
				}
			} catch (Exception e) {
				list = false;
			}
			if (list) {
				modeList.setSelection(true);
			} else {
				rows.clear();
				modeExpression.setSelection(true);
			}
		}
		expression.setText(value);
		viewer.refresh();
		refreshState();
	}

	private boolean isField(String key) {
		for (Field f : fields) {
			if (f.key.equals(key)) {
				return true;
			}
		}
		return false;
	}

	private void refreshState() {
		boolean isList = modeList.getSelection();
		boolean selected = viewer.getTable().getSelectionIndex() >= 0;
		viewer.getTable().setEnabled(isList);
		add.setEnabled(isList);
		remove.setEnabled(isList && selected);
		up.setEnabled(isList && selected);
		down.setEnabled(isList && selected);
		expression.setEditable(modeExpression.getSelection());
		if (!modeExpression.getSelection()) {
			expression.setText(toValue());
		}
		updatePreview();
	}

	private void updatePreview() {
		if (preview != null && !preview.isDisposed()) {
			preview.setText("Value: " + getValue());
			preview.getParent().layout(true);
		}
	}

	private String toValue() {
		if (modeNull.getSelection()) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("[");
		for (Map<String, String> row : rows) {
			if (row.values().stream().allMatch(String::isEmpty)) {
				// a row added and left empty
				continue;
			}
			if (sb.length() > 1) {
				sb.append(", ");
			}
			if (fields.length == 1) {
				sb.append(quote(row.getOrDefault(fields[0].key, "")));
			} else {
				sb.append('{');
				boolean first = true;
				for (Field f : fields) {
					String v = row.get(f.key);
					if (v != null && !v.isEmpty()) {
						sb.append(first ? "" : ", ").append(quote(f.key)).append(": ").append(quote(v));
						first = false;
					}
				}
				sb.append('}');
			}
		}
		return sb.append(']').toString();
	}

	private static String quote(String s) {
		try {
			return mapper.writeValueAsString(s);
		} catch (Exception e) {
			return '"' + s + '"';
		}
	}

	@Override
	public Object getValue() {
		return modeExpression.getSelection() ? expression.getText() : toValue();
	}
}
