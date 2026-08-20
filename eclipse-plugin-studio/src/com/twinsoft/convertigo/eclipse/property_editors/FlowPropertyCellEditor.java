/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by  the Free Software Foundation; either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONTokener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FlowVirtualObjectTreeObject;

public class FlowPropertyCellEditor extends TextCellEditor {

	private final FlowVirtualObjectTreeObject treeObject;
	private final String propertyName;
	private final JSONObject definition;
	private boolean inlineEditable = true;
	private String rawValue = "";
	private Composite editor;

	public FlowPropertyCellEditor(Composite parent, FlowVirtualObjectTreeObject treeObject, String propertyName,
			JSONObject definition) {
		super(parent, SWT.NONE);
		this.treeObject = treeObject;
		this.propertyName = propertyName;
		this.definition = definition == null ? new JSONObject() : definition;
		inlineEditable = isInlineEditable(this.definition);
		if (text != null && !text.isDisposed()) {
			text.setEditable(inlineEditable);
			if (!inlineEditable) {
				text.setToolTipText("Use ... to edit this structured Flow property");
			}
		}
	}

	@Override
	protected Control createControl(Composite parent) {
		editor = new Composite(parent, getStyle()) {
			@Override
			public boolean isFocusControl() {
				return true;
			}
		};
		editor.setFont(parent.getFont());
		editor.setBackground(parent.getBackground());

		var layout = new GridLayout(2, false);
		layout.horizontalSpacing = layout.marginHeight = layout.marginWidth = layout.verticalSpacing = 0;
		editor.setLayout(layout);

		super.createControl(editor);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		for (Listener listener : text.getListeners(SWT.FocusOut)) {
			text.removeListener(SWT.FocusOut, listener);
		}

		var fontData = parent.getFont().getFontData()[0];
		fontData.setStyle(SWT.BOLD);
		fontData.setHeight(Math.round(fontData.getHeight() * 0.7f));
		var buttonFont = new Font(parent.getDisplay(), fontData);

		var button = new Button(editor, SWT.PUSH);
		button.setText("...");
		button.setToolTipText("Open Flow property editor");
		button.setFont(buttonFont);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		button.addDisposeListener(e -> buttonFont.dispose());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				openFlowEditor();
			}
		});
		button.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent event) {
			}

			@Override
			public void keyReleased(KeyEvent event) {
				if (event.character == '\u001b') {
					fireCancelEditor();
				}
				if (event.character == '\r') {
					fireApplyEditorValue();
					deactivate();
				}
			}
		});

		return editor;
	}

	@Override
	protected Object doGetValue() {
		return inlineEditable && text != null && !text.isDisposed() ? text.getText() : rawValue;
	}

	@Override
	protected void doSetValue(Object value) {
		rawValue = value == null ? "" : value.toString();
		if (text != null && !text.isDisposed()) {
			text.setText(inlineEditable ? rawValue : structuredSummary(rawValue));
		}
	}

	@Override
	protected void keyReleaseOccured(KeyEvent event) {
		if (event.character == '\r') {
			event.doit = false;
			fireApplyEditorValue();
			deactivate();
			return;
		}
		if (event.character == '\u001b') {
			fireCancelEditor();
		}
	}

	private void openFlowEditor() {
		var parent = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		var dialog = new Dialog(parent) {
			private FlowPropertyEditorComposite composite;

			@Override
			protected void createButtonsForButtonBar(Composite parent) {
				super.createButtonsForButtonBar(parent);
				if (composite != null) {
					composite.setValidityListener(valid -> {
						var button = getButton(IDialogConstants.OK_ID);
						if (button != null && !button.isDisposed()) {
							button.setEnabled(valid);
						}
					});
				}
			}

			@Override
			protected int getShellStyle() {
				return super.getShellStyle() | SWT.RESIZE | SWT.MAX;
			}

			@Override
			protected Control createDialogArea(Composite parent) {
				var area = (Composite) super.createDialogArea(parent);
				area.setLayout(new GridLayout(1, false));
				composite = new FlowPropertyEditorComposite(area, SWT.NONE, treeObject, propertyName, definition,
						String.valueOf(doGetValue()));
				composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				return area;
			}

			@Override
			protected Point getInitialSize() {
				var size = super.getInitialSize();
				var bounds = clientArea(getParentShell());
				return new Point(Math.min(Math.max(size.x, 820), bounds.width - 80),
						Math.min(Math.max(size.y, 560), bounds.height - 80));
			}

			@Override
			protected void configureShell(Shell shell) {
				super.configureShell(shell);
				shell.setText("Flow property: " + propertyName);
			}

			@Override
			protected void okPressed() {
				if (composite != null && !composite.isValueValid()) {
					return;
				}
				if (composite != null) {
					rawValue = composite.getValue();
					text.setText(inlineEditable ? rawValue : structuredSummary(rawValue));
					composite.applyAdditionalValues(propertyName);
				}
				super.okPressed();
			}
		};
		if (dialog.open() == Window.OK) {
			fireApplyEditorValue();
			deactivate();
		}
	}

	static boolean isInlineEditable(JSONObject definition) {
		var kind = definition.optString("kind", "").toLowerCase();
		var type = definition.optString("type", "").toLowerCase();
		return !switch (kind) {
		case "binding", "expression", "code", "requestable", "json", "schema", "object", "array" -> true;
		default -> switch (type) {
			case "binding", "expression", "code", "requestable", "json", "schema", "object", "array" -> true;
			default -> false;
		};
		};
	}

	static String structuredSummary(String raw) {
		var trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		try {
			var value = new JSONTokener(trimmed).nextValue();
			if (!(value instanceof JSONObject object)) {
				return value == null || JSONObject.NULL.equals(value) ? "null" : String.valueOf(value);
			}
			return switch (object.optString("mode", "")) {
			case "literal" -> summaryValue(object.opt("value"));
			case "expression" -> expressionSummary(object);
			case "source" -> sourceSummary(object);
			default -> object.toString();
			};
		} catch (Exception e) {
			return raw;
		}
	}

	private static String summaryValue(Object value) {
		return value == null || JSONObject.NULL.equals(value) ? "null" : String.valueOf(value);
	}

	private static String expressionSummary(JSONObject binding) {
		var expression = binding.optString("expression", "");
		var parts = binding.optJSONArray("parts");
		if (parts == null) {
			return expression;
		}
		var summary = new StringBuilder();
		JSONObject previous = null;
		for (var i = 0; i < parts.length(); i++) {
			var part = parts.optJSONObject(i);
			if (part == null) {
				continue;
			}
			var kind = part.optString("kind", "");
			var valuePart = "literal".equals(kind) || "source".equals(kind);
			if (valuePart && previous != null && needsImplicitJoin(previous)) {
				summary.append(" + ");
			}
			if ("literal".equals(kind)) {
				var value = part.opt("value");
				summary.append(value instanceof String ? JSONObject.quote((String) value) : summaryValue(value));
			} else if ("expression".equals(kind)) {
				summary.append(part.optString("expression", ""));
			} else if ("source".equals(kind)) {
				summary.append(sourceSummary(part));
			}
			previous = part;
		}
		return summary.toString();
	}

	private static boolean needsImplicitJoin(JSONObject previous) {
		var kind = previous.optString("kind", "");
		if ("literal".equals(kind) || "source".equals(kind)) {
			return true;
		}
		if (!"expression".equals(kind)) {
			return false;
		}
		var expression = previous.optString("expression", "").trim();
		if (expression.isEmpty()) {
			return false;
		}
		for (var operator : new String[] { "++", "--", "??", "&&", "||", "=>" }) {
			if (expression.endsWith(operator)) {
				return false;
			}
		}
		return "+-*/%?:,&|!<>=([{".indexOf(expression.charAt(expression.length() - 1)) < 0;
	}

	private static String sourceSummary(JSONObject binding) {
		var source = binding.optJSONObject("source");
		if (source == null) {
			return binding.toString();
		}
		var category = source.optString("category", "");
		var root = switch (category) {
		case "local" -> "@local." + source.optString("name", "");
		case "iteration" -> "@" + source.optString("scopeId", "") + "." + source.optString("value", "item");
		case "event" -> "@event";
		case "route" -> "@route";
		default -> "@" + source.optString("actionId", "");
		};
		var path = binding.optJSONArray("path");
		if (path == null) {
			return root;
		}
		var summary = new StringBuilder(root);
		for (var i = 0; i < path.length(); i++) {
			var segment = path.optJSONObject(i);
			if (segment == null) {
				continue;
			}
			if ("index".equals(segment.optString("kind"))) {
				summary.append('[').append(segment.optInt("index")).append(']');
			} else {
				summary.append('.').append(segment.optString("name"));
			}
		}
		return summary.toString();
	}

	private static Rectangle clientArea(Shell parentShell) {
		var display = parentShell == null ? PlatformUI.getWorkbench().getDisplay() : parentShell.getDisplay();
		var reference = parentShell == null || parentShell.isDisposed()
				? display.getCursorLocation()
				: new Point(parentShell.getBounds().x + parentShell.getBounds().width / 2,
						parentShell.getBounds().y + parentShell.getBounds().height / 2);
		for (var monitor : display.getMonitors()) {
			var area = monitor.getClientArea();
			if (area.contains(reference)) {
				return area;
			}
		}
		return display.getPrimaryMonitor().getClientArea();
	}
}
