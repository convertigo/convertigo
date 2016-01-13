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

package com.twinsoft.convertigo.engine.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpPool;

public class HttpUtils {
	private final static Pattern c8o_request_pattern = Pattern.compile("((.*?)/(?:projects/(.+?)|admin))/.*");

	/**
	 * Calls a convertigo transaction from the same server and port than
	 * the HTTP servlet request. It will also use the same Convertigo web
	 * application directory (i.e. same "web-app") and the same project name.
	 * 
	 * @param request the original HTTP servlet request.
	 * @param convertigoRequest the Convertigo request (e.g. ".cxml?__transaction=mytransaction?param1=value1&param2=value2").
	 * @param bNewSession defines if a new session should be created or if the existing HTTP servlet session should be used.
	 * 
	 * @returns the Convertigo response (text).
	 * 
	 * @throws Exception if any error occurs.
	 */
	public static String callConvertigoTransaction(HttpServletRequest request, String convertigoRequest, boolean bNewSession) throws Exception {
		return callConvertigoTransaction(request, convertigoRequest, bNewSession, null);
	}

	public static String callConvertigoTransaction(HttpServletRequest request, String convertigoRequest, boolean bNewSession, String data) throws Exception {
		//URL url = new URL(javax.servlet.http.HttpUtils.getRequestURL(request).toString());
		URL url = new URL(request.getRequestURL().toString());
		String protocol = url.getProtocol();
		String serverName = url.getHost();
		int serverPort = url.getPort();
		
		String convertigoBase = url.getPath();
		int i = convertigoBase.indexOf('/');
		int j = convertigoBase.indexOf('/', i + 1);
		String convertigoWebApp = convertigoBase.substring(i + 1, j);
		i = convertigoBase.indexOf('/', j + 1);
		j = convertigoBase.indexOf('/', i + 1);
		String convertigoProjectName = convertigoBase.substring(i + 1, j);
		
		String fullyQualifiedConvertigoRequest = protocol + "://" + serverName + (serverPort == -1 ? "" : ":" + serverPort) + "/" + convertigoWebApp + "/projects/" + convertigoProjectName + "/" + convertigoRequest;
		return callConvertigoTransactionEx(request, fullyQualifiedConvertigoRequest, bNewSession, data);
	}

	/**
	 * Calls a convertigo transaction.
	 * 
	 * @param request the original HTTP servlet request.
	 * @param fullyQualifiedConvertigoRequest the Convertigo request (e.g. "http://localhost:8090/convertigo/projects/myproject/.cxml?__transaction=mytransaction?param1=value1&param2=value2").
	 * @param bNewSession defines if a new session should be created or if the existing HTTP servlet session should be used.
	 * 
	 * @returns the Convertigo response (text).
	 * 
	 * @throws Exception if any error occurs.
	 */
	public static String callConvertigoTransactionEx(HttpServletRequest request, String fullyQualifiedConvertigoRequest, boolean bNewSession) throws Exception {
		return callConvertigoTransactionEx(request, fullyQualifiedConvertigoRequest, bNewSession, (String) null);
	}

