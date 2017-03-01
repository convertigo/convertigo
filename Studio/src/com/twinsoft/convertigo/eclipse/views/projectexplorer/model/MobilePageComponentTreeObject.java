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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.PageComponentEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;

public class MobilePageComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject {

	public MobilePageComponentTreeObject(Viewer viewer, PageComponent object) {
		super(viewer, object);
		isDefault = getObject().isRoot;
	}

	public MobilePageComponentTreeObject(Viewer viewer, PageComponent object, boolean inherited) {
		super(viewer, object, inherited);
		isDefault = getObject().isRoot;
	}

	@Override
	public PageComponent getObject() {
		return (PageComponent) super.getObject();
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isRoot")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isDefault));
		}
		
		return super.testAttribute(target, name, value);
	}

	@Override
	public void launchEditor(String editorType) {
		openPageEditor();
	}

	private void openPageEditor() {
		PageComponent page = (PageComponent)getObject();
		synchronized (page) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorPart editorPart = getPageEditor(activePage, page);
				
				if (editorPart != null) {
					activePage.activate(editorPart);
				}
				else {
					try {
						editorPart = activePage.openEditor(new PageComponentEditorInput(page),
										"com.twinsoft.convertigo.eclipse.editors.mobile.PageComponentEditor");
					} catch (PartInitException e) {
						ConvertigoPlugin.logException(e,
								"Error while loading the page editor '"
										+ page.getName() + "'");
					}
				}
			}
		}
	}
	
	private IEditorPart getPageEditor(IWorkbenchPage activePage, PageComponent page) {
		IEditorPart editorPart = null;
		if (activePage != null) {
			if (page != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof PageComponentEditorInput)) {
							if (((PageComponentEditorInput)editorInput).is(page)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					} catch(PartInitException e) {
					}
				}
			}
		}
		return editorPart;
	}
	
	@Override
	public boolean rename(String newName, boolean bDialog) {
		PageComponent page = getObject();
		String oldName = page.getName();
		boolean renamed = super.rename(newName, bDialog);
		if (renamed) {
			try {
				page.getProject().getMobileBuilder().pageRenamed(page, oldName);
			} catch (EngineException e) {
				ConvertigoPlugin.logException(e,
						"Error while writting source files for page '" + page.getName() + "'");
			}
		}
		return renamed;
	}

	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
		if (bModified && !isInherited) {
			markTemplateAsDirty();
		}
	}
		
	protected void markTemplateAsDirty() {
		PageComponent page = getObject();
		try {
			page.doCompute();
			page.getProject().getMobileBuilder().pageComputed(page);
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the html template for page '" + page.getName() + "'");	}
	}
}
