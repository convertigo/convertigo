package com.twinsoft.convertigo.engine.enums;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public enum CouchKey {
	_id,
	_rev,
	_design("_design/"),
	_global("_global/"),
	_c8oHttp,
	id,
	docs,
	filters,
	map,
	reduce,
	rev,
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
	
	public String string(JsonObject json) {
		return json != null && json.has(key) ? json.get(key).getAsString() : null;
	}
	
	public void add(JsonObject json, String value) {
		json.addProperty(key, value);
	}
	
	public void add(JsonObject json, JsonElement value) {
		json.add(key, value);
	}
	
	public boolean has(JsonObject json) {
		return json.has(key);
	}
	
	public void remove(JsonObject json) {
		json.remove(key);
	}
	
	public void add(JSONObject json, Object value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			onJSONException(e);
		}
	}
	
	public boolean has(JSONObject json) {
		return json.has(key);
	}

	public String string(JSONObject json) {
		try {
			return json.getString(key);
		} catch (JSONException e) {
			onJSONException(e);
			return null;
		}
	}

	public void put(JSONObject json, String value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			onJSONException(e);
		}
	}
	
	public void remove(JSONObject json) {
		json.remove(key);
	}
	
	private void onJSONException(JSONException e) {
		
	}
}
