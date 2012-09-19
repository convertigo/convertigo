package com.twinsoft.convertigo.engine.enums;

import java.util.Map;

import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;

public enum SchemaMeta {
	container;
	
	public static void setContainerParticle(XmlSchemaParticle particle, XmlSchemaParticle eContainer) {
		particle.addMetaInfo(container, eContainer);
	}
	
	public static XmlSchemaParticle getContainerParticle(XmlSchemaParticle particle) {
		XmlSchemaParticle eContainer = (XmlSchemaParticle) getMetaInfo(particle, container);
		while (eContainer != null) {
			particle = eContainer;
			eContainer = (XmlSchemaParticle) getMetaInfo(particle, container);
		}
		return particle;
	}
	
	public static void setContainerElement(XmlSchemaElement element, XmlSchemaElement eContainer) {
		setContainerParticle(element, eContainer);
	}
	
	public static XmlSchemaElement getContainerElement(XmlSchemaElement element) {
		return (XmlSchemaElement) getContainerParticle(element);
	}
	
	private static Object getMetaInfo(XmlSchemaObject xso, SchemaMeta key) {
		Map<?, ?> map = xso.getMetaInfoMap();
		return map == null ? null : map.get(key);
	}
}
