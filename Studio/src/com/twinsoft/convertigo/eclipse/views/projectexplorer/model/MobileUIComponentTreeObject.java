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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UICustom;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.html.MobileComponentEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.StringComboBoxPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;

public class MobileUIComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject, IOrderableTreeObject {

	public MobileUIComponentTreeObject(Viewer viewer, UIComponent object) {
		super(viewer, object);
	}

	public MobileUIComponentTreeObject(Viewer viewer, UIComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public UIComponent getObject() {
		return (UIComponent) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}

	@Override
	public void launchEditor(String editorType) {
		MobileComponent mc = (MobileComponent)getObject();
		if (mc instanceof UICustom) {
			openFileEditor();
		}
	}

	private void openFileEditor() {
		final UICustom mc = (UICustom)getObject();
		String filePath = "/_private/" + mc.getQName() + " " + mc.getName()+".html";
		try {
			// Refresh project resource
			String projectName = mc.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Create temporary file if needed
			IFile file = project.getFile(filePath);
			if (!file.exists()) {
				try {
					InputStream is = new ByteArrayInputStream(mc.getCustomTemplate().getBytes("ISO-8859-1"));
					file.create(is, true, null);
					file.setCharset("ISO-8859-1", null);
				} catch (UnsupportedEncodingException e) {
				}
			}
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new MobileComponentEditorInput(file,mc);
				if (input != null) {
					//IPath path = file.getProjectRelativePath();
					//String fileName = path.removeFirstSegments(path.segmentCount() - 1).toString();
					//String editorId = getEditorId(fileName);
					String editorId = "org.eclipse.wst.html.core.htmlsource.source";
					
					IWorkbenchPage activePage = PlatformUI
							.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage();
	
					IEditorPart editorPart = activePage.openEditor(input, editorId);
					editorPart.addPropertyListener(new IPropertyListener() {
						boolean isFirstChange = false;
						
						@Override
						public void propertyChanged(Object source, int propId) {
							if (source instanceof ITextEditor) { //org.eclipse.wst.sse.ui.StructuredTextEditor
								if (propId == IEditorPart.PROP_DIRTY) {
									if (!isFirstChange) {
										isFirstChange = true;
										return;
									}
									
									isFirstChange = false;
									ITextEditor editor = (ITextEditor)source;
									IDocumentProvider dp = editor.getDocumentProvider();
									IDocument doc = dp.getDocument(editor.getEditorInput());
									String htmlTemplate = doc.get();
									MobileUIComponentTreeObject.this.setPropertyValue("htmlTemplate", htmlTemplate);
								}
							}
						}
					});
				}			
			}
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open file '" + filePath + "'!");
		}
	}
	
	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
		if (bModified && !isInherited) {
			markTemplateAsDirty();
		}
	}

	@Override
    protected List<PropertyDescriptor> getDynamicPropertyDescriptors() {
		List<PropertyDescriptor> l = super.getDynamicPropertyDescriptors();
		DatabaseObject dbo = getObject();
        if (dbo instanceof UIDynamicElement) {
        	IonBean ionBean = ((UIDynamicElement)dbo).getIonBean();
        	if (ionBean != null) {
	    		for (IonProperty property : ionBean.getProperties().values()) {
	    			String id = property.getName();
	    			String displayName = property.getLabel();
	    			Object[] values = property.getValues();
	    			int len = values.length;
	    			
					PropertyDescriptor propertyDescriptor = null;
					if (len == 0) {
						propertyDescriptor = new TextPropertyDescriptor(id, displayName);
					}
					else if (len == 1) {
						propertyDescriptor = new PropertyDescriptor(id, displayName);
					}
					else {
	        			boolean isEditable = values[len-1].equals(true);
	        			int size = isEditable ? len-1:len;
	        			String[] tags = new String[size];
	        			for (int i=0; i<size; i++) {
	        				Object value = values[i];
	        				tags[i] = value.equals(false) ? "not set":value.toString();
	        			}
	        			propertyDescriptor = new StringComboBoxPropertyDescriptor(id, displayName, tags, !isEditable);
	    	        }
	    	        propertyDescriptor.setCategory(property.getCategory());
	    	        propertyDescriptor.setDescription(property.getDescription());
	    			l.add(propertyDescriptor);
	    		}
        	}
        }
		return l;
    }
    
	@Override
	public Object getPropertyValue(Object id) {
		DatabaseObject dbo = getObject();
        if (dbo instanceof UIDynamicElement) {
        	IonBean ionBean = ((UIDynamicElement)dbo).getIonBean();
        	if (ionBean != null) {
	        	if (ionBean.hasProperty((String)id)) {
	        		return ionBean.getPropertyValue((String)id);
	        	}
        	}
        }
		return super.getPropertyValue(id);
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		DatabaseObject dbo = getObject();
        if (dbo instanceof UIDynamicElement) {
        	IonBean ionBean = ((UIDynamicElement)dbo).getIonBean();
        	if (ionBean != null) {
	        	if (ionBean.hasProperty((String)id)) {
	        		Object oldValue = ionBean.getPropertyValue((String)id);
	        		if (value != null && !value.equals(oldValue)) {
		        		ionBean.setPropertyValue((String)id, value);
		        		
		        		TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
		        		hasBeenModified(true);
		        		viewer.update(this, null);
		        		
		    	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(this, (String)id, oldValue, value);
		    	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
		        		return;
	        		}
	        	}
        	}
        }
		super.setPropertyValue(id, value);
	}

	protected void markTemplateAsDirty() {
		TreeParent treeParent = parent;
		while (treeParent != null) {
			if (treeParent instanceof MobileApplicationComponentTreeObject) {
				((MobileApplicationComponentTreeObject) treeParent).markAsDirty();
				break;
			}
			if (treeParent instanceof MobilePageComponentTreeObject) {
				((MobilePageComponentTreeObject) treeParent).markAsDirty();
				break;
			}
			treeParent = treeParent.getParent();
		}
	}
}
