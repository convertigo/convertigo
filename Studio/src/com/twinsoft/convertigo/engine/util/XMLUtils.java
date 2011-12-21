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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xerces.dom.AttrImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.common.XMLizable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.util.StringEx;

public class XMLUtils {
	private static ThreadLocal<DocumentBuilderFactory> defaultDocumentBuilderFactory = new ThreadLocal<DocumentBuilderFactory>() {
		protected DocumentBuilderFactory initialValue() {
			return DocumentBuilderFactory.newInstance();
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
	
	public static String simplePrettyPrintDOM(String sDocument) {
		StringEx sxDocument = new StringEx(sDocument);
		sxDocument.replaceAll(">", ">\n");
		return sxDocument.toString();
	}
    
	public static String prettyPrintDOM(String sDocument) throws ParserConfigurationException, SAXException, IOException {
		Document document = getDefaultDocumentBuilder().parse(new InputSource(new StringReader(sDocument)));
		return XMLUtils.prettyPrintDOM(document);
	}
    
	public static String prettyPrintDOM(String sDocument, String relativeUriResolver) throws ParserConfigurationException, SAXException, IOException {
        InputSource inputSource = new InputSource(new StringReader(sDocument));
        inputSource.setSystemId(relativeUriResolver);
        Document document = getDefaultDocumentBuilder().parse(inputSource);
        return XMLUtils.prettyPrintDOM(document);
    }

    public static String prettyPrintDOM(Node node) {
        return XMLUtils.prettyPrintDOM(node.getOwnerDocument());
    }
    
    public static String prettyPrintDOMWithEncoding(Document doc) {
    	return prettyPrintDOMWithEncoding(doc,"ISO-8859-1");
    }
    
    public static String prettyPrintDOMWithEncoding(Document doc, String defaultEncoding) {
    	StringWriter writer = new StringWriter();
    	prettyPrintDOMWithEncoding(doc, defaultEncoding, writer);
    	return writer.getBuffer().toString();
    }
    
    public static void prettyPrintDOM(Document doc, Writer writer) {
    	prettyPrintDOMWithEncoding(doc, "UTF-8", writer);
    }
    
    public static void prettyPrintDOM(Document doc, String defaultEncoding, Writer writer) {
    	prettyPrintDOMWithEncoding(doc, defaultEncoding, writer);
    }
    
    public static void prettyPrintDOMWithEncoding(Document doc, String defaultEncoding, Writer writer) {
		Node firstChild = doc.getFirstChild();
		boolean omitXMLDeclaration = false;
		String	encoding = defaultEncoding; // default Encoding char set if non is found in the PI
		
		if ( (firstChild.getNodeType() == Document.PROCESSING_INSTRUCTION_NODE)
		  && (firstChild.getNodeName().equals("xml"))) {
			omitXMLDeclaration = true;
			String piValue = firstChild.getNodeValue();
			// extract from PI the encoding Char Set
			int	encodingOffset = piValue.indexOf("encoding=\"");
			if (encodingOffset != -1) {
				encoding = piValue.substring(encodingOffset+10);
				// remove the last "
				encoding = encoding.substring(0, encoding.length()-1);
			}
		}

		TransformerFactory tfac = TransformerFactory.newInstance();
		try {
			Transformer t = tfac.newTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, encoding);
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty(OutputKeys.METHOD, "xml"); //xml, html, text
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration? "yes":"no");
			t.transform(new DOMSource(doc), new StreamResult(writer));
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception while pretty print DOM", e);
		}
		
    }
    
    public static String prettyPrintDOM(Document doc) {
    	return prettyPrintDOMWithEncoding(doc, "ISO-8859-1");
    }
    
