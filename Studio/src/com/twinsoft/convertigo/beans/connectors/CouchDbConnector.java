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

package com.twinsoft.convertigo.beans.connectors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.JsonDocument;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractDatabaseTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetViewTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;

public class CouchDbConnector extends Connector {

	private static final long serialVersionUID = -8895252401444085569L;

	private String databaseName = "";
	private String server = "127.0.0.1";
	private int port = 5984;
	private boolean https = false;
	private String couchUsername = "";
	private String couchPassword = "";
	private boolean jsonUseType = true;
	
	private transient CouchClient couchClient = null;
	
	public CouchDbConnector() {
		
	}

	@Override
	public Connector clone() throws CloneNotSupportedException {
		CouchDbConnector clonedObject = (CouchDbConnector) super.clone();
		clonedObject.couchClient = null;
		return clonedObject;
	}

	/**
	 * @return the databaseName
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @param databaseName the databaseName to set
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
	
	/**
	 * @return the server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * @param server the server to set
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the https
	 */
	public boolean isHttps() {
		return https;
	}

	/**
	 * @param https the https to set
	 */
	public void setHttps(boolean https) {
		this.https = https;
	}
	
	public String getCouchUsername() {
		return couchUsername;
	}

	public void setCouchUsername(String couchUsername) {
		this.couchUsername = couchUsername;
	}

	public String getCouchPassword() {
		return couchPassword;
	}

	public void setCouchPassword(String couchPassword) {
		this.couchPassword = couchPassword;
	}

	@Override
	public void release() {
		couchClient = null;
		super.release();
		//TODO: release
	}
	
	public CouchClient getCouchClient() {
		if (couchClient == null) {
			if (!isOriginal()) {
				couchClient = getOriginal().getCouchClient(); 
			} else {
				String url = isHttps() ? "https" : "http";
				url+= "://" + getServer() + ":" + getPort();
				couchClient = new CouchClient(url, couchUsername, couchPassword);
			}
		}
		return couchClient;
	}
	
	public void setCouchClient(CouchClient couchClient) {
		this.couchClient = couchClient;
	}
	
	public void setData(Object data) {
		fireDataChanged(new ConnectorEvent(this, data));
	}
	
	@Override
	public CouchDbConnector getOriginal() {
		return (CouchDbConnector) super.getOriginal();
	}
	
	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		
	}
	
	public String getTargetDatabase(AbstractDatabaseTransaction couchDbTransaction) throws EngineException {
		String targetDbName = couchDbTransaction.getParameterStringValue(CouchParam.db);
				
		if (targetDbName == null || targetDbName.isEmpty()) {
			targetDbName = getDatabaseName();
		}
		
		return targetDbName;
	}

	@Override
	public Transaction newTransaction() {
		return null;
	}

	public static final String internalView = "_Internal_GetView_";
	private transient GetViewTransaction internalViewTransaction = null;
	GetViewTransaction getInternalViewTransaction() {
		if (internalViewTransaction == null) {
			try {
				internalViewTransaction = new GetViewTransaction();
				internalViewTransaction.setName(internalView);
				internalViewTransaction.setQ_limit("50");
				RequestableVariable var_reduce = new RequestableVariable();
				var_reduce.setName(CouchParam.prefix + "reduce");
				var_reduce.setValueOrNull("true");
				internalViewTransaction.add(var_reduce);
				internalViewTransaction.setParent(this);
			} catch (EngineException e) {}
		}
		return internalViewTransaction;
	}
	
	@Override
	public Transaction getTransactionByName(String transactionName) {
		Transaction Transaction = super.getTransactionByName(transactionName);
		if (Transaction == null && internalView.equals(transactionName)) {
			return getInternalViewTransaction();
		}
		return Transaction;
	}

	@Override
	public void removeDocument(Document document) throws EngineException {
		JSONObject jsonDocument = ((JsonDocument)document).getJSONObject();
		String docid = CouchKey._id.String(jsonDocument);
		if (docid != null) {
			String rev = CouchKey._rev.String(jsonDocument);
			if (rev == null) {
				getCouchClient().deleteDocument(getDatabaseName(), docid, (Map<String, String>) null);
			}
			else {
				getCouchClient().deleteDocument(getDatabaseName(), docid, rev);
			}
		}
		
		super.removeDocument(document);
		
	}

	public void importCouchDbDesignDocuments() {
		List<String> list = getCouchDbDesignDocuments();
		
		for (String jsonString : list) {
			try {
				JSONObject jsonDocument = new JSONObject(jsonString);
				String _id = CouchKey._id.String(jsonDocument);
				String docName = _id.replaceAll(CouchKey._design.key(), "");
				
				if (getDocumentByName(docName) == null) { // document does'nt exist locally
					DesignDocument ddoc = new DesignDocument();
					ddoc.setName(docName);
					ddoc.setJSONObject(jsonDocument);
					
					ddoc.bNew = true;
					ddoc.hasChanged = true;
					
					addDocument(ddoc);
					
					this.hasChanged = true;
				}
			} catch (Exception e) {
				Engine.logBeans.warn("[CouchDbConnector] Unable to create design document from json '" + jsonString + "'", e);
			}
		}
	}
	
	private List<String> getCouchDbDesignDocuments() {
		CouchClient couchClient = getCouchClient();
		List<String> docList = new LinkedList<String>();

		int limit = 10;

		try {
			Map<String, String> query = new HashMap<String, String>(6);
			query.put("include_docs", "true");
			query.put("limit", Integer.toString(limit));
			query.put("end_key", "\"_design0\"");
			query.put("start_key", "\"_design/\"");

			String startkey = null, startkey_docid = null;
			boolean bContinue;

			do {
				bContinue = false;
				JSONObject json = couchClient.getAllDocs(getDatabaseName(), query);

				JSONArray rows = json.getJSONArray("rows");
				int lenght = rows.length();
				
				JSONObject row = null;
				for (int i = 0; i < lenght; i++) {
					row = rows.getJSONObject(i);
					JSONObject doc = row.getJSONObject("doc");
					docList.add(doc.toString());
				}

				if (lenght == limit && row != null) {
					startkey = "\"" + row.getString("key") + "\"";
					startkey_docid = "\"" + row.getString("id") + "\"";

					bContinue = true;
					query.put("start_key", startkey);
					query.put("startkey_docid", startkey_docid);
					query.put("skip", "1");
				}
			} while (bContinue);
		} catch (Throwable t) {
			Engine.logBeans.error("[CouchDbConnector] Unable to retrieve design documents from database", t);
		}

		return docList;
	}
	
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("couchPassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("couchPassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}
	
	public void beforeTransactionInvoke() {
		
	}
	
	public void afterTransactionInvoke() {
		
	}

	public boolean isJsonUseType() {
		return jsonUseType;
	}

	public void setJsonUseType(boolean jsonUseType) {
		this.jsonUseType = jsonUseType;
	}
}
