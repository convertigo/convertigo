/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.KeyExpiredException;
import com.twinsoft.convertigo.engine.MaxCvsExceededException;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpPool;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.tas.KeyManager;

public class HttpUtils {
	private final static Pattern c8o_request_pattern = Pattern.compile("((.*?)/(?:projects/(.+?)|admin))/.*");
	
	private static String endpointExUrl = null;
	private static Matcher endpointMatcher = Pattern.compile("http(s)?://(.*?)(/.*)").matcher("");
	
	private static Matcher getEndpointMatcher() {
		synchronized (endpointMatcher) {
			String url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_ENDPOINT);
			if (!url.equals(endpointExUrl)) {
				endpointExUrl = url;
				endpointMatcher.reset(endpointExUrl);
			}
			if (endpointMatcher.matches()) {
				return endpointMatcher;
			}
		}
		return null;
	}
	
	public static String originalRequestURI(HttpServletRequest request) {
		String uri;
		Matcher endpoint = getEndpointMatcher();
		if (endpoint != null && !(request instanceof InternalHttpServletRequest)) {
			String path = request.getContextPath();
			String rUri = request.getRequestURI();
			String base = endpoint.group(3);
			uri = base + rUri.substring(path.length());
		} else {
			uri = HeaderName.XConvertigoRequestURI.getHeader(request);
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
		}
		return uri;
	}
	
	public static String originalRequestURL(HttpServletRequest request) {
		String uri = originalRequestURI(request);
		Matcher endpoint = getEndpointMatcher();
		String host = endpoint != null ? endpoint.group(2) : HeaderName.XConvertigoRequestHost.getHeader(request);
		String https_state = endpoint != null ? (endpoint.group(1) == null ? "off" : "on") : HeaderName.XConvertigoHttpsState.getHeader(request);
		
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
				maxTotalConnections = Integer.valueOf(
						EnginePropertiesManager
								.getProperty(PropertyName.HTTP_CLIENT_MAX_TOTAL_CONNECTIONS))
						.intValue();
			} catch (NumberFormatException e) {
				Engine.logEngine
						.warn("Unable to retrieve the max number of connections; defaults to 100.");
			}
	
			int maxConnectionsPerHost = 50;
			try {
				maxConnectionsPerHost = Integer.valueOf(
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
	
	public static interface HttpClientInterface {

		CloseableHttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException;
		
	}
	
	public static HttpClientInterface makeHttpClient(boolean usePool) {
		
		return new HttpClientInterface() {
			private CloseableHttpClient httpClient = null;
			
			private synchronized CloseableHttpClient getHttpClient4() {
				if (httpClient == null) {
					HttpClientBuilder httpClientBuilder = HttpClients.custom();
					if (Engine.theApp != null && Engine.theApp.proxyManager.proxyMode == ProxyMode.manual) {
						HttpHost proxy = new HttpHost(Engine.theApp.proxyManager.getProxyServer(), Engine.theApp.proxyManager.getProxyPort());
						httpClientBuilder.setProxy(proxy);
						HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy) {

						    @Override
						    public HttpRoute determineRoute(
						            final HttpHost host,
						            final HttpRequest request,
						            final HttpContext context) throws HttpException {
						        String hostname = host.getHostName();
						        for (String domain: Engine.theApp.proxyManager.getBypassDomains()) {
						        	if (hostname.equals(domain)) {
						        		return new HttpRoute(host);
						        	}
						        }
						        return super.determineRoute(host, request, context);
						    }
						};
						httpClientBuilder.setRoutePlanner(routePlanner);
						if (Engine.theApp.proxyManager.proxyMethod == ProxyMethod.basic) {
							CredentialsProvider credsProvider = new BasicCredentialsProvider();
							credsProvider.setCredentials(
									new AuthScope(proxy.getHostName(), proxy.getPort()),
									new UsernamePasswordCredentials(Engine.theApp.proxyManager.getProxyUser(), Engine.theApp.proxyManager.getProxyPassword()));
							httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
						};
					}
					
					@SuppressWarnings("deprecation")
					String spec = CookieSpecs.BROWSER_COMPATIBILITY;
					httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(spec).build());
					
					if (usePool) {
						PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
						
						int maxTotalConnections = 100;
						try {
							maxTotalConnections = Integer.valueOf(
									EnginePropertiesManager
											.getProperty(PropertyName.HTTP_CLIENT_MAX_TOTAL_CONNECTIONS))
									.intValue();
						} catch (NumberFormatException e) {
							Engine.logEngine
									.warn("Unable to retrieve the max number of connections; defaults to 100.");
						}
						
						int maxConnectionsPerHost = 50;
						try {
							maxConnectionsPerHost = Integer.valueOf(
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
					
					
					httpClient = httpClientBuilder.build();
				}
				return httpClient;
			}
			
			@Override
			public CloseableHttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException {
				try {
					return getHttpClient4().execute(request);
				} catch (IllegalStateException e) {
					Engine.logEngine.warn("(HttpUtils) HttpClient4 retry execute because of IllegalStateException: " + e.getMessage());
					httpClient = null;
					return getHttpClient4().execute(request);
				}
			}
			
		};
	}
	
	public static void terminateSession(HttpSession httpSession) {
		terminateSession(httpSession, false);
	}
	
	public static void terminateSession(HttpSession httpSession, boolean force) {
		if (httpSession != null) {
			if (force || Engine.authenticatedSessionManager.isAnonymous(httpSession)) {
				httpSession.setMaxInactiveInterval(1);
				HttpSessionListener.removeSession(httpSession.getId());
				if (Engine.theApp != null && Engine.theApp.contextManager != null) {
					Engine.theApp.contextManager.removeAll(httpSession);
				}
			}
		}
	}
	
	public static JSONObject requestToJSON(CloseableHttpClient httpClient, HttpRequestBase request) throws ClientProtocolException, IOException, UnsupportedOperationException, JSONException {
		CloseableHttpResponse response = httpClient.execute(request);
		HttpEntity responseEntity = response.getEntity();
		JSONObject json;
		if (responseEntity != null) {
			ContentTypeDecoder contentType = new ContentTypeDecoder(responseEntity.getContentType() == null  ? "" : responseEntity.getContentType().getValue());
			json = new JSONObject(IOUtils.toString(responseEntity.getContent(), contentType.charset("UTF-8")));
		} else {
			json = new JSONObject();
		}
		
		return json;
	}
	
	public static String applyCorsHeaders(HttpServletRequest request, HttpServletResponse response, String corsOrigin, String methods) {
		if (corsOrigin != null && !RequestAttribute.corsOrigin.has(request)) {
			HeaderName.AccessControlAllowOrigin.setHeader(response, corsOrigin);
			HeaderName.AccessControlAllowCredentials.setHeader(response, "true");

			if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
				String method = HeaderName.AccessControlRequestMethod.getHeader(request);
				if (method != null) {
					if (methods == null) {
						methods = "GET, POST, PUT, OPTIONS, DELETE";
						if (!methods.contains(method.toUpperCase())) {
							methods += ", " + method;
						}
					}
	
					if (StringUtils.isNotBlank(methods)) {
						HeaderName.AccessControlAllowMethods.setHeader(response, methods);
					}
				}

				String headers = HeaderName.AccessControlRequestHeaders.getHeader(request);
				if (headers != null) {
					HeaderName.AccessControlAllowHeaders.setHeader(response, headers);
				}	
			}
		}
		RequestAttribute.corsOrigin.set(request, corsOrigin == null ? "" : corsOrigin);
		return corsOrigin;
	}
	
	public static String applyCorsHeaders(HttpServletRequest request, HttpServletResponse response, String corsOrigin) {
		return applyCorsHeaders(request, response, corsOrigin, null);
	}
	
	public static String applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
		String origin = HeaderName.Origin.getHeader(request);
		try {
			String globalCorsPolicy = EnginePropertiesManager.getOriginalProperty(PropertyName.CORS_POLICY);
			return applyCorsHeaders(request, response, filterCorsOrigin(globalCorsPolicy, origin));
		} catch (Exception e) {
			if (Engine.logEngine != null) {
				if (e instanceof IllegalStateException) {
					Engine.logEngine.warn("Cannot retrieve properties for applyCorsHeaders, Engine probably stopped.");
				} else {
					Engine.logEngine.warn("(HttpUtils) Failed applyCorsHeaders", e);
				}
			}
			return null;
		}
	}
	
	public static String applyFilterCorsHeaders(HttpServletRequest request, HttpServletResponse response, String corsPolicy) {
		String origin = HeaderName.Origin.getHeader(request);
		return applyCorsHeaders(request, response, filterCorsOrigin(corsPolicy, origin));
	}
	
	public static String filterCorsOrigin(String corsPolicy, String origin) {
		String corsOrigin = null;
		if (origin != null && StringUtils.isNotBlank(corsPolicy)) {
			if (corsPolicy.equals("=Global")) {
				try {
					String globalCorsPolicy = EnginePropertiesManager.getOriginalProperty(PropertyName.CORS_POLICY);
					if (!globalCorsPolicy.equals(corsPolicy)) {
						corsOrigin = filterCorsOrigin(globalCorsPolicy, origin);
					}
				} catch (Exception e) {
					if (Engine.logEngine != null) {
						Engine.logEngine.warn("(HttpUtils) Failed filterCorsOrigin", e);
					}
				}
			} else if (corsPolicy.equals("=Origin")) {
				corsOrigin = origin;
			} else if (corsPolicy.equals("*")) {
				corsOrigin = "*";
			} else {
				String[] urls = corsPolicy.split("#");
				for (String url: urls) {
					if (url.equals(origin)) {
						corsOrigin = origin;
						break;
					}
				}
			}
		}
		return corsOrigin;
	}
	
	public static void checkCV(HttpServletRequest request) throws EngineException {
		if (Engine.isEngineMode()) {
			String msg;
			
			String c8oMB = request.getHeader(HeaderName.XConvertigoMB.value());
			if (c8oMB != null) {
				Engine.logEngine.debug("Request from Mobile Builder: " + c8oMB);
				if (KeyManager.getCV(Session.EmulIDMOBILEBUILDER) < 1) {
					HttpUtils.terminateSession(request.getSession());
					if (KeyManager.has(Session.EmulIDMOBILEBUILDER) && KeyManager.hasExpired(Session.EmulIDMOBILEBUILDER)) {
						Engine.logEngine.error(msg = "Key expired for the Mobile Builder.");
						throw new KeyExpiredException(msg);
					}
					Engine.logEngine.error(msg = "No key for the Mobile Builder.");
					throw new MaxCvsExceededException(msg);
				}
			}
			
			String c8oSDK = request.getHeader(HeaderName.XConvertigoSDK.value());
			if (c8oSDK != null) {
				Engine.logEngine.debug("Request from Convertigo SDK: " + c8oSDK);
				if (c8oMB == null && KeyManager.getCV(Session.EmulIDC8OSDK) < 1) {
					HttpUtils.terminateSession(request.getSession());
					if (KeyManager.has(Session.EmulIDC8OSDK) && KeyManager.hasExpired(Session.EmulIDC8OSDK)) {
						Engine.logEngine.error(msg = "Key expired for the Convertigo SDK.");
						throw new KeyExpiredException(msg);
					}
					Engine.logEngine.error(msg = "No key for the Convertigo SDK.");
					throw new MaxCvsExceededException(msg);
				}
			}
		}
	}

	public static void terminateNewSession(HttpSession httpSession) {
		if (httpSession != null && httpSession.isNew()) {
			terminateSession(httpSession);
		}
	}

	public static void checkXSRF(HttpServletRequest request, HttpServletResponse response) throws EngineException {
		HttpSession session = request.getSession(true);
		String token = SessionAttribute.xsrfToken.get(session);
		String header = HeaderName.XXsrfToken.getHeader(request);
		if (token == null && header != null) {
			token = Base64.encodeBytes(DigestUtils.sha256(session.getId() + Engine.startStopDate)).replaceAll("[^\\w\\d]", "-");
			SessionAttribute.xsrfToken.set(session, token);
			HeaderName.XXsrfToken.setHeader(response, token);
		} else {
			if (header == null) {
				header = request.getParameter(Parameter.XsrfToken.getName());
			}
			if (token != null && !token.equals(header)) {
				EngineException e = new EngineException("Invalid XSRF Token header for this session.");
				e.setStackTrace(new StackTraceElement[0]);
				throw e;
			}
		}
	}
	
	public static void downloadFile(String url, File file) throws ClientProtocolException, IOException {
		var client = Engine.theApp != null ? Engine.theApp.httpClient4 : makeHttpClient(false);
		var get = new HttpGet(url);
		get.setConfig(RequestConfig.custom().setRedirectsEnabled(true).build());
		try (CloseableHttpResponse response = client.execute(get)) {
			var code = response.getStatusLine().getStatusCode();
			if (code < 200 || code >= 300) {
				throw new HttpResponseException(code, "(HttpUtils) downloadFile '" + url + "' failed, http status: " + code);
			}
			FileUtils.deleteQuietly(file);
			file.getParentFile().mkdirs();
			long length = response.getEntity().getContentLength();
			String sl = Long.toString(length);
			if (length < 1) {
				length = Integer.MAX_VALUE;
				sl = "??";
			}
			try (FileOutputStream fos = new FileOutputStream(file)) {
				InputStream is = response.getEntity().getContent();
				byte[] buf = new byte[1024 * 1024];
				int n;
				long t = 0, now, ts = 0;
				while (t < length && (n = is.read(buf, 0, (int) Math.min(length - t, buf.length))) > -1) {
					fos.write(buf, 0, n);
					t += n;
					now = System.currentTimeMillis();
					if (now > ts) {
						Engine.logEngine
								.debug("Download " + file.getName() + " from " + url + " : " + t + " / " + sl);
						ts = now + 2000;
					}
				}
				Engine.logEngine.debug("Download " + file.getName() + " from " + url + " : " + t + " / " + sl);
			}
			Engine.logEngine.info("Downloaded " + url + " to " + file.toString());
		}
	}
}
