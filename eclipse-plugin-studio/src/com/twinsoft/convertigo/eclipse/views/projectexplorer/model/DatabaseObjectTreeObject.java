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

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
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
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScDefaultHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.property_editors.AbstractDialogCellEditor;
import com.twinsoft.convertigo.eclipse.property_editors.ArrayOrNullEditor;
import com.twinsoft.convertigo.eclipse.property_editors.DataOrNullPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.DataPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.DynamicComboBoxPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.DynamicInfoPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.EmulatorTechnologyEditor;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithDynamicInfoEditor;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithDynamicTagsEditor;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithTagsEditor;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithTagsEditorAdvance;
import com.twinsoft.convertigo.eclipse.property_editors.PropertyWithValidatorEditor;
import com.twinsoft.convertigo.eclipse.property_editors.SmartTypeCellEditor;
import com.twinsoft.convertigo.eclipse.property_editors.StringComboBoxPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.StringOrNullEditor;
import com.twinsoft.convertigo.eclipse.property_editors.TextGenericCellEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.InfoPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.UndefinedSymbolsException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.EnumUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;


public class DatabaseObjectTreeObject extends TreeParent implements TreeObjectListener, IPropertySource, IActionFilter {

	public static final String P_TYPE = "#type";
	public static final String P_JAVA_CLASS = "#java_class";
	public static final String P_DEPTH = "#depth";
	public static final String P_PRIORITY = "#priority";
	public static final String P_QNAME = "#qname";
	public static final String P_NAME = "#name";
	public static final String P_EXPORTED = "#exported";
	public static final String P_MIN_VERSION = "#minversion";

	public String objectClassName = null;
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
	
	private boolean isValueInProcess = false;
	
	public boolean isEditingComment = false;
	
	public DatabaseObjectTreeObject(Viewer viewer, DatabaseObject object) {
		this(viewer,object,false);
	}

