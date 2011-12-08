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

package com.twinsoft.convertigo.engine.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.util.QuickSort;
import com.twinsoft.util.QuickSortItem;
import com.twinsoft.util.StringEx;

public class XSDUtils {

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
	
	protected XSDUtils() {
		
	}
	
	public static XSD getXSD(String xsdURI) throws XSDException {
		XSD xsd = new XSDUtils(). new XSD(xsdURI);
		return xsd;
	}
	
	public static XSD createXSD(String projectName, String xsdURI) throws XSDException {
		XSD xsd = new XSDUtils(). new XSD();
		xsd.create(projectName, xsdURI);
		return xsd;
	}
	
	public static Document parseDOM(String filePath) throws ParserConfigurationException, SAXException, IOException {
		Document doc = XSDUtils.getDefaultDocumentBuilder().parse(new File(filePath));
		return doc;
	}

	public static Document parseDOM(InputStream is) throws ParserConfigurationException, SAXException, IOException {
		Document doc = XSDUtils.getDefaultDocumentBuilder().parse(is);
		return doc;
	}
	
	public class XSDException extends ConvertigoException {
		private static final long serialVersionUID = 129800084129747142L;

		public XSDException(String message) {
			super(message);
		}

		public XSDException(String message, Throwable cause) {
			super(message, cause);
		}
	}
	
	public class XmlGenerationDescription {
		private boolean outputOccurences = false;
		private boolean outputOccursAttribute = false;
		private boolean outputElementWithNS = false;
		private boolean outputSchemaTypeCData = true;
		private String doneAttribute = "done";
		private String occursAttribute = "occurs";
		
		protected XmlGenerationDescription() {
			
		}
		public boolean isOutputOccurences() {
			return outputOccurences;
		}
		public void setOutputOccurences(boolean outputOccurences) {
			this.outputOccurences = outputOccurences;
		}
		public boolean isOutputOccursAttribute() {
			return outputOccursAttribute;
		}
		public void setOutputOccursAttribute(boolean outputOccursAttribute) {
			this.outputOccursAttribute = outputOccursAttribute;
		}
		public boolean isOutputSchemaTypeCData() {
			return outputSchemaTypeCData;
		}
		public void setOutputSchemaTypeCData(boolean outputSchemaTypeCData) {
			this.outputSchemaTypeCData = outputSchemaTypeCData;
		}
		public boolean isOutputElementWithNS() {
			return outputElementWithNS;
		}
		public void setOutputElementWithNS(boolean outputElementWithNS) {
			this.outputElementWithNS = outputElementWithNS;
		}
		public String getDoneAttribute() {
			return doneAttribute;
		}
		public void setDoneAttribute(String doneAttribute) {
			this.doneAttribute = doneAttribute;
		}
		public String getOccursAttribute() {
			return occursAttribute;
		}
		public void setOccursAttribute(String occursAttribute) {
			this.occursAttribute = occursAttribute;
		}
	}
	
	protected class XSDURIResolver extends DefaultURIResolver {

		@Override
		protected URL getFileURL(URL arg0, String arg1) throws IOException {
			return super.getFileURL(arg0, arg1);
		}

		@Override
		protected URL getURL(URL arg0, String arg1) throws IOException {
			return super.getURL(arg0, arg1);
		}

		@Override
		protected boolean isAbsolute(String uri) {
			return super.isAbsolute(uri);
		}

		@Override
		public InputSource resolveEntity(String ns, String location, String path) {
			InputSource entity;
			if (location.startsWith("../") && location.endsWith(".xsd")) {
				File f = new File(path+location);
				if (!f.exists()) { // deleted project or not yet imported project!
					String stream = "";
					stream += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
					stream += "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\""+ns+"\" targetNamespace=\""+ns+"\" attributeFormDefault=\"qualified\" elementFormDefault=\"qualified\">";
					stream += "</xsd:schema>";
					try {
						entity = new InputSource(new ByteArrayInputStream(stream.getBytes("UTF-8")));
					} catch (UnsupportedEncodingException e) {
						entity = new InputSource(new ByteArrayInputStream(stream.getBytes()));
					}
				}
				else {
					entity = super.resolveEntity(ns, location, path);
				}
			}
			else {
				entity =  super.resolveEntity(ns, location, path);
			}
			return entity;
		}
	}
	
	public class XSD {
		private XmlSchemaCollection schemaCol = null;
		private XmlSchema loadedSchema = null;
		private String baseURI = null;
		private String xsdURI = null;
		private boolean bModified = false;
		private HashMap<String,String> options = new HashMap<String,String>();
		private XmlGenerationDescription xmlgenDescription = new XmlGenerationDescription();
		
		protected XSD() {
			options.put(OutputKeys.METHOD, "xml");
			options.put(OutputKeys.ENCODING, "UTF-8");
			options.put(OutputKeys.INDENT, "yes");
			options.put(OutputKeys.OMIT_XML_DECLARATION, "no");
			options.put(OutputKeys.CDATA_SECTION_ELEMENTS, "{"+Constants.URI_2001_SCHEMA_XSD+"}documentation");
		}
		
		protected XSD(String xsdURI) throws XSDException {
			this();
			try {
				this.xsdURI = xsdURI;
				this.baseURI = xsdURI.substring(0, xsdURI.lastIndexOf("/")+1);
				schemaCol = new XmlSchemaCollection();
				schemaCol.setBaseUri(baseURI);
				schemaCol.setSchemaResolver(new XSDURIResolver());
				//loadedSchema = schemaCol.read(new StreamSource(new FileInputStream(xsdURI)), null); // StreamSource ignore CDATA nodes!
				loadedSchema = schemaCol.read(parseDOM(xsdURI), null);
				
				//System.out.println("\ngetXSD:");
				//if (loadedSchema != null) loadedSchema.write(System.out, options);
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
				throw new XSDException("Invalid XSD", e);
			}
		}
		
		public String getXsdURI() {
			return xsdURI;
		}
		
		public String getBaseURI() {
			return baseURI;
		}

