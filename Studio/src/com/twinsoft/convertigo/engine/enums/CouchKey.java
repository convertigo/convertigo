package com.twinsoft.convertigo.engine.enums;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public enum CouchKey {
	_id,
	_rev,
	_deleted,
	_design("_design/"),
	_global("_global/"),
	_c8oMeta,
	id,
	items,
	cancel,
	continuous,
	create_target,
	data,
	doc,
	docs,
	doc_ids,
	error,
	filters,
	keys,
	map,
	name,
	ok,
	password,
	proxy,
	reason,
	reduce,
	results,
	rev,
	rows,
	source,
	target,
	total_rows,
	updates,
	value,
	views,
	c8oAcl("~c8oAcl"),
	c8oHash("~c8oHash");
	
	String key;
	
	CouchKey() {
		key = name();
	}
	
	CouchKey(String key) {
		this.key = key;
	}
	
	public String key() {
		return key;
	}
	
	public boolean has(JSONObject json) {
		return json.has(key);
	}
	
	public boolean equals(JSONObject json1, JSONObject json2) {
		if (json1.has(key) && json2.has(key)) {
			try {
				return json1.get(key).equals(json2.get(key));
			} catch (JSONException e) {
				onJSONException(e);
			}
		}
		return false;
	}
	
	public void copy(JSONObject json1, JSONObject json2) {
		if (json1.has(key)) {
			try {
				json2.put(key, json1.get(key));
			} catch (JSONException e) {
				onJSONException(e);
			}
		}
	}
	
	public JSONObject put(JSONObject json, Object value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			onJSONException(e);
		}
		return json;
	}
	
	public void remove(JSONObject json) {
		json.remove(key);
	}

	public String String(JSONObject json) {
		try {
			return json.getString(key);
		} catch (JSONException e) {
			onJSONException(e);
			return null;
		}
	}

	public JSONObject JSONObject(JSONObject json) {
		try {
			return json.getJSONObject(key);
		} catch (JSONException e) {
			onJSONException(e);
			return null;
		}
	}

	public JSONArray JSONArray(JSONObject json) {
		try {
			return json.getJSONArray(key);
		} catch (JSONException e) {
			onJSONException(e);
			return null;
		}
	}
	
	private void onJSONException(JSONException e) {
		
	}
}
