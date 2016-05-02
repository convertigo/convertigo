/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.translators;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.NativeJavaObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DefaultInternalTranslator implements Translator {

	boolean bStrictMode = false;
	
	public void setStrictMode(boolean strictMode) {
		this.bStrictMode = strictMode;
	}
	
	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logContext.debug("Making input document");

		Map<String, Object> request = GenericUtils.cast(inputData);
		
		InputDocumentBuilder inputDocumentBuilder = new InputDocumentBuilder(context);
		
		// Indicates whether variable values were generated using strict mode or nor(text/childs only)
		inputDocumentBuilder.transactionVariablesElement.setAttribute("strictMode", Boolean.toString(bStrictMode));
		
		for (Entry<String, Object> entry : request.entrySet()) {
			String parameterName = entry.getKey();
			Object parameterObject = entry.getValue();

			if (!inputDocumentBuilder.handleSpecialParameter(parameterName, parameterObject)) {
				if (parameterObject instanceof String[]) {
					inputDocumentBuilder.addVariable(parameterName, (String[]) parameterObject);
				} else {
					addParameterObject(context.inputDocument,
							inputDocumentBuilder.transactionVariablesElement, parameterName, parameterObject);
				}
			}
		}

		Engine.logContext.info("Input document created");
    }
	
	private void addParameterObject(Document doc, Node parentItem, String parameterName, Object parameterObject) {
		if (parameterObject instanceof NativeJavaObject) {
			parameterObject = ((NativeJavaObject) parameterObject).unwrap();
		}
		
		if (parameterObject.getClass().isArray()) {
			int len = Array.getLength(parameterObject);
			for (int i = 0 ; i < len ; i++) {
				Object o = Array.get(parameterObject, i);
				if (o != null) {
					addParameterObject(doc, parentItem, parameterName, o);
				}
			}
		} else if (parameterObject instanceof Node) {
			Node node = (Node) parameterObject;
			Element item = doc.createElement("variable");
			item.setAttribute("name", parameterName);
			
			if (bStrictMode) { // append full structured node
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					item.appendChild(doc.importNode(node, true));
				}
				else {
					item.setAttribute("value", node.getNodeValue());
				}
			}
			else { // append only child nodes for a structured node
				if (node.getNodeType() == Node.TEXT_NODE) {
					item.setAttribute("value", node.getNodeValue());
				} else {
					NodeList nl = node.getChildNodes();
					if (nl.getLength() == 1 && nl.item(0).getNodeType() == Node.TEXT_NODE) {
						item.setAttribute("value", nl.item(0).getNodeValue());
					} else {
						nl = doc.importNode(node, true).getChildNodes();
						while (nl.getLength() > 0) {
							item.appendChild(node.removeChild(nl.item(0)));
						}
					}
				}
			}
			parentItem.appendChild(item);
		} else if (parameterObject instanceof NodeList) {
			NodeList nl = (NodeList) parameterObject;
			int len = nl.getLength();
			if (bStrictMode) {
				Element item = doc.createElement("variable");
				item.setAttribute("name", parameterName);

				for (int i = 0; i < len; i++) {
					Node node = nl.item(i);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						item.appendChild(doc.importNode(node, true));
					} else {
						item.setAttribute("value", node.getNodeValue());
					}
				}
				parentItem.appendChild(item);
			} else {
				for (int i = 0; i < len; i++) {
					addParameterObject(doc, parentItem, parameterName, nl.item(i));
				}
			}
		} else if (parameterObject instanceof XMLVector) {
			XMLVector<Object> values = GenericUtils.cast(parameterObject);
			for (Object object : values) {
				addParameterObject(doc, parentItem, parameterName, object);
			}
		} else if (parameterObject instanceof List) {
			List<Object> list = GenericUtils.cast(parameterObject);
			for (Object object : list) {
				addParameterObject(doc, parentItem, parameterName, object);
			}
		} else {
			Element item = doc.createElement("variable");
			item.setAttribute("name", parameterName);
			item.setAttribute("value", parameterObject.toString());
			parentItem.appendChild(item);
		}
	}

	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
		if (convertigoResponse instanceof String) {
			String encodingCharSet = "UTF-8";
			if (context.requestedObject != null) {
				encodingCharSet = context.requestedObject.getEncodingCharSet();
			}
			return ((String) convertigoResponse).getBytes(encodingCharSet);
		}
		return convertigoResponse;
	}

	public String getContextName(byte[] data) throws Exception {
		throw new EngineException("The DefaultInternalTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The DefaultInternalTranslator translator does not support the getProjectName() method");
	}

}