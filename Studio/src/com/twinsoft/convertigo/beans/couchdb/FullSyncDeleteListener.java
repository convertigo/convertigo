package com.twinsoft.convertigo.beans.couchdb;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import com.twinsoft.convertigo.engine.EngineException;

public interface FullSyncDeleteListener {
	public JSONArray onPreBulkDocs(JSONArray deletedIds) throws EngineException, JSONException;
}
