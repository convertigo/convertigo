package com.twinsoft.convertigo.engine;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ISchemaGenerator;
import com.twinsoft.convertigo.beans.core.Project;
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
		
//		XmlSchema xsdSchema = collection.schemaForNamespace("http://www.w3.org/2001/XMLSchema");
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
		
//		schema.getIncludes().
		new WalkHelper() {
			XmlSchemaObject parent = null;			
			
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
//		schema.compile(null);
//		System.out.println(schema.isCompiled());
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(new DOMSource(schema.getSchemaDocument()), new StreamResult(System.out));
		return collection;
	}

	private static void add(XmlSchemaObject parent, XmlSchemaObject child, XmlSchema schema) {
		if (parent != null && child != null) {
			if (parent instanceof XmlSchemaGroupBase) {
				XmlSchemaGroupBase parentt = (XmlSchemaGroupBase) parent;
				parentt.getItems().add(child);
			} else if (parent instanceof XmlSchemaElement) {
				XmlSchemaElement parentt = (XmlSchemaElement) parent;
				XmlSchemaType type = parentt.getSchemaType();
				XmlSchemaComplexType ctype;
				
				if (type == null) {
					ctype = new XmlSchemaComplexType(schema);
					parentt.setSchemaType(ctype);
				} else if (type instanceof XmlSchemaComplexType) {
					ctype = (XmlSchemaComplexType) type;
				} else {
					ctype = null;
				}
				
				if (ctype != null) {
					QName typeName = parentt.getSchemaTypeName();
					XmlSchemaObjectCollection attributes = null;
					if (typeName != null) {
						parentt.setSchemaTypeName(null);
						XmlSchemaSimpleContent sContent = new XmlSchemaSimpleContent();
						XmlSchemaSimpleContentExtension sContentExt = new XmlSchemaSimpleContentExtension();
						sContentExt.setBaseTypeName(typeName);
						sContent.setContent(sContentExt);
						ctype.setContentModel(sContent);

						attributes = sContentExt.getAttributes();
					} else {
						XmlSchemaSimpleContentExtension sContentExt = (XmlSchemaSimpleContentExtension) ctype.getContentModel().getContent();
						attributes = sContentExt.getAttributes();
					}
					attributes.add(child);
				}
			}
		}
	}
}
