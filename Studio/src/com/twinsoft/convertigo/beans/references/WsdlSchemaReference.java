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

import java.net.URL;
import java.util.ArrayList;
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
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.ISchemaReader;
import com.twinsoft.convertigo.beans.core.IWsdlReader;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public abstract class WsdlSchemaReference extends RemoteFileReference implements ISchemaReference, ISchemaReader, IWsdlReader {

	private static final long serialVersionUID = -3639937867834626528L;

	public XmlSchema readSchema(XmlSchemaCollection collection) {
		try {
			XmlSchema mainSchema = null;
			List<Definition> definitions = readWsdl();
			for (Definition definition: definitions) {
				Types types = definition.getTypes();
				List<?> list = types.getExtensibilityElements();
				Iterator<?> iterator = list.iterator();
				while (iterator.hasNext()) {
					ExtensibilityElement extensibilityElement = (ExtensibilityElement)iterator.next();
					if (extensibilityElement instanceof Schema) {
						Element element = ((Schema)extensibilityElement).getElement();
						XmlSchema xmlSchema = collection.read(element);
						if (definition.getTargetNamespace().equals(xmlSchema.getTargetNamespace())) {
							mainSchema = xmlSchema;
						}
					}
				}
			}
			return mainSchema;
		}
		catch (Exception e) {
			Engine.logBeans.error(e.getMessage());
		}
		return null;
	}

	public List<Definition> readWsdl() {
		List<Definition> list = new ArrayList<Definition>();
		try {
			URL wsdlURL = getReferenceUrl();
			if (wsdlURL != null) {
				WSDLFactory factory = WSDLFactory.newInstance();
				WSDLReader reader = factory.newWSDLReader();
				reader.setFeature("javax.wsdl.importDocuments", true);
				Definition definition = reader.readWSDL(null, wsdlURL.toString());
				list.addAll(readWsdlImports(definition));
			}
		}
		catch (Exception e) {
			Engine.logBeans.error(e.getMessage());
		}
		return list;
	}
	
	protected List<Definition> readWsdlImports(Definition definition) {
		List<Definition> list = new ArrayList<Definition>();
		list.add(definition);
		
		Map<String, List<Import>> imports = GenericUtils.cast(definition.getImports());
		for (List<Import> iList : imports.values()) {
			for (Import wsdlImport : iList) {
				Definition def = wsdlImport.getDefinition();
				list.addAll(readWsdlImports(def));
			}
		}
		return list;
	}
}
