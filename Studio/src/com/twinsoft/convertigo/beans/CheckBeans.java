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
 
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

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
					System.out.println("Adding " + javaClassName);
					javaClassNames.add(javaClassName);
					return true;
				}
				return false;
			}
		});
	}
	
	private static String srcBase;
	
	public static void main(String[] args) {
		srcBase = args[0];
		
		System.out.println("Browsing sources in " + srcBase);
		
		browsePackages(srcBase);
		
		System.out.println("\nFound " + javaClassNames.size() + " classes");
		
		for (String javaClassName : javaClassNames) {
			analyzeJavaClass(javaClassName);
		}

		System.out.println("\nFound " + nbWarnings1 + " non static, non transient and public field(s) possibly without getter and/or setter");
		for (String warning : warnings1) {
			System.out.println("   " + warning);
		}
		System.out.println("\nFound " + nbWarnings4 + " possible DBO property(ies) not declared as private");
		for (String warning : warnings4) {
			System.out.println("   " + warning);
		}
		System.out.println("\nFound " + nbWarnings2 + " non static, non transient and non public field(s)");	
		for (String warning : warnings2) {
			System.out.println("   " + warning);
		}
		System.out.println("\nIgnored " + nbWarnings3 + " non DBO class(es)");	
		for (String warning : warnings3) {
			System.out.println("   " + warning);
		}
	}

	private static int nbWarnings1 = 0;
	private static List<String> warnings1 = new ArrayList<String>();
	private static int nbWarnings2 = 0;
	private static List<String> warnings2 = new ArrayList<String>();
	private static int nbWarnings3 = 0;
	private static List<String> warnings3 = new ArrayList<String>();
	private static int nbWarnings4 = 0;
	private static List<String> warnings4 = new ArrayList<String>();
	
	private static void analyzeJavaClass(String javaClassName) {
		try {
			Class<?> javaClass = Class.forName(javaClassName);
			
			if (!DatabaseObject.class.isAssignableFrom(javaClass)) {
				warnings3.add(javaClassName);
				nbWarnings3++;
				return;
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

				if (Modifier.isStatic(fieldModifiers)) continue;
				
				if (Modifier.isTransient(fieldModifiers)) continue;
				
				String getter = "get" + field.getName();
				if (field.getType().equals(Boolean.class))
					getter = "is" + field.getName();
				String setter = "set" + field.getName();
				
				getter = getter.toLowerCase();
				setter = setter.toLowerCase();

				if (Modifier.isPublic(fieldModifiers)) {
					// check getter and setter
					if (!listMethodNames.contains(getter) || !listMethodNames.contains(setter)) {
						warnings1.add(javaClassName + ": " + field.getName());
						nbWarnings1++;
					}
				}

				if (Modifier.isProtected(fieldModifiers)) {
					// check getter and setter
					if (listMethodNames.contains(getter) && listMethodNames.contains(setter)) {
						warnings4.add(javaClassName + ": " + field.getName());
						nbWarnings4++;
					}
				}

				warnings2.add(javaClassName + ": " + field.getName());
				nbWarnings2++;
			}
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR on " + javaClassName);
			e.printStackTrace();
		}
	}

}
