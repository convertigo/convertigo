package com.twinsoft.convertigo.engine.studio.responses.popup.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public class DatabaseObjectDeleteActionResponse extends AbstractResponse {

	private boolean doDelete;
	
	public DatabaseObjectDeleteActionResponse(boolean doDelete) {
		this.doDelete = doDelete;
	}
	
	@Override
	public Element toXml(Document document, String qname) throws Exception {
		Element response =  super.toXml(document, qname);
		response.setAttribute("doDelete", Boolean.toString(doDelete));
		return response;
	}

}
