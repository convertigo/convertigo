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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.Engine;

public class SchemaUtils {
	
	private static ThreadLocal<DocumentBuilderFactory> defaultDocumentBuilderFactory = new ThreadLocal<DocumentBuilderFactory>() {
		protected DocumentBuilderFactory initialValue() {
			try {
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
				documentBuilderFactory.setNamespaceAware(true);
				documentBuilderFactory.setCoalescing(true);
				documentBuilderFactory.setFeature("http://apache.org/xml/features/validation/schema/normalized-value", false);
				
				return documentBuilderFactory;
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				return null;
			}
		}
	};
    
	private static ThreadLocal<DocumentBuilder> defaultDocumentBuilder = new ThreadLocal<DocumentBuilder>() {
		protected DocumentBuilder initialValue() {
			try {
				return defaultDocumentBuilderFactory.get().newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				return null;
			}
		}
	};
	
	public static DocumentBuilder getDefaultDocumentBuilder() {
		return defaultDocumentBuilder.get();
	}

	public static XmlSchema loadSchema(String xsdFilePath) throws SAXException, IOException {
		File xsdFile = new File(xsdFilePath);
		if (xsdFile.exists()) {
			Document xsdDocument = getDefaultDocumentBuilder().parse(xsdFile);
			XmlSchemaCollection xmlSchemaCollection = new XmlSchemaCollection();
			xmlSchemaCollection.setBaseUri(xsdFilePath.substring(0, xsdFilePath.lastIndexOf("/")+1));
			XmlSchema xmlSchema = xmlSchemaCollection.read(xsdDocument, null);
			return xmlSchema;
		}
		return null;
	}
	
	public static XmlSchemaCollection createSchemaCollection() {
		NamespaceMap nsMap = new NamespaceMap();
		nsMap.add("xsd", Constants.URI_2001_SCHEMA_XSD);
		return createSchemaCollection(nsMap);
	}
	
	public static XmlSchemaCollection createSchemaCollection(NamespaceMap nsMap) {
		XmlSchemaCollection xmlSchemaCollection = new XmlSchemaCollection();
		xmlSchemaCollection.setNamespaceContext(nsMap);
		return xmlSchemaCollection;
	}

	public static XmlSchema createSchema(String prefix, String targetNamespace, String elementFormDefault, String attributeFormDefault) {
		NamespaceMap nsMap = new NamespaceMap();
		nsMap.add("xsd", Constants.URI_2001_SCHEMA_XSD);
		nsMap.add(prefix, targetNamespace);
		
		XmlSchemaCollection xmlSchemaCollection = createSchemaCollection(nsMap);
		
		XmlSchema xmlSchema = new XmlSchema(targetNamespace, xmlSchemaCollection);
		xmlSchema.setNamespaceContext(nsMap);
		xmlSchema.setElementFormDefault(new XmlSchemaForm(elementFormDefault));
		xmlSchema.setAttributeFormDefault(new XmlSchemaForm(attributeFormDefault));
		return xmlSchema;
	}
	
	public static void saveSchema(String xsdFilePath, XmlSchema xmlSchema) throws IOException {
		HashMap<String,String> options = new HashMap<String,String>();
		options.put(OutputKeys.METHOD, "xml");
		options.put(OutputKeys.ENCODING, "UTF-8");
		options.put(OutputKeys.INDENT, "yes");
		options.put("{http://xml.apache.org/xslt}indent-amount", "4");
		options.put(OutputKeys.OMIT_XML_DECLARATION, "no");
		options.put(OutputKeys.CDATA_SECTION_ELEMENTS, "{"+Constants.URI_2001_SCHEMA_XSD+"}documentation");
		
		saveSchema(xsdFilePath, xmlSchema, options);
	}
	
