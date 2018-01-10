package com.twinsoft.convertigo.engine.studio.responses;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractResponse {

	public Element toXml(Document document, String qname) throws Exception {
		Element response = document.createElement("response");
		response.setAttribute("state", "success");
		response.setAttribute("name", getClass().getSimpleName());
		if (qname != null) {
			response.setAttribute("qname", qname);
		}

		return response;
	}
}
