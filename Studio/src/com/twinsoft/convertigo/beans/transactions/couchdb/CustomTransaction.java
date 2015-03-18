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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.net.URI;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;

public class CustomTransaction extends AbstractCouchDbTransaction {

	private static final long serialVersionUID = 8529770803364008729L;

	private transient Object eUrl = null;
	private transient Object eData = null;
	
	public CustomTransaction() {
		super();
	}

	@Override
	public CustomTransaction clone() throws CloneNotSupportedException {
		CustomTransaction clonedObject =  (CustomTransaction) super.clone();
		clonedObject.eUrl = null;
		clonedObject.eData = null;
		return clonedObject;
	}
	
    /** Holds value of property httpVerb. */
    private HttpMethodType httpVerb = HttpMethodType.GET;
    
    public HttpMethodType getHttpVerb() {
		return httpVerb;
	}

	public void setHttpVerb(HttpMethodType httpVerb) {
		this.httpVerb = httpVerb;
	}
	
	private String subUrl = "\"\"";
	
	public String getSubUrl() {
		return subUrl;
	}

	public void setSubUrl(String subUrl) {
		this.subUrl = subUrl;
	}

	private String httpData = "";
	
	public String getHttpData() {
		return httpData;
	}

	public void setHttpData(String httpData) {
		this.httpData = httpData;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return getDeclaredParameters(CouchDbParameter.empty);
	}

	@Override
	protected Object invoke() throws Exception {
		CouchClient provider = getCouchClient();
		
		String evaluatedUrl = eUrl == null ? "" : eUrl.toString();
		
		if (!evaluatedUrl.startsWith("/")) {
			evaluatedUrl = '/' + evaluatedUrl;
		}
		
		String db = getConnector().getDatabaseName();
		URI uri = new URI(provider.getDatabaseUrl(db) + evaluatedUrl);
		Engine.logBeans.debug("(CustomTransaction) CouchDb request uri: "+ uri.toString());
		
		String jsonString = null;
		Object jsond = toJson(eData);
		if (jsond != null) {
			JSONObject jsonData;
			
			if (jsond instanceof JSONObject) { // comes from a complex variable
				jsonData = (JSONObject) jsond;
			} else {
				jsonData = new JSONObject();
				jsonData.put("data", jsond);
			}
			jsonString = jsonData.toString();
			Engine.logBeans.debug("(CustomTransaction) CouchDb request data: "+ jsonString);
		}
		
		HttpRequestBase request = getHttpVerb().newInstance();
		
		if (request == null) {
			throw new EngineException("Unsupported HTTP method");
		}
		
		request.setURI(uri);
		
		if (jsonString != null && request instanceof HttpEntityEnclosingRequest) {
			provider.setJsonEntity((HttpEntityEnclosingRequest) request, jsonString);
		}
		
		return provider.execute(request);
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "customType");
	}
	
	@Override
	public void prepareForRequestable(Context context, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		super.prepareForRequestable(context, javascriptContext, scope);
		evaluateUrl(context, javascriptContext, scope);
		evaluateData(context, javascriptContext, scope);
	}

	private void evaluateUrl(Context context, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		try {
			eUrl = javascriptContext.evaluateString(scope, getSubUrl(), "subUrl", 1, null);
			if (eUrl instanceof org.mozilla.javascript.Undefined) {
				eUrl = null;
			}
		}
		catch(EcmaError e) {
			EngineException ee = new EngineException(
				"Unable to evaluate CouchDb relative URL.\n" +
				"CustomTransaction: \"" + getName() + "\"\n" +
				"A Javascript error has occured at line " + e.lineNumber() + ", column " + e.columnNumber() + ": " +
				e.getMessage() + "\n" +
				e.lineSource()
			);
			throw ee;
		}
		catch(EvaluatorException e) {
			EngineException ee = new EngineException(
				"Unable to evaluate CouchDb relative URL.\n" +
				"CustomTransaction: \"" + getName() + "\"\n" +
				"A Javascript evaluation error has occured: " + e.getMessage()
			);
			throw ee;
		}
		catch(JavaScriptException e) {
			throw new EngineException(
				"Unable to evaluate CouchDb relative URL.\n" +
				"CustomTransaction: \"" + getName() + "\"\n" +
				"Cause: " + e.getMessage()
			);
		}
	}

	private void evaluateData(Context context, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		try {
			eData = javascriptContext.evaluateString(scope, getHttpData(), "httpData", 1, null);
			if (eData instanceof org.mozilla.javascript.Undefined) {
				eData = null;
			}
		}
		catch(EcmaError e) {
			EngineException ee = new EngineException(
				"Unable to evaluate CouchDb http data.\n" +
				"CustomTransaction: \"" + getName() + "\"\n" +
				"A Javascript error has occured at line " + e.lineNumber() + ", column " + e.columnNumber() + ": " +
				e.getMessage() + "\n" +
				e.lineSource()
			);
			throw ee;
		}
		catch(EvaluatorException e) {
			EngineException ee = new EngineException(
				"Unable to evaluate CouchDb http data.\n" +
				"CustomTransaction: \"" + getName() + "\"\n" +
				"A Javascript evaluation error has occured: " + e.getMessage()
			);
			throw ee;
		}
		catch(JavaScriptException e) {
			throw new EngineException(
				"Unable to evaluate CouchDb http data.\n" +
				"CustomTransaction: \"" + getName() + "\"\n" +
				"Cause: " + e.getMessage()
			);
		}
	}
}