		public XmlGenerationDescription getXmlGenerationDescription() {
			return xmlgenDescription;
		}
		
		public Document[] getAllSchemas() {
			XmlSchema[] schemas = schemaCol.getXmlSchemas();
			int len = schemas.length;
			Document[] docs = new Document[len];
			
			for (int i=0; i<len; i++) {
				XmlSchema schema = schemas[i];
				try {
					docs[i] = getSchemaDOM(schema);
				}
				catch (Exception e) {}
			}
			
			return docs;
			//return loadedSchema.getAllSchemas(); // does not work!
		}
		
		public Document getSchemaDOM(XmlSchema xmlSchema) {
			Document doc = null;
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				xmlSchema.write(bos);
				doc = XMLUtils.parseDOM(new ByteArrayInputStream(bos.toByteArray()));
			}
			catch (Exception e) {
				//System.out.println(e);
			}
			return doc;
		}
		
		public String[] getTypes() {
			String[] types = new String[]{};
			Vector<QuickSortItem> v = new Vector<QuickSortItem>();
			
			NamespacePrefixList list = loadedSchema.getNamespaceContext();
			
			XmlSchema[] schemas = schemaCol.getXmlSchemas();
			for (int i=0; i<schemas.length; i++) {
				XmlSchema schema = schemas[i];
				v.addAll(getTypes(schema, list.getPrefix(schema.getTargetNamespace())));
			}
			
	        QuickSort q = new QuickSort(v);
	        v = GenericUtils.cast(q.perform(true));
	        
	        types = new String[v.size()];
			for (int i=0; i<v.size(); i++) {
				types[i] = v.elementAt(i).toString();
			}
	        
			return types;
		}
		
		public void addNamespaces(Map<String, String> map) {
			NamespaceMap namespacesMap = addNamespaces(loadedSchema, map);
			loadedSchema.setNamespaceContext(namespacesMap);
			bModified = true;
		}
		
		public Map<String, String> getNamespaceMap() {
			NamespaceMap namespacesMap = addNamespaces(loadedSchema, new HashMap<String, String>());
			return GenericUtils.cast(namespacesMap);
		}
		
		private NamespaceMap addNamespaces(XmlSchema xmlSchema, Map<String, String> map) {
			Map<String, String> namespacesMap = GenericUtils.cast(new NamespaceMap());
			NamespacePrefixList namespacePrefixList = loadedSchema.getNamespaceContext();
			String[] prefixes = namespacePrefixList.getDeclaredPrefixes();
			for (int i=0; i<prefixes.length; i++) {
				String prefix = prefixes[i];
				String ns = namespacePrefixList.getNamespaceURI(prefix);
				namespacesMap.put(prefix, ns);
			}
			namespacesMap.putAll(map);
			return (NamespaceMap) namespacesMap;
		}
		
		public void addImportObjects(Map<String, String> map) {
			Iterator<String> it = map.keySet().iterator();
			while (it.hasNext()) {
				String ns = it.next();
				String location = map.get(ns);
				
				XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
				xmlSchemaImport.setNamespace(ns);
				xmlSchemaImport.setSchemaLocation(location);
				
				boolean bFound = false;
				Iterator<Object> it2 = GenericUtils.cast(loadedSchema.getItems().getIterator());
				while (it2.hasNext()) {
					Object ob = it2.next();
					if (ob instanceof XmlSchemaImport) {
						if (((XmlSchemaImport)ob).getNamespace().equals(ns)) {
							bFound = true;
							break;
						}
					}
				}
				if (!bFound) {
					loadedSchema.getItems().add(xmlSchemaImport);
					bModified = true;
				}
			}
		}
		
		public boolean hasSchemaObject(String projectName, String xsdString) throws XSDException {
			XmlSchema typesSchema = toXmlSchema(xsdString, projectName);
			if (typesSchema != null) {
				XmlSchemaObjectCollection items = typesSchema.getItems();
				Iterator<Object> it0 = GenericUtils.cast(items.getIterator());
				if (it0.hasNext()) {
					Iterator<Object> it1 = null;
					Object searchOb = null;
					XmlSchemaObject object = (XmlSchemaObject)it0.next();
					if (object instanceof XmlSchemaType) {
						it1 = GenericUtils.cast(loadedSchema.getSchemaTypes().getNames());
						searchOb = ((XmlSchemaType)object).getQName();
					}
					else if (object instanceof XmlSchemaElement) {
			            it1 = GenericUtils.cast(loadedSchema.getElements().getNames());
			            searchOb = ((XmlSchemaType)object).getQName();
					}
					else if (object instanceof XmlSchemaGroup) {
			            it1 = GenericUtils.cast(loadedSchema.getGroups().getNames());
			            searchOb = ((XmlSchemaType)object).getName();
					}
					else if (object instanceof XmlSchemaAttribute) {
			            it1 = GenericUtils.cast(loadedSchema.getAttributes().getNames());
			            searchOb = ((XmlSchemaType)object).getQName();
					}
					
					if (it1 != null) {
						while (it1.hasNext()) {
							Object ob = it1.next();
							if (ob.toString().equals(searchOb.toString())) {
								return true;
							}
						}
					}
				}
			}
			
			return false;
		}
		
		public void addSchemaObjects(String projectName, String xsdString) throws XSDException {
			XmlSchema typesSchema = toXmlSchema(xsdString, projectName);
			removeSchemaObjects(typesSchema);
			addSchemaObjects(typesSchema);
		}
		
		public void removeSchemaObjects(String projectName, String xsdString) throws XSDException {
			XmlSchema typesSchema = toXmlSchema(xsdString, projectName);
			removeSchemaObjects(typesSchema);
		}
		
		public void removeSchemaObjectsNotIn(List<String> list) {
			List<QName> qnames = new ArrayList<QName>();
			for (String s : list) {
				String typePrefix = s.substring(0, s.indexOf(":"));
				String typeName = s.substring(s.indexOf(":")+1);
				String ns = loadedSchema.getNamespaceContext().getNamespaceURI(typePrefix);
				QName qname = new QName(ns,typeName,typePrefix);
				qnames.add(qname);
			}
			
			removeSchemaObjectsExcept(qnames);
		}
		
