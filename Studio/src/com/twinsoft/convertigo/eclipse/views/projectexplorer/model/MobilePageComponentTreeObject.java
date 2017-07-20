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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.mobile.ComponentFileEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class MobilePageComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject, IOrderableTreeObject {
	
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
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		return super.testAttribute(target, name, value);
	}

	@Override
    public boolean isEnabled() {
		setEnabled(getObject().isEnabled());
    	return super.isEnabled();
    }
	
	@Override
	public void launchEditor(String editorType) {
		ApplicationComponentEditor editor = ((MobileApplicationComponentTreeObject) getParentDatabaseObjectTreeObject()).activeEditor();
		editor.selectPage(getObject().getName());
	}
	
	public void editPageTsFile() {
		final PageComponent page = (PageComponent)getObject();
		try {
			// Refresh project resource
			String projectName = page.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Get filepath of page's temporary TypeScript file
			String filePath = page.getProject().getMobileBuilder().getTempTsRelativePath(page);
			IFile file = project.getFile(filePath);
			file.refreshLocal(IResource.DEPTH_ZERO, null);
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, page);
				if (input != null) {
					IEditorDescriptor desc = PlatformUI
							.getWorkbench()
							.getEditorRegistry()
							.getDefaultEditor(file.getName());
					
					IWorkbenchPage activePage = PlatformUI
							.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage();
	
					String editorId = desc.getId();
					
					IEditorPart editorPart = activePage.openEditor(input, editorId);
					editorPart.addPropertyListener(new IPropertyListener() {
						boolean isFirstChange = false;
						
						@Override
						public void propertyChanged(Object source, int propId) {
							if (source instanceof ITextEditor) {
								if (propId == IEditorPart.PROP_DIRTY) {
									if (!isFirstChange) {
										isFirstChange = true;
										return;
									}
									
									isFirstChange = false;
									ITextEditor editor = (ITextEditor)source;
									IDocumentProvider dp = editor.getDocumentProvider();
									IDocument doc = dp.getDocument(editor.getEditorInput());
									FormatedContent scriptContent = new FormatedContent(MobileBuilder.getMarkers(doc.get()));
									MobilePageComponentTreeObject.this.setPropertyValue("scriptContent", scriptContent);
								}
							}
						}
					});
				}			
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to open typescript file for page '" + page.getName() + "'!");
		}
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
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			try {
				if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent)dbo;
					if (getObject().equals(uic.getPage())) {
						markPageAsDirty();
					}
				}
				else if (this.equals(doto)) {
					if (propertyName.equals("scriptContent")) {
						if (!newValue.equals(oldValue)) {
							markPageTsAsDirty();
						}
					} else if (propertyName.equals("isEnabled")) {
						if (!newValue.equals(oldValue)) {
							markPageEnabledAsDirty();
						}
					} else if (propertyName.equals("segment")) {
						if (!newValue.equals(oldValue)) {
							markAppModuleTsAsDirty();
						}
					} else if (propertyName.equals("title") || propertyName.equals("inAutoMenu")) {
						if (!newValue.equals(oldValue)) {
							markAppComponentTsAsDirty();
						}
					} else {
						markPageAsDirty();
					}
				}
			} catch (Exception e) {}
		}
	}

	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
	}
	
	protected void markAppComponentTsAsDirty() {
		ApplicationComponent ac = (ApplicationComponent) getObject().getParent();
		try {
			ac.markComponentTsAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the app.component.ts for application '" + ac.getName() + "'");	}
	}
	
	protected void markAppModuleTsAsDirty() {
		ApplicationComponent ac = (ApplicationComponent) getObject().getParent();
		try {
			ac.markModuleTsAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the app.module.ts for application '" + ac.getName() + "'");	}
	}
	
	protected void markPageEnabledAsDirty() {
		PageComponent page = getObject();
		try {
			page.markPageEnabledAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while enabling/disabling page '" + page.getName() + "'");	}
	}
	
	protected void markPageAsDirty() {
		PageComponent page = getObject();
		try {
			page.markPageAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the source files for page '" + page.getName() + "'");	}
	}

	protected void markPageTsAsDirty() {
		PageComponent page = getObject();
		try {
			page.markPageTsAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the page.ts file for page '" + page.getName() + "'");	}
	}
}
