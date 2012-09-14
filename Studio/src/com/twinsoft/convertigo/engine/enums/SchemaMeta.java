package com.twinsoft.convertigo.engine.enums;

import java.util.Map;

import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;

public enum SchemaMeta {
	container;
	
	public static void setContainerElement(XmlSchemaElement element, XmlSchemaElement eContainer) {
		element.addMetaInfo(container, eContainer);
	}
	
	public static XmlSchemaElement getContainerElement(XmlSchemaElement element) {
		XmlSchemaElement eContainer = (XmlSchemaElement) getMetaInfo(element, container);
		while (eContainer != null) {
			element = eContainer;
			eContainer = (XmlSchemaElement) getMetaInfo(element, container);
		}
		return element;
	}
	
	private static Object getMetaInfo(XmlSchemaObject xso, SchemaMeta key) {
		Map<?, ?> map = xso.getMetaInfoMap();
		return map == null ? null : map.get(key);
	}
}
