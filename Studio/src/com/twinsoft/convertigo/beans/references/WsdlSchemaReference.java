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

import java.util.ArrayList;
import java.util.List;

import javax.wsdl.Definition;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.constants.Constants;
import com.twinsoft.convertigo.beans.core.ISchemaReader;
import com.twinsoft.convertigo.beans.core.IWsdlReader;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.SchemaManager;
import com.twinsoft.convertigo.engine.util.WSDLUtils;

public abstract class WsdlSchemaReference extends RemoteFileReference implements ISchemaReference, ISchemaReader, IWsdlReader {

	private static final long serialVersionUID = -3639937867834626528L;

	public XmlSchema readSchema(XmlSchemaCollection collection) {
		try {
			Definition definition = readWsdl();
			String mainSchemaNamespace = definition.getTargetNamespace();
			XmlSchemaCollection wsdlCol = WSDLUtils.readSchemas(definition);
			
			List<String> namespaceList = new ArrayList<String>();
			
			// First read main schema
			XmlSchema mxs = wsdlCol.schemaForNamespace(mainSchemaNamespace);
			if (mxs != null && collection.schemaForNamespace(mainSchemaNamespace) == null) {
				namespaceList.add(mainSchemaNamespace);
				collection.read(mxs.getSchemaDocument(),mxs.getSourceURI(),null);
			}
			
			// Then read others
			for (XmlSchema xs : wsdlCol.getXmlSchemas()) {
				String tns = xs.getTargetNamespace();
				if (!namespaceList.contains(tns) && !tns.equals(Constants.URI_2001_SCHEMA_XSD)) {
					namespaceList.add(tns);
					XmlSchema cxs = wsdlCol.schemaForNamespace(tns);
					if (cxs != null && collection.schemaForNamespace(tns) == null) {
						collection.read(cxs.getSchemaDocument(),cxs.getSourceURI(),null);
					}
				}
			}
			
			// Finally
			for (XmlSchema xmlSchema : collection.getXmlSchemas()) {
				String tns = xmlSchema.getTargetNamespace();
				if (namespaceList.contains(tns)) {
					// Add missing 'import' in collection schemas (for validation)
					String[] declaredPrefixes = xmlSchema.getNamespaceContext().getDeclaredPrefixes();
					for (int i=0; i <declaredPrefixes.length; i++) {
						String prefix = declaredPrefixes[i];
						String ns = xmlSchema.getNamespaceContext().getNamespaceURI(prefix);
						if (!ns.equals(tns)) {
							if (namespaceList.contains(ns)) {
								SchemaManager.addXmlSchemaImport(collection, xmlSchema, ns);
							}
						}
					}
				}
			}
			
			XmlSchema mainSchema = collection.schemaForNamespace(mainSchemaNamespace);
			return mainSchema;
		}
		catch (Exception e) {
			Engine.logBeans.error(e.getMessage(), e);
		}
		return null;
	}
	
	public Definition readWsdl() {
		Definition definition = null;
		try {
			definition = WSDLUtils.readWsdl(getReferenceUrl());
		}
		catch (Exception e) {
			Engine.logBeans.error(e.getMessage(), e);
		}
		return definition;
	}
}
