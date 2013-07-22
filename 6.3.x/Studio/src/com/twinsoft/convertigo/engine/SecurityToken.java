package com.twinsoft.convertigo.engine;

import java.util.Map;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class SecurityToken {

	public final String tokenID;
	public final String userID;
	public final long expiryDate;
	public final Map<String, String> data;

	public SecurityToken(String tokenID, String userID, long expiryDate, Map<String, String> data) {
		this.tokenID = tokenID;
		this.userID = userID;
		this.expiryDate = expiryDate;
		this.data = data;
	}

	public boolean isExpired() {
		long now = System.currentTimeMillis();
		return expiryDate - now < 0;
	}

	public boolean isZombie() {
		long now = System.currentTimeMillis();
		long tokenLifeTime = Long.parseLong(EnginePropertiesManager.getProperty(PropertyName.SECURITY_TOKEN_LIFE_TIME));
		// We estimate that a token is a zombie token if 2*tokenLifeTime has been passed
		return expiryDate + tokenLifeTime - now < 0;
	}

	public String toString() {
		return "TokenID: " + tokenID + ", userID: " + userID + ", expiryDate: " + expiryDate
				+ ", data: " + data.toString();
	}
}
