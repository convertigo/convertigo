/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.IScriptComponent;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UICustom;
import com.twinsoft.convertigo.beans.mobile.components.UICustomAction;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicAnimate;
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
import com.twinsoft.convertigo.eclipse.property_editors.StringComboBoxPropertyDescriptor;
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
		} else if (uic instanceof UICustomAction) {
			String functionMarker = "function:"+ ((UICustomAction)uic).getActionName();
			editFunction(uic, functionMarker, "actionValue");
		} else if (uic instanceof UIFormCustomValidator) {
			String functionMarker = "function:"+ ((UIFormCustomValidator)uic).getValidatorName();
			editFunction(uic, functionMarker , "validatorValue");
		} else {
			super.launchEditor(editorType);
		}
	}
	
	private void editFunction(final UIComponent uic, final String functionMarker, final String propertyName) {
		try {
			IScriptComponent main = uic.getMainScriptComponent();
			
			// Refresh project resources for editor
			String projectName = uic.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor and Reopen it after file has been rewritten
			String relativePath = uic.getProject().getMobileBuilder().getFunctionTempTsRelativePath(main);
			IFile file = project.getFile(relativePath);
			closeComponentFileEditor(file);
			
			if (main instanceof ApplicationComponent) {
				if (uic.compareToTplVersion("7.5.2.0") < 0) {
					ConvertigoPlugin.logError("The ability to use forms or actions inside a menu is avalaible since 7.5.2 version."
							+ "\nPlease change your Template project for the 'mobilebuilder_tpl_7_5_2' template.", true);
					return;
				}
			}
			uic.getProject().getMobileBuilder().writeFunctionTempTsFile(main, functionMarker);
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
			ConvertigoPlugin.logException(e, "Unable to edit function for '"+ uic.getName() +"' component!");
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
				unformated = unformated.replaceFirst("^\\.class\\d+\\s?\\{\\r?\\n?", "");
				unformated = unformated.substring(0, unformated.lastIndexOf("}"));
			} catch (Exception e) {
				unformated = s;
				e.printStackTrace();
			}
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
	    			
	    			if (property.isHidden()) {
	    				continue;
	    			}
	    			
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
						if (editor.equals("StringComboBoxPropertyDescriptor")) {
							try {
								Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.property_editors." + editor);
								Method getTags = c.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
								String[] tags = (String[]) getTags.invoke(null, new Object[] { this, id } );
								propertyDescriptor = new StringComboBoxPropertyDescriptor(id, displayName, tags, true);
							} catch (Exception e) {
								e.printStackTrace();
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
					}
	    	        propertyDescriptor.setCategory(property.getCategory());
	    	        propertyDescriptor.setDescription(cleanDescription(property.getDescription()));
	    	        propertyDescriptor.setValidator(getValidator(id));
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
	        		if (value != null) {
	        			if (value instanceof String) {
	        				value = new MobileSmartSourceType((String) value);
	        			}
		        		Object oldValue = ionBean.getPropertyValue((String)id);	        			
	        			if (!value.equals(oldValue)) {
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
				
				if (getObject() instanceof UIDynamicTab) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						MobilePageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("tabpage");
					}
				}
				else if (getObject() instanceof UIDynamicMenuItem) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						MobilePageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("itempage");
					}
				}
				else if (getObject() instanceof UIDynamicAnimate) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						MobilePageComponentTreeObject.class.isAssignableFrom(c) ||
						MobileUIComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("identifiable");
					}
				}
				else if (getObject() instanceof UIDynamicElement) {
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
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof UIDynamicTab) {
					return "tabpage".equals(propertyName);
				}
				else if (getObject() instanceof UIDynamicMenuItem) {
					return "itempage".equals(propertyName);
				}
				else if (getObject() instanceof UIDynamicAnimate) {
					return "identifiable".equals(propertyName);
				}
				else if (getObject() instanceof UIDynamicElement) {
					return "requestable".equals(propertyName) || 
								"fsview".equals(propertyName) ||
									"page".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof UIDynamicTab) {
					if ("tabpage".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(getObject().getProject()));
						}
					}
				}
				else if (getObject() instanceof UIDynamicMenuItem) {
					if ("itempage".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(getObject().getProject()));
						}
					}
				}
				else if (getObject() instanceof UIDynamicAnimate) {
					if ("identifiable".equals(propertyName)) {
						UIDynamicAnimate uda = (UIDynamicAnimate) getObject();
						if (nsObject instanceof UIElement) {
							UIElement ue = (UIElement)nsObject;
							if (uda.getMainScriptComponent().equals(ue.getMainScriptComponent())) {
								return !ue.getIdentifier().isEmpty();
							}
						}
					}
				}
				else if (getObject() instanceof UIDynamicElement) {
					if ("requestable".equals(propertyName)) {
						UIDynamicElement cc = (UIDynamicElement) getObject();
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
						if (cc.getIonBean().getName().equals("FullSyncGetAction")) {
							return nsObject instanceof FullSyncConnector;
						}
						if (cc.getIonBean().getName().equals("FullSyncDeleteAction")) {
							return nsObject instanceof FullSyncConnector;
						}
						if (cc.getIonBean().getName().equals("FullSyncPutAttachmentAction")) {
							return nsObject instanceof FullSyncConnector;
						}
						if (cc.getIonBean().getName().equals("FullSyncDeleteAttachmentAction")) {
							return nsObject instanceof FullSyncConnector;
						}
						if (cc.getIonBean().getName().equals("FSImage")) {
							return nsObject instanceof FullSyncConnector;
						}
					}
					if ("fsview".equals(propertyName)) {
						UIDynamicElement cc = (UIDynamicElement) getObject();
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
							if (getObject() instanceof UIDynamicTab) {
								if ("tabpage".equals(propertyName)) {
									((UIDynamicTab)getObject()).setTabPage(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (getObject() instanceof UIDynamicMenuItem) {
								if ("itempage".equals(propertyName)) {
									((UIDynamicMenuItem)getObject()).setItemPage(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (getObject() instanceof UIDynamicAnimate) {
								if ("identifiable".equals(propertyName)) {
									((UIDynamicAnimate)getObject()).setIdentifiable(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (getObject() instanceof UIDynamicElement) {
								if ("requestable".equals(propertyName)) {
									((UIDynamicElement)getObject()).getIonBean().
										setPropertyValue("requestable", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("fsview".equals(propertyName)) {
									((UIDynamicElement)getObject()).getIonBean().
										setPropertyValue("fsview", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("page".equals(propertyName)) {
									((UIDynamicElement)getObject()).getIonBean().
										setPropertyValue("page", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						
						ConvertigoPlugin.projectManager.getProjectExplorerView().updateTreeObject(MobileUIComponentTreeObject.this);
						getDescriptors();// refresh editors (e.g labels in combobox)
						
		    	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(MobileUIComponentTreeObject.this, propertyName, "", "");
		    	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
					}
				}
			}
		};
	}
}
