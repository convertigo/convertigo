/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.editors.mobile.ComponentFileEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class MobileApplicationComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject {
	public static final String P_TPL_VERSION = "#tplVersion";
	
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
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			
			try {
				// we add a page to this app
				if (dbo instanceof PageComponent && getObject().equals(dbo.getParent())) {
					PageComponent page = (PageComponent)dbo;
					if (page.bNew) {
						page.markPageAsDirty();
					}
				}
			} catch (Exception e) {}
		}
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
				// for any UI component inside a menu or a stack
				else if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent)dbo;
					
					UIDynamicMenu menu = uic.getMenu();
					if (menu != null) {
						if (getObject().equals(menu.getParent())) {
							if (propertyName.equals("FormControlName") || uic.isFormControlAttribute()) {
								if (!newValue.equals(oldValue)) {
									try {
										String oldSmart = ((MobileSmartSourceType)oldValue).getSmartValue();
										String newSmart = ((MobileSmartSourceType)newValue).getSmartValue();
										if (uic.getUIForm() != null) {
											String form = uic.getUIForm().getFormGroupName();
											if (menu.updateSmartSource(form+"\\?\\.controls\\['"+oldSmart+"'\\]", form+"?.controls['"+newSmart+"']")) {
												this.viewer.refresh();
											}
										}
									} catch (Exception e) {}
								}
							}
							
							markApplicationAsDirty();
						}
					}
				}
				// for this application
				else if (this.equals(doto)) {
					if (propertyName.equals("isPWA")) {
						if (!newValue.equals(oldValue)) {
							markPwaAsDirty();
						}
					} else if (propertyName.equals("componentScriptContent")) {
						if (!newValue.equals(oldValue)) {
							markComponentTsAsDirty();
							markApplicationAsDirty();
						}
					} else if (propertyName.equals("useClickForTap")) {
						for (TreeObject to: this.getChildren()) {
							if (to instanceof ObjectsFolderTreeObject) {
								ObjectsFolderTreeObject ofto = (ObjectsFolderTreeObject)to;
								if (ofto.folderType == ObjectsFolderTreeObject.FOLDER_TYPE_PAGES) {
									for (TreeObject cto: ofto.getChildren()) {
										if (cto instanceof MobilePageComponentTreeObject) {
											((MobilePageComponentTreeObject)cto).markPageAsDirty();
										}
									}
								}
							}
						}
						markApplicationAsDirty();
					} else if (propertyName.equals("tplProjectName")) {
						// close app editor and reinitialize builder
						Project project = getObject().getProject();
						closeAllEditors(false);
						MobileBuilder.releaseBuilder(project);
						MobileBuilder.initBuilder(project);
						
						IProject iproject = ConvertigoPlugin.getDefault().getProjectPluginResource(project.getName());
						iproject.refreshLocal(IResource.DEPTH_INFINITE, null);
						
						// force app sources regeneration
						for (TreeObject to: this.getChildren()) {
							if (to instanceof ObjectsFolderTreeObject) {
								ObjectsFolderTreeObject ofto = (ObjectsFolderTreeObject)to;
								if (ofto.folderType == ObjectsFolderTreeObject.FOLDER_TYPE_PAGES) {
									for (TreeObject cto: ofto.getChildren()) {
										if (cto instanceof MobilePageComponentTreeObject) {
											((MobilePageComponentTreeObject)cto).markPageAsDirty();
										}
									}
								}
							}
						}
						markApplicationAsDirty();
						
						// delete node modules and alert user
						final File nodeModules = new File(project.getDirPath(), "/_private/ionic/node_modules");
						if (nodeModules.exists()) {
							ProgressMonitorDialog dialog = new ProgressMonitorDialog(ConvertigoPlugin.getMainShell());
							dialog.open();
							dialog.run(true, false, new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									monitor.beginTask("deleting node modules", IProgressMonitor.UNKNOWN);
									String alert = "template changed!";
									if (com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(nodeModules)) {
										alert = "You have just changed the template.\nPackages have been deleted and will be reinstalled next time you run your application again.";
									} else {
										alert = "You have just changed the template: packages could not be deleted!\nDo not forget to reinstall the packages before running your application again, otherwise it may be corrupted!";
									}
									monitor.done();
									ConvertigoPlugin.infoMessageBox(alert);
								}
							});
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
	
	protected void markPwaAsDirty() {
		ApplicationComponent ac = getObject();
		try {
			ac.markPwaAsDirty();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e,
					"Error while writing the application PWA state for '" + ac.getName() + "'");	}
	}

	public void editAppComponentTsFile() {
		final ApplicationComponent application = getObject();
		try {
			// Refresh project resource
			String projectName = application.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor
			String filePath = application.getProject().getMobileBuilder().getTempTsRelativePath(application);
			IFile file = project.getFile(filePath);
			closeComponentFileEditor(file);
			
			// Write temporary file
			application.getProject().getMobileBuilder().writeAppComponentTempTs(application);
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
					addMarkers(file, editorPart);
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
	public void closeAllEditors(boolean save) {
		super.closeAllEditors(save);// will close any child component editor
		
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
		return activeEditor(true);
	}
	
	public ApplicationComponentEditor activeEditor(boolean autoLaunch) {
		ApplicationComponentEditor editorPart = null;
		ApplicationComponent application = (ApplicationComponent) getObject();
		
		synchronized (application) {
			String tpl = application.getTplProjectName();
			try {
				if (StringUtils.isBlank(tpl) || Engine.theApp.databaseObjectsManager.getOriginalProjectByName(tpl, false) == null) {
					throw new InvalidParameterException("The value '" + tpl + "' of the property 'Template project' from '" + application.getQName() + "' is incorrect.");
				}

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
								}
							}
						} catch(PartInitException e) {

						}
					}

					if (editorPart != null) {
						activePage.activate(editorPart);
					} else {
						IEditorPart editor = activePage.openEditor(new ApplicationComponentEditorInput(application, autoLaunch),
								"com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor");
						if (editor instanceof ApplicationComponentEditor) {
							editorPart = (ApplicationComponentEditor) editor;
						} else {
							ConvertigoPlugin.logWarning("The Application Component Editor won't open, please see the error log.");
						}
					}
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e,
						"Error while loading the page editor '"
								+ application.getName() + "'");
			}
		}
		return editorPart;
	}
	
	@Override
	protected List<PropertyDescriptor> getDynamicPropertyDescriptors() {
		List<PropertyDescriptor> l = super.getDynamicPropertyDescriptors();
		PropertyDescriptor pd = new PropertyDescriptor(P_TPL_VERSION, "Template version");
		pd.setDescription("The project's ionicTpl version.");
		pd.setCategory("Information");
		l.add(pd);
		return l;
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (P_TPL_VERSION.equals(id)) {
			return getObject().getTplProjectVersion();
		}
		return super.getPropertyValue(id);
	}	
	
}
