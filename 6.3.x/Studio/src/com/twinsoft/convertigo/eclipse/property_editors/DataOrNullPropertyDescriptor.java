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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class DataOrNullPropertyDescriptor extends PropertyDescriptor implements INullPropertyDescriptor {

	private Boolean isNull = Boolean.FALSE;
	protected CellEditor editor = null;
	Class<?> dataOrNullCellEditorClass = null;
	private int style = SWT.NONE;
	
	public DataOrNullPropertyDescriptor(Object id, String displayName, Class<?> dataOrNullCellEditorClass, final int style) {
		super(id, displayName);
		this.dataOrNullCellEditorClass = dataOrNullCellEditorClass;
		this.style = style;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		if (dataOrNullCellEditorClass != null) {
    		try {
    			Constructor<?> constructor = dataOrNullCellEditorClass.getConstructor(new Class[] { Composite.class, int.class });
    			editor = (CellEditor)constructor.newInstance(new Object[] { parent, style });
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unexpected exception");
			}
			
			if (editor != null) {
				((INullEditor)editor).setNullProperty(isNull);
		        if (getValidator() != null) {
					editor.setValidator(getValidator());
				}
			}
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
