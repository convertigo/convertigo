/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Item;

public class TableEditorCellModifier implements ICellModifier {

	protected TableViewer tableViewer;
	private TableEditorComposite tableEditorComposite;
	
	public TableEditorCellModifier(TableEditorComposite tableEditorComposite, TableViewer tableViewer) {
		this.tableViewer = tableViewer;
		this.tableEditorComposite = tableEditorComposite;
	}
	
	public boolean canModify(Object element, String property) {
		return true;
	}

	public Object getValue(Object element, String property) {
		int columnIndex = Arrays.asList(tableViewer.getColumnProperties()).indexOf(property);

		CellEditor[] cellEditors = tableViewer.getCellEditors();
		CellEditor cellEditor = cellEditors[columnIndex];
		boolean isComboBoxEditor = cellEditor instanceof ComboBoxCellEditor;
		boolean isTextCellEditor = cellEditor instanceof TextCellEditor;
		
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}
		
		TableEditorRow row = (TableEditorRow) element;
		Object object = row.getValue(columnIndex);
		if (isComboBoxEditor) {
			int index = Arrays.asList(((ComboBoxCellEditor)cellEditor).getItems()).indexOf(object.toString());
			object = new Integer(index);
		}
		
		if (isTextCellEditor && (!(object instanceof String))) {
			object  = object.toString();
		}
		
		return object;
	}

	public void modify(Object element, String property, Object value) {
		int columnIndex = Arrays.asList(tableViewer.getColumnProperties()).indexOf(property);

		CellEditor[] cellEditors = tableViewer.getCellEditors();
		CellEditor cellEditor = cellEditors[columnIndex];
		boolean isComboBoxEditor = cellEditor instanceof ComboBoxCellEditor;
		boolean isTextCellEditor = cellEditor instanceof TextCellEditor;
		
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}
		
		TableEditorRow row = (TableEditorRow) element;
		Object object = row.getValue(columnIndex);

		Class<?> objectClass = object.getClass();

		if (isComboBoxEditor) {
			String text = ((ComboBoxCellEditor)cellEditor).getItems()[((Integer)value).intValue()];
			try {
				Constructor<?> constructor = objectClass.getConstructor(new Class[]{String.class});
				value = constructor.newInstance(new Object[]{text});
			} catch (Exception e) {
				value = new String(text);
			}
		}
		
		if (isTextCellEditor && (!(object instanceof String))) {
			try {
				Constructor<?> constructor = objectClass.getConstructor(new Class[]{String.class});
				value = constructor.newInstance(new Object[]{value});
			} catch (Exception e) {
				value = object;
			}
		}
		
		row.setValue(value, columnIndex);
		
		tableEditorComposite.getRowList().rowChanged(row);
	}

}
