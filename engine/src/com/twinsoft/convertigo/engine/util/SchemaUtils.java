/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SchemaUtils {
	
	private static ThreadLocal<DocumentBuilderFactory> defaultDocumentBuilderFactory = new ThreadLocal<DocumentBuilderFactory>() {
		@Override
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
		@Override
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

	public static XmlSchema loadSchema(String xsdFilePath) throws SAXException, IOException {
		File xsdFile = new File(xsdFilePath);
		XmlSchemaCollection collection = new XmlSchemaCollection();
		collection.setBaseUri(xsdFilePath);
		return loadSchema(xsdFile, collection);
	}

	public static XmlSchema loadSchema(File xsdFile, XmlSchemaCollection xmlSchemaCollection) throws SAXException, IOException {
		if (xsdFile != null && xsdFile.exists() && xsdFile.isFile()) {
			return loadSchema(xsdFile.toURI().toURL(), xmlSchemaCollection);
		}
		return null;
	}
	
	@SuppressWarnings("unused")
	public static XmlSchema loadSchema(URL xsdUrl, XmlSchemaCollection xmlSchemaCollection) throws SAXException, IOException {
		long timeStart = System.currentTimeMillis();
		try {
			if (xsdUrl != null) {
				// build document from url
				Document xsdDocument = getDefaultDocumentBuilder().parse(xsdUrl.toString());
				
				// add soap-encoding import if needed (for validation)
				addSoapEncSchemaImport(xsdDocument.getDocumentElement());
	
				XmlSchema xmlSchema = xmlSchemaCollection.read(xsdDocument, xsdUrl.toString(), null);
				return xmlSchema;
				
			}
			return null;
		}
		finally {
			long timeStop = System.currentTimeMillis();
//			System.out.println("Schema for \"" + xsdUrl.toString() + "\" | Times >> total : " + (timeStop - timeStart) + " ms");
		}
	}
	
	public static Element getSchemaImport(Element schemaElement, String namespace) {
		if (schemaElement != null) {
			NodeList imports = schemaElement.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD, "import");
			for (int i=0; i<imports.getLength(); i++) {
				Element importElement = (Element)imports.item(i);
				if (namespace.equals((importElement).getAttribute("namespace"))) {
					return importElement;
				}
			}
		}
		return null;
	}
	
	public static void addSchemaImport(Element schemaElement, String namespace, String location) {
		if (schemaElement != null) {
			Element importElement = getSchemaImport(schemaElement, namespace);
			if (importElement == null) {
				Element imp = schemaElement.getOwnerDocument().createElementNS(Constants.URI_2001_SCHEMA_XSD, "import");
				imp.setAttribute("namespace", namespace);
				imp.setAttribute("schemaLocation", location);
				schemaElement.insertBefore(imp, schemaElement.getFirstChild());
			}
		}
	}
	
	public final static String URI_SOAP_ENC = "http://schemas.xmlsoap.org/soap/encoding/";
	
	public static void addSoapEncSchemaImport(Element schemaElement) {
		if (useSoapEncArrayType(schemaElement)) {
			addSchemaImport(schemaElement, URI_SOAP_ENC, URI_SOAP_ENC);
		}
		else {
			Element importElement = getSchemaImport(schemaElement, URI_SOAP_ENC);
			if (importElement != null) {
				importElement.setAttribute("namespace", URI_SOAP_ENC);
				importElement.setAttribute("schemaLocation", URI_SOAP_ENC);
			}
		}
	}
	
	public static boolean useSoapEncArrayType(Element schemaElement) {
		if (schemaElement != null) {
			NodeList attributes = schemaElement.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD, "attribute");
			for (int i=0; i<attributes.getLength(); i++) {
				Element attribute = (Element)attributes.item(i);
				NamedNodeMap map = attribute.getAttributes();
				for (int j = 0; j < map.getLength() ; j++) {
					if (map.item(j).getNodeName().endsWith("arrayType")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static XmlSchema loadSchema(String sDocument, XmlSchemaCollection xmlSchemaCollection) throws SAXException, IOException {
		if (sDocument != null) {
			Document xsdDocument = getDefaultDocumentBuilder().parse(new InputSource(new StringReader(sDocument)));
			XmlSchema xmlSchema = xmlSchemaCollection.read(xsdDocument, null);
			return xmlSchema;
		}
		return null;
	}
	
	public static XmlSchemaCollection createSchemaCollection() {
		NamespaceMap nsMap = new NamespaceMap();
		nsMap.add("xsd", Constants.URI_2001_SCHEMA_XSD);
		return createSchemaCollection(nsMap);
	}
	
	public static XmlSchemaCollection createSchemaCollection(NamespaceMap nsMap) {
		XmlSchemaCollection xmlSchemaCollection = new XmlSchemaCollection();
		xmlSchemaCollection.setNamespaceContext(nsMap);
		return xmlSchemaCollection;
	}

	public static XmlSchema createSchema(String prefix, String targetNamespace, String elementFormDefault, String attributeFormDefault) {
		NamespaceMap nsMap = new NamespaceMap();
		nsMap.add("xsd", Constants.URI_2001_SCHEMA_XSD);
		nsMap.add(prefix, targetNamespace);
		
		XmlSchemaCollection xmlSchemaCollection = createSchemaCollection(nsMap);
		
		XmlSchema xmlSchema = new XmlSchema(targetNamespace, xmlSchemaCollection);
		xmlSchema.setNamespaceContext(nsMap);
		xmlSchema.setElementFormDefault(new XmlSchemaForm(elementFormDefault));
		xmlSchema.setAttributeFormDefault(new XmlSchemaForm(attributeFormDefault));
		return xmlSchema;
	}
	
	public static void saveSchema(String xsdFilePath, XmlSchema xmlSchema) throws IOException {
		HashMap<String,String> options = new HashMap<String,String>();
		options.put(OutputKeys.METHOD, "xml");
		options.put(OutputKeys.ENCODING, "UTF-8");
		options.put(OutputKeys.INDENT, "yes");
		options.put("{http://xml.apache.org/xslt}indent-amount", "4");
		options.put(OutputKeys.OMIT_XML_DECLARATION, "no");
		options.put(OutputKeys.CDATA_SECTION_ELEMENTS, "{"+Constants.URI_2001_SCHEMA_XSD+"}documentation");
		
		saveSchema(xsdFilePath, xmlSchema, options);
	}
	
	private static void saveSchema(String xsdFilePath, XmlSchema xmlSchema, HashMap<String,String> options) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(xsdFilePath);
			xmlSchema.write(fos, options);
		}
		finally {
			if (fos != null) fos.close();
		}
	}
	
	public static NamespaceMap getNamespaceMap(XmlSchema xmlSchema) {
		NamespaceMap namespacesMap = new NamespaceMap();
		NamespacePrefixList namespacePrefixList = xmlSchema.getNamespaceContext();
		if (namespacePrefixList != null) {
			String[] prefixes = namespacePrefixList.getDeclaredPrefixes();
			for (int i=0; i<prefixes.length; i++) {
				String prefix = prefixes[i];
				String ns = namespacePrefixList.getNamespaceURI(prefix);
				namespacesMap.add(prefix, ns);
			}
		}
		return namespacesMap;
	}
}
