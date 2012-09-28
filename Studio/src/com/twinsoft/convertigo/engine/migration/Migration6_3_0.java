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

package com.twinsoft.convertigo.engine.migration;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.references.ImportLocalXsdReference;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;

public class Migration6_3_0 {

	public static void migrate(String projectName) {
		try {
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
			String projectXsdFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/"+ projectName + ".xsd";
			File xsdFile = new File(projectXsdFilePath);
			if (xsdFile.exists()) {
				Map<String, Reference> referenceMap = new HashMap<String, Reference>();
				
				// Load project schema from old XSD file
				XmlSchema projectSchema = SchemaUtils.loadSchema(projectXsdFilePath);
				
				for (Connector connector: project.getConnectorsList()) {
					for (Transaction transaction: connector.getTransactionsList()) {
						try {
							// Retrieve required XmlSchemaObjects for transaction
							QName requestQName = new QName(project.getTargetNamespace(), transaction.getXsdRequestElementName());
							QName responseQName = new QName(project.getTargetNamespace(), transaction.getXsdResponseElementName());
							LinkedHashMap<QName, XmlSchemaObject> map = new LinkedHashMap<QName, XmlSchemaObject>();
							XmlSchemaWalker dw = XmlSchemaWalker.newDependencyWalker(map, true, false);
							dw.walkByElementRef(projectSchema, requestQName);
							dw.walkByElementRef(projectSchema, responseQName);
							
							// Create transaction schema
							String targetNamespace = projectSchema.getTargetNamespace();
							String prefix = projectSchema.getNamespaceContext().getPrefix(targetNamespace);
							String elementFormDefault = projectSchema.getElementFormDefault().getValue();
							String attributeFormDefault = projectSchema.getAttributeFormDefault().getValue();
							XmlSchema transactionSchema = SchemaUtils.createSchema(prefix, targetNamespace, elementFormDefault, attributeFormDefault);
							
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
									NamespaceMap nsMap = SchemaUtils.getNamespaceMap(transactionSchema);
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
											String location = ((XmlSchemaImport) xmlSchemaObject).getSchemaLocation();
											if (!referenceMap.containsKey(namespaceURI)) {
												// Create a new local reference for project
												ImportLocalXsdReference reference = new ImportLocalXsdReference();
												reference.setName(location);
												reference.setFilepath(".//"+location);
												referenceMap.put(namespaceURI, reference);
											}
											
											// Add import
											XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
											xmlSchemaImport.setSchemaLocation("../../../" + location);
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
							String transactionXsdFilePath = transaction.getSchemaFilePath();
							new File(transaction.getSchemaFileDirPath()).mkdirs();
							SchemaUtils.saveSchema(transactionXsdFilePath, transactionSchema);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				// Add all references to project
				if (!referenceMap.isEmpty()) {
					for (Reference reference: referenceMap.values())
						project.add(reference);
				}
				
				//TODO: delete XSD/WSDL files....
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
