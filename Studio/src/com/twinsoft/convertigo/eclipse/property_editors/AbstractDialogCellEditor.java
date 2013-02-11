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

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public abstract class AbstractDialogCellEditor extends DialogCellEditor {
    protected String dialogTitle = "Table editor";
    protected Class<? extends Composite> dialogCompositeClass;
    public PropertyDescriptor propertyDescriptor;
    public DatabaseObjectTreeObject databaseObjectTreeObject;

    public AbstractDialogCellEditor(Composite parent) {
    	this(parent, SWT.NONE);
    }
        
    public AbstractDialogCellEditor(Composite parent, int style) {
    	super(parent, style);
    }
    
	@Override
	protected void updateContents(Object value) {
		Label labelCtrl = getDefaultLabel();
		if (labelCtrl == null)
			return;
		 String text = "";//$NON-NLS-1$
		 if (value != null) {
			 boolean isMasked = (getStyle() & SWT.PASSWORD) == 0;
			 String regexp = value instanceof String ? ".":"[^\\[\\]\\,]";
			 text = value.toString();
			 text = isMasked ? text:text.replaceAll(regexp, "*");
		 }
		 labelCtrl.setText(text);
	}

	protected Object openDialogBox(Control cellEditorWindow) {
		Shell parent = ConvertigoPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
    	EditorFrameworkDialog dialog = new EditorFrameworkDialog(parent, dialogCompositeClass, this);

		int userResponse = dialog.open();
		
		if (userResponse == Window.OK) {
			return dialog.newValue;
		}
		else {
			return null;
		}
    }

	public Object getEditorData() {
		return databaseObjectTreeObject.getPropertyValue(propertyDescriptor.getId());
	}
    
}
