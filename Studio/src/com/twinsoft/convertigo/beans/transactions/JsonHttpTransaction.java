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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/beans/transactions/XmlHttpTransaction.java $
 * $Author: julienda $
 * $Revision: 32341 $
 * $Date: 2012-10-30 15:07:42 +0100 (mar., 30 oct. 2012) $
 */

package com.twinsoft.convertigo.beans.transactions;

import java.util.Iterator;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class JsonHttpTransaction extends AbstractHttpTransaction {

	private static final long serialVersionUID = 1494278577299328199L;

	public JsonHttpTransaction()
	{
		super();
	}

	private String jsonEncoding = "UTF-8";
	
	public String getJsonEncoding() {
		return jsonEncoding;
	}

	public void setJsonEncoding(String jsonEncoding) {
		this.jsonEncoding = jsonEncoding;
	}
	
	private boolean includeDataType = true;

	public boolean getIncludeDataType() {
		return includeDataType;
	}

	public void setIncludeDataType(boolean includeDataType) {
		this.includeDataType = includeDataType;
	}
	
	public static final String[] JSON_ARRAY_TRANSLATION_POLICY = { "hierarchical", "compact" };
    public static int JSON_ARRAY_TRANSLATION_POLICY_HIERARCHICAL = 0;
    public static int JSON_ARRAY_TRANSLATION_POLICY_COMPACT = 1;
    
	private int jsonArrayTranslationPolicy = JSON_ARRAY_TRANSLATION_POLICY_HIERARCHICAL;

	public int getJsonArrayTranslationPolicy() {
		return jsonArrayTranslationPolicy;
	}

	public void setJsonArrayTranslationPolicy(int jsonArrayTranslationPolicy) {
		this.jsonArrayTranslationPolicy = jsonArrayTranslationPolicy;
	}

	@Override
	public void makeDocument(byte[] httpData) throws Exception {
		String jsonEncoding = getJsonEncoding();
		Engine.logBeans.trace("JSON encoding : " + jsonEncoding);
		
		String jsonData = new String(httpData, jsonEncoding);
		Engine.logBeans.debug("JSON text: " + jsonData);

    	Element outputDocumentRootElement = context.outputDocument.getDocumentElement();

    	try {
    		// Try to find whether the top JSON structure is a JSON object or a JSON array
        	char firstToken = jsonData.trim().charAt(0);

        	// JSON Object
        	if (firstToken == '{') {
            	JSONObject jsonObject = new JSONObject(jsonData);
   				jsonToXml(jsonObject, null, outputDocumentRootElement);
        	}
        	// JSON Array
        	else if (firstToken == '[') {
            	JSONArray jsonArray = new JSONArray(jsonData);
    			jsonToXml(jsonArray, null, outputDocumentRootElement);
        	}
        	else {
    			throw new EngineException("Invalid JSON data: not a JSON object neither a JSON array; JSON analyzed:\n" + jsonData);
        	}
		} catch (IndexOutOfBoundsException  e) {
			// This is an empty JSON data
			// Just ignore
		}
    }

	private void jsonToXml(Object object, String objectKey, Element parentElement) throws JSONException {
		// String, numeric, boolean value case
		if (object instanceof Character || object instanceof String ||
				object instanceof Byte || object instanceof Integer ||
				object instanceof Float || object instanceof Double ||
				object instanceof Boolean) {
			Element element = context.outputDocument.createElement(objectKey == null ? "value" : objectKey);
			parentElement.appendChild(element);
			
			Text text = context.outputDocument.createTextNode(object.toString());
			element.appendChild(text);

			if (includeDataType) {
				String objectType = object.getClass().toString();
				if (objectType.startsWith("class java.lang."))
					objectType = objectType.substring(16);
				element.setAttribute("type", objectType.toLowerCase());
			}
		}
		// Array value case
		else if (object instanceof JSONArray) {
			JSONArray array = (JSONArray) object;
			int len = array.length();

			Element arrayElement = parentElement;
			String arrayItemObjectKey = null;
			if (jsonArrayTranslationPolicy == JSON_ARRAY_TRANSLATION_POLICY_HIERARCHICAL) {
				arrayElement = context.outputDocument.createElement(objectKey == null ? "array" : objectKey);
				parentElement.appendChild(arrayElement);

				if (includeDataType) {
					arrayElement.setAttribute("type", "array");
					arrayElement.setAttribute("length",  "" + len);
				}
			}
			else if (jsonArrayTranslationPolicy == JSON_ARRAY_TRANSLATION_POLICY_COMPACT) {
				// Nothing to do
				arrayItemObjectKey = objectKey;
			}
			
			for (int i = 0; i < len; i++) {
				Object itemArray = array.get(i);
				jsonToXml(itemArray, arrayItemObjectKey, arrayElement);				
			}
		}
		// JSON object value case
		else if (object instanceof JSONObject) {
			JSONObject json = (JSONObject) object;

			Element element = context.outputDocument.createElement(objectKey == null ? "object" : objectKey);
			parentElement.appendChild(element);
			
			if (includeDataType) {
				element.setAttribute("type", "object");
			}
			
			Iterator<String> keys = GenericUtils.cast(json.keys());
			while (keys.hasNext()) {
				String key = keys.next();
				jsonToXml(json.get(key), key, element);
			}
		}
	}
}

