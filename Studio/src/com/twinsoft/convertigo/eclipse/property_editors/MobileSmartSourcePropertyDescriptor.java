/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class MobileSmartSourcePropertyDescriptor extends PropertyDescriptor {
	public DatabaseObjectTreeObject databaseObjectTreeObject;
	private boolean readOnly = false;
	private String[] labels;
	
	public MobileSmartSourcePropertyDescriptor(Object id, String displayName, String[] labels, boolean readOnly) {
		super(id, displayName);
		this.labels = labels;
		this.readOnly = readOnly;
	}

    public CellEditor createPropertyEditor(Composite parent) {
    	MobileSmartSourceTypeCellEditor editor = new MobileSmartSourceTypeCellEditor(parent, readOnly ? SWT.READ_ONLY:SWT.NONE, labels);
    	editor.databaseObjectTreeObject = this.databaseObjectTreeObject;
    	editor.propertyDescriptor = this;
        return editor;
    }

	public boolean isReadOnly() {
		return readOnly;
	}
	    
    public static String[] getTags(DatabaseObjectTreeObject databaseObjectTreeObject, String propertyName) {
    	DatabaseObject bean = (DatabaseObject) databaseObjectTreeObject.getObject();
    	ITagsProperty tagsProperty = null;
		
		if (bean instanceof ITagsProperty) {
			tagsProperty = (ITagsProperty) bean;
		}
		else {
			return new String[] { "" };
		}

		String[] sResults = tagsProperty.getTagsForProperty(propertyName);
		return sResults;
    }
}
