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

package com.twinsoft.convertigo.engine;


import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.log4j.Logger;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.PacManager.PacInfos;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.util.HttpUtils;

public class ProxyManager {

	private UUID hostConfId;

	public String proxyServer;
	private int proxyPort;
	public String proxyUrl;
	public ProxyMethod proxyMethod;
	public ProxyMode proxyMode;
	public String proxyUser;
	public String proxyPassword;
	
	public String bypassDomains;
	private PacManager pacUtils;
	private MyPropertyChangeEventListener myPropertyChangeEventListener = null;

	public ProxyManager() {
		// case of studio wizard, before the engine startup
		if (Engine.theApp == null) {
			Engine.logProxyManager = Logger.getRootLogger();
		}
	}

	private class MyPropertyChangeEventListener implements PropertyChangeEventListener {

		public void onEvent(final PropertyChangeEvent event) {
			if (event.getKey().toString().startsWith("htmlProxy.")) {
				getEngineProperties();
			}
		}
	};

	public void init() throws EngineException {
		if (Engine.theApp != null) {
			myPropertyChangeEventListener = new MyPropertyChangeEventListener();
			Engine.theApp.eventManager.addListener(myPropertyChangeEventListener, PropertyChangeEventListener.class);
		}
		getEngineProperties();
		new CertificateManager();
		ProxySelector.setDefault(new ProxySelector() {

			@Override
			public List<Proxy> select(URI uri) {
				if (proxyMode != ProxyMode.manual) {
					return Arrays.asList(Proxy.NO_PROXY);
				}
				for (String domain: getBypassDomains()) {
					if (uri.getHost().startsWith(domain)) {
						return Arrays.asList(Proxy.NO_PROXY);
					}
				}
				return Arrays.asList(new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(getProxyServer(), getProxyPort())));
			}

			@Override
			public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
				Engine.logConvertigo.warn("(ProxyManager) ProxySelector connectFailed for uri: " + uri, ioe);
			}

		});

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				if (getRequestorType() == RequestorType.PROXY) {
					return new PasswordAuthentication(getProxyUser(), getProxyPassword().toCharArray());
				}
				return null;
			}
		});
	}

	void destroy() {
		myPropertyChangeEventListener = null;
	}

	public void getEngineProperties() {
		//init hostConfiguration ID;
		this.hostConfId = UUID.randomUUID();

		try {
			this.proxyPort = Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_PORT));
		}
		catch (Exception e) {
			EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_PORT, "3128");
			this.proxyPort = 3128;
			Engine.logProxyManager.debug("(ProxyManager) Couldn't set proxy property, proxyPort parsing failed: " + e);
			Engine.logProxyManager.info("Wrong proxy configuration: bad proxy port");
		}
		this.proxyMethod = EnginePropertiesManager.getPropertyAsEnum(PropertyName.PROXY_SETTINGS_METHOD);
		this.proxyMode = EnginePropertiesManager.getPropertyAsEnum(PropertyName.PROXY_SETTINGS_MODE);
		this.proxyServer = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_HOST);
		this.proxyUser = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_USER);
		this.proxyPassword = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_PASSWORD);
		this.proxyUrl = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_AUTO);
		this.bypassDomains = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS);

		if (proxyMode == ProxyMode.auto) {
			this.pacUtils = new PacManager(proxyUrl);
			this.pacUtils.start();
		}

		if (Engine.theApp != null) {
			Engine.theApp.httpClient4 = HttpUtils.makeHttpClient(true);
		}
	}

	public boolean isEnabled() {
		return ProxyMode.off != proxyMode;
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

		if (isEnabled()) {
			if (needProxy) {
				if (proxyMode == ProxyMode.manual) {
					if (!proxyServer.equals("")) {
						hostConfiguration.setProxy(proxyServer, proxyPort);
						Engine.logProxyManager.debug("(ProxyManager) Using proxy: " + proxyServer + ":" + proxyPort);
					} else {
						disableProxy(hostConfiguration);
					}
				}
				else if (proxyMode == ProxyMode.auto) {
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

				if (proxyMethod == ProxyMethod.basic) {
					setBasicAuth(httpState);
				}
				else if (proxyMethod == ProxyMethod.ntlm) {
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

			Engine.logProxyManager.debug("(ProxyManager) Using credentials: " + proxyUser
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
		if ((!this.proxyServer.equals("")) && (this.proxyUser.equals(""))) {
			httpState.setProxyCredentials(
					new AuthScope(AuthScope.ANY),
					new Credentials() {
					});

			Engine.logProxyManager.debug("(ProxyManager) Proxy credentials: anonym");
		}
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
