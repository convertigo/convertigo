/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.CouchPostDocumentPolicy;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.providers.couchdb.documents.DesignDocumentC8o;
import com.twinsoft.convertigo.engine.util.GenericUtils;


public class PurgeDatabaseTransaction extends AbstractDatabaseTransaction implements ICouchParametersExtra {

	private static final long serialVersionUID = 5234196633383833697L;
	
	private String p_json_base = "";
	private boolean purgeAll = false;
	
	public PurgeDatabaseTransaction() {
		super();
	}

	@Override
	public PurgeDatabaseTransaction clone() throws CloneNotSupportedException {
		PurgeDatabaseTransaction clonedObject =  (PurgeDatabaseTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected JSONObject invoke() throws Exception {
		String db = getTargetDatabase();
		
		JSONObject jsonBase;
		
		try {
			jsonBase = new JSONObject(getParameterStringValue(CouchParam.json_base));
		} catch (Throwable t) {
			jsonBase = new JSONObject();
		}
		
		JSONObject jsonDocument = jsonBase;
		JSONObject response = null;
		int length;
		int purged = 0;
		CouchClient couchClient = getCouchClient();
		String version = couchClient.getServerVersion();
		if (version.startsWith("2.0.") || version.startsWith("2.1.") || version.startsWith("2.2.")) {
			return new JSONObject("{\"error\": \"'_purge' is implemented since CouchDB 2.3.0 and you are using CouchDB " + version + "\"}");
		}
		boolean old = version != null && version.compareTo("2.") < 0;
		if (isPurgeAll()) {
			JSONObject body = null;
			Map<String, String> query = new HashMap<>(3);
			if (!old) {
				query.put("filter", "_selector");
				body = new JSONObject("{\"selector\":{\"_deleted\":true}}");
			}
			int limit = 50;
			query.put("limit", Integer.toString(limit));
			
			String since = "0";
			
			while (since != null) {
				query.put("since", since);
				jsonDocument = new JSONObject();
				JSONObject json = old ? couchClient.getChanges(db, query) : couchClient.postChanges(db, query, body);
				JSONArray changes = CouchKey.results.JSONArray(json);
				length = changes.length();
				since = length < limit ? null : CouchKey.last_seq.String(json);
				handleChanges(changes, old, jsonDocument);
				if (jsonDocument.length() > 0) {
					response = couchClient.postPurge(db, jsonDocument);
					if (CouchKey.purged.has(response)) {
						purged += CouchKey.purged.JSONObject(response).length();
					} else {
						return response;
					}
				}
			}
		} else {
			Object p = getParameterValue("_id");
			List<String> ids = null;
			if (p != null) {
				if (p instanceof String) {
					ids = Arrays.asList((String) p);
				} else if (p instanceof List) {
					ids = GenericUtils.cast(p);
				}
			}
			if (ids != null && !ids.isEmpty()) {
				List<String> revs = null;
				if ((p = getParameterValue("_rev")) != null) {
					if (p instanceof String) {
						revs = Arrays.asList((String) p);
					} else if (p instanceof List) {
						revs = GenericUtils.cast(p);
					}
				}
				Iterator<String> irev = revs != null ? revs.iterator() : Collections.emptyIterator();
				for (String id: ids) {
					JSONArray r = null;
					if (jsonDocument.has(id)) {
						Object o = jsonDocument.get(id);
						if (o instanceof JSONArray) {
							r = (JSONArray) o;
						}
					}
					if (r == null) {
						jsonDocument.put(id, r = new JSONArray());
					}
					if (irev.hasNext()) {
						r.put(irev.next());
					}
				}
			}
			if ((length = jsonDocument.length()) > 0) {
				Set<String> keys = new HashSet<>(length);
				for (Iterator<?> i = jsonDocument.keys(); i.hasNext();) {
					String key = (String) i.next();
					Object o = jsonDocument.get(key);
					if (!(o instanceof JSONArray)) {
						jsonDocument.put(key, new JSONArray());
						keys.add(key);
					} else if (((JSONArray) o).length() == 0) {
						keys.add(key);
					}
				}
				if (!keys.isEmpty()) {
					JSONObject body = new JSONObject();
					CouchKey.doc_ids.put(body, new JSONArray(keys));
					JSONObject changes = couchClient.postChanges(db, null, body);
					JSONArray result = CouchKey.results.JSONArray(changes);
					handleChanges(result, false, jsonDocument);
				}
				if (jsonDocument.length() > 0) {
					response = couchClient.postPurge(db, jsonDocument);
					purged += CouchKey.purged.JSONObject(response).length();
				}
			}
		}
		
		if (response == null) {
			response = new JSONObject("{\"purge_seq\":0,\"purged\":{},\"_c8oMeta\":{\"statusCode\":200,\"status\":\"success\",\"reasonPhrase\":\"OK\",\"headers\":{\"Content-Type\":\"application\\/json\"}}}");
		}
		
		CouchKey.total_purged.put(response, purged);
		
		if (purged > 0) {
			try {
				JSONObject ddoc = couchClient.getDocument(db, DesignDocumentC8o.getId());
				JSONObject meta = CouchKey._c8oMeta.JSONObject(ddoc);
				if (meta.getInt("statusCode") == 200) {
					String dbVersion = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
					CouchKey.c8oDbVersion.put(ddoc, dbVersion);
					couchClient.postDocument(db, ddoc, null, CouchPostDocumentPolicy.none, false);
					Engine.logBeans.info("(PurgeDatabaseTransaction) Database '" + db + "' version changed to '" + dbVersion + "'.");
				}
			} catch (Exception e) {
				Engine.logBeans.warn("(PurgeDatabaseTransaction) Failed to update database '" + db + "' version", e);
			}
		}
		
		return response;
	}
	
	private void handleChanges(JSONArray result, boolean old, JSONObject jsonDocument) throws JSONException {
		int length = result.length();
		for (int i = 0; i < length; i++) {
			JSONObject json = result.getJSONObject(i);
			if (old && !CouchKey.deleted.has(json)) {
				continue;
			}
			JSONArray changes = CouchKey.changes.JSONArray(json);
			JSONArray revs = new JSONArray(changes.length());
			int jLength = changes.length();
			for (int j = 0; j < jLength; j++) {
				revs.put(CouchKey.rev.String(changes.getJSONObject(j)));
			}
			jsonDocument.put(CouchKey.id.String(json), revs);
		}
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "purgeDatabaseType");
	}

	public String getP_json_base() {
		return p_json_base;
	}

	public void setP_json_base(String p_json_base) {
		this.p_json_base = p_json_base;
	}

	public boolean isPurgeAll() {
		return purgeAll;
	}

	public void setPurgeAll(boolean purgeAll) {
		this.purgeAll = purgeAll;
	}
	
	@Override
	public Collection<CouchExtraVariable> getCouchParametersExtra() {
		return Arrays.asList(
				CouchExtraVariable._ids,
				CouchExtraVariable._revs
		);
	}
}
