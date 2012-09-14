package com.twinsoft.convertigo.engine;

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ISchemaAttributeGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaElementGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaGenerator;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;


public class SchemaManager implements AbstractManager {

	public void init() throws EngineException {
		// TODO Auto-generated method stub
		
	}

	public void destroy() throws EngineException {
		// TODO Auto-generated method stub
		
	}
	
	public XmlSchemaCollection getSchemasForProject(String projectName) throws Exception {
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		final XmlSchemaCollection collection = new XmlSchemaCollection();
		
		final XmlSchema schema = new XmlSchema(project.getName(), collection);
		
		NamespaceMap nsMap = new NamespaceMap();
		int cpt = 0;
		for (XmlSchema xs : collection.getXmlSchemas()) {
			String tns = xs.getTargetNamespace();
			String prefix;
			if (Constants.URI_2001_SCHEMA_XSD.equals(tns)) {
				prefix = "xsd";
			} else if (project.getName().equals(tns)) {
				prefix = "myns";
			} else {
				prefix = "p" + cpt++;
			}
			nsMap.add(prefix, tns);
		}
		
		schema.setNamespaceContext(nsMap);
		collection.setNamespaceContext(nsMap);
		
//		new WalkHelper() {
//			XmlSchemaObject parent = schema;
//			
//			@Override
//			protected void walk(DatabaseObject databaseObject) throws Exception {
//				XmlSchemaObject myParent = parent;
//				if (databaseObject instanceof ISchemaGenerator) {
//					ISchemaGenerator generator = (ISchemaGenerator) databaseObject;
//					XmlSchemaObject current = generator.getXmlSchemaObject(collection, schema);
//					add(myParent, current, schema);
//					parent = current;
//				}
//				super.walk(databaseObject);
//				
//				parent = myParent;
//			}
//		}.init(project);
		
		new WalkHelper() {
			List<XmlSchemaElement> elementChildren;
			List<XmlSchemaAttribute> attributeChildren;

			@Override
			public void init(DatabaseObject databaseObject) throws Exception {
				List<XmlSchemaElement> myElementChildren = elementChildren = new LinkedList<XmlSchemaElement>();
				List<XmlSchemaAttribute> myAttributeChildren = attributeChildren = new LinkedList<XmlSchemaAttribute>();
				
				super.init(databaseObject);
				
				for (XmlSchemaElement element : myElementChildren) {
					schema.getItems().add(element);
				}
				
				for (XmlSchemaAttribute attribute : myAttributeChildren) {
					schema.getItems().add(attribute);
				}
			}
			
			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				List<XmlSchemaElement> parentElementChildren = elementChildren;
				List<XmlSchemaAttribute> parentAttributeChildren = attributeChildren;
				
				if (databaseObject instanceof ISchemaGenerator && ((ISchemaGenerator) databaseObject).isOutput()) {
					List<XmlSchemaElement> myElementChildren = null;
					List<XmlSchemaAttribute> myAttributeChildren = null;
					
					// prepare to receive children
					if (databaseObject instanceof ISchemaElementGenerator) {
						myElementChildren = elementChildren = new LinkedList<XmlSchemaElement>();
						myAttributeChildren = attributeChildren = new LinkedList<XmlSchemaAttribute>();
					}
					
					// deep walk
					super.walk(databaseObject);
					
					// generate itself and add to the caller list
					if (databaseObject instanceof ISchemaAttributeGenerator) {
						// Attribute case
						parentAttributeChildren.add(((ISchemaAttributeGenerator) databaseObject).getXmlSchemaObject(collection, schema));
						
					} else if (databaseObject instanceof ISchemaElementGenerator) {
						// Element case
						XmlSchemaElement element = ((ISchemaElementGenerator) databaseObject).getXmlSchemaObject(collection, schema);
						parentElementChildren.add(element);
						
						// do something only on case of child
						if (!myElementChildren.isEmpty() || !myAttributeChildren.isEmpty()) {
	
							// retrieve the xsd:element to add children
							element = SchemaMeta.getContainerElement(element);
							
							// prepare element children in a xsd:sequence
							XmlSchemaSequence sequence = null;
							if (!myElementChildren.isEmpty()) {
								sequence = new XmlSchemaSequence();
								
								for (XmlSchemaElement elt : myElementChildren) {
									sequence.getItems().add(elt);
								}
							}
							
							// new complexType to enhance the element
							XmlSchemaComplexType cType = new XmlSchemaComplexType(schema);
							element.setSchemaType(cType);
							
							// check for existing type
							QName typeName = element.getSchemaTypeName();
							if (typeName != null) {
								// the type must be customized, create an extension
								element.setSchemaTypeName(null);
								
								XmlSchemaComplexContent cContent = new XmlSchemaComplexContent();
								cType.setContentModel(cContent);
								
								XmlSchemaComplexContentExtension cContentExt = new XmlSchemaComplexContentExtension();
								cContent.setContent(cContentExt);
									
								cContentExt.setBaseTypeName(typeName);
								cContentExt.setParticle(sequence);
								
								// add attributes
								for (XmlSchemaAttribute attribute : myAttributeChildren) {
									cContentExt.getAttributes().add(attribute);
								}
								
								// add elements
								if (sequence != null) {
									cContentExt.setParticle(sequence);
								}
							} else {
								
								// add attributes
								for (XmlSchemaAttribute attribute : myAttributeChildren) {
									cType.getAttributes().add(attribute);
								}
								
								// add elements
								if (sequence != null) {
									cType.setParticle(sequence);
								}
							}
						}
					}
				} else {
					// doesn't generate schema, just deep walk
					super.walk(databaseObject);
				}

				elementChildren = parentElementChildren;
				attributeChildren = parentAttributeChildren;
			}
		}.init(project);
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(new DOMSource(schema.getSchemaDocument()), new StreamResult(System.out));
		return collection;
	}

