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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.references.ImportLocalXsdReference;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;

public class Migration6_3_0 {

	public static void migrate(String projectName) {
		try {
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
			String projectXsdFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/"+ projectName + ".xsd";
			File xsdFile = new File(projectXsdFilePath);
			if (xsdFile.exists()) {
				
				// Copy all xsd files to xsd directory
				File destDir = new File(project.getXsdDirPath());
				copyXsdOfProject(projectName, destDir);
				
				Map<String, Reference> referenceMap = new HashMap<String, Reference>();
				
				// Load project schema from old XSD file
				XmlSchemaCollection collection = new XmlSchemaCollection();
				XmlSchema projectSchema = SchemaUtils.loadSchema(new File(projectXsdFilePath), collection);
				SchemaMeta.setCollection(projectSchema, collection);
				
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
											
											// This is a convertigo project reference
											if (location.startsWith("../")) {
												// Copy all xsd files to xsd directory
												String targetProjectName = location.substring(3, location.indexOf("/",3));
												copyXsdOfProject(targetProjectName, destDir);
												
												location = location.substring(location.lastIndexOf("/")+1);
											}
											
											// Add reference
											addReferenceToMap(referenceMap, namespaceURI, location);
											
											// Add import
											addImport(transactionSchema, namespaceURI, location);
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
					
					for (Sequence sequence: project.getSequencesList()) {
						// Modify sequence's typeName
						// TODO: set the correct property when implemented : should be a QName !!
						//sequence.setComment(new QName(project.getTargetNamespace(),sequence.getName()+ "ResponseData").toString());
						handleSteps(destDir, projectSchema,referenceMap, sequence.getSteps());
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
	
	private static void copyXsdOfProject(String projectName, File destDir) throws IOException {
		File srcDir = new File(Engine.PROJECTS_PATH + "/" + projectName);
		Collection<File> xsdFiles = GenericUtils.cast(FileUtils.listFiles(srcDir, new String[] { "xsd" }, false));
		for (File file: xsdFiles) {
			FileUtils.copyFileToDirectory(file, destDir);
		}
	}
	
	private static void addImport(XmlSchema xmlSchema, String namespaceURI, String location) {
		XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
		xmlSchemaImport.setSchemaLocation("../../" + location);
		xmlSchemaImport.setNamespace(namespaceURI);
		xmlSchema.getIncludes().add(xmlSchemaImport);
		xmlSchema.getItems().add(xmlSchemaImport);
	}
	
	private static void addReferenceToMap(Map<String, Reference> referenceMap, String namespaceURI, String location) throws EngineException {
		if (!referenceMap.containsKey(namespaceURI)) {
			ImportLocalXsdReference reference = new ImportLocalXsdReference();
			reference.setName(StringUtils.normalize(location));
			reference.setFilepath(".//xsd/"+location);
			referenceMap.put(namespaceURI, reference);
		}
	}
	
	private static void handleSteps(File destDir, XmlSchema projectSchema, Map<String, Reference> referenceMap, List<Step> stepList) {
		for (Step step: stepList) {
			String targetProjectName = null;
			String typeLocalName = null;
			if (step instanceof TransactionStep) {
				targetProjectName = ((TransactionStep)step).getProjectName();
				typeLocalName = ((TransactionStep)step).getConnectorName()+ "__"	+
							((TransactionStep)step).getTransactionName() 	+
							"ResponseData";
			}
			else if (step instanceof SequenceStep) {
				targetProjectName = ((SequenceStep)step).getProjectName();
				typeLocalName = ((SequenceStep)step).getSequenceName() +
							"ResponseData";
			}
			
			String namespaceURI = null;
			if (targetProjectName != null) {
				try {
					namespaceURI = Project.CONVERTIGO_PROJECTS_NAMESPACEURI + targetProjectName;
					if (!targetProjectName.equals(step.getProject().getName())) {
						// Copy all xsd files to xsd directory
						copyXsdOfProject(targetProjectName, destDir);
						
						String location = targetProjectName + ".xsd";
						try {
							namespaceURI = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(targetProjectName).getTargetNamespace();
						}
						catch (Exception e) {}
						
						// Add reference
						addReferenceToMap(referenceMap, namespaceURI, location);
					}
						
					// Set step's typeQName
					step.setXmlComplexTypeAffectation(new XmlQName(new QName(namespaceURI,typeLocalName)));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				String targetNamespace = projectSchema.getTargetNamespace();
				String targetPrefix = projectSchema.getNamespaceContext().getPrefix(targetNamespace);
				
				String s = step.getSchemaType(targetPrefix);
				if ((s != null) && (!s.equals("")) && (!s.startsWith("xsd:"))) {
					String prefix = s.split(":")[0];
					typeLocalName = s.split(":")[1];
					if (prefix.equals(targetPrefix)) {
						// TODO: ignore or handle XmlCopy ??
					}
					else {
						// Retrieve namespace uri
						namespaceURI = projectSchema.getNamespaceContext().getNamespaceURI(prefix);
						
						// Set step's typeQName
						step.setXmlComplexTypeAffectation(new XmlQName(new QName(namespaceURI,typeLocalName)));
					}
				}
			}
			
			if (step instanceof StepWithExpressions) {
				handleSteps(destDir, projectSchema,referenceMap, ((StepWithExpressions)step).getSteps());
			}
		}
	}
}
