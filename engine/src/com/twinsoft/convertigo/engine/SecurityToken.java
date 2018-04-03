/*
 * Copyright (c) 2001-2018 Convertigo SA.
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
 */

package com.twinsoft.convertigo.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.util.GenericUtils;

@Entity
public class SecurityToken {
	private String tokenID;
	protected String userID;
	protected long expiryDate;
	protected Map<String, String> data;
	protected String jsonData;

	public SecurityToken() {
	}
	
	public SecurityToken(String tokenID, String userID, long expiryDate, Map<String, String> data) {
		this.tokenID = tokenID;
		this.userID = userID;
		this.expiryDate = expiryDate;
		this.data = data;
	}

	@Id
	public String getTokenID() {
		return tokenID;
	}

	public void setTokenID(String tokenID) {
		this.tokenID = tokenID;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public long getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(long expiryDate) {
		this.expiryDate = expiryDate;
	}
	
	@Transient
	public Map<String, String> getData() {
		return data;
	}

	public void setData(Map<String, String> data) {
		this.data = data;
	}

	public String getJsonData() {
		String jsonData = new JSONObject(data).toString(); 
		return jsonData;
	}

	public void setJsonData(String data) {
		this.data = new HashMap<String, String>();
		JSONObject json;
		try {
			json = new JSONObject(data);
			for (Iterator<String> i = GenericUtils.cast(json.keys()); i.hasNext();) {
				String key = i.next();
				this.data.put(key, json.getString(key));
			}
		} catch (JSONException e) {
			Engine.logSecurityTokenManager.warn("(SecurityToken) Failed to decode JSON data", e);
		}
	}

	@Transient
	public boolean isExpired() {
		long now = System.currentTimeMillis();
		return expiryDate - now < 0;
	}

	@Override
	public String toString() {
		return "TokenID: " + tokenID + ", userID: " + userID + ", expiryDate: " + expiryDate
				+ ", data: " + data.toString();
	}
}
