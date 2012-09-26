package com.twinsoft.convertigo.engine.enums;

import java.util.Map;

import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaType;

public enum SchemaMeta {
	dynamicType,
	containerElement,
	containerGroupBase;
	
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

	public static void setDynamic(XmlSchemaType type) {
		type.addMetaInfo(dynamicType, true);
	}
	
	public static boolean isDynamic(XmlSchemaType type) {
		Boolean isDynamic = (Boolean) getMetaInfo(type, dynamicType);
		return isDynamic != null ? isDynamic : false;
	}
}
