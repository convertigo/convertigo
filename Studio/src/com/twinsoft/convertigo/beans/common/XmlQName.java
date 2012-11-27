package com.twinsoft.convertigo.beans.common;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class XmlQName implements XMLizable {
	private String namespace = "";
	private String localPart = "";
	
	public XmlQName() {
		
	}
	
	public XmlQName(QName qName) {
		namespace = qName.getNamespaceURI();
		localPart = qName.getLocalPart();
	}
	
	public QName getQName() {
		return new QName(namespace, localPart);
	}
	
	public boolean isEmpty() {
		return namespace.equals("") && localPart.equals("");
	}
	
	public Node writeXml(Document document) throws Exception {
		Element elt = document.createElement("schemaDefinition");
		if (namespace.length() > 0) {
			elt.setAttribute("pNamespace", namespace);
		}
		if (localPart.length() > 0) {
			elt.setAttribute("pLocalPart", localPart);
		}
		return elt;
	}

	public void readXml(Node node) throws Exception {
		Element elt = (Element) node;
		namespace = elt.getAttribute("pNamespace");
		if (namespace == null) {
			namespace = "";
		}
		localPart = elt.getAttribute("pLocalPart");
		if (localPart == null) {
			localPart = "";
		}
	}

	@Override
	public String toString() {
		return localPart + "{" + namespace + "}";
	}
}
