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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.couchdb.FullSyncListener;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallAction;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallFullSync;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallSequence;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCustomAction;
import com.twinsoft.convertigo.beans.mobile.components.UIControlListenFullSyncSource;
import com.twinsoft.convertigo.beans.mobile.components.UIControlListenSequenceSource;
import com.twinsoft.convertigo.beans.mobile.components.UIControlListenSource;
import com.twinsoft.convertigo.beans.mobile.components.UICustom;
import com.twinsoft.convertigo.beans.mobile.components.UICustomAction;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenuItem;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicTab;
import com.twinsoft.convertigo.beans.mobile.components.UIElement;
import com.twinsoft.convertigo.beans.mobile.components.UIFormCustomValidator;
import com.twinsoft.convertigo.beans.mobile.components.UIStyle;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.mobile.ComponentFileEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.AbstractDialogCellEditor;
import com.twinsoft.convertigo.eclipse.property_editors.MobileSmartSourcePropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

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
		UIComponent uic = getObject();
		if (uic instanceof UICustom) {
			openHtmlFileEditor();
		} else if (uic instanceof UIStyle) {
			openCssFileEditor();
		} else if (uic instanceof UIControlCustomAction) {
			String functionMarker = "function:"+ ((UIControlCustomAction)uic).getActionName();
			editPageFunction(uic, functionMarker, "actionValue");
		} else if (uic instanceof UICustomAction) {
			String functionMarker = "function:"+ ((UICustomAction)uic).getActionName();
			editPageFunction(uic, functionMarker, "actionValue");
		} else if (uic instanceof UIFormCustomValidator) {
			String functionMarker = "function:"+ ((UIFormCustomValidator)uic).getValidatorName();
			editPageFunction(uic, functionMarker , "validatorValue");
		} else {
			super.launchEditor(editorType);
		}
	}

	private void closeComponentFileEditor(final IFile file) {
		try {
			IWorkbenchPage activePage = PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage();
			
			for (IEditorReference editorReference : activePage.getEditorReferences()) {
				IEditorInput editorInput = editorReference.getEditorInput();
				if (editorInput instanceof ComponentFileEditorInput) {
					ComponentFileEditorInput cfei = (ComponentFileEditorInput) editorInput;
					if (cfei.getFile().equals(file)) {
						activePage.closeEditor(editorReference.getEditor(false), true);
						return;
					}
				}
			}
		} catch (Exception e) {
			
		}
	}
	
	private void editPageFunction(final UIComponent uic, final String functionMarker, final String propertyName) {
		final PageComponent page = uic.getPage();
		try {
			// Refresh project resources for editor
			String projectName = page.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor and Reopen it after file has been rewritten
			String relativePath = page.getProject().getMobileBuilder().getFunctionTempTsRelativePath(page);
			IFile file = project.getFile(relativePath);
			closeComponentFileEditor(file);
			page.getProject().getMobileBuilder().writeFunctionTempTsFile(page, functionMarker);
			file.refreshLocal(IResource.DEPTH_ZERO, null);
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, uic);
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
									String marker = MobileBuilder.getMarker(doc.get(), functionMarker);
									String beginMarker = "/*Begin_c8o_" + functionMarker + "*/";
									String endMarker = "/*End_c8o_" + functionMarker + "*/";
									String content = marker.replace(beginMarker+ System.lineSeparator(), "")
																.replace("\t\t"+endMarker, "") // for validator
																	.replace("\t"+endMarker, ""); // for action
									FormatedContent formated = new FormatedContent(content);
									MobileUIComponentTreeObject.this.setPropertyValue(propertyName, formated);
								}
							}
						}
					});
				}			
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to edit function for page '" + page.getName() + "'!");
		}
	}
	
	private void openHtmlFileEditor() {
		final UICustom mc = (UICustom)getObject();
		String filePath = "/_private/" + mc.priority+".html";
		try {
			// Refresh project resource
			String projectName = mc.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			IFile file = project.getFile(filePath);
			
			// Close editor
			closeComponentFileEditor(file);
			
			// Write html file
			try {
				InputStream is = new ByteArrayInputStream(mc.getCustomTemplate().getBytes("ISO-8859-1"));
				file.create(is, true, null);
				file.setCharset("ISO-8859-1", null);
			} catch (UnsupportedEncodingException e) {}
			file.refreshLocal(IResource.DEPTH_ZERO, null);
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, mc);
				if (input != null) {
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
		String formated = ms.getStyleContent().getString();
		DatabaseObject parentDbo = ms.getParent();
		if (parentDbo != null && parentDbo instanceof UIElement) {
			String dboTagClass = ((UIElement)parentDbo).getTagClass();
			if (formated.isEmpty()) {
				formated = String.format("."+ dboTagClass +" {%n%s%n}", formated);
			} else {
				formated = String.format("."+ dboTagClass +" {%n%s}", formated);
			}
		}
		return formated;
	}
	
	private String unformatStyleContent(UIStyle ms, String s) {
		String unformated = s;
		DatabaseObject parentDbo = ms.getParent();
		if (parentDbo != null && parentDbo instanceof UIElement) {
			try {
				unformated = null;
				Pattern p = Pattern.compile("\\.class\\d+\\s?\\{\\r?\\n?([^\\{\\}]*)\\r?\\n?\\}");
				Matcher m = p.matcher(s);
				if (m.matches()) {
					unformated = m.group(1);
				}
			} catch (Exception e) {}
		};
		return unformated;
	}
	
	private void openCssFileEditor() {
		final UIStyle ms = (UIStyle)getObject();
		String filePath = "/_private/" + ms.priority+".css";
		try {
			// Refresh project resource
			String projectName = ms.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			IFile file = project.getFile(filePath);
			
			// Close editor
			closeComponentFileEditor(file);
			
			// Write css file
			try {
				String content = formatStyleContent(ms);
				InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
				file.create(is, true, null);
				file.setCharset("UTF-8", null);
			} catch (UnsupportedEncodingException e) {}
			file.refreshLocal(IResource.DEPTH_ZERO, null);
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, ms);
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
									if (content != null) {
										FormatedContent formatedContent = new FormatedContent(content);
										MobileUIComponentTreeObject.this.setPropertyValue("styleContent", formatedContent);
									}
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
	    			String editor = property.getEditor();
	    			Object[] values = property.getValues();
	    			int len = values.length;
	    			
					PropertyDescriptor propertyDescriptor = null;
					if (editor.isEmpty()) {
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
		        			//propertyDescriptor = new StringComboBoxPropertyDescriptor(id, displayName, tags, !isEditable);
		        			propertyDescriptor = new MobileSmartSourcePropertyDescriptor(id, displayName, tags, !isEditable);
		        			((MobileSmartSourcePropertyDescriptor)propertyDescriptor).databaseObjectTreeObject = this;
		    	        }
					} else {
						propertyDescriptor = new PropertyDescriptor(id, displayName) {
							@Override
							public CellEditor createPropertyEditor(Composite parent) {
								CellEditor cellEditor = null;
								try {
									Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.property_editors." + editor);
									cellEditor = (CellEditor) c.getConstructor(Composite.class).newInstance(parent);
									if (cellEditor instanceof AbstractDialogCellEditor) {
										((AbstractDialogCellEditor)cellEditor).databaseObjectTreeObject = MobileUIComponentTreeObject.this;
										((AbstractDialogCellEditor)cellEditor).propertyDescriptor = this;
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								return cellEditor;
							}
							
						};
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
		if (isSymbolValue(value)) {
			ConvertigoPlugin.logError("Symbols are not allowed for mobile components", true);
			return;
		}
		
		DatabaseObject dbo = getObject();
        if (dbo instanceof UIDynamicElement) {
        	IonBean ionBean = ((UIDynamicElement)dbo).getIonBean();
        	if (ionBean != null) {
	        	if (ionBean.hasProperty((String)id)) {
	        		Object oldValue = ionBean.getPropertyValue((String)id);
	        		if (value != null && !value.equals(oldValue)) {
	        			if (value instanceof String) {
	        				value = new MobileSmartSourceType((String) value);
	        			}
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
				if (getObject() instanceof UIDynamicAction) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c))
					{
						list.add("requestable");
					}
					
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
							MobileApplicationTreeObject.class.isAssignableFrom(c) ||
							MobileApplicationComponentTreeObject.class.isAssignableFrom(c) ||
							MobilePageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("page");
					}
					
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c) ||
						DesignDocumentTreeObject.class.isAssignableFrom(c) ||
						DesignDocumentViewTreeObject.class.isAssignableFrom(c))
					{
						list.add("fsview");
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
				if (getObject() instanceof UIDynamicTab) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						MobilePageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("tabpage");
					}
				}
				if (getObject() instanceof UIDynamicMenuItem) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						MobilePageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("itempage");
					}
				}
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof UIControlCallAction) {
					return "target".equals(propertyName);
				}
				if (getObject() instanceof UIDynamicAction) {
					return "requestable".equals(propertyName) || 
								"fsview".equals(propertyName) ||
									"page".equals(propertyName);
				}
				if (getObject() instanceof UIControlListenSource) {
					return "target".equals(propertyName);
				}
				if (getObject() instanceof UIDynamicTab) {
					return "tabpage".equals(propertyName);
				}
				if (getObject() instanceof UIDynamicMenuItem) {
					return "itempage".equals(propertyName);
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
				if (getObject() instanceof UIDynamicAction) {
					if ("requestable".equals(propertyName)) {
						UIDynamicAction cc = (UIDynamicAction) getObject();
						if (cc.getIonBean().getName().equals("CallSequenceAction")) {
							return nsObject instanceof Sequence;
						}
						if (cc.getIonBean().getName().equals("CallFullSyncAction")) {
							return nsObject instanceof FullSyncConnector;
						}
						if (cc.getIonBean().getName().equals("FullSyncSyncAction")) {
							return nsObject instanceof FullSyncConnector;
						}
						if (cc.getIonBean().getName().equals("FullSyncViewAction")) {
							return nsObject instanceof DesignDocument;
						}
						if (cc.getIonBean().getName().equals("FullSyncPostAction")) {
							return nsObject instanceof FullSyncConnector;
						}
					}
					if ("fsview".equals(propertyName)) {
						UIDynamicAction cc = (UIDynamicAction) getObject();
						if (cc.getIonBean().getName().equals("FullSyncViewAction")) {
							return nsObject instanceof String;
						}
					}
					if ("page".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(getObject().getProject()));
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
				if (getObject() instanceof UIDynamicTab) {
					if ("tabpage".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(getObject().getProject()));
						}
					}
				}
				if (getObject() instanceof UIDynamicMenuItem) {
					if ("itempage".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(getObject().getProject()));
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
					
					Object oValue = getPropertyValue(propertyName);
					
					String pValue;
					if (oValue instanceof MobileSmartSourceType)
						pValue = ((MobileSmartSourceType)oValue).getSmartValue();
					else
						pValue = (String) oValue;
					
					if (pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if (getObject() instanceof UIControlCallAction) {
								if ("target".equals(propertyName)) {
									((UIControlCallAction)getObject()).setTarget(_pValue);
									hasBeenRenamed = true;
								}
							}
							if (getObject() instanceof UIDynamicAction) {
								if ("requestable".equals(propertyName)) {
									((UIDynamicAction)getObject()).getIonBean().
										setPropertyValue("requestable", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("fsview".equals(propertyName)) {
									((UIDynamicAction)getObject()).getIonBean().
										setPropertyValue("fsview", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("page".equals(propertyName)) {
									((UIDynamicAction)getObject()).getIonBean().
										setPropertyValue("page", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
							}
							if (getObject() instanceof UIControlListenSource) {
								if ("target".equals(propertyName)) {
									((UIControlListenSource)getObject()).setTarget(_pValue);
									hasBeenRenamed = true;
								}
							}
							if (getObject() instanceof UIDynamicTab) {
								if ("tabpage".equals(propertyName)) {
									((UIDynamicTab)getObject()).setTabPage(_pValue);
									hasBeenRenamed = true;
								}
							}
							if (getObject() instanceof UIDynamicMenuItem) {
								if ("itempage".equals(propertyName)) {
									((UIDynamicMenuItem)getObject()).setItemPage(_pValue);
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						viewer.refresh();
						getDescriptors();// refresh editors (e.g labels in combobox)
						
		    	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(MobileUIComponentTreeObject.this, propertyName, "", "");
		    	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
					}
				}
			}
		};
	}
}
