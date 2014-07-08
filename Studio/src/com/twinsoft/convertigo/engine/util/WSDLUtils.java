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

package com.twinsoft.convertigo.engine.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;

import org.apache.ws.commons.schema.XmlSchemaCollection;

public class WSDLUtils {

	public static XmlSchemaCollection readSchemas(Definition definition) {
		XmlSchemaCollection schemaCol = new XmlSchemaCollection();
		schemaCol.setBaseUri(definition.getDocumentBaseURI());
		readSchemas(schemaCol, definition);
		return schemaCol;
	}
	
	private static void readSchemas(XmlSchemaCollection schemaCol, Definition definition) {
		// Read schemas of included WSDL files
		Map<String, List<Import>> imap = GenericUtils.cast(definition.getImports());
		Iterator<String> it1 = imap.keySet().iterator();
		while (it1.hasNext()) {
			String uri = it1.next();
			List<Import> list = imap.get(uri);
			for (Import imp: list) {
				readSchemas(schemaCol,imp.getDefinition());
			}
		}
		
		// Read schemas of WSDL types
		Types types = definition.getTypes();
		Iterator<?> exs = types.getExtensibilityElements().iterator();
		while (exs.hasNext()) {
			ExtensibilityElement ee = (ExtensibilityElement)exs.next();
			if (ee instanceof Schema) {
				schemaCol.read(((Schema)ee).getElement());
			}
		}
	}
}