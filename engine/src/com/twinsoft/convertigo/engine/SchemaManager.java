/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;


public class SchemaManager implements AbstractManager {
	
	public enum Option {
		fullSchema,
		noCache;
		
		boolean is(Option[] options) {
			for (Option option: options) {
				if (option == this) {
					return true;
				}
			}
			return false;
		}
		
		boolean not(Option[] options) {
			return !is(options);
		}
	}
	
	private class XmlResponseWalker extends XmlSchemaWalker {
		private List<XmlSchemaObject> elist = null;
		private List<XmlSchemaObject> alist = null;
		private Map<QName, XmlSchemaObject> mso = new HashMap<QName, XmlSchemaObject>();
		private Map<QName, XmlSchemaObject> mto = new HashMap<QName, XmlSchemaObject>();
		Map<XmlSchemaObject, List<List<XmlSchemaObject>>> map = new LinkedHashMap<XmlSchemaObject, List<List<XmlSchemaObject>>>();
		private boolean debug = false;
		
		private XmlResponseWalker(boolean deep, boolean deepExternal, boolean debug) {
			super(deep, deepExternal);
			this.debug = debug;
		}
		
		void init(XmlSchema xmlSchema, QName qname, Element element) {
			XmlSchemaElement rxe = SchemaMeta.getCollection(xmlSchema).getElementByQName(qname);
			if (rxe != null) {
				List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
				list.add(new ArrayList<XmlSchemaObject>());
				list.add(new ArrayList<XmlSchemaObject>());
				map.put(rxe, list);
				mso.put(qname, rxe);
				elist = list.get(0);
				alist = list.get(1);
				
				// walk schemas
				walkElement(xmlSchema, rxe);
				
				if (debug) {
					for (XmlSchemaObject xso: map.keySet()) {
						System.out.println(((XmlSchemaElement)xso).getName());
						System.out.print("\t[");
						for (XmlSchemaObject child: map.get(xso).get(1)) {
							System.out.print(((XmlSchemaAttribute)child).getName()+",");
						}
						System.out.println("]");
						for (XmlSchemaObject child: map.get(xso).get(0)) {
							System.out.println("\t"+((XmlSchemaElement)child).getName());
						}
					}
				}
				
				// make compliant: add c8o_xxx attributes if needed
				makeCompliant(rxe, element);
				
				if (debug) {
					System.out.println("Before clean");
					System.out.println(XMLUtils.prettyPrintElement(element));
				}
				
				// removes unwanted prefixes or attributes (xsi, xmlns, ...)
				clean(element);
				
				if (debug) {
					System.out.println("After clean");
					System.out.println(XMLUtils.prettyPrintElement(element));
				}
				
				if (elist != null) {
					elist.clear();
					elist = null;
				}
				if (alist != null) {
					alist.clear();
					alist = null;
				}
				if (mso != null) {
					mso.clear();
					mso = null;
				}
				if (mto != null) {
					mto.clear();
					mto = null;
				}
				if (map != null) {
					map.clear();
					map = null;
				}
			}
		}
		
		private void renameNode(Node node) {
			Document doc = node.getOwnerDocument();
			String tns = node.getPrefix();
			if ((tns != null) && !tns.equals("")) {
				node = doc.renameNode(node, "", node.getLocalName());
			}
		}
		
		private void clean(Node node) {
			if (node.getNodeType() == Node.DOCUMENT_NODE) {
				clean(((Document)node).getDocumentElement());
			} else if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;

				renameNode(element);
				
				if (element.hasAttributes()) {
					List<Node> list = new ArrayList<Node>();
					NamedNodeMap attributes = element.getAttributes();
					for (int i=0; i <attributes.getLength(); i++) {
						Node attr = attributes.item(i);
						String prefix = attr.getPrefix();
						if (prefix != null && (
								prefix.toLowerCase().equals("xsi") || 
								prefix.toLowerCase().equals("xmlns") ||
								prefix.toLowerCase().startsWith("soap-")
						)) {
							list.add(attr);
						} else {
							renameNode(attr);
						}
					}
					for (Node attr: list) {
						element.removeAttributeNode((Attr) attr);
					}
				}
				
				if (element.hasChildNodes()) {
					NodeList children = element.getChildNodes();
					for (int i= 0; i < children.getLength(); i++) {
						clean(children.item(i));
					}
				}
			}
		}
		
		public boolean makeCompliant(XmlSchemaObject xso, Node node) {
			return false;
		}
		
		
		@Override
		public void walkByTypeName(XmlSchema xmlSchema, QName qname) {
			XmlSchemaType obj = SchemaMeta.getCollection(xmlSchema).getTypeByQName(qname);
			if (obj != null) {
				if (!mso.containsKey(qname)) {
					mso.put(qname, obj);
					super.walkByTypeName(xmlSchema, qname);
				}
				else {
					if (mto.containsKey(qname)) {
						//System.out.println("\nWalk type "+ qname);
						elist.addAll(map.get(mto.get(qname)).get(0));
					}
				}
			}
		}

		@Override
		public void walkByElementRef(XmlSchema xmlSchema, QName qname) {
			XmlSchemaElement obj = SchemaMeta.getCollection(xmlSchema).getElementByQName(qname);
			if (obj != null) {
				if (!mso.containsKey(qname)) {
					mso.put(qname, obj);
					super.walkByElementRef(xmlSchema, qname);
				}
				else {
					if (mto.containsKey(qname)) {
						//System.out.println("\nWalk elem ref "+ qname);
						elist.add(mto.get(qname));
					}
				}
			}
		}

		@Override
		public void walkByAttributeGroupRef(XmlSchema xmlSchema, QName qname) {
			XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
			XmlSchemaAttributeGroup obj = (XmlSchemaAttributeGroup) schema.getAttributeGroups().getItem(qname);
			if (obj != null) {
				if (!mso.containsKey(qname)) {
					mso.put(qname, obj);
					super.walkByAttributeGroupRef(xmlSchema, qname);
				}
			}
		}

		@Override
		public void walkByAttributeRef(XmlSchema xmlSchema, QName qname) {
			XmlSchemaAttribute obj = SchemaMeta.getCollection(xmlSchema).getAttributeByQName(qname);
			if (obj != null) {
				if (!mso.containsKey(qname)) {
					mso.put(qname, obj);
					super.walkByAttributeRef(xmlSchema, qname);
				}
				else {
					if (mto.containsKey(qname)) {
						//System.out.println("\nWalk attr ref "+ qname);
						alist.add(mto.get(qname));
					}
				}
			}
		}

		@Override
		public void walkByGroupRef(XmlSchema xmlSchema, QName qname) {
			XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
			XmlSchemaGroup obj = (XmlSchemaGroup) schema.getGroups().getItem(qname);
			if (obj != null) {
				if (!mso.containsKey(qname)) {
					mso.put(qname, obj);
					super.walkByGroupRef(xmlSchema, qname);
				}
			}
		}
		
