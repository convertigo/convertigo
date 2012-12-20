package com.twinsoft.convertigo.eclipse;

import java.util.Properties;

public enum DeploymentKey {
	adminUser("admin.user"),
	adminPassword("admin.password"),
	server("server", "trial.convertigo.net/cems"),
	sslHttps("ssl.https", "true"),
	sslTrustCert("ssl.trustCert", "false"),
	assembleXsl("assembleXsl", "false");
	
	private String key;
	private String defaultValue;
	
	DeploymentKey(String key) {
		this(key, null);
	}
	
	DeploymentKey(String key, String defaultValue) {
		this.defaultValue = defaultValue;
		this.key = key;
	}
	
	String key(int i) {
		return "deploy." + i + "." + key; 
	}
	
	String value(Properties properties, int i) {
		return properties.getProperty(key(i));
	}
	
	boolean hasDefault() {
		return defaultValue != null;
	}
	
	void setValue(Properties properties, int i) {
		if (hasDefault()) {
			setValue(properties, i, defaultValue);
		}
	}
	
	void setValue(Properties properties, int i, String value) {
		properties.setProperty(key(i), value);
	}
}
