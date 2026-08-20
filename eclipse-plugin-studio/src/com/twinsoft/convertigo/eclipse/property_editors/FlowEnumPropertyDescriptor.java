/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.Arrays;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class FlowEnumPropertyDescriptor extends PropertyDescriptor {

	private final String[] values;

	public FlowEnumPropertyDescriptor(Object id, String displayName, String[] values) {
		super(id, displayName);
		this.values = values == null ? new String[0] : Arrays.copyOf(values, values.length);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		var editor = new FlowEnumCellEditor(parent, values);
		if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
		return editor;
	}

	private static class FlowEnumCellEditor extends CellEditor {

		private final String[] values;
		private CCombo combo;

		FlowEnumCellEditor(Composite parent, String[] values) {
			super(parent, SWT.READ_ONLY);
			this.values = Arrays.copyOf(values, values.length);
			combo.setItems(this.values);
		}

		@Override
		public void activate(ColumnViewerEditorActivationEvent event) {
			super.activate(event);
			combo.getDisplay().asyncExec(() -> {
				if (!combo.isDisposed()) {
					combo.setListVisible(true);
				}
			});
		}

		@Override
		protected Control createControl(Composite parent) {
			combo = new CCombo(parent, getStyle());
			combo.setFont(parent.getFont());
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent event) {
					applyEditorValueAndDeactivate();
				}
			});
			combo.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent event) {
					keyReleaseOccured(event);
				}
			});
			combo.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent event) {
					FlowEnumCellEditor.this.focusLost();
				}
			});
			return combo;
		}

		@Override
		protected Object doGetValue() {
			return combo.getText();
		}

		@Override
		protected void doSetFocus() {
			combo.setFocus();
		}

		@Override
		protected void doSetValue(Object value) {
			var text = value == null ? "" : String.valueOf(value);
			var index = Arrays.asList(values).indexOf(text);
			if (index < 0 && !text.isBlank()) {
				combo.add(text);
				index = combo.getItemCount() - 1;
			}
			combo.select(index);
		}

		private void applyEditorValueAndDeactivate() {
			markDirty();
			var valid = isCorrect(combo.getText());
			setValueValid(valid);
			if (valid) {
				fireApplyEditorValue();
				deactivate();
			}
		}
	}
}