    public static String prettyPrintElement(Element elt, boolean omitXMLDeclaration, boolean bIndent) {
		Node firstChild = elt;
		String	encoding = "ISO-8859-1"; // default Encoding char set if non is found in the PI
		
		if (omitXMLDeclaration 	&&
			(firstChild.getNodeType() == Document.PROCESSING_INSTRUCTION_NODE) && 
			(firstChild.getNodeName().equals("xml"))) {
			String piValue = firstChild.getNodeValue();
			// extract from PI the encoding Char Set
			int	encodingOffset = piValue.indexOf("encoding=\"");
			if (encodingOffset != -1) {
				encoding = piValue.substring(encodingOffset+10);
				// remove the last "
				encoding = encoding.substring(0, encoding.length()-1);
			}
		}
		StringWriter strWtr = new StringWriter();
		TransformerFactory tfac = TransformerFactory.newInstance();
		try {
			Transformer t = tfac.newTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, encoding);
			t.setOutputProperty(OutputKeys.INDENT, bIndent? "yes":"no");
			t.setOutputProperty(OutputKeys.METHOD, "xml"); //xml, html, text
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration? "yes":"no");
			t.transform(new DOMSource(elt), new StreamResult(strWtr));
			return strWtr.getBuffer().toString();
		} catch (Exception e) {
			System.err.println("XML.toString(Document): " + e);
			Engine.logEngine.error("Unexpected exception", e);
			return e.getMessage();
		}
    }

