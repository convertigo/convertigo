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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/model/ReferenceTreeObject.java $
 * $Author: nathalieh $
 * $Revision: 39934 $
 * $Date: 2015-06-11 19:30:12 +0200 (jeu., 11 juin 2015) $
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.mobile.ComponentFileEditorInput;

public class MobileComponentTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {
	
	public MobileComponentTreeObject(Viewer viewer, MobileComponent object) {
		super(viewer, object);
	}

	public MobileComponentTreeObject(Viewer viewer, MobileComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public MobileComponent getObject() {
		return (MobileComponent) super.getObject();
	}

	protected boolean isSymbolValue(Object value) {
		if (value != null) {
			String val = null;
			if (value instanceof String) {
				val = String.valueOf(value);
			} else if (value instanceof MobileSmartSourceType) {
				val = ((MobileSmartSourceType)value).getValue();
			}
			return val != null && val.startsWith("${") && val.endsWith("}");
		}
		return false;
	}
	
	@Override
	public void setPropertyValue(Object id, Object value) {
		if (isSymbolValue(value)) {
			ConvertigoPlugin.logError("Symbols are not allowed for mobile components", true);
			return;
		}
		super.setPropertyValue(id, value);
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}
	
	@Override
	public void launchEditor(String editorType) {
		DatabaseObjectTreeObject parent = getParentDatabaseObjectTreeObject();
		while (!(parent == null || parent instanceof MobileApplicationComponentTreeObject)) {
			parent = parent.getParentDatabaseObjectTreeObject();
		}
		
		if (parent != null) {
			ApplicationComponentEditor editor = ((MobileApplicationComponentTreeObject) parent).activeEditor();
			editor.highlightComponent(getObject());
		}
		
	}

	public void closeAllEditors() {
		MobileComponent mc = getObject();
		
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (activePage != null) {
			IEditorReference[] editorRefs = activePage.getEditorReferences();
			for (int i = 0; i < editorRefs.length; i++) {
				IEditorReference editorRef = (IEditorReference) editorRefs[i];
				try {
					IEditorInput editorInput = editorRef.getEditorInput();
					if (editorInput != null) {
						if (editorInput instanceof ComponentFileEditorInput) {
							if (((ComponentFileEditorInput)editorInput).is(mc) ||
								((ComponentFileEditorInput)editorInput).isChildOf(mc)) {
									activePage.closeEditor(editorRef.getEditor(false),false);
							}
						}
					}
				} catch(Exception e) {
					
				}
			}
		}
	}
}
