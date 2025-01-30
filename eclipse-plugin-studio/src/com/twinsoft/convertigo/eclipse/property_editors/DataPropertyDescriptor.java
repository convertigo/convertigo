/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class DataPropertyDescriptor extends PropertyDescriptor {

	protected CellEditor editor = null;
	Class<?> dataCellEditorClass = null;
	private int style = SWT.NONE;
	DatabaseObjectTreeObject databaseObjectTreeObject;
	java.beans.PropertyDescriptor databaseObjectPropertyDescriptor;

	public DataPropertyDescriptor(Object id, String displayName, Class<?> dataCellEditorClass, final int style, DatabaseObjectTreeObject databaseObjectTreeObject, java.beans.PropertyDescriptor databaseObjectPropertyDescriptor) {
		super(id, displayName);
		this.dataCellEditorClass = dataCellEditorClass;
		this.style = style;
		this.databaseObjectTreeObject = databaseObjectTreeObject;
		this.databaseObjectPropertyDescriptor = databaseObjectPropertyDescriptor;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		if (dataCellEditorClass != null) {
			try {
				try {
					Constructor<?> constructor = dataCellEditorClass.getConstructor(new Class[] { Composite.class, int.class, DatabaseObjectTreeObject.class, java.beans.PropertyDescriptor.class });
					editor = (CellEditor) constructor.newInstance(new Object[] { parent, style, databaseObjectTreeObject, databaseObjectPropertyDescriptor });
				} catch (Exception e) {
					Constructor<?> constructor = dataCellEditorClass.getConstructor(new Class[] { Composite.class, int.class });
					editor = (CellEditor) constructor.newInstance(new Object[] { parent, style });
				}
				if (getValidator() != null) {
					editor.setValidator(getValidator());
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unexpected exception");
			}
	}
	return editor;
}
}
