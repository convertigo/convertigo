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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallAction;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallFullSync;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallSequence;
import com.twinsoft.convertigo.beans.mobile.components.UIControlListenFullSyncSource;
import com.twinsoft.convertigo.beans.mobile.components.UIControlListenSequenceSource;
import com.twinsoft.convertigo.beans.mobile.components.UIControlListenSource;
import com.twinsoft.convertigo.beans.mobile.components.UICustom;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.mobile.components.UIElement;
import com.twinsoft.convertigo.beans.mobile.components.UIStyle;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.html.MobileComponentEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.StringComboBoxPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public class MobileUIComponentTreeObject extends MobileComponentTreeObject implements IEditableTreeObject, IOrderableTreeObject, INamedSourceSelectorTreeObject {
	
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
		MobileComponent mc = (MobileComponent) getObject();
		if (mc instanceof UICustom) {
			openHtmlFileEditor();
		} else if (mc instanceof UIStyle) {
			openCssFileEditor();
		} else {
			super.launchEditor(editorType);
		}
	}

	private void openHtmlFileEditor() {
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
	
	private String formatStyleContent(UIStyle ms) {
		String formated = ms.getStyleContent();
		DatabaseObject parentDbo = ms.getParent();
		if (parentDbo != null) {
			if (parentDbo instanceof UIElement) {
				String dboTagClass = ((UIElement)parentDbo).getTagClass();
				formated = String.format("."+ dboTagClass +" {%n%s%n}", formated);
			}
		}
		return formated;
	}
	
	private String unformatStyleContent(UIStyle ms, String s) {
		String unformated = s;
		DatabaseObject parentDbo = ms.getParent();
		if (parentDbo != null) {
			try {
				if (parentDbo instanceof UIElement) {
					String dboTagClass = ((UIElement)parentDbo).getTagClass();
					Pattern p = Pattern.compile("\\."+ dboTagClass +" \\{\\r\\n([^\\{\\}]*)\\r\\n\\}");
					Matcher m = p.matcher(s);
					if (m.matches()) {
						unformated = m.group(1);
					}
				}
			} catch (Exception e) {}
		};
		return unformated;
	}
	
	private void openCssFileEditor() {
		final UIStyle ms = (UIStyle)getObject();
		String filePath = "/_private/" + ms.getQName() + " " + ms.getName()+".css";
		try {
			// Refresh project resource
			String projectName = ms.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Create temporary file if needed
			IFile file = project.getFile(filePath);
			if (!file.exists()) {
				try {
					String content = formatStyleContent(ms);
					InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
					file.create(is, true, null);
					file.setCharset("UTF-8", null);
				} catch (UnsupportedEncodingException e) {}
			}
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new MobileComponentEditorInput(file,ms);
				if (input != null) {
					String editorId = "org.eclipse.wst.css.core.csssource.source";
					
					IWorkbenchPage activePage = PlatformUI
							.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage();
	
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
									String content = unformatStyleContent(ms, doc.get());
									MobileUIComponentTreeObject.this.setPropertyValue("styleContent", content);
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
	        	if (((String)id).equals(P_TYPE)) {
	        		return ionBean.getName();
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

	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {

			@Override
			Object thisTreeObject() {
				return MobileUIComponentTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (getObject() instanceof UIControlCallAction) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c))
					{
						list.add("target");
					}
				}
				if (getObject() instanceof UIControlListenSource) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c))
					{
						list.add("target");
					}
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof UIControlCallAction) {
					return "target".equals(propertyName);
				}
				if (getObject() instanceof UIControlListenSource) {
					return "target".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof UIControlCallAction) {
					if ("target".equals(propertyName)) {
						UIControlCallAction cc = (UIControlCallAction) getObject();
						if (cc instanceof UIControlCallSequence) {
							return nsObject instanceof Sequence;
						}
						if (cc instanceof UIControlCallFullSync) {
							return nsObject instanceof FullSyncConnector;
						}
					}
				}
				if (getObject() instanceof UIControlListenSource) {
					if ("target".equals(propertyName)) {
						UIControlListenSource cc = (UIControlListenSource) getObject();
						if (cc instanceof UIControlListenSequenceSource) {
							return nsObject instanceof Sequence;
						}
						if (cc instanceof UIControlListenFullSyncSource) {
							return nsObject instanceof FullSyncConnector;
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
							if (getObject() instanceof UIControlCallAction) {
								if ("target".equals(propertyName)) {
									((UIControlCallAction)getObject()).setTarget(_pValue);
									hasBeenRenamed = true;
								}
							}
							if (getObject() instanceof UIControlListenSource) {
								if ("target".equals(propertyName)) {
									((UIControlListenSource)getObject()).setTarget(_pValue);
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						viewer.refresh();
						
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
			}
		};
	}
}
