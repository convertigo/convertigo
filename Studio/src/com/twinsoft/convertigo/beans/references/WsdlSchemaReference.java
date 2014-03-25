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

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.ISchemaReader;
import com.twinsoft.convertigo.beans.core.IWsdlReader;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.SchemaManager;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public abstract class WsdlSchemaReference extends RemoteFileReference implements ISchemaReference, ISchemaReader, IWsdlReader {

	private static final long serialVersionUID = -3639937867834626528L;

	public XmlSchema readSchema(XmlSchemaCollection collection) {
		try {
			List<String> namespaceList = new ArrayList<String>();
			String mainSchemaNamespace = null;
			XmlSchema mainSchema = null;
			boolean bImport = false;
			
			// First read all schemas from WSDL in a new Collection
			List<Definition> definitions = readWsdl();
			XmlSchemaCollection c = new XmlSchemaCollection();
			for (Definition definition: definitions) {
				String baseURI = definition.getDocumentBaseURI();
				if (baseURI != null) c.setBaseUri(baseURI);
				Types types = definition.getTypes();
				List<?> list = types.getExtensibilityElements();
				Iterator<?> iterator = list.iterator();
				while (iterator.hasNext()) {
					ExtensibilityElement extensibilityElement = (ExtensibilityElement)iterator.next();
					if (extensibilityElement instanceof Schema) {
						Element element = ((Schema)extensibilityElement).getElement();
						
						// overwrites elementFormDefault, attributeFormDefault
						//element.setAttribute("elementFormDefault", Project.XSD_FORM_UNQUALIFIED);
						//element.setAttribute("attributeFormDefault", Project.XSD_FORM_UNQUALIFIED);
						
						// check for targetNamespace declaration
						if (!element.hasAttribute("targetNamespace"))
							element.setAttribute("targetNamespace", definition.getTargetNamespace());
						
						// read schema
						XmlSchema xmlSchema = c.read(element);
						
						String schemaNamespace = xmlSchema.getTargetNamespace();
						namespaceList.add(schemaNamespace);
						if (definition.getTargetNamespace().equals(schemaNamespace)) {
							mainSchemaNamespace = schemaNamespace;
						}
					}
				}
			}

			// Then load schemas into our Collection
			String baseURI = ((DefaultURIResolver)c.getSchemaResolver()).getCollectionBaseURI();
			if (baseURI != null) collection.setBaseUri(baseURI);
			for (XmlSchema xs : c.getXmlSchemas()) {
				String nsuri = xs.getTargetNamespace();
				if (namespaceList.contains(nsuri)) {
					if (collection.schemaForNamespace(nsuri) == null) {
						// ! schema with this ns does not exist !
						// ! because of xsd:include it is possible to have more than one schema with the same nsuri !
						// ! we must retrieve the whole schema through collection.schemaForNamespace instead of using xs !
						if (c.schemaForNamespace(nsuri) != null) {
							collection.read(c.schemaForNamespace(nsuri).getSchemaDocument(),null);
							if (nsuri.equals(mainSchemaNamespace)) {
								bImport = true;
							}
						}
					}
					else {
						if (!bImport) {
							// ! a previously imported (from wsdl) schema already exist with same ns !
							// ! we need to add all items to existing schema !
							XmlSchema xc1 = c.schemaForNamespace(nsuri);
							XmlSchema xc2 = collection.schemaForNamespace(nsuri);
							for (XmlSchemaObject ob : new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaObject>(xc1.getItems())) {
								XmlSchemaUtils.add(xc2, ob);
							}
						}
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
			
			// Remember main schema to import in project's one
			if (mainSchemaNamespace != null) {
				if (bImport)
					mainSchema = collection.schemaForNamespace(mainSchemaNamespace);
				else
					mainSchema = null;
			}
			
			return mainSchema;
		}
		catch (Exception e) {
			Engine.logBeans.error(e.getMessage(), e);
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
				//reader.setFeature("javax.wsdl.importDocuments", true);
				Definition definition = reader.readWSDL(null, wsdlURL.toString());
				list.addAll(readWsdlImports(definition));
			}
		}
		catch (Exception e) {
			Engine.logBeans.error(e.getMessage(), e);
		}
		return list;
	}
	
	protected List<Definition> readWsdlImports(Definition definition) {
		List<Definition> list = new ArrayList<Definition>();
		Map<String, List<Import>> imports = GenericUtils.cast(definition.getImports());
		for (List<Import> iList : imports.values()) {
			for (Import wsdlImport : iList) {
				Definition def = wsdlImport.getDefinition();
				list.addAll(readWsdlImports(def));
			}
		}
		
		// add soap-encoding import if needed (for validation)
		addRpcSoapEncImport(definition);
		
		list.add(definition);
		return list;
	}

	protected void addRpcSoapEncImport(Definition definition) {
		boolean hasRpc = false;
		Map<QName, Binding> bmap = GenericUtils.cast(definition.getBindings());
		Iterator<QName> it = bmap.keySet().iterator();
		while (it.hasNext()) {
			Binding bind = bmap.get(it.next());
			List<ExtensibilityElement> exs = GenericUtils.cast(bind.getExtensibilityElements());
			for (ExtensibilityElement ee : exs) {
				if (ee instanceof SOAPBinding) {
					String style = ((SOAPBinding)ee).getStyle();
					if (style != null && style.toLowerCase().equals("rpc")) {
						hasRpc = true;
					}
				}
			}
		}
		if (hasRpc) {
			Types types = definition.getTypes();
			Iterator<?> exs = types.getExtensibilityElements().iterator();
			while (exs.hasNext()) {
				ExtensibilityElement ee = (ExtensibilityElement)exs.next();
				if (ee instanceof Schema) {
					Element se = ((Schema)ee).getElement();
					SchemaUtils.addSoapEncSchemaImport(se);
				}
			}
		}
	}
}