		public List<QName> getSchemaElementNames() {
			ArrayList<QName> names = new ArrayList<QName>();
			XmlSchemaObjectTable elementsTable = loadedSchema.getElements();
			Iterator<QName> it = GenericUtils.cast(elementsTable.getNames());
			while (it.hasNext()) {
				names.add(it.next());
			}
			return names;
		}

		private Document getXsdToXmlDocument() throws ParserConfigurationException {
			Document xmlDom = createXmlDom();
			Element root = xmlDom.getDocumentElement();
			root.setAttribute("context","string");
			root.setAttribute("contextId","string");
			root.setAttribute("fromcache","string");
			root.setAttribute("project","string");
			root.setAttribute("connector","string");
			root.setAttribute("sequence","string");
			root.setAttribute("transaction","string");
			root.setAttribute("generated","long");
			root.setAttribute("version","string");
			return xmlDom;
		}
		
		public Document generateDocumentXmlStructure() throws ParserConfigurationException {
			Document xmlDom = getXsdToXmlDocument();
			XmlSchemaObjectTable elementsTable = loadedSchema.getElements();
			Iterator<XmlSchemaObject> it = GenericUtils.cast(elementsTable.getValues());
			while (it.hasNext()) {
				schemaObjectToXml(xmlDom, xmlDom.getDocumentElement(), it.next());
			}
			return xmlDom;
		}
		
		public Document generateElementXmlStructure(QName elementQName) throws ParserConfigurationException {
			Document xmlDom = getXsdToXmlDocument();
			XmlSchemaElement xmlSchemaElement = loadedSchema.getElementByName(elementQName);
			if (xmlSchemaElement != null) {
				schemaElementToXml(xmlDom, xmlDom.getDocumentElement(), xmlSchemaElement);
			}
			return xmlDom;
		}
		
		public Document generateTypeXmlStructure(String typePrefix, String typeName) throws ParserConfigurationException {
			String ns = loadedSchema.getNamespaceContext().getNamespaceURI(typePrefix);
			QName qname = new QName(ns,typeName,typePrefix);
			return generateTypeXmlStructure(qname);
		}

		public Document generateTypeXmlStructure(QName typeQName) throws ParserConfigurationException {
			Document xmlDom = getXsdToXmlDocument();
			XmlSchemaType xmlSchemaType = loadedSchema.getTypeByName(typeQName);
			if (xmlSchemaType != null) {
				schemaTypeToXml(xmlDom, xmlDom.getDocumentElement(), xmlSchemaType);
			}
			return xmlDom;
		}
		
