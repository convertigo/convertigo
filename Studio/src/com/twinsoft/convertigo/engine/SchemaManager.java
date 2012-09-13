package com.twinsoft.convertigo.engine;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
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
		
		new WalkHelper() {
			XmlSchemaObject parent = schema;			
			
			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				XmlSchemaObject myParent = parent;
				if (databaseObject instanceof ISchemaGenerator) {
					ISchemaGenerator generator = (ISchemaGenerator) databaseObject;
					XmlSchemaObject current = generator.getXmlSchemaObject(collection, schema);
					add(myParent, current, schema);
					parent = current;
				}
				super.walk(databaseObject);
				
				parent = myParent;
			}
		}.init(project);
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(new DOMSource(schema.getSchemaDocument()), new StreamResult(System.out));
		return collection;
	}

	private static void add(XmlSchemaObject parent, XmlSchemaObject child, XmlSchema schema) {
		if (parent != null && child != null) {
			XmlSchemaObject container;
//			do {
				container = (XmlSchemaObject) getMetaInfo(parent, SchemaMeta.container);
				if (container != null) {
					parent = container;
				}
//			} while (container != null);
			
			if (parent instanceof XmlSchema) {
				add((XmlSchema) parent, child, schema);
			} else if (parent instanceof XmlSchemaGroupBase) {
				add((XmlSchemaGroupBase) parent, child, schema);
			} else if (parent instanceof XmlSchemaElement) {
				add((XmlSchemaElement) parent, child, schema);
			}
		}
	}
	
	private static void add(XmlSchema parent, XmlSchemaObject child, XmlSchema schema) {
		add(parent.getItems(), child, schema);
	}
	
	private static void add(XmlSchemaGroupBase parent, XmlSchemaObject child, XmlSchema schema) {
		add(parent.getItems(), child, schema);
	}
	
	private static void add(XmlSchemaObjectCollection parent, XmlSchemaObject child, XmlSchema schema) {
		parent.add(child);
	}
	
	private static void add(XmlSchemaElement parent, XmlSchemaObject child, XmlSchema schema) {
		XmlSchemaType type = parent.getSchemaType();
		XmlSchemaComplexType ctype;
		
		if (type == null) {
			ctype = new XmlSchemaComplexType(schema);
			parent.setSchemaType(ctype);
		} else if (type instanceof XmlSchemaComplexType) {
			ctype = (XmlSchemaComplexType) type;
		} else {
			ctype = null;
		}
		
		if (ctype != null) {
			QName typeName = parent.getSchemaTypeName();
			if (typeName != null) {
				parent.setSchemaTypeName(null);
				
				XmlSchemaSimpleContent sContent = new XmlSchemaSimpleContent();
				ctype.setContentModel(sContent);
				
				XmlSchemaSimpleContentExtension sContentExt = new XmlSchemaSimpleContentExtension();
				sContentExt.setBaseTypeName(typeName);
				sContent.setContent(sContentExt);

//				parent.getMetaInfoMap().put(SchemaMeta.container, sContentExt);
				add(sContentExt.getAttributes(), child, schema);
			} else {
				XmlSchemaContentModel contentModel = ctype.getContentModel();
				if (contentModel == null) {
					XmlSchemaSequence sequence = new XmlSchemaSequence();
					ctype.setParticle(sequence);
					parent.addMetaInfo(SchemaMeta.container, sequence);
					add(sequence, child, schema);
				} else {
					XmlSchemaSimpleContentExtension sContentExt = (XmlSchemaSimpleContentExtension) ctype.getContentModel().getContent();
					add(sContentExt.getAttributes(), child, schema);
				}
			}
		}
	}
	
	private static Object getMetaInfo(XmlSchemaObject xso, Object key) {
		Map<?, ?> map = xso.getMetaInfoMap();
		return map == null ? null : map.get(key);
	}
}