//	private static void add(XmlSchemaObject parent, XmlSchemaObject child, XmlSchema schema) {
//		if (parent != null && child != null) {
//			XmlSchemaObject container;
//			do {
//				container = (XmlSchemaObject) getMetaInfo(parent, SchemaMeta.container);
//				if (container != null) {
//					parent = container;
//				}
//			} while (container != null);
//			
//			if (parent instanceof XmlSchema) {
//				add((XmlSchema) parent, child, schema);
//			} else if (parent instanceof XmlSchemaGroupBase) {
//				add((XmlSchemaGroupBase) parent, child, schema);
//			} else if (parent instanceof XmlSchemaElement) {
//				add((XmlSchemaElement) parent, child, schema);
//			}
//		}
//	}
//	
//	private static void add(XmlSchema parent, XmlSchemaObject child, XmlSchema schema) {
//		add(parent.getItems(), child, schema);
//	}
//	
//	private static void add(XmlSchemaGroupBase parent, XmlSchemaObject child, XmlSchema schema) {
//		add(parent.getItems(), child, schema);
//	}
//	
//	private static void add(XmlSchemaObjectCollection parent, XmlSchemaObject child, XmlSchema schema) {
//		parent.add(child);
//	}
//	
//	private static void add(XmlSchemaElement parent, XmlSchemaObject child, XmlSchema schema) {
//		
//		
//		if (child instanceof XmlSchemaElement) {
//			add(parent, (XmlSchemaElement) child, schema);
//		} else if (child instanceof XmlSchemaAttribute) {
//			add(parent, (XmlSchemaAttribute) child, schema);
//		}
//		
//		XmlSchemaType type = parent.getSchemaType();
//		XmlSchemaComplexType ctype;
//		
//		if (type == null) {
//			ctype = new XmlSchemaComplexType(schema);
//			parent.setSchemaType(ctype);
//		} else if (type instanceof XmlSchemaComplexType) {
//			ctype = (XmlSchemaComplexType) type;
//		} else {
//			ctype = null;
//		}
//		
//		if (ctype != null) {
//			QName typeName = parent.getSchemaTypeName();
//			if (typeName != null) {
//				parent.setSchemaTypeName(null);
//				
//				XmlSchemaSimpleContent sContent = new XmlSchemaSimpleContent();
//				ctype.setContentModel(sContent);
//				
//				XmlSchemaSimpleContentExtension sContentExt = new XmlSchemaSimpleContentExtension();
//				sContentExt.setBaseTypeName(typeName);
//				sContent.setContent(sContentExt);
//				
////				parent.getMetaInfoMap().put(SchemaMeta.container, sContentExt);
//				add(sContentExt.getAttributes(), child, schema);
//			} else {
//				XmlSchemaContentModel contentModel = ctype.getContentModel();
//				if (contentModel == null) {
//					XmlSchemaSequence sequence = new XmlSchemaSequence();
//					ctype.setParticle(sequence);
//					parent.addMetaInfo(SchemaMeta.container, sequence);
//					add(sequence, child, schema);
//				} else {
//					XmlSchemaSimpleContentExtension sContentExt = (XmlSchemaSimpleContentExtension) ctype.getContentModel().getContent();
//					add(sContentExt.getAttributes(), child, schema);
//				}
//			}
//		}
//	}
//	
//	private static void add(XmlSchemaElement parent, XmlSchemaElement child, XmlSchema schema) {
//		
//	}
//	
//	private static void add(XmlSchemaElement parent, XmlSchemaAttribute child, XmlSchema schema) {
//		
//	}
//	
//	private static Object getMetaInfo(XmlSchemaObject xso, Object key) {
//		Map<?, ?> map = xso.getMetaInfoMap();
//		return map == null ? null : map.get(key);
//	}
}
