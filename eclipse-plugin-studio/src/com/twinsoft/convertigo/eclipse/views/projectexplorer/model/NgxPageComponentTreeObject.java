/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ICellEditorValidator;
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
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.ngx.ComponentFileEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.validators.NgxPageSegmentValidator;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class NgxPageComponentTreeObject extends NgxComponentTreeObject implements IEditableTreeObject, IOrderableTreeObject, INamedSourceSelectorTreeObject {

	public NgxPageComponentTreeObject(Viewer viewer, PageComponent object, boolean inherited) {
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
	protected ICellEditorValidator getValidator(String propertyName) {
		if ("segment".equals(propertyName))
			return new NgxPageSegmentValidator(getObject());
		return super.getValidator(propertyName);
	} 
	
	@Override
    public boolean isEnabled() {
		setEnabled(getObject().isEnabled());
    	return super.isEnabled();
    }
	
	@Override
	public void launchEditor(String editorType) {
		ApplicationComponentEditor editor = ((NgxApplicationComponentTreeObject) getParentDatabaseObjectTreeObject()).activeEditor();
		editor.selectPage(getObject().getSegment());
	}
	
	public void editPageTsFile() {
		final PageComponent page = (PageComponent)getObject();
		try {
			// Refresh project resource
			String projectName = page.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor
			String filePath = page.getProject().getMobileBuilder().getTempTsRelativePath(page);
			IFile file = project.getFile(filePath);
			closeComponentFileEditor(file);
			
			// Write temporary file
			page.getProject().getMobileBuilder().writePageTempTs(page);
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
									FormatedContent scriptContent = new FormatedContent(MobileBuilder.getMarkers(doc.get()));
									NgxPageComponentTreeObject.this.setPropertyValue("scriptContent", scriptContent);
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
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
	}
	
	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {

			@Override
			Object thisTreeObject() {
				return NgxPageComponentTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (getObject() instanceof PageComponent) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						NgxUIComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("startMenu");
						list.add("endMenu");
					}
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof PageComponent) {
					return "startMenu".equals(propertyName) || "endMenu".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof PageComponent) {
					if ("startMenu".equals(propertyName) || "endMenu".equals(propertyName)) {
						PageComponent pc = getObject();
						if (nsObject instanceof UIDynamicMenu) {
							return (((UIDynamicMenu)nsObject).getProject().equals(pc.getProject()));
						}
					}
				}
				return false;
			}
			
			@Override
			protected void handleSourceCleared(String propertyName) {
				// nothing to do
			}
			
			@Override
			protected void handleSourceRenamed(String propertyName, String oldName, String newName) {
				if (isNamedSource(propertyName)) {
					boolean hasBeenRenamed = false;
					
					String pValue = (String) getPropertyValue(propertyName);
					if (pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if (getObject() instanceof PageComponent) {
								if ("startMenu".equals(propertyName)) {
									getObject().setStartMenu(_pValue);
									hasBeenRenamed = true;
								}
								if ("endMenu".equals(propertyName)) {
									getObject().setEndMenu(_pValue);
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						
						ConvertigoPlugin.projectManager.getProjectExplorerView().updateTreeObject(NgxPageComponentTreeObject.this);
						getDescriptors();// refresh editors (e.g labels in combobox)
						
		    	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(NgxPageComponentTreeObject.this, propertyName, "", "");
		    	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
					}
				}
			}
		};
	}
}
