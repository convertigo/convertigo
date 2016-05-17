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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.twinsoft.convertigo.beans.common.XMLizable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.util.StringEx;

public class XMLUtils {
	private static ThreadLocal<DocumentBuilderFactory> defaultDocumentBuilderFactory = new ThreadLocal<DocumentBuilderFactory>() {
		@Override
		protected DocumentBuilderFactory initialValue() {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			try {
				String s = EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_NAMESPACE_AWARE);
				if (s.equalsIgnoreCase("true"))
					documentBuilderFactory.setNamespaceAware(true);
			}
			catch (IllegalStateException e) {
				documentBuilderFactory.setNamespaceAware(true);
			}
			return documentBuilderFactory;
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
	
	private static ThreadLocal<TransformerFactory> defaultTransformerFactory = new ThreadLocal<TransformerFactory>() {
		@Override
		protected TransformerFactory initialValue() {
			return TransformerFactory.newInstance();
		}
	};

	public static Transformer getNewTransformer() throws TransformerConfigurationException {
		return defaultTransformerFactory.get().newTransformer();
	}
	
	public static Transformer getNewTransformer(Source source) throws TransformerConfigurationException {
		return defaultTransformerFactory.get().newTransformer(source);
	}
	
	private static ThreadLocal<SAXParser> defaultSAXParser = new ThreadLocal<SAXParser>() {
		@Override
		protected SAXParser initialValue() {
			try {
				return SAXParserFactory.newInstance().newSAXParser();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	};
	
	public static void saxParse(File file, DefaultHandler defaultHandler) throws SAXException, IOException {
		defaultSAXParser.get().parse(file, defaultHandler);
	}
	
	public static void saxParse(InputStream inputStream, DefaultHandler defaultHandler) throws SAXException, IOException {
		defaultSAXParser.get().parse(inputStream, defaultHandler);
	}
	
	public static String simplePrettyPrintDOM(String sDocument) {
		StringEx sxDocument = new StringEx(sDocument);
		sxDocument.replaceAll(">", ">\n");
		return sxDocument.toString();
	}

	public static String prettyPrintDOM(String sDocument) throws ParserConfigurationException, SAXException,
			IOException {
		Document document = getDefaultDocumentBuilder().parse(new InputSource(new StringReader(sDocument)));
		return XMLUtils.prettyPrintDOM(document);
	}

	public static String prettyPrintDOM(String sDocument, String relativeUriResolver)
			throws ParserConfigurationException, SAXException, IOException {
		InputSource inputSource = new InputSource(new StringReader(sDocument));
		inputSource.setSystemId(relativeUriResolver);
		Document document = getDefaultDocumentBuilder().parse(inputSource);
		return XMLUtils.prettyPrintDOM(document);
	}

	public static String prettyPrintDOM(Node node) {
		return XMLUtils.prettyPrintDOM(node.getOwnerDocument());
	}

	public static String prettyPrintDOMWithEncoding(Document doc) {
		return prettyPrintDOMWithEncoding(doc, "ISO-8859-1");
	}

	public static String prettyPrintDOMWithEncoding(Document doc, String defaultEncoding) {
		StringWriter writer = new StringWriter();
		prettyPrintDOMWithEncoding(doc, defaultEncoding, writer);
		return writer.getBuffer().toString();
	}

	public static String prettyPrintDOMWithEncoding(String sDocument, String encoding)
			throws ParserConfigurationException, SAXException, IOException {
		Document document = getDefaultDocumentBuilder().parse(new InputSource(new StringReader(sDocument)));
		return XMLUtils.prettyPrintDOMWithEncoding(document, encoding);
	}

	public static void prettyPrintDOM(Document doc, Writer writer) {
		prettyPrintDOMWithEncoding(doc, "UTF-8", writer);
	}

	public static void prettyPrintDOM(Document doc, String defaultEncoding, Writer writer) {
		prettyPrintDOMWithEncoding(doc, defaultEncoding, writer);
	}
	
	public static void prettyPrintDOMWithEncoding(Document doc, String defaultEncoding, Writer writer) {
		prettyPrintDOMWithEncoding(doc, defaultEncoding, new StreamResult(writer));
	}
	
	public static void prettyPrintDOMWithEncoding(Document doc, String defaultEncoding, OutputStream outputStream) {
		prettyPrintDOMWithEncoding(doc, defaultEncoding, new StreamResult(outputStream));
	}
	
	public static void prettyPrintDOMWithEncoding(Document doc, String defaultEncoding, Result result) {
		Node firstChild = doc.getFirstChild();
		boolean omitXMLDeclaration = false;
		String encoding = defaultEncoding; // default Encoding char set if non
											// is found in the PI

		if ((firstChild.getNodeType() == Document.PROCESSING_INSTRUCTION_NODE)
				&& (firstChild.getNodeName().equals("xml"))) {
			omitXMLDeclaration = true;
			String piValue = firstChild.getNodeValue();
			// extract from PI the encoding Char Set
			int encodingOffset = piValue.indexOf("encoding=\"");
			if (encodingOffset != -1) {
				encoding = piValue.substring(encodingOffset + 10);
				// remove the last "
				encoding = encoding.substring(0, encoding.length() - 1);
			}
		}

		try {
			Transformer t = getNewTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, encoding);
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty(OutputKeys.METHOD, "xml"); // xml, html, text
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration ? "yes" : "no");
			t.transform(new DOMSource(doc), result);
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception while pretty print DOM", e);
		}
	}

	public static String prettyPrintDOM(Document doc) {
		return prettyPrintDOMWithEncoding(doc, "ISO-8859-1");
	}
	
	public static String prettyPrintElement(Element elt) {
		return prettyPrintElement(elt, true, true);
	}
	
	public static String prettyPrintElement(Element elt, boolean omitXMLDeclaration, boolean bIndent) {
		Node firstChild = elt;
		String encoding = "ISO-8859-1"; // default Encoding char set if non is
										// found in the PI

		if (omitXMLDeclaration && (firstChild.getNodeType() == Document.PROCESSING_INSTRUCTION_NODE)
				&& (firstChild.getNodeName().equals("xml"))) {
			String piValue = firstChild.getNodeValue();
			// extract from PI the encoding Char Set
			int encodingOffset = piValue.indexOf("encoding=\"");
			if (encodingOffset != -1) {
				encoding = piValue.substring(encodingOffset + 10);
				// remove the last "
				encoding = encoding.substring(0, encoding.length() - 1);
			}
		}
		StringWriter strWtr = new StringWriter();
		try {
			Transformer t = getNewTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, encoding);
			t.setOutputProperty(OutputKeys.INDENT, bIndent ? "yes" : "no");
			t.setOutputProperty(OutputKeys.METHOD, "xml"); // xml, html, text
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration ? "yes" : "no");
			t.transform(new DOMSource(elt), new StreamResult(strWtr));
			return strWtr.getBuffer().toString();
		} catch (Exception e) {
			System.err.println("XML.toString(Document): " + e);
			Engine.logEngine.error("Unexpected exception", e);
			return e.getMessage();
		}
	}

	/*
	 * public static String prettyPrintElement(Element elt) { Node firstChild =
	 * elt; boolean omitXMLDeclaration = false; if ( (firstChild.getNodeType()
	 * == Document.PROCESSING_INSTRUCTION_NODE) &&
	 * (firstChild.getNodeName().equals("xml"))) omitXMLDeclaration = true;
	 * 
	 * OutputFormat format = new OutputFormat("XML", "ISO-8859-1", true);
	 * format.setLineWidth(0); format.setIndent(4);
	 * 
	 * format.setNonEscapingElements(new String[] {"SCRIPT", "script"});
	 * 
	 * format.setOmitDocumentType(true);
	 * format.setOmitXMLDeclaration(omitXMLDeclaration); StringWriter sw = new
	 * StringWriter(); XMLSerializer serializer = new XMLSerializer(sw, format);
	 * 
	 * try { serializer.serialize(elt); return sw.toString(); }
	 * catch(IOException e) { Engine.logEngine.error("Unexpected exception", e);
	 * return e.getMessage(); } }
	 * 
	 * public static String prettyPrintElement(Element elt, boolean
	 * omitXMLDeclaration, int indent) { OutputFormat format = new
	 * OutputFormat("XML", "ISO-8859-1", true); format.setLineWidth(0);
	 * format.setIndent(indent);
	 * 
	 * format.setNonEscapingElements(new String[] {"SCRIPT", "script"});
	 * 
	 * format.setOmitDocumentType(true);
	 * format.setOmitXMLDeclaration(omitXMLDeclaration); StringWriter sw = new
	 * StringWriter(); XMLSerializer serializer = new XMLSerializer(sw, format);
	 * 
	 * try { serializer.serialize(elt); return sw.toString(); }
	 * catch(IOException e) { Engine.logEngine.error("Unexpected exception", e);
	 * return e.getMessage(); } }
	 */
	public static Node writeObjectToXml(Document document, Object object) throws Exception {
		return writeObjectToXml(document, object, null);
	}

	public static Node writeObjectToXml(Document document, Object object, Object compiledValue)
			throws Exception {
		if (object == null)
			return null;
		
		if (object instanceof Enum) {
			object = ((Enum<?>) object).name();
		}
		
		// Simple objects
		if ((object instanceof Boolean) || (object instanceof Integer) || (object instanceof Double)
				|| (object instanceof Float) || (object instanceof Character) || (object instanceof Long)
				|| (object instanceof Short) || (object instanceof Byte)) {
			Element element = document.createElement(object.getClass().getName());
			element.setAttribute("value", object.toString());
			if (compiledValue != null)
				element.setAttribute("compiledValue", compiledValue.toString());
			return element;
		}
		// Strings
		else if (object instanceof String) {
			Element element = document.createElement(object.getClass().getName());
			element.setAttribute("value", object.toString());
			if (compiledValue != null) {
				element.setAttribute("compiledValueClass", compiledValue.getClass().getCanonicalName());
				element.setAttribute("compiledValue", compiledValue.toString());
			}
			return element;
		}
		// Arrays
		else if (object.getClass().getName().startsWith("[")) {
			String arrayClassName = object.getClass().getName();
			int i = arrayClassName.lastIndexOf('[');

			String itemClassName = arrayClassName.substring(i + 1);
			char c = itemClassName.charAt(itemClassName.length() - 1);
			switch (c) {
			case ';':
				itemClassName = itemClassName.substring(1, itemClassName.length() - 1);
				break;
			case 'B':
				itemClassName = "byte";
				break;
			case 'C':
				itemClassName = "char";
				break;
			case 'D':
				itemClassName = "double";
				break;
			case 'F':
				itemClassName = "float";
				break;
			case 'I':
				itemClassName = "int";
				break;
			case 'J':
				itemClassName = "long";
				break;
			case 'S':
				itemClassName = "short";
				break;
			case 'Z':
				itemClassName = "boolean";
				break;
			}

			Element element = document.createElement("array");
			element.setAttribute("classname", itemClassName);

			int len = Array.getLength(object);
			element.setAttribute("length", Integer.toString(len));

			Node subNode;
			for (int k = 0; k < len; k++) {
				subNode = writeObjectToXml(document, Array.get(object, k));
				if (subNode != null) {
					element.appendChild(subNode);
				}
			}

			return element;
		}
		// XMLization
		else if (object instanceof XMLizable) {
			Element element = document.createElement("xmlizable");
			element.setAttribute("classname", object.getClass().getName());
			element.appendChild(((XMLizable) object).writeXml(document));
			return element;
		}
		// Default serialization
		else {
			Element element = document.createElement("serializable");
			element.setAttribute("classname", object.getClass().getName());

			String objectBytesEncoded = null;
			byte[] objectBytes = null;

			// We write the object to a bytes array
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
			objectOutputStream.writeObject(object);
			objectOutputStream.flush();
			outputStream.close();

			// Now, we retrieve the object bytes
			objectBytes = outputStream.toByteArray();
			objectBytesEncoded = org.apache.commons.codec.binary.Base64.encodeBase64String(objectBytes);

			CDATASection cDATASection = document.createCDATASection(new String(objectBytesEncoded));
			element.appendChild(cDATASection);

			return element;
		}
	}

	public static Object readObjectFromXml(Element node) throws Exception {
		String nodeName = node.getNodeName();
		String nodeValue = ((Element) node).getAttribute("value");

		if (nodeName.equals("java.lang.Boolean")) {
			return new Boolean(nodeValue);
		} else if (nodeName.equals("java.lang.Byte")) {
			return new Byte(nodeValue);
		} else if (nodeName.equals("java.lang.Character")) {
			return new Character(nodeValue.charAt(0));
		} else if (nodeName.equals("java.lang.Integer")) {
			return new Integer(nodeValue);
		} else if (nodeName.equals("java.lang.Double")) {
			return new Double(nodeValue);
		} else if (nodeName.equals("java.lang.Float")) {
			return new Float(nodeValue);
		} else if (nodeName.equals("java.lang.Long")) {
			return new Long(nodeValue);
		} else if (nodeName.equals("java.lang.Short")) {
			return new Short(nodeValue);
		} else if (nodeName.equals("java.lang.String")) {
			return nodeValue;
		} else if (nodeName.equals("array")) {
			String className = node.getAttribute("classname");
			String length = node.getAttribute("length");
			int len = (new Integer(length)).intValue();

			Object array;
			if (className.equals("byte")) {
				array = new byte[len];
			} else if (className.equals("boolean")) {
				array = new boolean[len];
			} else if (className.equals("char")) {
				array = new char[len];
			} else if (className.equals("double")) {
				array = new double[len];
			} else if (className.equals("float")) {
				array = new float[len];
			} else if (className.equals("int")) {
				array = new int[len];
			} else if (className.equals("long")) {
				array = new long[len];
			} else if (className.equals("short")) {
				array = new short[len];
			} else {
				array = Array.newInstance(Class.forName(className), len);
			}

			Node xmlNode = null;
			NodeList nl = node.getChildNodes();
			int len_nl = nl.getLength();
			int i = 0;
			for (int j = 0; j < len_nl; j++) {
				xmlNode = nl.item(j);
				if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
					Object o = XMLUtils.readObjectFromXml((Element) xmlNode);
					Array.set(array, i, o);
					i++;
				}
			}

			return array;
		}
		// XMLization
		else if (nodeName.equals("xmlizable")) {
			String className = node.getAttribute("classname");

			Node xmlNode = findChildNode(node, Node.ELEMENT_NODE);
			Object xmlizable = Class.forName(className).newInstance();
			((XMLizable) xmlizable).readXml(xmlNode);

			return xmlizable;
		}
		// Serialization
		else if (nodeName.equals("serializable")) {
			Node cdata = findChildNode(node, Node.CDATA_SECTION_NODE);
			String objectSerializationData = cdata.getNodeValue();
			Engine.logEngine.debug("Object serialization data:\n" + objectSerializationData);
			byte[] objectBytes = org.apache.commons.codec.binary.Base64.decodeBase64(objectSerializationData);

			// We read the object to a bytes array
			ByteArrayInputStream inputStream = new ByteArrayInputStream(objectBytes);
			ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
			Object object = objectInputStream.readObject();
			inputStream.close();

			return object;
		}

		return null;
	}

	public static Node findChildNode(Node parentNode, int type) {
		Node foundNode = null;
		NodeList nl = parentNode.getChildNodes();
		int len_nl = nl.getLength();
		for (int j = 0; j < len_nl; j++) {
			foundNode = nl.item(j);
			if (foundNode.getNodeType() == type)
				return foundNode;
		}

		return null;
	}

	public static Element findNodeByAttributeValue(NodeList nodeList, String attributeName, String attributeValue) {
		int len = nodeList.getLength();
		String tmp;
		Element property;
		for (int i = 0; i < len; i++) {
			property = (Element) nodeList.item(i);
			tmp = property.getAttribute(attributeName);
			if (attributeValue.equals(tmp)) {
				return property;
			}
		}
		return null;
	}

	public static Object findPropertyValue(Element databaseObjectNode, String propertyName) throws Exception {
		NodeList properties = databaseObjectNode.getElementsByTagName("property");

		Element projectNameElement = XMLUtils.findNodeByAttributeValue(properties, "name", propertyName);

		Node xmlNode = null;
		NodeList nl = projectNameElement.getChildNodes();
		int len_nl = nl.getLength();
		for (int j = 0; j < len_nl; j++) {
			xmlNode = nl.item(j);
			if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
				return XMLUtils.readObjectFromXml((Element) xmlNode);
			}
		}

		throw new EngineException("No such property ('" + propertyName + "')");
	}

	public static String findXslHref(Node node) {
		int type = node.getNodeType();

		if (type == Node.PROCESSING_INSTRUCTION_NODE) {
			String nodeName = node.getNodeName();
			if (nodeName.equalsIgnoreCase("xml-stylesheet")) {
				String nodeValue = node.getNodeValue();
				try {
					int i = nodeValue.indexOf("href=\"");
					int j = nodeValue.indexOf("\"", i + 6);
					String result = nodeValue.substring(i + 6, j);
					return result;
				} catch (StringIndexOutOfBoundsException e) {
					return null;
				}
			}
			return null;
		} else {
			NodeList children = node.getChildNodes();
			int len = children.getLength();
			for (int i = 0; i < len; i++) {
				String result = findXslHref(children.item(i));
				if (result != null)
					return result;
			}
		}
		return null;
	}

	public static void copyDocument(Document from, Document to) {
		Node node = to.importNode(from.getDocumentElement(), true);
		to.getDocumentElement().appendChild(node);
	}

	public static Element findSingleElement(Element parent, String fullXPath) {
		Element elt = null;
		if (parent != null) {
			if (parent.hasChildNodes()) {
				if (fullXPath.startsWith("/"))
					fullXPath = fullXPath.substring(1);
				int index = fullXPath.indexOf("/");
				String childName = ((index != -1) ? fullXPath.substring(0, index) : fullXPath);

				NodeList list = parent.getChildNodes();
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						if (child.getNodeName().equalsIgnoreCase(childName)) {
							if (index == -1) {
								elt = (Element) child;
								break;
							} else {
								fullXPath = fullXPath.substring(index + 1);
								elt = findSingleElement((Element) child, fullXPath);
							}
						}
					}
				}
			}
		}
		return elt;
	}

	public static NodeList findElements(Element parent, String fullXPath) {
		NodeList list = null;
		Element elt = findSingleElement(parent, fullXPath);
		if (elt != null)
			list = ((Element) elt.getParentNode()).getElementsByTagName(elt.getNodeName());
		return list;
	}

	public static String getXPath(Element elt) {
		String xpath = "";
		if (elt != null) {
			Document doc = elt.getOwnerDocument();
			Element root = doc.getDocumentElement();
			Element parent = elt;
			while (parent != root) {
				xpath = "/" + parent.getNodeName() + xpath;
				parent = (Element) parent.getParentNode();
			}
		}
		return xpath;
	}

	public static String calcXpath(Node node) {
		return calcXpath(node, null);
	}

	/**
	 * Compute the xpath of a node relative to an anchor.
	 * 
	 * @param node
	 *            node to find the xpath from.
	 * @param anchor
	 *            the relative point to fid from.
	 * @return the computed xpath.
	 */
	public static String calcXpath(Node node, Node anchor) {
		String xpath = "";
		Node current = null;

		if (node == null || node.equals(anchor))
			return "";

		// add attribute to xpath
		if (node instanceof Attr) {
			Attr attr = (Attr) node;
			node = attr.getOwnerElement();
			xpath = '@' + attr.getName() + '/';
		}

		while ((current = node.getParentNode()) != anchor) {
			Engine.logEngine.trace("Calc Xpath : current node : " + current.getNodeName());
			NodeList childs = current.getChildNodes();
			int index = 0;
			for (int i = 0; i < childs.getLength(); i++) {
				if (childs.item(i).getNodeType() != Node.ELEMENT_NODE
						&& !childs.item(i).getNodeName().equalsIgnoreCase("#text"))
					continue;

				Engine.logEngine.trace("Calc Xpath : ==== > Child node : " + childs.item(i).getNodeName());

				// Bump the index if we have the same tag names..
				if (childs.item(i).getNodeName().equalsIgnoreCase(node.getNodeName())) {
					// tag names are equal ==> bump the index.
					index++;
					// is our node the one that is listed ?
					if (childs.item(i).equals(node))
						// We found our node in the parent node list
						break;
				}
			}
			// count the number of elements having the same tag
			int nbElements = 0;
			for (int i = 0; i < childs.getLength(); i++) {
				if (childs.item(i).getNodeName().equalsIgnoreCase(node.getNodeName())) {
					nbElements++;
				}
			}

			String name = node.getNodeName();
			if (name.equalsIgnoreCase("#text"))
				name = "text()";
			name = xpathEscapeColon(name);

			if (nbElements > 1) {
				xpath = name + "[" + index + "]/" + xpath;
			} else {
				// only one element had the same tag ==> do not compute the [xx]
				// syntax..
				xpath = name + "/" + xpath;
			}
			node = current;
		}
		if (xpath.length() > 0)
			// remove the trailing '/'
			xpath = xpath.substring(0, xpath.length() - 1);

		return xpath;
	}

	public static String xpathEscapeColon(String nameToEscape) {
		if (nameToEscape.contains(":"))
			return "*[name()=\"" + nameToEscape + "\"]";
		return nameToEscape;
	}
	
	public static String xpathGenerateConcat(String queryString) {
		String returnString = "";
		String searchString = queryString;
	    
	    int quotePosition = (searchString.indexOf("'") != -1)?searchString.indexOf("'"):searchString.indexOf("\"");
	    if (quotePosition == -1) {
	    	returnString = "'" + searchString + "'";
	    } else {
	        returnString = "concat(";
	        while (quotePosition != -1)
	        {
	            String subString = searchString.substring(0, quotePosition);
	            returnString += "'" + subString + "', ";
	            if (("'").equals(searchString.substring(quotePosition, quotePosition + 1)))
	            {
	                returnString += "\"'\", ";
	            }
	            else
	            {
	                //must be a double quote
	                returnString += "'\"', ";
	            }
	            searchString = searchString.substring(quotePosition + 1,searchString.length());
	            quotePosition = (searchString.indexOf("'") != -1)?searchString.indexOf("'"):searchString.indexOf("\"");
	        }
	        returnString += "'" + searchString + "')";
	    }
	    return returnString;
	}

	public static Document loadXml(File file) throws ParserConfigurationException, SAXException, IOException {
		Document document = getDefaultDocumentBuilder().parse(file);
		return document;
	}
	
	public static Document loadXml(String filePath) throws ParserConfigurationException, SAXException, IOException {
		return loadXml(new File(filePath));
	}

	public static Document createDom() throws ParserConfigurationException {
		Document document = getDefaultDocumentBuilder().newDocument();
		return document;
	}

	public static Document createDom(String xmlEngine) throws ParserConfigurationException {
		return createDom();
	}

	public static Document parseDOM(String xmlEngine, String xml) throws ParserConfigurationException,
			SAXException, IOException {
		Document document = getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		return document;
	}

	public static void saveXml(Document dom, String filePath) throws IOException {
		saveXml(dom, filePath, false);
	}
	
	public static void saveXml(Document dom, String filePath, boolean omitXmlDeclaration) throws IOException {
		saveXml(dom, new File(filePath), omitXmlDeclaration);
	}

	public static void saveXml(Document dom, File file) throws IOException {
		saveXml(dom, file, false);
	}
	
	public static void saveXml(Document dom, File file, boolean omitXmlDeclaration) throws IOException {
		try {
			Transformer transformer = getNewTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
			transformer.transform(new DOMSource(dom.getDocumentElement()), new StreamResult(file.toURI().getPath()));
		} catch (Exception e) {
			throw new IOException("saveXml failed because : " + e.getMessage());
		}
	}

	public static String getNormalizedText(Node node) {
		String res = "";
		if (node.hasChildNodes()) {
			NodeList nl = node.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				if (nl.item(i).getNodeType() == Node.TEXT_NODE) {
					res += nl.item(i).getNodeValue();
				} else {
					// ignore <SCRIPT> nodes ...
					if (!nl.item(i).getNodeName().equalsIgnoreCase("script"))
						res += getNormalizedText(nl.item(i));
				}
			}
		}
		return res;
	}

	public static void removeChilds(Node node) {
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
			node.removeChild(nl.item(i));
	}

	public static void removeNode(Node node) {
		node.getParentNode().removeChild(node);
	}

	public static void removeNodeListContent(NodeList nodeList) {
		while (nodeList.getLength() > 0) {
			removeNode(nodeList.item(0));
		}
	}

	static public Document parseDOM(InputStream is) throws SAXException, IOException, EngineException {
		Document doc = getDefaultDocumentBuilder().parse(is);
		doc.normalizeDocument();
		return doc;

	}

	static public Document parseDOM(File file) throws SAXException, IOException, EngineException {
		InputStream is = null;
		try {
			return parseDOM(is = new FileInputStream(file));
		} finally {
			try {
				is.close();
			} catch (Exception e) {}
		}
	}

	static public Document parseDOM(String filename) throws SAXException, IOException, EngineException {
		return parseDOM(new File(filename));
	}

	static public Document parseDOMFromString(String sDom) throws SAXException, IOException {
		Document dom = getDefaultDocumentBuilder().parse(new InputSource(new StringReader(sDom)));
		return dom;
	}

	public static EntityResolver getEntityResolver() {
		return new EntityResolver() {
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				if ("-//W3C//ENTITIES Latin 1 for XHTML//EN".equals(publicId))
					return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml-lat1.ent"));
				if ("-//W3C//ENTITIES Special for XHTML//EN".equals(publicId))
					return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml-special.ent"));
				if ("-//W3C//ENTITIES Symbols for XHTML//EN".equals(publicId))
					return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml-symbol.ent"));
				if ("-//W3C//DTD XHTML 1.0 Strict//EN".equals(publicId))
					return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml1-strict.dtd"));
				if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(publicId))
					return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml1-transitional.dtd"));
				return new InputSource(new FileInputStream(systemId));
			}
		};
	}
	
	public static CatalogResolver getCatalogResolver() {
		return new CatalogResolver() {

			@Override
			public Catalog getCatalog() {
				return super.getCatalog();
			}

			@Override
			public String getResolvedEntity(String publicId, String systemId) {
				return super.getResolvedEntity(publicId, systemId);
			}

			@Override
			public Source resolve(String href, String base) throws TransformerException {
				return super.resolve(href, base);
			}

			@Override
			public InputSource resolveEntity(String publicId, String systemId) {
				try {
					if ("-//W3C//ENTITIES Latin 1 for XHTML//EN".equals(publicId))
						return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml-lat1.ent"));
					if ("-//W3C//ENTITIES Special for XHTML//EN".equals(publicId))
						return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml-special.ent"));
					if ("-//W3C//ENTITIES Symbols for XHTML//EN".equals(publicId))
						return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml-symbol.ent"));
					if ("-//W3C//DTD XHTML 1.0 Strict//EN".equals(publicId))
						return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml1-strict.dtd"));
					if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(publicId))
						return new InputSource(new FileInputStream(Engine.DTD_PATH + "/xhtml1-transitional.dtd"));
				}
				catch (FileNotFoundException e) {}
				return super.resolveEntity(publicId, systemId);
			}
        };
	}
	
	public static String getCDataText(String s) {
		String cdataText = "";
		try {
			if (!s.equals("")) {
				Document dom = createDom("java");
				Element root = dom.createElement("root");
				CDATASection cDATASection = dom.createCDATASection(s);
				root.appendChild(cDATASection);
				dom.appendChild(root);

				cdataText = prettyPrintElement(root, true, true);
				cdataText = cdataText.replaceAll("<root>", "");
				cdataText = cdataText.replaceAll("</root>", "");

				String cdataStart = "<![CDATA[";
				if (cdataText.startsWith(cdataStart)) {
					int i = cdataText.substring(cdataStart.length()).indexOf(cdataStart);
					if (i < 0) {
						cdataText = cdataText.replaceAll("<!\\[CDATA\\[", "");
						cdataText = cdataText.replaceAll("\\]\\]>", "");
					}
				}
			}
		} catch (ParserConfigurationException e) {
		}
		return cdataText;
	}

	public static String getCDataXml(String s) {
		return StringEscapeUtils.escapeXml(getCDataText(s));
	}

	public static int MAX_XML_SIZE_FOR_LOG_INFO = 5;

	public static void logXml(Document document, Logger log, String message) {
		if (document != null && log.isInfoEnabled()) {
			String xml = XMLUtils.prettyPrintDOM(document);

			if (xml.length() > XMLUtils.MAX_XML_SIZE_FOR_LOG_INFO * 1000) {
				if (!log.isDebugEnabled()) {
					log.info(message
							+ "\n[XML size is > " + XMLUtils.MAX_XML_SIZE_FOR_LOG_INFO
							+ "KB, enable DEBUG log level for this logger to see it completly!]\n"
							+ "[Extract limited to the first " + XMLUtils.MAX_XML_SIZE_FOR_LOG_INFO + "KB]\n"
							+ xml.substring(0, XMLUtils.MAX_XML_SIZE_FOR_LOG_INFO * 1000)
							+ "... (see the complete message in DEBUG log level)");
				}
				log.debug(message + ":\n" + xml);
			} else {
				log.info(message + ":\n" + xml);
			}
		}
	}
	
	public static Node[] toNodeArray(NodeList nl) {
		Node[] res = new Node[nl.getLength()];
		for (int i = 0 ; i < res.length ; i++) {
			res[i] = nl.item(i);
		}
		return res;
	}
	
	public static List<Node> toArrayList(NodeList nl) {
		List<Node> res = new ArrayList<Node>();
		for (int i = 0 ; i < nl.getLength() ; i++) {
			res.add(nl.item(i));
		}
		return res;
	}
	
	public static NodeList toNodeList(List<Node> nl) {	
		Document doc = getDefaultDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		for (Node n : nl) {
			root.appendChild(doc.adoptNode(n));
		}
		return root.getChildNodes();
	}
		
	public static void spreadNamespaces(Node node, String tns, boolean overwrite) {
		Document doc = node instanceof Document ? (Document) node : node.getOwnerDocument();
		boolean isParent = false;
		while (node != null) {
			Node next = null;
			if (!isParent && node.getNodeType() == Node.ELEMENT_NODE) {
				if (node.getNamespaceURI() == null) {
					node = doc.renameNode(node, tns, node.getNodeName());
				} else {
					if (overwrite) {
						tns = node.getNamespaceURI();
					}
				}
				NamedNodeMap nodeMap = node.getAttributes();
				int nodeMapLengthl = nodeMap.getLength();
				for (int i = 0; i < nodeMapLengthl; i++) {
					Node attr = nodeMap.item(i);
					if (attr.getNamespaceURI() == null) {
						doc.renameNode(attr, tns, attr.getNodeName());
					}
				}
			}
			isParent = (isParent || (next = node.getFirstChild()) == null) && (next = node.getNextSibling()) == null;
			node = isParent ? node.getParentNode() : next;
			if (isParent && node != null) {
				if (overwrite) {
					tns = node.getNamespaceURI();
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <N extends Node> N setNamespace(N node, String tns) {
		return (N) node.getOwnerDocument().renameNode(node, tns, node.getNodeName());
	}
	
	public static NodeList asNodeList(String...strings) {
		Document doc = getDefaultDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		for (String string : strings) {
			root.appendChild(doc.createTextNode(string));
		}
		return root.getChildNodes();
	}

	/**
	 * Check if a name is XML-compliant.
	 * @param xmlName the XML name
	 * @return true if the provided name is XML compliant or false otherwise
	 */
	public static boolean checkName(String xmlName) {
		try {
			createDom("java").createElement(xmlName);
			return true;
		} catch (DOMException e) {
			return false;
		} catch (ParserConfigurationException e) {
			// Should never occur
			return false;
		}
	}

	public static Document copyDocumentWithoutNamespace(Document document) throws ParserConfigurationException {
		Document newDocument = XMLUtils.createDom("java");
		copyNodeWithoutNamespace(newDocument, newDocument, document);
		return newDocument;
	}
	
	private static void copyNodeWithoutNamespace(Document document, Node parentNode, Node sourceNode) {
		Node destinationNode;
		if (sourceNode instanceof Document) {
			destinationNode = parentNode;
		} else {
			if (sourceNode instanceof Element) {
				String localName = XMLUtils.getLocalName(sourceNode);
				destinationNode = document.createElement(localName);
				
				// Copy attributes
				NamedNodeMap attributes = sourceNode.getAttributes();
				for (int i = 0; i < attributes.getLength(); i++) {
					Node sourceAttribute = attributes.item(i);
					
					String prefix = XMLUtils.getPrefix(sourceAttribute);
					
					if (!prefix.equalsIgnoreCase("xmlns")) {
						((Element) destinationNode).setAttribute(XMLUtils.getLocalName(sourceAttribute),
								sourceAttribute.getNodeValue());
					}
				}
			} else {
				destinationNode = document.importNode(sourceNode, false);
			}
			
			parentNode.appendChild(destinationNode);
		}
		
		NodeList childNodes = sourceNode.getChildNodes();
		int len = childNodes.getLength();
		for (int i = 0; i < len; i++) {
			XMLUtils.copyNodeWithoutNamespace(document, destinationNode, childNodes.item(i));
		}
	}
	
	public static String getPrefix(Node node) {
		String prefix = node.getPrefix();
		if (prefix == null) {
			prefix = node.getNodeName();
							
			// If the document is not namespace aware, we must split the attribute name
			// with ':' character.
			int i = prefix.indexOf(':');
			if (i != -1) {
				prefix = prefix.substring(0, i);
			}
		}
		
		return prefix;
	}
	
	public static String getLocalName(Node node) {
		String localName = node.getLocalName();
		if (localName == null) {
			localName = node.getNodeName();
							
			// If the document is not namespace aware, we must split the tag name
			// with ':' character.
			int i = localName.indexOf(':');
			if (i != -1) {
				localName = localName.substring(i+1);
			}
		}
		
		return localName;
	}
	
	private static Object getValue(Element elt, boolean ignoreStepIds, boolean useType) throws JSONException {
		Object value = null;

		try {
			if (elt.hasAttribute("type")) {
				String type = elt.getAttribute("type");

				if (type.equals("object")) {
					JSONObject jsonObject = new JSONObject();

					NodeList nl = elt.getChildNodes();
					for (int i = 0; i < nl.getLength(); i++) {
						Node node = nl.item(i);

						if (node instanceof Element) {
							Element child = (Element) node;
							String childName = child.hasAttribute("originalKeyName") ? child.getAttribute("originalKeyName") : child.getTagName();
							Object childValue = getValue(child, ignoreStepIds, useType);
							
							if (childValue != null) {
								jsonObject.put(childName, childValue);			
							} else {
								handleElement(child, jsonObject, ignoreStepIds, useType);
							}
						}
					}
					value = jsonObject;
				} else if (type.equals("array")) {
					JSONArray array = new JSONArray();

					NodeList nl = elt.getChildNodes();
					for (int i = 0; i < nl.getLength(); i++) {
						Node node = nl.item(i);

						if (node instanceof Element) {
							Element child = (Element) node;
							Object childValue = getValue(child, ignoreStepIds, useType);
							
							if (childValue != null) {
								array.put(childValue);							
							} else {
								JSONObject obj = new JSONObject();
								array.put(obj);
								handleElement(child, obj, ignoreStepIds, useType);
							}
						}
					}

					value = array;
				} else if (type.equals("string")) {
					value = elt.getTextContent();
				} else if (type.equals("boolean")) {
					value = Boolean.parseBoolean(elt.getTextContent());
				} else if (type.equals("null")) {
					value = JSONObject.NULL;
				} else if (type.equals("integer")) {
					value = Integer.parseInt(elt.getTextContent());
				} else if (type.equals("long")) {
					value = Long.parseLong(elt.getTextContent());
				} else if (type.equals("double")) {
					value = Double.parseDouble(elt.getTextContent());
				} else if (type.equals("float")) {
					value = Float.parseFloat(elt.getTextContent());
				}

				if (value != null) {
					elt.removeAttribute(type);
				}
			}
		} catch (Throwable t) {
			Engine.logEngine.debug("failed to convert the element " + elt.getTagName(), t);
		}
		
		return value;
	}
	
	public static  void handleElement(Element elt, JSONObject obj, boolean ignoreStepIds, boolean useType) throws JSONException {
		String key = elt.getTagName();
		JSONObject value = new JSONObject();
		NodeList nl = elt.getChildNodes();
		
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			
			if (node instanceof Element) {
				Element child = (Element) node;
				Object childValue = useType ? getValue(child, ignoreStepIds, useType) : null;
				
				if (childValue != null) {
					value.accumulate(child.getTagName(), childValue);
				} else {
					handleElement(child, value, ignoreStepIds, useType);					
				}
			}
		}

		JSONObject attr = new JSONObject();
		NamedNodeMap nnm = elt.getAttributes();
		
		for (int i = 0; i < nnm.getLength(); i++) {
			Node node = nnm.item(i);
			if (ignoreStepIds && (node.getNodeName() != "step_id")) {
				attr.accumulate(node.getNodeName(), node.getNodeValue());
			}
		}

		if (value.length() == 0) {
			String content = elt.getTextContent();
			if (attr.length() == 0) {
				obj.accumulate(key, content);
			} else {
				value.accumulate("text", content);
			}
		}

		if (attr.length() != 0) {
			value.accumulate("attr", attr);
		}

		if (value.length() != 0) {
			obj.accumulate(key, value);
		}
	}
	
	public static String XmlToJson(Element elt, boolean ignoreStepIds) throws JSONException {
		return(XmlToJson(elt, ignoreStepIds, false));
	}

	public static String XmlToJson(Element elt, boolean ignoreStepIds, boolean useType) throws JSONException {
		return (XmlToJson(elt, ignoreStepIds, useType, null));
	}
	
	public static String XmlToJson(Element elt, boolean ignoreStepIds, boolean useType, JsonRoot jsonRoot) throws JSONException {
		JSONObject json = new JSONObject();
		handleElement(elt, json, ignoreStepIds, useType);
		String jsonString =   json.toString(1);
		if (jsonRoot != null && !jsonRoot.equals(JsonRoot.docNode)) {
			JSONObject jso = new JSONObject(jsonString).getJSONObject(elt.getTagName());
			if (jsonRoot.equals(JsonRoot.docChildNodes)) {
				jso.remove("attr");
			}
			jsonString = jso.toString(1);
		}
		return jsonString;
	}
	
	public static void jsonToXml(Object object, Element element) throws JSONException {
		jsonToXml(object, null, element, true, true, false);
	}
	
	public static void jsonToXml(Object object, String objectKey, Element parentElement, boolean includeDataType, boolean compactArray) throws JSONException {
		jsonToXml(object, objectKey, parentElement, false, includeDataType, compactArray);
	}
	
	private static void jsonToXml(Object object, String objectKey, Element parentElement, boolean modifyElement, boolean includeDataType, boolean compactArray) throws JSONException {
		Engine.logBeans.trace("Converting JSON to XML: object=" + object + "; objectKey=\"" + objectKey + "\"");
		
		Document doc = parentElement.getOwnerDocument();

		// Normalize object key
		String originalObjectKey = objectKey;
		if (objectKey != null) {
			objectKey = StringUtils.normalize(objectKey);
		}

		// JSON object value case
		if (object instanceof JSONObject) {
			JSONObject json = (JSONObject) object;

			Element element = doc.createElement(objectKey == null ? "object" : objectKey);
			if (objectKey != null && !objectKey.equals(originalObjectKey)) {
				element.setAttribute("originalKeyName", originalObjectKey);
			}

			if (compactArray || modifyElement) {
				if (objectKey == null) {
					element = parentElement;
				} else {
					parentElement.appendChild(element);
				}
			} else {
				parentElement.appendChild(element);
			}

			if (includeDataType) {
				element.setAttribute("type", "object");
			}

			String[] keys = new String[json.length()];
			
			int index = 0;
			for (Iterator<String> i = GenericUtils.cast(json.keys()); i.hasNext();) {
				keys[index++] = i.next();
			}
			
			Arrays.sort(keys);
			
			for (String key: keys) {
				jsonToXml(json.get(key), key, element, false, includeDataType, compactArray);
			}
		}
		// Array value case
		else if (object instanceof JSONArray) {
			JSONArray array = (JSONArray) object;
			int len = array.length();

			Element arrayElement = parentElement;
			String arrayItemObjectKey = null;
			if (!(compactArray || modifyElement)) {
				arrayElement = doc.createElement(objectKey == null ? "array" : objectKey);
				if (objectKey != null && !objectKey.equals(originalObjectKey)) {
					arrayElement.setAttribute("originalKeyName", originalObjectKey);
				}
				parentElement.appendChild(arrayElement);

				if (includeDataType) {
					arrayElement.setAttribute("type", "array");
					arrayElement.setAttribute("length", "" + len);
				}
			} else {
				arrayItemObjectKey = objectKey;
			}

			for (int i = 0; i < len; i++) {
				Object itemArray = array.get(i);
				jsonToXml(itemArray, arrayItemObjectKey, arrayElement, false, includeDataType, compactArray);
			}
		}
		else {
			Element element = doc.createElement(objectKey == null ? "value" : objectKey);
			if (objectKey != null && !objectKey.equals(originalObjectKey)) {
				element.setAttribute("originalKeyName", originalObjectKey);
			}

			parentElement.appendChild(element);

			if (JSONObject.NULL.equals(object)) {
				object = null;
			}
			
			if (object != null) {
				Text text = doc.createTextNode(object.toString());
				element.appendChild(text);
			}
			
			if (includeDataType) {
				String objectType = object == null ? "null" : object.getClass().getCanonicalName();
				if (objectType.startsWith("java.lang.")) {
					objectType = objectType.substring(10);
				}
				element.setAttribute("type", objectType.toLowerCase());
			}
		}
		
	}
	
	public static Charset getEncoding(File file) {
		return getEncoding(file, null);
	}
	
	public static Charset getEncoding(File file, Charset charset) {
		InputStream is = null;
		try {
			byte[] buffer = new byte[128];
			int nb = (is = new FileInputStream(file)).read(buffer);
			String encoding = new String(buffer, 0, nb, "ASCII").replaceFirst("[\\d\\D]*encoding=\"(.*?)\"[\\d\\D]*", "$1");
			charset = Charset.forName(encoding);
		} catch (Exception e) {
			Engine.logEngine.debug("failed to detect xml encoding", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {}
			}
		}
		
		return charset == null ? Charset.defaultCharset() : charset;
	}
	
	public static Charset getEncoding(byte[] bytes, Charset charset) {
		try {
			String encoding = new String(bytes, 0, Math.min(bytes.length, 128), "ASCII").replaceFirst("[\\d\\D]*encoding=\"(.*?)\"[\\d\\D]*", "$1");
			charset = Charset.forName(encoding);
		} catch (Exception e) { }
		
		return charset == null ? Charset.defaultCharset() : charset;
	}
	
	public static String stripNonValidXMLCharacters(String text) {
		if (text == null || ("".equals(text))) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
		    int codePoint = text.codePointAt(i);
		    if (codePoint > 0xFFFF) {
		        i++;
		    }
		    if ((codePoint == 0x9) || (codePoint == 0xA) || (codePoint == 0xD)
		            || ((codePoint >= 0x20) && (codePoint <= 0xD7FF))
		            || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
		            || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF))) {
		        sb.appendCodePoint(codePoint);
		    }
		}
		return sb.toString();
	}
}
