package com.twinsoft.convertigo.engine.enums;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public enum SchemaMeta {
	referencedDatabaseObjects,
	dynamicType,
	containerElement,
	containerGroupBase;
	
	private static <E> E getMetaInfo(XmlSchemaObject xso, SchemaMeta key) {
		Map<?, ?> map = xso.getMetaInfoMap();
		return map == null ? null : GenericUtils.<E>cast(map.get(key));
	}

	public static void setContainerXmlSchemaElement(XmlSchemaParticle particle, XmlSchemaElement element) {
		particle.addMetaInfo(containerElement, element);
	}
	
	public static XmlSchemaElement getContainerXmlSchemaElement(XmlSchemaParticle particle) {
		XmlSchemaElement result = getMetaInfo(particle, containerElement);
		if (result == null && particle instanceof XmlSchemaElement) {
			result = (XmlSchemaElement) particle;
		}
		return result;
	}

	public static void setContainerXmlSchemaGroupBase(XmlSchemaParticle particle, XmlSchemaGroupBase group) {
		particle.addMetaInfo(containerGroupBase, group);
	}
	
	public static XmlSchemaGroupBase getContainerXmlSchemaGroupBase(XmlSchemaParticle particle) {
		XmlSchemaGroupBase result = getMetaInfo(particle, containerGroupBase);
		if (result == null && particle instanceof XmlSchemaGroupBase) {
			result = (XmlSchemaGroupBase) particle;
		}
		return result;
	}

	public static void setDynamic(XmlSchemaObject xso) {
		xso.addMetaInfo(dynamicType, true);
	}
	
	public static boolean isDynamic(XmlSchemaObject xso) {
		Boolean isDynamic = getMetaInfo(xso, dynamicType);
		return isDynamic != null ? isDynamic : false;
	}
	
	public static Set<DatabaseObject> getReferencedDatabaseObjects(XmlSchemaObject xso) {
		Set<DatabaseObject> set = getMetaInfo(xso, referencedDatabaseObjects);
		if (set == null) {
			set = new LinkedHashSet<DatabaseObject>();
			xso.addMetaInfo(referencedDatabaseObjects, set);
		}
		return set;
	}
}
