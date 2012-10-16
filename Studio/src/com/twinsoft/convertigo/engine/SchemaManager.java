package com.twinsoft.convertigo.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.ISchemaAttributeGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaImportGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaIncludeGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaParticleGenerator;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;


public class SchemaManager implements AbstractManager {
	
	private class XmlSchemaCacheEntry {
		XmlSchema schema;
		XmlSchema fullSchema;
		long lastChange;
	}
	
	Map<String, XmlSchemaCacheEntry> schemaCache = Collections.synchronizedMap(new HashMap<String, XmlSchemaCacheEntry>());
	
	public void init() throws EngineException {
		schemaCache.clear();
	}

	public void destroy() throws EngineException {
		schemaCache.clear();
	}

	public XmlSchema getSchemaForProject(String projectName) throws Exception {
		return getSchemaForProject(projectName, false);
	}
	
	public XmlSchemaCollection getSchemasForProject(String projectName) throws Exception {
		return SchemaMeta.getCollection(getSchemaForProject(projectName));
	}

	public XmlSchema getSchemaForProject(final String projectName, final boolean fullSchema) throws Exception {
		long timeStart = System.currentTimeMillis();

		// get directly the project reference (read only)
		final Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		
		synchronized (project) {
			long lastChange = project.getLastChange();

			XmlSchemaCacheEntry cacheEntry = schemaCache.get(projectName);

			if (cacheEntry != null && cacheEntry.lastChange == lastChange) {
				if (!fullSchema && cacheEntry.schema != null) {
					return cacheEntry.schema;
				}
				if (fullSchema && cacheEntry.fullSchema != null) {
					return cacheEntry.fullSchema;
				}
			}

			final XmlSchemaCollection collection = new XmlSchemaCollection();

			// empty schema for the current project
			final XmlSchema schema = XmlSchemaUtils.makeDynamic(project, new XmlSchema(project.getTargetNamespace(), collection));

			try {
				schema.setElementFormDefault(new XmlSchemaForm(project.getSchemaElementForm()));
				schema.setAttributeFormDefault(new XmlSchemaForm(project.getSchemaElementForm()));

				new WalkHelper() {
					List<XmlSchemaParticle> particleChildren;
					List<XmlSchemaAttribute> attributeChildren;

					public void init(Project project) throws Exception {
						List<XmlSchemaParticle> myParticleChildren = particleChildren = new LinkedList<XmlSchemaParticle>();
						List<XmlSchemaAttribute> myAttributeChildren = attributeChildren = new LinkedList<XmlSchemaAttribute>();

						super.init(project);

						for (XmlSchemaParticle particle : myParticleChildren) {
							schema.getItems().add(particle);
						}

						for (XmlSchemaAttribute attribute : myAttributeChildren) {
							schema.getItems().add(attribute);
						}
					}

					@Override
					protected void walk(DatabaseObject databaseObject) throws Exception {
						if ((databaseObject instanceof Step && !((Step) databaseObject).isEnable())) {
							return;
						}

						List<XmlSchemaParticle> parentParticleChildren = particleChildren;
						List<XmlSchemaAttribute> parentAttributeChildren = attributeChildren;

						if (databaseObject instanceof ISchemaGenerator && ((databaseObject instanceof Step && fullSchema && ((Step) databaseObject).isXml()) || ((ISchemaGenerator) databaseObject).isGenerateSchema())) {
							List<XmlSchemaParticle> myParticleChildren = null;
							List<XmlSchemaAttribute> myAttributeChildren = null;

							// prepare to receive children
							if (databaseObject instanceof ISchemaParticleGenerator) {
								myParticleChildren = particleChildren = new LinkedList<XmlSchemaParticle>();
								if (fullSchema || ((ISchemaParticleGenerator) databaseObject).isGenerateElement()) {
									myAttributeChildren = attributeChildren = new LinkedList<XmlSchemaAttribute>();
								}
							}

							// deep walk
							super.walk(databaseObject);

							// generate itself and add to the caller list
							if (databaseObject instanceof ISchemaImportGenerator) {
								// Import case
								XmlSchemaImport schemaImport = ((ISchemaImportGenerator) databaseObject).getXmlSchemaObject(collection, schema);
								SchemaMeta.setXmlSchemaObject(schema, databaseObject, schemaImport);
								schema.getItems().add(schemaImport);

							} else if (databaseObject instanceof ISchemaIncludeGenerator) {
								// Include case
								XmlSchemaInclude include = ((ISchemaIncludeGenerator)databaseObject).getXmlSchemaObject(collection, schema);
								SchemaMeta.setXmlSchemaObject(schema, databaseObject, include);

								XmlSchema xmlSchema = include.getSchema();
								if (xmlSchema != null) {
									XmlSchemaObjectCollection c = xmlSchema.getItems();
									Iterator<XmlSchemaObject> it = GenericUtils.cast(c.getIterator());
									while (it.hasNext()) {
										XmlSchemaObject xmlSchemaObject  = it.next();
										SchemaMeta.getReferencedDatabaseObjects(xmlSchemaObject).add(databaseObject);

										if (xmlSchemaObject instanceof XmlSchemaImport) {
											// ignore (already handle by reference)
										}
										else if (xmlSchemaObject instanceof XmlSchemaInclude) {
											// ignore (already handle by reference)
										}
										else if (xmlSchemaObject instanceof XmlSchemaElement) {
											XmlSchemaElement element = (XmlSchemaElement) xmlSchemaObject;
											if (collection.getElementByQName(element.getQName()) == null) {
												schema.getItems().add(xmlSchemaObject);
											}
										}
										else if (xmlSchemaObject instanceof XmlSchemaType) {
											XmlSchemaType type = (XmlSchemaType) xmlSchemaObject;
											if (collection.getTypeByQName(type.getQName()) == null) {
												schema.addType(type);
												schema.getItems().add(type);
											}
										}
										else {
											schema.getItems().add(xmlSchemaObject);
										}
									}
								}							
							} else if (databaseObject instanceof ISchemaAttributeGenerator) {
								// Attribute case
								XmlSchemaAttribute attribute = ((ISchemaAttributeGenerator) databaseObject).getXmlSchemaObject(collection, schema);
								SchemaMeta.setXmlSchemaObject(schema, databaseObject, attribute);
								parentAttributeChildren.add(attribute);

							} else if (databaseObject instanceof ISchemaParticleGenerator) {
								// Particle case
								XmlSchemaParticle particle = ((ISchemaParticleGenerator) databaseObject).getXmlSchemaObject(collection, schema);
								SchemaMeta.setXmlSchemaObject(schema, databaseObject, particle);
								parentParticleChildren.add(particle);

								// retrieve the xsd:element to add children
								XmlSchemaElement element = SchemaMeta.getContainerXmlSchemaElement(particle);

								// retrieve the group to add children if any
								XmlSchemaGroupBase group = SchemaMeta.getContainerXmlSchemaGroupBase(element != null ? element : particle);

								// new complexType to enhance the element
								XmlSchemaComplexType cType = element != null ? (XmlSchemaComplexType) element.getSchemaType() : null;
								boolean newComplexType = false;							

								// do something only on case of child
								if (!myParticleChildren.isEmpty() || (myAttributeChildren != null && !myAttributeChildren.isEmpty())) {
									if (cType == null) {
										cType = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaComplexType(schema));
										newComplexType = true;
									}

									// prepare element children in the group
									if (!myParticleChildren.isEmpty()) {									
										if (group == null) {
											group = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaSequence());
										}

										for (XmlSchemaParticle child : myParticleChildren) {
//											if (child instanceof XmlSchemaAll) {
//												XmlSchemaGroup subGroup = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaGroup());
//												//											subGroup.setParticle((XmlSchemaAll) child);
//												group.getItems().add(XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaSequence()));
//											} else {
												group.getItems().add(child);
//											}
										}
									}

									if (element != null) {
										// check for existing type
										QName typeName = element.getSchemaTypeName();
										if (typeName != null) {
											// the type must be customized, create an extension
											element.setSchemaTypeName(null);

											XmlSchemaSimpleContent sContent = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaSimpleContent());
											cType.setContentModel(sContent);

											XmlSchemaSimpleContentExtension sContentExt = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaSimpleContentExtension());
											sContent.setContent(sContentExt);

											sContentExt.setBaseTypeName(typeName);

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
											if (newComplexType && group != null) {
												cType.setParticle(group);
											}
										}
									}
								}

								if (element != null) {
									// check if the type is named
									XmlSchemaType type = null;
									QName qName = databaseObject instanceof IComplexTypeAffectation ? ((IComplexTypeAffectation) databaseObject).getComplexTypeAffectation() : null;
									if (qName != null && qName.getLocalPart().length() > 0) {
										if (cType == null) {
											cType = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaComplexType(schema));
											newComplexType = true;
										}

										type = qName.getNamespaceURI().length() == 0 ? schema.getTypeByName(qName.getLocalPart()) : collection.getTypeByQName(qName);

										if (type == null) {
											// the type doesn't exist, declare it
											cType.setName(qName.getLocalPart());
											schema.addType(cType);
											schema.getItems().add(cType);
										} else {
											// the type already exists, merge it
											XmlSchemaComplexType currentCType = (XmlSchemaComplexType) type;
											merge(currentCType, cType);
											cType = currentCType;
										}

										// reference the type in the current element
										element.setSchemaTypeName(cType.getQName());
										element.setSchemaType(null);
									} else if (newComplexType && element.getSchemaTypeName() == null && cType != null) {
										// the element contains an anonymous type
										element.setSchemaType(cType);
									}
								}

							} else {
								XmlSchemaObject object;
								if (databaseObject instanceof XMLCopyStep && !fullSchema) {
									XmlSchemaCollection collection;
									XmlSchema schema = SchemaManager.this.getSchemaForProject(projectName, true);
									collection = SchemaMeta.getCollection(schema);
									object = ((ISchemaGenerator) databaseObject).getXmlSchemaObject(collection, schema);
								} else {
									object = ((ISchemaGenerator) databaseObject).getXmlSchemaObject(collection, schema);
								}
								SchemaMeta.setXmlSchemaObject(schema, databaseObject, object);
								if (object instanceof XmlSchemaParticle) {
									particleChildren.add((XmlSchemaParticle) object);
								} else if (object instanceof XmlSchemaAttribute) {
									attributeChildren.add((XmlSchemaAttribute) object);
								}
							}
						} else if (databaseObject instanceof Project) {
							// override Project walking order
							Project project = (Project) databaseObject;

							if (before(databaseObject, Reference.class)) {
								for (Reference reference : project.getReferenceList()) {
									walk(reference);
								}
							}

							if (before(databaseObject, Connector.class)) {
								for (Connector connector : project.getConnectorsList()) {
									walk(connector);
								}
							}

							if (before(databaseObject, Sequence.class)) {
								for (Sequence sequence : project.getSequencesList()) {
									walk(sequence);
								}
							}
						} else {
							// doesn't generate schema, just deep walk
							super.walk(databaseObject);
						}

