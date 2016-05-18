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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

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
				XMLUtils.jsonToXml(value, null, outputDocumentRootElement, includeDataType, jsonArrayTranslationPolicy == JSON_ARRAY_TRANSLATION_POLICY_COMPACT ? null : "object");
			} else if (message == null) {
				message = "no JSON delimitor [ or {, nor null, boolean, string or number";					
			}
			
			if (message != null) {
				throw new EngineException(
					"Invalid JSON value:\n" +
					message + 
					";\nanalyzed JSON:\n" + StringUtils.reduce(jsonData, 500)
				);				
			}
		}
	}
}