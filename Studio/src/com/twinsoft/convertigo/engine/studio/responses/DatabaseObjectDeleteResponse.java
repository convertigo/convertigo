package com.twinsoft.convertigo.engine.studio.responses;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DatabaseObjectDeleteResponse extends AbstractResponse {

	private boolean doDelete;
	
	public DatabaseObjectDeleteResponse(boolean doDelete) {
		this.doDelete = doDelete;
	}
	
	@Override
	public Element toXml(Document document, String qname) throws Exception {
		Element response =  super.toXml(document, qname);
		response.setAttribute("doDelete", Boolean.toString(doDelete));
		return response;
	}

}
