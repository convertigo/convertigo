/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import com.twinsoft.convertigo.beans.core.ISharedComponent;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IScriptComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIAppGuard;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlVariable;
import com.twinsoft.convertigo.beans.ngx.components.UICustom;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAnimate;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInfiniteScroll;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenuItem;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicTab;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicTabButton;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIStackVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIAppGuard.AppGuardType;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.ngx.ComponentFileEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.AbstractDialogCellEditor;
import com.twinsoft.convertigo.eclipse.property_editors.NgxSmartSourcePropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.StringComboBoxPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.mobile.NgxBuilder;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager.Mode;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class NgxUIComponentTreeObject extends NgxComponentTreeObject implements IEditableTreeObject, IOrderableTreeObject, INamedSourceSelectorTreeObject {
	
	public NgxUIComponentTreeObject(Viewer viewer, UIComponent object) {
		super(viewer, object);
	}

	public NgxUIComponentTreeObject(Viewer viewer, UIComponent object, boolean inherited) {
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
		} else {
			super.launchEditor(editorType);
		}
	}
	
	public void editCompTsFile() {
		if (!(getObject() instanceof ISharedComponent)) {
			return;
		}
		
		final UISharedComponent comp = (UISharedComponent)getObject();
		try {
			// Refresh project resource
			String projectName = comp.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor
			String filePath = comp.getProject().getMobileBuilder().getTempTsRelativePath((ISharedComponent)comp);
			IFile file = project.getFile(filePath);
			closeComponentFileEditor(file);
			
			// Write temporary file
			comp.getProject().getMobileBuilder().writeCompTempTs((ISharedComponent) comp);
			file.refreshLocal(IResource.DEPTH_ZERO, null);

			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, comp);
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
									NgxUIComponentTreeObject.this.setPropertyValue("scriptContent", scriptContent);
								}
							}
						}
					});
				}			
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to open typescript file for component '" + comp.getName() + "'!");
		}
	}
	
	private void editFunction(final UIComponent uic, final String functionMarker, final String propertyName) {
		try {
			IScriptComponent main = uic.getMainScriptComponent();
			if (main == null) {
				return;
			}
			
			// Refresh project resources for editor
			String projectName = uic.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor and Reopen it after file has been rewritten
			String relativePath = uic.getProject().getMobileBuilder().getFunctionTempTsRelativePath(uic);
			IFile file = project.getFile(relativePath);
			if (!(uic instanceof UICustomAction)) {
				closeComponentFileEditor(file);
			}
			
			if (main instanceof ApplicationComponent) {
				if (uic.compareToTplVersion("7.5.2.0") < 0) {
					ConvertigoPlugin.logError("The ability to use forms or actions inside a menu is avalaible since 7.5.2 version."
							+ "\nPlease change your Template project for the 'mobilebuilder_tpl_7_5_2' template.", true);
					return;
				}
			}
			uic.getProject().getMobileBuilder().writeFunctionTempTsFile(uic, functionMarker);
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
									String content = MobileBuilder.getFormatedContent(marker, functionMarker);
									FormatedContent formated = new FormatedContent(content);
									NgxUIComponentTreeObject.this.setPropertyValue(propertyName, formated);
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
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor
			IFile file = project.getFile(filePath);
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
					String editorId = "org.eclipse.ui.genericeditor.GenericEditor";
					
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
									NgxUIComponentTreeObject.this.setPropertyValue("htmlTemplate", htmlTemplate);
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
		final UIStyle ms = (UIStyle) getObject();
		String filePath = "/_private/editor/" + StringUtils.hash(ms.getParent().getQName()) + "/" + ms.getName() + ".scss";
		try {
			// Refresh project resource
			String projectName = ms.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Close editor
			IFile file = project.getFile(filePath);
			closeComponentFileEditor(file);
			
			// Write css file
			SwtUtils.fillFile(file, formatStyleContent(ms));
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, ms);
				if (input != null) {
					String editorId = "org.eclipse.ui.genericeditor.GenericEditor";
					
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
										NgxUIComponentTreeObject.this.setPropertyValue("styleContent", formatedContent);
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
		        			propertyDescriptor = new NgxSmartSourcePropertyDescriptor(id, displayName, tags, !isEditable);
		        			((NgxSmartSourcePropertyDescriptor)propertyDescriptor).databaseObjectTreeObject = this;
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
											((AbstractDialogCellEditor)cellEditor).databaseObjectTreeObject = NgxUIComponentTreeObject.this;
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
	        				MobileBuilder mb = null;
	        				
	        				IEditorPart editorPart = ConvertigoPlugin.getDefault().getApplicationComponentEditor();
	        				if (editorPart != null) {
	        					IEditorInput input = editorPart.getEditorInput();
	        					if (input instanceof com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput) {
	        						com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput editorInput = GenericUtils.cast(input);
	        						mb = editorInput.getApplication().getProject().getMobileBuilder();
	        					}
	        				}
	        				try {
				        		ionBean.setPropertyValue((String)id, value);
				        		
				        		TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
				        		hasBeenModified(true);
				        		viewer.update(this, null);
				        		
				    			Engine.logStudio.info("---------------------- SetPropertyValue started: "+ (String)id + "----------------------");
				    			if (mb != null) {
				    				mb.prepareBatchBuild();
				    			}
				    			BatchOperationHelper.start();
				    			
				    	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(this, (String)id, oldValue, value);
				    	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
				        		
				    	        BatchOperationHelper.stop();
	        				} catch (Exception e) {
	        					
	        				} finally {
	        					BatchOperationHelper.cancel();
	        					Engine.logStudio.info("---------------------- SetPropertyValue ended:   "+ (String)id + "----------------------");
	        				}
	        				
			    	        return;
	        			}
	        		}
	        	}
        	}
        }
        if (dbo instanceof UIAppGuard) {
        	UIAppGuard dboGuard = (UIAppGuard)dbo;
        	if ("guardType".equals(id)) {
        		String guardType = (String)value;
        		if (!guardType.equals(dboGuard.getGuardType().name())) {
        			if (dboGuard.getApplication().hasGuard(AppGuardType.valueOf(guardType))) {
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
				return NgxUIComponentTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				UIComponent object = getObject();
				
				if (object instanceof UIDynamicTabButton) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						NgxPageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("tabpage");
					}
				}
				else if (object instanceof UIDynamicMenuItem) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						NgxPageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("itempage");
					}
				}
				else if (object instanceof UIDynamicAnimate) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						NgxPageComponentTreeObject.class.isAssignableFrom(c) ||
						NgxUIComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("identifiable");
					}
				}
				else if (object instanceof UIDynamicInvoke) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						NgxUIComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("stack");
					}
				}
				else if (object instanceof UIUseShared) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						NgxUIComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("sharedcomponent");
					}
				}
				else if (object instanceof UIDynamicInfiniteScroll) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						MobileApplicationTreeObject.class.isAssignableFrom(c) ||
						NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
						NgxPageComponentTreeObject.class.isAssignableFrom(c) ||
						NgxUIComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("scrollaction");
					}
				}
				else if (object instanceof UIDynamicElement) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c))
					{
						list.add("requestable");
					}
					
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
							MobileApplicationTreeObject.class.isAssignableFrom(c) ||
							NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
							NgxPageComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("page");
					}
					
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
							MobileApplicationTreeObject.class.isAssignableFrom(c) ||
							NgxApplicationComponentTreeObject.class.isAssignableFrom(c) ||
							NgxUIComponentTreeObject.class.isAssignableFrom(c))
					{
						list.add("event");
						list.add("compvar");
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
				UIComponent object = getObject();
				
				if (object instanceof UIDynamicTab) {
					return "tabpage".equals(propertyName);
				}
				else if (object instanceof UIDynamicMenuItem) {
					return "itempage".equals(propertyName);
				}
				else if (object instanceof UIDynamicAnimate) {
					return "identifiable".equals(propertyName);
				}
				else if (object instanceof UIDynamicInvoke) {
					return "stack".equals(propertyName);
				}
				else if (object instanceof UIUseShared) {
					return "sharedcomponent".equals(propertyName);
				}
				else if (object instanceof UIDynamicInfiniteScroll) {
					return "scrollaction".equals(propertyName);
				}
				else if (object instanceof UIDynamicElement) {
					return "requestable".equals(propertyName) || 
								"fsview".equals(propertyName) ||
									"page".equals(propertyName) ||
										"event".equals(propertyName) ||
											"compvar".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				UIComponent object = getObject();
				
				if (object instanceof UIDynamicTabButton) {
					if ("tabpage".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(object.getProject()));
						}
					}
				}
				else if (object instanceof UIDynamicMenuItem) {
					if ("itempage".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(object.getProject()));
						}
					}
				}
				else if (object instanceof UIDynamicAnimate) {
					if ("identifiable".equals(propertyName)) {
						UIDynamicAnimate uda = (UIDynamicAnimate) object;
						if (nsObject instanceof UIElement) {
							UIElement ue = (UIElement)nsObject;
							if (hasSameScriptComponent(uda, ue)) {
								return !ue.getIdentifier().isEmpty();
							}
						}
					}
				}
				else if (object instanceof UIDynamicInvoke) {
					if ("stack".equals(propertyName)) {
						return nsObject instanceof UIActionStack;
					}
				}
				else if (object instanceof UIUseShared) {
					if ("sharedcomponent".equals(propertyName)) {
						return nsObject instanceof UISharedComponent;
					}
				}
				else if (object instanceof UIDynamicInfiniteScroll) {
					if ("scrollaction".equals(propertyName)) {
						if (nsObject instanceof UIDynamicAction) {
							UIDynamicAction uida = (UIDynamicAction) nsObject;
							if (uida.getProject().equals(object.getProject())) {
								if (uida.getIonBean().getName().equals("CallSequenceAction")) {
									return true;
								}
								if (uida.getIonBean().getName().equals("FullSyncViewAction")) {
									return true;
								}
							}
						}
					}
				}
				else if (object instanceof UIDynamicElement) {
					if ("requestable".equals(propertyName)) {
						UIDynamicElement cc = (UIDynamicElement) object;
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
						if (cc.getIonBean().getName().equals("AutoScrollComponent")) {
							return nsObject instanceof Sequence;
						}
					}
					if ("fsview".equals(propertyName)) {
						UIDynamicElement cc = (UIDynamicElement) object;
						if (cc.getIonBean().getName().equals("FullSyncViewAction")) {
							return nsObject instanceof String;
						}
						if (cc.getIonBean().getName().equals("AutoScrollComponent")) {
							return nsObject instanceof DesignDocument || nsObject instanceof String;
						}
					}
					if ("page".equals(propertyName)) {
						if (nsObject instanceof PageComponent) {
							return (((PageComponent)nsObject).getProject().equals(object.getProject()));
						}
					}
					if ("event".equals(propertyName)) {
						UIDynamicElement cc = (UIDynamicElement) object;
						if (cc.getIonBean().getName().equals("EmitEventAction")) {
							if (nsObject instanceof UICompEvent) {
								return (((UICompEvent)nsObject).getSharedComponent().equals(object.getSharedComponent()));
							}
						}
					}
					if ("compvar".equals(propertyName)) {
						UIDynamicElement cc = (UIDynamicElement) object;
						if (cc.getIonBean().getName().equals("EmitValueAction")) {
							if (nsObject instanceof UICompVariable) {
								return (((UICompVariable)nsObject).getSharedComponent().equals(object.getSharedComponent()));
							}
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
					if (oValue instanceof MobileSmartSourceType) {
						MobileSmartSourceType sst = (MobileSmartSourceType) oValue;
						pValue = sst.getSmartValue();
					} else {
						pValue = (String) oValue;
					}
					
					String _pValue = pValue;
					if (pValue != null && (pValue.startsWith(oldName + ".") || pValue.equals(oldName))) {
						_pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							UIComponent object = getObject();
							if (object instanceof UIDynamicTab) {
								if ("tabpage".equals(propertyName)) {
									((UIDynamicTab)object).setTabPage(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (object instanceof UIDynamicMenuItem) {
								if ("itempage".equals(propertyName)) {
									((UIDynamicMenuItem)object).setItemPage(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (object instanceof UIDynamicAnimate) {
								if ("identifiable".equals(propertyName)) {
									((UIDynamicAnimate)object).setIdentifiable(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (object instanceof UIDynamicInvoke) {
								if ("stack".equals(propertyName)) {
									((UIDynamicInvoke)object).setSharedActionQName(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (object instanceof UIUseShared) {
								if ("sharedcomponent".equals(propertyName)) {
									((UIUseShared)object).setSharedComponentQName(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (object instanceof UIDynamicInfiniteScroll) {
								if ("scrollaction".equals(propertyName)) {
									((UIDynamicInfiniteScroll)object).setScrollAction(_pValue);
									hasBeenRenamed = true;
								}
							}
							else if (object instanceof UIDynamicElement) {
								if ("requestable".equals(propertyName)) {
									((UIDynamicElement)object).getIonBean().
										setPropertyValue("requestable", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("fsview".equals(propertyName)) {
									((UIDynamicElement)object).getIonBean().
										setPropertyValue("fsview", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("page".equals(propertyName)) {
									((UIDynamicElement)object).getIonBean().
										setPropertyValue("page", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("event".equals(propertyName)) {
									((UIDynamicElement)object).getIonBean().
										setPropertyValue("event", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
								if ("compvar".equals(propertyName)) {
									((UIDynamicElement)object).getIonBean().
										setPropertyValue("compvar", new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
							}
							else if (object instanceof UIText) {
								if ("textValue".equals(propertyName)) {
									((UIText) object).setTextSmartType(new MobileSmartSourceType(_pValue));
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						viewer.refresh();
						
						ConvertigoPlugin.projectManager.getProjectExplorerView().updateTreeObject(NgxUIComponentTreeObject.this);
						getDescriptors();// refresh editors (e.g labels in combobox)
						
		    	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(NgxUIComponentTreeObject.this, propertyName, pValue, _pValue);
		    	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
					}
				}
			}
			
			@Override
			protected void refactorSmartSources(Class<?> c, String oldName, String newName) {
				try {
					// A project has been renamed
					if (ProjectTreeObject.class.isAssignableFrom(c)) {
						UIComponent object = getObject();
						for (java.beans.PropertyDescriptor pd: CachedIntrospector.getBeanInfo(object).getPropertyDescriptors()) {
							if (pd.getPropertyType().equals(MobileSmartSourceType.class)) {
								String propertyName = pd.getName();
								Object oValue = getPropertyValue(propertyName);
								MobileSmartSourceType msst = (MobileSmartSourceType) oValue;
								MobileSmartSource mss = msst.getSmartSource();
								boolean hasBeenChanged = false;
								if (mss != null) {
									if (oldName.equals(mss.getProjectName())) {
										mss.setProjectName(newName);
										msst.setSmartValue(mss.toJsonString());
										hasBeenChanged = true;
									}
								}
								
								if (hasBeenChanged) {
									Object nValue = getPropertyValue(propertyName);
									
									hasBeenModified(true);
									viewer.refresh();
									
									ConvertigoPlugin.projectManager.getProjectExplorerView().updateTreeObject(NgxUIComponentTreeObject.this);
									getDescriptors();// refresh editors (e.g labels in combobox)
									
					    	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(NgxUIComponentTreeObject.this, propertyName, oValue, nValue);
					    	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
								}
							}
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		};
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);

		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Set<Object> done = checkDone(treeObjectEvent);
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			
			try {
				if (this.equals(treeObject)) {
					UIActionStack uisa = ((UIComponent)dbo).getSharedAction();
					UISharedComponent uisc = ((UIComponent)dbo).getSharedComponent();
					if (uisa != null && !uisa.equals(getObject())) {
						notifyDataseObjectPropertyChanged(uisa, "", null, null, done);
					}
					if (uisc != null && !uisc.equals(getObject())) {
						notifyDataseObjectPropertyChanged(uisc, "", null, null, done);
					}
				} else {
					if (dbo instanceof UIActionStack) {
						handleSharedActionChanged((UIActionStack) dbo, done);
					}
					else if (dbo instanceof UISharedComponent) {
						handleSharedComponentChanged((UISharedComponent) dbo, done);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Set<Object> done = checkDone(treeObjectEvent);
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject deletedTreeObject = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject deletedObject = deletedTreeObject.getObject();
			try {
				if (deletedTreeObject != null && this.equals(deletedTreeObject.getParentDatabaseObjectTreeObject())) {
					UIComponent parentDbo = getObject();
					
					if (deletedObject instanceof UIUseShared) {
						UIUseShared use = (UIUseShared)deletedObject;
						String compQName = use.getSharedComponentQName();
						String useQNname = parentDbo.getQName() + "." + deletedObject.getName();
						ComponentRefManager.get(Mode.use).removeConsumer(compQName, useQNname);
					}
										
					UIActionStack uisa = parentDbo.getSharedAction();
					UISharedComponent uisc = parentDbo.getSharedComponent();
					if (uisa != null) {
						notifyDataseObjectPropertyChanged(uisa, "", null, null, done);
					}
					else if (uisc != null) {
						notifyDataseObjectPropertyChanged(uisc, "", null, null, done);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);

		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Set<Object> done = checkDone(treeObjectEvent);
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		refactorSmartSources(treeObjectEvent);
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			
			try {
				if (this.equals(treeObject)) {
					if (propertyName.equals("scriptContent")) {
						if (!newValue.equals(oldValue)) {
							markMainTsAsDirty();
							markMainAsDirty(getObject(), done);
						}
					} else {
						if (propertyName.equals("sharedcomponent")) {
							if (!newValue.equals(oldValue)) {
								if (!((String)newValue).isBlank()) {
									ComponentRefManager.get(Mode.use).addConsumer((String)newValue, dbo.getQName());
								}
							}
						}
						
						markMainAsDirty(getObject(), done);
					}
					
					UIActionStack uisa = ((UIComponent)dbo).getSharedAction();
					UISharedComponent uisc = ((UIComponent)dbo).getSharedComponent();
					if (uisa != null && !uisa.equals(getObject())) {
						notifyDataseObjectPropertyChanged(uisa, "", null, null, done);
					}
					if (uisc != null && !uisc.equals(getObject())) {
						notifyDataseObjectPropertyChanged(uisc, "", null, null, done);
					}
				} else {
					if (propertyName.equals("name")) {
						handlesBeanNameChanged(treeObjectEvent);
					}
					
					if (dbo instanceof UIActionStack) {
						handleSharedActionChanged((UIActionStack) dbo, done);
					}
					else if (dbo instanceof UISharedComponent) {
						if (getObject() instanceof UIUseShared) {
							UIUseShared uiUse = (UIUseShared)getObject();
							String useQName = uiUse.getQName();
							String compQName = dbo.getQName();
							if (ComponentRefManager.get(Mode.use).getAllConsumers(compQName).contains(useQName)) {
								((NgxBuilder)uiUse.getProject().getMobileBuilder()).updateConsumer();
							}
						}
						
						handleSharedComponentChanged((UISharedComponent) dbo, done);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void handleSharedActionChanged(UIActionStack sharedAction, Set<Object> done) {
		if (sharedAction != null) {
			// a uic has changed/added/removed from a shared action referenced by this UIDynamicInvoke
			if (getObject() instanceof UIDynamicInvoke) {
				UIDynamicInvoke udi = (UIDynamicInvoke)getObject();
				if (udi.getSharedActionQName().equals(sharedAction.getQName())) {
					UIActionStack uisa = udi.getSharedAction();
					UISharedComponent uisc = udi.getSharedComponent();
					
					// udi inside a shared action
					if (uisa != null && !uisa.equals(sharedAction)) {
						notifyDataseObjectPropertyChanged(uisa, "", null, null, done);
					}
					// udi inside a shared component
					else if (uisc != null) {
						notifyDataseObjectPropertyChanged(uisc, "", null, null, done);
					}
					// udi inside a page or menu
					else {
						try {
							markMainAsDirty(udi, done);
						} catch (EngineException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	protected void handleSharedComponentChanged(UISharedComponent sharedComponent, Set<Object> done) {
		if (sharedComponent != null) {
			// a uic has changed/added/removed from a shared component referenced by this UIUseShared
			if (getObject() instanceof UIUseShared) {
				UIUseShared udu = (UIUseShared)getObject();
				
				if (udu.getSharedComponentQName().equals(sharedComponent.getQName())) {
					UISharedComponent uisc = udu.getSharedComponent();
					// udu inside a shared component
					if (uisc != null && !uisc.equals(sharedComponent)) {
						notifyDataseObjectPropertyChanged(uisc, "", null, null, done);
					}
					// udu inside a page or menu
					else {
						try {
							markMainAsDirty(udu, done);
						} catch (EngineException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	protected void markMainTsAsDirty() {
		if (getObject() instanceof UISharedComponent) {
			UISharedComponent comp = (UISharedComponent)getObject();
			try {
				comp.markCompTsAsDirty();
			} catch (EngineException e) {
				ConvertigoPlugin.logException(e,
						"Error while writing the component.ts file for component '" + comp.getName() + "'");
			}
		}
	}
	
	protected void markMainAsDirty(UIComponent uic, Set<Object> done) throws EngineException {
		if (uic != null) {
			IScriptComponent main = uic.getMainScriptComponent();
			if (main != null) {
				if (!done.add(main)) {
					return;
				}
				//System.out.println("---markMainAsDirty for dbo@"+ uic.priority +" " + uic.toString() + ", with done : '" + done + "'");
				if (main instanceof UISharedComponent) {
					((UISharedComponent)main).markCompAsDirty();
				}
				if (main instanceof ApplicationComponent) {
					((ApplicationComponent)main).markApplicationAsDirty();
				}
				if (main instanceof PageComponent) {
					((PageComponent)main).markPageAsDirty();
				}
			}
		}
	}
	
	protected void markMainAsDirty(UIComponent uic) throws EngineException {
		markMainAsDirty(uic, new HashSet<Object>());
	}

	protected static boolean hasSameScriptComponent(UIComponent uic1, UIComponent uic2) {
		if (uic1 != null && uic2 != null) {
			try {
				return uic1.getMainScriptComponent().equals(uic2.getMainScriptComponent());
			} catch (Exception e) {}
		}
		return false;
	}
	
	protected void refactorSmartSources(TreeObjectEvent treeObjectEvent) {
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		// Case of DatabaseObjectTreeObject
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			try {
				boolean sourcesUpdated = false;
				
				// A bean name has changed
				if (propertyName.equals("name")) {
					boolean fromSameProject = getProjectTreeObject().equals(doto.getProjectTreeObject());
					if ((treeObjectEvent.update == TreeObjectEvent.UPDATE_ALL) 
						|| ((treeObjectEvent.update == TreeObjectEvent.UPDATE_LOCAL) && fromSameProject)) {
						try {
							if (dbo instanceof Project) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								if (!newValue.equals(oldValue)) {
									if (getObject().updateSmartSource("'"+oldName+"\\.", "'"+newName+".")) {
										sourcesUpdated = true;
									}
									if (getObject().updateSmartSource("\\/"+oldName+"\\.", "/"+newName+".")) {
										sourcesUpdated = true;
									}
								}
							}
							else if (dbo instanceof Sequence) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								String projectName = dbo.getProject().getName();
								if (!newValue.equals(oldValue)) {
									if (getObject().updateSmartSource("'"+projectName+"\\."+oldName, "'"+projectName+"."+newName)) {
										sourcesUpdated = true;
									}
								}
							}
							else if (dbo instanceof FullSyncConnector) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								String projectName = dbo.getProject().getName();
								if (!newValue.equals(oldValue)) {
									if (getObject().updateSmartSource("\\/"+projectName+"\\."+oldName+"\\.", "/"+projectName+"."+newName+".")) {
										sourcesUpdated = true;
									}
									if (getObject().updateSmartSource("\\/"+oldName+"\\.", "/"+newName+".")) {
										sourcesUpdated = true;
									}
								}
							}
							else if (dbo instanceof DesignDocument) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								if (!newValue.equals(oldValue)) {
									if (getObject().updateSmartSource("ddoc='"+oldName+"'", "ddoc='"+newName+"'")) {
										sourcesUpdated = true;
									}
								}
							}
							if (dbo instanceof UIComponent) {
								if (!newValue.equals(oldValue)) {
									try {
										String oldName = (String)oldValue;
										String newName = (String)newValue;
										if (getObject().updateSmartSource("\\?\\."+oldName, "?."+newName)) {
											sourcesUpdated = true;
										}
									} catch (Exception e) {}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent)dbo;
					if (hasSameScriptComponent(getObject(), uic)) {
						// A ControlName property has changed
						if (propertyName.equals("ControlName") || uic.isFormControlAttribute()) {
							if (!newValue.equals(oldValue)) {
								try {
									String oldSmart = ((MobileSmartSourceType)oldValue).getSmartValue();
									String newSmart = ((MobileSmartSourceType)newValue).getSmartValue();
									if (uic.getUIForm() != null) {
										if (getObject().updateSmartSource("\\?\\.controls\\['"+oldSmart+"'\\]", "?.controls['"+newSmart+"']")) {
											sourcesUpdated = true;
										}
									}
								} catch (Exception e) {}
							}
						}
						else if (propertyName.equals("identifier")) {
							if (!newValue.equals(oldValue)) {
								try {
									String oldId = (String)oldValue;
									String newId = (String)newValue;
									if (uic.getUIForm() != null) {
										if (getObject().updateSmartSource("\"identifier\":\""+oldId+"\"", "\"identifier\":\""+newId+"\"")) {
											sourcesUpdated = true;
										}
									}
								} catch (Exception e) {}
							}
						}
					}
				}
				
				// Need TS regeneration
				if (sourcesUpdated) {
					hasBeenModified(true);
					viewer.refresh();
					
					markMainAsDirty(getObject());
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Case of DesignDocumentViewTreeObject
		else if (treeObject instanceof DesignDocumentViewTreeObject) {
			DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject)treeObject;
			try {
				boolean sourcesUpdated = false;
				
				// View name changed
				if (propertyName.equals("name")) {
					boolean fromSameProject = getProjectTreeObject().equals(ddvto.getProjectTreeObject());
					if ((treeObjectEvent.update == TreeObjectEvent.UPDATE_ALL) 
						|| ((treeObjectEvent.update == TreeObjectEvent.UPDATE_LOCAL) && fromSameProject)) {
						try {
							String oldName = (String)oldValue;
							String newName = (String)newValue;
							if (!newValue.equals(oldValue)) {
								if (getObject().updateSmartSource("view='"+oldName+"'", "view='"+newName+"'")) {
									sourcesUpdated = true;
								}
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				// Need TS regeneration
				if (sourcesUpdated) {
					hasBeenModified(true);
					
					viewer.refresh();
					markMainAsDirty(getObject());
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		int update = treeObjectEvent.update;
		
		if (update != TreeObjectEvent.UPDATE_NONE) {
			// Case a UIStackVariable has been renamed
			if (databaseObject instanceof UIStackVariable) {
				UIStackVariable variable = (UIStackVariable)databaseObject;
				UIActionStack stack = variable.getSharedAction();
				if (stack != null) {
					// rename variable for InvokeAction
					if (getObject() instanceof UIDynamicInvoke) {
						UIDynamicInvoke udi = (UIDynamicInvoke)getObject();
						if (udi.getSharedActionQName().equals(stack.getQName())) {
							boolean isLocalProject = variable.getProject().equals(udi.getProject());
							boolean isSameValue = variable.getName().equals(oldValue);
							boolean shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
							
							if (!isSameValue && shouldUpdate) {
								Iterator<UIComponent> it = udi.getUIComponentList().iterator();
								while (it.hasNext()) {
									UIComponent component = (UIComponent)it.next();
									if (component instanceof UIControlVariable) {
										UIControlVariable uicv = (UIControlVariable)component;
										if (uicv.getName().equals(oldValue)) {
											try {
												uicv.setName((String) newValue);
												uicv.hasChanged = true;
												
												hasBeenModified(true);
												viewer.refresh();
												markMainAsDirty(udi);
												
												notifyDataseObjectPropertyChanged(uicv, "name", oldValue, newValue, new HashSet<Object>());
												break;
											} catch (EngineException e) {
												ConvertigoPlugin.logException(e, "Unable to refactor the references of '" + newValue + "' variable for InvokeAction !");
											}
										}
									}
								}
							}
						}
					}
				}
			}
			// Case a UICompVariable has been renamed
			else if (databaseObject instanceof UICompVariable) {
				UICompVariable variable = (UICompVariable)databaseObject;
				UISharedComponent comp = variable.getSharedComponent();
				if (comp != null) {
					// rename variable for comp
					if (comp.equals(getObject())) {
						if (comp.isRegular()) {
							String oldString = "\\."+oldValue;
							String newString = "."+newValue;
							if (comp.updateSmartSources(oldString, newString)) {
								try {
									hasBeenModified(true);
									viewer.refresh();
									
									markMainAsDirty(comp);
								} catch (EngineException e) {
									ConvertigoPlugin.logException(e, "Unable to refactor the references of '" + newValue + "' variable for SharedComponent !");
								}
							}
						}
					}
					
					// rename variable for UseShared
					if (getObject() instanceof UIUseShared) {
						UIUseShared uus = (UIUseShared)getObject();
						if (uus.getSharedComponentQName().equals(comp.getQName())) {
							boolean isLocalProject = variable.getProject().equals(uus.getProject());
							boolean isSameValue = variable.getName().equals(oldValue);
							boolean shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
							
							if (!isSameValue && shouldUpdate) {
								Iterator<UIComponent> it = uus.getUIComponentList().iterator();
								while (it.hasNext()) {
									UIComponent component = (UIComponent)it.next();
									if (component instanceof UIUseVariable) {
										UIUseVariable uicv = (UIUseVariable)component;
										if (uicv.getName().equals(oldValue)) {
											try {
												uicv.setName((String) newValue);
												uicv.hasChanged = true;
												
												hasBeenModified(true);
												viewer.refresh();
												markMainAsDirty(uus);
												
												notifyDataseObjectPropertyChanged(uicv, "name", oldValue, newValue, new HashSet<Object>());
												break;
											} catch (EngineException e) {
												ConvertigoPlugin.logException(e, "Unable to refactor the references of '" + newValue + "' variable for UseShared !");
											}
										}
									}
								}
							}
						}
					}
				}
			}
			// Case a UICompEvent has been renamed
			else if (databaseObject instanceof UICompEvent) {
				UICompEvent event = (UICompEvent)databaseObject;
				UISharedComponent comp = event.getSharedComponent();
				if (comp != null) {
					// rename control event for UseShared
					if (getObject() instanceof UIUseShared) {
						UIUseShared uus = (UIUseShared)getObject();
						if (uus.getSharedComponentQName().equals(comp.getQName())) {
							boolean isLocalProject = event.getProject().equals(uus.getProject());
							boolean isSameValue = event.getName().equals(oldValue);
							boolean shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
							
							if (!isSameValue && shouldUpdate) {
								Iterator<UIComponent> it = uus.getUIComponentList().iterator();
								while (it.hasNext()) {
									UIComponent component = (UIComponent)it.next();
									if (component instanceof UIControlEvent) {
										UIControlEvent uice = (UIControlEvent)component;
										if (uice.getEventName().equals(oldValue)) {
											try {
												uice.setEventName((String) newValue);
												uice.hasChanged = true;
												
												hasBeenModified(true);
												viewer.refresh();
												markMainAsDirty(uus);
												
												notifyDataseObjectPropertyChanged(uice, "eventName", oldValue, newValue, new HashSet<Object>());
												break;
											} catch (EngineException e) {
												ConvertigoPlugin.logException(e, "Unable to refactor the references of '" + newValue + "' event for UseShared !");
											}
										}
									}
								}
							}
						}
					}
				}
			}
			// Case a RequestableVariable has been renamed
			else if (databaseObject instanceof RequestableVariable) {
				RequestableVariable variable = (RequestableVariable)databaseObject;
				DatabaseObject parent = variable.getParent();
				if (getObject() instanceof UIDynamicAction) {
					UIDynamicAction uia = (UIDynamicAction)getObject();
					IonBean ionBean = uia.getIonBean();
					if (ionBean != null) {
						// rename variable for CallSequenceAction
						if (ionBean.getName().equals("CallSequenceAction")) {
							Object p_val = ionBean.getProperty("requestable").getValue();
							if (!p_val.equals(false)) {
								if (parent.getQName().equals(p_val.toString())) {
									boolean isLocalProject = variable.getProject().equals(uia.getProject());
									boolean isSameValue = variable.getName().equals(oldValue);
									boolean shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
									
									if (!isSameValue && shouldUpdate) {
										Iterator<UIComponent> it = uia.getUIComponentList().iterator();
										while (it.hasNext()) {
											UIComponent component = (UIComponent)it.next();
											if (component instanceof UIControlVariable) {
												UIControlVariable uicv = (UIControlVariable)component;
												if (uicv.getName().equals(oldValue)) {
													try {
														uicv.setName((String) newValue);
														uicv.hasChanged = true;
														
														hasBeenModified(true);
														viewer.refresh();
														markMainAsDirty(uia);
														
														notifyDataseObjectPropertyChanged(uicv, "name", oldValue, newValue, new HashSet<Object>());
														break;
													} catch (EngineException e) {
														ConvertigoPlugin.logException(e, "Unable to refactor the references of '" + newValue + "' variable for CallSequenceAction !");
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	protected void notifyDataseObjectPropertyChanged(DatabaseObject dbo, String propertyName, Object oldValue, Object newValue, Set<Object> done) {
		TreeObject to = ConvertigoPlugin.projectManager.getProjectExplorerView().findTreeObjectByUserObject(dbo);
		if (to != null) {
			notifyTreeObjectPropertyChanged(to, propertyName, oldValue, newValue, done);
		}
	}
	
	synchronized protected void notifyTreeObjectPropertyChanged(TreeObject to, String propertyName, Object oldValue, Object newValue, Set<Object> done) {
		if (done == null) {
			done = new HashSet<Object>();
		}
		if (!done.add(to)) {
			return;
		}
		
		if (to instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject tdbo = (DatabaseObjectTreeObject)to;
    		TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    		tdbo.hasBeenModified(true);
    		viewer.update(tdbo, null);
		}
		
		//System.out.println("---notifyDataseObjectPropertyChanged for dbo " + to.toString() + " with propertyName : '" + propertyName + "'");
        TreeObjectEvent toe = new TreeObjectEvent(to, propertyName, oldValue, newValue, 0, done);
        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(toe);
	}
}
