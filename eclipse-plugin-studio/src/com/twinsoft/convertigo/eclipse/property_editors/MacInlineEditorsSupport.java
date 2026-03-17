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

import java.lang.reflect.Constructor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class MacInlineEditorsSupport<R> {

	interface ValueGetter<R> {
		Object get(R row, int columnIndex);
	}

	interface ValueSetter<R> {
		void set(R row, int columnIndex, Object value);
	}

	private static final boolean ENABLED = System.getProperty("os.name", "").toLowerCase().startsWith("mac");

	static boolean isEnabled() {
		return ENABLED;
	}

	private final Supplier<R> selectedRowSupplier;
	private final ValueGetter<R> valueGetter;
	private final ValueSetter<R> valueSetter;
	private final Consumer<R> rowChanged;
	private Text[] editors = null;
	private boolean refreshing = false;

	MacInlineEditorsSupport(Composite parent, TableViewer tableViewer, String[] columnNames, Supplier<R> selectedRowSupplier,
			ValueGetter<R> valueGetter, ValueSetter<R> valueSetter, Consumer<R> rowChanged) {
		this.selectedRowSupplier = selectedRowSupplier;
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
		this.rowChanged = rowChanged;

		if (!ENABLED) {
			return;
		}

		createEditorArea(parent, columnNames);
		tableViewer.addSelectionChangedListener(event -> refreshEditors());
		refreshEditors();
	}

	private void createEditorArea(Composite parent, String[] columnNames) {
		Composite editorArea = new Composite(parent, SWT.NONE);
		editorArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		editorArea.setLayout(new GridLayout(2, false));

		editors = new Text[columnNames.length];
		for (int i = 0; i < columnNames.length; i++) {
			Label label = new Label(editorArea, SWT.NONE);
			label.setText(columnNames[i]);
			Text text = new Text(editorArea, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			final int columnIndex = i;
			text.addModifyListener(e -> {
				if (refreshing) {
					return;
				}
				R row = selectedRowSupplier.get();
				if (row == null) {
					return;
				}
				Object originalValue = valueGetter.get(row, columnIndex);
				Object convertedValue = convertValue(originalValue, text.getText());
				valueSetter.set(row, columnIndex, convertedValue);
				rowChanged.accept(row);
			});
			editors[i] = text;
		}
	}

	private void refreshEditors() {
		if (editors == null) {
			return;
		}

		R row = selectedRowSupplier.get();
		refreshing = true;
		try {
			for (int i = 0; i < editors.length; i++) {
				Text text = editors[i];
				if (row == null) {
					text.setText("");
					text.setEnabled(false);
				} else {
					Object value = valueGetter.get(row, i);
					text.setText(value == null ? "" : value.toString());
					text.setEnabled(true);
				}
			}
		} finally {
			refreshing = false;
		}
	}

	private static Object convertValue(Object originalValue, String textValue) {
		if (originalValue == null || originalValue instanceof String) {
			return textValue;
		}

		Class<?> objectClass = originalValue.getClass();
		try {
			Constructor<?> constructor = objectClass.getConstructor(String.class);
			return constructor.newInstance(textValue);
		} catch (Exception e) {
			return originalValue;
		}
	}
}
