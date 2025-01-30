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

package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class ReadXMLStep extends ReadFileStep {

	private static final long serialVersionUID = 9145682088577678134L;

	public ReadXMLStep() {
		super();
	}

	@Override
	public ReadXMLStep clone() throws CloneNotSupportedException {
		ReadXMLStep clonedObject = (ReadXMLStep) super.clone();
		return clonedObject;
	}

	@Override
	public ReadXMLStep copy() throws CloneNotSupportedException {
		ReadXMLStep copiedObject = (ReadXMLStep) super.copy();
		return copiedObject;
	}

	@Override
	public String getStepNodeName() {
		if (replaceStepElement) {
			File file = getFile();
			if (file != null && file.exists() && file.length() <= 10000000) {
				String[] nodeName = {null};
				try {
					XMLUtils.saxParse(file, new DefaultHandler() {

						@Override
						public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
							nodeName[0] = qName;
							throw new SAXException("stop");
						}

					});
				} catch (Exception e) {
					if (nodeName[0] != null) {
						return nodeName[0];
					}
				}
			}
		}
		return super.getStepNodeName();
	}	

	@Override
	public String toString() {
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {
		}

		return "ReadXML: " + label;
	}

	protected Document read(String filePath, boolean schema) {
		return readMyXML(filePath);
	}

	static private boolean hasXmlRoot(Document xmlDoc) {
		if (xmlDoc != null) {
			Element xmlRoot = xmlDoc.getDocumentElement();
			if (xmlRoot == null) {
				return false;
			}
			Node first = xmlRoot.getFirstChild();
			if (first == null) {
				return false;
			}
			for (Node node = first; node != null; node = node.getNextSibling()) {
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					return true;
				}
			}

		}
		return false;
	}

	protected Document readMyXML(String filePath) {
		Document xmlDoc = null;

		try {
			File xmlFile = new File(getAbsoluteFilePath(filePath));
			if (!xmlFile.exists()) {
				Engine.logBeans.warn("(ReadXML) XML File '" + filePath + "' does not exist.");

				xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				xmlDoc.appendChild(xmlDoc.createElement("readxml_error"));
				Element myEl = xmlDoc.createElement("message");
				myEl.appendChild(xmlDoc.createTextNode("File '" + filePath + "' not found." ));
				xmlDoc.getDocumentElement().appendChild(myEl);
			} else {
				xmlDoc = XMLUtils.parseDOM(xmlFile);
				if (!hasXmlRoot(xmlDoc)) {
					Engine.logBeans.warn("(ReadXML) XML File '" + filePath + "' is missing a root element.");
					Element xmlRoot = xmlDoc.getDocumentElement();
					Element newRoot = xmlDoc.createElement("document");
					xmlDoc.replaceChild(newRoot, xmlRoot);
					newRoot.appendChild(xmlRoot);
				}
				if (Engine.logBeans.isDebugEnabled()) {
					String xmlContent = XMLUtils.prettyPrintDOM(xmlDoc);
					if (Engine.logBeans.isTraceEnabled()) {
						Engine.logBeans.trace("(ReadXML) XML File [" + xmlContent.length() + "] content '" + xmlContent + "'");
					} else {
						Engine.logBeans.debug("(ReadXML) XML File [" + xmlContent.length() + ", show max 10000] content '" + xmlContent.substring(0, Math.min(xmlContent.length(), 10000)) + "'");
					}
				}
			}
		} catch (Exception e1) {
			Engine.logBeans.warn("(ReadXML) Error while trying to parse XML file : " + e1.toString());
			try {
				xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				xmlDoc.appendChild(xmlDoc.createElement("document"));
				Element myEl = xmlDoc.createElement("message");
				myEl.appendChild(xmlDoc.createTextNode("Unable to parse file '" + filePath + "'." ));
				xmlDoc.getDocumentElement().appendChild(myEl);
			}
			catch (Exception e2) {
				Engine.logBeans.warn("(ReadXML) An error occured while building error xml document: " + e1.toString());
			}
		}

		return xmlDoc;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);

		File file = getFile();
		if (file != null && file.exists() && file.length() <= 10000000) {
			try {
				Document doc = XMLUtils.parseDOM(file);
				if (!hasXmlRoot(doc)) {
					Element xmlRoot = doc.getDocumentElement();
					Element newRoot = doc.createElement("document");
					doc.replaceChild(newRoot, xmlRoot);
					newRoot.appendChild(xmlRoot);
				}

				XmlSchemaElement elt = XmlSchemaUtils.extractXmlSchemaElement(doc, schema, this);

				if (element != null && !replaceStepElement) {
					XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
					element.setType(cType);

					XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
					cType.setParticle(sequence);

					sequence.getItems().add(elt);
				} else {
					element = elt;
				}
			} catch (Exception e) {
				Engine.logBeans.info("Failed to load the xml file " + file.getAbsolutePath(), e);
			}
		}

		if (element == null) {
			element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		}

		return element;
	}

	@Override
	protected String migrateSourceXpathFor620(String filePath, String xpath) throws Exception {
		File xmlFile = new File(getAbsoluteFilePath(filePath));
		if (xmlFile.exists()) {
			Document xmlDoc = XMLUtils.parseDOM(xmlFile);
			if (!hasXmlRoot(xmlDoc)) {
				if (xpath.startsWith("./")) {
					xpath = xpath.replaceFirst("./", "./document/");
				}
			}
		}
		return xpath;
	}

}
