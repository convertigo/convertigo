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
import org.apache.ws.commons.schema.XmlSchemaInclude;

import com.twinsoft.convertigo.beans.core.ISchemaIncludeGenerator;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class IncludeXsdSchemaReference extends XsdSchemaReference implements ISchemaIncludeGenerator {

	private static final long serialVersionUID = -3563843372006164149L;

	public XmlSchemaInclude getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaInclude schemaInclude = new XmlSchemaInclude();
		try {
			// load schema
			XmlSchema includedSchema = readSchema(collection);
			if (includedSchema != null) {
				// check for same namespace
				if (XmlSchemaUtils.hasSameNamespace(schema, includedSchema)) {
					// initialize include
					schemaInclude.setSchemaLocation(getReferenceUrl().toString());
					schemaInclude.setSchema(includedSchema);
				}
				else throw new EngineException("Incorrect schema include +" +
						"("+getReferenceUrl().toString()+"): target namespace differs from \""+schema.getTargetNamespace()+"\"");
			}
		} catch (Exception e) {
			if (e instanceof EngineException)
				Engine.logBeans.error(e.getMessage());
			e.printStackTrace();
		}
		return schemaInclude;
	}
	
	public boolean isGenerateSchema() {
		return true;
	}

}