	public static String callConvertigoTransactionEx(HttpServletRequest request, String fullyQualifiedConvertigoRequest, boolean bNewSession, String data) throws Exception {
		URL url = new URL(fullyQualifiedConvertigoRequest);
		URLConnection urlConnection = url.openConnection();
		HttpSession session = request.getSession();

		Engine.logEngine.debug("(HttpUtils) Protocol: " + url.getProtocol());
		Engine.logEngine.debug("(HttpUtils) Host: " + url.getHost());
		Engine.logEngine.debug("(HttpUtils) Port: " + url.getPort());
		Engine.logEngine.debug("(HttpUtils) Request: " + fullyQualifiedConvertigoRequest);
		Engine.logEngine.debug("(HttpUtils) SessionID: " + session.getId());
		Engine.logEngine.debug("(HttpUtils) New session required: " + bNewSession);

		String sCookies = null;

		urlConnection.setDoOutput(true);
		urlConnection.setDoInput(true);
		
		// Retrieve cookies
		if (bNewSession) {
			// Cookies from the new created session
			Engine.logEngine.debug("(HttpUtils) Retrieving cookies from the new created session");
			sCookies = (String) session.getAttribute("cookies");
		}
		else {
			// Cookies from the existing HTTP session
			Engine.logEngine.debug("(HttpUtils) Retrieving cookies from the current request");
			Cookie[] cookies = request.getCookies();
			Cookie cookie;
			if (cookies != null) {
				sCookies = "";
				for (int k = 0; k < cookies.length; k++) {
					cookie = cookies[k];
					sCookies += cookie.getName() + "=" + cookie.getValue() + ";";
				}
			}
		}

		Engine.logEngine.debug("(HttpUtils) Cookies: " + sCookies);
		
		if (sCookies != null) {
			urlConnection.setRequestProperty("Cookie", sCookies);
		}

		if (data != null) {
			Engine.logEngine.debug("(HttpUtils) Data:\n" + data);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
            out.write(data);
            out.close();
		} 
		
		InputStream is = urlConnection.getInputStream();

		// Retrieve cookies
		if (bNewSession) {
			// Cookies from the new created session
			String newCookies = urlConnection.getHeaderField("Set-Cookie");

			if (newCookies != null) {
				try {
					newCookies = newCookies.substring(0, newCookies.indexOf(';'));
				}
				catch (StringIndexOutOfBoundsException e) {
					// Do nothing: get the whole cookie
				}
			}
			
			if ((sCookies == null) || ((newCookies != null) && (!sCookies.equalsIgnoreCase(newCookies)))) {
				sCookies = newCookies;
			}

			// Storing cookies
			if (sCookies != null) {
				Engine.logEngine.debug("(HttpUtils) Stored cookies: " + sCookies);
				session.setAttribute("cookies", sCookies);
			}
		}

		Engine.logEngine.debug("(HttpUtils) Receiving response from Convertigo...");
		StringBuffer response = new StringBuffer("");
		String line;
		BufferedReader bf = new BufferedReader(new InputStreamReader(is));
		while ((line = bf.readLine()) != null) {
			response.append(line);
			response.append("\n");
		}

		Engine.logEngine.debug("(HttpUtils) Response from Convertigo received!");
		return response.toString();
	}

	public static String originalRequestURI(HttpServletRequest request) {
		String uri = request.getHeader(HeaderName.XConvertigoRequestURI.value());
		if (uri == null) {
			uri = request.getRequestURI();
		} else {
			String frontal = request.getHeader(HeaderName.XConvertigoFrontal.value());
			if ("apache".equalsIgnoreCase(frontal)) {
				try {
					uri = new URI(null, null, uri, null).toASCIIString();
				} catch (URISyntaxException e) {
					// Transformation failed, keep existing uri
					Engine.logEngine.debug("(HttpUtils) Apache URI escape failed : " + e.getMessage());
				}
			}
		}
		return uri;
	}
	
	public static String originalRequestURL(HttpServletRequest request) {
		String uri = originalRequestURI(request);
		String host = request.getHeader(HeaderName.XConvertigoRequestHost.value());
		String https_state = request.getHeader(HeaderName.XConvertigoHttpsState.value());
		
		if (uri != null && host != null && https_state != null) {
			String url = "http" + (https_state.equalsIgnoreCase("on") ? "s" : "") + "://" + host + uri;
			return url;
		}
		return request.getRequestURL().toString();
	}
	
	public static String convertigoRequestURL(HttpServletRequest request) {
		String url = originalRequestURL(request);
		url = c8o_request_pattern.matcher(url).replaceFirst("$2");
		return url;
	}
	
	public static String projectRequestURL(HttpServletRequest request) {
		String url = originalRequestURL(request);
		url = c8o_request_pattern.matcher(url).replaceFirst("$1");
		return url;
	}
	