/*
     public static String prettyPrintElement(Element elt) {
		Node firstChild = elt;
		boolean omitXMLDeclaration = false; 
		if ( (firstChild.getNodeType() == Document.PROCESSING_INSTRUCTION_NODE)
		  && (firstChild.getNodeName().equals("xml")))
			omitXMLDeclaration = true;

        OutputFormat format = new OutputFormat("XML", "ISO-8859-1", true);
        format.setLineWidth(0);
        format.setIndent(4);
        
        format.setNonEscapingElements(new String[] {"SCRIPT", "script"});
        
        format.setOmitDocumentType(true);
		format.setOmitXMLDeclaration(omitXMLDeclaration);
        StringWriter sw = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(sw, format);
        
        try {
            serializer.serialize(elt);
            return sw.toString();
        }
        catch(IOException e) {
			Engine.logEngine.error("Unexpected exception", e);
            return e.getMessage();
        }
    }

    public static String prettyPrintElement(Element elt, boolean omitXMLDeclaration, int indent) {
		OutputFormat format = new OutputFormat("XML", "ISO-8859-1", true);
        format.setLineWidth(0);
        format.setIndent(indent);
        
        format.setNonEscapingElements(new String[] {"SCRIPT", "script"});
        
        format.setOmitDocumentType(true);
		format.setOmitXMLDeclaration(omitXMLDeclaration);
        StringWriter sw = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(sw, format);
        
        try {
            serializer.serialize(elt);
            return sw.toString();
        }
        catch(IOException e) {
			Engine.logEngine.error("Unexpected exception", e);
            return e.getMessage();
        }
    }
*/
    public static Node writeObjectToXml(Document document, Object object) throws Exception {
    	return writeObjectToXml(document, object, null);
    }

    public static Node writeObjectToXml(Document document, Object object, Object compiledValue) throws Exception {
        if (object == null) return null;
        
        // Simple objects
        if ((object instanceof Boolean) || (object instanceof Integer) ||
        (object instanceof Double) || (object instanceof Float) || (object instanceof Character) ||
        (object instanceof Long) || (object instanceof Short) || (object instanceof Byte)) {
            Element element = document.createElement(object.getClass().getName());
            element.setAttribute("value", object.toString());
            if (compiledValue != null) element.setAttribute("compiledValue", compiledValue.toString());
            return element;
        }
        // Strings
		else if (object instanceof String) {
			Element element = document.createElement(object.getClass().getName());
			element.setAttribute("value", object.toString());
            if (compiledValue != null) element.setAttribute("compiledValue", compiledValue.toString());
			return element;
		}
        // Arrays
        else if (object.getClass().getName().startsWith("[")) {
            String arrayClassName = object.getClass().getName();
            int i = arrayClassName.lastIndexOf('[');
            
            String itemClassName = arrayClassName.substring(i+1);
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
            for (int k = 0 ; k < len ; k++) {
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
            ByteArrayOutputStream outputStream =  new ByteArrayOutputStream(1024);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            outputStream.close();
            
            // Now, we retrieve the object bytes
            objectBytes = outputStream.toByteArray();
            objectBytesEncoded = Base64.encodeBytes(objectBytes);
            
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
        }
        else if (nodeName.equals("java.lang.Byte")) {
            return new Byte(nodeValue);
        }
        else if (nodeName.equals("java.lang.Character")) {
            return new Character(nodeValue.charAt(0));
        }
        else if (nodeName.equals("java.lang.Integer")) {
            return new Integer(nodeValue);
        }
        else if (nodeName.equals("java.lang.Double")) {
            return new Double(nodeValue);
        }
        else if (nodeName.equals("java.lang.Float")) {
            return new Float(nodeValue);
        }
        else if (nodeName.equals("java.lang.Long")) {
            return new Long(nodeValue);
        }
        else if (nodeName.equals("java.lang.Short")) {
            return new Short(nodeValue);
        }
        else if (nodeName.equals("java.lang.String")) {
			StringEx sxValue = new StringEx(nodeValue);
			sxValue.replaceAll("\\n", "\n");
			return sxValue.toString();
        }
        else if (nodeName.equals("array")) {
            String className = node.getAttribute("classname");
            String length = node.getAttribute("length");
            int len = (new Integer(length)).intValue();
            
            Object array;
            if (className.equals("byte")) {
                array = new byte[len];
            }
            else if (className.equals("boolean")) {
                array = new boolean[len];
            }
            else if (className.equals("char")) {
                array = new char[len];
            }
            else if (className.equals("double")) {
                array = new double[len];
            }
            else if (className.equals("float")) {
                array = new float[len];
            }
            else if (className.equals("int")) {
                array = new int[len];
            }
            else if (className.equals("long")) {
                array = new long[len];
            }
            else if (className.equals("short")) {
                array = new short[len];
            }
            else {
                array = Array.newInstance(Class.forName(className), len);
            }
            
            Node xmlNode = null;
            NodeList nl = node.getChildNodes();
            int len_nl = nl.getLength();
            int i = 0;
            for (int j = 0 ; j < len_nl ; j++) {
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
            byte[] objectBytes = Base64.decode(objectSerializationData);
            
            // We read the object to a bytes array
            ByteArrayInputStream inputStream =  new ByteArrayInputStream(objectBytes);
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
        for (int j = 0 ; j < len_nl ; j++) {
            foundNode = nl.item(j);
            if (foundNode.getNodeType() == type) return foundNode;
        }
        
        return null;
    }
    
    public static Node findNodeByAttributeValue(NodeList nodeList, String attributeName, String attributeValue) {
        int len = nodeList.getLength();
        String tmp;
        Element property;
        for (int i = 0 ; i < len ; i++) {
            property = (Element) nodeList.item(i);
            tmp = property.getAttribute(attributeName);
            if (attributeValue.equals(tmp)) {
                return (Node) property;
            }
        }
        return null;
    }
    
    public static Object findPropertyValue(Element databaseObjectNode, String propertyName) throws Exception {
        NodeList properties = databaseObjectNode.getElementsByTagName("property");

        Element projectNameElement = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", propertyName);

        Node xmlNode = null;
        NodeList nl = projectNameElement.getChildNodes();
        int len_nl = nl.getLength();
        for (int j = 0 ; j < len_nl ; j++) {
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
                }
                catch(StringIndexOutOfBoundsException e) {
                    return null;
                }
            }
            return null;
        }
        else {
            NodeList children = node.getChildNodes();
            int len = children.getLength();
            for (int i = 0 ; i < len ; i++) {
                String result = findXslHref(children.item(i));
                if (result != null) return result;
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
    			String childName = ((index != -1) ? fullXPath.substring(0,index):fullXPath);
    			
    			NodeList list = parent.getChildNodes();
    			for (int i=0; i<list.getLength(); i++) {
    				Node child = list.item(i);
    				if (child.getNodeType() == Node.ELEMENT_NODE) {
    					if (child.getNodeName().equalsIgnoreCase(childName)) {
    						if (index == -1) {
    							elt = (Element)child;
								break;
    						}
    						else {
								fullXPath = fullXPath.substring(index+1);
								elt = findSingleElement((Element)child,fullXPath);
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
    	Element elt = findSingleElement(parent,fullXPath);
    	if (elt != null)
    		list = ((Element)elt.getParentNode()).getElementsByTagName(elt.getNodeName());
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
				parent = (Element)parent.getParentNode();
			}
		}
		return xpath;
	}
	
	public static  String calcXpath(Node node){
		return calcXpath(node, null);
	}
	
	/**
	 * Compute the xpath of a node relative to an anchor.
	 * @param node		node to find the xpath from.
	 * @param anchor	the relative point to fid from.
	 * @return			the computed xpath.
	 */
	public static  String calcXpath(Node node, Node anchor) 
	{
		String 	xpath = "";
		Node	current = null;
		
		if (node == null || node.equals(anchor))
			return "";
		
		// add attribute to xpath
		if(node instanceof Attr){
			Attr attr = (AttrImpl)node;
			node = attr.getOwnerElement();
			xpath = '@'+attr.getName()+'/';
		}
		
		while ((current = node.getParentNode()) != anchor) {
			Engine.logEngine.trace("Calc Xpath : current node : " + current.getNodeName());
			NodeList childs = current.getChildNodes();
			int index = 0;
			for (int i =0;  i < childs.getLength(); i++) {
				if (childs.item(i).getNodeType() != Node.ELEMENT_NODE && !childs.item(i).getNodeName().equalsIgnoreCase("#text")) 
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
			for (int i=0; i < childs.getLength(); i++) {
				if (childs.item(i).getNodeName().equalsIgnoreCase(node.getNodeName())) {
					nbElements++;
				}
			}
			
			String name = node.getNodeName();
			if(name.equalsIgnoreCase("#text")) name = "text()";
			name= xpathEscapeColon(name);
			
			if (nbElements > 1) {
				xpath = name + "[" + index + "]/" + xpath; 
			} else {
				// only one element had the same tag ==> do not compute the [xx] syntax..
				xpath = name +"/" + xpath; 
			}
			node = current;
		}
		if (xpath.length() > 0)
			// remove the trailing '/'
			xpath = xpath.substring(0, xpath.length()-1);
		
		return xpath;
	}
	
	
	public static String xpathEscapeColon(String nameToEscape){
		if(nameToEscape.contains(":")) 
			return "*[name()=\""+nameToEscape+"\"]";
		return nameToEscape;
	}
	
	public static Document loadXml(String filePath) throws ParserConfigurationException, SAXException, IOException {
        Document document = getDefaultDocumentBuilder().parse(new File(filePath));
        return document;
	}
	
    public static Document createDom(String xmlEngine) throws ParserConfigurationException {
        Document document = null;
        
        // Java default XML engine
        if (xmlEngine.equals("java")) {
            document = getDefaultDocumentBuilder().newDocument();
        }
        // MSXML document
        else if (xmlEngine.equals("msxml")) {
            document = new com.ms.xml.Document();
        }

        return document;
    }
	
    public static Document parseDOM(String xmlEngine, String xml) throws ParserConfigurationException, SAXException, IOException {
        Document document = null;
        
        // Java default XML engine
        if (xmlEngine.equals("java")) {
    		document = getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        }
        // MSXML engine
        else if (xmlEngine.equals("msxml")) {
        	com.ms.xml.Document msxmlDocument = new com.ms.xml.Document();
        	document = msxmlDocument;
        	msxmlDocument.setAsync(false);
            msxmlDocument.loadXML(xml);
        }
        
        return document;
    }
	
	public static void saveXml(Document dom, String filePath)  throws IOException {
		try {
			File file = new File(filePath);
			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(dom), new StreamResult(file.toURI().getPath()));
		} catch (Exception e) {
			throw new IOException("saveXml failed because : " + e.getMessage());
		}
	}
	
	public static String getNormalizedText(Node node){
		String res = "";
		if (node.hasChildNodes()) {
			NodeList nl = node.getChildNodes();
			for (int i = 0; i< nl.getLength(); i++) {
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
	
	public static void removeChilds(Node node){
		NodeList nl = node.getChildNodes();
		for(int i=0;i<nl.getLength();i++) node.removeChild(nl.item(i));
	}
	
	static public Document parseDOM(InputStream is) throws SAXException, IOException, EngineException{
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			doc.normalizeDocument();
			return doc;
		} catch (ParserConfigurationException e1) {
			throw new EngineException("Unable create document builder", e1);
		}
		
	}
	
	static public Document parseDOM(File file) throws SAXException, IOException, EngineException{
		return parseDOM(new FileInputStream(file));
	}
	
	static public Document parseDOM(String filename) throws SAXException, IOException, EngineException{
		return parseDOM(new File(filename)); 
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
			        if (i<0) {
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
}
