package com.twinsoft.convertigo.engine.enums;

import java.util.Map;

import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;

public enum SchemaMeta {
	containerElement,
	containerGroupBase;
//	container;
	
//	public static void setContainerParticle(XmlSchemaParticle particle, XmlSchemaParticle eContainer) {
//		particle.addMetaInfo(container, eContainer);
//	}
//	
//	public static XmlSchemaParticle getContainerParticle(XmlSchemaParticle particle) {
//		XmlSchemaParticle eContainer = (XmlSchemaParticle) getMetaInfo(particle, container);
////		while (eContainer != null) {
////			particle = eContainer;
////			eContainer = (XmlSchemaParticle) getMetaInfo(particle, container);
////		}
//		return eContainer != null ? eContainer : particle;
//	}
//	
//	public static void setContainerElement(XmlSchemaElement element, XmlSchemaElement eContainer) {
//		setContainerParticle(element, eContainer);
//	}
//	
//	public static XmlSchemaElement getContainerElement(XmlSchemaElement element) {
//		return (XmlSchemaElement) getContainerParticle(element);
//	}
	
	private static Object getMetaInfo(XmlSchemaObject xso, SchemaMeta key) {
		Map<?, ?> map = xso.getMetaInfoMap();
		return map == null ? null : map.get(key);
	}

	public static void setContainerXmlSchemaElement(XmlSchemaParticle particle, XmlSchemaElement element) {
		particle.addMetaInfo(containerElement, element);
	}
	
	public static XmlSchemaElement getContainerXmlSchemaElement(XmlSchemaParticle particle) {
		XmlSchemaElement result = (XmlSchemaElement) getMetaInfo(particle, containerElement);
		if (result == null && particle instanceof XmlSchemaElement) {
			result = (XmlSchemaElement) particle;
		}
		return result;
	}

	public static void setContainerXmlSchemaGroupBase(XmlSchemaParticle particle, XmlSchemaGroupBase group) {
		particle.addMetaInfo(containerGroupBase, group);
	}
	
	public static XmlSchemaGroupBase getContainerXmlSchemaGroupBase(XmlSchemaParticle particle) {
		XmlSchemaGroupBase result = (XmlSchemaGroupBase) getMetaInfo(particle, containerGroupBase);
		if (result == null && particle instanceof XmlSchemaGroupBase) {
			result = (XmlSchemaGroupBase) particle;
		}
		return result;
	}
}
