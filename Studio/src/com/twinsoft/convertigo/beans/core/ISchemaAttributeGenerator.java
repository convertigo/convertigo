package com.twinsoft.convertigo.beans.core;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;

public interface ISchemaAttributeGenerator extends ISchemaGenerator {
	XmlSchemaAttribute getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema);
}
