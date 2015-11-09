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

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;

public class WSDLUtils {

	public static Definition readWsdl(URL wsdlURL) {
		Definition definition = null;
		try {
			if (wsdlURL != null) {
				WSDLFactory factory = WSDLFactory.newInstance();
				WSDLReader reader = factory.newWSDLReader();
				//reader.setFeature("javax.wsdl.importDocuments", true);
				definition = reader.readWSDL(null, wsdlURL.toString());
			}
		}
		catch (Throwable t) {
			Engine.logEngine.error("(WSDLUtils) error while reading WSDL", t);
		}
		return definition;
	}
		
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
		String defns = definition.getTargetNamespace();
		Types types = definition.getTypes();
		Iterator<?> exs = types.getExtensibilityElements().iterator();
		while (exs.hasNext()) {
			ExtensibilityElement ee = (ExtensibilityElement)exs.next();
			if (ee instanceof Schema) {
				try {
					Schema schema = (Schema)ee;
					Element se = schema.getElement();
					if (se != null) {
						// Modify Schema element for RPC if needed
						SchemaUtils.addSoapEncSchemaImport(se);
						
						// Read Schema element
						XmlSchemaCollection collection = new XmlSchemaCollection();
						collection.setBaseUri(schema.getDocumentBaseURI());
						collection.read(se);
						
						// Add main XmSchema to collection
						readSchema(schemaCol, collection.schemaForNamespace(defns));
						
						// Add other XmSchema(s) to collection
						for (XmlSchema xs : collection.getXmlSchemas()) {
							readSchema(schemaCol, xs);
						}
					}
				}
				catch (Throwable t) {
					Engine.logEngine.error("(WSDLUtils) error while reading Schema", t);
				}
			}
		}
	}
	
	private static void readSchema(XmlSchemaCollection schemaCol, XmlSchema xs) throws XmlSchemaSerializerException {
		if (xs != null) {
			String tns = xs.getTargetNamespace();
			if (tns != null && !tns.equals("")) {
				if (schemaCol.schemaForNamespace(tns) == null) {
					Engine.logEngine.debug("(WSDLUtils) {"+tns+"} reading schema...");
					if (Engine.logEngine.isTraceEnabled()) {
						Engine.logEngine.trace(XMLUtils.prettyPrintDOM(xs.getSchemaDocument()));
					}
					XmlSchema xr = schemaCol.read(xs.getSchemaDocument(),null);
					xr.setSourceURI(xs.getSourceURI());
				}
			}
		}
	}
}