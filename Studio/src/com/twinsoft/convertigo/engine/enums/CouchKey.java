package com.twinsoft.convertigo.engine.enums;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public enum CouchKey {
	_id,
	_rev,
	_design("_design/"),
	_global("_global/"),
	id,
	filters,
	map,
	reduce,
	rev,
	views,
	c8oAcl("~c8oAcl");
	
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
}