		@Override
		protected void walkComplexContentExtension(XmlSchema xmlSchema, XmlSchemaComplexContentExtension obj) {
			List<XmlSchemaObject> el = elist;
			List<XmlSchemaObject> al = alist;
			
			QName baseTypeName = obj.getBaseTypeName();
			if (!mso.containsKey(baseTypeName)) {
				String nsURI = xmlSchema.getTargetNamespace();
				String prefix = xmlSchema.getNamespaceContext().getPrefix(nsURI);
				String localName = baseTypeName.getLocalPart() + "Extension";
				XmlSchemaElement xmlSchemaElement = new XmlSchemaElement();
				xmlSchemaElement.setName(localName);
				xmlSchemaElement.setQName(new QName(nsURI, localName, prefix));
				xmlSchemaElement.setSchemaTypeName(baseTypeName);
				XmlSchemaUtils.add(xmlSchema, xmlSchemaElement);
				SchemaMeta.setSchema(xmlSchemaElement, xmlSchema);
				
				List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
				list.add(new ArrayList<XmlSchemaObject>());
				list.add(new ArrayList<XmlSchemaObject>());
				
				elist = list.get(0);
				alist = list.get(1);
				
				walkElement(xmlSchema, xmlSchemaElement);
				
				elist = el;
				alist = al;
			}
			
			super.walkComplexContentExtension(xmlSchema, obj);
		}

		@Override
		protected void walkChoice(XmlSchema xmlSchema, XmlSchemaChoice obj) {
			List<XmlSchemaObject> el = elist;
			List<XmlSchemaObject> al = alist;
			
			List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
			list.add(new ArrayList<XmlSchemaObject>());
			list.add(new ArrayList<XmlSchemaObject>());
			
			elist = list.get(0);
			alist = list.get(1);
			super.walkChoice(xmlSchema, obj);
			if (obj.getMaxOccurs() > 1) {
				if (elist.size() > 0) {
					XmlSchemaElement startC8oFakeOccurs = new XmlSchemaElement();
					startC8oFakeOccurs.setName("StartC8oFakeOccurs");
					elist.add(0, startC8oFakeOccurs);
					XmlSchemaElement endC8oFakeOccurs = new XmlSchemaElement();
					endC8oFakeOccurs.setName("EndC8oFakeOccurs");
					elist.add(endC8oFakeOccurs);
				}
			}
			el.addAll(elist);
			al.addAll(alist);
			
			elist = el;
			alist = al;			
		}

		@Override
		protected void walkSequence(XmlSchema xmlSchema, XmlSchemaSequence obj) {
			List<XmlSchemaObject> el = elist;
			List<XmlSchemaObject> al = alist;
			
			List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
			list.add(new ArrayList<XmlSchemaObject>());
			list.add(new ArrayList<XmlSchemaObject>());
			
			elist = list.get(0);
			alist = list.get(1);
			super.walkSequence(xmlSchema, obj);
			if (obj.getMaxOccurs() > 1) {
				if (elist.size() > 0) {
					XmlSchemaElement startC8oFakeOccurs = new XmlSchemaElement();
					startC8oFakeOccurs.setName("StartC8oFakeOccurs");
					elist.add(0, startC8oFakeOccurs);
					XmlSchemaElement endC8oFakeOccurs = new XmlSchemaElement();
					endC8oFakeOccurs.setName("EndC8oFakeOccurs");
					elist.add(endC8oFakeOccurs);
				}
			}
			el.addAll(elist);
			al.addAll(alist);
			
			elist = el;
			alist = al;			
		}
		