						particleChildren = parentParticleChildren;
						attributeChildren = parentAttributeChildren;
					}

					@Override
					protected boolean before(DatabaseObject databaseObject, Class<? extends DatabaseObject> dboClass) {
						// just walk ISchemaGenerator DBO or Connector
						return ISchemaGenerator.class.isAssignableFrom(dboClass) || Connector.class.isAssignableFrom(dboClass);
					}
				}.init(project);

				// defined prefixes for this schema
				NamespaceMap nsMap = new NamespaceMap();
				int cpt = 0;
				for (final XmlSchema xs : collection.getXmlSchemas()) {
					String tns = xs.getTargetNamespace();
					String prefix;
					if (Constants.URI_2001_SCHEMA_XSD.equals(tns)) {
						prefix = "xsd";
					} else if (project.getTargetNamespace().equals(tns)) {
						prefix = "";//project.getName() + "_ns";
					} else {
						prefix = "p" + cpt++;
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

					}.init(xs);
				}
				schema.setNamespaceContext(nsMap);
				collection.setNamespaceContext(nsMap);

				long timeStop = System.currentTimeMillis();

				// pretty print
//				Transformer transformer = TransformerFactory.newInstance().newTransformer();
//				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//				transformer.transform(new DOMSource(schema.getSchemaDocument()), new StreamResult(System.out));
				System.out.println("Schema for " + projectName + " | Times >> total : " + (timeStop - timeStart) + " ms");
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
			
			cacheEntry = schemaCache.get(projectName);
			if (cacheEntry == null) {
				cacheEntry = new XmlSchemaCacheEntry();
				schemaCache.put(projectName, cacheEntry);
			}
			
			cacheEntry.lastChange = lastChange;
			if (fullSchema) {
				cacheEntry.fullSchema = schema;
			} else {
				cacheEntry.schema = schema;
			}

			return schema;
		}
	}
	
	private static void merge(XmlSchemaComplexType first, XmlSchemaComplexType second) {
		// check if the type is dynamic and can be merged
		if (SchemaMeta.isDynamic(first)) {
			
			// merge attributes
			mergeAttributes(first.getAttributes(), second.getAttributes());
			
			// merge sequence of xsd:element if any
			XmlSchemaSequence firstSequence = (XmlSchemaSequence) first.getParticle();
			XmlSchemaSequence secondSequence = (XmlSchemaSequence) second.getParticle();
			
			if (firstSequence != null || secondSequence != null) {
				// create missing sequence
				if (firstSequence == null) {
					firstSequence = XmlSchemaUtils.makeDynamic(SchemaMeta.getReferencedDatabaseObjects(first), new XmlSchemaSequence());
					first.setParticle(firstSequence);
				} else if (secondSequence == null) {
					secondSequence = XmlSchemaUtils.makeDynamic(SchemaMeta.getReferencedDatabaseObjects(second), new XmlSchemaSequence());
				}
				
				// merge sequence
				mergeParticules(firstSequence, secondSequence);
			} else {
				// suppose the type contains an extension
				XmlSchemaSimpleContent firstContent = (XmlSchemaSimpleContent) first.getContentModel();
				XmlSchemaSimpleContent secondContent = (XmlSchemaSimpleContent) second.getContentModel();
				
				if (firstContent != null && secondContent != null) {
					SchemaMeta.getReferencedDatabaseObjects(firstContent).addAll(SchemaMeta.getReferencedDatabaseObjects(secondContent));
					
					XmlSchemaSimpleContentExtension firstContentExtension = (XmlSchemaSimpleContentExtension) firstContent.getContent();
					XmlSchemaSimpleContentExtension secondContentExtension = (XmlSchemaSimpleContentExtension) secondContent.getContent();
		
					mergeAttributes(firstContentExtension.getAttributes(), secondContentExtension.getAttributes());
					
					SchemaMeta.getReferencedDatabaseObjects(firstContentExtension).addAll(SchemaMeta.getReferencedDatabaseObjects(secondContentExtension));
				}
			}
		}
		SchemaMeta.getReferencedDatabaseObjects(first).addAll(SchemaMeta.getReferencedDatabaseObjects(second));
	}
	
	private static void mergeAttributes(XmlSchemaObjectCollection first, XmlSchemaObjectCollection second) {
		// merge only if there attributes
		if (first.getCount() != 0 && second.getCount() != 0) {
			// copy and sort attributes
			Iterator<XmlSchemaAttribute> iFirst = XmlSchemaUtils.attributesToSortedSet(first).iterator();
			Iterator<XmlSchemaAttribute> iSecond = XmlSchemaUtils.attributesToSortedSet(second).iterator();
			XmlSchemaAttribute aFirst = GenericUtils.nextOrNull(iFirst);
			XmlSchemaAttribute aSecond = GenericUtils.nextOrNull(iSecond);
			
			// prepare to receive ordered attributes
			XmlSchemaUtils.clear(first);
	
			while (aFirst != null && aSecond != null) {
				// compare attributes name
				int compare = XmlSchemaUtils.attributeNameComparator.compare(aFirst, aSecond);
				if (compare == 0) {
					// same name, make it optional if one of them is optional
					if (!aFirst.getUse().equals(aSecond.getUse())) {
						aFirst.setUse(XmlSchemaUtils.attributeUseOptional);
					}
					first.add(aFirst);
					SchemaMeta.getReferencedDatabaseObjects(aFirst).addAll(SchemaMeta.getReferencedDatabaseObjects(aSecond));
					aFirst = GenericUtils.nextOrNull(iFirst);
					aSecond = GenericUtils.nextOrNull(iSecond);
				} else if (compare < 0) {
					// not same name, add the first attribute as optional
					aFirst.setUse(XmlSchemaUtils.attributeUseOptional);
					first.add(aFirst);
					aFirst = GenericUtils.nextOrNull(iFirst);
				} else {
					// not same name, add the second attribute as optional
					aSecond.setUse(XmlSchemaUtils.attributeUseOptional);
					first.add(aSecond);
					aSecond = GenericUtils.nextOrNull(iSecond);
				}
			}
			
			// copy tailing attributes as optional 
			if (aFirst != null || (aFirst = aSecond) != null) {
				aFirst.setUse(XmlSchemaUtils.attributeUseOptional);
				first.add(aFirst);			
			}
			
			while (iFirst.hasNext() || (iFirst = iSecond).hasNext()) {
				aFirst = iFirst.next();
				aFirst.setUse(XmlSchemaUtils.attributeUseOptional);
				first.add(aFirst);
			}
		}
	}
	
	private static void mergeParticules(XmlSchemaGroupBase first, XmlSchemaGroupBase second) {
		// wrap element collection in a standard java List interface
		List<XmlSchemaParticle> lFirst = new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaParticle>(first.getItems());
		List<XmlSchemaParticle> result = new ArrayList<XmlSchemaParticle>(first.getItems().getCount() + second.getItems().getCount());
		List<Boolean> minor = new ArrayList<Boolean>(first.getItems().getCount() + second.getItems().getCount());
		
		GenericUtils.merge(lFirst, new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaParticle>(second.getItems()), result, minor, new Comparator<XmlSchemaParticle>() {
			public int compare(XmlSchemaParticle first, XmlSchemaParticle second) {
				if (first instanceof XmlSchemaGroupBase && first.getClass().equals(second.getClass())) {
					mergeParticules((XmlSchemaGroupBase) first, (XmlSchemaGroupBase) second);
					return 0;
				} else if (first instanceof XmlSchemaElement && second instanceof XmlSchemaElement) {
					XmlSchemaElement eFirst = (XmlSchemaElement) first;
					XmlSchemaElement eSecond = (XmlSchemaElement) second;
					int comp = eFirst.getName().compareTo(eSecond.getName());
					if (comp == 0) {
						// merge element
						XmlSchemaComplexType tFirst = (XmlSchemaComplexType) eFirst.getSchemaType();
						XmlSchemaComplexType tSecond = (XmlSchemaComplexType) eSecond.getSchemaType();
						if (tSecond != null) {
							if (tFirst != null) {
								merge(tFirst, tSecond);
							} else {
								eFirst.setSchemaTypeName(null);
								eFirst.setSchemaType(tSecond);
							}
						}
						SchemaMeta.getReferencedDatabaseObjects(eFirst).addAll(SchemaMeta.getReferencedDatabaseObjects(eSecond));
					}
					return comp;
				}
				return -1;
			}
		});
		
		// set the merged content in the first collection
		lFirst.clear();
		
		for (int i = 0; i < result.size(); i++) {
			XmlSchemaParticle element = result.get(i);
			if (minor.get(i)) {
				element.setMinOccurs(0);
			}
			lFirst.add(element);
		}
		
		SchemaMeta.getReferencedDatabaseObjects(first).addAll(SchemaMeta.getReferencedDatabaseObjects(second));
	}
	
	public void clearCache(String projectName) {
		schemaCache.remove(projectName);
	}
	
