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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.references;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaInclude;

import com.twinsoft.convertigo.beans.core.ISchemaIncludeGenerator;
import com.twinsoft.convertigo.engine.util.SchemaUtils;

public abstract class AbstractIncludeLocalXsdReference extends AbstractLocalXsdReference implements ISchemaIncludeGenerator {
	private static final long serialVersionUID = 5753767474982023399L;

	public XmlSchemaInclude getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaInclude schemaInclude = new XmlSchemaInclude();
		try {
			XmlSchema incluedSchema = SchemaUtils.loadSchema(getXsdFile(), new XmlSchemaCollection());
			
			schemaInclude.setSchemaLocation(getXsdFile().toURI().toString());
			schemaInclude.setSchema(incluedSchema);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return schemaInclude;
	}
	
	public boolean isGenerateSchema() {
		return true;
	}	
	
}
