/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.requesters;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.util.JsonNativeOutput;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class JsonServletRequester extends ServletRequester {

    public JsonServletRequester() {
    }

    @Override
    public String getName() {
        return "JsonServletRequester";
    }

	public void setStyleSheet(Document document) {
        return;
    }
	
	@Override
	public boolean acceptNativeJson() {
		return context.project == null || context.project.getJsonOutput() == JsonOutput.useType
				&& context.project.getJsonRoot() == JsonRoot.docChildNodes;
	}

	@Override
	public Object postGetNativeJson() throws Exception { return JsonResponse.of(context, null); }

	@Override
	public Object postGetDocument(Document document) throws Exception {
		return toJson(context, document);
	}

	/** the JSON answer of a document: the native JSON outputs of the sequence are written as is beside its XML */
	public static Object toJson(Context context, Document document) throws Exception {
		if (document == null) {
			// a native sequence without XML: its outputs only
			Map<String, JsonNativeOutput> nativeOutputs = context.jsonNativeOutputs;
			context.jsonNativeOutputs = null;
			if (nativeOutputs == null || nativeOutputs.isEmpty()) {
				return "{}";
			}
			String charset = context.requestedObject != null ? context.requestedObject.getEncodingCharSet() : "UTF-8";
			Object json = XMLUtils.XmlToJsonResponse(null, true, nativeOutputs.values(), charset);
			if (json != null) {
				return json;
			}
			context.jsonNativeOutputs = nativeOutputs;
			document = GenericRequester.nativeOutputDocument(context);
		}
		ConvertigoError.cleanDocument(context.getXpathApi(), document);
		var useType = context.isDefaultProjectAnswer || context.project != null && context.project.getJsonOutput() == JsonOutput.useType;
		var jsonRoot = context.isDefaultProjectAnswer ? JsonRoot.docChildNodes
				: context.project != null ? context.project.getJsonRoot() : JsonRoot.docNode;
		Map<String, JsonNativeOutput> nativeOutputs = context.jsonNativeOutputs;
		context.jsonNativeOutputs = null;
		if (nativeOutputs != null && !nativeOutputs.isEmpty() && !context.isErrorDocument) {
			if (useType && JsonRoot.docChildNodes.equals(jsonRoot)) {
				// a String, or a response written in stream by the servlet (JsonStreamedResponse)
				String charset = context.requestedObject != null ? context.requestedObject.getEncodingCharSet() : "UTF-8";
				Object json = XMLUtils.XmlToJsonResponse(document.getDocumentElement(), true, nativeOutputs.values(), charset);
				if (json != null) {
					return json;
				}
			}
			// a key is in both: the kept JSON goes back to XML, as without Sequence.jsonNativeOutput
			for (JsonNativeOutput nativeOutput : nativeOutputs.values()) {
				document.getDocumentElement().appendChild(nativeOutput.toElement(document));
			}
		}
		return XMLUtils.XmlToJson(document.getDocumentElement(), true, useType, jsonRoot);
	}
	
	protected Object addStatisticsAsData(Object result) { 
		return result; 
	} 
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{ 
		return result; 
	} 
}
