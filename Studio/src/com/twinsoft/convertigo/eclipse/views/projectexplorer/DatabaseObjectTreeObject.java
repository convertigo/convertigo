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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetEntry;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.INillableProperty;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScDefaultHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.property_editors.AbstractDialogCellEditor;
import com.twinsoft.convertigo.eclipse.property_editors.ArrayOrNullEditor;
import com.twinsoft.convertigo.eclipse.property_editors.DataOrNullPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.DynamicComboBoxPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.EmulatorTechnologyEditor;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithDynamicTagsEditor;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithTagsEditor;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithTagsEditorAdvance;
import com.twinsoft.convertigo.eclipse.property_editors.StringOrNullEditor;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;


public class DatabaseObjectTreeObject extends TreeParent implements TreeObjectListener, IPropertySource, IActionFilter {

	public static final String P_TYPE = "#type";
	public static final String P_JAVA_CLASS = "#java_class";
	public static final String P_DEPTH = "#depth";
	public static final String P_PRIORITY = "#priority";
	public static final String P_QNAME = "#qname";
	public static final String P_NAME = "#name";

	public String objectClassName = null;
	public String latestSavedDatabaseObjectQName = null;
	public String latestSavedDatabaseObjectName = null;
	public boolean isModified = false;
	public boolean canPaste = false;
	
    /**
     * Indicates the inheritance status of the object.
     */
    public boolean isInherited = false;
    
    /**
     * Indicates the detection status of the object.
     */
    public boolean isDetectedObject = false;

    /**
     * Indicates if the object is under version control.
     */
    public boolean isUnderCvs = false;
   
    /**
     * Indicates if the object is currently checked out.
     */
    public boolean isCheckedOut = false;
    
    /**
     * Indicates if the object is currently enabled.
     */
	protected boolean isEnabled = true;

	/**
	 * Indicates if the object is currently the default one.
	 */
	public boolean isDefault = false;
	
    protected int nModifications = 0;
    
    public DatabaseObjectTreeObject(Viewer viewer, DatabaseObject object) {
    	this(viewer,object,false);
	}

