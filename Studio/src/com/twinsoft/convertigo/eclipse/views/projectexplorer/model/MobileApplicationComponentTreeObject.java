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
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.editors.mobile.ComponentFileEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class MobileApplicationComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject {
	
	public MobileApplicationComponentTreeObject(Viewer viewer, ApplicationComponent object) {
		super(viewer, object);
	}

	public MobileApplicationComponentTreeObject(Viewer viewer, ApplicationComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
	}

	@Override
	public ApplicationComponent getObject() {
		return (ApplicationComponent) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
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
				// for Page or Menu or Route
				if (getObject().equals(dbo.getParent())) {
					markApplicationAsDirty();
				}
				// for any component inside a route
				else if (getObject().equals(dbo.getParent().getParent())) {
					markApplicationAsDirty();
				}
				// for any UI component inside a menu
				else if (dbo instanceof UIComponent) {
					UIDynamicMenu menu = ((UIComponent)dbo).getMenu();
					if (menu != null && getObject().equals(menu.getParent())) {
						markApplicationAsDirty();
					}
				}
				// for this application
				else if (this.equals(doto)) {
					if (propertyName.equals("componentScriptContent")) {
						if (!newValue.equals(oldValue)) {
							markComponentTsAsDirty();
						}
					} else {
						markApplicationAsDirty();
					}
				}
			} catch (Exception e) {}
		}
	}
	
	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
	}
	
	protected void markComponentTsAsDirty() {
		ApplicationComponent ac = getObject();
		try {
			ac.markComponentTsAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the app.component.ts for application '" + ac.getName() + "'");	}
	}
	
	protected void markApplicationAsDirty() {
		ApplicationComponent ac = getObject();
		try {
			ac.markApplicationAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the application source files for '" + ac.getName() + "'");	}
	}
	
	public void editAppComponentTsFile() {
		final ApplicationComponent application = getObject();
		try {
			// Refresh project resource
			String projectName = application.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Get filepath of application.component.ts file
			String filePath = application.getProject().getMobileBuilder().getTempTsRelativePath(application);
			IFile file = project.getFile(filePath);
			file.refreshLocal(IResource.DEPTH_ZERO, null);
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, application);
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
									FormatedContent componentScriptContent = new FormatedContent(MobileBuilder.getMarkers(doc.get()));
									MobileApplicationComponentTreeObject.this.setPropertyValue("componentScriptContent", componentScriptContent);
								}
							}
						}
					});
				}			
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to open typescript file for page '" + application.getName() + "'!");
		}
	}
	
	@Override
	public void launchEditor(String editorType) {
		activeEditor();
	}
	
	@Override
	public void closeAllEditors() {
		super.closeAllEditors();// will close any child component editor
		
		ApplicationComponent application = (ApplicationComponent) getObject();
		synchronized (application) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if (editorInput != null && editorInput instanceof ApplicationComponentEditorInput) {
							if (((ApplicationComponentEditorInput) editorInput).is(application)) {
								activePage.closeEditor(editorRef.getEditor(false),false);
							}
						}
					} catch(Exception e) {
						
					}
				}
			}
		}
	}

	public ApplicationComponentEditor activeEditor() {
		ApplicationComponentEditor editorPart = null;
		ApplicationComponent application = (ApplicationComponent) getObject();
		
		synchronized (application) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ApplicationComponentEditorInput)) {
							if (((ApplicationComponentEditorInput) editorInput).is(application)) {
								editorPart = (ApplicationComponentEditor) editorRef.getEditor(false);
								break;
							}
						}
					} catch(PartInitException e) {
					}
				}
				
				if (editorPart != null) {
					activePage.activate(editorPart);
				} else {
					try {
						IEditorPart editor = activePage.openEditor(new ApplicationComponentEditorInput(application),
								"com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor");
						if (editor instanceof ApplicationComponentEditor) {
							editorPart = (ApplicationComponentEditor) editor;
						} else {
							ConvertigoPlugin.logWarning("The Application Component Editor won't open, please see the error log.");
						}
					} catch (PartInitException e) {
						ConvertigoPlugin.logException(e,
								"Error while loading the page editor '"
										+ application.getName() + "'");
					}
				}
			}
		}
		
		return editorPart;
	}
}
