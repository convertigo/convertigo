/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.references;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;

import com.twinsoft.convertigo.beans.core.ISchemaImportGenerator;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class ImportWsdlSchemaReference extends WsdlSchemaReference implements ISchemaImportGenerator {

	private static final long serialVersionUID = 5650511511557578528L;

	public XmlSchemaImport getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaImport schemaImport = new XmlSchemaImport();
		try {
			// load schema
			XmlSchema importedSchema = readSchema(collection);
			if (importedSchema != null) {
				// check for different namespace
				if (!XmlSchemaUtils.hasSameNamespace(schema, importedSchema)) {
					// initialize import
					schemaImport.setNamespace(importedSchema.getTargetNamespace());
					schemaImport.setSchema(importedSchema);
				}
				else throw new EngineException("Incorect schema import +" +
						"("+getFile().getPath()+"): target namespace is the same as \""+schema.getTargetNamespace()+"\"");
			}
		} catch (Exception e) {
			if (e instanceof EngineException)
				Engine.logBeans.error(e.getMessage());
			e.printStackTrace();
		}
		return schemaImport;
	}

	public boolean isGenerateSchema() {
		return true;
	}

}
