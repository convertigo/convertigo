package com.twinsoft.convertigo.engine.studio.responses;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;

public class SetPropertyResponse extends AbstractResponse {

	private String propertyName;

	public SetPropertyResponse(String propertyName) {
		super();
		this.propertyName = propertyName;
	}

	@Override
	public Element toXml(Document document, String qname) throws Exception {
		// Create property
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		Element property = dbo.toXml(document, propertyName);

		// If we update the source definition, the display name of the dbo will change so we need it
		if (propertyName == "sourceDefinition") {
		    property.setAttribute("name", dbo.toString());
		}

		// Create response
		Element response = super.toXml(document, qname);
		response.setAttribute("message", "Property \"" + propertyName + "\" has been successfully updated.");
		response.appendChild(property);

		return response;
	}
}
