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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.1.3/Studio/src/com/twinsoft/convertigo/beans/BeansDoc.java $
 * $Author: nicolasa $
 * $Revision: 30608 $
 * $Date: 2012-05-29 14:40:29 +0200 (mar., 29 mai 2012) $
 */

package com.twinsoft.convertigo.beans;
 
import java.beans.BeanDescriptor;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.Engine;

public class CheckBeans {

	private enum Error {
		//NON_DATABASE_OBJECT("Non database object"),
		MISSING_BEAN_INFO("Missing bean info"),
		ABSTRACT_CLASS_WITH_ICON("Abstract class should not have icon"),
		ABSTRACT_CLASS_WITH_DISPLAY_NAME("Abstract class should not have a display name"),
		ABSTRACT_CLASS_WITH_DESCRIPTION("Abstract class should not have a description"),
		BEAN_DEFINED_BUT_NOT_USED("Bean defined but not used"),
		BEAN_MISSING_DISPLAY_NAME("Bean missing display name"),
		BEAN_MISSING_DESCRIPTION("Bean missing description"),
		BEAN_MISSING_ICON("Declared icon missing"),
		BEAN_ICON_NAMING_POLICY("Wrong icon name "),
		BEAN_ICON_NOT_USED("Not used icon"),
		PROPERTY_DECLARED_BUT_NOT_FOUND("Declared property but not found"),
		PROPERTY_NAMING_POLICY("Declared and defined bean property name mismatch"),
		PROPERTY_NOT_PRIVATE("Non private bean property"),
		PROPERTY_TRANSIENT("Bean property should not be transient"),
		GETTER_SETTER_DECLARED_BUT_NOT_FOUND("Declared getter or setter but not found"),
		GETTER_SETTER_DECLARED_EXPECTED_NAMES_MISMATCH("Declared and expected getter and/or setter mismatch"),
		FIELD_NOT_TRANSIENT("Field not transient");
		
		private String label;
		
		Error(String label) {
			this.label = label;
		}
		
		public String toString() {
			return label;
		}
		
		public void add(String errorMessage) {
			List<String> errorList = errors.get(this);
			if (errorList == null) {
				errorList = new ArrayList<String>();
				errors.put(this, errorList);
			}
			errorList.add(errorMessage);
		}
	}
	
	private static String srcBase;
	
	private static List<String> dboXmlDeclaredDatabaseObjects;

	private static DocumentBuilderFactory defaultDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder defaultDocumentBuilder;
	
