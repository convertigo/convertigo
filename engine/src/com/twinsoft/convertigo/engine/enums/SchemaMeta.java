/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
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
	dynamic,
	readOnly,
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
		xso.addMetaInfo(dynamic, true);
	}
	
	public static boolean isDynamic(XmlSchemaObject xso) {
		Boolean isDynamic = getMetaInfo(xso, dynamic);
		return isDynamic != null ? isDynamic : false;
	}
	
	public static void setReadOnly(XmlSchemaObject xso) {
		xso.addMetaInfo(readOnly, true);
	}
	
	public static boolean isReadOnly(XmlSchemaObject xso) {
		Boolean isReadOnly = getMetaInfo(xso, readOnly);
		return isReadOnly != null ? isReadOnly : !isDynamic(xso);
	}
	
	public static Set<DatabaseObject> getReferencedDatabaseObjects(XmlSchemaObject xso) {
		Set<DatabaseObject> set = getMetaInfo(xso, referencedDatabaseObjects);
		if (set == null) {
			set = new LinkedHashSet<DatabaseObject>();
			xso.addMetaInfo(referencedDatabaseObjects, set);
		}
		return set;
	}
	
	public static <T extends XmlSchemaObject> T setSchema(T xso, XmlSchema schema) {
		xso.addMetaInfo(ownerSchema, schema);
		return xso;
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
		} else if (xso instanceof XmlSchemaAttributeGroup) {
			qName = ((XmlSchemaAttributeGroup) xso).getName();
		} else if (xso instanceof XmlSchemaGroup) {
			qName = ((XmlSchemaGroup) xso).getName();
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

	public static void adoptReferences(XmlSchema schema, XmlSchemaObject first, XmlSchemaObject second) {
		Set<DatabaseObject> dbos = SchemaMeta.getReferencedDatabaseObjects(first);
		for (Iterator<DatabaseObject> iDbo = SchemaMeta.getReferencedDatabaseObjects(second).iterator(); iDbo.hasNext(); iDbo.remove()) {
			DatabaseObject dbo = iDbo.next();
			if (SchemaMeta.getXmlSchemaObject(schema, dbo) == second) {
				SchemaMeta.setXmlSchemaObject(schema, dbo, first);
			}
			dbos.add(dbo);
		}
	}
}
