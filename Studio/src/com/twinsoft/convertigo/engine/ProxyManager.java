/*
 * Copyright (c) 2001-2011 Convertigo SA.
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
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine;


import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.BasicScheme;

import com.twinsoft.convertigo.engine.PacManager.PacInfos;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.parsers.AbstractXulWebViewer;

public class ProxyManager {
	
	transient public CertificateManager certificateManager = null;

	transient public UUID hostConfId;	
	
	transient public String proxyServer;
	transient public int proxyPort;
	transient public String proxyUrl;
	transient public String proxyMethod;
	transient public String proxyMode;
	transient public String proxyUser;
	transient public String proxyPassword;
	
	transient public String promptUser;
	transient public String promptPassword;
	transient public String basicValue;
	transient public String bypassDomains;
	transient private PacManager pacUtils;
	
	public ProxyManager() {
		Engine.theApp.eventManager.addListener(myPropertyChangeEventListener = new MyPropertyChangeEventListener(), PropertyChangeEventListener.class);
	}
	
	public class MyPropertyChangeEventListener implements PropertyChangeEventListener {
		
		public void onEvent(final PropertyChangeEvent event) {
			if (event.getKey().toString().startsWith("htmlProxy.")) {
				getEngineProperties();
				AbstractXulWebViewer.setProxySettings();
			}
		}
	};
	static MyPropertyChangeEventListener myPropertyChangeEventListener;
	
	public void init() throws EngineException {	
		getEngineProperties();
		certificateManager = new CertificateManager();
	}
	
	public void getEngineProperties() {
		//init hostConfiguration ID;
		this.hostConfId = UUID.randomUUID();

		try {
			this.proxyPort = Integer.parseInt(EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PORT));
		}
		catch (Exception e) {
			EnginePropertiesManager.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PORT, "3128");
			this.proxyPort = 3128;
			Engine.logProxyManager.debug("(ProxyManager) Couldn't set proxy property, proxyPort parsing failed: " + e); 
			Engine.logProxyManager.info("Wrong proxy configuration: bad proxy port");
		}
		this.proxyMethod = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_METHOD);
		this.proxyMode = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_MODE);
		this.proxyServer = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_HOST);
		this.proxyUser = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_USER);
		this.proxyPassword = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PASSWORD);
		this.proxyUrl = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_AUTO);
		this.bypassDomains = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS);
		
		if (proxyMode.equals(ProxyMode.auto.name())) {
			this.pacUtils = new PacManager(proxyUrl);
			this.pacUtils.start();
		}
	}
	
	public enum ProxyMode {
    	off,
    	auto,
    	manual;

		final String value;
		
		ProxyMode() {
			this.value = name();
		}

		public String getValue() {
			return value;
		}
		
		public int index() {
			return Arrays.binarySearch(ProxyMode.values(), this);
		}
	}
	
	public enum ProxyMethod {
		anonymous,
		basic,
		ntlm;
		
		final String value;
		
		ProxyMethod() {
			this.value = name();
		}

		public String getValue() {
			return value;
		}
		
		public int index() {
			return Arrays.binarySearch(ProxyMode.values(), this);
		}
	}
	
	public void disableProxy(HostConfiguration hostConfiguration) {
		hostConfiguration.setProxyHost(null);
	}

	public void setProxy(HostConfiguration hostConfiguration, HttpState httpState, URL url) throws EngineException {
		// Proxy configuration
		Boolean needProxy = true;
		String[] bpDomains = getBypassDomains();
		String urlHost = url.getHost();
		
		for (String domain :bpDomains) {
			if (domain.equals(urlHost)) {
				needProxy = false;
			} 
		}
		
		hostConfiguration.getParams().setParameter("hostConfId",this.hostConfId);
		
		if (!proxyMode.equals(ProxyMode.off.name())) {
			if (needProxy) {
				if (proxyMode.equals(ProxyMode.manual.name())) {
					if (!proxyServer.equals("")) {
						hostConfiguration.setProxy(proxyServer, proxyPort);
						Engine.logProxyManager.debug("(ProxyManager) Using proxy: " + proxyServer + ":" + proxyPort);
					} else {
						disableProxy(hostConfiguration);
					}
				}
				else if (proxyMode.equals(ProxyMode.auto.name())) {
//					String result = pacUtils.evaluate(url.toString(), url.getHost());
//					
//					if (result.startsWith("PROXY")) {
//						result = result.replaceAll("PROXY\\s*", "");
//						String pacServer = result.split(":")[0];
//						int pacPort =  Integer.parseInt(result.split(":")[1]);
//						this.proxyServer = pacServer;
//						this.proxyPort = pacPort;
//						hostConfiguration.setProxy(pacServer, pacPort);
//						Engine.logProxyManager.debug("(ProxyManager) Using proxy from auto configuration file: " + proxyServer + ":" + proxyPort);
//					}
//					else {
//						hostConfiguration.setProxyHost(null);
//					}
					
					PacInfos pacInfos = getPacInfos(url.toString(), url.getHost());
					if (pacInfos != null) {
						proxyServer = pacInfos.getServer();
						proxyPort = pacInfos.getPort();
						hostConfiguration.setProxy(proxyServer, proxyPort);
						Engine.logProxyManager.debug("(ProxyManager) Using proxy from auto configuration file: " + proxyServer + ":" + proxyPort);
					}
					else {
						disableProxy(hostConfiguration);
					}
				}
				
				if (proxyMethod.equals(ProxyMethod.basic.name())) {
					setBasicAuth(httpState);
				}
				else if (proxyMethod.equals(ProxyMethod.ntlm.name())) {
					int indexSlash = this.proxyUser.indexOf("\\");	
					if (indexSlash != -1) {
						setNtlmAuth(httpState);
					}
					else {
						throw new EngineException("\nWrong username, please indicate the domain name for ntlm authentication. (eg: domain\\user)\n");
					}
				}
				else {
					setAnonymAuth(httpState);
				}
			}
			else {
				disableProxy(hostConfiguration);
			}
		}
	}
	
	public PacInfos getPacInfos(String url, String host) {
		return pacUtils.getPacInfos(url, host);
	}
	
	public void setBasicAuth(HttpState httpState) {
		// Setting basic authentication for proxy
		if ((!this.proxyServer.equals("")) && (!this.proxyUser.equals(""))) {
				httpState.setProxyCredentials(
						new AuthScope(this.proxyServer, -1, AuthScope.ANY_REALM),
						new UsernamePasswordCredentials(this.proxyUser, this.proxyPassword));

				Engine.logProxyManager.debug("(ProxyManager) Using credentials: " + promptUser
						+ ", <password not logged, set engine logger log level to TRACE to see it>");
				Engine.logProxyManager.trace("(ProxyManager) Using password: " + proxyPassword);
		}
	}
	
	public void setNtlmAuth(HttpState httpState) {
		// Setting NTLM authentication for proxy
		int indexSlash = this.proxyUser.indexOf("\\");	
		String domain = this.proxyUser.substring(0, indexSlash);
		String username = this.proxyUser.substring(indexSlash + 1);

		Engine.logProxyManager.debug("(ProxyManager) Using NTLM authentication on domain: " + domain);

		httpState.setProxyCredentials(
				new AuthScope(this.proxyServer, -1, AuthScope.ANY_REALM),
				new NTCredentials(username, this.proxyPassword, this.proxyServer, domain));
		
	}
	
	public void setAnonymAuth(HttpState httpState) {
		// Setting anonym authentication for proxy
		if ((!this.proxyServer.equals("")) && (!this.proxyUser.equals(""))) {
				httpState.setProxyCredentials(
						new AuthScope(AuthScope.ANY),
						new Credentials() {
						});

			Engine.logProxyManager.debug("(ProxyManager) Proxy credentials: anonym");
		}
	}

	public void setCredentials() {
		//Setting credentials for XUL
		if (this.promptUser == null) {
			this.promptUser = this.proxyUser;
		}
		if (this.promptPassword == null) {
			this.promptPassword = this.proxyPassword;
		}
		if (this.promptUser != null && this.promptPassword.length() > 0 && proxyMethod.equals(ProxyMethod.basic.name())) {
			this.basicValue = BasicScheme.authenticate(new UsernamePasswordCredentials(this.promptUser, this.promptPassword), "UTF-8");
		} else {
			this.basicValue = null;
		}	
	}
	
	public void setCredentials(AbstractXulWebViewer wv) {
		wv.setCredentials(this.promptUser, this.promptPassword);
	}
	
	public String getProxyServer() {
		return proxyServer;
	}

	public void setProxyServer(String proxyServer) {
		this.proxyServer = proxyServer;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}

	public String getProxyUser() {
		return proxyUser;
	}

	public void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	
	public String[] getBypassDomains() {
		String[] domains = bypassDomains.split(",");
		return domains;
	}

	public void setBypassDomains(String bypassDomains) {
		this.bypassDomains = bypassDomains;
	}
	
	public UUID getHostConfId() {
		return hostConfId;
	}
}
