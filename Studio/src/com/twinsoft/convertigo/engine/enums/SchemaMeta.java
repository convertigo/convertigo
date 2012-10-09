package com.twinsoft.convertigo.engine.enums;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaType;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public enum SchemaMeta {
	schemaPrefix,
	ownerCollection,
	ownerSchema,
	dboSchemaMap,
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
	
	public static void addReferencedDatabaseObjects(XmlSchemaObject first, XmlSchemaObject second) {
		getReferencedDatabaseObjects(first).addAll(getReferencedDatabaseObjects(second));
	}
	
	public static void setSchema(XmlSchemaObject xso, XmlSchema schema) {
		xso.addMetaInfo(ownerSchema, schema);
	}
	
	public static XmlSchema getSchema(XmlSchemaObject xso) {
		return (XmlSchema) (xso instanceof XmlSchema ? (XmlSchema) xso : getMetaInfo(xso, ownerSchema));
	}
	
	public static void setPrefix(XmlSchema schema, String prefix) {
		schema.addMetaInfo(schemaPrefix, prefix);
	}
	
	public static String getPrefix(XmlSchema schema) {
		return getMetaInfo(schema, schemaPrefix);
	}
	
	public static void setCollection(XmlSchema schema, XmlSchemaCollection collection) {
		schema.addMetaInfo(ownerCollection, collection);
	}
	
	public static XmlSchemaCollection getCollection(XmlSchemaObject xso) {
		XmlSchema schema = getSchema(xso);
		return (XmlSchemaCollection) (schema != null ? getMetaInfo(schema, ownerCollection) : null);
	}
	
	public static XmlSchemaType getType(XmlSchemaObject xso, QName qName) {
		if (qName == null) {
			return null;
		} else {
			XmlSchemaCollection collection = getCollection(xso);
			return collection.getTypeByQName(qName);
		}
	}
	
	public static QName getQName(XmlSchemaObject xso) {
		QName qName = null;
		if (xso instanceof XmlSchemaType) {
			qName = ((XmlSchemaType) xso).getQName();
		} else if (xso instanceof XmlSchemaElement) {
			qName = ((XmlSchemaElement) xso).getQName();
		} else if (xso instanceof XmlSchemaAttribute) {
			qName = ((XmlSchemaAttribute) xso).getQName();
		}
		return qName;
	}
	
	public static String getPrefix(XmlSchemaObject xso) {
		String prefix = null;
		XmlSchema schema = getSchema(xso);
		if (!isDynamic(schema) && getQName(xso) != null) {
			prefix = getPrefix(schema);
		}
		return prefix;
	}
	
	private static Map<DatabaseObject, XmlSchemaObject> getXmlSchemaObject(XmlSchema schema) {
		Map<DatabaseObject, XmlSchemaObject> map = getMetaInfo(schema, dboSchemaMap);
		if (map == null) {
			map = new HashMap<DatabaseObject, XmlSchemaObject>();
			schema.addMetaInfo(dboSchemaMap, map);
		}
		return map;
	}
	
	public static void setXmlSchemaObject(XmlSchema schema, DatabaseObject databaseObject, XmlSchemaObject xso) {
		getXmlSchemaObject(schema).put(databaseObject, xso);
	}
	
	public static XmlSchemaObject getXmlSchemaObject(XmlSchema schema, DatabaseObject databaseObject) {
		return getXmlSchemaObject(schema).get(databaseObject);
	}
}
