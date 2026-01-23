/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
	private final static Set<String> cacheClassNames = Collections.synchronizedSet(new HashSet<String>());
	
	public static Set<PropertyDescriptor> getPropertyDescriptors(DatabaseObject databaseObject, Property property) {
		return getPropertyDescriptors(databaseObject.getClass(), property);
	}
	
	private static Set<PropertyDescriptor> getPropertyDescriptors(Class<? extends DatabaseObject> beanClass, Property property) {
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
	
	
	
	private static void prefetchDatabaseObjects() {
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
								cacheClassNames.add(classname);
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
