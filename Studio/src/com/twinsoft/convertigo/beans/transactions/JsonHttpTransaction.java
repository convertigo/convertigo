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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class JsonHttpTransaction extends AbstractHttpTransaction {

	private static final long serialVersionUID = 1494278577299328199L;

	Pattern reJSONP = Pattern.compile(".*?((?:\\{|\\[).*(?:\\]|\\})).*?", Pattern.DOTALL);
	
	public JsonHttpTransaction() {
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

		if (httpData == null) {
			Engine.logBeans.debug("No JSON response data, do nothing");
			return;
		}
			
		String jsonData = new String(httpData, jsonEncoding);
		if (Engine.logBeans.isTraceEnabled()) {
			Engine.logBeans.trace("JSON text: " + jsonData);
		} else {
			Engine.logBeans.debug("JSON text (limit 100): " + StringUtils.reduce(jsonData, 100));
		}
		
		Matcher mJSONP = reJSONP.matcher(jsonData);
		if (mJSONP.matches()) {
			jsonData = mJSONP.group(1);
		} else {
			jsonData = jsonData.trim();			
		}
		
		Engine.logBeans.debug("Trimmed JSON part (limit 250): " + StringUtils.reduce(jsonData, 250));

		Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
		
		
		if (!jsonData.isEmpty()) {
			String message = null;
			Object value = null;
			try {
				// Try to find whether the top JSON structure is a JSON object or a
				// JSON array

				// JSON Object
				if (jsonData.startsWith("{")) {
					Engine.logBeans.debug("Detected JSON object");
					value = new JSONObject(jsonData);
				}
				// JSON Array
				else if (jsonData.startsWith("[")) {
					Engine.logBeans.debug("Detected JSON array");
					value = new JSONArray(jsonData);
				}
				else if (jsonData.equals("null")) {
					Engine.logBeans.debug("Detected null JSON");
				}
				else if (jsonData.equals("true")) {
					Engine.logBeans.debug("Detected boolean (true) JSON");
					value = true;
				}
				else if (jsonData.equals("false")) {
					Engine.logBeans.debug("Detected boolean (false) JSON");
					value = false;
				}
				else if (jsonData.startsWith("\"") && jsonData.endsWith("\"")) {
					Engine.logBeans.debug("Detected String JSON");
					value = jsonData.substring(1, jsonData.length() - 1).replace("\\\"", "\"");
				}
				else {
					try {
						value = Long.parseLong(jsonData);
						Engine.logBeans.debug("Detected int JSON");
					} catch (NumberFormatException nfe) {
						try {
							value = Double.parseDouble(jsonData);
							Engine.logBeans.debug("Detected decimal JSON");
						} catch (NumberFormatException nfe2) {						
						}
					}
				}
			} catch (Exception e) {
				message = StringUtils.reduce(e.getMessage(), 50);
			}
			
			if (value != null || jsonData.equals("null")) {
				jsonToXml(value, null, outputDocumentRootElement);
			} else {
				message = "no JSON delimitor [ or {, nor null, boolean, string or number";					
			}
			
			if (message != null) {
				throw new EngineException(
					"Invalid JSON structure: neither a JSON object nor a JSON array;\n" +
					message + 
					";\nanalyzed JSON:\n" + StringUtils.reduce(jsonData, 500)
				);				
			}
		}
	}

	private void jsonToXml(Object object, String objectKey, Element parentElement) throws JSONException {
		Engine.logBeans.trace("Converting JSON to XML: object=" + object + "; objectKey=\""
				+ objectKey + "\"");

		// Normalize object key
		String originalObjectKey = objectKey;
		if (objectKey != null) {
			objectKey = StringUtils.normalize(objectKey);
		}

		// JSON object value case
		if (object instanceof JSONObject) {
			JSONObject json = (JSONObject) object;

			Element element = context.outputDocument.createElement(objectKey == null ? "object" : objectKey);
			if (objectKey != null && !objectKey.equals(originalObjectKey)) {
				element.setAttribute("originalKeyName", originalObjectKey);
			}

			if (jsonArrayTranslationPolicy == JSON_ARRAY_TRANSLATION_POLICY_COMPACT) {
				if (objectKey == null) {
					element = parentElement;
				} else {
					parentElement.appendChild(element);
				}
			} else {
				parentElement.appendChild(element);
			}

			if (includeDataType) {
				element.setAttribute("type", "object");
			}

			Iterator<String> keys = GenericUtils.cast(json.keys());
			while (keys.hasNext()) {
				String key = keys.next();
				jsonToXml(json.get(key), key, element);
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
				if (objectKey != null && !objectKey.equals(originalObjectKey)) {
					arrayElement.setAttribute("originalKeyName", originalObjectKey);
				}
				parentElement.appendChild(arrayElement);

				if (includeDataType) {
					arrayElement.setAttribute("type", "array");
					arrayElement.setAttribute("length", "" + len);
				}
			} else if (jsonArrayTranslationPolicy == JSON_ARRAY_TRANSLATION_POLICY_COMPACT) {
				arrayItemObjectKey = objectKey;
			}

			for (int i = 0; i < len; i++) {
				Object itemArray = array.get(i);
				jsonToXml(itemArray, arrayItemObjectKey, arrayElement);
			}
		}
		else {
			Element element = context.outputDocument.createElement(objectKey == null ? "value" : objectKey);
			if (objectKey != null && !objectKey.equals(originalObjectKey)) {
				element.setAttribute("originalKeyName", originalObjectKey);
			}

			parentElement.appendChild(element);

			if (JSONObject.NULL.equals(object)) {
				object = null;
			}
			
			if (object != null) {
				Text text = context.outputDocument.createTextNode(object.toString());
				element.appendChild(text);
			}
			
			if (includeDataType) {
				String objectType = object == null ? "null":object.getClass().toString();
				if (objectType.startsWith("class java.lang."))
					objectType = objectType.substring(16);
				element.setAttribute("type", objectType.toLowerCase());
			}
		}
		
	}
}