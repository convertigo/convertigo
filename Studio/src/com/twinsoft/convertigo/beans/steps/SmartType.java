package com.twinsoft.convertigo.beans.steps;

import java.io.Serializable;

import org.apache.xpath.XPathAPI;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XMLizable;
import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class SmartType implements XMLizable, Serializable {
	private static final long serialVersionUID = 6063228569094166129L;
	
	enum Types {
		plain,
		js,
		source
	}
	
	private Types type = Types.plain;
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private String expression = "";

	public void readXml(Node node) throws Exception {
        expression = XPathAPI.selectSingleNode(node, "./expression/text()").getNodeValue();
        String sType = XPathAPI.selectSingleNode(node, "./type/text()").getNodeValue();
        type = Types.valueOf(sType);
        sourceDefinition.readXml(XPathAPI.selectSingleNode(node, "./sourceDefinition"));
	}

	public Node writeXml(Document document) throws Exception {
		Element self = document.createElement(getClass().getName());
		Element sub = document.createElement("expression");
		sub.setTextContent(expression);
		sub = document.createElement("source");
		sub.setTextContent(type.name());
		sub = document.createElement("sourceDefinition");
		sub.appendChild(sourceDefinition.writeXml(document));
		return self;
	}
	
	public String getSingleString(DatabaseObject owner, Context javascriptContext, Scriptable scope) {
		String result;
		switch (type) {
		case plain:
			result = expression;
			break;
		default:
			result = expression;
			break;
		}
		return result;
	}
}
