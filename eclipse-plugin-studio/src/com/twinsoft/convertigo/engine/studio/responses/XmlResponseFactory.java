package com.twinsoft.convertigo.engine.studio.responses;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlResponseFactory {

	private XmlResponseFactory() {
	}

	public static Element createMessageBoxResponse(Document document, String qname, String message) throws Exception {
		return new MessageBoxResponse(message).toXml(document, qname);
	}

	public static Element createMessageDialogResponse(Document document, String qname, String title, String message, String[] buttons) throws Exception {
		return new MessageDialogResponse(title, message, buttons).toXml(document, qname);
	}
}
