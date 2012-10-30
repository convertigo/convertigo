package com.twinsoft.convertigo.beans.references;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;

import com.twinsoft.convertigo.beans.core.ISchemaImportGenerator;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.SchemaUtils;

public abstract class AbstractImportLocalXsdReference extends AbstractLocalXsdReference implements ISchemaImportGenerator  {
	private static final long serialVersionUID = -3021403823265525359L;
	
	public XmlSchemaImport getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaImport schemaImport = new XmlSchemaImport();
		try {
			// load schema
			XmlSchema importedSchema = SchemaUtils.loadSchema(getXsdFile(), collection);
			if (importedSchema != null) {
				try {
					// check for different namespace
					checkTargetNamespace(schema, importedSchema);
					
					// initialize import
					schemaImport.setSchemaLocation(getXsdFile().toURI().toString());
					schemaImport.setNamespace(importedSchema.getTargetNamespace());
					schemaImport.setSchema(importedSchema);
				}
				catch (EngineException e) {
					Engine.logBeans.error(e.getMessage());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return schemaImport;
	}
	
	public boolean isGenerateSchema() {
		return true;
	}
	
	private void checkTargetNamespace(XmlSchema mainSchema, XmlSchema importedSchema) throws EngineException {
		String tns1 = mainSchema.getTargetNamespace();
		String tns2 = importedSchema.getTargetNamespace();
		if (tns1 != null && tns2 != null && !tns1.equals(tns2)) return;
		throw new EngineException("Incorect schema import +" +
				"("+getXsdFile().getPath()+"): target namespace is the same as \""+tns1+"\"");
	}
}