	public DatabaseObjectTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object);
		isInherited = inherited;
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
			
			Object object = getObject();
			if (object instanceof DatabaseObject) {
				if (((DatabaseObject)object).bNew) {
					// Notify here new bean has been added (Fix ticket #20)
					//ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectAdded(new TreeObjectEvent(this));
					
					// Fix issue on notification while reloading (Ticket #2496)
					// Notification is done after reload is complete (see ProjectExplorerView:ReloadWithProgress)
					ConvertigoPlugin.projectManager.getProjectExplorerView().addedTreeObjects.add(this);
				}
			}
		}
	}

	
	public boolean acceptSymbols() {
		return true;
	}
	
	@Override
	public void update() {
		if (isInherited) {
			return;
		}
		
		hasBeenModified(hasChanged());
	}

	@Override
	protected void remove() {
		if (isInherited) {
			return;
		}
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	public void hasBeenModified(boolean bModified) {
		if (bModified && !isInherited) {
			markAsChanged(true);
			ProjectTreeObject projectTree = getProjectTreeObject();
			if (projectTree != null) {
				projectTree.hasBeenModified(true);
			}
		}
	}
	
	public DatabaseObjectTreeObject getOwnerDatabaseObjectTreeObject() {
		TreeObject owner = getParent();
		while (owner!=null && !(owner instanceof DatabaseObjectTreeObject)) {
			owner = owner.getParent();
		}
		return (DatabaseObjectTreeObject) owner;
	}
	
	public BeanInfo databaseObjectBeanInfo = null;

	private IPropertyDescriptor[] propertyDescriptors = null;
	private BeanDescriptor databaseObjectBeanDescriptor = null;
	private java.beans.PropertyDescriptor[] databaseObjectPropertyDescriptors;

	protected void reloadDescriptors() {
		propertyDescriptors = null;
		getDescriptors();
	}
	
	protected List<PropertyDescriptor> getDynamicPropertyDescriptors() {
		return new ArrayList<PropertyDescriptor>();
	}
	
	
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
			BeanInfo bi = databaseObjectBeanInfo = CachedIntrospector.getBeanInfo(databaseObject.getClass());
			databaseObjectBeanDescriptor = bi.getBeanDescriptor();
			databaseObjectPropertyDescriptors = bi.getPropertyDescriptors();
			len = databaseObjectPropertyDescriptors.length;
		}
		catch (Exception e) {
			String message = "Error while introspecting object " + databaseObject.getName() + " (" + databaseObject.getQName() + ")"; 
			ConvertigoPlugin.logException(e, message);
			return;
		}
		
		List<PropertyDescriptor> vPropertyDescriptors = new ArrayList<PropertyDescriptor>(32);
		
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
		
		propertyDescriptor = new PropertyDescriptor(P_EXPORTED, "Exported");
		propertyDescriptor.setCategory("Information");
		vPropertyDescriptors.add(propertyDescriptor);
		
		propertyDescriptor = new PropertyDescriptor(P_MIN_VERSION, "Min version");
		propertyDescriptor.setCategory("Information");
		vPropertyDescriptors.add(propertyDescriptor);
		
		// Get Dynamic properties
		List<PropertyDescriptor> dynamicPropertyDescriptors = getDynamicPropertyDescriptors();
		for (PropertyDescriptor dynamicPropertyDescriptor : dynamicPropertyDescriptors) {
			vPropertyDescriptors.add(dynamicPropertyDescriptor);
		}
		
		// Get properties
		boolean isExtractionRule = ( databaseObject instanceof com.twinsoft.convertigo.beans.core.ExtractionRule) ;
		boolean isMaskedProperty = false;
		
		for (int i = 0 ; i < len ; i++) {
			databaseObjectPropertyDescriptor = databaseObjectPropertyDescriptors[i];
			
			// Don't display hidden or expert propertyDescriptors.
			if (databaseObjectPropertyDescriptor.isHidden()) {
				continue;
			}
			
			if (databaseObject.checkBlackListParentClass(databaseObjectPropertyDescriptor)) {
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
	
	// A default validator which accept any value
	/*protected ICellEditorValidator getValidator(String propertyName) {
		return new ICellEditorValidator() {
			public String isValid(Object value) {
				return null;
			}
		};
	}*/
	protected ICellEditorValidator getValidator(String propertyName) {
		return new ICellEditorValidator() {
			@Override
			public String isValid(Object value) {
				if (!acceptSymbols() && isSymbolValue(value)) {
					return "Symbols are not allowed for this component";
				}
				return null;
			}
		};
	}
	
	protected boolean isSymbolValue(Object value) {
		if (value != null) {
			String val = null;
			if (value instanceof String) {
				val = String.valueOf(value);
			} else if (value instanceof com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType) {
				val = ((com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType)value).getValue();
			} else if (value instanceof com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType) {
				val = ((com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType)value).getValue();
			}
			if (val != null) {
				Pattern pattern = Pattern.compile("\\$\\{(.*)\\}");
				Matcher matcher = pattern.matcher(val);
				return matcher.find();
			}
		}
		return false;
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
				propertyDescriptor = new DynamicComboBoxPropertyDescriptor(name, displayName, values, this, name);
			}
			else if (value instanceof Number) {
				propertyDescriptor = new TextPropertyDescriptor(name, displayName);
//				propertyDescriptor.setValidator(new CompilableValueValidator(new NumberValidator(value.getClass())));
			}
		}
		// Complex types
		else {
			if (PropertyWithValidatorEditor.class.isAssignableFrom(pec)) {
				propertyDescriptor = new TextPropertyDescriptor(name, displayName);
				propertyDescriptor.setValidator(getValidator(name));
			}
			else if (PropertyWithTagsEditor.class.isAssignableFrom(pec)) {
				String[] tags;
				if (PropertyWithDynamicTagsEditor.class.isAssignableFrom(pec)){
					Method getTags = pec.getMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
					tags = (String[]) getTags.invoke(null, new Object[] { this, name } );
					propertyDescriptor = new DynamicComboBoxPropertyDescriptor(name, displayName, getTags, this, name);
				} else if (PropertyWithTagsEditorAdvance.class.isAssignableFrom(pec)){
					Method getTags = pec.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
					tags = (String[]) getTags.invoke(null, new Object[] { this, name } );
					propertyDescriptor = new DynamicComboBoxPropertyDescriptor(name, displayName, tags, this, name);
				} else {
					Method getTags = pec.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class});
					tags = (String[]) getTags.invoke(null, new Object[] { this } );
					propertyDescriptor = new DynamicComboBoxPropertyDescriptor(name, displayName, tags, this, name);
				}
			}
			else if (StringComboBoxPropertyDescriptor.class.isAssignableFrom(pec)) {
				Method getTags = pec.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
				String[] tags = (String[]) getTags.invoke(null, new Object[] { this, name } );
				boolean isReadOnly = false;
				try {
					Method method = getObject().getClass().getMethod("isReadOnlyProperty", new Class[] { String.class});
					isReadOnly = (boolean) method.invoke(getObject(), new Object[] { name });
				} catch (Exception e) {}
				propertyDescriptor = new StringComboBoxPropertyDescriptor(name, displayName, tags, isReadOnly);
			}
			else if (com.twinsoft.convertigo.eclipse.property_editors.MobileSmartSourcePropertyDescriptor.class.isAssignableFrom(pec)) {
				Method getTags = pec.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
				String[] tags = (String[]) getTags.invoke(null, new Object[] { this, name } );
				propertyDescriptor = new com.twinsoft.convertigo.eclipse.property_editors.MobileSmartSourcePropertyDescriptor(name, displayName, tags, false);
				((com.twinsoft.convertigo.eclipse.property_editors.MobileSmartSourcePropertyDescriptor)propertyDescriptor).databaseObjectTreeObject = this;
			}
			else if (com.twinsoft.convertigo.eclipse.property_editors.NgxSmartSourcePropertyDescriptor.class.isAssignableFrom(pec)) {
				Method getTags = pec.getDeclaredMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
				String[] tags = (String[]) getTags.invoke(null, new Object[] { this, name } );
				propertyDescriptor = new com.twinsoft.convertigo.eclipse.property_editors.NgxSmartSourcePropertyDescriptor(name, displayName, tags, false);
				((com.twinsoft.convertigo.eclipse.property_editors.NgxSmartSourcePropertyDescriptor)propertyDescriptor).databaseObjectTreeObject = this;
			}
			else if (PropertyWithDynamicInfoEditor.class.isAssignableFrom(pec)) {
				Method getInfo = pec.getMethod("getInfo", new Class[] { DatabaseObjectTreeObject.class, String.class });
				propertyDescriptor = new DynamicInfoPropertyDescriptor(name, displayName, getInfo, this, name);
			}
			else if (AbstractDialogCellEditor.class.isAssignableFrom(pec)) {
				propertyDescriptor = new PropertyDescriptor(name, displayName) {
					@Override
					public CellEditor createPropertyEditor(Composite parent) {
						try {
							Constructor<?> constructor = pec.getConstructor(new Class[] { Composite.class });
							AbstractDialogCellEditor editor = (AbstractDialogCellEditor) constructor.newInstance(new Object[] { parent });
							editor.propertyDescriptor = this;
							editor.databaseObjectTreeObject = DatabaseObjectTreeObject.this;
							if (getValidator() != null) {
								editor.setValidator(getValidator());
							}
	
							return editor;
						}
						catch(Exception e) {
							ConvertigoPlugin.logException(e, "Unexpected exception");
							return null;
						}
					}
				};
			}
			else if (Enum.class.isAssignableFrom(pec)) {
				String[] tags = EnumUtils.toStrings(pec);
				
				propertyDescriptor = new DynamicComboBoxPropertyDescriptor(name, displayName, tags, this, name);
			}
			else if (SmartTypeCellEditor.class.isAssignableFrom(pec)) {
				propertyDescriptor = new PropertyDescriptor(name, displayName) {
					@Override
					public CellEditor createPropertyEditor(Composite parent) {
						try {

							Constructor<?> constructor = pec.getConstructor(new Class[] {
								Composite.class, DatabaseObjectTreeObject.class, java.beans.PropertyDescriptor.class
							});
							SmartTypeCellEditor editor = (SmartTypeCellEditor) constructor.newInstance(new Object[] {
								parent, DatabaseObjectTreeObject.this, databaseObjectPropertyDescriptor
							});
							if (getValidator() != null) {
								editor.setValidator(getValidator());
							}
							return editor;
						} catch(Exception e) {
							ConvertigoPlugin.logException(e, "Unexpected exception");
							return null;
						}
					}
				};
			}
		}

		// Special cases
		if (propertyDescriptor == null) {
			boolean disable = Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue(MySimpleBeanInfo.DISABLE));
			
			if (disable) {
				propertyDescriptor = new InfoPropertyDescriptor(name, displayName);
			} else {
				boolean multiline = Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue(MySimpleBeanInfo.MULTILINE));
				int style = isMasked ? SWT.PASSWORD : multiline ? SWT.MULTI : SWT.NONE;
				boolean nillable = Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue(MySimpleBeanInfo.NILLABLE));
				if (nillable) {
					if (value instanceof String) {
						propertyDescriptor = new DataOrNullPropertyDescriptor(name, displayName, StringOrNullEditor.class, style, this, databaseObjectPropertyDescriptor);
					} else if (value instanceof XMLVector) {
						propertyDescriptor = new DataOrNullPropertyDescriptor(name, displayName, ArrayOrNullEditor.class, style, this, databaseObjectPropertyDescriptor);
					}
				} else if (multiline) {
					propertyDescriptor = new DataPropertyDescriptor(name, displayName, TextGenericCellEditor.class, style, this, databaseObjectPropertyDescriptor);
				}
			}
		}

		// Default case
		if (propertyDescriptor == null) {
			propertyDescriptor = new TextPropertyDescriptor(name, displayName);
//			propertyDescriptor.setValidator(new CompilableValueValidator(getValidator(name)));
		}
		
		if (propertyDescriptor != null) {
			ICellEditorValidator validator = getValidator(name);
			if (validator != null) {
				propertyDescriptor.setValidator(validator);
			}
			
			final ILabelProvider labelProvider = propertyDescriptor.getLabelProvider();
			propertyDescriptor.setLabelProvider(new ILabelProvider() {
				
				@Override
				public void removeListener(ILabelProviderListener listener) {
					labelProvider.removeListener(listener);
				}
				
				@Override
				public boolean isLabelProperty(Object element, String property) {
					return labelProvider.isLabelProperty(element, property);
				}
				
				@Override
				public void dispose() {
					labelProvider.dispose();
				}
				
				@Override
				public void addListener(ILabelProviderListener listener) {
					labelProvider.addListener(listener);
				}
				
				@Override
				public String getText(Object element) {
					String text = labelProvider.getText(element);
					try {
						String compiled = Engine.theApp.databaseObjectsManager.getCompiledValue(text);
						if (!text.equals(compiled)) {
							text += "  => " + compiled;
						}
					} catch (UndefinedSymbolsException e) {
						text += "  /!\\ undefined symbol /!\\";
					}
					return text;
				}
				
				@Override
				public Image getImage(Object element) {
					return labelProvider.getImage(element);
				}
			});
			String beanDescription = databaseObjectPropertyDescriptor.getShortDescription();
			int id = beanDescription.indexOf("|");
			if (id != -1) {
				beanDescription = beanDescription.substring(0, id);
			}

			if (isExtractionRule) {
				propertyDescriptor.setCategory(databaseObjectPropertyDescriptor.isExpert() ? "Selection" : "Configuration");
			} else {
				Object categoryValue = databaseObjectPropertyDescriptor.getValue(MySimpleBeanInfo.CATEGORY);
				String category = categoryValue == null ? "Base properties" : String.valueOf(categoryValue);

				propertyDescriptor.setCategory(databaseObjectPropertyDescriptor.isExpert() ? "Expert" : category);
			}

			beanDescription = cleanDescription(beanDescription);
			propertyDescriptor.setDescription(beanDescription);
		}
		return propertyDescriptor;
	}

	static final private Pattern pNoTag = Pattern.compile("</?\\w+?.*?>");
	static final private Map<String, String> cachedDescriptions = new HashMap<String, String>();

	protected String cleanDescription(String description) {
		String cleanDescription = cachedDescriptions.get(description);
		if (cleanDescription == null) {
			cleanDescription = description.replace("\n", " | ");
			cleanDescription = pNoTag.matcher(cleanDescription).replaceAll("");
			cleanDescription = cleanDescription.trim();
			cachedDescriptions.put(description, cleanDescription);
		}
		
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
		if (hasChanged) {
			getObject().changed();
		} else {
			getObject().hasChanged = false;
		}
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
		else if (propertyName.equals(P_DEPTH)) {
			if (databaseObject instanceof ScreenClass) return Integer.toString(((ScreenClass) databaseObject).getDepth());
			else return org.apache.commons.lang3.StringUtils.countMatches(databaseObject.getQName(), '.');
		}
		else if (propertyName.equals(P_EXPORTED)) {
			return databaseObject.getProject().getInfoForProperty("exported");
		}
		else if (propertyName.equals(P_MIN_VERSION)) {
			return databaseObject.getProject().getMinVersion();
		}
		else {
			try {
				java.beans.PropertyDescriptor databaseObjectPropertyDescriptor = getPropertyDescriptor(propertyName);
				if (databaseObjectPropertyDescriptor == null) {
					return null;
				}
				
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
				
				boolean done = false;
				
				if (value instanceof String) {
					PropertyDescriptor propertyDescriptor = findPropertyDescriptor(id);
					if (propertyDescriptor instanceof DynamicComboBoxPropertyDescriptor) {
						DynamicComboBoxPropertyDescriptor pd = (DynamicComboBoxPropertyDescriptor) propertyDescriptor;
						value = pd.setValue((String) value);
						done = true;
					}
				}
				
				if (done) {
					// do nothing
				} else if (value instanceof Boolean) {
					value = ((Boolean) value).booleanValue() ? Integer.valueOf(0) : Integer.valueOf(1); 
				}
				else if ((pec != null) && (PropertyWithTagsEditor.class.isAssignableFrom(pec) || Enum.class.isAssignableFrom(pec))) {
					if (!(value instanceof Integer)) {
						if (PropertyWithTagsEditorAdvance.class.isAssignableFrom(pec)) {
							Method getTags = pec.getMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
							String[] tags = (String[]) getTags.invoke(null, new Object[] { this, propertyName } );
	
							int i;
							for (i = 0 ; i < tags.length ; i++) {
								if (tags[i].equals(value)) {
									value = Integer.valueOf(i);
									break;
								}
							}
	
							// if we did not find our string in the tag list set value to index 0
							if (i == tags.length) {
								value = Integer.valueOf(0);
								String message = "Incorrect property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\".";
								ConvertigoPlugin.logWarning(message);
							}
						} else if (Enum.class.isAssignableFrom(pec)) {
							value = Integer.valueOf(((Enum<?>) value).ordinal());
						} else if (StringComboBoxPropertyDescriptor.class.isAssignableFrom(pec)) {
							// nothing to do: value is a string
						}
					}
					if ((EmulatorTechnologyEditor.class.equals(pec))) {
						Method getEmulatorClassNames = pec.getDeclaredMethod("getEmulatorClassNames", new Class[] { DatabaseObjectTreeObject.class });
						String[] emulatorClassNames = (String[]) getEmulatorClassNames.invoke(null, new Object[] { this } );
						
						for (int i = 0 ; i < emulatorClassNames.length ; i++) {
							if (emulatorClassNames[i].equals(value)) {
								value = Integer.valueOf(i);
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
				if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue(MySimpleBeanInfo.NILLABLE))) {
					try {
						Boolean isNull = ((INillableProperty)databaseObject).isNullProperty(propertyName);
						PropertyDescriptor pd = findPropertyDescriptor(propertyName);
						if ((pd != null) && (pd instanceof DataOrNullPropertyDescriptor)) {
							((DataOrNullPropertyDescriptor)pd).setNullProperty(isNull);
							if (isNull) {
								// Overrides value by fake one used by property editor
								if (value instanceof String)
									value = "<value is null>";
								if (value instanceof XMLVector) {
									XMLVector<Object> xmlv = new XMLVector<Object>();
									xmlv.add("null");
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
				if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue(DatabaseObject.PROPERTY_XMLNAME))) {
					// Ignore compilable property source value
					if (compilablePropertySourceValue == null) {
						if (value instanceof String) {
							if (!XMLUtils.checkName(value.toString())) {
								String message = "Property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\" isn't XML compliant.";
								ConvertigoPlugin.logError(message, Boolean.TRUE);
							}
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

	public void resetPropertyValue(Object id) {	}
	
	public void setPropertyValue(Object id, Object value) {
		MobileBuilder mb = null;
		
		IEditorPart editorPart = ConvertigoPlugin.getDefault().getApplicationComponentEditor();
		if (editorPart != null) {
			IEditorInput input = editorPart.getEditorInput();
			if (input instanceof com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput) {
				com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput editorInput = GenericUtils.cast(input);
				mb = editorInput.getApplication().getProject().getMobileBuilder();
			}
			if (input instanceof com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput) {
				com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput editorInput = GenericUtils.cast(input);
				mb = editorInput.getApplication().getProject().getMobileBuilder();
			}
		}
		
		DatabaseObject databaseObject = getObject();
		Object oldValue = getPropertyValue(id);
		String propertyName = (String) id;
		
		ComboBoxCellEditor editor = DynamicComboBoxPropertyDescriptor.getLast();
		if (editor != null && (!acceptSymbols() || !Integer.valueOf(editor.getItems().length - 1).equals(value))) {
			editor = null;
		}
		
		if (isValueInProcess || (oldValue != null && oldValue.equals(value) && editor == null)) {
			return;
		}
		
		try {
			isValueInProcess = true;
			java.beans.PropertyDescriptor databaseObjectPropertyDescriptor = getPropertyDescriptor(propertyName);
			TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);

			if (databaseObjectPropertyDescriptor == null) return;

			Class<?> propertyClass = databaseObjectPropertyDescriptor.getPropertyType();
			Class<?> pec = databaseObjectPropertyDescriptor.getPropertyEditorClass();
			
			if (editor != null) {
				Control control = editor.getControl();
				Display display = control.getDisplay(); 
				final Shell shell = new Shell(control.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS | SWT.APPLICATION_MODAL);
				shell.setLocation(control.toDisplay(0, 0));
				shell.setSize(control.getSize());
				shell.setLayout(new FillLayout());
				final Text text = new Text(shell, SWT.NONE);
				final String[] newValue = new String[] { null };
				String[] items = editor.getItems();
				text.setText(items[items.length - 1]);
				text.addTraverseListener(new TraverseListener() {					
					@Override
					public void keyTraversed(TraverseEvent e) {
						if (e.detail == SWT.TRAVERSE_RETURN) {
							newValue[0] = text.getText();
							shell.close();
						}
					}
				});
				shell.open();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch()) {
						display.sleep();
					}
				}
				
				if (newValue[0] != null) {
					value = newValue[0];
				}
			}

			Object oriValue = value;
			boolean changed;
			do {
				changed = false;
				boolean wasSymbolError = databaseObject.isSymbolError();
				value = databaseObject.compileProperty(propertyClass, propertyName, oriValue);
				
				try {
					oldValue = Engine.theApp.databaseObjectsManager.getCompiledValue(oldValue);
				} catch (UndefinedSymbolsException e) {
					oldValue = e.incompletValue();
				}
				
				Set<String> symbolsErrors = databaseObject.getSymbolsErrors(propertyName);
				if (symbolsErrors != null) {
					boolean[] res = ConvertigoPlugin.warningGlobalSymbols(databaseObject.getProject().getName(),
							databaseObject.getName(), databaseObject.getDatabaseType(),
							propertyName, "" + databaseObject.getCompilablePropertySourceValue(propertyName),
							symbolsErrors, false);
					changed = res[0];
					if (changed) {
						Engine.theApp.databaseObjectsManager.symbolsCreateUndefined(symbolsErrors);
					} else {
						databaseObject.getProject().undefinedGlobalSymbols = true;
						viewer.update(getProjectTreeObject(), null);
					}
				} else if (wasSymbolError) {
					Engine.theApp.databaseObjectsManager.symbolsProjectCheckUndefined(databaseObject.getProject().getName());
					viewer.update(getProjectTreeObject(), null);
				}
			} while (changed);
			
			if (editor != null && value instanceof String) {
				String[] items = editor.getItems();
				int len = items.length - 1;
				String strValue = (String) value;
				value = 0;
				for (int i = 0; i < len; i++) {
					if (items[i].equals(strValue)) {
						value = i;
						break;
					};
				}
				
			}
			
			if (pec != null && propertyClass != int.class && propertyClass != Integer.class && value instanceof Integer) {
				Object[] values = null;

				try {
					int index = (Integer) value;
					if (PropertyWithTagsEditorAdvance.class.isAssignableFrom(pec)) {
						Method getTags = pec.getMethod("getTags", new Class[] { DatabaseObjectTreeObject.class, String.class });
						values = (String[]) getTags.invoke(null, new Object[] { this, propertyName } );
						value = values[index];
					} else if (Enum.class.isAssignableFrom(pec)) {
						values = (Enum[]) pec.getMethod("values").invoke(null);
						value = index < values.length ? values[index] : values[0];
						if (propertyClass == String.class) {
							value = value.toString();
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					value = values.length > 0 ? values[0] : "";
					String message = "Incorrect property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\".";
					ConvertigoPlugin.logWarning(message);
				}
			}
			if ((EmulatorTechnologyEditor.class.equals(pec))) {
				Method getEmulatorClassNames = pec.getDeclaredMethod("getEmulatorClassNames", new Class[] { DatabaseObjectTreeObject.class });
				String[] emulatorClassNames = (String[]) getEmulatorClassNames.invoke(null, new Object[] { this } );

				value = emulatorClassNames[((Integer) value).intValue()];
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
			if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue(MySimpleBeanInfo.NILLABLE))) {
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
			
			// Check XML name property value if needed
			if (Boolean.TRUE.equals(databaseObjectPropertyDescriptor.getValue(DatabaseObject.PROPERTY_XMLNAME))) {
				if (value instanceof String) {
					String sValue = value.toString();
					if (!XMLUtils.checkName(sValue)) {
						String message = "The property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\" is not a valid XML name: " + sValue;
						ConvertigoPlugin.logWarning(message);
						return;
					}
				}
			}
			
			Method setter = databaseObjectPropertyDescriptor.getWriteMethod();
			
			Object args[] = { value };
			setter.invoke(databaseObject, args);
			
			hasBeenModified(true);
		  
			// Set treeObject isEnabled attribute value (Fix #1129)
			if (propertyName.equals("isEnabled") || propertyName.equals("isEnable")) {
				setEnabled(value.equals(true));
			}
			
			viewer.update(this, null);
			// Fix #2528 #2533 : commented next line because of stack overflow on multiselection
			//viewer.setSelection(viewer.getSelection(),true);
		  
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
			
			Engine.logStudio.info("---------------------- SetPropertyValue started: "+ propertyName + "----------------------");
			if (mb != null) {
				mb.prepareBatchBuild();
			}
			BatchOperationHelper.start();
			
			TreeObjectEvent treeObjectEvent = new TreeObjectEvent(this, propertyName, oldValue, value);
			ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent);
			
			BatchOperationHelper.stop();
		}
		catch (Exception e) {
			String message = "Error while trying to set property \"" + propertyName + "\" value for the object \"" + databaseObject.getName() + "\".";
			ConvertigoPlugin.logException(e, message);
		}
		finally {
			BatchOperationHelper.cancel();
			Engine.logStudio.info("---------------------- SetPropertyValue ended:   "+ propertyName + "----------------------");
			isValueInProcess = false;
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

	protected void rename_(String newName, boolean bDialog) throws ConvertigoException, CoreException {
		// Verify if an object with the same name exists
		TreeObject siblingTreeObject = this;
		while ((siblingTreeObject = siblingTreeObject.getPreviousSibling()) != null) {
			if (siblingTreeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject siblingDatabaseObjectTreeObject = (DatabaseObjectTreeObject)siblingTreeObject;
				if (!siblingDatabaseObjectTreeObject.isInherited) {
					DatabaseObject databaseObjectTmp = siblingDatabaseObjectTreeObject.getObject();
					String databaseObjectName = databaseObjectTmp.getName();
					if (databaseObjectName.equalsIgnoreCase(newName)) {
						throw new ConvertigoException("Another object with the same name already exists (case insensitive).");
					}
				}
			}
		}

		siblingTreeObject = this;
		while ((siblingTreeObject = siblingTreeObject.getNextSibling()) != null) {
			if (siblingTreeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject siblingDatabaseObjectTreeObject = (DatabaseObjectTreeObject)siblingTreeObject;
				if (!siblingDatabaseObjectTreeObject.isInherited) {
					DatabaseObject databaseObjectTmp = siblingDatabaseObjectTreeObject.getObject();

					String databaseObjectName = databaseObjectTmp.getName();
					if (databaseObjectName.equalsIgnoreCase(newName)) {
						throw new ConvertigoException("Another object with the same name already exists (case insensitive).");
					}
				}
			}
		}

		DatabaseObject databaseObject = getObject();
		databaseObject.setName(newName);
		databaseObject.hasChanged = true;
		hasBeenModified(true);
	}
	
	public boolean rename(String newName, boolean bDialog) {
		try {
			if (!StringUtils.isNormalized(newName)) {
				throw new ConvertigoException("The name \"" + newName + "\" must be normalized.\nDon't start with number and don't use non ASCII caracters.");
			}

			DatabaseObject databaseObject = getObject();
			String oldName = databaseObject.getName();

			if (newName.equals(oldName)) {
				return true;
			}

			if (newName.equalsIgnoreCase(oldName)) {
				throw new ConvertigoException("The rename operation is case insensitive.");
			}
			
			rename_(newName, bDialog);
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
		if (this instanceof ProjectTreeObject) {
			return (ProjectTreeObject) this;
		}
		
		TreeParent treeParent = getParent();
		while (treeParent != null) {
			if (treeParent instanceof ProjectTreeObject) {
				return (ProjectTreeObject) treeParent;
			}
			treeParent = treeParent.getParent();
		}
		
		return null;
	}
	
	public DatabaseObjectTreeObject getParentDatabaseObjectTreeObject() {
		DatabaseObjectTreeObject databaseObjectTreeObject = null;
		
		TreeParent treeParent = getParent();
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
	public boolean testAttribute(Object target, String name, String value) {
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		if (name.equals("canPaste")) {
			canPaste = ((ConvertigoPlugin.clipboardManagerSystem.isCopy) || (ConvertigoPlugin.clipboardManagerSystem.isCut));
			if (!canPaste) {
				try {
					Clipboard cb = new Clipboard(Display.getCurrent());
					String content = (String) cb.getContents(TextTransfer.getInstance());
					List<Object> dbos = ConvertigoPlugin.clipboardManagerSystem.read(content);
					canPaste = !dbos.isEmpty();
				} catch (Exception e) {
					// can fail if the clipboad doesn't contain text
				}
			}
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(canPaste));
		}
		return super.testAttribute(target, name, value);
	}

	public String getImageName() {
		//Check the status to the object to display the right image.children at this point aren't updated
		String imageName = MySimpleBeanInfo.getIconName(getObject(), BeanInfo.ICON_COLOR_16x16);
		imageName += (!isEnabled() ? "_disabled": hasAncestorDisabled()? "_unreachable":"");
		imageName += (isInherited ? "_inherited":"");
		imageName += (isDetectedObject ? "_detected":"");
		return imageName;
	}
	
	protected Set<Object> checkDone(TreeObjectEvent treeObjectEvent) {
		if (treeObjectEvent.done == null) {
			treeObjectEvent.done = new HashSet<Object>();
		}
		return treeObjectEvent.done;
	}
	
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		checkDone(treeObjectEvent);
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		if (!(treeObject.equals(this)))
			getDescriptors();// refresh editors (e.g labels in combobox)
	}

	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		checkDone(treeObjectEvent);
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		
		if (!(treeObject.equals(this))) {
			// this is a pool
			if (getObject() instanceof Pool) {
				Pool pool = (Pool)getObject();
				// case transaction deleted
				if (treeObject instanceof TransactionTreeObject) {
					if (treeObject.getConnectorTreeObject().equals(getConnectorTreeObject())) {
						if (pool.getStartTransaction().equals(treeObject.getName())) {
							pool.setStartTransaction("");
							hasBeenModified(true);
							viewer.refresh();
						}
					}
				}
				// case screenclass deleted
				if (treeObject instanceof ScreenClassTreeObject) {
					if (treeObject.getConnectorTreeObject().equals(getConnectorTreeObject())) {
						if (pool.getInitialScreenClass().equals(treeObject.getName())) {
							pool.setInitialScreenClass("");
							hasBeenModified(true);
							viewer.refresh();
						}
					}
				}
			}
		}
		
		if (!(treeObject.equals(this)) && (!getParents().contains(treeObject)))
			getDescriptors();// refresh editors (e.g labels in combobox)
	}

	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		checkDone(treeObjectEvent);
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (this instanceof INamedSourceSelectorTreeObject) {
			((INamedSourceSelectorTreeObject)this).getNamedSourceSelector().treeObjectPropertyChanged(treeObjectEvent);
		}
		
		// this is a pool
		if (getObject() instanceof Pool) {
			// handle bean's name changes
			if ("name".equals(propertyName)) {
				Pool pool = (Pool)getObject();
				// case transaction name changed
				if (treeObject instanceof TransactionTreeObject) {
					Transaction transaction = (Transaction) treeObject.getObject();
					if (transaction.getConnector().equals(pool.getConnector())) {
						if (pool.getStartTransaction().equals(oldValue)) {
							pool.setStartTransaction(newValue.toString());
							hasBeenModified(true);
							viewer.refresh();
						}
					}
				}
				// case screenclass name changed
				if (treeObject instanceof ScreenClassTreeObject) {
					ScreenClass sc = (ScreenClass) treeObject.getObject();
					if (sc.getConnector().equals(pool.getConnector())) {
						if (pool.getInitialScreenClass().equals(oldValue)) {
							pool.setInitialScreenClass(newValue.toString());
							hasBeenModified(true);
							viewer.refresh();
						}
					}
				}
			}
		}

		getDescriptors();// refresh editors (e.g labels in combobox)
	}
	
	public boolean hasAncestorDisabled(){
		DatabaseObjectTreeObject parent = getParentDatabaseObjectTreeObject();
		return parent!=null && ( !parent.isEnabled() || parent.hasAncestorDisabled());
	}
	
	public DatabaseObjectTreeObject findDatabaseObjectTreeObjectChild(DatabaseObject databaseObject) {
		for (TreeObject treeObject : getChildren()) {
			DatabaseObjectTreeObject databaseObjectTreeObject = findDatabaseObjectTreeObjectChild(treeObject, databaseObject);
			if (databaseObjectTreeObject != null) {
				return databaseObjectTreeObject;
			}
		}
		return null;
	}
	
	private static DatabaseObjectTreeObject findDatabaseObjectTreeObjectChild(TreeObject tree, DatabaseObject databaseObject) {
		if (tree instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) tree;
			if (databaseObjectTreeObject.getObject().equals(databaseObject)) {
				return databaseObjectTreeObject;
			}
		} else if (tree instanceof TreeParent) {
			for (TreeObject treeObject : ((TreeParent) tree).getChildren()) {
				DatabaseObjectTreeObject databaseObjectTreeObject = findDatabaseObjectTreeObjectChild(treeObject, databaseObject);
				if (databaseObjectTreeObject != null) {
					return databaseObjectTreeObject;
				}
			}
		}
		return null;
	}

	public boolean isSelected() {
		try {
			StructuredSelection selection = (StructuredSelection) viewer.getSelection();
			for (Iterator<?> i = selection.iterator(); i.hasNext();) {
				if (i.next() == this) {
					return true;
				}
			}
		}
		catch (Throwable t) {};
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (obj instanceof DatabaseObjectTreeObject) {
			DatabaseObject o1 = getObject();
			DatabaseObject o2 = ((DatabaseObjectTreeObject) obj).getObject();
			return o1 != null && o1.equals(o2);
		}
		return false;
	}
	
	@Override
	public TreeObject check() {
		if (parent != null) {
			return this;
		}
		TreeObject dboTree = getProjectExplorerView().findTreeObjectByUserObject(getObject());
		return dboTree != null ? dboTree : this;
	}
	
	@Override
	public TreeParent getParent() {
		return parent == null ? check().parent : parent;
	}
}