	private static void saveSchema(String xsdFilePath, XmlSchema xmlSchema, HashMap<String,String> options) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(xsdFilePath);
			xmlSchema.write(fos, options);
		}
		finally {
			if (fos != null) fos.close();
		}
	}
	
	public static NamespaceMap getNamespaceMap(XmlSchema xmlSchema) {
		NamespaceMap namespacesMap = new NamespaceMap();
		NamespacePrefixList namespacePrefixList = xmlSchema.getNamespaceContext();
		String[] prefixes = namespacePrefixList.getDeclaredPrefixes();
		for (int i=0; i<prefixes.length; i++) {
			String prefix = prefixes[i];
			String ns = namespacePrefixList.getNamespaceURI(prefix);
			namespacesMap.add(prefix, ns);
		}
		return namespacesMap;
	}
	
	public static void testMigration_6_3_0(String projectName) {
		try {
			Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			String projectXsdFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/"+ projectName + ".xsd";
			File xsdFile = new File(projectXsdFilePath);
			if (xsdFile.exists()) {
				// Load project schema from old XSD file
				XmlSchema projectSchema = loadSchema(projectXsdFilePath);
				
				for (Connector connector: project.getConnectorsList()) {
					for (Transaction transaction: connector.getTransactionsList()) {
						try {
							// Retrieve required XmlSchemaObjects for transaction
							QName requestQName = new QName(project.getTargetNamespace(), transaction.getXsdRequestElementName());
							QName responseQName = new QName(project.getTargetNamespace(), transaction.getXsdResponseElementName());
							LinkedHashMap<QName, XmlSchemaObject> map = new LinkedHashMap<QName, XmlSchemaObject>();
							XmlSchemaWalker dw = XmlSchemaWalker.newDependencyWalker(map);
							dw.walkByElementRef(projectSchema, requestQName, true);
							dw.walkByElementRef(projectSchema, responseQName, true);
							
							// Create transaction schema
							String targetNamespace = projectSchema.getTargetNamespace();
							String prefix = projectSchema.getNamespaceContext().getPrefix(targetNamespace);
							String elementFormDefault = projectSchema.getElementFormDefault().getValue();
							String attributeFormDefault = projectSchema.getAttributeFormDefault().getValue();
							XmlSchema transactionSchema = createSchema(prefix, targetNamespace, elementFormDefault, attributeFormDefault);
							
							// Add required prefix declarations
							List<String> nsList = new LinkedList<String>();
							for (QName qname: map.keySet()) {
								String nsURI = qname.getNamespaceURI();
								if (!nsURI.equals(Constants.URI_2001_SCHEMA_XSD)) {
									if (!nsList.contains(nsURI)) {
										nsList.add(nsURI);
									}
								}
								String nsPrefix = qname.getPrefix();
								if (!nsURI.equals(targetNamespace)) {
									NamespaceMap nsMap = getNamespaceMap(transactionSchema);
									if (nsMap.getNamespaceURI(nsPrefix) == null) {
										nsMap.add(nsPrefix, nsURI);
										transactionSchema.setNamespaceContext(nsMap);
									}
								}
							}
							
							// Add required imports
							for (String namespaceURI: nsList) {
								XmlSchemaObjectCollection includes = projectSchema.getIncludes();
								for (int i=0; i < includes.getCount(); i++) {
									XmlSchemaObject xmlSchemaObject = includes.getItem(i);
									if (xmlSchemaObject instanceof XmlSchemaImport) {
										if (((XmlSchemaImport) xmlSchemaObject).getNamespace().equals(namespaceURI)) {
											XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
											xmlSchemaImport.setSchemaLocation("../../../" +((XmlSchemaImport) xmlSchemaObject).getSchemaLocation());
											xmlSchemaImport.setNamespace(namespaceURI);
											transactionSchema.getIncludes().add(xmlSchemaImport);
											transactionSchema.getItems().add(xmlSchemaImport);
										}
									}
								}
							}
							
							
							// Add required schema objects
							for (QName qname: map.keySet()) {
								if (qname.getNamespaceURI().equals(targetNamespace)) {
									transactionSchema.getItems().add(map.get(qname));
								}
							}
							
							// Save schema to file
							String transactionXsdFilePath = transaction.getSchemaFilePath().replaceFirst("\\.xsd", ".mgr.xsd");
							new File(transaction.getSchemaFileDirPath()).mkdirs();
							saveSchema(transactionXsdFilePath, transactionSchema);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
