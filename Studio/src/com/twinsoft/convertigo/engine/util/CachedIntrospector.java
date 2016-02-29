/*
* Copyright (c) 2001-2016 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.engine.Engine;

public class CachedIntrospector {
	public enum Property {
		sourceDefinition,
		sourcesDefinition,
		smartType;
	}
	
	private final static Map<Class<? extends DatabaseObject>, BeanInfo> cacheBeanInfo = Collections.synchronizedMap(new WeakHashMap<Class<? extends DatabaseObject>, BeanInfo>());
	private final static Map<Class<? extends DatabaseObject>, Map<Property, Set<PropertyDescriptor>>> cacheProperties = Collections.synchronizedMap(new WeakHashMap<Class<? extends DatabaseObject>, Map<Property, Set<PropertyDescriptor>>>());

	public static <T> Set<T> getPropertyValues(DatabaseObject databaseObject, Property property) {
		Set<PropertyDescriptor> propertyDescriptors = getPropertyDescriptors(databaseObject, property);
		Set<T> values = new HashSet<T>(propertyDescriptors.size());
		for (PropertyDescriptor propertyDescriptor: propertyDescriptors) {
			try {
				values.add(GenericUtils.<T>cast(propertyDescriptor.getReadMethod().invoke(databaseObject)));
			} catch (Exception e) {
				throw new RuntimeException("Failed to getPropertyValue for " + databaseObject, e);
			}
		}
		return values;
	}
	
	public static <T> T getPropertyValue(DatabaseObject databaseObject, Property property) {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptor(databaseObject, property);
		if (propertyDescriptor != null) {
			try {
				return GenericUtils.cast(propertyDescriptor.getReadMethod().invoke(databaseObject));
			} catch (Exception e) {
				throw new RuntimeException("Failed to getPropertyValue for " + databaseObject, e);
			}
		}
		return null;
	}
	
	public static PropertyDescriptor getPropertyDescriptor(DatabaseObject databaseObject, Property property) {
		Set<PropertyDescriptor> propertyDescriptors = getPropertyDescriptors(databaseObject.getClass(), property);
		return propertyDescriptors.isEmpty() ? null : propertyDescriptors.iterator().next();
	}
	
	public static Set<PropertyDescriptor> getPropertyDescriptors(DatabaseObject databaseObject, Property property) {
		return getPropertyDescriptors(databaseObject.getClass(), property);
	}
	
	public static Set<PropertyDescriptor> getPropertyDescriptors(Class<? extends DatabaseObject> beanClass, Property property) {
		try {
			Map<Property, Set<PropertyDescriptor>> beanProperties = cacheProperties.get(beanClass);
			if (beanProperties == null) {
				cacheProperties.put(beanClass, beanProperties = new WeakHashMap<Property, Set<PropertyDescriptor>>());
			}
			Set<PropertyDescriptor> propertyDescriptors = beanProperties.get(property);
			if (propertyDescriptors == null) {
				BeanInfo beanInfo = getBeanInfo(beanClass);
				for (PropertyDescriptor propertyDescriptor: beanInfo.getPropertyDescriptors()) {
					boolean add = false;
					switch (property) {
					case sourceDefinition:
						add = "sourceDefinition".equals(propertyDescriptor.getName());
						break;
					case sourcesDefinition:
						add = "sourcesDefinition".equals(propertyDescriptor.getName());
						break;
					case smartType:
						add = propertyDescriptor.getPropertyType().equals(SmartType.class);
						break;
					}
					if (add) {
						if (propertyDescriptors == null) {
							propertyDescriptors = new HashSet<PropertyDescriptor>();
						}
						propertyDescriptors.add(propertyDescriptor);
					}
				}
				if (propertyDescriptors == null) {
					beanProperties.put(property, propertyDescriptors = GenericUtils.<Set<PropertyDescriptor>>cast(Collections.EMPTY_SET));
				} else {
					beanProperties.put(property, propertyDescriptors = Collections.unmodifiableSet(propertyDescriptors));
				}
			}
			return propertyDescriptors;
		} catch (IntrospectionException e) {
			throw new RuntimeException("Failed to getPropertyDescriptors for " + beanClass, e);
		}
	}
	
	public static BeanInfo getBeanInfo(DatabaseObject databaseObject) throws IntrospectionException {
		return getBeanInfo(databaseObject.getClass());
	}
	
	public static BeanInfo getBeanInfo(Class<? extends DatabaseObject> beanClass) throws IntrospectionException {
		BeanInfo beanInfo = cacheBeanInfo.get(beanClass);
		if (beanInfo == null) {
			cacheBeanInfo.put(beanClass, beanInfo = Introspector.getBeanInfo(beanClass));
		}
		return beanInfo;
	}
	
	public static void prefetchDatabaseObjects() {
		long time = System.currentTimeMillis();
		final long[] count = {0};
		Engine.logEngine.debug("(CachedIntrospector) Start to prefetch beans");
		InputStream inputStream = CachedIntrospector.class.getResourceAsStream("/com/twinsoft/convertigo/beans/database_objects.xml");
		try {
			XMLUtils.saxParse(inputStream, new DefaultHandler() {

				@SuppressWarnings("unchecked")
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					if ("bean".equals(qName)) {
						String classname = attributes.getValue("classname");
						if (classname != null) {
							try {
								Class<? extends DatabaseObject> cl = (Class<? extends DatabaseObject>) Class.forName(classname);
								getBeanInfo(cl);
								count[0]++;
							} catch (ClassNotFoundException e) {
								// silent exception
							} catch (IntrospectionException e) {
								// silent exception
							}
						}
					}
				}

			});
			inputStream.close();
		} catch (Exception e) {
			// silent exception
		}
		
		time = System.currentTimeMillis() - time;
		Engine.logEngine.info("(CachedIntrospector) " + count[0] + " beans prefetched in " + time + " ms");
	}
	
	public static void prefetchDatabaseObjectsAsync() {
		Thread th = new Thread(new Runnable() {

			public void run() {
				prefetchDatabaseObjects();
			}
			
		}, "prefetchDatabaseObjects");
		th.setDaemon(true);
		th.setPriority(Thread.MIN_PRIORITY);
		th.start();
	}
}
