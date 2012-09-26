package com.twinsoft.convertigo.beans.references;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;

import com.twinsoft.convertigo.beans.core.Reference;

public abstract class AbstractImportLocalXsdReference extends Reference {
	private static final long serialVersionUID = -3021403823265525359L;
	
	abstract protected File getXsdFile();
	
	public XmlSchemaImport getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaImport schemaImport = new XmlSchemaImport();
		try {
			XmlSchema importedSchema = collection.read(new FileReader(getXsdFile()), null);
			schemaImport.setSchemaLocation(getXsdFile().toURI().toString());
			if (importedSchema.getTargetNamespace() == null) {
				importedSchema.setTargetNamespace("http://no.name.space/" + getName());
			}
			schemaImport.setNamespace(importedSchema.getTargetNamespace());
			schemaImport.setSchema(importedSchema);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return schemaImport;
	}
}