    public DatabaseObjectTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object);
		isInherited = inherited;
        latestSavedDatabaseObjectQName = ((object.hasChanged && !object.bNew) ? object.getOldQName():object.getQName());
        latestSavedDatabaseObjectName = ((object.hasChanged && !object.bNew) ? object.getOldName():object.getName());
        hasBeenModified((object.bNew) || (object.hasChanged && !object.bNew));
		getDescriptors();
	}

    @Override
    public DatabaseObject getObject(){
    	return (DatabaseObject) super.getObject();
    }
    
    @Override
    public void setParent(TreeParent parent) {
		super.setParent(parent);
		if (parent == null) {
			ConvertigoPlugin.projectManager.getProjectExplorerView().removeTreeObjectListener(this);
		}
		else {
			ConvertigoPlugin.projectManager.getProjectExplorerView().addTreeObjectListener(this);
			
			// Notify here new bean has been added (Fix ticket #20)
			Object object = getObject();
			if (object instanceof DatabaseObject) {
				if (((DatabaseObject)object).bNew) {
					ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectAdded(new TreeObjectEvent(this));
				}
			}
		}
	}

    @Override
	public void update() {
		if (isInherited)
			return;
		
		DatabaseObject databaseObject = getObject();
		if ((nModifications == 0) && (databaseObject.hasChanged))
			hasBeenModified(true);
		else if ((nModifications > 0) && (!databaseObject.hasChanged))
			hasBeenModified(false);
	}

    @Override
	protected void remove() {
		if (isInherited)
			return;
	}
	
    public boolean isEnabled() {
    	return isEnabled;
    }
    
    public void setEnabled(boolean isEnabled) {
    	this.isEnabled = isEnabled;
    }
    
    public void hasBeenModified(boolean bModified) {
		if (isInherited)
			return;

		String projectName = getObject().getProject().getName();
		
		if (bModified) {
    		if (nModifications == 0) {
    			nModifications++;
    			markAsChanged(true);
    			try {
    				TreeObject project = ((ViewContentProvider) ((TreeViewer)viewer).getContentProvider()).getProjectRootObject(projectName);
    				if (project instanceof ProjectTreeObject) {
    					((ProjectTreeObject)project).hasBeenModified(bModified);
    				}
    			} catch (EngineException e) {}
    		}
    	}
    	else {
    		if (nModifications == 1) {
    			nModifications--;
    			try {
    				TreeObject project = ((ViewContentProvider) ((TreeViewer)viewer).getContentProvider()).getProjectRootObject(projectName);
    				if (project instanceof ProjectTreeObject) {
    					((ProjectTreeObject)project).hasBeenModified(bModified);
    				}
    			} catch (EngineException e) {}
    		}
    	}
	}
	
	public boolean save(boolean bDialog) {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = display.getActiveShell();
		shell.setCursor(waitCursor);
        
        try {
            if (hasChanged()) {
            	DatabaseObject databaseObject = getObject();

            	int response = SWT.YES;
                if (bDialog) {
                	MessageBox messageBox = new MessageBox(shell,SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
                	messageBox.setMessage("The object \""+ databaseObject.getName() +"\" has not been saved. Do you want to save your work now?");
                	response = messageBox.open();
                }
            	
                if (response == SWT.YES) {
                	ConvertigoPlugin.logInfo("Saving the object '"+ databaseObject.getName() +"'");
                	ConvertigoPlugin.projectManager.save(databaseObject, false);
                    ConvertigoPlugin.logInfo("Object '"+ databaseObject.getName() +"' saved!");
                }
                else if (response == SWT.NO) {
                	Engine.theApp.databaseObjectsManager.cacheRemoveObject(databaseObject.getQName());
                    return true;
                }
                else if (response == SWT.CANCEL) {
                    return false;
                }
            }
        }
        catch (Exception e) {
        	ConvertigoPlugin.logException(e, "Unable to save the object!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
        
        return true;
	}

	public DatabaseObjectTreeObject getOwnerDatabaseObjectTreeObject() {
		if (this instanceof ProjectTreeObject)	return this;
		TreeObject owner = getParent();
		while (owner!=null && !(owner instanceof DatabaseObjectTreeObject)) {
			owner = owner.getParent();
		}
		return (DatabaseObjectTreeObject)owner;
	}
	
	public BeanInfo databaseObjectBeanInfo = null;

    private IPropertyDescriptor[] propertyDescriptors = null;
    private BeanDescriptor databaseObjectBeanDescriptor = null;
    private java.beans.PropertyDescriptor[] databaseObjectPropertyDescriptors;

    protected void getDescriptors() {
    	if (propertyDescriptors != null && databaseObjectBeanDescriptor != null &&
    			databaseObjectPropertyDescriptors != null) {
    		return;
    	}
    	
    	DatabaseObject databaseObject = getObject();
		if ((!(databaseObject instanceof Project)) && (databaseObject.getParent() == null))
			return; // No needs for removed object
		
        int len;
        
        java.beans.PropertyDescriptor databaseObjectPropertyDescriptor;
        
        try {
    		String beanInfoClassName = databaseObject.getClass().getName() + "BeanInfo";
    		Class<?> beanInfoClass = Class.forName(beanInfoClassName);
    		databaseObjectBeanInfo = (BeanInfo) beanInfoClass.newInstance();
        	
        	BeanInfo bi = Introspector.getBeanInfo(databaseObject.getClass());
            databaseObjectBeanDescriptor = bi.getBeanDescriptor();
            databaseObjectPropertyDescriptors = bi.getPropertyDescriptors();
            len = databaseObjectPropertyDescriptors.length;
        }
        catch (Exception e) {
            String message = "Error while introspecting object " + databaseObject.getName() + " (" + databaseObject.getQName() + ")"; 
            ConvertigoPlugin.logException(e, message);
	        return;
        }
        
        Vector<PropertyDescriptor> vPropertyDescriptors = new Vector<PropertyDescriptor>(32);
        
        PropertyDescriptor propertyDescriptor;
        
        propertyDescriptor = new PropertyDescriptor(P_TYPE, "Type");
        propertyDescriptor.setCategory("Information");
        vPropertyDescriptors.add(propertyDescriptor);
        propertyDescriptor = new PropertyDescriptor(P_JAVA_CLASS, "Java class");
        propertyDescriptor.setCategory("Information");
        vPropertyDescriptors.add(propertyDescriptor);
        
        propertyDescriptor = new PropertyDescriptor(P_NAME, "Name");
        propertyDescriptor.setCategory("Information");
        vPropertyDescriptors.add(propertyDescriptor);
        
        propertyDescriptor = new PropertyDescriptor(P_QNAME, "QName");
        propertyDescriptor.setCategory("Information");
        vPropertyDescriptors.add(propertyDescriptor);
        propertyDescriptor = new PropertyDescriptor(P_PRIORITY, "Priority");
        propertyDescriptor.setCategory("Information");
        vPropertyDescriptors.add(propertyDescriptor);
        propertyDescriptor = new PropertyDescriptor(P_DEPTH, "Depth");
        propertyDescriptor.setCategory("Information");
        vPropertyDescriptors.add(propertyDescriptor);
        
        // Get properties
        boolean isExtractionRule = ( databaseObject instanceof com.twinsoft.convertigo.beans.core.ExtractionRule) ;
        boolean isMaskedProperty = false;
        for (int i = 0 ; i < len ; i++) {
            
            databaseObjectPropertyDescriptor = databaseObjectPropertyDescriptors[i];
            
            // Don't display hidden or expert propertyDescriptors.
            if (databaseObjectPropertyDescriptor.isHidden()) {
                continue;
            }
            
            String name = databaseObjectPropertyDescriptor.getName();
            String displayName = databaseObjectPropertyDescriptor.getDisplayName();
            Method getter = databaseObjectPropertyDescriptor.getReadMethod();
            Method setter = databaseObjectPropertyDescriptor.getWriteMethod();
            
            // Only display read/write propertyDescriptors.
            if (getter == null || setter == null) {
                continue;
            }
            
            Class<?> pec = null;
            
            try {
                Object args[] = { };
                Object value = getter.invoke(databaseObject, args);
                
                pec = databaseObjectPropertyDescriptor.getPropertyEditorClass();

                // Now figure out how to display it...
                isMaskedProperty = databaseObject.isMaskedProperty(Visibility.Studio, name);
                propertyDescriptor = findPropertyDescriptor(name, displayName, databaseObjectPropertyDescriptor, value, pec, isExtractionRule, isMaskedProperty);
                if (isMaskedProperty) {
                	propertyDescriptor.setLabelProvider(new LabelProvider() {
            			@Override
            			public String getText(Object element) {
            				String regexp = element instanceof String ? ".":"[^\\[\\]\\,]";
            				String text = super.getText(element);
            				return text.replaceAll(regexp, "*");
            			}
                    	
                    });
                }
                vPropertyDescriptors.add(propertyDescriptor);
            }
            catch (Exception e) {
                String message = "Error while introspecting parameter \"" + name + "\"";
                ConvertigoPlugin.logException(e, message);
                continue;
            }
        }

        propertyDescriptors = (IPropertyDescriptor[]) vPropertyDescriptors.toArray(new IPropertyDescriptor[] {});
	}
    
    private PropertyDescriptor findPropertyDescriptor(final String name, 
    													String displayName, 
    													java.beans.PropertyDescriptor databaseObjectPropertyDescriptor, 
    													Object value, 
    													final Class<?> pec,
    													boolean isExtractionRule,
    													boolean isMasked) 
    												throws IllegalAccessException, 
    													NoSuchMethodException, 
    													InvocationTargetException {
    	PropertyDescriptor propertyDescriptor = null;
    	
    	// Primitive types
        if (pec == null) {
			if (value instanceof Boolean) {
		    	String[] values = new String[] { "true", "false" };
				propertyDescriptor = new ComboBoxPropertyDescriptor(name, displayName, values);
		    }
		    else if (value instanceof Short) {
		        propertyDescriptor = new TextPropertyDescriptor(name, displayName);
		        propertyDescriptor.setValidator(new ICellEditorValidator() {
		        	public String isValid(Object value) {
		        		if (((String)value).indexOf("${")== -1)
	        			{
			        		try {
			            		Short.valueOf((String)value);
			            		return null;
			        		}
			        		catch(NumberFormatException e) {
			            		return "The value \"" + value + "\" is not a valid short number!";
			        		}
	        			}
		        		return null;
		        	}
		        });
		    }
		    else if (value instanceof Byte) {
		        propertyDescriptor = new TextPropertyDescriptor(name, displayName);
		        propertyDescriptor.setValidator(new ICellEditorValidator() {
		        	public String isValid(Object value) {
		        		if (((String)value).indexOf("${")== -1)
	        			{
			        		try {
			        			Byte.valueOf((String)value);
			            		return null;
			        		}
			        		catch(NumberFormatException e) {
			            		return "The value \"" + value + "\" is not a valid byte number!";
			        		}	
	        			}
		        		return null;
		        	}
		        });
		    }
		    else if (value instanceof Integer) {
		        propertyDescriptor = new TextPropertyDescriptor(name, displayName);
		        propertyDescriptor.setValidator(new ICellEditorValidator() {
		        	public String isValid(Object value) {
		        		if (((String)value).indexOf("${")== -1)
	        			{
			        		try {
			  		            Integer.valueOf((String)value);
			            		return null;
			        		}
			        		catch(NumberFormatException e) {
			            		return "The value \"" + value + "\" is not a valid integer number!";
			        		}
	        			}
		        		return null;
		        	}
		        });
		    }
		    else if (value instanceof Long) {
		        propertyDescriptor = new TextPropertyDescriptor(name, displayName);
		        propertyDescriptor.setValidator(new ICellEditorValidator() {
		        	public String isValid(Object value) {
		        		if (((String)value).indexOf("${")== -1)
	        			{
			        		try {
			            		Long.valueOf((String)value);
			            		return null;
			        		}
			        		catch(NumberFormatException e) {
			            		return "The value \"" + value + "\" is not a valid long number!";
			        		}
	        			}
		        		return null;
		        	}
		        });
		    }
		    else if (value instanceof Float) {
		        propertyDescriptor = new TextPropertyDescriptor(name, displayName);
		        propertyDescriptor.setValidator(new ICellEditorValidator() {
		        	public String isValid(Object value) {
		        		if (((String)value).indexOf("${")== -1)
	        			{
			        		try {
			            		Float.valueOf((String)value);
			            		return null;
			        		}
			        		catch(NumberFormatException e) {
			            		return "The value \"" + value + "\" is not a valid float number!";
			        		}
	        			}
		        		return null;
		        	}
		        });
		    }
		    else if (value instanceof Double) {
		        propertyDescriptor = new TextPropertyDescriptor(name, displayName);
		        propertyDescriptor.setValidator(new ICellEditorValidator() {
		        	public String isValid(Object value) {
		        		if (((String)value).indexOf("${")== -1)
	        			{
			        		try {
			            		Double.valueOf((String)value);
			            		return null;
			        		}
			        		catch(NumberFormatException e) {
			            		return "The value \"" + value + "\" is not a valid double number!";
			        		}
	        			}
		        		return null;
		        	}
		        });
		    }
        }
    	// Complex types
        else {
        	if (PropertyWithTagsEditor.class.isAssignableFrom(pec)) {	    		
    			String[] tags;
    			if (PropertyWithDynamicTagsEditor.class.isAssignableFrom(pec)){
    				Method getTags = pec.getMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
    				tags = (String[]) getTags.invoke(null, new Object[] { this, name } );
    				propertyDescriptor = new DynamicComboBoxPropertyDescriptor(name, displayName, getTags, this, name);
    			} else if (PropertyWithTagsEditorAdvance.class.isAssignableFrom(pec)){
    				Method getTags = pec.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
    				tags = (String[]) getTags.invoke(null, new Object[] { this, name } );
    				propertyDescriptor = new ComboBoxPropertyDescriptor(name, displayName, tags);
    			} else {
    				Method getTags = pec.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class});
    				tags = (String[]) getTags.invoke(null, new Object[] { this } );
    				propertyDescriptor = new ComboBoxPropertyDescriptor(name, displayName, tags);
    			}
   	        }
	        else if (AbstractDialogCellEditor.class.isAssignableFrom(pec)) {
	        	final DatabaseObjectTreeObject dbotoThis = this;
				propertyDescriptor = new PropertyDescriptor(name, displayName) {
					@Override
				    public CellEditor createPropertyEditor(Composite parent) {
				    	try {
					    	Constructor<?> constructor = pec.getConstructor(new Class[] { Composite.class });
					    	AbstractDialogCellEditor editor = (AbstractDialogCellEditor) constructor.newInstance(new Object[] { parent });
					    	editor.propertyDescriptor = this;
					    	editor.databaseObjectTreeObject = dbotoThis;
					        if (getValidator() != null)
					            editor.setValidator(getValidator());
	
					        return editor;
				    	}
				    	catch(Exception e) {
							ConvertigoPlugin.logException(e, "Unexpected exception");
				    		return null;
				    	}
				    }
				};
	        }
        }

        // Special cases
        if (propertyDescriptor == null) {
        	// editor for scriptable properties
        	Object scriptable = databaseObjectPropertyDescriptor.getValue("scriptable");
        	if ((scriptable != null) && (scriptable.equals(Boolean.TRUE)))
        		propertyDescriptor = new ScriptablePropertyDescriptor(name, displayName);
        	
        	// editor for nillable properties
        	if (propertyDescriptor == null) {
        		Object nillable = databaseObjectPropertyDescriptor.getValue("nillable");
            	if ((nillable != null) && (nillable.equals(Boolean.TRUE))) {
            		int style = isMasked ? SWT.PASSWORD:SWT.NONE;
            		if (value instanceof String) {
            			propertyDescriptor = new DataOrNullPropertyDescriptor(name, displayName, StringOrNullEditor.class, style);
            		}
            		else if (value instanceof XMLVector) {
            			propertyDescriptor = new DataOrNullPropertyDescriptor(name, displayName, ArrayOrNullEditor.class, style);
            		}
            	}
        	}
        	
        	// editor for disabled properties
        	Object disable = databaseObjectPropertyDescriptor.getValue("disable");
        	if ((disable != null) && (disable.equals(Boolean.TRUE)))
        		propertyDescriptor = new InfoPropertyDescriptor(name, displayName);
        	
        }

    	// Default case
    	if (propertyDescriptor == null)
    		propertyDescriptor = new TextPropertyDescriptor(name, displayName);
        
    	if (propertyDescriptor != null) {
            String beanDescription =  databaseObjectPropertyDescriptor.getShortDescription();
            String[] beanDescriptions = beanDescription.split("\\|");
            String beanShortDescription = beanDescriptions[0];

            if (isExtractionRule)
				propertyDescriptor.setCategory(databaseObjectPropertyDescriptor.isExpert() ? "Selection" : "Configuration");
			else
				propertyDescriptor.setCategory(databaseObjectPropertyDescriptor.isExpert() ? "Expert" : "Base properties");
            
            beanShortDescription = cleanDescription(beanShortDescription);
            propertyDescriptor.setDescription(beanShortDescription);
    	}
        return propertyDescriptor;
    }
    
    private String cleanDescription(String description) {
		String cleanDescription = description;
		// Replace first space
		if (cleanDescription.charAt(0) == ' ') cleanDescription = cleanDescription.substring(1);
		// Replace all {{...}}}
		cleanDescription = cleanDescription.replaceAll("(\\{\\{[^\\{\\}]*\\}\\})", "");
		// Replace all \s\n
		cleanDescription = cleanDescription.replaceAll("(\\s\\\n)", "\n\n");
		// Replace all \n\s
		cleanDescription = cleanDescription.replaceAll("(\\\n\\s)", "\n\n");
		// Then replace all ***
		cleanDescription = cleanDescription.replaceAll("(\\*+)", "\n * ");
		return cleanDescription;
	}

	private PropertyDescriptor findPropertyDescriptor(Object id) {
		PropertyDescriptor propertyDescriptor;
		if (propertyDescriptors != null) {
			for (int i=0;i<propertyDescriptors.length;i++) {
				propertyDescriptor = (PropertyDescriptor)propertyDescriptors[i];
				if (propertyDescriptor != null) {
					if (propertyDescriptor.getId().equals(id))
						return propertyDescriptor;
				}
			}
		}
		return null;
	}
	
    @SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
//		if (adapter.equals(IContentOutlinePage.class)) {
//			if (userContentOutline == null) {
//				//Create a property outline page using the parsed result of
//				// passing in the document provider.
//				userContentOutline = new PropertySheetContentOutlinePage(
//						new UserFileParser().parse(getDocumentProvider()));
//			}
//			return userContentOutline;
//		}
		if (adapter.equals(IPropertySheetPage.class)) {
			PropertySheetPage propertySheetPage = new PropertySheetPage();
			return propertySheetPage;
		}
		return super.getAdapter(adapter);
	}

	public boolean hasChanged() {
        return getObject().hasChanged;
    }
    
	@Override
	public String getName() {
		return getObject().getName();
	}
	
    public void markAsChanged(boolean hasChanged) {
    	getObject().hasChanged = hasChanged;
    }
    
    public Object getEditableValue() {
		return getObject();
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		return propertyDescriptors;
	}

	public Object getPropertyValue(Object id) {
		if (id == null) return null;

		DatabaseObject databaseObject = getObject();
		String propertyName = (String) id;
		
		if (propertyName.equals(P_TYPE))
			return databaseObjectBeanDescriptor.getDisplayName();
		else if (propertyName.equals(P_JAVA_CLASS))
			return databaseObject.getClass().getName();
		else if (propertyName.equals(P_NAME))
			return databaseObject.getName();
		else if (propertyName.equals(P_QNAME))
			return databaseObject.getQName();
		else if (propertyName.equals(P_PRIORITY))
			return Long.toString(databaseObject.priority);
		else if (propertyName.equals(P_DEPTH))
	        if (databaseObject instanceof ScreenClass) return Integer.toString(((ScreenClass) databaseObject).getDepth());
			else return "n/a";
		else {
			try {
				java.beans.PropertyDescriptor databaseObjectPropertyDescriptor = getPropertyDescriptor(propertyName);
	            if (databaseObjectPropertyDescriptor == null) return null;

	            Class<?> pec = databaseObjectPropertyDescriptor.getPropertyEditorClass();
				
	            Method getter = databaseObjectPropertyDescriptor.getReadMethod();
				
	            Object compilablePropertySourceValue = databaseObject.getCompilablePropertySourceValue(propertyName);
	            Object value;
	            if (compilablePropertySourceValue == null) {
		            Object args[] = { };
		            value = getter.invoke(databaseObject, args);
	            }
	            else {
	            	value = compilablePropertySourceValue;
	            }
	            
	            if (value instanceof Boolean) {
	            	value = ((Boolean) value).booleanValue() ? new Integer(0) : new Integer(1); 
	            }
	            else if ((pec != null) && PropertyWithTagsEditor.class.isAssignableFrom(pec)) {
	        		if (PropertyWithTagsEditorAdvance.class.isAssignableFrom(pec)) {      			
	        			Method getTags = pec.getMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
	        			String[] tags = (String[]) getTags.invoke(null, new Object[] { this, propertyName } );
	        			
		        		int i;
		        		for (i = 0 ; i < tags.length ; i++) {
	        				if (tags[i].equals(value)) {
	        					value = new Integer(i);
	        					break;
	        				}
	        			}
		        		
		        		// if we did not find our string in the tag list set value to index 0
		        		if (i == tags.length) {
		        			value = new Integer(0);
		                    String message = "Incorrect property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\".";
		                    ConvertigoPlugin.logWarning(message);
		        		}
	        		}
	        		if ((EmulatorTechnologyEditor.class.equals(pec))) {
	            		Method getEmulatorClassNames = pec.getDeclaredMethod("getEmulatorClassNames", new Class[] { DatabaseObjectTreeObject.class });
	            		String[] emulatorClassNames = (String[]) getEmulatorClassNames.invoke(null, new Object[] { this } );
	        			
		        		for (int i = 0 ; i < emulatorClassNames.length ; i++) {
	        				if (emulatorClassNames[i].equals(value)) {
	        					value = new Integer(i);
	        					break;
	        				}
	        			}
	        		}
	        		// else simply return the combo index
	            }
	            else if (value instanceof Number) {
	            	value = ((Number) value).toString(); 
	            }
	            
	            // Get property's nillable value
	            if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue("nillable"))) {
	            	try {
		            	Boolean isNull = ((INillableProperty)databaseObject).isNullProperty(propertyName);
		            	PropertyDescriptor pd = findPropertyDescriptor(propertyName);
		            	if ((pd != null) && (pd instanceof DataOrNullPropertyDescriptor)) {
		            		((DataOrNullPropertyDescriptor)pd).setNullProperty(isNull);
			            	if (isNull) {
			            		// Overrides value by fake one used by property editor
			            		if (value instanceof String)
			            			value = "null";
			            		if (value instanceof XMLVector) {
			            			XMLVector<Object> xmlv = new XMLVector<Object>();
			            			xmlv.addElement("null");
			            			value = xmlv;
			            		}
			            	}
		            	}
	            	}
	            	catch (Exception e) {
	                    String message = "Error while trying to retrieve 'isNull' attribute of property \"" + propertyName + "\" for the object \"" + databaseObject.getName() + "\".";
	                    ConvertigoPlugin.logException(e, message);
	            	}
	            }
	            
				// Check for property normalized value if needed
				if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue("normalizable"))) {
	        		if (value instanceof String) {
	        			String normalizedValue = StringUtils.normalize(value.toString());
	        			if (!value.equals(normalizedValue)) {
		                    String message = "Property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\" isn't normalized.";
		                    ConvertigoPlugin.logError(message, Boolean.TRUE);
	        			}
	        		}
				}
	            
	            return value;
            }
            catch (Exception e) {
                String message = "Error while trying to retrieve property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\".";
                ConvertigoPlugin.logException(e, message);
                return null;
            }
		}
	}

	public boolean isPropertySet(Object id) {
		return false;
	}

	public void resetPropertyValue(Object id) {
	}

	public void setPropertyValue(Object id, Object value) {
		DatabaseObject databaseObject = getObject();
		Object oldValue = getPropertyValue(id);
		String propertyName = (String) id;
		try {
			java.beans.PropertyDescriptor databaseObjectPropertyDescriptor = getPropertyDescriptor(propertyName);

			if (databaseObjectPropertyDescriptor == null) return;

			Class<?> propertyClass = databaseObjectPropertyDescriptor.getPropertyType();
			Class<?> pec = databaseObjectPropertyDescriptor.getPropertyEditorClass();
			
    		if (PropertyWithTagsEditorAdvance.class.isAssignableFrom(pec)) {
        			Method getTags = pec.getMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
        			String[] tags = (String[]) getTags.invoke(null, new Object[] { this, propertyName } );
            		
            		try {
            			value = tags[((Integer) value).intValue()];
            		}
            		catch (ArrayIndexOutOfBoundsException e) {
            			value = "";
	                    String message = "Incorrect property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\".";
	                    ConvertigoPlugin.logWarning(message);
            		}
            }
    		if ((EmulatorTechnologyEditor.class.equals(pec))) {
        		Method getEmulatorClassNames = pec.getDeclaredMethod("getEmulatorClassNames", new Class[] { DatabaseObjectTreeObject.class });
        		String[] emulatorClassNames = (String[]) getEmulatorClassNames.invoke(null, new Object[] { this } );

        		value = emulatorClassNames[((Integer) value).intValue()];
            }
    		
    		
			if ((propertyClass == boolean.class) || (propertyClass == Boolean.class)) {
            	if (((Integer) value).intValue() == 0) value = Boolean.TRUE;
            	else value = Boolean.FALSE;
			}
			else
			{
				// Retrieve compiled value or remove source value if any
				if (value.toString().indexOf("${") == -1)
					databaseObject.removeCompilablePropertySourceValue(propertyName);
				else
					value = DatabaseObject.compileProperty(databaseObject, propertyName, value);
				
				if ((propertyClass == int.class) || (propertyClass == Integer.class)) {
					if (!(value instanceof Integer))	value = new Integer(value.toString());
				}
				else if ((propertyClass == double.class) || (propertyClass == Double.class)) {
					value = new Double(value.toString());
				}
				else if ((propertyClass == byte.class) || (propertyClass == Byte.class)){ 
					value = new Byte(value.toString());
				}
				else if ((propertyClass == short.class) || (propertyClass == Short.class)){ 
					value = new Short(value.toString());
				}
				else if ((propertyClass == long.class) || (propertyClass == Long.class)) {
					value = new Long(value.toString());
				}
				else if ((propertyClass == float.class) || (propertyClass == Float.class)) {
					value = new Float(value.toString());
				}
				else if ((propertyClass == double.class) || (propertyClass == Double.class)) {
					value = new Double(value.toString());
				}
			}
			
			// Must rename bean when normalizedScreenClassName changed
			if (databaseObject instanceof ScHandlerStatement) {
				ScHandlerStatement scHandlerStatement = (ScHandlerStatement)databaseObject;
				if (propertyName.equals("normalizedScreenClassName")) {
					if (!this.rename("on"+ (String)value+ scHandlerStatement.getHandlerType(), Boolean.FALSE))
						return;
				}
			}
			// Must rename bean when handlerType changed
			else if ((databaseObject instanceof HandlerStatement) && !(databaseObject instanceof ScDefaultHandlerStatement)) {
				//HandlerStatement handlerStatement = (HandlerStatement)databaseObject;
				if (propertyName.equals("handlerType")) {
					if (!this.rename("on"+ (String)value, Boolean.FALSE))
						return;
				}
			}
			
            // Set property's nillable value
			if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue("nillable"))) {
            	try {
            		PropertyDescriptor pd = findPropertyDescriptor(propertyName);
            		if ((pd != null) && (pd instanceof DataOrNullPropertyDescriptor)) {
            			Boolean isNull = ((DataOrNullPropertyDescriptor)pd).isNullProperty();
		            	((INillableProperty)databaseObject).setNullProperty(propertyName, isNull);
		            	if (isNull) {
		            		// Overrides fake editor value to real bean's one
		            		if (value instanceof String)
		            			value = "";
		            		if (value instanceof XMLVector)
		            			value = new XMLVector<Object>();
		            	}
            		}
            	}
            	catch (Exception e) {
                    String message = "Error while trying to set 'isNull' attribute of property \"" + propertyName + "\" for the object \"" + databaseObject.getName() + "\".";
                    ConvertigoPlugin.logException(e, message);
            	}
            }
			
			// Normalize property value if needed
			if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue("normalizable"))) {
        		if (value instanceof String) {
        			String normalizedValue = StringUtils.normalize(value.toString());
        			if (!value.equals(normalizedValue)) {
        				value = normalizedValue;
	                    String message = "Property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\" has been normalized.";
	                    ConvertigoPlugin.logWarning(message);
        			}
        		}
			}
			
	        Method setter = databaseObjectPropertyDescriptor.getWriteMethod();
			
            Object args[] = { value };
	        setter.invoke(databaseObject, args);
	        
	        databaseObject.hasChanged = true;
	        hasBeenModified(true);
	      
			// Set treeObject isEnabled attribute value (Fix #1129)
			if (propertyName.equals("isEnabled") || propertyName.equals("isEnable")) {
				setEnabled(value.equals(true));
			}
			
	        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
        	viewer.update(this, null);
	        viewer.setSelection(viewer.getSelection(),true);
	      
	       //update property view and display the new value for zone editor
	        if (pec !=null)
	        {
		        PropertySheet propertySheet = ConvertigoPlugin.getDefault().getPropertiesView();
		        if (propertySheet != null && pec.getName().contains("ZoneEditor")) {
		        	Tree tree = (Tree) propertySheet.getCurrentPage().getControl();
		        	TreeItem[] treeItems = tree.getSelection();
		        	for (int i = 0; i < treeItems.length; i++) {
		        		TreeItem treeItem = treeItems[i];
		        		if (treeItem.getText().equals(databaseObjectPropertyDescriptor.getDisplayName())) {
		        			PropertySheetEntry propEntry=(PropertySheetEntry)treeItem.getData();
		        			propEntry.getEditor(tree).setValue(value);
		        			propEntry.setValues(args);
		        		}
		        	}
		        	
		        	tree.update();
		        }	        
	        }
	        TreeObjectEvent treeObjectEvent = new TreeObjectEvent(this, propertyName, oldValue, value);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
        }
        catch (Exception e) {
            String message = "Error while trying to set property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\".";
            ConvertigoPlugin.logException(e, message);
        }
	}
	
	protected java.beans.PropertyDescriptor getPropertyDescriptor(String propertyName) {
		int len = databaseObjectPropertyDescriptors.length;

		java.beans.PropertyDescriptor databaseObjectPropertyDescriptor = null;
		for (int i = 0; i < len; i++) {
			databaseObjectPropertyDescriptor = databaseObjectPropertyDescriptors[i];
			if (propertyName.equals(databaseObjectPropertyDescriptor.getName()))
				return databaseObjectPropertyDescriptor;
		}

		return null;
	}

	public boolean rename(String newName, Boolean bDialog) {
        try {
        	if(!StringUtils.isNormalized(newName)) throw new ConvertigoException("The name \"" + newName + "\" must be normalized.\nDon't start with number and don't use non ASCII caracters.");
        	
			DatabaseObject databaseObject = (DatabaseObject)getObject();
			String oldName = databaseObject.getName();
			
			if (newName.equals(oldName))
				return true;
			
            if (databaseObject instanceof Project) {
            	Project project = (Project) databaseObject;
            	
                // First verify if an object with the same name exists
            	if (Engine.theApp.databaseObjectsManager.existsProject(newName)) {
					throw new ConvertigoException("The project \"" + newName + "\" already exist!");
            	}
            	
            	// save only objects which have changed
           		ConvertigoPlugin.projectManager.save(project, false);
            	
            	// Set new name and save
				project.setName(newName);
				project.hasChanged = true;
				ConvertigoPlugin.projectManager.save(project, false);

				// Export project to xml file
				String exportedProjectFileName = Engine.PROJECTS_PATH + "/" + newName + "/" + newName + ".xml";
				CarUtils.exportProject(project, exportedProjectFileName);
				
				// Rename old .xsd file
				try {
					ProjectUtils.renameXsdFile(Engine.PROJECTS_PATH, oldName, newName);
				} catch (Exception e) {
					throw new ConvertigoException(e.getMessage());
				}
				
				// Rename old .wsdl file
				try {
					ProjectUtils.renameWsdlFile(Engine.PROJECTS_PATH, oldName, newName);
				} catch (Exception e) {
					throw new ConvertigoException(e.getMessage());
				}

				// Delete old .temp.xsd file
				File xsdTemp = new File(Engine.PROJECTS_PATH + "/" + newName + "/" + oldName + ".temp.xsd");
		        if (xsdTemp.exists() && !xsdTemp.delete()) {
					throw new ConvertigoException("Unable to delete the xsd file \"" + oldName + ".temp.xsd\".");
				}
				
				// Delete old .temp.wsdl file
				File wsdlTemp = new File(Engine.PROJECTS_PATH + "/" + newName + "/" + oldName + ".temp.wsdl");
		        if (wsdlTemp.exists() && !wsdlTemp.delete()) {
					throw new ConvertigoException("Unable to delete the wsdl file \"" + oldName + ".temp.wsdl\".");
				}
				
				// Delete the old .xml file
		        String xmlFilePath = Engine.PROJECTS_PATH + "/" + newName + "/" + oldName + ".xml";
		        File xmlFile = new File(xmlFilePath);
		        if (!xmlFile.exists()) {
		        	throw new ConvertigoException("The xml file \"" + oldName + ".xml\" doesn't exist.");
		        }
		        if (!xmlFile.canWrite()) {
		    		throw new ConvertigoException("Unable to access the xml file \"" + oldName + ".xml\".");
		        }
		        if (!xmlFile.delete()) {
					throw new ConvertigoException("Unable to delete the xml file \"" + oldName + ".xml\".");
				}
				
		        // Delete .project file
		        String ressourcePath = Engine.PROJECTS_PATH + "/" + newName + "/.project";
		        File ressourceFile = new File(ressourcePath);
		        ressourceFile.delete();
		        
				// delete old resources plugin
				ConvertigoPlugin.getDefault().deleteProjectPluginResource(oldName);
				// create new resources plugin
				ConvertigoPlugin.getDefault().createProjectPluginResource(newName);
            }
            else {
				// Verify if an object with the same name exists
				DatabaseObjectTreeObject siblingDatabaseObjectTreeObject;
				TreeObject siblingTreeObject;
				String databaseObjectName;
				Object object;
				DatabaseObject databaseObjectTmp;

				siblingTreeObject = this;
				while ((siblingTreeObject = siblingTreeObject.getPreviousSibling()) != null) {
					if (siblingTreeObject instanceof DatabaseObjectTreeObject) {
						siblingDatabaseObjectTreeObject = (DatabaseObjectTreeObject)siblingTreeObject;
						if (!siblingDatabaseObjectTreeObject.isInherited) {
							object = siblingDatabaseObjectTreeObject.getObject();
							if (object instanceof DatabaseObject) {
								databaseObjectTmp = (DatabaseObject) object;
								databaseObjectName = databaseObjectTmp.getName();
								if (databaseObjectName.equals(newName)) {
									throw new ConvertigoException("Another object with the same name already exists.");
								}
							}
						}
					}
				}
            
				siblingTreeObject = this;
				while ((siblingTreeObject = siblingTreeObject.getNextSibling()) != null) {
					if (siblingTreeObject instanceof DatabaseObjectTreeObject) {
						siblingDatabaseObjectTreeObject = (DatabaseObjectTreeObject)siblingTreeObject;
						if (!siblingDatabaseObjectTreeObject.isInherited) {
							object = siblingDatabaseObjectTreeObject.getObject();
							if (object instanceof DatabaseObject) {
								databaseObjectTmp = (DatabaseObject) siblingDatabaseObjectTreeObject.getObject();
								databaseObjectName = databaseObjectTmp.getName();
								if (databaseObjectName.equals(newName)) {
									throw new ConvertigoException("Another object with the same name already exists.");
								}
							}
						}
					}
				}
				
				databaseObject.setName(newName);
				databaseObject.hasChanged = true;
				hasBeenModified(true);
            }
        }
        catch(ConvertigoException e) {
            ConvertigoPlugin.logException(e, "Unable to change the object name.", bDialog);
            return false;
        } catch (CoreException e) {
        	ConvertigoPlugin.logException(e, "Unable to change the object name.", bDialog);
        	return false;
		}
        return true;
    }
	
	@Override
	public ProjectTreeObject getProjectTreeObject() {
		ProjectTreeObject projectTreeObject = null;
		
		if (this instanceof ProjectTreeObject) {
			return (ProjectTreeObject)this;
		}
		
		TreeParent treeParent = parent;
		while (treeParent != null) {
			if (treeParent instanceof ProjectTreeObject) {
				projectTreeObject = (ProjectTreeObject)treeParent;
				break;
			}
			treeParent = treeParent.getParent();
		}
		
		return projectTreeObject;
	}
	
	public DatabaseObjectTreeObject getParentDatabaseObjectTreeObject() {
		DatabaseObjectTreeObject databaseObjectTreeObject = null;
		
		TreeParent treeParent = parent;
		while ((treeParent != null) && (!(treeParent instanceof DatabaseObjectTreeObject)))
			treeParent = treeParent.getParent();
		
		if (treeParent != null)
			databaseObjectTreeObject = (DatabaseObjectTreeObject)treeParent;
		
		return databaseObjectTreeObject;
	}
	
	@Override
	public TreeObject findTreeObjectByUserObject(Object databaseObject) {
		TreeObject treeObject = null;
		if (databaseObject != null) {
			if ((!isInherited) && (getObject().equals(databaseObject))) {
				treeObject = this;
			}
			else
				treeObject = super.findTreeObjectByUserObject(databaseObject); 
		}
		return treeObject;
	}

	@Override
	public TreeObject findTreeObjectByUserObjectQName(String databaseObjectQName) {
		if (databaseObjectQName != null && (!isInherited) && (getObject().getQName().equals(databaseObjectQName)))
			return this;
		return null;
	}
	
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isStatementWithExpressions")) {
			DatabaseObject databaseObject = getObject();
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(databaseObject instanceof StatementWithExpressions));
		}
		if (name.equals("isStepWithExpressions")) {
			DatabaseObject databaseObject = getObject();
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(databaseObject instanceof StepWithExpressions));
		}		
		if (name.equals("objectClassName")) {
			DatabaseObject databaseObject = getObject();
			objectClassName = databaseObject.getClass().getName();
			return objectClassName.equals(value);
		}
		if (name.equals("isInherited")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isInherited));
		}
		if (name.equals("isModified")) {
			isModified = hasChanged();
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isModified));
		}
		if (name.equals("isDefault")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isDefault));
		}
		if (name.equals("canPaste")) {
			canPaste = ((ConvertigoPlugin.clipboardManager2.isCopy) || (ConvertigoPlugin.clipboardManager2.isCut));
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(canPaste));
		}
		if (name.equals("isUnderCvs")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isUnderCvs));
		}
		if (name.equals("isCheckedOut")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isCheckedOut));
		}
		return super.testAttribute(target, name, value);
	}

	public String getImageName() {
		//Check the status to the object to display the right image.children at this point aren't updated
		String imageName = getObject().getClass().getName();
		imageName += (!isEnabled() ? "_disabled": hasAncestorDisabled()? "_unreachable":"");
		imageName += (isInherited ? "_inherited":"");
		imageName += (isDetectedObject ? "_detected":"");
		return imageName;
	}
	
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		if (!(treeObject.equals(this)))
			getDescriptors();// refresh editors (e.g labels in combobox)
	}

	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (!(treeObject.equals(this)) && (!getParents().contains(treeObject)))
			getDescriptors();// refresh editors (e.g labels in combobox)
	}

	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		getDescriptors();// refresh editors (e.g labels in combobox)
	}
	
	public boolean hasAncestorDisabled(){
		DatabaseObjectTreeObject parent = getParentDatabaseObjectTreeObject();
		return parent!=null && ( !parent.isEnabled() || parent.hasAncestorDisabled());
	}
}
