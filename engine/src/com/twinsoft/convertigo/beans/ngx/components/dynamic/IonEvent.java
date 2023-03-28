/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.beans.ngx.components.dynamic;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class IonEvent implements Cloneable {
	
	enum Key {
		name,
		attr,
		label,
		description
		;
	}
	
	private JSONObject jsonEvent;
	
	public IonEvent() {
		try {
			jsonEvent = new JSONObject()
				.put(Key.name.name(), "event")
				.put(Key.attr.name(), "")
				.put(Key.description.name(), "description");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public IonEvent(JSONObject jsonOb) {
		this();
		for (Key k: Key.values()) {
			if (jsonOb.has(k.name())) {
				try {
					jsonEvent.put(k.name(), jsonOb.get(k.name()));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public JSONObject getJSONObject() {
		return jsonEvent;
	}
	
	public String getName() {
		try {
			return jsonEvent.getString(Key.name.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "event";
		}
	}
	
	protected void setName(String name) {
		try {
			jsonEvent.put(Key.name.name(), name);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public String getAttr() {
		try {
			return jsonEvent.getString(Key.attr.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public String getDescription() {
		try {
			return jsonEvent.getString(Key.description.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "description";
		}
	}
	
	public String toString() {
		return jsonEvent.toString();
	}
}
