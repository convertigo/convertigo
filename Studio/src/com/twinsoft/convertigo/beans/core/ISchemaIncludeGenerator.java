package com.twinsoft.convertigo.beans.core;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaInclude;

public interface ISchemaIncludeGenerator extends ISchemaGenerator {
	XmlSchemaInclude getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema);
}
