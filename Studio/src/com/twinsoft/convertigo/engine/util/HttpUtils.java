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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
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

	public static String originalRequestURI(HttpServletRequest request) {
		String uri = HeaderName.XConvertigoRequestURI.getHeader(request);
		if (uri == null) {
			uri = request.getRequestURI();
		} else {
			String frontal = HeaderName.XConvertigoFrontal.getHeader(request);
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
		String host = HeaderName.XConvertigoRequestHost.getHeader(request);
		String https_state = HeaderName.XConvertigoHttpsState.getHeader(request);
		
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
		
		/** #5066 : httpClient auto retries failed request up to 3 times by default */
		httpClientParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));

		httpClient.setParams(httpClientParams);
		
		return httpClient;
	}
	
	@SuppressWarnings("deprecation")
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
