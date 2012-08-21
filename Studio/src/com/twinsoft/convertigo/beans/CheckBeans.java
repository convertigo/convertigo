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
 
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.Engine;

public class CheckBeans {

	private static List<String> javaClassNames = new ArrayList<String>();

	private static void browsePackages(final String currentPackage) {
		new File(currentPackage).listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory()) {
					browsePackages(currentPackage + "/" + file.getName());
					return false;
				}
				else if (file.getParent().equals(srcBase)) {
					return false;
				}
				else if (file.getName().endsWith(".java")) {
					String javaClassName = file.getPath();
					javaClassName = javaClassName.substring(srcBase.length()).replace('/', '.');
					javaClassName = javaClassName.substring(0, javaClassName.lastIndexOf(".java"));
					
					if (javaClassName.endsWith("BeanInfo")) return false;
					
					javaClassName = "com.twinsoft.convertigo.beans" + javaClassName;
					//System.out.println("Adding " + javaClassName);
					javaClassNames.add(javaClassName);
					return true;
				}
				return false;
			}
		});
	}
	
	private static String srcBase;
	
	public static void main(String[] args) {
		Engine.logBeans = Logger.getLogger(BeansDoc.class);

		srcBase = args[0];
		
		System.out.println("Browsing sources in " + srcBase);
		
		browsePackages(srcBase);
		
		System.out.println("\nFound " + javaClassNames.size() + " classes");
		
		for (String javaClassName : javaClassNames) {
			analyzeJavaClass(javaClassName);
		}

		System.out.println();
		
		for (Error error : Error.values()) {
			List<String> errorList = errors.get(error);
			if (errorList == null) continue;
			System.out.println(error);
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

		for (Error error : Error.values()) {
			List<String> errorList = errors.get(error);
			int nError = 0;
			if (errorList != null) nError = errorList.size();
			System.out.println(error + ": found " + nError + " error(s)");
		}
		
		System.out.println("\nBean checking finished!");
	}

	private enum Error {
		//NON_DATABASE_OBJECT("Non database object"),
		MISSING_BEAN_INFO("Missing bean info"),
		BEAN_ICON_NAMING_POLICY("Wrong icon name "),
		BEAN_PROPERTY_NAMING_POLICY("Wrong property name"),
		BEAN_PROPERTY_NOT_PRIVATE("Non private bean property"),
		BEAN_PROPERTY_TRANSIENT("Transient bean property"),
		GETTER_SETTER_NAMING_POLICY("Wrong getter and/or setter name"),
		GETTER_SETTER_DECLARED_EXPECTED_NAME_MISMATCH("Declared and expected getter and/or setter mismatch"),
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
	
	private static Map<Error, List<String>> errors = new HashMap<Error, List<String>>();

	private static void analyzeJavaClass(String javaClassName) {
		try {
			Class<?> javaClass = Class.forName(javaClassName);
			String javaClassSimpleName = javaClass.getSimpleName();
			
			if (!DatabaseObject.class.isAssignableFrom(javaClass)) {
				//Error.NON_DATABASE_OBJECT.add(javaClassName);
				return;
			}
			
			String dboBeanInfoClassName = javaClassName + "BeanInfo";
			MySimpleBeanInfo dboBeanInfo = null;
			try {
				dboBeanInfo = (MySimpleBeanInfo) (Class.forName(dboBeanInfoClassName)).newInstance();
			} catch (ClassNotFoundException e) {
				Error.MISSING_BEAN_INFO.add(javaClassName + " (expected bean info: " + dboBeanInfoClassName + ")");
				return;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
			// Check icon name policy
			String expectedIconName = javaClassName.replace(javaClassSimpleName, "images/" + javaClassSimpleName);
			expectedIconName = "/" + expectedIconName.replace('.', '/') + "_color_16x16";
			expectedIconName = expectedIconName.toLowerCase();
			String declaredIconName = MySimpleBeanInfo.getIconName(dboBeanInfo, MySimpleBeanInfo.ICON_COLOR_16x16);
			if (declaredIconName != null) {
				declaredIconName = declaredIconName.substring(0, declaredIconName.length() - 4);
				if (!declaredIconName.equals(expectedIconName)) {
					Error.BEAN_ICON_NAMING_POLICY.add(javaClassName + "\n"
							+ "      Declared: " + declaredIconName + "\n"
							+ "      Expected: " + expectedIconName);
					return;
				}
			}

			Method[] methods = javaClass.getDeclaredMethods();
			List<Method> listMethods = Arrays.asList(methods);
			List<String> listMethodNames = new ArrayList<String>();
			for (Method method : listMethods) {
				listMethodNames.add(method.getName().toLowerCase());
			}
			
			Field[] fields = javaClass.getDeclaredFields();
			
			for (Field field : fields) {
				int fieldModifiers = field.getModifiers();

				// Ignore static fields (constants)
				if (Modifier.isStatic(fieldModifiers)) continue;

				String fieldName = field.getName();
				
				String errorMessage = javaClassName + ": " + field.getName();
				
				// Check against bean info class
				PropertyDescriptor propertyDescriptor = isBeanProperty(fieldName, dboBeanInfo);
				if (propertyDescriptor != null) {
					String declaredGetter = propertyDescriptor.getReadMethod().getName();
					String declaredSetter = propertyDescriptor.getWriteMethod().getName();
					
					String formattedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
					String expectedGetter = "get" + formattedFieldName;
					String expectedSetter = "set" + formattedFieldName;
					
					// Check bean property name policy
					if (!propertyDescriptor.getName().equals(fieldName)) {
						Error.BEAN_PROPERTY_NAMING_POLICY.add(errorMessage);
						continue;
					}
					
					// Check getter and setter name policy
					if (!declaredGetter.equals(expectedGetter) || !declaredGetter.equals(expectedSetter)) {
						Error.GETTER_SETTER_DECLARED_EXPECTED_NAME_MISMATCH.add(errorMessage);
						continue;
					}
					
					// Check required private modifiers for bean property
					if (!Modifier.isPrivate(fieldModifiers)) {
						Error.BEAN_PROPERTY_NOT_PRIVATE.add(errorMessage);
						continue;
					}
					
					// Check getter and setter
					if (!listMethodNames.contains(declaredGetter) || !listMethodNames.contains(declaredSetter)) {
						Error.GETTER_SETTER_NAMING_POLICY.add(errorMessage);
						continue;
					}

					// Check non transient modifier
					if (Modifier.isTransient(fieldModifiers)) {
						Error.BEAN_PROPERTY_TRANSIENT.add(errorMessage);
						continue;
					}
					
					continue;
				}

				if (!Modifier.isTransient(fieldModifiers)) {
					Error.FIELD_NOT_TRANSIENT.add(errorMessage);
					continue;
				}
			}
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR on " + javaClassName);
			e.printStackTrace();
		}
	}

	private static PropertyDescriptor isBeanProperty(String fieldName, MySimpleBeanInfo dboBeanInfo) {
		PropertyDescriptor[] propertyDescriptors = dboBeanInfo.getPropertyDescriptors();
		
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			if (propertyDescriptor.getName().equals(fieldName)) return propertyDescriptor;
		}
		
		return null;
	}

}