	public static void main(String[] args) {
		Engine.logBeans = Logger.getLogger(BeansDoc.class);

		srcBase = args[0];

		System.out.println("Loading database objects XML DB in " + srcBase);
		try {
			defaultDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
			defaultDocumentBuilder = defaultDocumentBuilderFactory.newDocumentBuilder();

			dboXmlDeclaredDatabaseObjects = new ArrayList<String>();
			
			Document dboXmlDocument = defaultDocumentBuilder.parse(new FileInputStream(new File(srcBase + "/com/twinsoft/convertigo/beans/database_objects.xml")));
			NodeList beanList = dboXmlDocument.getElementsByTagName("bean");
			int n = beanList.getLength();
			for (int i = 0; i < n; i++) {
				Element element = (Element) beanList.item(i);
				dboXmlDeclaredDatabaseObjects.add(element.getAttribute("classname"));
			}
		} catch (Exception e) {
			System.out.println("Error while loading DBO XML DB");
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println("Browsing sources in " + srcBase);
		
		browsePackages(srcBase + "com/twinsoft/convertigo/beans");
		
		System.out.println();
		System.out.println("Found " + javaClassNames.size() + " classes");
		
		for (String javaClassName : javaClassNames) {
			analyzeJavaClass(javaClassName);
		}

		for (String icon : icons) {
			Error.BEAN_ICON_NOT_USED.add(icon);
		}

		System.out.println();
		
		for (Error error : Error.values()) {
			List<String> errorList = errors.get(error);
			if (errorList == null) continue;
			int nError = errorList.size();
			System.out.println(error + " (" + nError + ")");
			for (String errorMessage : errorList) {
				System.out.println("   " + errorMessage);
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("=======");
		System.out.println("Summary");
		System.out.println("=======");
		System.out.println();
		System.out.println("Found " + nBeanClass + " bean classes (including abstract classes)");
		System.out.println("Found " + nBeanClassNotAbstract + " instantiable bean classes");
		System.out.println();

		int nTotalError = 0;
		for (Error error : Error.values()) {
			List<String> errorList = errors.get(error);
			int nError = 0;
			if (errorList != null) nError = errorList.size();
			nTotalError += nError;
			System.out.println(error + ": found " + nError + " error(s)");
		}

		System.out.println();
		System.out.println("Found " + nTotalError + " error(s)");
		System.out.println();
		System.out.println("Beans check finished!");
		
		System.exit(nTotalError);
	}

	private static List<String> javaClassNames = new ArrayList<String>();
	private static List<String> icons = new ArrayList<String>();

	private static void browsePackages(final String currentPackage) {
		Collection<File> javaFiles = FileUtils.listFiles(new File(currentPackage), new String[] { "java" }, true);
		for (File file : javaFiles) {
			String javaClassName = file.getPath();
			javaClassName = javaClassName.substring(srcBase.length()).replace('/', '.').replace('\\', '.');
			javaClassName = javaClassName.substring(0, javaClassName.lastIndexOf(".java"));
			
			if (!javaClassName.endsWith("BeanInfo")) {
				javaClassNames.add(javaClassName);
			}
		}

		Collection<File> iconFiles = FileUtils.listFiles(new File(currentPackage), new String[] { "png" }, true);
		for (File file : iconFiles) {
			String iconPath = file.getPath();
			iconPath = iconPath.substring(srcBase.length() - 1);

			icons.add(iconPath);
		}
	}
	
	private static Map<Error, List<String>> errors = new HashMap<Error, List<String>>();

	private static int nBeanClass = 0;
	private static int nBeanClassNotAbstract = 0;
	
	private static void analyzeJavaClass(String javaClassName) {
		try {
			Class<?> javaClass = Class.forName(javaClassName);
			String javaClassSimpleName = javaClass.getSimpleName();
			
			if (!DatabaseObject.class.isAssignableFrom(javaClass)) {
				//Error.NON_DATABASE_OBJECT.add(javaClassName);
				return;
			}
			
			nBeanClass++;
			
			String dboBeanInfoClassName = javaClassName + "BeanInfo";
			MySimpleBeanInfo dboBeanInfo = null;
			try {
				dboBeanInfo = (MySimpleBeanInfo) (Class.forName(dboBeanInfoClassName)).newInstance();
			} catch (ClassNotFoundException e) {
				if (!Modifier.isAbstract(javaClass.getModifiers())) {
					Error.MISSING_BEAN_INFO.add(javaClassName + " (expected bean info: " + dboBeanInfoClassName + ")");
				}
				return;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
			BeanDescriptor beanDescriptor = dboBeanInfo.getBeanDescriptor();
			
			// Check abstract class
			if (Modifier.isAbstract(javaClass.getModifiers())) {
				// Check icon (16x16)
				String declaredIconName = MySimpleBeanInfo.getIconName(dboBeanInfo, MySimpleBeanInfo.ICON_COLOR_16x16);
				if (declaredIconName != null) {
					Error.ABSTRACT_CLASS_WITH_ICON.add(javaClassName);
				}

				// Check icon (32x32)
				declaredIconName = MySimpleBeanInfo.getIconName(dboBeanInfo, MySimpleBeanInfo.ICON_COLOR_32x32);
				if (declaredIconName != null) {
					Error.ABSTRACT_CLASS_WITH_ICON.add(javaClassName);
				}

				// Check display name
				if (!beanDescriptor.getDisplayName().equals("?")) {
					Error.ABSTRACT_CLASS_WITH_DISPLAY_NAME.add(javaClassName);
				}

				// Check description
				if (!beanDescriptor.getShortDescription().equals("?")) {
					Error.ABSTRACT_CLASS_WITH_DESCRIPTION.add(javaClassName);
				}
			}
			else {
				nBeanClassNotAbstract++;
				
				// Check bean declaration in database_objects.xml
				if (!dboXmlDeclaredDatabaseObjects.contains(javaClassName)) {
					Error.BEAN_DEFINED_BUT_NOT_USED.add(javaClassName);
				}
				
				// Check icon name policy (16x16)
				String declaredIconName = MySimpleBeanInfo.getIconName(dboBeanInfo, MySimpleBeanInfo.ICON_COLOR_16x16);
				String expectedIconName = javaClassName.replace(javaClassSimpleName, "images/" + javaClassSimpleName);
				expectedIconName = "/" + expectedIconName.replace('.', '/') + "_color_16x16";
				expectedIconName = expectedIconName.toLowerCase() + ".png";
				if (declaredIconName != null) {
					if (!declaredIconName.equals(expectedIconName)) {
						Error.BEAN_ICON_NAMING_POLICY.add(javaClassName + "\n"
								+ "      Declared: " + declaredIconName + "\n"
								+ "      Expected: " + expectedIconName);
					}
				}
				
				// Check icon file (16x16)
				File iconFile = new File(srcBase + declaredIconName);
				if (!iconFile.exists()) {
					Error.BEAN_MISSING_ICON.add(javaClassName+ " - icon missing: " + declaredIconName);
				}
				else {
					icons.remove(declaredIconName);
				}
				
				// Check icon name policy (32x32)
				declaredIconName = MySimpleBeanInfo.getIconName(dboBeanInfo, MySimpleBeanInfo.ICON_COLOR_32x32);
				expectedIconName = javaClassName.replace(javaClassSimpleName, "images/" + javaClassSimpleName);
				expectedIconName = "/" + expectedIconName.replace('.', '/') + "_color_32x32";
				expectedIconName = expectedIconName.toLowerCase() + ".png";
				if (declaredIconName != null) {
					if (!declaredIconName.equals(expectedIconName)) {
						Error.BEAN_ICON_NAMING_POLICY.add(javaClassName + "\n"
								+ "      Declared: " + declaredIconName + "\n"
								+ "      Expected: " + expectedIconName);
					}
				}
				
				// Check icon file (32x32)
				iconFile = new File(srcBase + declaredIconName);
				if (!iconFile.exists()) {
					Error.BEAN_MISSING_ICON.add(javaClassName+ " - icon missing: " + declaredIconName);
				}
				else {
					icons.remove(declaredIconName);
				}
				
				// Check display name
				if (beanDescriptor.getDisplayName().equals("?")) {
					Error.BEAN_MISSING_DISPLAY_NAME.add(javaClassName);
				}

				// Check description
				if (beanDescriptor.getShortDescription().equals("?")) {
					Error.BEAN_MISSING_DESCRIPTION.add(javaClassName);
				}
			}
			
			// Check declared bean properties
			PropertyDescriptor[] propertyDescriptors = dboBeanInfo.getLocalPropertyDescriptors();
			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				String propertyName = propertyDescriptor.getName();
				try {
					javaClass.getDeclaredField(propertyName);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					try {
						// Try to find it in the upper classes
						javaClass.getField(propertyName);
					} catch (SecurityException e1) {
						// printStackTrace();
					} catch (NoSuchFieldException e1) {
						Error.PROPERTY_DECLARED_BUT_NOT_FOUND.add(javaClassName + ": " + propertyName);
					}
				}
			}

			Method[] methods = javaClass.getDeclaredMethods();
			List<Method> listMethods = Arrays.asList(methods);
			List<String> listMethodNames = new ArrayList<String>();
			for (Method method : listMethods) {
				listMethodNames.add(method.getName());
			}
			
			Field[] fields = javaClass.getDeclaredFields();
			
			for (Field field : fields) {
				int fieldModifiers = field.getModifiers();

				// Ignore static fields (constants)
				if (Modifier.isStatic(fieldModifiers)) continue;

				String fieldName = field.getName();
				
				String errorMessage = javaClassName + ": " + field.getName();
				
				// Check bean info
				PropertyDescriptor propertyDescriptor = isBeanProperty(fieldName, dboBeanInfo);
				if (propertyDescriptor != null) {
					// Check bean property name policy
					if (!propertyDescriptor.getName().equals(fieldName)) {
						Error.PROPERTY_NAMING_POLICY.add(errorMessage);
					}
					
					String declaredGetter = propertyDescriptor.getReadMethod().getName();
					String declaredSetter = propertyDescriptor.getWriteMethod().getName();
					
					String formattedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
					String expectedGetter = "get" + formattedFieldName;
					String expectedSetter = "set" + formattedFieldName;
					
					// Check getter name policy
					if (!declaredGetter.equals(expectedGetter)) {
						Error.GETTER_SETTER_DECLARED_EXPECTED_NAMES_MISMATCH.add(errorMessage + "\n"
								+ "      Declared getter: " + declaredGetter + "\n"
								+ "      Expected getter: " + expectedGetter);
					}
					
					// Check setter name policy
					if (!declaredSetter.equals(expectedSetter)) {
						Error.GETTER_SETTER_DECLARED_EXPECTED_NAMES_MISMATCH.add(errorMessage + "\n"
								+ "      Declared setter: " + declaredSetter + "\n"
								+ "      Expected setter: " + expectedSetter);
					}
					
					// Check required private modifiers for bean property
					if (!Modifier.isPrivate(fieldModifiers)) {
						Error.PROPERTY_NOT_PRIVATE.add(errorMessage);
					}
					
					// Check getter
					if (!listMethodNames.contains(declaredGetter)) {
						Error.GETTER_SETTER_DECLARED_BUT_NOT_FOUND.add(errorMessage + " - Declared getter not found: " + declaredGetter);
					}

					// Check setter
					if (!listMethodNames.contains(declaredSetter)) {
						Error.GETTER_SETTER_DECLARED_BUT_NOT_FOUND.add(errorMessage + " - Declared setter not found: " + declaredGetter);
					}

					// Check non transient modifier
					if (Modifier.isTransient(fieldModifiers)) {
						Error.PROPERTY_TRANSIENT.add(errorMessage);
					}
				}
				else if (!Modifier.isTransient(fieldModifiers)) {
					Error.FIELD_NOT_TRANSIENT.add(errorMessage);
				}
			}
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR on " + javaClassName);
			e.printStackTrace();
		}
	}

	private static PropertyDescriptor isBeanProperty(String fieldName, MySimpleBeanInfo dboBeanInfo) {
		if (dboBeanInfo == null) return null;
		
		PropertyDescriptor[] propertyDescriptors = dboBeanInfo.getLocalPropertyDescriptors();
		
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			if (propertyDescriptor.getName().equals(fieldName)) return propertyDescriptor;
		}
		
		return null;
	}

}
