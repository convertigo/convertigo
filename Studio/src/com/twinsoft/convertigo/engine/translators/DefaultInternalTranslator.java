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

	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logContext.debug("Making input document");

		Map<String, Object> request = GenericUtils.cast(inputData);
		
		InputDocumentBuilder inputDocumentBuilder = new InputDocumentBuilder(context);
        
		// We transform the HTTP post data into XML data.
		for (Entry<String, Object> entry : request.entrySet()) {
			String parameterName = entry.getKey();
			Object parameterObject = entry.getValue();
			String[] parameterValues = null;  
			String parameterValue = null;
			if (parameterObject instanceof String[]) {
				parameterValues = (String[]) parameterObject;
				
				if (parameterValues.length > 0) {
					parameterValue = parameterValues[0];
				}
			} else if (parameterObject instanceof String) {
				parameterValue = (String) parameterObject;
			}
			
			if (!inputDocumentBuilder.handleSpecialParameter(parameterName, parameterValues != null ? parameterValues : new String[] { parameterValue })) {
				if (parameterValues == null) {
					addParameterObject(context.inputDocument, inputDocumentBuilder.transactionVariablesElement, parameterName, parameterObject);
				} else {
					inputDocumentBuilder.addVariable(parameterName, parameterValues);
				}
			};
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
			Node node = doc.importNode((Node) parameterObject, true);
			Element item = doc.createElement("variable");
			item.setAttribute("name", parameterName);
			if (node.getNodeType() == Node.TEXT_NODE) {
				item.setAttribute("value", node.getNodeValue());
			} else {
				NodeList nl = node.getChildNodes();
				if (nl.getLength() == 1 && nl.item(0).getNodeType() == Node.TEXT_NODE) {
					item.setAttribute("value", nl.item(0).getNodeValue());
				} else {
					while (nl.getLength() > 0) {
						item.appendChild(node.removeChild(nl.item(0)));
					}
				}
			}
			parentItem.appendChild(item);
		} else if (parameterObject instanceof NodeList) {
			NodeList nl = (NodeList) parameterObject;
			for (int i = 0 ; i < nl.getLength() ; i++) {
				addParameterObject(doc, parentItem, parameterName, nl.item(i));
			}
		} else if (parameterObject instanceof XMLVector) {
			XMLVector<Object> values = GenericUtils.cast(parameterObject);
			for (Object object : values) {
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
		throw new EngineException("The DefaultServletTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The DefaultServletTranslator translator does not support the getProjectName() method");
	}

}