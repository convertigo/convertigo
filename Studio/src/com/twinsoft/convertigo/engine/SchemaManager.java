package com.twinsoft.convertigo.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
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
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ISchemaAttributeGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaElementGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaGenerator;
import com.twinsoft.convertigo.beans.core.ISchemaIncludeGenerator;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;


public class SchemaManager implements AbstractManager {

	public void init() throws EngineException {
		// TODO Auto-generated method stub
		
	}

	public void destroy() throws EngineException {
		// TODO Auto-generated method stub
		
	}
	
	public XmlSchemaCollection getSchemasForProject(String projectName) throws Exception {
		long timeStart = System.currentTimeMillis();
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		final XmlSchemaCollection collection = new XmlSchemaCollection();
		
		final XmlSchema schema = new XmlSchema(project.getTargetNamespace(), collection);
		
		NamespaceMap nsMap = new NamespaceMap();
		int cpt = 0;
		for (XmlSchema xs : collection.getXmlSchemas()) {
			String tns = xs.getTargetNamespace();
			String prefix;
			if (Constants.URI_2001_SCHEMA_XSD.equals(tns)) {
				prefix = "xsd";
			} else if (project.getTargetNamespace().equals(tns)) {
				prefix = project.getName() + "_ns";
			} else {
				prefix = "p" + cpt++;
			}
			nsMap.add(prefix, tns);
		}
		
		schema.setNamespaceContext(nsMap);
		collection.setNamespaceContext(nsMap);
		
		schema.setElementFormDefault(new XmlSchemaForm(project.getSchemaElementForm()));
		schema.setAttributeFormDefault(new XmlSchemaForm(project.getSchemaElementForm()));
		
		new WalkHelper() {
			List<XmlSchemaInclude> includeChildren;
			List<XmlSchemaElement> elementChildren;
			List<XmlSchemaAttribute> attributeChildren;

			@Override
			public void init(DatabaseObject databaseObject) throws Exception {
				List<XmlSchemaInclude> myIncludeChildren = includeChildren = new LinkedList<XmlSchemaInclude>();
				List<XmlSchemaElement> myElementChildren = elementChildren = new LinkedList<XmlSchemaElement>();
				List<XmlSchemaAttribute> myAttributeChildren = attributeChildren = new LinkedList<XmlSchemaAttribute>();
				
				super.init(databaseObject);
				
				for (XmlSchemaInclude include : myIncludeChildren) {
					XmlSchema xmlSchema = include.getSchema();
					if (xmlSchema != null) {
						XmlSchemaObjectCollection c = xmlSchema.getItems();
						Iterator<XmlSchemaObject> it = GenericUtils.cast(c.getIterator());
						while (it.hasNext()) {
							XmlSchemaObject xmlSchemaObject  = it.next();
							if (xmlSchemaObject instanceof XmlSchemaElement) {
								XmlSchemaElement element = (XmlSchemaElement)xmlSchemaObject;
								if (collection.getElementByQName(element.getQName()) == null)
									schema.getItems().add(xmlSchemaObject);
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
				}

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
					if (databaseObject instanceof ISchemaIncludeGenerator) {
						// Include case
						includeChildren.add(((ISchemaIncludeGenerator)databaseObject).getXmlSchemaObject(collection, schema));
						
					}
					else if (databaseObject instanceof ISchemaAttributeGenerator) {
						// Attribute case
						parentAttributeChildren.add(((ISchemaAttributeGenerator) databaseObject).getXmlSchemaObject(collection, schema));
						
					} else if (databaseObject instanceof ISchemaElementGenerator) {
						// Element case
						XmlSchemaElement element = ((ISchemaElementGenerator) databaseObject).getXmlSchemaObject(collection, schema);
						parentElementChildren.add(element);
						
						// new complexType to enhance the element
						XmlSchemaComplexType cType = new XmlSchemaComplexType(schema);
						
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
						
						XmlSchemaType type = null;
						String elementType = databaseObject.getComment();
						if (elementType != null && elementType.startsWith("tn:")) {
							elementType = elementType.substring(3);
							type = schema.getTypeByName(elementType);
							if (type == null) {
								cType.setName(elementType);
								schema.addType(cType);
								schema.getItems().add(cType);
							} else {
								XmlSchemaComplexType currentCType = (XmlSchemaComplexType) type;
								merge(currentCType, cType);
							}

							element.setSchemaTypeName(cType.getQName());
						} else {
							element.setSchemaType(cType);
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
		
		long timeStop = System.currentTimeMillis();
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(new DOMSource(schema.getSchemaDocument()), new StreamResult(System.out));
		System.out.println("Times >> total : " + (timeStop - timeStart) + " ms");
		return collection;
	}
	
	private static void merge(XmlSchemaComplexType first, XmlSchemaComplexType second) {
		mergeAttributes(first.getAttributes(), second.getAttributes());
		XmlSchemaSequence firstSequence = (XmlSchemaSequence) first.getParticle();
		XmlSchemaSequence secondSequence = (XmlSchemaSequence) second.getParticle();
		if (firstSequence != null || secondSequence != null) {
			if (firstSequence == null) {
				firstSequence = new XmlSchemaSequence();
				first.setParticle(firstSequence);
			} else if (secondSequence == null) {
				secondSequence = new XmlSchemaSequence();
			}
			mergeElements(firstSequence.getItems(), secondSequence.getItems());
		}
	}
	

	
	private static void mergeAttributes(XmlSchemaObjectCollection first, XmlSchemaObjectCollection second) {
		if (first.getCount() != 0 && second.getCount() != 0) {
			Iterator<XmlSchemaAttribute> iFirst = XmlSchemaUtils.attributesToSortedSet(first).iterator();
			Iterator<XmlSchemaAttribute> iSecond = XmlSchemaUtils.attributesToSortedSet(second).iterator();
			XmlSchemaAttribute aFirst = GenericUtils.nextOrNull(iFirst);
			XmlSchemaAttribute aSecond = GenericUtils.nextOrNull(iSecond);
			
			XmlSchemaUtils.clear(first);
	
			while (aFirst != null && aSecond != null) {			
				int compare = XmlSchemaUtils.attributeNameComparator.compare(aFirst, aSecond);
				if (compare == 0) {
					if (!aFirst.getUse().equals(aSecond.getUse())) {
						aFirst.setUse(XmlSchemaUtils.attributeUseOptional);
					}
					first.add(aFirst);
					aFirst = GenericUtils.nextOrNull(iFirst);
					aSecond = GenericUtils.nextOrNull(iSecond);
				} else if (compare < 0) {
					aFirst.setUse(XmlSchemaUtils.attributeUseOptional);
					first.add(aFirst);
					aFirst = GenericUtils.nextOrNull(iFirst);
				} else {
					aSecond.setUse(XmlSchemaUtils.attributeUseOptional);
					first.add(aSecond);
					aSecond = GenericUtils.nextOrNull(iSecond);
				}
			}
			
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
	
	private static void mergeElements(XmlSchemaObjectCollection first, XmlSchemaObjectCollection second) {
		List<XmlSchemaElement> lFirst = new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaElement>(first);
		List<XmlSchemaElement> result = new ArrayList<XmlSchemaElement>(first.getCount() + second.getCount());
		List<Boolean> minor = new ArrayList<Boolean>(first.getCount() + second.getCount());
		
		GenericUtils.merge(lFirst, new XmlSchemaUtils.XmlSchemaObjectCollectionList<XmlSchemaElement>(second), result, minor, new Comparator<XmlSchemaElement>() {
			public int compare(XmlSchemaElement o1, XmlSchemaElement o2) {
				int comp = o1.getName().compareTo(o2.getName());
				if (comp == 0) {
					// merge element
					comp += 0;
				}
				return comp;
			}
		});
		
		lFirst.clear();
		
		for (int i = 0; i < result.size(); i++) {
			XmlSchemaElement element = result.get(i);
			if (minor.get(i)) {
				element.setMinOccurs(0);
			}
			lFirst.add(element);
		}
	}
}