	public static void logCurrentHttpConnection(HttpClient httpClient, HostConfiguration hostConfiguration, HttpPool poolMode) {
		if (Engine.logEngine.isInfoEnabled() && httpClient != null) {
			if (poolMode == HttpPool.no) {
				Engine.logEngine.info("(HttpUtils) Use a not pooled HTTP connection for " + hostConfiguration.getHost());
			} else {
				HttpConnectionManager httpConnectionManager = httpClient.getHttpConnectionManager();
				if (httpConnectionManager != null && httpConnectionManager instanceof MultiThreadedHttpConnectionManager) {
					MultiThreadedHttpConnectionManager mtHttpConnectionManager = (MultiThreadedHttpConnectionManager) httpConnectionManager;
					int connections = mtHttpConnectionManager.getConnectionsInPool();
					int connectionsForHost = mtHttpConnectionManager.getConnectionsInPool(hostConfiguration);
					Engine.logEngine.info("(HttpUtils) Use a " + poolMode.name() + " pool with " + connections + " HTTP connections, " + connectionsForHost + " for " + hostConfiguration.getHost() + "; Getting one ... [for instance " + httpClient.hashCode() + "]");
				}
			}
		}
	}
	
	public static HttpClient makeHttpClient3(boolean usePool) {
		HttpClient httpClient;
		
		if (usePool) {
			MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
	
			int maxTotalConnections = 100;
			try {
				maxTotalConnections = new Integer(
						EnginePropertiesManager
								.getProperty(PropertyName.HTTP_CLIENT_MAX_TOTAL_CONNECTIONS))
						.intValue();
			} catch (NumberFormatException e) {
				Engine.logEngine
						.warn("Unable to retrieve the max number of connections; defaults to 100.");
			}
	
			int maxConnectionsPerHost = 50;
			try {
				maxConnectionsPerHost = new Integer(
						EnginePropertiesManager
								.getProperty(PropertyName.HTTP_CLIENT_MAX_CONNECTIONS_PER_HOST))
						.intValue();
			} catch (NumberFormatException e) {
				Engine.logEngine
						.warn("Unable to retrieve the max number of connections per host; defaults to 100.");
			}
	
			HttpConnectionManagerParams httpConnectionManagerParams = new HttpConnectionManagerParams();
			httpConnectionManagerParams.setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
			httpConnectionManagerParams.setMaxTotalConnections(maxTotalConnections);
			connectionManager.setParams(httpConnectionManagerParams);
			
			httpClient = new HttpClient(connectionManager);
		} else {
			httpClient = new HttpClient();
		}
		
		HttpClientParams httpClientParams = (HttpClientParams) HttpClientParams.getDefaultParams();
		httpClientParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		/** #741 : belambra wants only one Set-Cookie header */
		httpClientParams.setParameter("http.protocol.single-cookie-header", Boolean.TRUE);
		
		httpClient.setParams(httpClientParams);
		
		return httpClient;
	}
	
	public static CloseableHttpClient makeHttpClient4(boolean usePool) {
		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build());
		
		if (usePool) {
			PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
	
			int maxTotalConnections = 100;
			try {
				maxTotalConnections = new Integer(
						EnginePropertiesManager
								.getProperty(PropertyName.HTTP_CLIENT_MAX_TOTAL_CONNECTIONS))
						.intValue();
			} catch (NumberFormatException e) {
				Engine.logEngine
						.warn("Unable to retrieve the max number of connections; defaults to 100.");
			}
	
			int maxConnectionsPerHost = 50;
			try {
				maxConnectionsPerHost = new Integer(
						EnginePropertiesManager
								.getProperty(PropertyName.HTTP_CLIENT_MAX_CONNECTIONS_PER_HOST))
						.intValue();
			} catch (NumberFormatException e) {
				Engine.logEngine
						.warn("Unable to retrieve the max number of connections per host; defaults to 100.");
			}
			
			connManager.setDefaultMaxPerRoute(maxConnectionsPerHost);
			connManager.setMaxTotal(maxTotalConnections);
			
			httpClientBuilder.setConnectionManager(connManager);
		}
		
		
		return httpClientBuilder.build();
	}
	
	public static void terminateSession(HttpSession httpSession) {
		if (httpSession != null) {
			if ("true".equals("" + httpSession.getAttribute("administration"))) {
				httpSession.setMaxInactiveInterval(1);						
			}
		}
	}
}
