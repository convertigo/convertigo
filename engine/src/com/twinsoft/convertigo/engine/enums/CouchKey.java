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
	_attachments,
	_c8oMeta,
	_c8oEntity,
	id,
	items,
	cancel,
	changes,
	continuous,
	create_target,
	data,
	deleted,
	doc,
	docs,
	doc_ids,
	error,
	filters,
	keys,
	language,
	last_seq,
	map,
	name,
	ok,
	password,
	filter,
	proxy,
	purged,
	reason,
	reduce,
	results,
	rev,
	rows,
	seq,
	source,
	target,
	total_purged,
	total_rows,
	updates,
	value,
	views,
	validate_doc_update,
	c8oAcl("~c8oAcl"),
	c8oGrp,
	c8oDbVersion("~c8oDbVersion"),
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

	public Object Object(JSONObject json) {
		try {
			return json.get(key);
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
