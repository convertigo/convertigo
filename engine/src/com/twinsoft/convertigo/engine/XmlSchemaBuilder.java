/*
 * Copyright (c) 2001-2023 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.w3c.dom.Attr;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.IElementRefAffectation;
import com.twinsoft.convertigo.beans.core.ISchemaAttributeGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaImportGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaIncludeGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaParticleGenerator;
import com.twinsoft.convertigo.beans.core.ISimpleTypeAffectation;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;

public class XmlSchemaBuilder {

	protected class XmlSchemaFullBuilder extends XmlSchemaBuilder {
		protected XmlSchemaFullBuilder(String projectName) {
			super(projectName);
			this.isFull = true;
		}
	}

	private XmlSchemaBuilderExecutor builderExecutor;
	private XmlSchemaCollection collection;
	private XmlSchema schema;
	private String projectName;
	private Project project;
	protected boolean isFull = false;
	
	protected XmlSchemaBuilder(String projectName) {
		this.projectName = projectName;
	}
	
	@Override
	public boolean equals(Object obj) {
		try {
			return this.hashCode() == obj.hashCode();
		} catch (Exception e) {}
		return false;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "@" + this.projectName + "@" + Boolean.valueOf(this.isFull).toString();
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	protected XmlSchema getXmlSchema() {
		return schema;
	}
	
	protected String getProjectName() {
		return projectName;
	}

	protected String getTargetNamespace() {
		return project.getTargetNamespace();
	}
	
	private XmlSchemaType resolveTypeByQName(QName qname) {
		if (qname != null) {
			//System.out.println("resolving qname: "+ qname.toString());
			String namespaceURI = qname.getNamespaceURI();
			if (namespaceURI.length() == 0 || namespaceURI.equals(getTargetNamespace()) || namespaceURI.equals(Constants.URI_2001_SCHEMA_XSD)) {
				return schema.getTypeByName(qname) != null ? schema.getTypeByName(qname) : collection.getTypeByQName(qname);
			} else {
				final XmlSchemaBuilder builder = builderExecutor.getBuilderByTargetNamespace(namespaceURI);
				if (builder != null) {
					return builder.resolveTypeByQName(qname);
				}
			}
			System.out.println("warning: could not retrieve XmlSchemaType for "+ namespaceURI);
		}
		return null;
	}
	
	private XmlSchemaElement resolveElementByQName(QName qname) {
		if (qname != null) {
			String namespaceURI = qname.getNamespaceURI();
			if (namespaceURI.length() == 0 || namespaceURI.equals(getTargetNamespace()) || namespaceURI.equals(Constants.URI_2001_SCHEMA_XSD)) {
				return schema.getElementByName(qname) != null ? schema.getElementByName(qname) : collection.getElementByQName(qname);
			} else {
				final XmlSchemaBuilder builder = builderExecutor.getBuilderByTargetNamespace(namespaceURI);
				if (builder != null) {
					return builder.resolveElementByQName(qname);
				}
			}
			System.out.println("warning: could not retrieve XmlSchemaElement for "+ namespaceURI);
		}
		return null;
	}
	
	protected void beginBuildSchema(XmlSchemaBuilderExecutor builderExecutor) throws EngineException {
		try {
			this.builderExecutor = builderExecutor;
			initBuildSchema();
			preBuildSchema();
			midBuildSchema(0); // first pass
		} catch (Exception e) {
			throw new EngineException("beginBuildSchema failed", e);
		}
	}
	
	private void initBuildSchema() throws EngineException {
		try {
			project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
			this.collection = new XmlSchemaCollection();
			this.schema = XmlSchemaUtils.makeDynamicReadOnly(project, new XmlSchema(project.getTargetNamespace(), collection));
			SchemaMeta.setCollection(schema, collection);
		} catch (Exception e) {
			throw new EngineException("initBuildSchema failed", e);
		}
	}
	
	private void preBuildSchema() throws EngineException {
		try {
			schema.setElementFormDefault(new XmlSchemaForm(project.getSchemaElementForm().name()));
			schema.setAttributeFormDefault(new XmlSchemaForm(project.getSchemaElementForm().name()));
			ConvertigoError.addXmlSchemaObjects(schema);
			EngineStatistics.addXmlSchemaObjects(schema);
			
			// static and read-only generation : references, transactions, sequences declaration
			new WalkHelper() {
				
				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {

					if (databaseObject instanceof ISchemaGenerator) {
							// generate itself and add to the caller list
							if (databaseObject instanceof ISchemaImportGenerator) {
								// Import case
								if (databaseObject instanceof ProjectSchemaReference) {
									// done further in buildSchema
								} else {
									XmlSchemaImport schemaImport = ((ISchemaImportGenerator) databaseObject).getXmlSchemaObject(collection, schema);
									if (schemaImport != null) {
										SchemaMeta.setXmlSchemaObject(schema, databaseObject, schemaImport);
										XmlSchemaUtils.add(schema, schemaImport);
									}
								}
							} else if (databaseObject instanceof ISchemaIncludeGenerator) {
								// Include case
								if (databaseObject instanceof Transaction) {
									XmlSchemaInclude schemaInclude = ((ISchemaIncludeGenerator)databaseObject).getXmlSchemaObject(collection, schema);
									addSchemaIncludeObjects(databaseObject, schemaInclude.getSchema());
								} else {
									XmlSchemaInclude schemaInclude = ((ISchemaIncludeGenerator)databaseObject).getXmlSchemaObject(collection, schema);
									SchemaMeta.setXmlSchemaObject(schema, databaseObject, schemaInclude);
									XmlSchemaUtils.add(schema, schemaInclude);
								}
							} else if (databaseObject instanceof Sequence) {
								// Sequence case
								XmlSchemaElement element = ((Sequence) databaseObject).getXmlSchemaObject(collection, schema);
								SchemaMeta.setXmlSchemaObject(schema, databaseObject, element);
								XmlSchemaUtils.add(schema, element);
							}
					} else {
						// doesn't generate schema, just deep walk
						super.walk(databaseObject);
					}
				}

				@Override
				protected boolean before(DatabaseObject databaseObject, Class<? extends DatabaseObject> dboClass) {
					// just walk references, transactions, sequences declaration
					return 	Step.class.isAssignableFrom(dboClass) ||
							Transaction.class.isAssignableFrom(dboClass) || 
							Sequence.class.isAssignableFrom(dboClass) ||
							Reference.class.isAssignableFrom(dboClass) ||
							Connector.class.isAssignableFrom(dboClass);
				}
				
				protected void addSchemaIncludeObjects(DatabaseObject databaseObject, XmlSchema xmlSchema) {
					if (xmlSchema != null) {
						XmlSchemaObjectCollection c = xmlSchema.getItems();
						Iterator<XmlSchemaObject> it = GenericUtils.cast(c.getIterator());
						while (it.hasNext()) {
							XmlSchemaObject xmlSchemaObject  = it.next();
							SchemaMeta.getReferencedDatabaseObjects(xmlSchemaObject).add(databaseObject);

							if (xmlSchemaObject instanceof XmlSchemaImport) {
								// ignore
							}
							else if (xmlSchemaObject instanceof XmlSchemaInclude) {
								XmlSchemaInclude schemaInclude = (XmlSchemaInclude)xmlSchemaObject;
								addSchemaIncludeObjects(databaseObject, schemaInclude.getSchema());
							}
							else if (xmlSchemaObject instanceof XmlSchemaAttribute) {
								XmlSchemaAttribute attribute = (XmlSchemaAttribute) xmlSchemaObject;
								if (schema.getAttributes().getItem(attribute.getQName()) == null) {
									schema.getAttributes().add(attribute.getQName(), attribute);
									schema.getItems().add(attribute);
								}
							}
							else if (xmlSchemaObject instanceof XmlSchemaAttributeGroup) {
								XmlSchemaAttributeGroup attributeGroup = (XmlSchemaAttributeGroup) xmlSchemaObject;
								if (schema.getAttributeGroups().getItem(attributeGroup.getName()) == null) {
									schema.getAttributeGroups().add(attributeGroup.getName(), attributeGroup);
									schema.getItems().add(attributeGroup);
								}
							}
							else if (xmlSchemaObject instanceof XmlSchemaGroup) {
								XmlSchemaGroup group = (XmlSchemaGroup) xmlSchemaObject;
								if (schema.getGroups().getItem(group.getName()) == null) {
									schema.getGroups().add(group.getName(), group);
									schema.getItems().add(group);
								}
							}
							else if (xmlSchemaObject instanceof XmlSchemaElement) {
								XmlSchemaElement element = (XmlSchemaElement) xmlSchemaObject;
								if (collection.getElementByQName(element.getQName()) == null &&
										schema.getElementByName(element.getQName()) == null) {
									schema.getElements().add(element.getQName(), element);
									schema.getItems().add(element);
								}
							}
							else if (xmlSchemaObject instanceof XmlSchemaType) {
								XmlSchemaType schemaType = (XmlSchemaType) xmlSchemaObject;
								if (collection.getTypeByQName(schemaType.getQName()) == null &&
										schema.getTypeByName(schemaType.getQName()) == null) {
									schema.addType(schemaType);
									schema.getItems().add(schemaType);
								}
							}
							else {
								schema.getItems().add(xmlSchemaObject);
							}
						}
					}
				}
			}.init(project);
		} catch (Exception e) {
			throw new EngineException("preBuildSchema failed", e);
		}
	}
	
	private void midBuildSchema(final int nb) throws EngineException {
		//System.out.println("buildSchema for "+ getTargetNamespace());
		
		boolean fullSchema = isFull;
		
		try {
			new WalkHelper() {
				
				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {

					if (databaseObject instanceof ISchemaGenerator) {
							// generate itself and add to the caller list
							if (databaseObject instanceof ISchemaImportGenerator) {
								// Import case
								if (databaseObject instanceof ProjectSchemaReference) {
									ProjectSchemaReference ref = (ProjectSchemaReference)databaseObject;
									String targetProjectName = ref.getParser().getProjectName();
									String tns = Project.getProjectTargetNamespace(targetProjectName);
									if (collection.schemaForNamespace(tns) == null) {
										if (SchemaMeta.getXmlSchemaObject(schema, databaseObject) == null) {
											XmlSchemaImport schemaImport = new XmlSchemaImport();
											schemaImport.setNamespace(tns);
											SchemaMeta.setXmlSchemaObject(schema, databaseObject, schemaImport);
											XmlSchemaUtils.add(schema, schemaImport);
										} else {
											XmlSchemaBuilder builder = builderExecutor.getBuilderByTargetNamespace(tns);
											if (builder != null) {
												XmlSchemaUtils.remove(schema, SchemaMeta.getXmlSchemaObject(schema, databaseObject));
												XmlSchema xmlSchema = collection.read(builder.schema.getSchemaDocument(), null);
												XmlSchemaImport schemaImport = new XmlSchemaImport();
												schemaImport.setNamespace(tns);
												schemaImport.setSchema(xmlSchema);
												SchemaMeta.setXmlSchemaObject(schema, databaseObject, schemaImport);
												XmlSchemaUtils.add(schema, schemaImport);
											}
										}
									}
								}
							}
					} else {
						// doesn't generate schema, just deep walk
						super.walk(databaseObject);
					}
				}

				@Override
				protected boolean before(DatabaseObject databaseObject, Class<? extends DatabaseObject> dboClass) {
					// just walk references
					return 	Reference.class.isAssignableFrom(dboClass);
				}
			}.init(project);

			// add missing references import
			if (nb == 1) {
				if (this.equals(builderExecutor.getMainBuilder())) {
					List<String> refs = new ArrayList<String>();
					SchemaManager.getProjectReferences(refs, projectName);
					List<String> missing = new ArrayList<String>();
					missing.addAll(refs);
					missing.remove(projectName);
					
					for (String pname: refs) {
						XmlSchemaObjectCollection col = schema.getIncludes();
						for (int i= 0; i < col.getCount(); i++) {
							XmlSchemaObject ob = col.getItem(i);
							if (ob instanceof XmlSchemaImport) {
								XmlSchemaImport xmlSchemaImport = (XmlSchemaImport)ob;
								String tns = Project.getProjectTargetNamespace(pname);
								if (xmlSchemaImport.getNamespace().equals(tns)) {
									missing.remove(pname);
								}
							}
						}
					}
					
					for (String pname: missing) {
						String tns = Project.getProjectTargetNamespace(pname);
						XmlSchemaBuilder builder = builderExecutor.getBuilderByTargetNamespace(tns);
						if (builder != null) {
							XmlSchema xmlSchema = collection.read(builder.schema.getSchemaDocument(), null);
							XmlSchemaImport schemaImport = new XmlSchemaImport();
							schemaImport.setNamespace(tns);
							schemaImport.setSchema(xmlSchema);
							XmlSchemaUtils.add(schema, schemaImport);
						}
					}
				}
			}
			
			new WalkHelper() {
				List<XmlSchemaParticle> particleChildren;
				List<XmlSchemaAttribute> attributeChildren;
				
				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					// Transaction case
					if (databaseObject instanceof Transaction) {
						Transaction transaction = (Transaction)databaseObject;
						String ns = schema.getTargetNamespace();
						List<QName> partElementQNames = new ArrayList<QName>();
						partElementQNames.add(new QName(ns, transaction.getXsdRequestElementName()));
						partElementQNames.add(new QName(ns, transaction.getXsdResponseElementName()));
						LinkedHashMap<QName, XmlSchemaObject> map = new LinkedHashMap<QName, XmlSchemaObject>();
						XmlSchemaWalker dw = XmlSchemaWalker.newDependencyWalker(map, true, true);
						for (QName qname: partElementQNames) {
							dw.walkByElementRef(schema, qname);
						}
						for (QName qname: map.keySet()) {
							String nsURI = qname.getNamespaceURI();
							if (nsURI.equals(ns)) continue;
							if (nsURI.equals(Constants.URI_2001_SCHEMA_XSD)) continue;
							SchemaManager.addXmlSchemaImport(collection, schema, nsURI);
						}
						map.clear();
						
						// add the 'statistics' element
						if (transaction.getAddStatistics()) {
							XmlSchemaComplexType xmlSchemaComplexType = (XmlSchemaComplexType) schema.getTypeByName(transaction.getXsdResponseTypeName());
							XmlSchemaGroupBase xmlSchemaGroupBase = (XmlSchemaGroupBase)xmlSchemaComplexType.getParticle();
							XmlSchemaType statisticsType = schema.getTypeByName("ConvertigoStatsType");
							XmlSchemaElement eStatistics = XmlSchemaUtils.makeDynamicReadOnly(databaseObject, new XmlSchemaElement());
							eStatistics.setName("statistics");
							eStatistics.setMinOccurs(0);
							eStatistics.setMaxOccurs(1);
							eStatistics.setSchemaTypeName(statisticsType.getQName());
							xmlSchemaGroupBase.getItems().add(eStatistics);
							SchemaMeta.getReferencedDatabaseObjects(statisticsType).add(transaction);
						}
					}
					// Sequence case
					else if (databaseObject instanceof Sequence) {
						Sequence sequence = (Sequence) databaseObject;
						//System.out.println("--sequence:"+ sequence.toString());
						particleChildren = new LinkedList<XmlSchemaParticle>();
						attributeChildren = new LinkedList<XmlSchemaAttribute>();
						
						super.walk(databaseObject);
						
						// check for an 'error' element if needed
						boolean errorFound = false;
						XmlSchemaType errorType = schema.getTypeByName("ConvertigoError");
						if (errorType != null) {
							Set<DatabaseObject> dbos = SchemaMeta.getReferencedDatabaseObjects(errorType);
							for (DatabaseObject dbo : dbos) {
								if (dbo instanceof Step) {
									Step errorStep = (Step)dbo;
									if (errorStep.getSequence().equals(sequence) && (errorStep instanceof XMLCopyStep || errorStep.getStepNodeName().equals("error"))) {
										errorFound = true;
										break;
									}
								}
							}
						}
						
						// set particle : choice or sequence
						XmlSchemaComplexType cType = (XmlSchemaComplexType) schema.getTypeByName(sequence.getComplexTypeAffectation().getLocalPart());
						XmlSchemaSequence xmlSeq = new XmlSchemaSequence();
						XmlSchemaChoice xmlChoice = new XmlSchemaChoice();
						cType.setParticle(errorFound ? xmlSeq : xmlChoice);
						if (!errorFound) {
							XmlSchemaElement eError = XmlSchemaUtils.makeDynamicReadOnly(databaseObject, new XmlSchemaElement());
							eError.setName("error");
							eError.setMinOccurs(0);
							eError.setMaxOccurs(1);
							eError.setSchemaTypeName(errorType.getQName());
							SchemaMeta.getReferencedDatabaseObjects(errorType).add(sequence);
							
							xmlChoice.getItems().add(xmlSeq);
							xmlChoice.getItems().add(eError);
						}
													
						// add child particles
						if (!particleChildren.isEmpty()) {
							for (XmlSchemaParticle child : particleChildren) {
								xmlSeq.getItems().add(child);
							}
						}
						particleChildren.clear();
						
						// add child attributes
						for (XmlSchemaAttribute attribute : attributeChildren) {
							cType.getAttributes().add(attribute);
						}
						attributeChildren.clear();
						
						// add the 'statistics' element
						if (sequence.getAddStatistics()) {
							XmlSchemaType statisticsType = schema.getTypeByName("ConvertigoStatsType");
							XmlSchemaElement eStatistics = XmlSchemaUtils.makeDynamicReadOnly(databaseObject, new XmlSchemaElement());
							eStatistics.setName("statistics");
							eStatistics.setMinOccurs(0);
							eStatistics.setMaxOccurs(1);
							eStatistics.setSchemaTypeName(statisticsType.getQName());
							xmlSeq.getItems().add(eStatistics);
							SchemaMeta.getReferencedDatabaseObjects(statisticsType).add(sequence);
						}

						//--------------------------- For Further Use -------------------------------------------------//
						//Modify schema to avoid 'cosamb' (same tagname&type in different groupBase at same level)
						//TODO : IfThenElse steps must be modified for xsd:sequence instead of xsd:choice
						//TODO : Then/Else steps must be modified to add minOccurs=0 on xsd:sequence
						//TODO : review/improve cosnoamb(XmlSchema, XmlSchemaGroupBase, XmlSchemaGroupBase) method
						//---------------------------------------------------------------------------------------------//
					}
					// Step case
					else if (databaseObject instanceof Step) {
						Step step = (Step) databaseObject;
						if (!step.isEnabled()) {
							// stop walking for disabled steps
							return;
						}

						List<XmlSchemaParticle> parentParticleChildren = particleChildren;
						List<XmlSchemaAttribute> parentAttributeChildren = attributeChildren;
						
						//System.out.println("step:"+ step.toString());
						if (step instanceof TransactionStep) {
							//System.out.println("SCHEMA TARGET STEP "+ step.toString() + "(" + step.hashCode() + ")");								
						}
						
						if (step.isGenerateSchema() || (fullSchema && step.isXmlOrOutput())) {
							//System.out.println("-> generate schema...");
							
							List<XmlSchemaParticle> myParticleChildren = null;
							List<XmlSchemaAttribute> myAttributeChildren = null;
							
							// is base affected ?
							@SuppressWarnings("unused")
							XmlSchemaType base = null;
							QName baseQName = step instanceof ISimpleTypeAffectation ? ((ISimpleTypeAffectation) step).getSimpleTypeAffectation() : null;
							if (baseQName != null && baseQName.getLocalPart().length() > 0) {
								//base = baseQName.getNamespaceURI().length() == 0 ? schema.getTypeByName(baseQName.getLocalPart()) : collection.getTypeByQName(baseQName);
								base = XmlSchemaBuilder.this.resolveTypeByQName(baseQName);
							}
							
							// is type affected ?
							XmlSchemaType type = null;
							QName typeQName = step instanceof IComplexTypeAffectation ? ((IComplexTypeAffectation) step).getComplexTypeAffectation() : null;
							if (typeQName != null && typeQName.getLocalPart().length() > 0) {
								//type = typeQName.getNamespaceURI().length() == 0 ? schema.getTypeByName(typeQName.getLocalPart()) : collection.getTypeByQName(typeQName);
								type = XmlSchemaBuilder.this.resolveTypeByQName(typeQName);
							}

							// is element affected ?
							XmlSchemaElement ref = null;
							QName refQName = step instanceof IElementRefAffectation ? ((IElementRefAffectation) step).getElementRefAffectation() : null;
							if (refQName != null && refQName.getLocalPart().length() > 0) {
								//ref = refQName.getNamespaceURI().length() == 0 ? schema.getElementByName(refQName.getLocalPart()) : collection.getElementByQName(refQName);
								ref = XmlSchemaBuilder.this.resolveElementByQName(refQName);
								
								typeQName = new QName(schema.getTargetNamespace(),refQName.getLocalPart()+"Type");
								
								if (ref == null && refQName.getNamespaceURI().equals(schema.getTargetNamespace())) {
									ref = XmlSchemaUtils.makeDynamic(step, new XmlSchemaElement());
									ref.setQName(refQName);
									ref.setName(refQName.getLocalPart());
									ref.setSchemaTypeName(baseQName);
									XmlSchemaUtils.add(schema, ref);
								}
								else if (ref != null) {
									ref.setSchemaTypeName(baseQName);
									//type = typeQName.getNamespaceURI().length() == 0 ? schema.getTypeByName(typeQName.getLocalPart()) : collection.getTypeByQName(typeQName);
									type = XmlSchemaBuilder.this.resolveTypeByQName(typeQName);
								}
							}
							
							if (type == null || !SchemaMeta.isReadOnly(type)) {

								// prepare to receive children
								if (step instanceof ISchemaParticleGenerator) {
									myParticleChildren = particleChildren = new LinkedList<XmlSchemaParticle>();
									if (fullSchema || ((ISchemaParticleGenerator) step).isGenerateElement()) {
										myAttributeChildren = attributeChildren = new LinkedList<XmlSchemaAttribute>();
									}
								}

								// deep walk
								super.walk(step);

								// generate itself and add to the caller list
								if (step instanceof ISchemaAttributeGenerator) {
									// Attribute case
									XmlSchemaAttribute attribute = ((ISchemaAttributeGenerator) step).getXmlSchemaObject(collection, schema);
									SchemaMeta.setXmlSchemaObject(schema, step, attribute);
									parentAttributeChildren.add(attribute);

								} else if (step instanceof ISchemaParticleGenerator) {
									
									if (step instanceof RequestableStep) {
										RequestableStep requestableStep = (RequestableStep)step;
										String targetProjectName = requestableStep.getProjectName();
										Project targetProject = requestableStep.getSequence().getLoadedProject(targetProjectName);
										if (targetProject == null) {
											Engine.logEngine.warn("(XmlSchemaBuilder) Not complete schema because: Missing required or not loaded project \"" + targetProjectName + "\"");
										} else if (step instanceof SequenceStep) {
											// SequenceStep case : walk target sequence first
											try {
												Sequence targetSequence = ((SequenceStep)step).getTargetSequence();
												targetProjectName = targetSequence.getProject().getName();
												String targetSequenceName = targetSequence.getName();
												String stepSequenceName = step.getSequence().getName();

												if (projectName.equals(targetProjectName)) {
													boolean isAfter = targetSequenceName.compareToIgnoreCase(stepSequenceName) > 0;
													if (isAfter) {
														walk(targetSequence);
													}
												}
											} catch (EngineException e) {
												if (!e.getMessage().startsWith("There is no ")) {
													throw e;
												} else {
													Engine.logEngine.warn("(XmlSchemaBuilder) Not complete schema because: " + e.getMessage());
												}
											}
										}
									}
									
									
																			
									// Particle case
									XmlSchemaParticle particle = ((ISchemaParticleGenerator) step).getXmlSchemaObject(collection, schema);
									
									SchemaMeta.setXmlSchemaObject(schema, step, particle);
									parentParticleChildren.add(particle);
									
									// retrieve the xsd:element to add children
									XmlSchemaElement element = SchemaMeta.getContainerXmlSchemaElement(ref == null ? particle : ref);

									// retrieve the group to add children if any
									XmlSchemaGroupBase group = SchemaMeta.getContainerXmlSchemaGroupBase(element != null ? element : particle);

									// new complexType to enhance the element
									XmlSchemaComplexType cType = element != null ? (XmlSchemaComplexType) element.getSchemaType() : null;

									// do something only on case of child
									if (!myParticleChildren.isEmpty() || (myAttributeChildren != null && !myAttributeChildren.isEmpty())) {
										if (cType == null) {
											cType = XmlSchemaUtils.makeDynamic(step, new XmlSchemaComplexType(schema));
										}

										// prepare element children in the group
										if (!myParticleChildren.isEmpty()) {									
											if (group == null) {
												group = XmlSchemaUtils.makeDynamic(step, new XmlSchemaSequence());
											}

											for (XmlSchemaParticle child : myParticleChildren) {
												group.getItems().add(child);
											}
										}

										if (element != null) {
											XmlSchemaSimpleContentExtension sContentExt = SchemaManager.makeSimpleContentExtension(step, element, cType);
											if (sContentExt != null) {
												// add attributes
												for (XmlSchemaAttribute attribute : myAttributeChildren) {
													sContentExt.getAttributes().add(attribute);
												}
											} else {
												// add attributes
												for (XmlSchemaAttribute attribute : myAttributeChildren) {
													cType.getAttributes().add(attribute);
												}

												// add elements
												if (SchemaMeta.isDynamic(cType) && group != null) {
													cType.setParticle(group);
												}
											}
										}
									}

									if (element != null) {
										// check if the type is named
										if (typeQName != null && typeQName.getLocalPart().length() > 0) {
											if (cType == null) {
												cType = XmlSchemaUtils.makeDynamic(step, new XmlSchemaComplexType(schema));
												SchemaManager.makeSimpleContentExtension(step, element, cType);
											}

											if (type == null) {
												// the type doesn't exist, declare it
												cType.setName(typeQName.getLocalPart());
												schema.addType(cType);
												schema.getItems().add(cType);
											} else {
												// the type already exists, merge it
												XmlSchemaComplexType currentCType = (XmlSchemaComplexType) type;
												SchemaManager.merge(schema, currentCType, cType);
												cType = currentCType;
											}

											// reference the type in the current element
											element.setSchemaTypeName(cType.getQName());
											element.setSchemaType(null);
										} else if (cType != null && SchemaMeta.isDynamic(cType) && element.getSchemaTypeName() == null) {
											// the element contains an anonymous type
											element.setSchemaType(cType);
										}
									}
								} else {
									XmlSchemaObject object;
									XmlSchema xmlSchema = null;
									if (step instanceof XMLCopyStep && !fullSchema) {
										final XmlSchemaBuilder fullBuilder = builderExecutor.getBuilder(projectName, true);
										if (fullBuilder != null) {
											xmlSchema = fullBuilder.getXmlSchema();
											XmlSchemaCollection xmlCollection = SchemaMeta.getCollection(xmlSchema);
											object = step.getXmlSchemaObject(xmlCollection, xmlSchema);
										} else {
											xmlSchema = schema;
											object = step.getXmlSchemaObject(collection, schema);
											SchemaMeta.setXmlSchemaObject(schema, step, object);
										}
									} else {
										xmlSchema = schema;
										object = step.getXmlSchemaObject(collection, schema);
										SchemaMeta.setXmlSchemaObject(schema, step, object);
									}
									
									if (step instanceof XMLCopyStep) {
										if (object instanceof XmlSchemaElement) {
											XmlSchemaElement xmlSchemaElement = (XmlSchemaElement)object;
											QName qname = xmlSchemaElement.getSchemaTypeName();
											if (qname != null) {
												XmlSchemaType xmlSchemaType = xmlSchema.getTypeByName(qname);
												if (xmlSchemaType != null) {
													SchemaMeta.getReferencedDatabaseObjects(xmlSchemaType).add(step);
												}
											}
										}
									}
									
									if (object instanceof XmlSchemaParticle) {
										particleChildren.add((XmlSchemaParticle) object);
									} else if (object instanceof XmlSchemaAttribute) {
										attributeChildren.add((XmlSchemaAttribute) object);
									}
								}
							} else {
								// re-use read only type									
								XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(step, new XmlSchemaElement());
								SchemaMeta.getReferencedDatabaseObjects(type).add(step);
								SchemaMeta.setXmlSchemaObject(schema, step, elt);
								elt.setName(step.getStepNodeName());
								elt.setSchemaTypeName(typeQName);
								particleChildren.add(elt);
							}
						}
						// Other case
						else {
							// doesn't generate schema, just deep walk
							//System.out.println("-> do not generate schema (deep walk)");
							super.walk(step);
						}
						
						// restore lists for siblings
						particleChildren = parentParticleChildren;
						attributeChildren = parentAttributeChildren;
					} else {
						// just deep walk
						super.walk(databaseObject);
					}
				}

				@Override
				protected boolean before(DatabaseObject databaseObject, Class<? extends DatabaseObject> dboClass) {
					// just walk ISchemaGenerator DBO
					return Step.class.isAssignableFrom(dboClass) || 
							Sequence.class.isAssignableFrom(dboClass) ||
							Transaction.class.isAssignableFrom(dboClass) ||
							Connector.class.isAssignableFrom(dboClass);
				}
				
			}.init(project);
			
			
		} catch (Exception e) {
			throw new EngineException("midBuildSchema failed", e);
		}
	}
	
	protected void postBuildSchema() throws EngineException {
		try {
			
			midBuildSchema(1); // second pass
			
			// sort schemas by namespaces ascending order
			List<XmlSchema> schemaList = Arrays.asList(collection.getXmlSchemas());
			Collections.sort(schemaList, new Comparator<XmlSchema>() {
				@Override
				public int compare(XmlSchema o1, XmlSchema o2) {
					return o1.getTargetNamespace().compareTo(o2.getTargetNamespace());
				}
			});
			
			// defined prefixes for this schema
			NamespaceMap nsMap = new NamespaceMap();
			int cpt1 = 0, cpt2 = 0;
			for (final XmlSchema xs : schemaList) {
				String tns = xs.getTargetNamespace();
				String prefix;
				if (Constants.URI_2001_SCHEMA_XSD.equals(tns)) {
					prefix = "xsd";
				} else if (project.getTargetNamespace().equals(tns)) {
					prefix = "n" + cpt1++;
				} else {
					prefix = "p" + cpt2++;
				}
				SchemaMeta.setPrefix(xs, prefix);
				SchemaMeta.setCollection(xs, collection);
				nsMap.add(prefix, tns);
				
				new XmlSchemaWalker.XmlSchemaWalkerWatcher() {

					@Override
					protected boolean on(XmlSchemaObject obj) {
						SchemaMeta.setSchema(obj, xs);							
						return super.on(obj);
					}
					
					@Override
					protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
						// Fixed issue for soap-enc arrayType attribute (rpc mode)
						Attr[] attrs = obj.getUnhandledAttributes();
						List<Attr> list = new ArrayList<Attr>();
						if (attrs != null) {
							for (Attr attribute : attrs) {
								if (attribute.getNodeName().startsWith("xmlns") && attribute.getNodeValue().equals(""))
									;// remove empty namespace
								else
									list.add(attribute);
							}
							obj.setUnhandledAttributes(list.toArray(new Attr[list.size()]));
						}
						super.walkAttribute(xmlSchema, obj);
					}

				}.init(xs);
			}
			collection.setNamespaceContext(nsMap);
			schema.setNamespaceContext(nsMap);
			
		} catch (Exception e) {
			throw new EngineException("postBuildSchema failed", e);
		}
	}

	protected void endBuildSchema() throws EngineException {
		try {
			// for dependencies : resolve import's schema (needed by SchemaViewLabelProvider) 
			for (final XmlSchema xs : collection.getXmlSchemas()) {
				XmlSchemaObjectCollection col = xs.getIncludes();
				for (int i= 0; i < col.getCount(); i++) {
					XmlSchemaObject ob = col.getItem(i);
					if (ob instanceof XmlSchemaImport) {
						XmlSchemaImport xmlSchemaImport = (XmlSchemaImport)ob;
						if (xmlSchemaImport.getSchema() == null) {
							String tns = xmlSchemaImport.getNamespace();
							final XmlSchemaBuilder builder = builderExecutor.getBuilderByTargetNamespace(tns);
							if (builder != null) {
								NamespaceMap nsMap = (NamespaceMap) collection.getNamespaceContext();
								XmlSchemaCollection xmlSchemaCollection = new XmlSchemaCollection();
								xmlSchemaCollection.read(builder.getXmlSchema().getSchemaDocument(), null);
								XmlSchema xmlSchema = xmlSchemaCollection.schemaForNamespace(tns);
								SchemaMeta.setPrefix(xmlSchema, nsMap.getPrefix(tns));
								xmlSchemaCollection.setNamespaceContext(nsMap);
								xmlSchema.setNamespaceContext(nsMap);
								xmlSchemaImport.setSchema(xmlSchema);
							} else {
								XmlSchema xmlSchema = collection.schemaForNamespace(tns);
								xmlSchemaImport.setSchema(xmlSchema);
							}
						}
						
					}
				}
			}
		} catch (Exception e) {
			throw new EngineException("endBuildSchema failed", e);
		}
	}
	
}