		public void save() throws XSDException {
			try {
				if (bModified) {
					FileOutputStream fos = null;
					fos = new FileOutputStream(xsdURI);
					loadedSchema.write(fos, options);
					if (fos != null) fos.close();
					bModified = false;
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
				throw new XSDException("Unable to save XSD to file",e);
			}
		}

		public void writeTo(String filePath) throws XSDException {
			if ((filePath == null) || (filePath.equals("")))
				throw new XSDException("Invalid filePath parameter");
			
			try {
				FileOutputStream fos = new FileOutputStream(filePath);
				loadedSchema.write(fos, options);
				if (fos != null) fos.close();
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
				throw new XSDException("Unable to write XSD to file",e);
			}
		}

		private void schemaObjectToXml(Document xmlDom, Node parentNode, XmlSchemaObject xmlSchemaObject) {
			if (xmlSchemaObject instanceof XmlSchemaAnnotated) {
				schemaAnnotatedToXml(xmlDom, parentNode, (XmlSchemaAnnotated)xmlSchemaObject);
			}
			else if (xmlSchemaObject instanceof XmlSchemaAnnotation) {
				schemaAnnotationToXml(xmlDom, parentNode, (XmlSchemaAnnotation)xmlSchemaObject);
			}
			else if (xmlSchemaObject instanceof XmlSchemaAppInfo) {
				schemaAppInfoToXml(xmlDom, parentNode, (XmlSchemaAppInfo)xmlSchemaObject);
			}
			else if (xmlSchemaObject instanceof XmlSchemaDocumentation) {
				schemaDocumentationToXml(xmlDom, parentNode, (XmlSchemaDocumentation)xmlSchemaObject);
			}
		}
		
		private void schemaAnnotationToXml(Document xmlDom, Node parentNode, XmlSchemaAnnotation xmlSchemaAnnotation) {
			//TODO
		}
		
		private void schemaAppInfoToXml(Document xmlDom, Node parentNode, XmlSchemaAppInfo xmlSchemaAppInfo) {
			//TODO
		}
		
		private void schemaDocumentationToXml(Document xmlDom, Node parentNode, XmlSchemaDocumentation xmlSchemaDocumentation) {
			//TODO
		}
		
		private void schemaAnnotatedToXml(Document xmlDom, Node parentNode, XmlSchemaAnnotated xmlSchemaAnnotated) {
			if (xmlSchemaAnnotated instanceof XmlSchema) {
				;
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaAnyAttribute) {
				schemaAnyAttributeToXml(xmlDom, parentNode, (XmlSchemaAnyAttribute)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaAttribute) {
				schemaAttributeToXml(xmlDom, parentNode, (XmlSchemaAttribute)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaAttributeGroup) {
				schemaAttributeGroupToXml(xmlDom, parentNode, (XmlSchemaAttributeGroup)xmlSchemaAnnotated);
				
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaAttributeGroupRef) {
				schemaAttributeGroupRefToXml(xmlDom, parentNode, (XmlSchemaAttributeGroupRef)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaContent) {
				schemaContentToXml(xmlDom, parentNode, (XmlSchemaContent)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaContentModel) {
				schemaContentModelToXml(xmlDom, parentNode, (XmlSchemaContentModel)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaExternal) {
				;
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaFacet) {
				;
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaGroup) {
				schemaGroupToXml(xmlDom, parentNode, (XmlSchemaGroup)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaIdentityConstraint) {
				;
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaNotation) {
				;
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaParticle) {
				schemaParticleToXml(xmlDom, parentNode, (XmlSchemaParticle)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaSimpleTypeContent) {
				schemaSimpleTypeContentToXml(xmlDom, parentNode, (XmlSchemaSimpleTypeContent)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaType) {
				schemaTypeToXml(xmlDom, parentNode, (XmlSchemaType)xmlSchemaAnnotated);
			}
			else if (xmlSchemaAnnotated instanceof XmlSchemaXPath) {
				;
			}
		}

		private void schemaAnyAttributeToXml(Document xmlDom, Node parentNode, XmlSchemaAnyAttribute xmlSchemaAnyAttribute) {
			;
		}

		private void schemaAttributeToXml(Document xmlDom, Node parentNode, XmlSchemaAttribute xmlSchemaAttribute) {
			String name = xmlSchemaAttribute.getName();
			//Object type = xmlSchemaAttribute.getAttributeType();
			XmlSchemaSimpleType  xmlSchemaSimpleType = xmlSchemaAttribute.getSchemaType();
			
			Attr attr = xmlDom.createAttribute(name);
			((Element)parentNode).setAttributeNode(attr);
			
			if (xmlSchemaSimpleType == null) {
				String value = xmlSchemaAttribute.getDefaultValue();
				try {
					value = ((value == null) ? xmlSchemaAttribute.getFixedValue():value);
					value = ((value == null) ? xmlSchemaAttribute.getSchemaTypeName().getLocalPart():value);
				}
				catch (Exception e) {}
				
				if (value != null) {
					attr.setNodeValue(value);
				}
			}
			else {
				schemaSimpleTypeToXml(xmlDom, attr, xmlSchemaSimpleType);
			}
		}

		private void schemaAttributeGroupToXml(Document xmlDom, Node parentNode, XmlSchemaAttributeGroup xmlSchemaAttributeGroup) {
			XmlSchemaObjectCollection collection = xmlSchemaAttributeGroup.getAttributes();
			Iterator<XmlSchemaAttribute> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaAttributeToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaAttributeGroupRefToXml(Document xmlDom, Node parentNode, XmlSchemaAttributeGroupRef xmlSchemaAttributeGroupRef) {
			XmlSchemaAttributeGroup xmlSchemaAttributeGroup = (XmlSchemaAttributeGroup)loadedSchema.getGroups().getItem(xmlSchemaAttributeGroupRef.getRefName());
			schemaAttributeGroupToXml(xmlDom, parentNode, xmlSchemaAttributeGroup);
		}
		
		private void schemaContentToXml(Document xmlDom, Node parentNode, XmlSchemaContent xmlSchemaContent) {
			if (xmlSchemaContent instanceof XmlSchemaComplexContentExtension) {
				schemaComplexContentExtensionToXml(xmlDom, parentNode, (XmlSchemaComplexContentExtension)xmlSchemaContent);
			}
			else if (xmlSchemaContent instanceof XmlSchemaComplexContentRestriction) {
				schemaComplexContentRestrictionToXml(xmlDom, parentNode, (XmlSchemaComplexContentRestriction)xmlSchemaContent);
			}
			else if (xmlSchemaContent instanceof XmlSchemaSimpleContentExtension) {
				schemaSimpleContentExtensionToXml(xmlDom, parentNode, (XmlSchemaSimpleContentExtension)xmlSchemaContent);
			}
			else if (xmlSchemaContent instanceof XmlSchemaSimpleContentRestriction) {
				schemaSimpleContentRestrictionToXml(xmlDom, parentNode, (XmlSchemaSimpleContentRestriction)xmlSchemaContent);
			}
		}
		
		private void schemaSimpleContentRestrictionToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleContentRestriction xmlSchemaSimpleContentRestriction) {
			schemaSimpleTypeToXml(xmlDom, parentNode, xmlSchemaSimpleContentRestriction.getBaseType());
			
			XmlSchemaObjectCollection collection = xmlSchemaSimpleContentRestriction.getAttributes();
			Iterator<XmlSchemaAttribute> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaAttributeToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaSimpleContentExtensionToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleContentExtension xmlSchemaSimpleContentExtension) {
			XmlSchemaObjectCollection collection = xmlSchemaSimpleContentExtension.getAttributes();
			Iterator<XmlSchemaAttribute> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaAttributeToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaComplexContentRestrictionToXml(Document xmlDom, Node parentNode, XmlSchemaComplexContentRestriction xmlSchemaComplexContentRestriction) {
			schemaParticleToXml(xmlDom, parentNode, xmlSchemaComplexContentRestriction.getParticle());
			
			XmlSchemaObjectCollection collection = xmlSchemaComplexContentRestriction.getAttributes();
			Iterator<XmlSchemaAttribute> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaAttributeToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaComplexContentExtensionToXml(Document xmlDom, Node parentNode, XmlSchemaComplexContentExtension xmlSchemaComplexContentExtension) {
			schemaParticleToXml(xmlDom, parentNode, xmlSchemaComplexContentExtension.getParticle());
			
			XmlSchemaObjectCollection collection = xmlSchemaComplexContentExtension.getAttributes();
			Iterator<XmlSchemaAttribute> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaAttributeToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaContentModelToXml(Document xmlDom, Node parentNode, XmlSchemaContentModel xmlSchemaContentModel) {
			if (xmlSchemaContentModel instanceof XmlSchemaComplexContent) {
				schemaComplexContentToXml(xmlDom, parentNode, (XmlSchemaComplexContent)xmlSchemaContentModel);
			}
			else if (xmlSchemaContentModel instanceof XmlSchemaSimpleContent) {
				schemaSimpleContentToXml(xmlDom, parentNode, (XmlSchemaSimpleContent)xmlSchemaContentModel);
			}
		}
		
		private void schemaSimpleContentToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleContent xmlSchemaSimpleContent) {
			schemaContentToXml(xmlDom, parentNode, xmlSchemaSimpleContent.getContent());
		}

		private void schemaComplexContentToXml(Document xmlDom, Node parentNode, XmlSchemaComplexContent xmlSchemaComplexContent) {
			schemaContentToXml(xmlDom, parentNode, xmlSchemaComplexContent.getContent());
		}

		private void schemaGroupToXml(Document xmlDom, Node parentNode, XmlSchemaGroup xmlSchemaGroup) {
			schemaParticleToXml(xmlDom, parentNode, xmlSchemaGroup.getParticle());
		}

		private void schemaParticleToXml(Document xmlDom, Node parentNode, XmlSchemaParticle xmlSchemaParticle) {
			if (xmlSchemaParticle instanceof XmlSchemaAny) {
				;
			}
			else if (xmlSchemaParticle instanceof XmlSchemaElement) {
				schemaElementToXml(xmlDom, parentNode, (XmlSchemaElement)xmlSchemaParticle);
			}
			else if (xmlSchemaParticle instanceof XmlSchemaGroupBase) {
				schemaGroupBaseToXml(xmlDom, parentNode, (XmlSchemaGroupBase)xmlSchemaParticle);
			}
			else if (xmlSchemaParticle instanceof XmlSchemaGroupRef) {
				schemaGroupRefToXml(xmlDom, parentNode, (XmlSchemaGroupRef)xmlSchemaParticle);
			}
		}
		
		private Element createElement(Document xmlDom, Node parentNode, XmlSchemaElement xmlSchemaElement) {
			boolean outputElementWithNS = xmlgenDescription.isOutputElementWithNS();
			String doneAttr = xmlgenDescription.getDoneAttribute();
			
			Element element = null;
			
			QName xmlSchemaElementQName = xmlSchemaElement.getQName();
			if (!outputElementWithNS || (xmlSchemaElementQName == null))
				element = xmlDom.createElement(xmlSchemaElement.getName());
			else {
				String nsURI = xmlSchemaElementQName.getNamespaceURI();
				String prefix = loadedSchema.getNamespaceContext().getPrefix(nsURI);
				String name = xmlSchemaElement.getName();
				if (prefix == null) prefix = "";
				if (prefix.equals("")) prefix = loadedSchema.getNamespaceContext().getPrefix(loadedSchema.getTargetNamespace());
				if (prefix.equals("")) prefix = "target_ns";
				
				NodeList elements = xmlDom.getElementsByTagNameNS(nsURI, name);
				if (elements.getLength()>0) {
					Element elementNode = (Element)elements.item(0);
					boolean done = "true".equals(elementNode.getAttribute(doneAttr));
					if (done)
						((Element)parentNode).appendChild(elementNode.cloneNode(true));
					else {
						// This means there's a recursion on element: ignore
						((Element)parentNode).appendChild(xmlDom.createElement("recursion"));
					}
				}
				else {
					element = xmlDom.createElementNS(nsURI,prefix+":"+name);
				}
			}
			return element;
		}
		
		private void schemaElementToXml(Document xmlDom, Node parentNode, XmlSchemaElement xmlSchemaElement) {
			boolean outputOccurrences = xmlgenDescription.isOutputOccurences();
			boolean outputOccursAttribute = xmlgenDescription.isOutputOccursAttribute();
			boolean outputSchemaTypeCData = xmlgenDescription.isOutputSchemaTypeCData();
			boolean outputElementWithNS = xmlgenDescription.isOutputElementWithNS();
			String doneAttr = xmlgenDescription.getDoneAttribute();
			
			long maxOccurs = xmlSchemaElement.getMaxOccurs();
			long occurs = (outputOccurrences ? ((maxOccurs > 2) ? 2:maxOccurs):1);
			
			XmlSchemaType xmlSchemaType = xmlSchemaElement.getSchemaType();
			if (xmlSchemaType != null) {
				Element element = createElement(xmlDom, parentNode, xmlSchemaElement);
				if (element != null) {
					if (outputSchemaTypeCData) {
						Element schemaType = xmlDom.createElement("schema-type");
						String xsdSchema = xmlSchemaElement.toString("xsd", 3);
						if (xmlSchemaType.getName()== null) {
							xsdSchema = xsdSchema.replaceAll(" name=\"\"", "");
						}
						else {
							int index = xsdSchema.indexOf(">");
							if (index != -1) {
								xsdSchema = xsdSchema.substring(0, index) + "/>";
							}
						}

						NamespacePrefixList namespacePrefixList = loadedSchema.getNamespaceContext();
						String[] prefixes = namespacePrefixList.getDeclaredPrefixes();
						for (int i=0; i<prefixes.length; i++) {
							String prefix = prefixes[i];
							String ns = namespacePrefixList.getNamespaceURI(prefix);
							xsdSchema = xsdSchema.replaceAll("\\{"+ns+"\\}", prefix+":");
							xsdSchema = xsdSchema.replaceAll("\"minOccurs=", "\" minOccurs=");
						}
						
		                CDATASection cDATASection = xmlDom.createCDATASection(xsdSchema);
		                schemaType.appendChild(cDATASection);
		                element.appendChild(schemaType);
					}
	                
					if (outputOccursAttribute) element.setAttribute("occurs", ""+maxOccurs);
					if (outputElementWithNS) element.setAttribute(doneAttr, "false");// to avoid recursion
	                Node node = ((Element)parentNode).appendChild(element);
					schemaTypeToXml(xmlDom, element, xmlSchemaType);
					if (outputElementWithNS) element.setAttribute(doneAttr, "true");
					
					while (--occurs>0) {
						((Element)parentNode).appendChild(node.cloneNode(true));
					}
				}
			}
			else {
				// TODO (e.g. BnppInfoGreffe:Extrait)
				
				QName refName = xmlSchemaElement.getRefName();
				if (refName != null) {
					XmlSchemaElement xmlSchemaElementRef = loadedSchema.getElementByName(refName);
					schemaElementToXml(xmlDom, parentNode, xmlSchemaElementRef);
				}
				else {
					//Missing type or ref attribute OR type is xsd:anyType
					Element element = createElement(xmlDom, parentNode, xmlSchemaElement);
					if (element != null) {
						element.appendChild(xmlDom.createTextNode("any"));
						if (outputElementWithNS) element.setAttribute(doneAttr, "true");
		                Node node = ((Element)parentNode).appendChild(element);
						while (--occurs>0) {
							((Element)parentNode).appendChild(node.cloneNode(true));
						}
					}
				}
			}
		}

		private void schemaGroupBaseToXml(Document xmlDom, Node parentNode, XmlSchemaGroupBase xmlSchemaGroup) {
			boolean outputOccurrences = xmlgenDescription.isOutputOccurences();
			long maxOccurs = xmlSchemaGroup.getMaxOccurs();
			long occurs = (outputOccurrences ? ((maxOccurs > 2) ? 2:maxOccurs):1);
			
			if (xmlSchemaGroup instanceof XmlSchemaAll) {
				while (occurs-->0)
					schemaAllToXml(xmlDom, parentNode, (XmlSchemaAll)xmlSchemaGroup);
			}
			else if (xmlSchemaGroup instanceof XmlSchemaChoice) {
				while (occurs-->0)
					schemaChoiceToXml(xmlDom, parentNode, (XmlSchemaChoice)xmlSchemaGroup);
			}
			else if (xmlSchemaGroup instanceof XmlSchemaSequence) {
				while (occurs-->0)
					schemaSequenceToXml(xmlDom, parentNode, (XmlSchemaSequence)xmlSchemaGroup);
			}
		}
		
		private void schemaAllToXml(Document xmlDom, Node parentNode, XmlSchemaAll xmlSchemaAll) {
			XmlSchemaObjectCollection collection = xmlSchemaAll.getItems();
			Iterator<XmlSchemaObject> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaObjectToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaChoiceToXml(Document xmlDom, Node parentNode, XmlSchemaChoice xmlSchemaChoice) {
			XmlSchemaObjectCollection collection = xmlSchemaChoice.getItems();
			Iterator<XmlSchemaObject> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaObjectToXml(xmlDom, parentNode, it.next());
				//break;// TODO: to optimize for occurrence output in parent
			}
		}

		private void schemaSequenceToXml(Document xmlDom, Node parentNode, XmlSchemaSequence xmlSchemaSequence) {
			XmlSchemaObjectCollection collection = xmlSchemaSequence.getItems();
			Iterator<XmlSchemaObject> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaObjectToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaGroupRefToXml(Document xmlDom, Node parentNode, XmlSchemaGroupRef xmlSchemaGroupRef) {
			boolean outputOccurrences = xmlgenDescription.isOutputOccurences();
			long maxOccurs = xmlSchemaGroupRef.getMaxOccurs();
			long occurs = (outputOccurrences ? ((maxOccurs > 2) ? 2:maxOccurs):1);
			
			XmlSchemaGroup xmlSchemaGroup = (XmlSchemaGroup)loadedSchema.getGroups().getItem(xmlSchemaGroupRef.getRefName());
			while (occurs-->0)
				schemaGroupToXml(xmlDom, parentNode, xmlSchemaGroup);
		}
		
		private void schemaSimpleTypeContentToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleTypeContent xmlSchemaSimpleTypeContent) {
			if (xmlSchemaSimpleTypeContent instanceof XmlSchemaSimpleTypeList) {
				schemaSimpleTypeListToXml(xmlDom, parentNode, (XmlSchemaSimpleTypeList)xmlSchemaSimpleTypeContent);
			}
			else if (xmlSchemaSimpleTypeContent instanceof XmlSchemaSimpleTypeRestriction) {
				schemaSimpleTypeRestrictionToXml(xmlDom, parentNode, (XmlSchemaSimpleTypeRestriction)xmlSchemaSimpleTypeContent);
			}
			else if (xmlSchemaSimpleTypeContent instanceof XmlSchemaSimpleTypeUnion) {
				schemaSimpleTypeUnionToXml(xmlDom, parentNode, (XmlSchemaSimpleTypeUnion)xmlSchemaSimpleTypeContent);
			}
		}

		private void schemaSimpleTypeUnionToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleTypeUnion xmlSchemaSimpleTypeUnion) {
			XmlSchemaObjectCollection collection = xmlSchemaSimpleTypeUnion.getBaseTypes();
			Iterator<XmlSchemaSimpleType> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaSimpleTypeToXml(xmlDom, parentNode, it.next());
			}
		}

		private void schemaSimpleTypeRestrictionToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleTypeRestriction xmlSchemaSimpleTypeRestriction) {
			schemaSimpleTypeToXml(xmlDom, parentNode, xmlSchemaSimpleTypeRestriction.getBaseType());
		}

		private void schemaSimpleTypeListToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleTypeList xmlSchemaSimpleTypeList) {
			schemaSimpleTypeToXml(xmlDom, parentNode, xmlSchemaSimpleTypeList.getItemType());
		}

		private void schemaTypeToXml(Document xmlDom, Node parentNode, XmlSchemaType xmlSchemaType) {
			if (xmlSchemaType instanceof XmlSchemaSimpleType) {
				schemaSimpleTypeToXml(xmlDom, parentNode, (XmlSchemaSimpleType)xmlSchemaType);
			}
			else if (xmlSchemaType instanceof XmlSchemaComplexType) {
				schemaComplexTypeToXml(xmlDom, parentNode, (XmlSchemaComplexType)xmlSchemaType);
			}
		}
		
		private void schemaSimpleTypeToXml(Document xmlDom, Node parentNode, XmlSchemaSimpleType xmlSchemaSimpleType) {
			String value = xmlSchemaSimpleType.getName(); //"value";
			if (value == null) {
				value = "";
			}
			
			XmlSchemaDatatype dataType = xmlSchemaSimpleType.getDataType();
			if (dataType != null) {
				Class<?> c = dataType.valueType();
				if (c.isAssignableFrom(Number.class)) {
					value = "10";
				}
			}
			
			if (parentNode instanceof Element) {
				parentNode.appendChild(xmlDom.createTextNode(value));
			}
			else if (parentNode instanceof Attr) {
				((Attr)parentNode).setNodeValue(value);
			}
		}
		
		private void schemaComplexTypeToXml(Document xmlDom, Node parentNode, XmlSchemaComplexType xmlSchemaComplexType) {
			schemaParticleToXml(xmlDom, parentNode, xmlSchemaComplexType.getParticle());
			
			XmlSchemaObjectCollection collection = xmlSchemaComplexType.getAttributes();
			Iterator<XmlSchemaAttribute> it = GenericUtils.cast(collection.getIterator());
			while (it.hasNext()) {
				schemaAttributeToXml(xmlDom, parentNode, it.next());
			}
		}
		
		private void addSchemaObjects(XmlSchema typesSchema) {
			if (typesSchema != null) {
				XmlSchemaObjectCollection items = typesSchema.getItems();
				Iterator<XmlSchemaObject> it = GenericUtils.cast(items.getIterator());
				while (it.hasNext()) {
					XmlSchemaObject object = it.next();
					addSchemaObject(object);
				}
			}
		}
		
		private void addSchemaObject(XmlSchemaObject object) {
			if (object instanceof XmlSchemaType) {
	            loadedSchema.getItems().add(object);
	            loadedSchema.getSchemaTypes().add(((XmlSchemaType)object).getQName(), object);
	            bModified = true;
			}
			else if (object instanceof XmlSchemaElement) {
	            loadedSchema.getItems().add(object);
	            loadedSchema.getElements().add(((XmlSchemaElement)object).getQName(), object);
	            bModified = true;
			}
			else if (object instanceof XmlSchemaGroup) {
	            loadedSchema.getItems().add(object);
	            loadedSchema.getGroups().add(((XmlSchemaGroup)object).getName(), object);
	            bModified = true;
			}
			else if (object instanceof XmlSchemaAttribute) {
	            loadedSchema.getItems().add(object);
	            loadedSchema.getAttributes().add(((XmlSchemaAttribute)object).getQName(), object);
	            bModified = true;
			}
			else {
				
			}
		}
		
		private void removeSchemaObjects(XmlSchema typesSchema) {
			if (typesSchema != null) {
				XmlSchemaObjectCollection items = typesSchema.getItems();
				Iterator<XmlSchemaObject> it = GenericUtils.cast(items.getIterator());
				while (it.hasNext()) {
					XmlSchemaObject object = (XmlSchemaObject)it.next();
					removeSchemaObject(object);
				}
			}
		}
		
		private void removeSchemaObject(XmlSchemaObject object) {
			if (object == null)
				return;
			
			for (int i=0;i<loadedSchema.getItems().getCount(); i++) {
				XmlSchemaObject ob = loadedSchema.getItems().getItem(i);
				if ((ob instanceof XmlSchemaType) && (object instanceof XmlSchemaType)) {
					if (((XmlSchemaType)ob).getQName().toString().equals(((XmlSchemaType)object).getQName().toString())) {
						loadedSchema.getItems().removeAt(i);
						bModified = true;
						return;
					}
				}
				else if ((ob instanceof XmlSchemaElement) && (object instanceof XmlSchemaElement)) {
					if (((XmlSchemaElement)ob).getQName().toString().equals(((XmlSchemaElement)object).getQName().toString())) {
						loadedSchema.getItems().removeAt(i);
						bModified = true;
						return;
					}
				}
				else if ((ob instanceof XmlSchemaGroup) && (object instanceof XmlSchemaGroup)) {
					if (((XmlSchemaGroup)ob).getName().toString().equals(((XmlSchemaGroup)object).getName().toString())) {
						loadedSchema.getItems().removeAt(i);
						bModified = true;
						return;
					}
				}
				else if ((ob instanceof XmlSchemaAttribute) && (object instanceof XmlSchemaAttribute)) {
					if (((XmlSchemaAttribute)ob).getQName().toString().equals(((XmlSchemaAttribute)object).getQName().toString())) {
						loadedSchema.getItems().removeAt(i);
						bModified = true;
						return;
					}
				}
				else {
					
				}
			}
		}
		
		private void removeSchemaObjectsExcept(List<QName> qnames) {
			if (qnames == null)
				return;
			
			String message = "\nqnames: "+qnames.toString();
			boolean bFound = false;
			QName qname;
			
			do {
				bFound = false;
				for (int i=0;i<loadedSchema.getItems().getCount(); i++) {
					XmlSchemaObject ob = loadedSchema.getItems().getItem(i);
					if (ob instanceof XmlSchemaType) {
						qname = ((XmlSchemaType)ob).getQName();
						if (!qnames.contains(qname)) {
							message += "\nremove at "+i+": "+qname.toString();
							loadedSchema.getItems().removeAt(i);
							bFound = true;
							break;
						}
						else message += "\nkeep at "+i+": "+qname.toString();
					}
					else if (ob instanceof XmlSchemaElement) {
						qname = ((XmlSchemaElement)ob).getQName();
						if (!qnames.contains(qname)) {
							message += "\nremove at "+i+": "+qname.toString();
							loadedSchema.getItems().removeAt(i);
							bFound = true;
							break;
						}
						else message += "\nkeep at "+i+": "+qname.toString();
					}
					else if (ob instanceof XmlSchemaGroup) {
						qname = ((XmlSchemaGroup)ob).getName();
						if (!qnames.contains(qname)) {
							message += "\nremove at "+i+": "+qname.toString();
							loadedSchema.getItems().removeAt(i);
							bFound = true;
							break;
						}
						else message += "\nkeep at "+i+": "+qname.toString();
					}
					else if (ob instanceof XmlSchemaAttribute) {
						qname = ((XmlSchemaAttribute)ob).getQName();
						if (!qnames.contains(qname)) {
							message += "\nremove at "+i+": "+qname.toString();
							loadedSchema.getItems().removeAt(i);
							bFound = true;
							break;
						}
						else message += "\nkeep at "+i+": "+qname.toString();
					}
					else {
						message += "\nfound at "+i+": "+ob.toString();
					}
				}
			} while (bFound);
			
			//System.out.println(message);
			//loadedSchema.write(System.out, options);
		}
		
		private Vector<QuickSortItem> getTypes(XmlSchema schema, String prefix) {
			Vector<QuickSortItem> v = new Vector<QuickSortItem>();
			
			XmlSchemaObjectTable types = schema.getSchemaTypes();
			Iterator<QName> itt = GenericUtils.cast(types.getNames());
			while (itt.hasNext()) {
				QName qname = itt.next();
				String qname_localPart = qname.getLocalPart();
				if ((prefix == null) || prefix.equals("")) prefix = qname.getPrefix();
				if ((prefix == null) || prefix.equals("")) prefix = "xsd";
				v = addType(v, prefix + ":" + qname_localPart);
			}
			
			return v;
		}
		
		private Vector<QuickSortItem> addType(Vector<QuickSortItem> v, String type) {
	        v.addElement(new QuickSortItem(type){
	            public Object getQuickSortValue() throws Exception {
	                return toString();
	            }
	            public String toString() {
	                return ((String) object);
	            }
	        });
	        return v;
		}
		
		private void create(String projectName, String xsdURI) throws XSDException{
			try {
				this.xsdURI = xsdURI;
				this.baseURI = xsdURI.substring(0, xsdURI.lastIndexOf("/")+1);
				
				String targetNamespace = Project.getProjectTargetNamespace(projectName);
				Map<String, String> nameSpacesMap = GenericUtils.cast(new NamespaceMap());
				
				schemaCol = new XmlSchemaCollection();
				loadedSchema = new XmlSchema(targetNamespace, schemaCol);
				loadedSchema.setAttributeFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));
				loadedSchema.setElementFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));

		        nameSpacesMap.put("xsd", Constants.URI_2001_SCHEMA_XSD);
		        nameSpacesMap.put(projectName + "_ns", targetNamespace);
		        loadedSchema.setNamespaceContext((NamespaceMap) nameSpacesMap);
				
				XmlSchema errorSchema = toXmlSchema(Engine.getExceptionSchema(), projectName);
				if (errorSchema != null) {
					XmlSchemaObjectTable errorTypes = errorSchema.getSchemaTypes();
					Iterator<QName> it = GenericUtils.cast(errorTypes.getNames());
					while (it.hasNext()) {
						QName qname = it.next();
						XmlSchemaObject object = errorTypes.getItem(qname);
						if (object instanceof XmlSchemaType) {
				            loadedSchema.getItems().add(object);
				            loadedSchema.getSchemaTypes().add(((XmlSchemaType)object).getQName(), object);
						}
					}
				}

				OutputStream os = new FileOutputStream(xsdURI);
				loadedSchema.write(os, options);
				if (os != null) os.close();
			}
			catch (Exception e) {
				throw new XSDException("Unable to create XSD", e);
			}
		}
		
		private XmlSchema toXmlSchema(String s, String projectName) throws XSDException {
			InputStream is = null;
			try {
				// Retrieve input types
				StringEx sx = new StringEx(s);
				sx.replaceAll("\"p_ns:", "\""+ projectName + "_ns:");
				String sTypes = sx.toString();
				
				// Create a new XmlSchema
				String targetNamespace = Project.getProjectTargetNamespace(projectName);
				XmlSchemaCollection newSchemaCol = new XmlSchemaCollection();
				newSchemaCol.setBaseUri(baseURI);
				XmlSchema newSchema = new XmlSchema(targetNamespace, newSchemaCol);
				newSchema.setAttributeFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));
				newSchema.setElementFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));
				newSchema.setNamespaceContext(loadedSchema.getNamespaceContext());

				Iterator<XmlSchemaObject> it = GenericUtils.cast(loadedSchema.getItems().getIterator());
				while (it.hasNext()) {
					XmlSchemaObject ob = it.next();
					if (ob instanceof XmlSchemaImport) {
						newSchema.getItems().add((XmlSchemaImport)ob);
					}
				}
				
				// Write schema to string and add types to it
				StringWriter sw = new StringWriter();
				newSchema.write(sw, options);
				if (sw != null) sw.close();
				
				String schema = sw.getBuffer().toString();
				int index = schema.indexOf("</xsd:schema>");
				if (index != -1) {// schema with imports
					schema = schema.substring(0,index) + sTypes + "</xsd:schema>";
				}
				else {// schema with no imports
					index = schema.indexOf("/>");
					if (index != -1) {
						schema = schema.substring(0,index) + ">" + sTypes + "</xsd:schema>";
					}
				}
				//System.out.println("\nschema:\n"+ schema);
				
				// Finally load schema
				XmlSchema xmlSchema = null;
	            is = new ByteArrayInputStream(schema.getBytes("UTF-8"));
	            XmlSchemaCollection xmlSchemaCollection = new XmlSchemaCollection();
	            xmlSchemaCollection.setBaseUri(baseURI);
	            xmlSchemaCollection.setSchemaResolver(new XSDURIResolver());
	            //xmlSchema = xmlSchemaCollection.read(new StreamSource(is), null);
	            xmlSchema = xmlSchemaCollection.read(parseDOM(is), null);
				
	            //System.out.println("\ntoXmlSchema:");
				//if (xmlSchema != null) xmlSchema.write(System.out, options);
	            
				return xmlSchema;
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
				throw new XSDException("Error while parsing source string",e);
			}
			finally {
				try {
					if (is != null)
						is.close();
				}
				catch (IOException e) {}
			}
		}
		
		private Document createXmlDom() throws ParserConfigurationException {
			Document dom = XMLUtils.createDom("java");
			Element root = dom.createElement("document");
			dom.appendChild(root);
			return dom;
		}

	}
}
