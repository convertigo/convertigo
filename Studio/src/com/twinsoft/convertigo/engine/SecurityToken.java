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
			Engine.logEngine.warn("(SecurityToken) Failed to decode JSON data", e);
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
