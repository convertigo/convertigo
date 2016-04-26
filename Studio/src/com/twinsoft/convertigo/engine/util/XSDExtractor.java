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

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XSDExtractor {

	Document xsdDom = null;
	Map<String, XSDObject> items = new HashMap<String, XSDObject>(10);
	
	public static Document extractXSD(String prefixName, Document doc) throws Exception {
		Document xsdDom = new XSDExtractor().parse(prefixName, doc);
		return xsdDom;
	}
	
	public static Document extractXSD(String prefixName, String xml) throws Exception {
		Document doc = XMLUtils.parseDOM("java", xml);
		Document xsdDom = new XSDExtractor().parse(prefixName, doc); 
		return xsdDom;
	}
	
	public static Document extractXSD(String prefixName, InputStream is) throws Exception {
		Document doc = XMLUtils.parseDOM(is);
		Document xsdDom = new XSDExtractor().parse(prefixName, doc); 
		return xsdDom;
	}

	protected XSDExtractor() {
		
	}
	
	private Document createDom() throws ParserConfigurationException {
		Document dom = XMLUtils.createDom("java");
		Element root = dom.createElement("xsd:schema");
		dom.appendChild(root);
		return dom;
	}
	
	private Document parse(String prefixName, Document xmlDom) throws Exception {
		xsdDom = createDom();
		if (xmlDom != null) {
			//Element root = xmlDom.getDocumentElement();
			NodeList list = xmlDom.getChildNodes();
			for (int i=0; i<list.getLength(); i++) {
				parse(list.item(i));
			}
			
			Enumeration<XSDObject> e = Collections.enumeration(items.values());
			while (e.hasMoreElements()) {
				e.nextElement().toXML(prefixName, null, xsdDom.getDocumentElement());
			}
		}
		return xsdDom;
	}
	
	private void parse(Node node) {
		if (node != null) {
			short type = node.getNodeType();
			switch (type) {
				case Node.ELEMENT_NODE:
					XSDElement xsdElement = new XSDElement((Element)node);
					items.put(xsdElement.name, xsdElement);
					xsdElement.parse();
					break;
				default:
					//System.out.println("Node '"+ node.getNodeName() +"' type:"+ type);
					break;
			}
		}
	}
		
	private abstract class XSDObject {
		protected static final int UNKNOWN_OBJECT_TYPE	= 0;
		protected static final int ELEMENT_SIMPLE_TYPE	= 1;
		protected static final int ELEMENT_COMPLEX_TYPE = 2;
		protected static final int ATTRIBUTE_SIMPLE_TYPE = 3;
		//protected static final int ATTRIBUTE_COMPLEX_TYPE = 4;

		protected int type = UNKNOWN_OBJECT_TYPE;
		protected String name = "";
		protected String value = "";
		
		abstract protected void parse();
		abstract protected void toXML(String prefixName, XSDObject parentXSDObject, Element parentElement);
		
		protected String getSchemaType() {
			String datatype = "xsd:string";
			
			// boolean
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
				return "xsd:boolean";
			
			// byte
			try {
				Byte.parseByte(value);
				return "xsd:byte";
			}
			catch (NumberFormatException e) {
			}

			// short
			try {
				Short.parseShort(value);
				return "xsd:short";
			}
			catch (NumberFormatException e) {
			}

			// integer
			try {
				Integer.parseInt(value);
				return "xsd:integer";
			}
			catch (NumberFormatException e) {
			}
			
			// long
			try {
				Long.parseLong(value);
				return "xsd:long";
			}
			catch (NumberFormatException e) {
			}

			// double
			try {
				Double.parseDouble(value);
				return "xsd:double";
			}
			catch (NumberFormatException e) {
			}
			
			// float
			try {
				Float.parseFloat(value);
				return "xsd:float";
			}
			catch (NumberFormatException e) {
			}
			
			// datetime
			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
				df.parse(value);
				return "xsd:datetime";
			} catch (ParseException e1) {
			}
			
			// date
			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				df.parse(value);
				return "xsd:date";
			} catch (ParseException e1) {
			}
			
			// time
			try {
				SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
				df.parse(value);
				return "xsd:time";
			} catch (ParseException e1) {
			}
			
			// uri
			try {
				new URL(value);
				return "xsd:anyURI";
			}
			catch (Exception e) {
			}
			
			return datatype;
		}
	}
	
	private class XSDElement extends XSDObject {
		private ArrayList<String> nes = new ArrayList<String>();
		private ArrayList<String> nas = new ArrayList<String>();
		private Map<String, XSDObject> elements = new HashMap<String, XSDObject>(4);
		private Map<String, Integer> occurs = new HashMap<String, Integer>(4);
		private Map<String, XSDObject> attributes = new HashMap<String, XSDObject>();
		private Element source;

		private XSDElement(Element element) {
			this.source = element;
			this.name = element.getTagName();
		}

		protected XSDObject add(Node node) {
			XSDObject xsdObject = null;
			if (node != null) {
				String key;
				short type = node.getNodeType();
				switch (type) {
					case Node.ELEMENT_NODE:
						key = ((Element)node).getTagName();
						//if (!elements.containsKey(key)) {
						if (!nes.contains(key)) {
							nes.add(key);
							xsdObject = new XSDElement((Element)node);
							elements.put(xsdObject.name, xsdObject);
							occurs.put(key, Integer.valueOf(1));
							xsdObject.parse();
						}
						else {
							int occur = ((Integer)occurs.get(key)).intValue();
							occurs.put(key, Integer.valueOf(occur+1));
							xsdObject = elements.get(key);
							((XSDElement)xsdObject).parse((Element)node);
						}
						break;
					case Node.ATTRIBUTE_NODE:
						key = ((Attr)node).getNodeName();
						//if (!attributes.containsKey(key)) {
						if (!nas.contains(key)) {
							nas.add(key);
							xsdObject = new XSDAttribute((Attr)node);
							attributes.put(xsdObject.name, xsdObject);
							xsdObject.parse();
						}
						else {
							// should not append
						}
						break;
					case Node.TEXT_NODE:
						break;
					default:
						//System.out.println("Node '"+ node.getNodeName() +"' type:"+ type);
						break;
				}
			}
			return xsdObject;
		}
		
		protected void parse() {
			parse(source);
		}
		
		protected void parse(Element element) {
			if (XMLUtils.findChildNode(element, Node.ELEMENT_NODE) != null) {
				this.type = ELEMENT_COMPLEX_TYPE;
			}
			else if (element.hasAttributes()) {
				this.type = ELEMENT_COMPLEX_TYPE;
			}
			else {
				this.type = ELEMENT_SIMPLE_TYPE;
				this.value = element.getTextContent().trim();
			}
			
			NamedNodeMap map = element.getAttributes();
			for (int i=0; i<map.getLength(); i++) {
				add(map.item(i));
			}
			
			NodeList list = element.getChildNodes();
			for (int i=0; i<list.getLength(); i++) {
				add(list.item(i));
			}
		}

		protected void toXML(String prefixName, XSDObject parentXSDObject, Element parentElement) {
			int occurence = 1;
			if ((parentXSDObject != null) && (parentXSDObject instanceof XSDElement)) {
				Object ob = ((XSDElement)parentXSDObject).occurs.get(name);
				if (ob != null) {
					occurence = ((Integer)ob).intValue();
				}
			}

			int index = name.indexOf(":");
			String extractedName = (index == -1) ? name:name.substring(index+1);
			String extractedPrefix = prefixName + name.replace(':', '-');
			String extractedType = extractedPrefix + "Type";//prefixName + extractedName + "Type";
			
			if (type == ELEMENT_COMPLEX_TYPE) {
				Element element = xsdDom.createElement("xsd:element");
				element.setAttribute("name", extractedName);
				element.setAttribute("type", "p_ns:"+ extractedType);
				if (parentXSDObject != null)
					element.setAttribute("minOccurs", "0");
				if (occurence > 1) {
					element.setAttribute("maxOccurs", "unbounded");
				}
				parentElement.appendChild(element);
				
				Element complex = xsdDom.createElement("xsd:complexType");
				complex.setAttribute("name", extractedType);
				
				prefixName = extractedPrefix + "_";//prefixName = prefixName + extractedName + "_";
				
				/*Enumeration e;
				e = elements.elements();
				if (e.hasMoreElements()) {
					Element sequence = xsdDom.createElement("xsd:sequence");
					while (e.hasMoreElements()) {
						((XSDElement)e.nextElement()).toXML(prefixName, this, sequence);
					}
					complex.appendChild(sequence);
				}*/
				if (!nes.isEmpty()) {
					Element sequence = xsdDom.createElement("xsd:sequence");
					for (String key: nes) {
						((XSDElement)elements.get(key)).toXML(prefixName, this, sequence);
					}
					complex.appendChild(sequence);
				}
				
				/*e = attributes.elements();
				while (e.hasMoreElements()) {
					((XSDAttribute)e.nextElement()).toXML(prefixName, this, complex);
				}*/
				if (!nas.isEmpty()) {
					for (String key: nas) {
						((XSDAttribute)attributes.get(key)).toXML(prefixName, this, complex);
					}
				}
				xsdDom.getDocumentElement().appendChild(complex);
			}
			else if (type == ELEMENT_SIMPLE_TYPE) {
				Element element = xsdDom.createElement("xsd:element");
				element.setAttribute("name", extractedName);
				element.setAttribute("type", getSchemaType());
				if (parentXSDObject != null)
					element.setAttribute("minOccurs", "0");
				if (occurence > 1) {
					element.setAttribute("maxOccurs", "unbounded");
				}
				parentElement.appendChild(element);
			}
		}
	}
	
	private class XSDAttribute extends XSDObject {
//		private Attr source = null;
		
		private XSDAttribute(Attr attr) {
//			this.source = attr;
			this.name = attr.getName();
			this.value = attr.getValue();
			this.type = ATTRIBUTE_SIMPLE_TYPE;
		}
		
//		public Attr getSource() {
//			return source;
//		}

		protected void parse() {
			
		}

		protected void toXML(String prefixName, XSDObject parentXSDObject, Element parentElement) {
			if (name.startsWith("xmlns"))
				return;
			
			if (name.indexOf(":") != -1)
				return;

			if (type == ATTRIBUTE_SIMPLE_TYPE) {
				Element attr = xsdDom.createElement("xsd:attribute");
				attr.setAttribute("name", name );
				attr.setAttribute("type", getSchemaType());
				parentElement.appendChild(attr);
			}
		}
		
	}
	
	/*public static void main(String[] args) {
		//String xsdURI = "C:/Development/SVN/Convertigo4.4.3/testXSD.xml";
		//String xsdURI = "C:/Development/SVN/Convertigo4.4.3/testXSD1.xml";
		//String xsdURI = "C:/Development/Tests/T867_array1.xml";
		try {
			Document doc = extractXSD("", new FileInputStream(xsdURI));
			String s = XMLUtils.prettyPrintDOMWithEncoding(doc, "UTF-8");
			System.out.println(s);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

}
