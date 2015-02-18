package com.twinsoft.convertigo.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gson.JsonElement;
import com.twinsoft.convertigo.engine.util.Json;

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
		String jsonData = Json.toJson(data); 
		return jsonData;
	}

	public void setJsonData(String data) {
		this.data = new HashMap<String, String>();
		
		try {
			for (Entry<String, JsonElement> entry: Json.newJsonObject(data).entrySet()) {
				this.data.put(entry.getKey(), entry.getValue().getAsString());
			}
		} catch (Exception e) {
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
