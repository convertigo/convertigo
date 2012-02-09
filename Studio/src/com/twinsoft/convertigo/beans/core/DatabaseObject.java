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

package com.twinsoft.convertigo.beans.core;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import com.twinsoft.convertigo.beans.Version;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.DatabaseObjectNotFoundException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.Crypto;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * This is the base class for all Convertigo objects which should be
 * serialized in the Convertigo database.
 */
public abstract class DatabaseObject implements Serializable, Cloneable {
	private static final long serialVersionUID = -873065042105207891L;

	public enum ExportOption {
		bIncludeDisplayName,
		bIncludeCompiledValue,
		bIncludeShortDescription,
		bIncludeEditorClass,
		bIncludeBlackListedElements,
		bIncludeVersion;
	}

	transient protected static long lastTime = 0;
	
	transient protected long identity;

	transient public boolean isImporting = false;

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
			BeanInfo bi = Introspector.getBeanInfo(getClass());
			BeanDescriptor bd = bi.getBeanDescriptor();
			setBeanName(StringUtils.normalize(bd.getDisplayName())); // normalize bean name #283
			identity = getNewOrderValue();
			compilablePropertySourceValuesMap = new HashMap<String, Object>(5);
		} catch(Exception e) {
			name = getClass().getName();
			Engine.logBeans.error("Unable to set the default name; using the class name instead (\"" + name + "\").", e);
		}
	}
	
	protected SortedSet<String> vPropertiesForAdmin = new TreeSet<String>();
	
	public List<String> getPropertiesForAdmin() {
		return new ArrayList<String>(vPropertiesForAdmin);
	}
	

	public Icon getIcon(int iconKind) {
		try {
			BeanInfo bi = Introspector.getBeanInfo(getClass());
			return new ImageIcon(bi.getIcon(iconKind));
		} catch(Exception e) {
			Engine.logBeans.error("Unable to get the bean icon.", e);
			return null;
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		try {
			Object clone = super.clone();
			((DatabaseObject) clone).isImporting = false;
			((DatabaseObject) clone).isSubLoaded = Engine.isEngineMode() ? isSubLoaded : false;
			((DatabaseObject) clone).hasChanged = Engine.isEngineMode() ? false : hasChanged;//Studio case of refresh without saving
			((DatabaseObject) clone).compilablePropertySourceValuesMap = new HashMap<String, Object>(5);
			((DatabaseObject) clone).compilablePropertySourceValuesMap.putAll(compilablePropertySourceValuesMap);
			return clone;
		} catch(Exception e) {
			Engine.logBeans.error("Unable to clone the object \"" + getName() + "\"", e);
			String message = "DatabaseObject.clone() " + e.getClass().getName() + "\n" + e.getMessage();
			throw new CloneNotSupportedException(message);
		}
	}

	public void add(DatabaseObject databaseObject) throws EngineException {
		databaseObject.setParent(this);
	}

	public void remove(DatabaseObject databaseObject) throws EngineException {
		databaseObject.parent = null;
	}

	public void delete() throws EngineException {
		boolean bResult = true;
		try {
			File dboFile = new File(Engine.PROJECTS_PATH + getQName());
			if (dboFile.exists()) {
				bResult = dboFile.delete();
			}
		}
		catch(Exception e) {
			throw new EngineException("Unable to delete the object \"" + getQName() + "\".", e);
		}

		if (!bResult) {
			throw new EngineException("Unable to delete the object \"" + getQName() + "\". The file does not exist or is probably locked by another application.");
		}
	}

	public transient boolean hasChanged = false;

	public transient boolean bNew = false;

	/**
	 * The type of the database object. This type is used for storing
	 * the object into the database.
	 */
	String databaseType = "undefined";

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
		return parent;
	}

	public Project getProject() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof Project)) {
			databaseObject = databaseObject.getParent();
		}
		return (Project) databaseObject;
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
		parent = databaseObject;
	}

	/**
	 * Returns the path for writing the object in the Convertigo database repository; it
	 * always contains the leading "/" character, but not the trailing one.
	 *
	 * @return the path for writing the object.
	 */
	public abstract String getPath();

	protected String getOldPath() {
		return getPath();
	}

	/**
	 * Returns the file name of the object.
	 *
	 * @return the file name of the object.
	 */
	public String getFileName() {
		return computeFileName() + ".xml";        
	}

	protected String getOldFileName() {
		return computeOldFileName() + ".xml";
	}

	// My 64 valid Base64 values 
	private final static char[] ALPHABET = {
		(char) 'A', (char) 'B', (char) 'C', (char) 'D', (char) 'E', (char) 'F', (char) 'G',
		(char) 'H', (char) 'I', (char) 'J', (char) 'K', (char) 'L', (char) 'M', (char) 'N',
		(char) 'O', (char) 'P', (char) 'Q', (char) 'R', (char) 'S', (char) 'T', (char) 'U',
		(char) 'V', (char) 'W', (char) 'X', (char) 'Y', (char) 'Z',
		(char) 'a', (char) 'b', (char) 'c', (char) 'd', (char) 'e', (char) 'f', (char) 'g',
		(char) 'h', (char) 'i', (char) 'j', (char) 'k', (char) 'l', (char) 'm', (char) 'n',
		(char) 'o', (char) 'p', (char) 'q', (char) 'r', (char) 's', (char) 't', (char) 'u',
		(char) 'v', (char) 'w', (char) 'x', (char) 'y', (char) 'z',
		(char) '0', (char) '1', (char) '2', (char) '3', (char) '4',
		(char) '5', (char) '6', (char) '7', (char) '8', (char) '9',
		(char) '_', (char) '-'
	};

	private transient String computedFileName = null;
	public transient String oldComputedFileName = null;
	
	protected String computeFileName() {
		if (computedFileName == null) {
			long hashcode = Math.abs(name.hashCode());
			long len = name.length();
			
			long checksum = 0;
			for (int i = 1 ; i < len ; i++) {
				checksum += ((byte) name.charAt(i)) - 32;
			}
			
			len = name.length();
			hashcode = (hashcode << 12) | (len + checksum);
			
			computedFileName = "";
			
			long reminder = hashcode;
			int mod;
			//int pow = 0;
			while (reminder != 0) {
				mod = (int) (reminder % 64);
				
				if (mod == 0) {
					//pow = (int) reminder / 64;
					computedFileName += ALPHABET[0];
				} 
				else {
					computedFileName += ALPHABET[mod];
				} 
				reminder = (reminder - mod) / 64;
				//pow++;
			}
		}
		return computedFileName;
	}

	protected String computeOldFileName() {
		return oldComputedFileName;
	}
	/**
	 * Returns the fully qualified name of the object, i.e. with the object path, for
	 * writing purposes.
	 *
	 * @return the fully qualified name of the object.
	 */
	public String getQName() {
		return getPath() + "/" + getFileName();
	}

	public String getOldQName() {
		return getOldPath() + "/" + getOldFileName();
	}

	/**
	 * The priority of object.
	 */
	public long priority = 0;

	/**
	 * The object name.
	 */
	protected String name = "new object";

	public transient String oldName = null;
	/**
	 * Retrieves the object name.
	 *
	 * @return the object name.
	 */
	public String getName() {
		return name;
	}

	public String getOldName() {
		return oldName;
	}

	/**
	 * Sets the bean name and computes file name.
	 *
	 * @param name the bean name.
	 */
	private void setBeanName(String name) throws EngineException {
		if (name.length() == 0) {
			throw new EngineException("The object name cannot be empty!");
		}
		this.name = name;
		computedFileName = null;
		computeFileName();
	}

	/**
	 * Sets the object name.
	 *
	 * @param name the object name.
	 */
	public void setName(String name) throws EngineException {
		if (name == null) {
			throw new EngineException("The object name cannot be null!");
		}
		if (name.length() == 0) {
			throw new EngineException("The object name cannot be empty!");
		}
		
		// warns if bean name is not normalized
		if (!name.equals(StringUtils.normalize(name))) {
			Engine.logBeans.warn("Unnormalized name : \"" + name + "\" for databaseObject (" + getClass().getSimpleName() + ")");
		}
		
		// get old name
		oldName = ((oldName == null) ? name : this.name);
		
		// set new name and new computed file name
		setBeanName(name);
		
		// get old computed file name
		oldComputedFileName = ((oldComputedFileName == null) ? computedFileName : oldComputedFileName);
	}

	/**
	 * Returns an available name for a new child database object to add.
	 * 
	 * @param eDatabaseObjects an enumeration of children database objects
	 * @param dboName the name of a new child database object
	 * @return
	 */
	public String getChildBeanName(Collection<? extends DatabaseObject> v,String dboName, boolean isNew) {
		String newDatabaseObjectName = dboName;
		boolean bContinue = isNew;
		int index = 0;
		while (bContinue) {
			if (index == 0) {
				newDatabaseObjectName = dboName;
			} else {
				newDatabaseObjectName = dboName + index;
			}
			try {
				for (DatabaseObject databaseObject : v) {
					if (newDatabaseObjectName.equals(databaseObject.getName())) {
						throw new ObjectWithSameNameException("Unable to add the object \"" + newDatabaseObjectName + "\" because an object with the same name already exists.");
					}
				}
				bContinue = false;
			} catch (ObjectWithSameNameException e) {
				index++;
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

	synchronized public long getNewOrderValue() {
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
	public Object getOrder(Object object) throws EngineException	{
		if (object instanceof DatabaseObject) {
			return ((DatabaseObject) object).getOrderedValue();
		}
		return object.toString();
	}

	@Override
	public String toString() {
		return name;
	}

	public transient long newPriority = 0;

	public String toXml() throws EngineException {
		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		
		ProcessingInstruction pi = document.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"ISO-8859-1\"");
		document.appendChild(pi);
		
		Element rootElement = toXml(document);
		document.appendChild(rootElement);
		
		return XMLUtils.prettyPrintDOM(document);
	}

	public Element toXml(Document document) throws EngineException {    	
		Element element = document.createElement(getDatabaseType().toLowerCase());
		
		element.setAttribute("classname", getClass().getName());
		if (exportOptions.contains(ExportOption.bIncludeVersion)) {
			element.setAttribute("version", com.twinsoft.convertigo.beans.Version.version);
		}
		
		// Storing the database object priority
		element.setAttribute("priority", new Long(priority).toString());
		
		int len;
		PropertyDescriptor[] propertyDescriptors;
		PropertyDescriptor propertyDescriptor;
		Element propertyElement;
		
		try {
			BeanInfo bi = Introspector.getBeanInfo(getClass());
			propertyDescriptors = bi.getPropertyDescriptors();
			len = propertyDescriptors.length;
			if (exportOptions.contains(ExportOption.bIncludeDisplayName)) {
				element.setAttribute("displayName", bi.getBeanDescriptor().getDisplayName()); 
			}
		} catch (IntrospectionException e) {
			throw new EngineException("Couldn't introspect the bean \"" + getName() + "\"", e);
		}
		
		for (int i = 0 ; i < len ; i++) {
			propertyDescriptor = propertyDescriptors[i];
			String name = propertyDescriptor.getName();
			String displayName = propertyDescriptor.getDisplayName();
			String shortDescription=propertyDescriptor.getShortDescription();
			
			Method getter = propertyDescriptor.getReadMethod();
			
			// Only analyze read propertyDescriptors.
			if (getter == null) {
				continue;
			}
			
			try {
				// Storing the database object bean properties
				Object uncompiledValue = getCompilablePropertySourceValue(name);
				Object compiledValue = null;
				Object cypheredValue = null;
				Object value;
				
				if (uncompiledValue == null) {
					Object args[] = { };
					value = getter.invoke(this, args);            		
				} else {
					value = uncompiledValue;
					compiledValue = getCompiledValue(value);            		
				}
				
				// Only write non-null values
				if (value == null) {
					Engine.logBeans.warn("Attempting to store null property (\"" + name + "\"); skipping...");
					continue;
				}
				
				propertyElement = document.createElement("property");
				propertyElement.setAttribute("name", name);
				
				// Encrypts value if needed
				if (isCipheredProperty(name) && !exportOptions.contains(ExportOption.bIncludeDisplayName)) {
					cypheredValue = encryptPropertyValue(value);
					if (!value.equals(cypheredValue)) {
						value = cypheredValue;
						propertyElement.setAttribute("ciphered", "true");
					}
				}
				
				// Stores the value
				Node node = XMLUtils.writeObjectToXml(document, value);
				if (exportOptions.contains(ExportOption.bIncludeCompiledValue)) {
					node = XMLUtils.writeObjectToXml(document, value, compiledValue);
				}
				propertyElement.appendChild(node);
				
				// Add visibility for logs
				if (!isTraceableProperty(name)) {
					propertyElement.setAttribute("traceable", "false");
				}
				
				if (exportOptions.contains(ExportOption.bIncludeBlackListedElements)) {
					Object propertyDescriptorBlackListValue=propertyDescriptor.getValue(MySimpleBeanInfo.BLACK_LIST_NAME);
					if (propertyDescriptorBlackListValue != null && (Boolean) propertyDescriptorBlackListValue) {
						propertyElement.setAttribute("blackListed", "blackListed");
					}
				}
				if (exportOptions.contains(ExportOption.bIncludeDisplayName)) {
					propertyElement.setAttribute("displayName", displayName);
					propertyElement.setAttribute("isHidden", Boolean.toString(propertyDescriptor.isHidden()));
					propertyElement.setAttribute("isMasked", isMaskedProperty(Visibility.Platform, name) ? "true" : "false");
				}
				if (exportOptions.contains(ExportOption.bIncludeShortDescription)) {
					propertyElement.setAttribute("shortDescription", shortDescription);
				}
				
				if (exportOptions.contains(ExportOption.bIncludeEditorClass)) {
					String message="";
					if (propertyDescriptor.getPropertyEditorClass() != null) {
						message = propertyDescriptor.getPropertyEditorClass().toString().replaceFirst("(.)*\\.", "");
					} else {
						message="null";
					}
					if (this instanceof ITagsProperty) {
						ITagsProperty tagsProperty = (ITagsProperty) this;
						String[] sResults=null;
						try{
							sResults = tagsProperty.getTagsForProperty(name);
						}catch(Exception ex){
							sResults = new String[0];
						}
						if (sResults.length > 0) {
							Element possibleValues = document.createElement("possibleValues");
							Element possibleValue = null;
							for (int j = 0 ; j < sResults.length ; j++) {
								possibleValue = document.createElement("value");
								possibleValue.setTextContent(sResults[j]);
								possibleValues.appendChild(possibleValue);
							}
							propertyElement.appendChild(possibleValues);
						}
					}
					propertyElement.setAttribute("editorClass", message);
				}
				
				element.appendChild(propertyElement);
				
				if (Boolean.TRUE.equals(propertyDescriptor.getValue("nillable"))) {
					try {
						Method method = this.getClass().getMethod("isNullProperty", new Class[] { String.class });
						Object isNull = method.invoke(this, new Object[] { name });
						propertyElement.setAttribute("isNull", isNull.toString());
					} catch (Exception ex) {
						Engine.logBeans.error("[Serialization] Skipping 'isNull' attribute for property \"" + name + "\".", ex);
					}
				}
			} catch (Exception e) {
				Engine.logBeans.error("[Serialization] Skipping property \"" + name + "\".", e);
			}
		}
		
		return element;
	}

	public Element toXml(Document document, ExportOption... exportOptions) throws EngineException {
		try {
			this.exportOptions.addAll(Arrays.asList(exportOptions));
			return toXml(document);
		} finally {
			this.exportOptions.clear();
		}
	}

	/**
	 * Performs custom configuration on the XML document for this database object
	 * BEFORE its de-serialization.
	 */
	public void preconfigure(Element element) throws Exception {
		// Do nothing by default
	}

	/**
	 * Performs custom configuration on the database object
	 * AFTER its de-serialization.
	 */
	public void configure(Element element) throws Exception {
		// Do nothing by default
	}

	/**
	 * Reads the object from XML serialized data.
	 */
	public static DatabaseObject read(String serializationData) throws EngineException {
		Element rootElement = null;
		try {
			Charset cs = Charset.forName("ISO-8859-1");
			ByteBuffer bb =  cs.encode(serializationData);
			byte[] xmlSerializationData = bb.array();
			
			Document document = XMLUtils.getDefaultDocumentBuilder().parse(new ByteArrayInputStream(xmlSerializationData));
			
			rootElement = document.getDocumentElement();
		} catch(Exception e) {
			throw new EngineException("Unable to create the object from the serialized data.", e);
		}
		
		return DatabaseObject.read(rootElement);
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
		if (VersionUtils.compareProductVersion(Version.productVersion, version) < 0) {
			String message = "Unable to create an object of product version superior to the current beans product version (" +
					com.twinsoft.convertigo.beans.Version.version + ").\n" +
					"Object class: " + objectClassName + "\n" +
					"Object version: " + version;
			EngineException ee = new EngineException(message);
			throw ee;
		}
		
		try {
			Engine.logBeans.trace("Creating object of class \"" + objectClassName + "\"");
			databaseObject = (DatabaseObject) Class.forName(objectClassName).newInstance();
		} catch(Exception e) {
			String s = node.getNodeName();//XMLUtils.prettyPrintDOM(node);
			String message = "Unable to create a new instance of the object from the serialized XML data.\n" +
					"Object class: '" + objectClassName + "'\n" +
					"Object version: " + version + "\n" +
					"XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
		
		try {
			// Performs custom configuration before object de-serialization
			databaseObject.preconfigure(element);
		} catch(Exception e) {
			String s = XMLUtils.prettyPrintDOM(node);
			String message = "Unable to configure the object from serialized XML data before its creation.\n" +
					"Object class: '" + objectClassName + "'\n" +
					"XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
		
		try {
			long priority = new Long(element.getAttribute("priority")).longValue();
			databaseObject.priority = priority;
			
			Class<?> databaseObjectClass = databaseObject.getClass();
			BeanInfo bi = Introspector.getBeanInfo(databaseObjectClass);
			PropertyDescriptor[] pds = bi.getPropertyDescriptors();
			
			NodeList childNodes = element.getChildNodes();
			int len = childNodes.getLength();
			
			Node childNode;
			PropertyDescriptor pd;
			Object propertyObjectValue;
			Class<?> propertyType;
			NamedNodeMap childAttributes;
			boolean maskValue = false;
			
			for (int i = 0 ; i < len ; i++) {
				childNode = childNodes.item(i);
				if (childNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				childNodeName = childNode.getNodeName();
				
				Engine.logBeans.trace("Analyzing node '" + childNodeName + "'");
				
				if (childNodeName.equalsIgnoreCase("property")) {
					childAttributes = childNode.getAttributes();
					propertyName = childAttributes.getNamedItem("name").getNodeValue();
					Engine.logBeans.trace("  name = '" + propertyName + "'");
					pd = findPropertyDescriptor(pds, propertyName);
					if (pd == null) {
						Engine.logBeans.warn("Unable to find the definition of property \"" + propertyName + "\"; skipping.");
						continue;
					}
					propertyType = pd.getPropertyType();
					propertyObjectValue = XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(childNode, Node.ELEMENT_NODE));
					
					// Hides value in log trace if needed
					try {
						if ("false".equals(childAttributes.getNamedItem("traceable").getNodeValue())) {
							maskValue = true;
						}
					} catch (Exception e) {}
					Engine.logBeans.trace("  value='" + (maskValue ? Visibility.maskValue(propertyObjectValue):propertyObjectValue) + "'");
					
					// Decrypts value if needed
					try {
						if ("true".equals(childAttributes.getNamedItem("ciphered").getNodeValue())) {
							propertyObjectValue = decryptPropertyValue(propertyObjectValue);
						}
					} catch (Exception e) {}
					
					propertyObjectValue = compileProperty(databaseObject, propertyType, propertyName, propertyObjectValue);
					propertyValue = propertyObjectValue.toString();
					
					if ((propertyType == int.class) || (propertyType == Integer.class)) {
						propertyObjectValue = new Integer(propertyValue);
					} else if ((propertyType == double.class) || (propertyType == Double.class)) { 
						propertyObjectValue = new Double(propertyValue);
					} else if ((propertyType == byte.class) || (propertyType == Byte.class)) {
						propertyObjectValue = new Byte(propertyValue);
					} else if ((propertyType == short.class) || (propertyType == Short.class)) {
						propertyObjectValue = new Short(propertyValue);
					} else if ((propertyType == long.class) || (propertyType == Long.class)) {
						propertyObjectValue = new Long(propertyValue);
					} else if ((propertyType == float.class) || (propertyType == Float.class)) {
						propertyObjectValue = new Float(propertyValue);
					} else if ((propertyType == double.class) || (propertyType == Double.class)) {
						propertyObjectValue = new Double(propertyValue);
					}
					
					Method setter = pd.getWriteMethod();
					Engine.logBeans.trace("  setter='" + setter.getName() + "'");
					Engine.logBeans.trace("  param type='" + propertyObjectValue.getClass().getName() + "'");
					Engine.logBeans.trace("  expected type='" + propertyType.getName() + "'");
					try {
						setter.invoke(databaseObject, new Object[] { propertyObjectValue });
					} catch(InvocationTargetException e) {
						Throwable targetException = e.getTargetException();
						Engine.logBeans.warn("Unable to set the property '" + propertyName + "' for the object '" + databaseObject.getName() + "' (" + objectClassName + "): [" + targetException.getClass().getName() + "] " + targetException.getMessage());
					}
					
					if (Boolean.TRUE.equals(pd.getValue("nillable"))) {
						Node nodeNull = childAttributes.getNamedItem("isNull");
						String valNull = (nodeNull == null) ? "false" : nodeNull.getNodeValue();
						Engine.logBeans.trace("  treats as null='" + valNull + "'");
						try {
							Method method = databaseObject.getClass().getMethod("setNullProperty", new Class[] { String.class, Boolean.class });
							method.invoke(databaseObject, new Object[] { propertyName, Boolean.valueOf(valNull) });
						} catch (Exception ex) {
							Engine.logBeans.warn("Unable to set the 'isNull' attribute for property '" + propertyName + "' of '" + databaseObject.getName() + "' object");
						}
					}
				}
			}
		} catch(Exception e) {
			String message = "Unable to set the object properties from the serialized XML data.\n" +
					"Object class: '" + objectClassName + "'\n" +
					"XML analyzed node: " + childNodeName + "\n" +
					"Property name: " + propertyName + "\n" +
					"Property value: " + propertyValue;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
		
		try {
			// Performs custom configuration
			databaseObject.configure(element);
		} catch(Exception e) {
			String s = XMLUtils.prettyPrintDOM(node);
			String message = "Unable to configure the object from serialized XML data after its creation.\n" +
					"Object class: '" + objectClassName + "'\n" +
					"XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
		
		return databaseObject;
	}

	public static boolean valueIsCompilable(Object propertyObjectValue) {
		if (propertyObjectValue instanceof String) {
			return (propertyObjectValue.toString().indexOf("${") != -1);
		} else if (propertyObjectValue instanceof XMLVector<?>) {
			XMLVector<Object> xmlv = new XMLVector<Object>(GenericUtils.<XMLVector<Object>>cast(propertyObjectValue));
			for (int i = 0; i<xmlv.size();i++) {
				Object ob = xmlv.get(i);
				if (valueIsCompilable(ob)) {
					return true;
				}
			}
		}
		return false;
	}

	public static Object compileProperty(DatabaseObject databaseObject, String propertyName, Object propertyObjectValue) {
		return compileProperty(databaseObject, String.class, propertyName, propertyObjectValue);
	}

	public static Object compileProperty(DatabaseObject databaseObject, Class<?> propertyType, String propertyName, Object propertyObjectValue) {
		// This a property that does not need to be compiled; remove source value if any
		if (!valueIsCompilable(propertyObjectValue)) {
			databaseObject.removeCompilablePropertySourceValue(propertyName);
			return propertyObjectValue;
		}

		// Update source value and retrieve compiled value
		databaseObject.setCompilablePropertySourceValue(propertyName, propertyObjectValue);
		Engine.logBeans.trace("  source value='" + propertyObjectValue.toString() + "'");
		Object compiledValue = getCompiledValue(propertyType, propertyObjectValue);
		return compiledValue;
	}

	public static Object getCompiledValue(Class<?> propertyType, Object propertyObjectValue) {
		Object compiledValue = getCompiledValue(propertyObjectValue);
		if ((compiledValue instanceof String) && (propertyType != String.class)) {
			compiledValue = "".equals(compiledValue) ? "80" : compiledValue;
		}
		return compiledValue;
	}

	private static Object getCompiledValue(Object propertyObjectValue) {
    	if (propertyObjectValue instanceof String) {
			String sPropertyObjectValue = propertyObjectValue.toString();
			String compiledObjectValue = "";
			String symbolCompiledValue=null;
			try {
				int i = sPropertyObjectValue.indexOf("${");
				int k = 0;
				while ((i != -1) && (i < sPropertyObjectValue.length())) {
					compiledObjectValue += sPropertyObjectValue.substring(k, i);
					int j = sPropertyObjectValue.indexOf("}", i + 3);
					while (j + 1 < sPropertyObjectValue.length()) {
						if (sPropertyObjectValue.charAt(j + 1) == '}') {
							j = sPropertyObjectValue.indexOf("}", j + 2);
						} else {
							break;
						}
					}
					if (j == -1) {
						Engine.logBeans.error("Syntax error: " + sPropertyObjectValue);
						return propertyObjectValue;
					} else {
						String symbol = sPropertyObjectValue.substring(i + 2, j);
						int idxEqual = symbol.indexOf('=');
						String symbolName = idxEqual == -1 ? symbol : symbol.substring(0, idxEqual);
						String symbolDefaultValue = idxEqual == -1 ? null : symbol.substring(idxEqual + 1).replaceAll("\\}\\}", "}");
						symbolCompiledValue = Engine.theApp.databaseObjectsManager.getSymbolValue(symbolName);
						if (symbolDefaultValue == null) {
							if (System.getProperties().containsKey("convertigo_global_symbols")) {
								if (symbolCompiledValue == null){
									Engine.logBeans.warn("No symbol " + symbolName + " has been defined in " + System.getProperty("convertigo_global_symbols"));
								}
							} else {
								Engine.logBeans.error("No file has been defined in JVM argument for the property \"convertigo_global_symbols\"");
							}
						}
						
						if (symbolDefaultValue == null) {
							symbolDefaultValue = "";
						}
						if (symbolCompiledValue == null) {
							symbolCompiledValue = symbolDefaultValue;
						}
						
						compiledObjectValue += symbolCompiledValue;
						
						i = j + 1;
					}
					
					k = i;
					if (i < sPropertyObjectValue.length()) {
						i = sPropertyObjectValue.indexOf("${", i);
					}
				}
				
				// Add end of string
				if (k < sPropertyObjectValue.length()) {
					compiledObjectValue += sPropertyObjectValue.substring(k);
				}
				
				return compiledObjectValue;
			} catch (StringIndexOutOfBoundsException e) {
				// Ignore: syntax error
			}
		} else if (propertyObjectValue instanceof XMLVector<?>) {
			try {
				XMLVector<Object> xmlv = new XMLVector<Object>(GenericUtils.<XMLVector<Object>>cast(propertyObjectValue));
				for (int i=0; i<xmlv.size();i++) {
					Object ob = xmlv.get(i);
					xmlv.set(i,getCompiledValue(ob));
				}
				return xmlv;
			} catch (Exception e) {}
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

	public static Object encryptPropertyValue(Object propertyValue) {
		if (propertyValue == null) {
			return null;
		}
		
		Object encryptedValue = null;
		if (propertyValue instanceof String) {
			encryptedValue = Crypto.encodeToHexString3((String)propertyValue);
		} else if (propertyValue instanceof XMLVector<?>) {
			try {
				XMLVector<Object> xmlv = new XMLVector<Object>(GenericUtils.<XMLVector<Object>>cast(propertyValue));
				for (int i = 0; i < xmlv.size(); i++) {
					Object ob = xmlv.get(i);
					xmlv.set(i,encryptPropertyValue(ob));
				}
				encryptedValue = xmlv;
			} catch (Exception e) {}
		}
		
		if (encryptedValue == null) {
			encryptedValue = propertyValue;
		}
		return encryptedValue;
	}
	
	public static Object decryptPropertyValue(Object encryptedValue) {
		if (encryptedValue == null) {
			return null;
		}
		
		Object propertyValue = null;
		if (encryptedValue instanceof String) {
			propertyValue = Crypto.decodeFromHexString3((String) encryptedValue);
		} else if (encryptedValue instanceof XMLVector<?>) {
			try {
				XMLVector<Object> xmlv = new XMLVector<Object>(GenericUtils.<XMLVector<Object>>cast(encryptedValue));
				for (int i = 0; i < xmlv.size(); i++) {
					Object ob = xmlv.get(i);
					xmlv.set(i,decryptPropertyValue(ob));
				}
				propertyValue = xmlv;
			} catch (Exception e) {}
		}
		
		if (propertyValue == null) {
			propertyValue = encryptedValue;
		}
		return propertyValue;
	}
	
	/**
	 * Reloads the object from XML serialized data.
	 */
	public void reload(String serializationData) throws EngineException {
		Element rootElement = null;
		try {
			Engine.logBeans.trace("[DatabaseObject.reload()] serializationData:\n" + serializationData);
			
			DocumentBuilder documentBuilder = XMLUtils.getDefaultDocumentBuilder();
			Document document = documentBuilder.parse(new ByteArrayInputStream(serializationData.getBytes()));
			
			rootElement = document.getDocumentElement();
		} catch(Exception e) {
			throw new EngineException("Unable to reload the object from the serialized data.", e);
		}
		
		reload(rootElement);
	}

	public void reload(Node node) throws EngineException {
		String objectClassName = "n/a";
		String childNodeName = "n/a";
		String propertyName = "n/a";
		String propertyValue = "n/a";
		DatabaseObject databaseObject = null;
		Element element = (Element) node;
		
		objectClassName = element.getAttribute("classname");
		
		String version = element.getAttribute("version");
		if (VersionUtils.compareProductVersion(Version.version, version) < 0) {
			String message = "Unable to reload from an object of product version superior to the current beans product version (" +
					com.twinsoft.convertigo.beans.Version.version + ").\n" +
					"Object class: " + objectClassName + "\n" +
					"Object version: " + version;
			EngineException ee = new EngineException(message);
			throw ee;
		}
		
		try {
			Engine.logBeans.trace("Creating object " + objectClassName);
			databaseObject = (DatabaseObject) Class.forName(objectClassName).newInstance();
		} catch(Exception e) {
			String message = "Unable to create a new instance of the object from the serialized XML data." +
					"Object class: " + objectClassName + "\n" +
					"Object version: " + version;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
		
		try {
			// Performs custom configuration before object de-serialization
			databaseObject.preconfigure(element);
		} catch(Exception e) {
			String s = XMLUtils.prettyPrintDOM(node);
			String message = "Unable to configure the object from serialized XML data before its creation.\n" +
					"Object class: " + objectClassName + "\n" +
					"XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
		
		try {
			long priority = new Long(element.getAttribute("priority")).longValue();
			this.priority = priority;
			
			Class<?> databaseObjectClass = this.getClass();
			BeanInfo bi = Introspector.getBeanInfo(databaseObjectClass);
			PropertyDescriptor[] pds = bi.getPropertyDescriptors();
			
			NodeList childNodes = element.getChildNodes();
			int len = childNodes.getLength();
			
			Node childNode;
			PropertyDescriptor pd;
			Object propertyObjectValue;
			Class<?> propertyType;
			NamedNodeMap childAttributes;
			
			for (int i = 0 ; i < len ; i++) {
				childNode = childNodes.item(i);
				
				if (childNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				
				childNodeName = childNode.getNodeName();
				
				Engine.logBeans.trace("Analyzing node '" + childNodeName + "'");
				
				if (childNodeName.equalsIgnoreCase("property")) {
					
					childAttributes = childNode.getAttributes();
					propertyName = childAttributes.getNamedItem("name").getNodeValue();
					Engine.logBeans.trace("  name = '" + propertyName + "'");
					pd = findPropertyDescriptor(pds, propertyName);
					
					if (pd == null) {
						Engine.logBeans.warn("Unable to find the definition of property \"" + propertyName + "\"; skipping.");
						continue;
					}
					
					propertyType = pd.getPropertyType();
					propertyObjectValue = XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(childNode, Node.ELEMENT_NODE));
					Engine.logBeans.trace("  value='" + propertyObjectValue.toString() + "'");
					propertyObjectValue = compileProperty(databaseObject, propertyType, propertyName, propertyObjectValue);
					
					Method setter = pd.getWriteMethod();
					Engine.logBeans.trace("  setter='" + setter.getName() + "'");
					Engine.logBeans.trace("  param type='" + propertyObjectValue.getClass().getName() + "'");
					Engine.logBeans.trace("  expected type='" + propertyType.getName() + "'");
					setter.invoke(this, new Object[] { propertyObjectValue });
				}
			}
		} catch(Exception e) {
			String message = "Unable to set the object properties from the serialized XML data." +
					"Object class: " + objectClassName + "\n" +
					"XML analyzed node: " + childNodeName + "\n" +
					"Property name: " + propertyName + "\n" +
					"Property value: " + propertyValue;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
		
		try {
			// Performs custom configuration
			this.configure(element);
		} catch(Exception e) {
			String s = XMLUtils.prettyPrintDOM(node);
			String message = "Unable to configure the object from serialized XML data after its creation.\n" +
					"Object class: " + objectClassName + "\n" +
					"XML data:\n" + s;
			EngineException ee = new EngineException(message, e);
			throw ee;
		}
	}

	protected static PropertyDescriptor findPropertyDescriptor(PropertyDescriptor[] pds, String pn) {
		int len = pds.length;
		PropertyDescriptor pd;
		for (int i = 0 ; i < len ; i++) {
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
		String databaseObjectQName = getQName();
		write(databaseObjectQName);
	}

	/**
	 * Writes the object to the Convertigo repository database.
	 */
	public void write(String databaseObjectQName) throws EngineException {
		String fileName = Engine.PROJECTS_PATH + databaseObjectQName;
		
		try {
			Engine.logBeans.trace("Trying to write the object " + databaseObjectQName + "[" + getName() + "]...");
			
			// The parent must have been saved!          
			if ((parent != null) && ((parent.bNew) || (parent.hasChanged && !isImporting))) {
				throw new EngineException("The parent object (\"" + parent.getName() + "\") has not been saved; save it first!");
			}
			
			// Verify if there is a conflict of qname
			File file = new File(fileName);
			if (bNew) {
				if (file.exists()) {
					throw new EngineException("A file with the same qname (\"" + databaseObjectQName + "\") already exists! Try to change the name of the object.");
				}
			}
			
			// Create directories
			if (!file.exists()) {
				File dir = file.getParentFile();
				if (!dir.exists()) {
					Engine.logBeans.trace("Creating directories... " + dir.mkdirs());
				}
			}
			
			// Write the object
			FileOutputStream fos = new FileOutputStream(file);
			
			String xmlSerializationData = toXml();
			Charset cs = Charset.forName("ISO-8859-1");
			ByteBuffer bb = cs.encode(xmlSerializationData);
			fos.write(bb.array());
			fos.close();
			
			oldName = name;
			oldComputedFileName = computedFileName;
			hasChanged = false;
			bNew = false;
			
			Engine.logBeans.trace("The object \"" + databaseObjectQName + "\" has been written.");
		} catch(Exception e) {
			if (e instanceof EngineException) {
				throw (EngineException)e;
			} else {
				throw new EngineException("Unable to write the object \"" + databaseObjectQName + "\".", e);
			}
		}
	}

	/** Holds value of property comment. */
	private String comment = "";

	/** Getter for property comment.
	 * @return Value of property comment.
	 */
	public String getComment() {
		return this.comment;
	}

	/** Setter for property comment.
	 * @param comment New value of property comment.
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	transient public boolean isSubLoaded = false;

	synchronized public void checkSubLoaded(){
		if (bNew) {
			return;
		}
		
		if (isImporting) {
			return;
		}
		
		if (!isSubLoaded) {
			isSubLoaded = true;
			try {
				Engine.theApp.databaseObjectsManager.getSubDatabaseObjects(this);
			} catch (EngineException e) {
				Engine.logBeans.error("(DatabaseObject) getSubDatabaseObjects failed with EngineException !", e);
			} catch (DatabaseObjectNotFoundException e) {
				Engine.logBeans.error("(DatabaseObject) getSubDatabaseObjects failed with DatabaseObjectNotFoundException !", e);
			}
		}
	}

	public long getIdentity() {
		return identity;
	}

	protected <E extends DatabaseObject> List<E> sort(List<E> list){
		return sort(list, true);
	}

	protected <E extends Object> List<E> sort(List<E> list, boolean ascending){
		Collections.sort(list, new Comparator<Object>() {
			@SuppressWarnings("unchecked")
			public int compare(Object o1, Object o2) {
				try {
					return ((Comparable<Object>) getOrder(o1)).compareTo(getOrder(o2));
				} catch (EngineException e) {
					return 0;
				}
			}
		});
		List<E> res = new ArrayList<E>(list);
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

	public String[] getNamesWithFirstEmptyItem(Collection<? extends DatabaseObject> dbos){
		String[] res = new String[dbos.size() + 1];
		res[0] = "";
		int i = 1;
		for (DatabaseObject dbo : dbos) {
			res[i++] = dbo != null ? dbo.getName() : "";
		}
		return res;
	}

	public <E extends DatabaseObject> List<E> getAllChildren(){
		return new Vector<E>();
	}
}