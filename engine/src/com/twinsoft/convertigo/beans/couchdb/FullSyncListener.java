/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.couchdb;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;

public class FullSyncListener extends AbstractFullSyncViewListener {

	private static final long serialVersionUID = -7580433107225235685L;
	
	public FullSyncListener() {
		super();
	}

	@Override
	public FullSyncListener clone() throws CloneNotSupportedException {
		FullSyncListener clonedObject =  (FullSyncListener) super.clone();
		return clonedObject;
	}
	
	@Override
	protected void triggerSequence(InternalHttpServletRequest request, JSONArray ids) throws EngineException {
		if (isEnabled()) {
			if (targetView == null || targetView.isEmpty()) {
				throw new EngineException("No target view defined");
			}
			
			String ddoc = getTargetDocName();
			if (ddoc == null) {
				throw new EngineException("Target design document name is null");
			}
			
			String view = getTargetViewName();
			if (view == null) {
				throw new EngineException("Target view name is null");
			}
			
			int len = ids.length();
			
			Map<String, String> query = new HashMap<String, String>(5);
			query.put("reduce", "false");
			query.put("include_docs", "true");
			query.put("conflicts", "true");
			query.put("stable", "true");
			query.put("update", "true");
			
			String db = getDatabaseName();
			try {
				CouchClient client = getCouchClient();
				for (int i = 0; i < len;) {
					JSONArray doc_ids = getChunk(ids, i);
					int ids_len = doc_ids.length();
					i += ids_len;
					
					Engine.logBeans.debug("(FullSyncListener) Listener \"" + getName() + "\" : [" + db + "] post view '" + ddoc + "/" + view + "' for _id keys " + doc_ids);
					JSONObject json = client.postView(db, ddoc, view, query, CouchKey.keys.put(new JSONObject(), doc_ids));
					Engine.logBeans.debug("(FullSyncListener) Listener \"" + getName() + "\" : [" + db + "] post view '" + ddoc + "/" + view + "' returned following documents :\n" + json.toString());
					
					if (json != null) {
						if (CouchKey.error.has(json)) {
							String error = CouchKey.error.String(json);
							error = error == null ? "unknown" : error;
							String reason = CouchKey.reason.String(json);
							reason = reason == null ? "unknown" : reason;
							throw new EngineException("View '" + db + "/" + ddoc + "/" + view + "' returned error: " + error + ", reason: " + reason);
						}
						runDocs(request, CouchKey.rows.JSONArray(json));
					}
				}
			} catch (Throwable t) {
				throw new EngineException("Query view named '"+ db + "/" + ddoc + "/" + view + "' failed", t);
			}
		}
	}
}
