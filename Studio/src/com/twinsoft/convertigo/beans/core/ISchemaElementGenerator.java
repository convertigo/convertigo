package com.twinsoft.convertigo.beans.core;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;

public interface ISchemaElementGenerator extends ISchemaParticleGenerator {
	XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema);
}
