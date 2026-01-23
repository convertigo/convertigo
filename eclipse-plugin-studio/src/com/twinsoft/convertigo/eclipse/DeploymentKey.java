/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
	
	public String value(Properties properties, int i) {
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
