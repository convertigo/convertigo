package com.twinsoft.convertigo.engine.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class CachedIntrospector {
	private final static Map<Class<? extends DatabaseObject>, BeanInfo> cache = Collections.synchronizedMap(new WeakHashMap<Class<? extends DatabaseObject>, BeanInfo>());

	public static BeanInfo getBeanInfo(Class<? extends DatabaseObject> beanClass) throws IntrospectionException {
		BeanInfo beanInfo = cache.get(beanClass);
		if (beanInfo == null) {
			cache.put(beanClass, beanInfo = Introspector.getBeanInfo(beanClass));
		}
		return beanInfo;
	}
	
	public static void prefetchDatabaseObjects() {
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
