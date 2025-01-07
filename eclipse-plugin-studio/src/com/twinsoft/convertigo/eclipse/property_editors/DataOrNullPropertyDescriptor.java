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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class DataOrNullPropertyDescriptor extends DataPropertyDescriptor implements INullPropertyDescriptor {

	private Boolean isNull = Boolean.FALSE;
	
	public DataOrNullPropertyDescriptor(Object id, String displayName, Class<?> dataCellEditorClass, final int style, DatabaseObjectTreeObject databaseObjectTreeObject, PropertyDescriptor databaseObjectPropertyDescriptor) {
		super(id, displayName, dataCellEditorClass, style, databaseObjectTreeObject, databaseObjectPropertyDescriptor);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		super.createPropertyEditor(parent);
		if (editor != null) {
			((INullEditor)editor).setNullProperty(isNull);
		}
		return editor;
	}
	
	public void setNullProperty(Boolean isNull) {
		this.isNull = isNull;
		if (editor != null)
			this.isNull = ((INullEditor)editor).isNullProperty();
	}
	
	public Boolean isNullProperty() {
		if (editor != null)
			return ((INullEditor)editor).isNullProperty();
		return isNull;
	}
	
}
