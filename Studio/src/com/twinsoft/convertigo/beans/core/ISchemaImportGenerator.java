package com.twinsoft.convertigo.beans.core;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;

public interface ISchemaImportGenerator extends ISchemaGenerator {
	XmlSchemaImport getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema);
}
