package com.twinsoft.convertigo.beans.references;

import java.io.File;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;

import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.engine.util.SchemaUtils;

public abstract class AbstractImportLocalXsdReference extends Reference {
	private static final long serialVersionUID = -3021403823265525359L;
	
	abstract protected File getXsdFile();
	
	public XmlSchemaImport getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaImport schemaImport = new XmlSchemaImport();
		try {
//			XmlSchema importedSchema = SchemaUtils.loadSchema(getXsdFile(), new XmlSchemaCollection());
//			
//			if (importedSchema.getTargetNamespace() == null) {
//				importedSchema.setTargetNamespace("http://no.name.space/" + getName());
//				importedSchema = collection.read(importedSchema.getSchemaDocument().getDocumentElement());
//			} else {
//				importedSchema = SchemaUtils.loadSchema(getXsdFile(), collection);
//			}
			
			XmlSchema importedSchema = SchemaUtils.loadSchema(getXsdFile(), collection);
			
			schemaImport.setSchemaLocation(getXsdFile().toURI().toString());
			schemaImport.setNamespace(importedSchema.getTargetNamespace());
			schemaImport.setSchema(importedSchema);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return schemaImport;
	}
}
