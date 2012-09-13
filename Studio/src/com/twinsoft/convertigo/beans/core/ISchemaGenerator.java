package com.twinsoft.convertigo.beans.core;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;

public interface ISchemaGenerator {
	XmlSchemaObject getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema);
}
