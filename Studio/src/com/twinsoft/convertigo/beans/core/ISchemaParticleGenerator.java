package com.twinsoft.convertigo.beans.core;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;

public interface ISchemaParticleGenerator extends ISchemaGenerator {
	XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema);
	boolean isGenerateElement();
}
