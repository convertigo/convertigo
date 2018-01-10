package com.twinsoft.convertigo.engine.studio.responses;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;

public class MessageBoxResponse extends AbstractResponse {

	private String message;

	public MessageBoxResponse(String message) {
		super();
		this.message = message;
	}

	@Override
	public Element toXml(Document document, String qname) throws Exception {
		// Create dialog
		Element messageBox = document.createElement("messageBox");
		messageBox.appendChild(DOMUtils.createElementWithText(document, "title", "Convertigo"));
		messageBox.appendChild(DOMUtils.createElementWithText(document, "message", message));

		// Create response
		Element response = super.toXml(document, qname);
		response.appendChild(messageBox);

		return response;
	}
}
