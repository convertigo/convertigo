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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractDatabaseTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbProperties;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbProvider;
import com.twinsoft.convertigo.engine.util.ParameterUtils;

public class CouchDbConnector extends Connector {

	private static final long serialVersionUID = -8895252401444085569L;

	private String databaseName = "";
	private String server = "127.0.0.1";
	private int port = 5984;
	private boolean https = false;
	
	private transient CouchDbProvider dbClient = null;
	private transient CouchClient couchClient = null;
	
	public CouchDbConnector() {
		
	}

	@Override
	public Connector clone() throws CloneNotSupportedException {
		CouchDbConnector clonedObject = (CouchDbConnector) super.clone();
		clonedObject.dbClient = dbClient;
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
	
	@Override
	public void release() {
		super.release();

		try {
			if (dbClient != null) {
				dbClient.shutdown();
				Engine.logBeans.debug("(CouchDbConnector) Connector released");
			}
        } catch (Exception ee) {
			Engine.logBeans.error("(CouchDbConnector) An error occured while releasing connector", ee);
		}
		finally {
			dbClient = null;
		}
		
	}

	public CouchDbProvider getCouchDbClient() {
		if (dbClient == null) {
			String dbName = getDatabaseName();
			
			CouchDbProperties properties = new CouchDbProperties()
			  .setDbName(dbName)
			  .setCreateDbIfNotExist(dbName.isEmpty() ? false:true)
			  .setProtocol(isHttps() ? "https":"http")
			  .setHost(getServer())
			  .setPort(getPort())
			  .setMaxConnections(10);
			
			dbClient = new CouchDbProvider(properties);
		}
		return dbClient;
	}
	
	public CouchClient getCouchClient() {
		if (couchClient == null) {
			String url = isHttps() ? "https" : "http";
			url+= "://" + getServer() + ":" + getPort();
			couchClient = new CouchClient(url);
		}
		return couchClient;
	}
	
	public void setCouchDbClient(CouchDbProvider dbClient) {
		this.dbClient = dbClient;
	}
	
	public void setData(Object data) {
		fireDataChanged(new ConnectorEvent(this, data));
	}
	
	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		AbstractCouchDbTransaction couchDbTransaction = (AbstractCouchDbTransaction) context.requestedObject;
		
		// Set the target database
		if (couchDbTransaction instanceof AbstractDatabaseTransaction) {
			String targetDbName = ParameterUtils.toString(couchDbTransaction.getParameterValue(AbstractDatabaseTransaction.var_database));
			if (targetDbName == null /*|| targetDbName.isEmpty()*/) {
				targetDbName = getDatabaseName();
			}
			((AbstractDatabaseTransaction)couchDbTransaction).setTargetDatabase(targetDbName);
		}
	}

	@Override
	public Transaction newTransaction() {
		return null;
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
		if (getCouchClient() != null) {
			CouchClient couchClient = getCouchClient();
			List<String> docList = new LinkedList<String>();

			int limit = 10;

			try {
				List<NameValuePair> options = new LinkedList<NameValuePair>();
				options.add(new BasicNameValuePair("include_docs", "true"));
				options.add(new BasicNameValuePair("limit", Integer.toString(limit)));
				options.add(new BasicNameValuePair("end_key", "\"_design0\""));
				options.add(new BasicNameValuePair("start_key", "\"_design/\""));

				String startkey = null, startkey_docid = null;
				boolean bContinue;

				do {
					bContinue = false;
					Object json = couchClient.allDocs(getDatabaseName(), options);

					if (json instanceof JSONObject) {
						JSONArray rows = ((JSONObject) json).getJSONArray("rows");
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
							options = options.subList(0, 3);
							options.add(new BasicNameValuePair("start_key", startkey));
							options.add(new BasicNameValuePair("startkey_docid", startkey_docid));
							options.add(new BasicNameValuePair("skip", "1"));
						}
					}
				} while (bContinue);
			} catch (Throwable t) {
				Engine.logBeans.error("[CouchDbConnector] Unable to retrieve design documents from database", t);
			}

			return docList;
		}
		
		CouchDbProvider provider = getCouchDbClient();
		String targetDbName = getDatabaseName();
		
		List<String> docList = new ArrayList<String>();
		JsonElement json = null, rows = null;
		JsonObject row = null, doc = null;
		JsonArray arr = null;
		int size;
		
		Integer limit = 10;
		
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("startkey", "\"_design/\"");
		options.put("endkey", "\"_design0\"");
		options.put("include_docs", "true");
		options.put("limit", limit);
		
		String startkey = null, startkey_docid = null;
		boolean bContinue;
		
		do {
			bContinue = false;
			try {
				json = provider.context().db(targetDbName).document().all(options, null);
				
				if (json.isJsonObject()) {
					rows = json.getAsJsonObject().get("rows");
					
					if (rows.isJsonArray()) {
						arr = rows.getAsJsonArray();
						size = arr.size();
						
						if (size > 0) {
							for (int i=0; i<size; i++) {
								row = arr.get(i).getAsJsonObject();
								doc = row.get("doc").getAsJsonObject();
								docList.add(doc.toString());
							}
							
							if (size == limit && row != null) {
								startkey = "\""+row.get("key").getAsString()+"\"";
								startkey_docid = "\""+row.get("id").getAsString()+"\"";
								
								bContinue = true;
								options.put("startkey", startkey);
								options.put("startkey_docid", startkey_docid);
								options.put("skip",new Integer(1));
							}
						}
					}
				}
			} catch (Throwable t) {
				bContinue = false;
				Engine.logBeans.error("[CouchDbConnector] Unable to retrieve design documents from database", t);
			}
		} while (bContinue);
		
		return docList;
	}
}
