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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.lang.reflect.Method;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class DynamicInfoPropertyDescriptor extends PropertyDescriptor {

	private DatabaseObjectTreeObject databaseObjectTreeObject;
	private Method getInfoMethod;
	private String propertyName;
	
	public DynamicInfoPropertyDescriptor(Object id, String displayName, Method getInfoMethod,
			DatabaseObjectTreeObject databaseObjectTreeObject, String propertyName) {
		super(id, displayName);
		this.databaseObjectTreeObject = databaseObjectTreeObject;
		this.getInfoMethod = getInfoMethod;
		this.propertyName = propertyName;
	}
	
	public CellEditor createPropertyEditor(Composite parent) {
        CellEditor editor = new TextCellEditor(parent, SWT.READ_ONLY);
        editor.getControl().setEnabled(false);
        if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
		return editor;
	}
	
	
    @Override
	public ILabelProvider getLabelProvider() {
        if (isLabelProviderSet()) {
			return super.getLabelProvider();
		}
        
        return new LabelProvider() {
			@Override
			public String getText(Object element) {
				return getInfo();
			}
        	
        };
	}

	private String getInfo() {
    	String info;
		try {
			info = (String) getInfoMethod.invoke(null, new Object[] { databaseObjectTreeObject, propertyName } );
		} catch (Exception e) {
			info = "";
		}
		return info;
    }
	
}
