/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileInPlaceEditorInput;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.Engine;

public class GenericTextEditor extends AbstractDialogCellEditor {

	public GenericTextEditor(Composite parent) {
		this(parent, SWT.NONE);
	}
	
	public GenericTextEditor(Composite parent, int style) {
		super(parent, style);

		dialogTitle = "Text expression";
		dialogCompositeClass = TextEditorComposite.class;
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		if (dialogCompositeClass != TextEditorComposite.class) {
			return super.openDialogBox(cellEditorWindow);
		}
		try {
			IWorkbenchPage activePage = PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage();
			if (activePage == null) {
				return null;
			}
			
			String propertyName = (String) propertyDescriptor.getId();
			java.beans.PropertyDescriptor[] propertyDescriptors = databaseObjectTreeObject.databaseObjectBeanInfo.getPropertyDescriptors();
			java.beans.PropertyDescriptor propertyDescriptor = null;
			for (java.beans.PropertyDescriptor pd: propertyDescriptors) {
				if (propertyName.equals(pd.getName())) {
					propertyDescriptor = pd;
					break;
				}
			}
			String extension = (propertyDescriptor != null &&
					propertyDescriptor.getValue(MySimpleBeanInfo.GENERIC_EDITOR_EXTENSION) != null) ?
							propertyDescriptor.getValue(MySimpleBeanInfo.GENERIC_EDITOR_EXTENSION).toString() : "txt";
			
			DatabaseObject dbo = databaseObjectTreeObject.getObject();
			IFile file = databaseObjectTreeObject.getProjectTreeObject().getFile("_private/editor/" + dbo.getShortQName() + "/" + propertyName + "." + extension);
			SwtUtils.fillFile(file, getValue().toString());
			
			FileInPlaceEditorInput input = new FileInPlaceEditorInput(file);
			IEditorPart editor = activePage.openEditor(input, "org.eclipse.ui.genericeditor.GenericEditor");
			editor.addPropertyListener((Object source, int propId) -> {
				if (propId == IEditorPart.PROP_DIRTY && !((IEditorPart) source).isDirty()) {
					try (InputStream is = file.getContents()) {
						setNewValue(IOUtils.toString(is, "UTF-8"));
					} catch (Exception e) {
						Engine.logStudio.error("Failed to save " + file.getName(), e);
					}
				}
			});
		} catch (PartInitException e) {
			Engine.logStudio.error("failed to open editor", e);
		}
		return null;
	}
	
	protected void setNewValue(Object newValue) {
		databaseObjectTreeObject.setPropertyValue((String) propertyDescriptor.getId(), newValue);
		databaseObjectTreeObject.hasBeenModified(true);
	}
}
