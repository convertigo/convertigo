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
import org.eclipse.jface.dialogs.Dialog;
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
	private Composite editor;

	public FlowPropertyCellEditor(Composite parent, FlowVirtualObjectTreeObject treeObject, String propertyName,
			JSONObject definition) {
		super(parent, SWT.NONE);
		this.treeObject = treeObject;
		this.propertyName = propertyName;
		this.definition = definition == null ? new JSONObject() : definition;
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
		return text == null || text.isDisposed() ? "" : text.getText();
	}

	@Override
	protected void doSetValue(Object value) {
		if (text != null && !text.isDisposed()) {
			text.setText(value == null ? "" : value.toString());
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
				if (composite != null) {
					text.setText(composite.getValue());
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
