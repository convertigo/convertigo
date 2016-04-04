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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

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
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
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
	
	abstract protected void triggerSequence(InternalHttpServletRequest request, JSONArray array) throws EngineException, JSONException;
	
	public void onBulkDocs(HttpServletRequest request, final JSONArray array) {
		final InternalHttpServletRequest internalRequest = new InternalHttpServletRequest(request);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					triggerSequence(internalRequest, array);
				} catch (Exception e) {
					Engine.logBeans.error("Unable to handle 'bulkDocs' event for \""+ getName() +"\" listener", e);
				}
			}
			
		}).start();
		
	}
	
	protected void executeSequence(InternalHttpServletRequest request, JSONArray docs) throws EngineException {
		if (targetSequence == null || targetSequence.isEmpty()) {
			throw new EngineException("No target sequence defined");
		}
		
		if (docs == null) {
			throw new EngineException("Parameter 'docs' is null");
		}
		
		int len = docs.length(); 
		if (len == 0) {
			return;
		}
		
		for (int i = 0; i < len; i++) {
			try {
				CouchKey.c8oHash.remove(docs.getJSONObject(i));
				CouchKey.c8oAcl.remove(docs.getJSONObject(i));
			} catch (JSONException e) {
				throw new EngineException("Incoming documents error", e);
			}
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
			try {
				Map<String, Object> requestParams = new HashMap<String, Object>();
		    	boolean maintainContext = false;
		    	boolean maintainSession = false;
				
		    	requestParams.put(Parameter.Project.getName(), new String[] { projectName });
		    	requestParams.put(Parameter.Sequence.getName(), new String[] { sequenceName });
				if (!maintainContext) requestParams.put(Parameter.RemoveContext.getName(), new String[] { "" });
				if (!maintainSession) requestParams.put(Parameter.RemoveSession.getName(), new String[] { "" });
				//request.put("docs", itemsElement);
				requestParams.put("doc", docList);
				
	    		Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : internal invoke requested");
	        	InternalRequester internalRequester = new InternalRequester(requestParams, request);
	    		Object result = internalRequester.processRequest();
	        	if (result != null) {
	        		Document xmlHttpDocument = (Document) result;
	        		String contents = XMLUtils.prettyPrintDOMWithEncoding(xmlHttpDocument, "UTF-8");
	        		Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : sequence successfully executed with following result\n"+ contents + "\n");
	        	}
			}
			catch (Exception e) {
				throw new EngineException("Sequence named \""+ sequenceName +"\" failed", e);
			}
		} catch (Exception e) {
			throw new EngineException("Unable to execute sequence for \""+ getName() +"\" listener", e);
		}
	}
	
	protected void onDeletedDocs(InternalHttpServletRequest request, JSONArray deletedDocs) throws EngineException, JSONException {		
		int len = deletedDocs.length();
		
		for (int i = 0; i < len;) {
			JSONArray docs = getChunk(deletedDocs, i);
			i += docs.length();
			executeSequence(request, docs);
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
	
	protected void runDocs(InternalHttpServletRequest request, JSONArray rows) throws JSONException, EngineException {
		if (rows != null && rows.length() > 0) {

			// Retrieve the first results
			JSONArray docs = new JSONArray();
			for (int i = 0; i < rows.length(); i++) {
				docs.put(CouchKey.doc.JSONObject(rows.getJSONObject(i)));
			}
			
			executeSequence(request, docs);
		}
	}
	
	public boolean isEnabled() {
		return chunk > 0;
	}
}
