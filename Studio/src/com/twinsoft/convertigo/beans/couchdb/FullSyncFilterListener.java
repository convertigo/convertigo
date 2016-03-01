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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;

public class FullSyncFilterListener extends AbstractFullSyncFilterListener {

	private static final long serialVersionUID = -7580433107225235685L;
	
	public FullSyncFilterListener() {
		super();
	}

	@Override
	public FullSyncFilterListener clone() throws CloneNotSupportedException {
		FullSyncFilterListener clonedObject =  (FullSyncFilterListener) super.clone();
		return clonedObject;
	}
	
	@Override
	protected void triggerSequence(JSONArray ids) throws EngineException {
		if (targetFilter == null || targetFilter.isEmpty()) {
			throw new EngineException("No target view defined");
		}
		
		String filter = targetFilter;
		
		String ddoc = getTargetDocName();
		String filterName = getTargetFilterName();
		if (ddoc != null && filterName != null) {
			filter = ddoc + "/" + filterName;
		}
		
		int len = ids.length();
		
		Map<String, String> query = new HashMap<String, String>(2);
		query.put("filter", filter);
		query.put("include_docs", "true");
		try {
			for (int i = 0; i < len;) {
				JSONArray doc_ids = getChunk(ids, i);
				i += doc_ids.length();

				Engine.logBeans.debug("(FullSyncFilterListener) Listener \"" + getName() + "\" : post filter for _id keys " + doc_ids);
				JSONObject json = getCouchClient().postChange(getDatabaseName(), query, CouchKey.doc_ids.put(new JSONObject(), doc_ids));
				Engine.logBeans.debug("(FullSyncFilterListener) Listener \"" + getName() + "\" : post filter returned following documents :\n" + json.toString());

				if (json != null) {
					if (CouchKey.error.has(json)) {
						String error = CouchKey.error.String(json);
						error = error == null ? "unknown" : error;
						String reason = CouchKey.reason.String(json);
						reason = reason == null ? "unknown" : reason;
						throw new EngineException("Filter returned error: " + error + ", reason: " + reason);
					}
					runDocs(CouchKey.results.JSONArray(json));
				}
			}
		} catch (Throwable t) {
			throw new EngineException("Query filter named \""+ filter +"\" design document failed", t);
		}
	}
}
