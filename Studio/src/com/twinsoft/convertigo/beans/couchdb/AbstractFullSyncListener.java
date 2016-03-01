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

package com.twinsoft.convertigo.beans.couchdb;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.xpath.XPathAPI;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractFullSyncListener extends Listener {

	private static final long serialVersionUID = -7580433107225235685L;
	private String targetSequence = "";
	private int chunk = 10;
	
	public AbstractFullSyncListener() {
		super();
	}

	@Override
	public AbstractFullSyncListener clone() throws CloneNotSupportedException {
		AbstractFullSyncListener clonedObject =  (AbstractFullSyncListener) super.clone();
		return clonedObject;
	}

	@Override
	public String getRenderer() {
		return "FullSyncListenerTreeObject";
	}
	
	@Override
	public FullSyncConnector getConnector() {
		return (FullSyncConnector) super.getConnector();
	}
	
	public String getTargetSequence() {
		return targetSequence;
	}

	public void setTargetSequence(String targetSequence) {
		this.targetSequence = targetSequence;
	}

	public int getChunk() {
		return chunk;
	}

	public void setChunk(int chunk) {
		this.chunk = chunk;
	}	
	
	public CouchClient getCouchClient() {
		return getConnector().getCouchClient();
	}
	
	public String getDatabaseName() {
		return getConnector().getDatabaseName();
	}
	
	abstract protected void triggerSequence(JSONArray array) throws EngineException, JSONException;
	
	public void onBulkDocs(final JSONArray array) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					triggerSequence(array);
				} catch (Exception e) {
					Engine.logBeans.error("Unable to handle 'bulkDocs' event for \""+ getName() +"\" listener", e);
				}
			}
			
		}).start();
		
	}
	
	protected void executeSequence(JSONArray docs, boolean isInternalRequest) throws EngineException {
		if (targetSequence == null || targetSequence.isEmpty()) {
			throw new EngineException("No target sequence defined");
		}
		
		if (docs == null) {
			throw new EngineException("Parameter 'docs' is null");
		}
		
		if (docs.length() == 0) {
			return;
		}
		
		try {
			Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element itemsElement = document.createElement("items");
			XMLUtils.JsonToXml(docs, itemsElement);
			NodeList docList = XPathAPI.selectNodeList(itemsElement, "item");
			
			StringTokenizer st = new StringTokenizer(getTargetSequence(),".");
			String projectName = st.nextToken();
			String sequenceName = st.nextToken();
		
			Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : execute sequence \""+sequenceName+"\"");
			if (isInternalRequest) {
				try {
					Map<String, Object> request = new HashMap<String, Object>();
			    	boolean maintainContext = false;
			    	boolean maintainSession = false;
					
			    	request.put(Parameter.Project.getName(), new String[] { projectName });
			    	request.put(Parameter.Sequence.getName(), new String[] { sequenceName });
			    	request.put(Parameter.Context.getName(), new String[] { "" });
			    	request.put(Parameter.SessionId.getName(), new String[] { "" });
					if (!maintainContext) request.put(Parameter.RemoveContext.getName(), new String[] { "" });
					if (!maintainSession) request.put(Parameter.RemoveSession.getName(), new String[] { "" });
					//request.put("docs", itemsElement);
					request.put("doc", docList);
					
		    		Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : internal invoke requested");
		        	InternalRequester internalRequester = new InternalRequester();
		        	internalRequester.setStrictMode(getProject().isStrictMode());
		        	
		        	internalRequester.inputData = request;
		    		Object result = internalRequester.processRequest(request);
		        	if (result != null) {
		        		Document xmlHttpDocument = (Document) result;
		        		String contents = XMLUtils.prettyPrintDOMWithEncoding(xmlHttpDocument, "UTF-8");
		        		Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : sequence successfully executed with following result\n"+ contents + "\n");
		        	}
				}
				catch (Exception e) {
					throw new EngineException("Sequence named \""+ sequenceName +"\" failed", e);
				}
			}
			else {
				String targetUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
				targetUrl += (targetUrl.endsWith("/") ? "":"/") + "projects/"+ projectName + "/.xml";
	
				PostMethod postMethod = null;
				try {
					URL url = new URL(targetUrl);
					HostConfiguration hostConfiguration = new HostConfiguration();
					hostConfiguration.setHost(url.getHost());
					HttpState httpState = new HttpState();
					
					postMethod = new PostMethod(targetUrl);
					postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
					postMethod.addParameter(Parameter.Sequence.getName(), sequenceName);
					postMethod.addParameter("__handleComplex", "true");
					//postMethod.addParameter("docs", XMLUtils.prettyPrintElement(itemsElement, true, false));
					for (int i=0; i< docList.getLength(); i++) {
						Element itemElement = (Element) docList.item(i);
						postMethod.addParameter("doc", XMLUtils.prettyPrintElement(itemElement, true, false));
					}
					
					Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : http invoke requested");
					int statusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, postMethod, httpState);
					
					if (statusCode != -1) {
						String contents = postMethod.getResponseBodyAsString();
						Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : sequence successfully executed with following result\n"+ contents + "\n");
					}
				} catch (Exception e) {
					throw new EngineException("Sequence named \""+ sequenceName +"\" failed", e);
				} finally {
					if (postMethod != null) {
						postMethod.releaseConnection();
					}
				}
				
			}
		} catch (Exception e) {
			throw new EngineException("Unable to execute sequence for \""+ getName() +"\" listener", e);
		}
	}
	
	protected void onDeletedDocs(JSONArray deletedDocs) throws EngineException, JSONException {		
		int len = deletedDocs.length();
		
		for (int i = 0; i < len;) {
			JSONArray docs = getChunk(deletedDocs, i);
			i += docs.length();
			executeSequence(docs, false);
		}
	}
	
	protected JSONArray getDeletedDocs(JSONArray rows) throws JSONException {
		if (rows.length() > 0) {
			JSONArray deletedDocs = new JSONArray();
			for (int i = 0; i < rows.length(); i++) {
				JSONObject doc = CouchKey.doc.JSONObject(rows.getJSONObject(i));
				deletedDocs.put(doc);
			}
			return deletedDocs;
		}
		return null;
	}
	
	protected JSONArray getChunk(JSONArray array, int offset) throws JSONException {
		if (offset == 0 && array.length() < chunk) {
			return array;
		}
		int limit = Math.min(array.length(), offset + chunk);
		JSONArray sub = new JSONArray();
		for (int i = offset; i < limit; i++) {
			sub.put(array.get(i));
		}
		return sub;
	}
	
	protected void runDocs(JSONArray rows) throws JSONException, EngineException {
		if (rows != null && rows.length() > 0) {

			// Retrieve the first results
			JSONArray docs = new JSONArray();
			for (int i = 0; i < rows.length(); i++) {
				docs.put(CouchKey.doc.JSONObject(rows.getJSONObject(i)));
			}
			
			executeSequence(docs, false); // true: internal requester, false: http requester
		}
	}
	
	public boolean isEnabled() {
		return chunk > 0;
	}
}