//	public void validateResponse(String projectName, String requestableName, Document document) {
//		try {
//			XmlSchema schema = getSchemaForProject(projectName);
//		
//			try {
//				Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
//				Element elt = doc.createElement(requestableName + "Response");
//				doc.appendChild(elt);
//				elt = doc.createElement("response");
//	
//				NodeList children = document.getDocumentElement().getChildNodes();
//				for (int i = 0; i < children.getLength(); i++) {
//					elt.appendChild(doc.importNode(children.item(i), true));
//				}
//				
//				if (elt.getChildNodes().getLength() > 0) {
//					doc.getDocumentElement().appendChild(elt);
//				}
//				
//				XMLUtils.setNamespace(doc, schema.getTargetNamespace());
//				
//				XmlSchemaUtils.validate(schema, doc);
//				
//				document.appendChild(document.createComment("Response valid"));
//			} catch (SAXException e) {
//				System.err.println(e.getMessage());
//				document.appendChild(document.createComment("Response not valid : " + e.getMessage()));
//			}
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	public void validateResponse(String projectName, String requestableName, Document document) throws SAXException {
		try {
			XmlSchema schema = getSchemaForProject(projectName);
		
			
				Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				Element elt = doc.createElement(requestableName + "Response");
				doc.appendChild(elt);
				elt = doc.createElement("response");
	
				NodeList children = document.getDocumentElement().getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					elt.appendChild(doc.importNode(children.item(i), true));
				}
				
				String sequence = document.getDocumentElement().getAttribute("sequence");
				if ((sequence != null && sequence.length() > 0) || elt.getChildNodes().getLength() > 0) {
					doc.getDocumentElement().appendChild(elt);
				}

				XMLUtils.setNamespace(doc, schema.getTargetNamespace());

				XmlSchemaUtils.validate(schema, doc);
		} catch (SAXException e) {
			throw e;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
