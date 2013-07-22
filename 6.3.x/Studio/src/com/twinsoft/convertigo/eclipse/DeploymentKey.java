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
	
	public String key(int i) {
		return "deploy." + i + "." + key; 
	}
	
	public String value(Properties properties, int i) {
		return properties.getProperty(key(i));
	}
	
	public boolean hasDefault() {
		return defaultValue != null;
	}
	
	public void setValue(Properties properties, int i) {
		if (hasDefault()) {
			setValue(properties, i, defaultValue);
		}
	}
	
	public void setValue(Properties properties, int i, String value) {
		properties.setProperty(key(i), value);
	}
}
