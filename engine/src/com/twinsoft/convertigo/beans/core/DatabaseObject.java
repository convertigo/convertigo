/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.ClassUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.Version;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.transactions.SapJcoTransaction;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.UndefinedSymbolsException;
import com.twinsoft.convertigo.engine.VersionException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.EnumUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * This is the base class for all Convertigo objects which should be serialized
 * in the Convertigo database.
 */
@DboFolderType
public abstract class DatabaseObject implements Serializable, Cloneable, ITokenPath {
	private static final long serialVersionUID = -873065042105207891L;
	private static final Pattern pIntSuffix = Pattern.compile("\\d+$");
	protected final Object mutex = new Object();

	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface DboFolderType {
		FolderType type() default FolderType.NONE;
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DboCategoryInfo {
		String getCategoryId();
		String getCategoryName();
		String getIconClassCSS();
	}

	public static DboCategoryInfo getDboGroupInfo(Class<?> beanClass) {
		if (beanClass.isAssignableFrom(DatabaseObject.class)) {
			return null;
		}

		DboCategoryInfo dboCategoryInfo = beanClass.getAnnotation(com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo.class);
		while (dboCategoryInfo == null && !beanClass.isAssignableFrom(DatabaseObject.class)) {
			beanClass = beanClass.getSuperclass();
			dboCategoryInfo = beanClass.getAnnotation(com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo.class);
		}

		return dboCategoryInfo;
	}

	public static final String PROPERTY_XMLNAME = "xmlname";

	private transient Map<String, Set<String>> symbolsErrors = null;

	private static class SubLoader extends WalkHelper {
		DatabaseObject databaseObject;

		@Override
		public void init(DatabaseObject databaseObject) throws Exception {
			try {
				this.databaseObject = databaseObject;
				super.init(databaseObject);
			} finally {
				this.databaseObject = null;
			}
		}

		@Override
		protected void walk(DatabaseObject subDatabaseObject) throws Exception {
			if (subDatabaseObject.original != null) {
				super.walk(subDatabaseObject.original);
			} else {
				databaseObject.add(subDatabaseObject.clone());
			}
		}

	}

	public enum ExportOption {
		bIncludeDisplayName, bIncludeCompiledValue, bIncludeShortDescription, bIncludeEditorClass, bIncludeBlackListedElements, bIncludeVersion, bHidePassword;
	}

	transient protected static long lastTime = 0;

	transient private final static ThreadLocal<SubLoader> subLoader = new ThreadLocal<SubLoader>() {
		@Override
		protected SubLoader initialValue() {
			return new SubLoader();
		}
	};

	transient protected long identity;

	transient public boolean isImporting = false;

	transient private DatabaseObject original = null;

	transient public boolean isSubLoaded = false;

	/**
	 * The source values map for compilable properties.
	 */
	transient private Map<String, Object> compilablePropertySourceValuesMap;

	transient protected Collection<ExportOption> exportOptions = new HashSet<ExportOption>();

	public Object getCompilablePropertySourceValue(String propertyName) {
		return compilablePropertySourceValuesMap.get(propertyName);
	}

	public void setCompilablePropertySourceValue(String propertyName, Object uncompiledPropertyValue) {
		compilablePropertySourceValuesMap.put(propertyName, uncompiledPropertyValue);
	}

	public void removeCompilablePropertySourceValue(String propertyName) {
		compilablePropertySourceValuesMap.remove(propertyName);
	}

	public DatabaseObject() {
		try {
			BeanInfo bi = CachedIntrospector.getBeanInfo(getClass());
			BeanDescriptor bd = bi.getBeanDescriptor();
			setBeanName(StringUtils.normalize(defaultBeanName(bd.getDisplayName()))); // normalize
			// bean
			// name
			// #283
			identity = getNewOrderValue();
			compilablePropertySourceValuesMap = new HashMap<String, Object>(5);
		} catch (Exception e) {
			name = getClass().getName();
			Engine.logBeans.error("Unable to set the default name; using the class name instead (\"" + name
					+ "\").", e);
		}
	}

	public Icon getIcon(int iconKind) {
		try {
			BeanInfo bi = CachedIntrospector.getBeanInfo(getClass());
			return new ImageIcon(bi.getIcon(iconKind));
		} catch (Exception e) {
			Engine.logBeans.error("Unable to get the bean icon.", e);
			return null;
		}
	}

	public void checkSymbols() throws EngineException {
		if (isSymbolError()) {
			for (String property : symbolsErrors.keySet()) {
				String message = "The property '" + property + "' of '" + getName() + "' has an undefined global symbol!";
				if (Engine.isStudioMode()) {
					try {
						Class<?> convertigoPlugin = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
						Method m = convertigoPlugin.getMethod("warningMessageBox", String.class);
						m.invoke(null, message);
					} catch (Exception e) {
						Engine.logBeans.error("Unable to invoke the warningMessageBox method.", e);
					}
				}
				throw new EngineException(message);
			}
		}
	}

	@Override
	public DatabaseObject clone() throws CloneNotSupportedException {
		if (original == null) {
			try {
				DatabaseObject clone = (DatabaseObject) super.clone();
				clone.original = this;
				clone.parent = null;
				clone.bNew = false;
				clone.isImporting = false;
				clone.isSubLoaded = false; //Engine.isEngineMode() ? isSubLoaded : false;
				clone.hasChanged = false; //Engine.isEngineMode() ? false : hasChanged;// Studio
				// case
				// of
				// refresh
				// without
				// saving
				clone.compilablePropertySourceValuesMap = new HashMap<String, Object>(5);
				clone.compilablePropertySourceValuesMap.putAll(compilablePropertySourceValuesMap);
				return clone;
			} catch (Exception e) {
				Engine.logBeans.error("Unable to clone the object \"" + getName() + "\"", e);
				String message = "DatabaseObject.clone() " + e.getClass().getName() + "\n" + e.getMessage();
				throw new CloneNotSupportedException(message);
			}
		} else {
			return original.clone();
		}
	}

	public void add(DatabaseObject databaseObject) throws EngineException {
		databaseObject.setParent(this);
	}

	public void remove(DatabaseObject databaseObject) throws EngineException {
		databaseObject.parent = null;
	}

	public void delete() throws EngineException {
		Engine.logBeans.info("Deleting the object \"" + getName() + "\"");
		if (parent != null) {
			parent.remove(this);
		}
		parent = null;
	}

	public transient boolean hasChanged = false;

	public transient boolean bNew = false;

	/**
	 * The type of the database object. This type is used for storing the object
	 * into the database.
	 */
	transient protected String databaseType = "undefined";

	public String getDatabaseType() {
		return databaseType;
	}

	/**
	 * The parent database object (null only for top object : project).
	 */
	transient protected DatabaseObject parent = null;

	/**
	 * Retrieves the parent Object object.
	 */
	public DatabaseObject getParent() {
		//		if (parent == null && !isImporting) {
		//			String msg = "(DatabaseObject) parent == null\n"
		//				+ "name: " + name + "\n"
		//				+ "priority: " + priority + "\n"
		//				+ "stack: " + Arrays.toString(Thread.currentThread().getStackTrace());
		//			Engine.logEngine.warn(msg);
		//		}
		return parent;
	}

	public String getParentName() {
		DatabaseObject parent = getParent();
		return parent != null ? parent.getName() : "[null]";
	}

	public Project getProject() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof Project) && databaseObject != null) {
			databaseObject = databaseObject.getParent();
		}

		if (databaseObject == null)
			return null;
		else
			return (Project) databaseObject;
	}

	public String getTokenPath(String oldName) {
		String tokenPath = oldName == null ? getName():oldName;

		DatabaseObject parentDbo = parent;
		while (parentDbo != null) {
			tokenPath = parentDbo.getName() + "." + tokenPath;
			if (parentDbo instanceof Project)
				break;
			else
				parentDbo = parentDbo.getParent();
		}
		return tokenPath;
	}

	public Connector getConnector() {
		if (this instanceof Project) {
			return null;
		}
		if (this instanceof Sequence) {
			return null;
		}
		if (this instanceof Step) {
			return null;
		}
		if (this instanceof Connector) {
			return (Connector) this;
		} else {
			DatabaseObject databaseObject = parent;
			while (!(databaseObject instanceof Connector)) {
				databaseObject = databaseObject.getParent();
			}
			return (Connector) databaseObject;
		}
	}

	/**
	 * Sets the parent Object object.
	 */
	public void setParent(DatabaseObject databaseObject) {
		if (parent != null) {
			parent.changed();
		}

		parent = databaseObject;

		if (parent != null) {
			parent.changed();
		}

		//changed();
	}

	/**
	 * Returns the fully qualified name of the object, i.e. with the object
	 * path, for writing purposes.
	 *
	 * @return the fully qualified name of the object.
	 */
	public String getQName(boolean full) {
		String qname = full ? getFolderType().qnamePart(this) : getName();
		if (parent != null) {
			qname = parent.getQName(full) + "." + qname;
		}
		return qname;
	}

	public String getQName() {
		return getQName(false);
	}

	public String getFullQName() {
		return getQName(true);
	}

	public String getShortQName() {
		String qn = getQName(true).replace(':', '-');
		int i = qn.lastIndexOf('.');
		if (i > 0) {
			return StringUtils.hash(qn.substring(0, i)) + qn.substring(i);
		} else {
			return qn;
		}
	}

	/**
	 * The priority of object.
	 */
	transient public long priority = 0;

	/**
	 * The object name.
	 */
	private String name = "new object";

	/**
	 * Retrieves the object name.
	 *
	 * @return the object name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the bean name and computes file name.
	 *
	 * @param name
	 *            the bean name.
	 */
	private void setBeanName(String name) throws EngineException {
		if (name.length() == 0) {
			throw new EngineException("The object name cannot be empty!");
		}
		this.name = name;
	}

	/**
	 * Sets the object name.
	 *
	 * @param name
	 *            the object name.
	 */
	public void setName(String name) throws EngineException {
		if (name == null) {
			throw new EngineException("The object name cannot be null!");
		}
		if (name.length() == 0) {
			throw new EngineException("The object name cannot be empty!");
		}

		String[] newName = {StringUtils.normalize(name)};
		if (parent != null) {
			FolderType fd = getFolderType();
			boolean ok = false;
			while (!ok) {
				try {
					ok = true;
					new WalkHelper() {

						@Override
						protected void walk(DatabaseObject databaseObject) throws Exception {
							if (DatabaseObject.this.parent == databaseObject) {
								super.walk(databaseObject);
							} else if (DatabaseObject.this != databaseObject && fd.equals(databaseObject.getFolderType()) && newName[0].equals(databaseObject.getName())) {
								throw new ObjectWithSameNameException("");
							}
						}

					}.init(parent);
				} catch (ObjectWithSameNameException e) {
					ok = false;
					newName[0] = incrementName(newName[0]);
				} catch (Exception e) {
					// should not occurs
				}
			}
		}
		String oldName = this.name;
		// set new name and new computed file name
		setBeanName(newName[0]);
		if (original == null && !isImporting && !oldName.equals(name)) {
			onBeanNameChanged(oldName, name);
		}
	}

	public static String incrementName(String name) {
		Matcher m = pIntSuffix.matcher(name);
		if (m.find()) {
			int inc = Integer.parseInt(m.group()) + 1;
			name = m.replaceFirst(Integer.toString(inc));
		} else {
			name += "1";
		}
		return name;
	}

	/**
	 * Returns an available name for a new child database object to add.
	 *
	 * @param eDatabaseObjects
	 *            an enumeration of children database objects
	 * @param dboName
	 *            the name of a new child database object
	 * @return
	 */
	public String getChildBeanName(Collection<? extends DatabaseObject> v, String dboName, boolean isNew) {
		String newDatabaseObjectName = dboName;
		boolean bContinue = isNew || true;
		while (bContinue) {
			try {
				for (DatabaseObject databaseObject : v) {
					if (newDatabaseObjectName.equals(databaseObject.getName())) {
						throw new ObjectWithSameNameException("Unable to add the object \""
								+ newDatabaseObjectName
								+ "\" because an object with the same name already exists.");
					}
				}
				bContinue = false;
			} catch (ObjectWithSameNameException e) {
				newDatabaseObjectName = incrementName(newDatabaseObjectName);
			}
		}
		return newDatabaseObjectName;
	}

	/**
	 * Get order for quick sort.
	 */
	public Object getOrderedValue() {
		return name.toLowerCase();
	}

	public long getNewOrderValue() {
		long now = System.currentTimeMillis();
		if (lastTime < now) {
			lastTime = now;
		} else {
			lastTime++;
		}

		return lastTime;
	}

	/**
	 * Get representation of order for quick sort of a given database object.
	 */
	public Object getOrder(Object object) throws EngineException {
		if (object instanceof DatabaseObject) {
			return ((DatabaseObject) object).getOrderedValue();
		}
		return object.toString();
	}

	@Override
	public String toString() {
		return name;
	}

	public Element toXml(Document document) throws EngineException {
		Element element = document.createElement(getDatabaseType().toLowerCase());

		element.setAttribute("classname", getClass().getName());
		if (exportOptions.contains(ExportOption.bIncludeVersion)) {
			element.setAttribute("version", com.twinsoft.convertigo.beans.Version.version);
		}

		// Storing the database object priority
		element.setAttribute("priority", Long.toString(priority));

		int len;
		PropertyDescriptor[] propertyDescriptors;
		PropertyDescriptor propertyDescriptor;

		try {
			BeanInfo bi = CachedIntrospector.getBeanInfo(getClass());
			propertyDescriptors = bi.getPropertyDescriptors();
			len = propertyDescriptors.length;
			if (exportOptions.contains(ExportOption.bIncludeDisplayName)) {
				element.setAttribute("displayName", bi.getBeanDescriptor().getDisplayName());
			}
		} catch (IntrospectionException e) {
			throw new EngineException("Couldn't introspect the bean \"" + getName() + "\"", e);
		}

		for (int i = 0; i < len; i++) {
			propertyDescriptor = propertyDescriptors[i];
			String propertyDescriptorName = propertyDescriptor.getName();
			addProperty(document, element, propertyDescriptor, propertyDescriptorName);
		}

		return element;
	}

	public Element toXml(Document document, String propertyName) throws EngineException {
		Element element = document.createElement(getDatabaseType().toLowerCase());

		if (exportOptions.contains(ExportOption.bIncludeVersion)) {
			element.setAttribute("version", com.twinsoft.convertigo.beans.Version.version);
		}

		int len;
		PropertyDescriptor[] propertyDescriptors;
		PropertyDescriptor propertyDescriptor;

		try {
			BeanInfo bi = CachedIntrospector.getBeanInfo(getClass());
			propertyDescriptors = bi.getPropertyDescriptors();
			len = propertyDescriptors.length;
			if (exportOptions.contains(ExportOption.bIncludeDisplayName)) {
				element.setAttribute("displayName", bi.getBeanDescriptor().getDisplayName());
			}
		} catch (IntrospectionException e) {
			throw new EngineException("Couldn't introspect the bean \"" + getName() + "\"", e);
		}

		boolean found = false;

		for (int i = 0; !found && i < len; i++) {
			propertyDescriptor = propertyDescriptors[i];
			String propertyDescriptorName = propertyDescriptor.getName();

			if (found = propertyDescriptorName.equals(propertyName)) {
				addProperty(document, element, propertyDescriptor, propertyDescriptorName);
			}
		}

		return element;
	}

	private void addProperty(Document document, Element root, PropertyDescriptor propertyDescriptor, String propertyDescriptorName) {
		String displayName = propertyDescriptor.getDisplayName();
		String shortDescription = propertyDescriptor.getShortDescription();

		Method getter = propertyDescriptor.getReadMethod();

		// Only analyze read propertyDescriptors.
		if (getter == null) {
			return;
		}

		if (checkBlackListParentClass(propertyDescriptor)) {
			return;
		}

		try {
			// Storing the database object bean properties
			Object uncompiledValue = getCompilablePropertySourceValue(propertyDescriptorName);
			Object compiledValue = null;
			Object cypheredValue = null;
			Object value = getter.invoke(this);

			if (uncompiledValue != null) {
				compiledValue = value;
				value = uncompiledValue;
			}

			// Only write non-null values
			if (value == null) {
				Engine.logBeans.warn("Attempting to store null property (\"" + propertyDescriptorName + "\"); skipping...");
				return;
			}

			Element propertyElement = document.createElement("property");
			propertyElement.setAttribute("name", propertyDescriptorName);

			// Encrypts value if needed
			//if (isCipheredProperty(propertyDescriptorName) && !this.exportOptions.contains(ExportOption.bIncludeDisplayName)) {
			if (isCipheredProperty(propertyDescriptorName) &&
					( this.exportOptions.contains(ExportOption.bHidePassword) ||
							!this.exportOptions.contains(ExportOption.bIncludeDisplayName))) {
				cypheredValue = encryptPropertyValue(value);
				if (!value.equals(cypheredValue)) {
					value = cypheredValue;
					propertyElement.setAttribute("ciphered", "true");
				}
			}

			// Stores the value
			Node node = null;
			if (exportOptions.contains(ExportOption.bIncludeCompiledValue)) {
				node = XMLUtils.writeObjectToXml(document, value, compiledValue);
			} else {
				node = XMLUtils.writeObjectToXml(document, value);
			}
			propertyElement.appendChild(node);

			// Add visibility for logs
			if (!isTraceableProperty(propertyDescriptorName)) {
				propertyElement.setAttribute("traceable", "false");
			}

			if (exportOptions.contains(ExportOption.bIncludeBlackListedElements)) {
				Object propertyDescriptorBlackListValue = propertyDescriptor
						.getValue(MySimpleBeanInfo.BLACK_LIST_NAME);
				if (propertyDescriptorBlackListValue != null && (Boolean) propertyDescriptorBlackListValue) {
					propertyElement.setAttribute("blackListed", "blackListed");
				}
			}
			if (exportOptions.contains(ExportOption.bIncludeDisplayName)) {
				propertyElement.setAttribute("displayName", displayName);
				propertyElement.setAttribute("isHidden", Boolean.toString(propertyDescriptor.isHidden()));
				propertyElement.setAttribute("isMasked",
						isMaskedProperty(Visibility.Platform, propertyDescriptorName) ? "true" : "false");
				propertyElement.setAttribute("isExpert", Boolean.toString(propertyDescriptor.isExpert()));
				
				boolean isDisabled = Boolean.TRUE.equals(propertyDescriptor.getValue(MySimpleBeanInfo.DISABLE));
				propertyElement.setAttribute("isDisabled", Boolean.toString(isDisabled));
				String category = (String) propertyDescriptor.getValue(MySimpleBeanInfo.CATEGORY);
				propertyElement.setAttribute("category", category);
			}
			if (exportOptions.contains(ExportOption.bIncludeShortDescription)) {
				propertyElement.setAttribute("shortDescription", shortDescription);
			}

			if (exportOptions.contains(ExportOption.bIncludeEditorClass)) {
				Class<?> pec = propertyDescriptor.getPropertyEditorClass();
				String message = "";
				if (pec != null) {
					message = propertyDescriptor.getPropertyEditorClass().toString()
							.replaceFirst("(.)*\\.", "");
				} else {
					message = "null";
				}
				if (this instanceof ITagsProperty || (pec != null && Enum.class.isAssignableFrom(pec))) {
					String[] sResults = null;
					try {
						if (this instanceof ITagsProperty) {
							sResults = ((ITagsProperty) this).getTagsForProperty(propertyDescriptorName);
						} else {
							sResults = EnumUtils.toNames(pec);
						}
					} catch (Exception ex) {
						sResults = new String[0];
					}

					if (sResults != null) {
						if (sResults.length > 0) {
							Element possibleValues = document.createElement("possibleValues");
							Element possibleValue = null;
							for (int j = 0; j < sResults.length; j++) {
								possibleValue = document.createElement("value");
								possibleValue.setTextContent(sResults[j]);
								possibleValues.appendChild(possibleValue);
							}
							propertyElement.appendChild(possibleValues);
						}
					}
				}
				propertyElement.setAttribute("editorClass", message);
				if (Boolean.TRUE.equals(propertyDescriptor.getValue(MySimpleBeanInfo.MULTILINE))) {
					propertyElement.setAttribute("isMultiline", "true");
				}
				if (Boolean.TRUE.equals(propertyDescriptor.getValue(MySimpleBeanInfo.SCRIPTABLE))) {
					propertyElement.setAttribute("isScriptable", "true");
				}
			}

			root.appendChild(propertyElement);

			if (Boolean.TRUE.equals(propertyDescriptor.getValue(MySimpleBeanInfo.NILLABLE))) {
				try {
					Method method = this.getClass().getMethod("isNullProperty",
							new Class[] { String.class });
					Object isNull = method.invoke(this, new Object[] { propertyDescriptorName });
					propertyElement.setAttribute("isNull", isNull.toString());
				} catch (Exception ex) {
					Engine.logBeans.error("[Serialization] Skipping 'isNull' attribute for property \""
							+ propertyDescriptorName + "\".", ex);
				}
			}
		} catch (Exception e) {
			Engine.logBeans.error("[Serialization] Skipping property \"" + propertyDescriptorName + "\".", e);
		}
	}


	public Element toXml(Document document, ExportOption... exportOptions) throws EngineException {
		try {
			this.exportOptions.addAll(Arrays.asList(exportOptions));
			return toXml(document);
		} finally {
			this.exportOptions.clear();
		}
	}

	public Element toXml(Document document, String propertyName, ExportOption... exportOptions) throws EngineException {
		try {
			this.exportOptions.addAll(Arrays.asList(exportOptions));
			return toXml(document, propertyName);
		} finally {
			this.exportOptions.clear();
		}
	}

	/**
	 * Performs custom configuration on the XML document for this database
	 * object BEFORE its de-serialization.
	 */
	public void preconfigure(Element element) throws Exception {
		// Do nothing by default
	}

	/**
	 * Performs custom configuration on the database object AFTER its
	 * de-serialization.
	 */
	public void configure(Element element) throws Exception {
		// Do nothing by default
	}

	public static DatabaseObject read(Node node) throws EngineException {
		String objectClassName = "n/a";
		String childNodeName = "n/a";
		String propertyName = "n/a";
		String propertyValue = "n/a";
		DatabaseObject databaseObject = null;
		Element element = (Element) node;

		objectClassName = element.getAttribute("classname");

		// Migration to 4.x+ projects
		if (objectClassName.equals("com.twinsoft.convertigo.beans.core.ScreenClass")) {
			objectClassName = "com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass";
		}

		String version = element.getAttribute("version");
		if ("".equals(version)) {
			version = node.getOwnerDocument().getDocumentElement().getAttribute("beans");
			if (!"".equals(version)) {
				element.setAttribute("version", version);
			}
		}
		// Verifying product version
		if ("com.twinsoft.convertigo.beans.core.Project".equals(objectClassName) && VersionUtils.compareProductVersion(Version.productVersion, version) < 0) {
			VersionException ee = new VersionException(version);
			throw ee;
		}

		try {
			Engine.logBeans.trace("Creating object of class \"" + objectClassName + "\"");
			databaseObject = (DatabaseObject) Class.forName(objectClassName).getConstructor().newInstance();
		} catch (Exception e) {
			String s = node.getNodeName();// XMLUtils.prettyPrintDOM(node);
			String message = "Unable to create a new instance of the object from the serialized XML data.\n"
					+ "Object class: '" + objectClassName + "'\n" + "Object version: " + version + "\n"
					+ "XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}

		databaseObject.isImporting = true;

		try {
			// Performs custom configuration before object de-serialization
			databaseObject.preconfigure(element);
		} catch (Exception e) {
			String s = XMLUtils.prettyPrintDOM(node);
			String message = "Unable to configure the object from serialized XML data before its creation.\n"
					+ "Object class: '" + objectClassName + "'\n" + "XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}

		try {
			long priority = Long.valueOf(element.getAttribute("priority")).longValue();
			databaseObject.priority = priority;

			Class<? extends DatabaseObject> databaseObjectClass = databaseObject.getClass();
			BeanInfo bi = CachedIntrospector.getBeanInfo(databaseObjectClass);
			PropertyDescriptor[] pds = bi.getPropertyDescriptors();

			NodeList childNodes = element.getChildNodes();
			int len = childNodes.getLength();

			PropertyDescriptor pd;
			Object propertyObjectValue;
			Class<?> propertyType;
			NamedNodeMap childAttributes;
			boolean maskValue = false;

			for (int i = 0; i < len; i++) {
				Node childNode = childNodes.item(i);
				if (childNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}

				Element childElement = (Element) childNode;

				childNodeName = childNode.getNodeName();

				Engine.logBeans.trace("Analyzing node '" + childNodeName + "'");

				if (childNodeName.equalsIgnoreCase("property")) {
					childAttributes = childNode.getAttributes();
					propertyName = childAttributes.getNamedItem("name").getNodeValue();
					Engine.logBeans.trace("  name = '" + propertyName + "'");
					pd = findPropertyDescriptor(pds, propertyName);
					if (pd == null) {
						Engine.logBeans.info("Unable to find the definition of property \"" + propertyName
								+ "\"; skipping.");
						continue;
					}
					propertyType = pd.getPropertyType();
					propertyObjectValue = XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(
							childNode, Node.ELEMENT_NODE));

					// Hides value in log trace if needed
					if ("false".equals(childElement.getAttribute("traceable"))) {
						maskValue = true;
					}
					Engine.logBeans.trace("  value='"
							+ (maskValue ? Visibility.maskValue(propertyObjectValue) : propertyObjectValue)
							+ "'");

					// Decrypts value if needed
					try {
						if ("true".equals(childElement.getAttribute("ciphered"))) {
							propertyObjectValue = decryptPropertyValue(propertyObjectValue);
						}
					} catch (Exception e) {
					}

					propertyObjectValue = databaseObject.compileProperty(propertyType, propertyName, propertyObjectValue);

					if (Enum.class.isAssignableFrom(propertyType)) {
						propertyObjectValue = EnumUtils.valueOf(propertyType, propertyObjectValue);
					}

					propertyValue = propertyObjectValue.toString();

					Method setter = pd.getWriteMethod();
					Engine.logBeans.trace("  setter='" + setter.getName() + "'");
					Engine.logBeans.trace("  param type='" + propertyObjectValue.getClass().getName() + "'");
					Engine.logBeans.trace("  expected type='" + propertyType.getName() + "'");
					try {
						setter.invoke(databaseObject, new Object[] { propertyObjectValue });
					} catch (InvocationTargetException e) {
						Throwable targetException = e.getTargetException();
						Engine.logBeans.warn("Unable to set the property '" + propertyName
								+ "' for the object '" + databaseObject.getName() + "' (" + objectClassName
								+ "): [" + targetException.getClass().getName() + "] "
								+ targetException.getMessage());
					}

					if (Boolean.TRUE.equals(pd.getValue(MySimpleBeanInfo.NILLABLE))) {
						Node nodeNull = childAttributes.getNamedItem("isNull");
						String valNull = (nodeNull == null) ? "false" : nodeNull.getNodeValue();
						Engine.logBeans.trace("  treats as null='" + valNull + "'");
						try {
							Method method = databaseObject.getClass().getMethod("setNullProperty",
									new Class[] { String.class, Boolean.class });
							method.invoke(databaseObject,
									new Object[] { propertyName, Boolean.valueOf(valNull) });
						} catch (Exception ex) {
							Engine.logBeans.warn("Unable to set the 'isNull' attribute for property '"
									+ propertyName + "' of '" + databaseObject.getName() + "' object");
						}
					}
				}
			}
		} catch (Exception e) {
			String message = "Unable to set the object properties from the serialized XML data.\n"
					+ "Object class: '" + objectClassName + "'\n" + "XML analyzed node: " + childNodeName
					+ "\n" + "Property name: " + propertyName + "\n" + "Property value: " + propertyValue;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}


		try {
			// Performs custom configuration
			databaseObject.configure(element);
		} catch (Exception e) {
			String s = XMLUtils.prettyPrintDOM(node);
			String message = "Unable to configure the object from serialized XML data after its creation.\n"
					+ "Object class: '" + objectClassName + "'\n" + "XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}

		return databaseObject;
	}

	public Object compileProperty(String propertyName, Object propertyObjectValue) {
		return compileProperty(String.class, propertyName, propertyObjectValue);
	}

	public Object compileProperty(Class<?> propertyType, String propertyName, Object propertyObjectValue) {
		Object compiled;
		try {
			compiled = Engine.theApp.databaseObjectsManager.getCompiledValue(propertyObjectValue);
			removeSymbolError(propertyName);
		} catch (UndefinedSymbolsException e) {
			addSymbolError(propertyName, e.undefinedSymbols());
			compiled = e.incompletValue();
		}

		if (compiled != propertyObjectValue) {
			setCompilablePropertySourceValue(propertyName, propertyObjectValue);
			propertyObjectValue = compiled;
		} else {
			removeCompilablePropertySourceValue(propertyName);
		}

		propertyType = ClassUtils.primitiveToWrapper(propertyType);
		if (Number.class.isAssignableFrom(propertyType)) {
			try {
				try {
					propertyObjectValue = propertyType.getConstructor(String.class).newInstance(propertyObjectValue.toString());
				} catch (Exception e) {
					Engine.logBeans.warn("(DatabaseObject) For property '"+ propertyName +"', failed to parse '" + propertyObjectValue + "' as " + propertyType.getSimpleName() + ". Set 0 instead.");
					propertyObjectValue = propertyType.getConstructor(String.class).newInstance("0");
				}
			} catch (Exception e) {
				Engine.logBeans.error("(DatabaseObject) For property '"+ propertyName +"', failed to parse '" + propertyObjectValue + "' as " + propertyType.getSimpleName());
			}
		} else if (Boolean.class.isAssignableFrom(propertyType)) {
			if (propertyObjectValue instanceof Integer) {
				propertyObjectValue = (Integer) propertyObjectValue == 0;
			} else {
				propertyObjectValue = Boolean.parseBoolean(propertyObjectValue.toString());
			}
		} else if (Enum.class.isAssignableFrom(propertyType) && propertyObjectValue instanceof String) {
			propertyObjectValue = EnumUtils.valueOf(propertyType, propertyObjectValue);
		}
		return propertyObjectValue;
	}



	public boolean isMaskedProperty(Visibility target, String propertyName) {
		return false;
	}

	public boolean isCipheredProperty(String propertyName) {
		return false;
	}

	public boolean isTraceableProperty(String propertyName) {
		return true;
	}

	public static String encryptPropertyValue(String propertyValue) {
		return Crypto2.encodeToHexString(propertyValue);
	}

	public static <E> E encryptPropertyValue(E propertyValue) {
		if (propertyValue == null) {
			return null;
		}

		E encryptedValue = null;
		if (propertyValue instanceof String) {
			encryptedValue = GenericUtils.cast(Crypto2.encodeToHexString((String) propertyValue));
		} else if (propertyValue instanceof XMLVector<?>) {
			try {
				XMLVector<Object> xmlv = new XMLVector<Object>(
						GenericUtils.<XMLVector<Object>>cast(propertyValue));
				for (int i = 0; i < xmlv.size(); i++) {
					Object ob = xmlv.get(i);
					xmlv.set(i, encryptPropertyValue(ob));
				}
				encryptedValue = GenericUtils.cast(xmlv);
			} catch (Exception e) {
			}
		} else if (propertyValue instanceof SmartType) {
			SmartType st = (SmartType) propertyValue;
			SmartType newSt = new SmartType();
			newSt.setMode(st.getMode());
			newSt.setExpression(encryptPropertyValue(st.getExpression()));
			newSt.setSourceDefinition(encryptPropertyValue(st.getSourceDefinition()));
			encryptedValue = GenericUtils.cast(newSt);
		}

		if (encryptedValue == null) {
			encryptedValue = propertyValue;
		}
		return encryptedValue;
	}

	public static <E> E decryptPropertyValue(E encryptedValue) {
		if (encryptedValue == null) {
			return null;
		}

		E propertyValue = null;
		if (encryptedValue instanceof String) {
			try {
				propertyValue = GenericUtils.cast(Crypto2.decodeFromHexString3((String) encryptedValue));
			} catch (Exception e) {
				Engine.logBeans.trace("Failed to decode a property value", e);
				propertyValue = encryptedValue;
			}
		} else if (encryptedValue instanceof XMLVector<?>) {
			try {
				XMLVector<Object> xmlv = new XMLVector<Object>(
						GenericUtils.<XMLVector<Object>> cast(encryptedValue));
				for (int i = 0; i < xmlv.size(); i++) {
					Object ob = xmlv.get(i);
					xmlv.set(i, decryptPropertyValue(ob));
				}
				propertyValue = GenericUtils.cast(xmlv);
			} catch (Exception e) {
			}
		} else if (encryptedValue instanceof SmartType) {
			SmartType st = (SmartType) encryptedValue;
			SmartType newSt = new SmartType();
			newSt.setMode(st.getMode());
			newSt.setExpression(decryptPropertyValue(st.getExpression()));
			newSt.setSourceDefinition(decryptPropertyValue(st.getSourceDefinition()));
			propertyValue = GenericUtils.cast(newSt);
		}

		if (propertyValue == null) {
			propertyValue = encryptedValue;
		}
		return propertyValue;
	}

	protected static PropertyDescriptor findPropertyDescriptor(PropertyDescriptor[] pds, String pn) {
		int len = pds.length;
		PropertyDescriptor pd;
		for (int i = 0; i < len; i++) {
			pd = pds[i];
			if (pn.equals(pd.getName())) {
				return pd;
			}
		}
		return null;
	}

	/**
	 * Writes the object to the Convertigo repository database.
	 */
	public void write() throws EngineException {
		hasChanged = false;
		bNew = false;
	}

	/**
	 * Writes the object to the Convertigo repository database.
	 */
	public void write(String databaseObjectQName) throws EngineException {
		write();
	}

	/** Holds value of property version. */
	private String version = "";

	/**
	 * Setter for property version.
	 *
	 * @param version
	 *            New value of property version.
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Getter for property version.
	 *
	 * @return Value of property version.
	 */
	public String getVersion() {
		return this.version;
	}

	/** Holds value of property comment. */
	private String comment = "";

	/**
	 * Getter for property comment.
	 *
	 * @return Value of property comment.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Setter for property comment.
	 *
	 * @param comment
	 *            New value of property comment.
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	public void checkSubLoaded() {
		synchronized (mutex) {
			if (original != null && !isSubLoaded) {
				isSubLoaded = true;
				try {
					subLoader.get().init(this);
				} catch (EngineException e) {
					Engine.logBeans.error("(DatabaseObject) getSubDatabaseObjects failed with EngineException!", e);
				} catch (Exception e) {
					Engine.logBeans.error("Another Exception: " + e.getMessage(), e);
				}
			}
		}
	}

	public long getIdentity() {
		return identity;
	}

	public DatabaseObject getOriginal() {
		return original != null ? original : this;
	}

	public boolean isOriginal() {
		return original == null;
	}

	protected <E extends DatabaseObject> List<E> sort(List<E> list) {
		return sort(list, true);
	}

	protected <E extends Object> List<E> sort(List<E> list, boolean ascending) {
		List<E> res = new ArrayList<E>(list);
		Collections.sort(res, new Comparator<Object>() {
			@SuppressWarnings("unchecked")
			public int compare(Object o1, Object o2) {
				try {
					return ((Comparable<Object>) getOrder(o1)).compareTo(getOrder(o2));
				} catch (EngineException e) {
					return 0;
				}
			}
		});
		if (!ascending) {
			Collections.reverse(res);
		}
		return res;
	}

	public String[] getNames(Collection<? extends DatabaseObject> dbos) {
		int i = 0;
		String[] res = new String[dbos.size()];
		for (DatabaseObject dbo : dbos) {
			res[i++] = dbo != null ? dbo.getName() : "";
		}
		return res;
	}

	public String[] getNamesWithFirstEmptyItem(Collection<? extends DatabaseObject> dbos) {
		String[] res = new String[dbos.size() + 1];
		res[0] = "";
		int i = 1;
		for (DatabaseObject dbo : dbos) {
			res[i++] = dbo != null ? dbo.getName() : "";
		}
		return res;
	}

	public <E extends DatabaseObject> List<E> getAllChildren() {
		return new ArrayList<E>();
	}

	public void changed() {
		if (!isImporting) {
			hasChanged = true;
		}
	}
	public void addSymbolError(String propertyName, Set<String> undefinedSymbols) {
		if (symbolsErrors == null) {
			symbolsErrors = new HashMap<String, Set<String>>();
		}

		symbolsErrors.put(propertyName, undefinedSymbols);
		DatabaseObjectsManager.getProjectLoadingData().undefinedGlobalSymbol = true;
	}

	private void removeSymbolError(String propertyName) {
		synchronized (mutex) {
			if (symbolsErrors != null) {
				symbolsErrors.remove(propertyName);
				if (symbolsErrors.isEmpty()) {
					symbolsErrors = null;
				}
			}
		}
	}

	public boolean isSymbolError() {
		synchronized (mutex) {
			return symbolsErrors != null;
		}
	}

	public boolean checkBlackListParentClass(PropertyDescriptor propertyDescriptor) {
		return parent != null && parent.getClass().getCanonicalName()
				.equals(propertyDescriptor.getValue(MySimpleBeanInfo.BLACK_LIST_PARENT_CLASS));
	}

	public Map<String, Set<String>> getSymbolsErrors() {
		synchronized (mutex) {
			return new HashMap<String, Set<String>>(symbolsErrors);
		}
	}

	public Set<String> getSymbolsErrors(String parameterName) {
		synchronized (mutex) {
			return symbolsErrors == null ? null : symbolsErrors.get(parameterName);
		}
	}

	public void updateSymbols() throws Exception {
		if (!compilablePropertySourceValuesMap.isEmpty()) {
			PropertyDescriptor[] propertyDescriptors = CachedIntrospector.getBeanInfo(getClass()).getPropertyDescriptors();
			for (Entry<String, Object> propertySource : compilablePropertySourceValuesMap.entrySet()) {
				String propertyName = propertySource.getKey();
				Object propertyValue = propertySource.getValue();
				PropertyDescriptor propertyDescriptor = findPropertyDescriptor(propertyDescriptors, propertyName);
				Class<?> propertyType = propertyDescriptor.getPropertyType();
				Object newValue = compileProperty(propertyType, propertyName, propertyValue);
				propertyDescriptor.getWriteMethod().invoke(this, newValue);
			}
		}
	}

	public List<DatabaseObject> getDatabaseObjectChildren() throws Exception {
		return getDatabaseObjectChildren(false);
	}

	public List <DatabaseObject> getDatabaseObjectChildren(boolean deep) throws Exception {
		final List <DatabaseObject> children = new LinkedList<>();
		new WalkHelper() {

			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				if (databaseObject == DatabaseObject.this) {
					super.walk(databaseObject);
				}
				else {
					children.add(databaseObject);
					if (deep) {
						super.walk(databaseObject);
					}
				}
			}
		}.init(this);
		return children;
	}
	
	public boolean hasDatabaseObjectChildren() throws Exception {
		boolean has[] = { false };
		new WalkHelper() {

			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				if (databaseObject == DatabaseObject.this) {
					super.walk(databaseObject);
				}
				else {
					has[0] = true;
				}
			}
		}.init(this);
		return has[0];
	}

	public List<DatabaseObject> getSiblingsInFolder() {
		List<DatabaseObject> siblings = new ArrayList<DatabaseObject>();
		try {
			List<DatabaseObject> children = parent.getDatabaseObjectChildren();
			for (DatabaseObject child: children) {
				if (child.getFolderType().equals(getFolderType())) {
					siblings.add(child);
				}
			}
		} catch (Exception e) {}
		return siblings;
	}

	public DatabaseObject getPreviousSiblingInFolder() {
		try {
			List<DatabaseObject> siblings = getSiblingsInFolder();
			int index = siblings.indexOf(this);
			return siblings.get(index - 1);
		} catch (Exception e) {}
		return null;
	}

	public DatabaseObject getNextSiblingInFolder() {
		try {
			List<DatabaseObject> siblings = getSiblingsInFolder();
			int index = siblings.indexOf(this);
			return siblings.get(index + 1);
		} catch (Exception e) {}
		return null;
	}

	public DatabaseObject getDatabaseObjectChild(String name) throws Exception {
		List<DatabaseObject> children = getDatabaseObjectChildren();
		for (DatabaseObject child: children) {
			if (child.getFolderType().qnamePart(child).equals(name) || child.getName().equals(name)) {
				return child;
			}
		}
		return null;
	}

	public boolean testAttribute(String name, String value) {
		if (name.equals("isStepWithExpressions")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(this instanceof StepWithExpressions));
		}
		if (name.equals("isTargetForSapTransaction")) {
			Boolean bool = Boolean.valueOf(value);
			try {
				return bool.equals(Boolean.valueOf(((TransactionStep) getParent()).getTargetTransaction() instanceof SapJcoTransaction));
			} catch (Throwable e) {}
			return false;
		}
		if (name.equals("objectClassName")) {
			String objectClassName = getClass().getName();
			objectClassName = Pattern.quote(objectClassName);
			return value.matches("(^|.*;)" + objectClassName + "($|;.*)");
		}
		if (name.equals("objectPackageName")) {
			String objectPackageName = getClass().getPackageName();
			objectPackageName = Pattern.quote(objectPackageName);
			return value.matches("(^|.*;)" + objectPackageName + "($|;.*)");
		}
		if (name.equals("isModified")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(hasChanged));
		}
		if (name.equals("isInherited")) {
			return true;
		}
		return false;
	}

	protected void checkBatchOperation(Runnable runnable) {
		BatchOperationHelper.check(runnable);
	}

	public FolderType getFolderType() {
		return getFolderType(getClass());
	}
	
	static public FolderType getFolderType(Class<?> cls) {
		var anno = cls.getAnnotation(DboFolderType.class);
		while (anno == null) {
			cls = cls.getSuperclass();
			anno = cls.getAnnotation(DboFolderType.class);
		}
		return anno.type();
	}

	protected void onBeanNameChanged(String oldName, String newName) {
	}

	protected String defaultBeanName(String displayName) {
		return displayName;
	}
}