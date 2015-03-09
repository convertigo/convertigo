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

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;

public class FullSyncListener extends Listener {

	private static final long serialVersionUID = -7580433107225235685L;

	private String targetView;
	
	public FullSyncListener() {
		super();
	}

	@Override
	public FullSyncListener clone() throws CloneNotSupportedException {
		FullSyncListener clonedObject =  (FullSyncListener) super.clone();
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
	
	public String getTargetView() {
		return targetView;
	}

	public void setTargetView(String targetView) {
		this.targetView = targetView;
	}

	private String getTargetDocName() {
		if (targetView != null) {
			try {
				return targetView.split("\\.")[2];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private String getTargetViewName() {
		if (targetView != null) {
			try {
				return targetView.split("\\.")[3];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public void onBulkDocs(List<String> ids) throws EngineException {
		try {
			if (targetView == null) {
				throw new EngineException("No target view defined");
			}
			
			String designDocName = getTargetDocName();
			if (designDocName == null) {
				throw new EngineException("Target design document name is null");
			}
			
			String viewName = getTargetViewName();
			if (viewName == null) {
				throw new EngineException("Target view name is null");
			}

			FullSyncConnector connector = getConnector();
			String db = connector.getDatabaseName();
			String docId = CouchKey._design.key() + designDocName;
			
			List<String> docList = new LinkedList<String>();
			int limit = 10;
			try {
				List<NameValuePair> options = new LinkedList<NameValuePair>();
				options.add(new BasicNameValuePair("include_docs", "true"));
				options.add(new BasicNameValuePair("limit", Integer.toString(limit)));
 
				JSONObject keys = new JSONObject();
				keys.put("keys", new JSONArray(ids));
				
				String startkey = null, startkey_docid = null;
				boolean bContinue;

				do {
					bContinue = false;
					JSONObject json = connector.getCouchClient().postView(db, docId, viewName, options, keys);
					if (json != null) {
						JSONArray rows = json.getJSONArray("rows");
						if (rows != null) {
							int lenght = rows.length();
							JSONObject row = null;
	
							// Retrieve the first results
							for (int i = 0; i < lenght; i++) {
								row = rows.getJSONObject(i);
								JSONObject doc = row.getJSONObject("doc");
								docList.add(doc.toString());
							}
	
							Engine.logBeans.debug("(FullSyncListener) view query returned following documents :\n"+ docList + "\n");
							
							// TODO : execute sequence with given docs
							if (lenght > 0 && row != null) {
								
							}
							
							// Retrieve the next results
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
						else {
							String error = json.getString("error");
							error = error == null ? "unknown" : error;
							String reason = json.getString("reason");
							reason = reason == null ? "unknown" : reason;
							throw new EngineException("View returned error: "+ error + ", reason: "+ reason);
						}
					}
					else {
						throw new EngineException("View returned a null result");
					}
				} while (bContinue);
			} catch (Throwable t) {
				throw new EngineException("Query view named \""+ viewName +"\" of \""+ designDocName +"\" design document failed", t);
			}
			
		} catch (Exception e) {
			throw new EngineException("Unable to handle 'bulkDocs' event for \""+ getName() +"\" listener", e);
		}
	}
}
