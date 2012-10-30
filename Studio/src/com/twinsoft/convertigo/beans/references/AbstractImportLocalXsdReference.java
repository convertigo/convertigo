package com.twinsoft.convertigo.beans.references;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;

import com.twinsoft.convertigo.beans.core.ISchemaImportGenerator;
import com.twinsoft.convertigo.engine.util.SchemaUtils;

public abstract class AbstractImportLocalXsdReference extends AbstractLocalXsdReference implements ISchemaImportGenerator  {
	private static final long serialVersionUID = -3021403823265525359L;
	
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
	
	public boolean isGenerateSchema() {
		return true;
	}	
}