		@Override
		protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj) {
			List<XmlSchemaObject> el = elist;
			List<XmlSchemaObject> al = alist;
			
			QName qname = obj.getQName();
			QName refName = obj.getRefName();
			QName typeName = obj.getSchemaTypeName();
			if (refName == null) {
				el.add(obj);
				List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
				list.add(new ArrayList<XmlSchemaObject>());
				list.add(new ArrayList<XmlSchemaObject>());
				map.put(obj, list);
				
				//System.out.print("\nwalkElement name="+obj.getName());
				String ns = SchemaMeta.getSchema(obj).getTargetNamespace();
				if (typeName != null) {
					if (!mto.containsKey(typeName)) {
						if (typeName.getNamespaceURI().equals(Constants.URI_2001_SCHEMA_XSD)) {
							typeName = new QName(ns, obj.getName());
						}
						mto.put(typeName, obj);
						//System.out.print("; mto put: typeN="+typeName);
					} else {
						map.put(obj, map.get(mto.get(typeName)));
						
						// type already done
						return;
					}
				}
				else {
					if (qname != null) {
						if (qname.getNamespaceURI().equals("")) {
							qname = new QName(ns, obj.getName());
						}
					} else {
						qname = new QName(ns, obj.getName());
					}
					
					if (!mto.containsKey(qname)) {
						mto.put(qname, obj);
						//System.out.print("; mto put: qname="+qname);
					}
				}
				
				elist = list.get(0);
				alist = list.get(1);
				super.walkElement(xmlSchema, obj);
				elist = el;
				alist = al;
			}
			else {
				el.add(obj);
				List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
				list.add(new ArrayList<XmlSchemaObject>());
				list.add(new ArrayList<XmlSchemaObject>());
				map.put(obj, list);
				
				elist = list.get(0);
				alist = list.get(1);
				super.walkElement(xmlSchema, obj);
				
				if (elist.size() == 1) {
					XmlSchemaElement original = (XmlSchemaElement) elist.remove(0);
					if (original != null && map.containsKey(original)) {
						elist.addAll(map.get(original).get(0));
						alist.addAll(map.get(original).get(1));
					}
				}
				
				elist = el;
				alist = al;
			}
		}

		@Override
		protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
			if (obj.getRefName() == null) {
				alist.add(obj);
			}
			super.walkAttribute(xmlSchema, obj);
			
		}
		
	}
	
	private class XmlSchemaCacheEntry {
		XmlSchema schema;
		XmlSchema fullSchema;
		long lastChange;
		long lastBuildTime;
	}
	
	private Map<String, XmlSchemaCacheEntry> schemaCache = Collections.synchronizedMap(new HashMap<String, XmlSchemaCacheEntry>());
	
	public void init() throws EngineException {
		schemaCache.clear();
	}

	public void destroy() throws EngineException {
		schemaCache.clear();
	}
	
	public XmlSchemaCollection getSchemasForProject(String projectName, Option... options) throws Exception {
		return SchemaMeta.getCollection(getSchemaForProject(projectName, options));
	}

	protected static void getProjectReferences(List<String> refs, String projectName) {
		if (refs == null) return;
		try {
			Project p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
			if (p != null) {
				if (!refs.contains(projectName)) {
					refs.add(projectName);
					for (Reference ref: p.getReferenceList()) {
						if (ref instanceof ProjectSchemaReference) {
							ProjectSchemaReference psr = (ProjectSchemaReference)ref;
							getProjectReferences(refs, psr.getParser().getProjectName());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean needSchemaRebuild(String projectName, long lastBuildTime) {
		if (lastBuildTime > 0) {
			List<String> refs = new ArrayList<String>();
			SchemaManager.getProjectReferences(refs, projectName);
			
			for (String pname: refs) {
				try {
					Project p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(pname);
					if (p != null && p.getLastChange() > lastBuildTime) {
						return true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	public XmlSchema getSchemaForProject(final String projectName, Option... options) throws Exception {
		
		final boolean fullSchema = Option.fullSchema.is(options);
		
		final Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		
		XmlSchemaCacheEntry cacheEntry = getCacheEntry(projectName);
		
		synchronized (cacheEntry) {
			
			boolean forceRebuild = needSchemaRebuild(projectName, cacheEntry.lastBuildTime);
			
			long lastChange = project.getLastChange();
			
			if (Option.noCache.not(options)) {
				if (cacheEntry != null && cacheEntry.lastChange == lastChange && !forceRebuild) {
					if (!fullSchema && cacheEntry.schema != null) {
						//System.out.println("Schema for project \"" + projectName + "\" returned from cache");
						return cacheEntry.schema;
					}
					if (fullSchema && cacheEntry.fullSchema != null) {
						//System.out.println("Full schema for project \"" + projectName + "\" returned from cache");
						return cacheEntry.fullSchema;
					}
				}
			}
			
			try {
				XmlSchema schema = new XmlSchemaBuilderExecutor().buildSchema(projectName, options);
				
				// always cache
				cacheEntry.lastChange = lastChange;
				cacheEntry.lastBuildTime = System.currentTimeMillis();
				if (fullSchema) {
					cacheEntry.fullSchema = schema;
				} else {
					cacheEntry.schema = schema;
				}
				
				return schema;
				
			} catch (Exception e) {
				System.out.println("Unabled to build schema for project \""+ projectName +"\" (see the complete error in logs)");
				Engine.logEngine.error("Unabled to build schema for project \""+ projectName +"\"", e);
				throw e;
			}
		}
	}

	/*public XmlSchema getSchemaForProject(final String projectName, Option... options) throws Exception {
		long timeStart = System.currentTimeMillis();
		
		final boolean fullSchema = Option.fullSchema.is(options);
		
		// get directly the project reference (read only)
		final Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		XmlSchemaCacheEntry cacheEntry = getCacheEntry(projectName);
		
		synchronized (cacheEntry) {
			long lastChange = project.getLastChange();

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
			final XmlSchema schema = XmlSchemaUtils.makeDynamicReadOnly(project, new XmlSchema(project.getTargetNamespace(), collection));
			SchemaMeta.setCollection(schema, collection);
			
			if (Option.noCache.not(options)) {
				cacheEntry.lastChange = lastChange;
				if (fullSchema) {
					cacheEntry.fullSchema = schema;
				} else {
					cacheEntry.schema = schema;
				}
			}
			
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
									XmlSchemaImport schemaImport = ((ISchemaImportGenerator) databaseObject).getXmlSchemaObject(collection, schema);
									if (schemaImport != null) {
										SchemaMeta.setXmlSchemaObject(schema, databaseObject, schemaImport);
										XmlSchemaUtils.add(schema, schemaImport);
									}
								} else if (databaseObject instanceof ISchemaIncludeGenerator) {
									// Include case
									if (databaseObject instanceof Transaction) {
										XmlSchemaInclude schemaInclude = ((ISchemaIncludeGenerator)databaseObject).getXmlSchemaObject(collection, schema);
										//SchemaMeta.setXmlSchemaObject(schema, databaseObject, schemaInclude);
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
						return Transaction.class.isAssignableFrom(dboClass) || 
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
								addXmlSchemaImport(collection, schema, nsURI);
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
							
							if (step.isGenerateSchema() || (fullSchema && step.isXmlOrOutput())) {
								List<XmlSchemaParticle> myParticleChildren = null;
								List<XmlSchemaAttribute> myAttributeChildren = null;
								
								// is base affected ?
								@SuppressWarnings("unused")
								XmlSchemaType base = null;
								QName baseQName = step instanceof ISimpleTypeAffectation ? ((ISimpleTypeAffectation) step).getSimpleTypeAffectation() : null;
								if (baseQName != null && baseQName.getLocalPart().length() > 0) {
									base = baseQName.getNamespaceURI().length() == 0 ? schema.getTypeByName(baseQName.getLocalPart()) : collection.getTypeByQName(baseQName);
								}
								
								// is type affected ?
								XmlSchemaType type = null;
								QName typeQName = step instanceof IComplexTypeAffectation ? ((IComplexTypeAffectation) step).getComplexTypeAffectation() : null;
								if (typeQName != null && typeQName.getLocalPart().length() > 0) {
									type = typeQName.getNamespaceURI().length() == 0 ? schema.getTypeByName(typeQName.getLocalPart()) : collection.getTypeByQName(typeQName);
								}

								// is element affected ?
								XmlSchemaElement ref = null;
								QName refQName = step instanceof IElementRefAffectation ? ((IElementRefAffectation) step).getElementRefAffectation() : null;
								if (refQName != null && refQName.getLocalPart().length() > 0) {
									ref = refQName.getNamespaceURI().length() == 0 ? schema.getElementByName(refQName.getLocalPart()) : collection.getElementByQName(refQName);
									
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
										type = typeQName.getNamespaceURI().length() == 0 ? schema.getTypeByName(typeQName.getLocalPart()) : collection.getTypeByQName(typeQName);
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

										// SequenceStep case : walk target sequence first
										if (step instanceof SequenceStep) {
											try {
												Sequence targetSequence = ((SequenceStep)step).getTargetSequence();
												if (targetSequence != null) {
													String targetProjectName = targetSequence.getProject().getName();
													String targetSequenceName = targetSequence.getName();
													String stepSequenceName = step.getSequence().getName();
													
													if (!projectName.equals(targetProjectName)) {
														SchemaManager.this.getSchemaForProject(targetProjectName);
													}
													else {
														boolean isAfter = targetSequenceName.compareToIgnoreCase(stepSequenceName) > 0;
														if (isAfter) {
															walk(targetSequence);
														}
													}
												}
											}
											catch (Exception e) {
												e.printStackTrace();
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
												XmlSchemaSimpleContentExtension sContentExt = makeSimpleContentExtension(step, element, cType);
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
													makeSimpleContentExtension(step, element, cType);
												}

												if (type == null) {
													// the type doesn't exist, declare it
													cType.setName(typeQName.getLocalPart());
													schema.addType(cType);
													schema.getItems().add(cType);
												} else {
													// the type already exists, merge it
													XmlSchemaComplexType currentCType = (XmlSchemaComplexType) type;
													merge(schema, currentCType, cType);
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
										if (step instanceof XMLCopyStep && !fullSchema) {
											XmlSchema xmlSchema = SchemaManager.this.getSchemaForProject(projectName, Option.fullSchema);
											XmlSchemaCollection xmlCollection = SchemaMeta.getCollection(xmlSchema);
											object = step.getXmlSchemaObject(xmlCollection, xmlSchema);
											SchemaMeta.setXmlSchemaObject(xmlSchema, step, object);
										} else {
											object = step.getXmlSchemaObject(collection, schema);
											SchemaMeta.setXmlSchemaObject(schema, step, object);
										}
										
										if (step instanceof XMLCopyStep) {
											if (object instanceof XmlSchemaElement) {
												XmlSchemaElement xmlSchemaElement = (XmlSchemaElement)object;
												QName qname = xmlSchemaElement.getSchemaTypeName();
												if (qname != null) {
													XmlSchemaType xmlSchemaType = schema.getTypeByName(qname);
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
						prefix = "n" + cpt1++;//"";//project.getName() + "_ns";
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

				long timeStop = System.currentTimeMillis();

				// pretty print
				//				if (fullSchema) {
				//					Transformer transformer = TransformerFactory.newInstance().newTransformer();
				//					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				//					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				//					transformer.transform(new DOMSource(schema.getSchemaDocument()), new StreamResult(System.out));
				//				}
				System.out.println("Schema for project \"" + projectName + "\" | Times >> total : " + (timeStop - timeStart) + " ms");
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}

			return schema;
		}
	}*/
	
	public static void addXmlSchemaImport(XmlSchemaCollection collection, XmlSchema xmlSchema, String ns) {
		try {
			if (ns.equals(Constants.URI_2001_SCHEMA_XSD))
				return;
			
			boolean imported = false;
			Iterator<?> it = xmlSchema.getItems().getIterator();
			while (it.hasNext()) {
				XmlSchemaObject ob = (XmlSchemaObject)it.next();
				if (ob instanceof XmlSchemaImport) {
					XmlSchemaImport xmlSchemaImport = ((XmlSchemaImport)ob);
					String ins = xmlSchemaImport.getNamespace();
					imported = ins.equals(ns);
					if (imported) break;
				}
			}
			if (!imported) {
				//System.out.println("For "+xmlSchema.getTargetNamespace()+", adding import for {"+ns+"}");
				XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
				xmlSchemaImport.setNamespace(ns);
				XmlSchema importedSchema = collection.schemaForNamespace(ns);
				xmlSchemaImport.setSchema(importedSchema);
				xmlSchema.getIncludes().add(xmlSchemaImport);
				xmlSchema.getItems().add(xmlSchemaImport);
			}
		}
		catch (Exception e) {}
	}
	
	static protected XmlSchemaSimpleContentExtension makeSimpleContentExtension(DatabaseObject databaseObject, XmlSchemaElement element, XmlSchemaComplexType cType) {
		QName typeName = element.getSchemaTypeName();
		if (typeName != null) {
			// the type must be customized, create an extension
			element.setSchemaTypeName(null);

			return makeSimpleContentExtension(databaseObject, cType, typeName);
		}
		return null;
	}
	
	static private XmlSchemaSimpleContentExtension makeSimpleContentExtension(DatabaseObject databaseObject, XmlSchemaComplexType cType, QName typeName) {
		if (typeName != null) {
			XmlSchemaSimpleContent sContent = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaSimpleContent());
			cType.setContentModel(sContent);

			XmlSchemaSimpleContentExtension sContentExt = XmlSchemaUtils.makeDynamic(databaseObject, new XmlSchemaSimpleContentExtension());
			sContent.setContent(sContentExt);

			sContentExt.setBaseTypeName(typeName);
			
			return sContentExt;
		}
		return null;
	}

	private static XmlSchemaParticle cosnoamb(XmlSchema xmlSchema, XmlSchemaGroupBase xmlParentSchemaGroupBase, XmlSchemaParticle xmlSchemaParticle) {
		if (xmlSchemaParticle instanceof XmlSchemaElement) {
			return (XmlSchemaParticle)xmlSchemaParticle;
		}
		else if (xmlSchemaParticle instanceof XmlSchemaGroupBase) {
			XmlSchemaGroupBase xmlSchemaGroupBase =  cosnoamb(xmlSchema, xmlParentSchemaGroupBase, (XmlSchemaGroupBase)xmlSchemaParticle);
			return xmlSchemaGroupBase;
		}
		return xmlSchemaParticle;
	}
	
	private static XmlSchemaGroupBase cosnoamb(XmlSchema xmlSchema, XmlSchemaGroupBase xmlParentSchemaGroupBase, XmlSchemaGroupBase xmlSchemaGroupBase) {
		System.out.println(" xmlSchemaGroupBase@"+xmlSchemaGroupBase.hashCode());
		List<XmlSchemaParticle> items = new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaParticle>(xmlSchemaGroupBase.getItems());
		int size = items.size();
		int i = 0, j = 1;
		
		if (size > 1 ) {
			while (i < size) {
				XmlSchemaParticle item1 = cosnoamb(xmlSchema, xmlSchemaGroupBase, items.get(i));
				while (j <= size-1) {
					XmlSchemaParticle item2 = cosnoamb(xmlSchema, xmlSchemaGroupBase, items.get(j));
					if (compare(xmlSchema, xmlSchemaGroupBase, item1, item2) == 0) {
						items = new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaParticle>(xmlSchemaGroupBase.getItems());
						size = items.size();
						item1 = items.get(j-1);
					}
					else {
						j++;
						break;
					}
				}
				i++;
			}
		}
		return xmlSchemaGroupBase;
	}
	
	
	private static int compare(XmlSchema xmlSchema, XmlSchemaGroupBase xmlSchemaGroupBase, XmlSchemaParticle item1, XmlSchemaParticle item2) {
		Comparator<XmlSchemaElement> c = new Comparator<XmlSchemaElement>() {
			public int compare(XmlSchemaElement e1, XmlSchemaElement e2) {
				String name1 = e1.getName().equals("") ? e1.getRefName().getLocalPart():e1.getName();
				String name2 = e2.getName().equals("") ? e2.getRefName().getLocalPart():e2.getName();
				System.out.println("  comparing " +  name1 +" and " + name2 +" ...");
				int result = -1;
				if (name1.equals(name2)) {
					if (e1.getRefName()!=null && e1.getRefName()!=null)
						result = e1.getRefName().equals(e2.getRefName()) ? 0:-1;
					else if (e1.getSchemaTypeName()!=null && e2.getSchemaTypeName()!=null)
						result = e1.getSchemaTypeName().equals(e2.getSchemaTypeName()) ? 0:-1;
					else if (e1.getSchemaType()!=null && e2.getSchemaType()!=null)
						result = e1.getSchemaType().equals(e2.getSchemaType()) ? 0:-1;
					
					if (result == 0) {
						
					}
				}
				return result;
			}
		};
		
		Map<XmlSchemaElement, XmlSchemaGroupBase> map1 = getFirstElement(xmlSchemaGroupBase, item1);
		Map<XmlSchemaElement, XmlSchemaGroupBase> map2 = getFirstElement(xmlSchemaGroupBase, item2);
		
		Entry<XmlSchemaElement, XmlSchemaGroupBase>  o1 = null, o2 = null;
		if (!map1.isEmpty()) o1 = map1.entrySet().iterator().next();
		if (!map2.isEmpty()) o2 = map2.entrySet().iterator().next();
		if (o1 != null && o2 != null) {
			XmlSchemaElement e1 = o1.getKey();
			XmlSchemaGroupBase g1 = o1.getValue();
			XmlSchemaElement e2 = o2.getKey();
			XmlSchemaGroupBase g2 = o2.getValue();
			if (c.compare(e1, e2) == 0) {
				System.out.println("     cosamb between (XmlSchemaElement@" + e1.hashCode()+") and (XmlSchemaElement@" + e2.hashCode()+") !");
				List<Long> dimensions = getMinMaxOccurs(xmlSchemaGroupBase, o1,o2);
				if (!dimensions.isEmpty()) {
					int index = XmlSchemaUtils.indexOf(xmlSchemaGroupBase.getItems(), item1);
					XmlSchemaUtils.remove(g1.getItems(), e1);
					XmlSchemaUtils.remove(g2.getItems(), e2);
					xmlSchemaGroupBase.getItems().setItem(index, e1);
					e1.setMinOccurs(dimensions.get(0));
					e1.setMaxOccurs(dimensions.get(1));
					
					// for source picker
					//DatabaseObject dbo2 = SchemaMeta.getReferencedDatabaseObjects(e2).iterator().next();
					//SchemaMeta.setXmlSchemaObject(xmlSchema,dbo2,e1);
					SchemaMeta.adoptReferences(xmlSchema, e1, e2);
					return 0;
				}
			}
		}
		else {
			if (o2 == null) {
				XmlSchemaUtils.remove(xmlSchemaGroupBase.getItems(), item2);
				return 0;
			}
		}
		return -1;
	}

	private static List<Long> getMinMaxOccurs(XmlSchemaGroupBase xmlSchemaGroupBase, Entry<XmlSchemaElement, XmlSchemaGroupBase> o1, Entry<XmlSchemaElement, XmlSchemaGroupBase> o2) {
		List<Long> dimensions = new ArrayList<Long>();
		XmlSchemaElement e1 = o1.getKey();
		XmlSchemaGroupBase g1 = o1.getValue();
		XmlSchemaElement e2 = o2.getKey();
		XmlSchemaGroupBase g2 = o2.getValue();
		
		long g1Min = g1.getMinOccurs();
		long g1Max = g1.getMaxOccurs();
		long e1Min = e1.getMinOccurs();
		long e1Max = e1.getMaxOccurs();
		
		long g2Min = g2.getMinOccurs();
		long g2Max = g2.getMaxOccurs();
		long e2Min = e2.getMinOccurs();
		long e2Max = e2.getMaxOccurs();
		
		if (g1.hashCode() == g2.hashCode()) {
			dimensions.add(e1Min + e2Min);
			dimensions.add(e1Max + e2Max);
		}
		else {
			if (xmlSchemaGroupBase.hashCode() != g1.hashCode() && xmlSchemaGroupBase.hashCode() != g2.hashCode()) {
				if (g1Min == g2Min) {
					if (g1Max == g2Max) {
						if (e1Min == e2Max && e1Max == e2Max) {
							dimensions.add(g1Min);
							dimensions.add(e1Max + e2Max);
						}
					}
					else {
						//TODO
					}
				}
				else {
					//TODO
				}
			}
			else {
				dimensions.add(e1Min);
				dimensions.add(e1Max);
			}
		}
		return dimensions;
	}

	private static Map<XmlSchemaElement, XmlSchemaGroupBase> getFirstElement(XmlSchemaGroupBase parentSchemaGroupBase, XmlSchemaParticle particle) {
		Map<XmlSchemaElement, XmlSchemaGroupBase> map = new HashMap<XmlSchemaElement, XmlSchemaGroupBase>();
		if (particle instanceof XmlSchemaElement) {
			map.put((XmlSchemaElement)particle, parentSchemaGroupBase);
		}
		else if (particle instanceof XmlSchemaGroupBase) {
			XmlSchemaGroupBase xmlSchemaGroupBase = (XmlSchemaGroupBase)particle;
			List<XmlSchemaParticle> items = new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaParticle>(xmlSchemaGroupBase.getItems());
			if (items.size() > 0) {
				if (items.get(0) instanceof XmlSchemaElement) {
					map.put((XmlSchemaElement)items.get(0), xmlSchemaGroupBase);
				}
				else {
					map = getFirstElement(xmlSchemaGroupBase, items.get(0));
				}
			}
		}
		return map;
	}
	
	protected static void merge(XmlSchema schema, XmlSchemaComplexType first, XmlSchemaComplexType second) {
		// check if the type is dynamic and can be merged
		if (SchemaMeta.isDynamic(first)) {
			
			// merge attributes
			mergeAttributes(schema, first.getAttributes(), second.getAttributes());
			
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
				mergeParticules(schema, firstSequence, secondSequence);
			} else {
				// suppose the type contains an extension
				XmlSchemaSimpleContent firstContent = (XmlSchemaSimpleContent) first.getContentModel();
				XmlSchemaSimpleContent secondContent = (XmlSchemaSimpleContent) second.getContentModel();
				
				if (firstContent != null && secondContent != null) {
					SchemaMeta.adoptReferences(schema, firstContent, secondContent);
					
					XmlSchemaSimpleContentExtension firstContentExtension = (XmlSchemaSimpleContentExtension) firstContent.getContent();
					XmlSchemaSimpleContentExtension secondContentExtension = (XmlSchemaSimpleContentExtension) secondContent.getContent();
		
					mergeAttributes(schema, firstContentExtension.getAttributes(), secondContentExtension.getAttributes());
					
					SchemaMeta.adoptReferences(schema, firstContentExtension, secondContentExtension);
				}
			}
		}
		SchemaMeta.adoptReferences(schema, first, second);
	}
	
	private static void mergeAttributes(XmlSchema schema, XmlSchemaObjectCollection first, XmlSchemaObjectCollection second) {
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
					SchemaMeta.adoptReferences(schema, aFirst, aSecond);
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
	
	private static void mergeParticules(final XmlSchema schema, XmlSchemaGroupBase first, XmlSchemaGroupBase second) {
		// wrap element collection in a standard java List interface
		List<XmlSchemaParticle> lFirst = new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaParticle>(first.getItems());
		List<XmlSchemaParticle> result = new ArrayList<XmlSchemaParticle>(first.getItems().getCount() + second.getItems().getCount());
		List<Boolean> minor = new ArrayList<Boolean>(first.getItems().getCount() + second.getItems().getCount());
		
		GenericUtils.merge(lFirst, new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaParticle>(second.getItems()), result, minor, new Comparator<XmlSchemaParticle>() {
			public int compare(XmlSchemaParticle first, XmlSchemaParticle second) {
				if (first instanceof XmlSchemaGroupBase && first.getClass().equals(second.getClass())) {
					mergeParticules(schema, (XmlSchemaGroupBase) first, (XmlSchemaGroupBase) second);
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
								merge(schema, tFirst, tSecond);
							} else {
								eFirst.setSchemaTypeName(null);
								eFirst.setSchemaType(tSecond);
							}
						}
						SchemaMeta.adoptReferences(schema, eFirst, eSecond);
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
		
		SchemaMeta.adoptReferences(schema, first, second);
	}
	
	public void clearCache(String projectName) {
		synchronized (schemaCache) {
			schemaCache.remove(projectName);
		}
	}
	
	public void validateResponse(String projectName, String requestableName, Document document) throws SAXException {
		try {
//			XmlSchema schema = getSchemaForProject(projectName);
//			XmlSchemaCollection collection = SchemaMeta.getCollection(schema);
//			Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
//			
//			String ns = schema.getTargetNamespace();
//			String prefix = collection.getNamespaceContext().getPrefix(ns);
//			Element elt = doc.createElementNS(ns, prefix+":"+requestableName + "Response");
//			doc.appendChild(elt);
//			
//			elt.appendChild(doc.renameNode(doc.importNode(document.getDocumentElement(), true), "", "response"));
//			
//			XMLUtils.spreadNamespaces(doc, "", false);
//			
//			XmlSchemaUtils.validate(collection, doc);

			XmlSchema schema = getSchemaForProject(projectName);
			XmlSchemaCollection collection = SchemaMeta.getCollection(schema);
			Document compliantDoc = makeResponse(document);
			if (Engine.logEngine.isTraceEnabled()) {
				Engine.logEngine.trace("(SchemaManager) validate response\n"+ XMLUtils.prettyPrintDOM(compliantDoc));
			}
			XmlSchemaUtils.validate(collection, compliantDoc);
			
		} catch (SAXException e) {
			throw e;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// add c8o_XXX attributes on element if needed (especially for self closed tags)
	public Document makeXmlRestCompliant(Document document) {
		return makeXmlRestCompliant(document, false);
	}
	
	private synchronized Document makeXmlRestCompliant(Document document, boolean debug) {
		try {
			Element documentElement = document.getDocumentElement();
			if (documentElement != null) {
				String project = documentElement.getAttribute("project");
				String sequence = documentElement.getAttribute("sequence");
				String connector = documentElement.getAttribute("connector");
				String transaction = documentElement.getAttribute("transaction");
				
				XmlSchema schema = getSchemaForProject(project);
				XmlSchemaCollection collection = SchemaMeta.getCollection(schema);
				
				String targetNamespace = schema.getTargetNamespace();
				String prefix = collection.getNamespaceContext().getPrefix(targetNamespace);
				String requestableName = sequence != null && sequence.length() > 0 ? sequence : connector + "__" + transaction;
				String tagname = prefix+":"+requestableName + "Response";
				QName qname = new QName(targetNamespace, requestableName + "Response");
				
				Project p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(project);
				boolean isIncludeResponseElement = true;
				if (!"".equals(sequence)) {
					try {
						Sequence seqObj = p.getSequenceByName(sequence);
						isIncludeResponseElement = seqObj.isIncludeResponseElement();
					}
					catch (Exception e) {}
				}
				
				Document responseDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				Element requestableResponse = responseDoc.createElementNS(targetNamespace, tagname);
				if (isIncludeResponseElement) {
					Node renamed = responseDoc.renameNode(responseDoc.importNode(documentElement, true), "", "response");
					requestableResponse.appendChild(renamed);
				}
				else {
					NodeList children = documentElement.getChildNodes();
					for (int i=0; i<children.getLength(); i++) {
						requestableResponse.appendChild(responseDoc.importNode(children.item(i), true));
					}
				}
				
				new XmlResponseWalker(true, true, debug) {
					
					private boolean keepText(String text) {
						try {
							if (!text.isEmpty()) {
								String strResult = "";
								StringTokenizer strtok = new StringTokenizer(text, "\r\n\t");
								while (strtok.hasMoreTokens()) {
									strResult += strtok.nextToken();
								}
								return text.equals(strResult);
							}
						} catch (Exception e) {}
						return true;
					}
					
					@Override
					public boolean makeCompliant(XmlSchemaObject xso, Node node) {
						return makeCompliant(xso, node, false);
					}
					
					protected boolean makeCompliant(XmlSchemaObject xso, Node node, boolean forceArray) {
						String nodeName = node.getNodeName();
						String localName = nodeName.substring(nodeName.indexOf(":")+1);
						String xsoName = xso instanceof XmlSchemaElement ? ((XmlSchemaElement)xso).getName() : ((XmlSchemaAttribute)xso).getName();
						
						if (xsoName.equals(localName)) {
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								Element element = (Element)node;
								
								String elName = element.getNodeName();
								
								long maxOccurs = ((XmlSchemaElement)xso).getMaxOccurs();
								if (debug && maxOccurs > 1L) {
									System.out.println(elName + ": maxOccurs="+ maxOccurs);
								}
								
								boolean isMixed = false;
								XmlSchemaType xmlSchemaType = ((XmlSchemaElement)xso).getSchemaType();
								if (xmlSchemaType != null) {
									isMixed = xmlSchemaType.isMixed();
								}
								if (debug && isMixed) {
									System.out.println(elName + ": is mixed !");
								}
								
								if (element.hasAttributes()) {
									List<Node> removeList = new ArrayList<Node>();
									NamedNodeMap attributes = element.getAttributes();
									for (int i=0; i <attributes.getLength(); i++) {
										Node attr = attributes.item(i);
										String prefix = attr.getPrefix();
										String attrname = attr.getNodeName();
										if ((prefix != null && (
												prefix.toLowerCase().equals("xsi") || 
												prefix.toLowerCase().equals("xmlns") ||
												prefix.toLowerCase().startsWith("soap-")
											)) ||
											(attrname != null && (
												attrname.toLowerCase().startsWith("xsi") || 
												attrname.toLowerCase().startsWith("xmlns") ||
												attrname.toLowerCase().startsWith("soap-")
											)))
										{
											removeList.add(attr);
										}
									}
									
									for (Node attr: removeList) {
										element.removeAttributeNode((Attr) attr);
									}
									
									try {
										// fix unsupported boolean deserialization from json (openapi-generator with resttemplate)
										// replace boolean string "0" or "1" with "false" or "true"
										for (XmlSchemaObject xsa: map.get(xso).get(1)) {
											if (xsa instanceof XmlSchemaAttribute) {
												XmlSchemaAttribute xmlSchemaAttribute= (XmlSchemaAttribute)xsa;
												String xsa_name = xmlSchemaAttribute.getName();
												QName xsa_qname = xmlSchemaAttribute.getSchemaTypeName();
												if (xsa_qname != null) {
													String xsa_type = xsa_qname.getLocalPart();
													Node attr = element.getAttributeNode(xsa_name);
													if (attr != null && !xsa_type.equals("string")) {
														String attr_value = attr.getNodeValue();
														if (xsa_type.equals("boolean")) {
															if (attr_value.equals("0")) {
																attr.setNodeValue("false");
															}
															if (attr_value.equals("1")) {
																attr.setNodeValue("true");
															}
														}
													}
												}
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
									
								}
								
								boolean hasAttributes = element.hasAttributes();
								int numOfAttr = map.get(xso).get(1).size();
								if (debug && !hasAttributes && numOfAttr > 0) {
									System.out.println(elName + ": has NO attributes and should have "+ numOfAttr);
								}
								
								boolean hasChildNode = element.hasChildNodes();
								int numOfChildren = map.get(xso).get(0).size();
								if (hasChildNode) {
									NodeList children = element.getChildNodes();
									int len = children.getLength();
									for (int i=0; i <len; i++) {
										Node n = children.item(i);
										if (n.getNodeType() == Node.TEXT_NODE) {
											String nv = n.getNodeValue();
											if (keepText(nv)) {
												//System.out.println("<text>"+nv+"</text>");
											}
											continue;
										}
										boolean needArray = false;
										for (XmlSchemaObject e : map.get(xso).get(0)) {
											if (e instanceof XmlSchemaElement) {
												XmlSchemaElement xse = (XmlSchemaElement)e;
												if ("StartC8oFakeOccurs".equals(xse.getName())) {
													needArray = true;
												}
												if ("EndC8oFakeOccurs".equals(xse.getName())) {
													needArray = false;
												}
											}
											
											if (makeCompliant(e, n, needArray)) {
												break;
											}
										}
									}
								}
								
								if (debug && !hasChildNode && numOfChildren > 0) {
									System.out.println(elName + ": has NO children and should have "+ numOfChildren);
								}
								
								boolean selfClosedTag = !hasChildNode;
								boolean needOfAttr = numOfAttr > 0;
								boolean needOfChildren = numOfChildren > 0;
								if (selfClosedTag) {
									// <tag/>  => {"tag": ""}
									// <tag c8o_emptyObject=""/>  => {"tag": {}}
									// <tag c8o_needAttr=""/>  => {"tag": {"text":"", "attr":[]}}
									// <tag c8o_emptyObject="" c8o_needAttr=""/>  => {"tag": {"attr":[]}}
									if (needOfChildren) {
										element.setAttribute("c8o_emptyObject", "");
									} else {
										element.setAttribute("c8o_nullObject", "");
									}
									if (needOfAttr) {
										element.setAttribute("c8o_needAttr", "");
									}
								} else {
									// <tag>text</tag>  => {"tag": {"text":"text"}}
									// <tag><user>user</user></tag>  => {"tag": {"user":"user"}}
									// <tag c8o_needAttr="">text</tag>  => {"tag": {"text":"text", "attr":[]}}
									// <tag c8o_needAttr=""><user>user</user></tag>  => {"tag": {"user":"user", "attr":[]}}
									if (isMixed || (needOfAttr && !hasAttributes)) {
										element.setAttribute("c8o_needAttr", "");
									}
								}
								// case need array
								if (maxOccurs > 1L || forceArray) {
									// <array c8o_arrayOfSingle=""/> => {"array": [""]}
									// <enum c8o_arrayOfSingle="">text</enum> => {"enum":["text"]}		
									// <user c8o_arrayOfSingle=""><name>name</name></user>  => {"user":[{"name":"name"}]}
									if (XMLUtils.countOccurrences(element) == 1) {
										element.setAttribute("c8o_arrayOfSingle", "");
									}
								}
							}
							return true;
						}
						else {
							//System.out.println(""+nodeName); // not matching child
						}
						return false;
					}
					
				}.init(schema, qname, requestableResponse);
				
				if (isIncludeResponseElement) {
					responseDoc.appendChild(requestableResponse.getFirstChild());
				}
				else {
					responseDoc.appendChild(requestableResponse);
				}
				return responseDoc;
			}
		}
		catch (Throwable t) {
			Engine.logContext.warn("An error occured while generating compliant XML for REST", t);
		}
		return document;
	}
	
	public synchronized Document makeResponse(Document document) {
		try {
			Element documentElement = document.getDocumentElement();
			if (documentElement != null) {
				String project = documentElement.getAttribute("project");
				String sequence = documentElement.getAttribute("sequence");
				String connector = documentElement.getAttribute("connector");
				String transaction = documentElement.getAttribute("transaction");
				
				XmlSchema schema = getSchemaForProject(project);
				XmlSchemaCollection collection = SchemaMeta.getCollection(schema);
				
				String targetNamespace = schema.getTargetNamespace();
				String prefix = collection.getNamespaceContext().getPrefix(targetNamespace);
				String requestableName = sequence != null && sequence.length() > 0 ? sequence : connector + "__" + transaction;
				String tagname = prefix+":"+requestableName + "Response";
				QName qname = new QName(targetNamespace, requestableName + "Response");

				Project p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(project);
				boolean isIncludeResponseElement = true;
				if (!"".equals(sequence)) {
					try {
						Sequence seqObj = p.getSequenceByName(sequence);
						isIncludeResponseElement = seqObj.isIncludeResponseElement();
					}
					catch (Exception e) {}
				}
				
				Document responseDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				Element requestableResponse = responseDoc.createElementNS(targetNamespace, tagname);
				if (isIncludeResponseElement) {
					Node renamed = responseDoc.renameNode(responseDoc.importNode(documentElement, true), "", "response");
					requestableResponse.appendChild(renamed);
				}
				else {
					NodeList children = documentElement.getChildNodes();
					for (int i=0; i<children.getLength(); i++) {
						requestableResponse.appendChild(responseDoc.importNode(children.item(i), true));
					}
				}
				
				final Map<XmlSchemaObject, List<List<XmlSchemaObject>>> map = new LinkedHashMap<XmlSchemaObject, List<List<XmlSchemaObject>>>();
				new XmlSchemaWalker(true, true) {
					List<XmlSchemaObject> elist = null;
					List<XmlSchemaObject> alist = null;
					Map<QName, XmlSchemaObject> mso = new HashMap<QName, XmlSchemaObject>();
					Map<QName, XmlSchemaObject> mto = new HashMap<QName, XmlSchemaObject>();
					
					public void init(XmlSchema xmlSchema, QName qname, Element element) {
						XmlSchemaElement rxe = SchemaMeta.getCollection(xmlSchema).getElementByQName(qname);
						if (rxe != null) {
							List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
							list.add(new ArrayList<XmlSchemaObject>());
							list.add(new ArrayList<XmlSchemaObject>());
							map.put(rxe, list);
							mso.put(qname, rxe);
							elist = list.get(0);
							alist = list.get(1);
							
							walkElement(xmlSchema, rxe);
							
							/*for (XmlSchemaObject xso: map.keySet()) {
								System.out.println(((XmlSchemaElement)xso).getName());
								System.out.print("\t[");
								for (XmlSchemaObject child: map.get(xso).get(1)) {
									System.out.print(((XmlSchemaAttribute)child).getName()+",");
								}
								System.out.println("]");
								for (XmlSchemaObject child: map.get(xso).get(0)) {
									System.out.println("\t"+((XmlSchemaElement)child).getName());
								}
							}*/
							
							makeCompliant(rxe, element);
						}
					}
					
					protected boolean makeCompliant(XmlSchemaObject xso, Node node) {
						String tns = node.getNamespaceURI();
						String nodeName = node.getNodeName();
						String localName = nodeName.substring(nodeName.indexOf(":")+1);
						String xsoName = xso instanceof XmlSchemaElement ? ((XmlSchemaElement)xso).getName() : ((XmlSchemaAttribute)xso).getName();
						
						if (xsoName.equals(localName)) {
							Document doc = node.getOwnerDocument();
							QName xsoQName = xso instanceof XmlSchemaElement ? ((XmlSchemaElement)xso).getQName() : ((XmlSchemaAttribute)xso).getQName();
							boolean elementQualified = SchemaMeta.getSchema(xso).getElementFormDefault().getValue().equals("qualified");
							boolean attributeQualified = SchemaMeta.getSchema(xso).getAttributeFormDefault().getValue().equals("qualified");
							boolean isQualified = (xsoQName != null && !xsoQName.getNamespaceURI().equals("")) || (xso instanceof XmlSchemaElement ? elementQualified : attributeQualified);
							//String targetNamespace = SchemaMeta.getSchema(xso).getTargetNamespace();
							//String prefix = SchemaMeta.getCollection(xso).getNamespaceContext().getPrefix(targetNamespace);
							String targetNamespace = xsoQName != null && !xsoQName.getNamespaceURI().equals("") ? xsoQName.getNamespaceURI():SchemaMeta.getSchema(xso).getTargetNamespace();
							String prefix = SchemaMeta.getCollection(xso).getNamespaceContext().getPrefix(targetNamespace);
							prefix = prefix == null ? (xsoQName != null && !xsoQName.getNamespaceURI().equals("") ? xsoQName.getPrefix():""):prefix;
							
							if (isQualified)
								node = doc.renameNode(node, targetNamespace, prefix+":"+localName);
							else
								node = doc.renameNode(node, "", localName);
							
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								Element element = (Element)node;
								
								if (element.hasAttributes()) {
									NamedNodeMap attributes = element.getAttributes();
									List<Node> list = new ArrayList<Node>();
									for (int i=0; i <attributes.getLength(); i++) {
										Node attr = attributes.item(i);
										String attrNodeName = attr.getNodeName();

										boolean found = false;
										for (XmlSchemaObject a : map.get(xso).get(1)) {
											if (makeCompliant(a, attr)) {
												found = true;
												break;
											}
										}
										
										if (!found) {
											if (attrNodeName.equals("xsi:type") || attrNodeName.startsWith("xmlns:") || attrNodeName.endsWith(":encodingStyle"))
											{
												list.add(attr);
											}
										}
										else {
											if (attrNodeName.startsWith("xsi:")) {
												attr = doc.renameNode(attr, Constants.URI_2001_SCHEMA_XSI, attrNodeName);
												
												String attrNodeValue = attr.getNodeValue();
												int index = attrNodeValue.indexOf(":");
												if (index !=- 1) {
													String pref = attrNodeValue.substring(0, index);
													if (attributes.getNamedItem("xmlns:"+pref) == null) {
														String ns = SchemaMeta.getCollection(xso).getNamespaceContext().getNamespaceURI(pref);
														if ("".equals(ns) || ns == null)
															ns = SchemaMeta.getSchema(xso).getTargetNamespace();
														element.setAttributeNS(Constants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+pref, ns);
													}
												}
											}
											if (attrNodeName.startsWith("xmlns:")) {
												attr = doc.renameNode(attr, Constants.XMLNS_ATTRIBUTE_NS_URI, attrNodeName);
											}
										}
									}
									
									for (Node attr : list) {
										element.removeAttributeNode((Attr) attr);
									}
									
								}
								
								if (element.hasChildNodes()) {
									NodeList children = element.getChildNodes();
									for (int i=0; i <children.getLength(); i++) {
										if (children.item(i).getNodeType() == Node.TEXT_NODE)
											continue;
										//System.out.println("element: "+children.item(i).getNodeName());
										for (XmlSchemaObject e : map.get(xso).get(0)) {
											//System.out.println(" test "+((XmlSchemaElement)e).getName());
											if (makeCompliant(e, children.item(i))) {
												break;
											}
										}
									}
								}
							}
							return true;
						}
						else {
							//System.out.println(nodeName);
							if ((tns != null) && !tns.equals("")) {
								Document doc = node.getOwnerDocument();
								String prefix = SchemaMeta.getCollection(xso).getNamespaceContext().getPrefix(tns);
								if ((prefix != null) && !prefix.equals("")) {
									node = doc.renameNode(node, tns, prefix+":"+localName);
								}
							}
						}
						return false;
					}
					
					
					@Override
					public void walkByTypeName(XmlSchema xmlSchema, QName qname) {
						XmlSchemaType obj = SchemaMeta.getCollection(xmlSchema).getTypeByQName(qname);
						if (obj != null) {
							if (!mso.containsKey(qname)) {
								mso.put(qname, obj);
								super.walkByTypeName(xmlSchema, qname);
							}
							else {
								if (mto.containsKey(qname)) {
									//System.out.println("\nWalk type "+ qname);
									elist.addAll(map.get(mto.get(qname)).get(0));
								}
							}
						}
					}

					@Override
					public void walkByElementRef(XmlSchema xmlSchema, QName qname) {
						XmlSchemaElement obj = SchemaMeta.getCollection(xmlSchema).getElementByQName(qname);
						if (obj != null) {
							if (!mso.containsKey(qname)) {
								mso.put(qname, obj);
								super.walkByElementRef(xmlSchema, qname);
							}
							else {
								if (mto.containsKey(qname)) {
									//System.out.println("\nWalk elem ref "+ qname);
									elist.add(mto.get(qname));
								}
							}
						}
					}

					@Override
					public void walkByAttributeGroupRef(XmlSchema xmlSchema, QName qname) {
						XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
						XmlSchemaAttributeGroup obj = (XmlSchemaAttributeGroup) schema.getAttributeGroups().getItem(qname);
						if (obj != null) {
							if (!mso.containsKey(qname)) {
								mso.put(qname, obj);
								super.walkByAttributeGroupRef(xmlSchema, qname);
							}
						}
					}

					@Override
					public void walkByAttributeRef(XmlSchema xmlSchema, QName qname) {
						XmlSchemaAttribute obj = SchemaMeta.getCollection(xmlSchema).getAttributeByQName(qname);
						if (obj != null) {
							if (!mso.containsKey(qname)) {
								mso.put(qname, obj);
								super.walkByAttributeRef(xmlSchema, qname);
							}
							else {
								if (mto.containsKey(qname)) {
									//System.out.println("\nWalk attr ref "+ qname);
									alist.add(mto.get(qname));
								}
							}
						}
					}

					@Override
					public void walkByGroupRef(XmlSchema xmlSchema, QName qname) {
						XmlSchema schema = SchemaMeta.getCollection(xmlSchema).schemaForNamespace(qname.getNamespaceURI());
						XmlSchemaGroup obj = (XmlSchemaGroup) schema.getGroups().getItem(qname);
						if (obj != null) {
							if (!mso.containsKey(qname)) {
								mso.put(qname, obj);
								super.walkByGroupRef(xmlSchema, qname);
							}
						}
					}
					
					@Override
					protected void walkElement(XmlSchema xmlSchema, XmlSchemaElement obj) {
						List<XmlSchemaObject> el = elist;
						List<XmlSchemaObject> al = alist;
						
						QName qname = obj.getQName();
						QName refName = obj.getRefName();
						QName typeName = obj.getSchemaTypeName();
						if (refName == null) {
							el.add(obj);
							List<List<XmlSchemaObject>> list = new ArrayList<List<XmlSchemaObject>>();
							list.add(new ArrayList<XmlSchemaObject>());
							list.add(new ArrayList<XmlSchemaObject>());
							map.put(obj, list);
							
							//System.out.print("\nname="+obj.getName());
							String ns = SchemaMeta.getSchema(obj).getTargetNamespace();
							if (typeName != null) {
								if (!mto.containsKey(typeName)) {
									if (typeName.getNamespaceURI().equals(Constants.URI_2001_SCHEMA_XSD)) {
										typeName = new QName(ns, obj.getName());
									}
									mto.put(typeName, obj);
									//System.out.print("; typeN="+typeName);
								} else {
									map.put(obj, map.get(mto.get(typeName)));
									
									// type already done
									return;
								}
							}
							else {
								if (qname != null) {
									if (qname.getNamespaceURI().equals("")) {
										qname = new QName(ns, obj.getName());
									}
								}
								else
									qname = new QName(ns, obj.getName());
								if (!mto.containsKey(qname)) {
									mto.put(qname, obj);
									//System.out.print("; qname="+qname);
								}
							}
							
							elist = list.get(0);
							alist = list.get(1);
							super.walkElement(xmlSchema, obj);
							elist = el;
							alist = al;
						}
						else {
							super.walkElement(xmlSchema, obj);
							elist = el;
							alist = al;
						}
					}
		
					@Override
					protected void walkAttribute(XmlSchema xmlSchema, XmlSchemaAttribute obj) {
						if (obj.getRefName() == null) {
							alist.add(obj);
						}
						super.walkAttribute(xmlSchema, obj);
						
					}
					
				}.init(schema, qname, requestableResponse);
		
				responseDoc.appendChild(requestableResponse);
				//System.out.println(XMLUtils.prettyPrintDOM(responseDoc));
				return responseDoc;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return document;
	}
	
	private XmlSchemaCacheEntry getCacheEntry(String projectName) {
		synchronized (schemaCache) {
			XmlSchemaCacheEntry cacheEntry = schemaCache.get(projectName);
			if (cacheEntry == null) {
				cacheEntry = new XmlSchemaCacheEntry();
				schemaCache.put(projectName, cacheEntry);
			}
			return cacheEntry;	
		}
	}
}
