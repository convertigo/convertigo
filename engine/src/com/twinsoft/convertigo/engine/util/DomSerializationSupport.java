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

package com.twinsoft.convertigo.engine.util;

import java.io.Serial;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.twinsoft.convertigo.engine.Engine;

public final class DomSerializationSupport {
	private DomSerializationSupport() {
	}

	public enum DomType {
		DOCUMENT,
		ELEMENT,
		NODE,
		NODE_LIST
	}

	public record SerializedDom(DomType type, String xml) implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		public SerializedDom {
			if (type == null || xml == null) {
				throw new IllegalArgumentException("type and xml must be non-null");
			}
		}
	}

	public static SerializedDom serialize(Object value) {
		if (value instanceof Document document) {
			return serializeDocument(document);
		}
		if (value instanceof Element element) {
			return serializeElement(element);
		}
		// Xerces nodes can also implement NodeList: prefer their precise Node type.
		try {
			if (value instanceof Node node) {
				return serializeNode(node);
			}
			if (value instanceof NodeList nodes) {
				var document = newDocument();
				var root = document.createElement("nodes");
				document.appendChild(root);
				for (int i = 0; i < nodes.getLength(); i++) {
					var item = serialize(nodes.item(i));
					if (item == null) {
						throw new IllegalArgumentException("Unsupported DOM node in NodeList");
					}
					var entry = document.createElement("item");
					entry.setAttribute("type", item.type().name());
					entry.setTextContent(item.xml());
					root.appendChild(entry);
				}
				return new SerializedDom(DomType.NODE_LIST, writeXml(document));
			}
		} catch (Exception e) {
			log("Failed to serialize DOM value", e);
		}
		return null;
	}

	public static SerializedDom serializeDocument(Document document) {
		if (document == null) {
			return null;
		}
		try {
			if (document.getDoctype() != null) {
				throw new IllegalArgumentException("DTD-bearing documents are not supported in stored DOM values");
			}
			var xml = document.hasChildNodes() ? writeXml(document) : "";
			return xml != null ? new SerializedDom(DomType.DOCUMENT, xml) : null;
		} catch (Exception e) {
			log("Failed to serialize document", e);
			return null;
		}
	}

	public static SerializedDom serializeElement(Element element) {
		if (element == null) {
			return null;
		}
		try {
			var detached = (Element) element.cloneNode(true);
			// Keep inherited bindings too (e.g. prefixes used only in xsi:type values).
			for (var parent = element.getParentNode(); parent instanceof Element; parent = parent.getParentNode()) {
				var attributes = parent.getAttributes();
				for (int i = 0; i < attributes.getLength(); i++) {
					var attribute = attributes.item(i);
					if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())
							&& !detached.hasAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attribute.getLocalName())) {
						detached.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attribute.getNodeName(), attribute.getNodeValue());
					}
				}
			}
			var xml = writeXml(detached);
			return xml != null ? new SerializedDom(DomType.ELEMENT, xml) : null;
		} catch (Exception e) {
			log("Failed to serialize element", e);
			return null;
		}
	}

	public static Object deserialize(SerializedDom serializedDom) {
		if (serializedDom == null) {
			return null;
		}
		return switch (serializedDom.type()) {
			case DOCUMENT -> deserializeDocument(serializedDom.xml());
			case ELEMENT -> deserializeElement(serializedDom.xml());
			case NODE -> deserializeNode(serializedDom.xml());
			case NODE_LIST -> deserializeNodeList(serializedDom.xml());
		};
	}

	public static Document deserializeDocument(String xml) {
		if (xml == null) {
			return null;
		}
		try {
			return xml.isEmpty() ? newDocument() : parseXml(xml);
		} catch (Exception e) {
			log("Failed to deserialize document", e);
			return null;
		}
	}

	public static Element deserializeElement(String xml) {
		if (xml == null || xml.isEmpty()) {
			return null;
		}
		try {
			var document = parseXml(xml);
			return document != null ? document.getDocumentElement() : null;
		} catch (Exception e) {
			log("Failed to deserialize element", e);
			return null;
		}
	}

	private static SerializedDom serializeNode(Node node) throws Exception {
		var document = newDocument();
		var root = document.createElement("node");
		document.appendChild(root);
		root.setAttribute("type", Short.toString(node.getNodeType()));
		root.setAttribute("name", node.getNodeName());
		if (node.getNamespaceURI() != null) {
			root.setAttribute("namespace", node.getNamespaceURI());
		}
		switch (node.getNodeType()) {
			case Node.ATTRIBUTE_NODE, Node.TEXT_NODE, Node.CDATA_SECTION_NODE,
					Node.COMMENT_NODE, Node.PROCESSING_INSTRUCTION_NODE -> root.setTextContent(node.getNodeValue());
			case Node.DOCUMENT_FRAGMENT_NODE -> {
				for (var child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
					root.appendChild(document.importNode(child, true));
				}
			}
			default -> throw new IllegalArgumentException("Unsupported DOM node type: " + node.getNodeType());
		}
		return new SerializedDom(DomType.NODE, writeXml(document));
	}

	private static Node deserializeNode(String xml) {
		try {
			var root = parseXml(xml).getDocumentElement();
			var document = newDocument();
			var value = root.getTextContent();
			return switch (Short.parseShort(root.getAttribute("type"))) {
				case Node.ATTRIBUTE_NODE -> {
					var attribute = root.hasAttribute("namespace")
							? document.createAttributeNS(root.getAttribute("namespace"), root.getAttribute("name"))
							: document.createAttribute(root.getAttribute("name"));
					attribute.setValue(value);
					yield attribute;
				}
				case Node.TEXT_NODE -> document.createTextNode(value);
				case Node.CDATA_SECTION_NODE -> document.createCDATASection(value);
				case Node.COMMENT_NODE -> document.createComment(value);
				case Node.PROCESSING_INSTRUCTION_NODE -> document.createProcessingInstruction(root.getAttribute("name"), value);
				case Node.DOCUMENT_FRAGMENT_NODE -> {
					var fragment = document.createDocumentFragment();
					for (var child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
						fragment.appendChild(document.importNode(child, true));
					}
					yield fragment;
				}
				default -> throw new IllegalArgumentException("Unsupported stored DOM node type");
			};
		} catch (Exception e) {
			log("Failed to deserialize node", e);
			return null;
		}
	}

	private static NodeList deserializeNodeList(String xml) {
		try {
			var root = parseXml(xml).getDocumentElement();
			var nodes = new ArrayList<Node>();
			for (var entry = root.getFirstChild(); entry != null; entry = entry.getNextSibling()) {
				if (entry instanceof Element item) {
					var node = deserialize(new SerializedDom(DomType.valueOf(item.getAttribute("type")), item.getTextContent()));
					if (!(node instanceof Node)) {
						throw new IllegalArgumentException("Invalid stored NodeList item");
					}
					nodes.add((Node) node);
				}
			}
			// A detached snapshot, not a live query against the original document.
			return new NodeList() {
				@Override
				public Node item(int index) {
					return index >= 0 && index < nodes.size() ? nodes.get(index) : null;
				}
				@Override
				public int getLength() {
					return nodes.size();
				}
			};
		} catch (Exception e) {
			log("Failed to deserialize node list", e);
			return null;
		}
	}

	private static Document newDocument() throws Exception {
		return DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument();
	}

	private static Document parseXml(String xml) throws Exception {
		var factory = DocumentBuilderFactory.newDefaultInstance();
		factory.setNamespaceAware(true);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		var builder = factory.newDocumentBuilder();
		builder.setEntityResolver((publicId, systemId) -> { throw new SAXException("External entities are forbidden"); });
		builder.setErrorHandler(new DefaultHandler() {
			@Override
			public void fatalError(SAXParseException e) throws SAXException {
				throw e;
			}
		});
		return builder.parse(new InputSource(new StringReader(xml)));
	}

	private static String writeXml(Node node) throws Exception {
		checkNodeTypes(node);
		var factory = TransformerFactory.newDefaultInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		var transformer = factory.newTransformer();
		// Pretty-printing adds text nodes and can invalidate signed XML.
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		var writer = new StringWriter();
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		return writer.toString();
	}

	private static void checkNodeTypes(Node node) {
		// Never silently drop entity/DTD nodes that the stored XML reader cannot restore.
		switch (node.getNodeType()) {
			case Node.DOCUMENT_TYPE_NODE, Node.ENTITY_NODE, Node.ENTITY_REFERENCE_NODE, Node.NOTATION_NODE ->
				throw new IllegalArgumentException("DTD/entity nodes are not supported in stored DOM values");
			default -> { }
		}
		for (var child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			checkNodeTypes(child);
		}
	}

	private static void log(String message, Exception e) {
		try {
			if (Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(DomSerializationSupport) " + message, e);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}
}
