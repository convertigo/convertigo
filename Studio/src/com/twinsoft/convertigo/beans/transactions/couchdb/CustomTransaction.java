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

import static com.twinsoft.convertigo.engine.providers.couchdb.util.URIBuilder.buildUri;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpMethod;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbProvider;
import com.twinsoft.convertigo.engine.providers.couchdb.util.URIBuilder;
import com.twinsoft.convertigo.engine.util.GenericUtils;

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
    private int httpVerb = 0;
    
    public int getHttpVerb() {
		return httpVerb;
	}

	public void setHttpVerb(int httpVerb) {
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
		return Arrays.asList(new CouchDbParameter[] {});
	}

	@Override
	protected Object invoke() throws Exception {
		CouchDbProvider provider = getCouchDbClient();
		
		String evaluatedUrl = eUrl == null ? "":eUrl.toString();
		String[] parts = evaluatedUrl.split("\\?");
		String requestPath = parts.length > 0 ? parts[0]:evaluatedUrl;
		String requestQuery = parts.length > 1 ? parts[1]:"";
		
		URIBuilder builder = buildUri(getCouchDbContext().getBaseUri());
		builder.path(requestPath);
		builder.query(requestQuery);
		URI uri = builder.build();
		Engine.logBeans.debug("(CustomTransaction) CouchDb request uri: "+ uri.toString());
		
		String jsonString = null;
		JsonElement jsond = toJson(getGson(), new JsonParser(), eData);
		if (jsond != null) {
			JsonObject jsonData = new JsonObject();
			if (jsond instanceof JsonObject) { // comes from a complex variable
				JsonObject jsonObject = jsond.getAsJsonObject();
				Set<Entry<String, JsonElement>> set = jsonObject.entrySet();
				for (Iterator<Entry<String, JsonElement>> it = GenericUtils.cast(set.iterator()); it.hasNext();) {
					Entry<String, JsonElement> entry = it.next();
					jsonData.add(entry.getKey(), entry.getValue());
				}
			}
			else {
				jsonData.add("data", jsond);
			}
			jsonString = encode(jsonData.toString());
			Engine.logBeans.debug("(CustomTransaction) CouchDb request data: "+ jsonString);
		}
		
		HttpMethod httpMethod = null;
		int httpVerb = getHttpVerb();
		if (httpVerb == AbstractHttpTransaction.HTTP_VERB_GET) {
			httpMethod = provider.getMethod(uri);
		}
		else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_POST) {
			if (jsonString != null)
				httpMethod = provider.postMethod(uri, jsonString);
			else
				httpMethod = provider.postMethod(uri);
		}
		else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_PUT) {
			if (jsonString != null)
				httpMethod = provider.putMethod(uri, jsonString);
			else
				httpMethod = provider.putMethod(uri);
		}
		else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_DELETE) {
			httpMethod = provider.deleteMethod(uri);
		}
		else if (httpVerb == AbstractHttpTransaction.HTTP_VERB_HEAD) {
			httpMethod = provider.headMethod(uri);
		}
		else {
			throw new EngineException("Unsupported HTTP method");
		}
		
		return provider.execute(httpMethod).toJson();
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
