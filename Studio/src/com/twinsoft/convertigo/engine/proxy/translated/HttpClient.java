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

package com.twinsoft.convertigo.engine.proxy.translated;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;

import com.twinsoft.convertigo.beans.connectors.ConnectionException;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;
import com.twinsoft.convertigo.engine.proxy.cache.CacheEntry;

public class HttpClient {
	private HttpMethod method = null;
	private HtmlInputStream htmlInputStream = new HtmlInputStream();
		
	public void connect(ParameterShuttle infoShuttle) throws Exception {
		
		boolean noEncoding = false;
		boolean zipEncoding = false;
		boolean doConvert = false;

		HttpConnector connector = (HttpConnector) infoShuttle.context.getConnector();

		// filter/convert http headers before they are outgoing to remote site
		HttpBridge.convertIncomingRequest(infoShuttle);

		// create HttpURLConnection
		Engine.logEngine.debug("(HttpClient) Site URL: " + infoShuttle.siteURL);

		String resourceUrl = infoShuttle.siteURL.toString();
		infoShuttle.siteInputStream = ProxyServletRequester.proxyCacheManager.getResource(resourceUrl);
		
		if (infoShuttle.siteInputStream == null) {
			Engine.logEngine.debug("(HttpClient) Resource requested: " + resourceUrl);

			// Getting result
			Engine.logEngine.debug("(HttpClient) Getting response");
			byte[] result = getData(connector, resourceUrl, infoShuttle);
			Engine.logEngine.trace("(HttpClient) Data received:\n" + new String(result));
			
			// get connected and get info from HTTP headers and save them into ParameterShuttle
			infoShuttle.httpCode = method.getStatusCode();
			Engine.logEngine.debug("(HttpClient) Response code: " + infoShuttle.httpCode);

			Header contentLength = method.getResponseHeader("Content-Length");
			if (contentLength != null) infoShuttle.siteContentSize = Integer.parseInt(contentLength.getValue());
			Header contentType = method.getResponseHeader("Content-Type");
			if (contentType != null) infoShuttle.siteContentType = contentType.getValue();

			Engine.logEngine.debug("(HttpClient) Content type: " + infoShuttle.siteContentType);

			infoShuttle.siteContentHTML = infoShuttle.siteContentType != null && infoShuttle.siteContentType.toLowerCase().startsWith("text/html");

			if (infoShuttle.siteContentHTML) {
				noEncoding = true;
				Header encodingHeader = method.getResponseHeader("Content-Encoding");
				if (encodingHeader != null) {
					String contentEncoding = encodingHeader.getValue();
					if (contentEncoding == null || contentEncoding.length() == 0)
						noEncoding = true;
					else if (contentEncoding.toLowerCase().equals("gzip"))
						zipEncoding = true;
				}

				doConvert = noEncoding || zipEncoding;
			}

			// filter/convert http headers before they are outgoing to user
			Engine.logEngine.debug("(HttpClient) Outcoming HTTP headers:");
			Header[] headers = method.getResponseHeaders();
			Header header;
			String value, key;
			for (int index = 0 ; index < headers.length ; index++) {
				header = headers[index];
				value = header.getValue();

				key = header.getName();
				if (key == null)
					continue;

				infoShuttle.siteHeaderNames.add(key.toLowerCase());
				infoShuttle.siteHeaderValues.add(value);
				Engine.logEngine.debug(key.toLowerCase() + "=" + value);
			}

			HttpBridge.convertOutgoingHeaders(infoShuttle, !doConvert);

			try {
				infoShuttle.siteInputStream = method.getResponseBodyAsStream();
			}
			catch (Exception e) {
				return;
			}
			Engine.logEngine.debug("(HttpClient) Connection opened");
		}
		else {
			Engine.logEngine.debug("(HttpClient) Resource returned from the cache: " + resourceUrl);
			CacheEntry cacheEntry = ProxyServletRequester.proxyCacheManager.getCacheEntry(resourceUrl);
			infoShuttle.siteContentSize = cacheEntry.contentLength;
			infoShuttle.siteContentType = cacheEntry.contentType;
			infoShuttle.siteContentHTML = false;
		}

		// choose right InputStream to output to browser
		if (doConvert) {
			if (zipEncoding) {
				infoShuttle.siteInputStream = htmlInputStream.open(infoShuttle, new GZIPInputStream(infoShuttle.siteInputStream), infoShuttle.siteURL);
			}
			else if (noEncoding) {
				infoShuttle.siteInputStream = htmlInputStream.open(infoShuttle, infoShuttle.siteInputStream, infoShuttle.siteURL);
			}
		}
	}

	public void disconnect() {
		if (method != null) {
			method.releaseConnection();
			method = null;
			
			htmlInputStream.close();
		}
	}
	
	private HttpState httpState = null;
	private boolean handleCookie = true;
	
	private void getHttpState(HttpConnector connector, ParameterShuttle infoShuttle) {
		Context context = infoShuttle.context;
		
		if (context.httpState == null) {
			Engine.logEngine.debug("(HttpClient) Creating new HttpState for context id "+ context.contextID);
	        httpState = new HttpState();
	        
			context.httpState = httpState;
		}
		else {
			Engine.logEngine.debug("(HttpClient) Using HttpState of context id "+ context.contextID);
			httpState = context.httpState;
		}
	}
	
	private byte[] getData(HttpConnector connector, String resourceUrl, ParameterShuttle infoShuttle) throws IOException, EngineException {
		byte[] result = null;
		
		try {
			Context context = infoShuttle.context;

			String proxyServer = Engine.theApp.proxyManager.getProxyServer();
			String proxyUser = Engine.theApp.proxyManager.getProxyUser();
			String proxyPassword = Engine.theApp.proxyManager.getProxyPassword();
			int proxyPort = Engine.theApp.proxyManager.getProxyPort();
			
			HostConfiguration hostConfiguration = connector.hostConfiguration;
			
			boolean trustAllServerCertificates = connector.isTrustAllServerCertificates();
			
			// Retrieving httpState
			getHttpState(connector, infoShuttle);
			
			Engine.logEngine.trace("(HttpClient) Retrieving data as a bytes array...");
			Engine.logEngine.debug("(HttpClient) Connecting to: " + resourceUrl);
			
			// Proxy configuration
			if (!proxyServer.equals("")) {
				hostConfiguration.setProxy(proxyServer, proxyPort);
				Engine.logEngine.debug("(HttpClient) Using proxy: " + proxyServer + ":" + proxyPort);
			}
			else {
				// Remove old proxy configuration
				hostConfiguration.setProxyHost(null);
			}

			Engine.logEngine.debug("(HttpClient) Https: " + connector.isHttps());
			CertificateManager certificateManager = connector.certificateManager;
			
			URL url = null;
			String host = "";
			int port = -1;
			if (resourceUrl.toLowerCase().startsWith("https:")) {
				Engine.logEngine.debug("(HttpClient) Setting up SSL properties");
				certificateManager.collectStoreInformation(context);
				
				url = new URL(resourceUrl);
				host = url.getHost();
				port = url.getPort();
				if (port == -1) port = 443;

				Engine.logEngine.debug("(HttpClient) Host: " + host + ":" + port);

				Engine.logEngine.debug("(HttpClient) CertificateManager has changed: " + certificateManager.hasChanged);
				if (certificateManager.hasChanged || (!host.equalsIgnoreCase(hostConfiguration.getHost())) || (hostConfiguration.getPort() != port)) {
					Engine.logEngine.debug("(HttpClient) Using MySSLSocketFactory for creating the SSL socket");
					Protocol myhttps = new Protocol("https", MySSLSocketFactory.getSSLSocketFactory(
							certificateManager.keyStore, certificateManager.keyStorePassword,
							certificateManager.trustStore, certificateManager.trustStorePassword,
							trustAllServerCertificates), port);

					hostConfiguration.setHost(host, port, myhttps);
				}

				resourceUrl = url.getFile();
				Engine.logEngine.debug("(HttpClient) Updated URL for SSL purposes: " + resourceUrl);
			}
			else {
				url = new URL(resourceUrl);
				host = url.getHost();
				port = url.getPort();

				Engine.logEngine.debug("(HttpClient) Host: " + host + ":" + port);
				hostConfiguration.setHost(host, port);
			}
			
			Engine.logEngine.debug("(HttpClient) Building method on: " + resourceUrl);
			Engine.logEngine.debug("(HttpClient) postFromUser=" + infoShuttle.postFromUser);
			Engine.logEngine.debug("(HttpClient) postToSite=" + infoShuttle.postToSite);
			if (infoShuttle.postFromUser && infoShuttle.postToSite) {
				method = new PostMethod(resourceUrl);
				((PostMethod) method).setRequestEntity(new StringRequestEntity(
						infoShuttle.userPostData, infoShuttle.userContentType,
						infoShuttle.context.httpServletRequest.getCharacterEncoding()));
			}
			else {
				method = new GetMethod(resourceUrl);
			}
			
			HttpMethodParams httpMethodParams = method.getParams();
			
			// Cookie configuration
			if (handleCookie) {
				Engine.logEngine.debug("(HttpClient) Setting cookie policy.");
				httpMethodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			}

			String basicUser = connector.getBasicUser();
			String basicPassword = connector.getBasicPassword();
			String givenBasicUser = connector.getGivenBasicUser();
			String givenBasicPassword = connector.getGivenBasicPassword();

			// Basic authentication configuration
			String realm = null;
			if (!basicUser.equals("") || (basicUser.equals("") && (givenBasicUser != null))) {
				String userName = ((givenBasicUser == null) ? basicUser:givenBasicUser);
				String userPassword = ((givenBasicPassword == null) ? basicPassword:givenBasicPassword);
				httpState.setCredentials(new AuthScope(host, port, realm), new UsernamePasswordCredentials(userName, userPassword));
				Engine.logEngine.debug("(HttpClient) Credentials: " + userName + ":******");
			}
			
			// Setting basic authentication for proxy
			if (!proxyServer.equals("") && !proxyUser.equals("")) {
				httpState.setProxyCredentials(new AuthScope(proxyServer, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPassword));
				Engine.logEngine.debug("(HttpClient) Proxy credentials: " + proxyUser + ":******");
			}
						
			// Setting HTTP headers
			Engine.logEngine.debug("(HttpClient) Incoming HTTP headers:");
			String headerName, headerValue;
			for (int k = 0, kSize = infoShuttle.userHeaderNames.size() ; k < kSize ; k++) {
				headerName = (String) infoShuttle.userHeaderNames.get(k);
				// Cookies are handled by HttpClient, so we do not have to proxy Cookie header
				// See #986 (Multiples cookies don't work with some proxies)
				if (headerName.toLowerCase().equals("cookie")) {
					Engine.logEngine.debug("Cookie header ignored");
				}
				else {
					headerValue = (String) infoShuttle.userHeaderValues.get(k);
					method.setRequestHeader(headerName, headerValue);
					Engine.logEngine.debug(headerName + "=" + headerValue);
				}
			}
			
			// Getting the result
			executeMethod(method, connector, resourceUrl, infoShuttle);
			result = method.getResponseBody();

		}
		finally {
			if (method != null) method.releaseConnection();
		}
		
		return result;
	}
	
	private void executeMethod(HttpMethod method, HttpConnector connector, String resourceUrl, ParameterShuttle infoShuttle) throws IOException, URIException, MalformedURLException, EngineException {
		doExecuteMethod(method, connector, resourceUrl, infoShuttle);
	}
	
	private int doExecuteMethod(HttpMethod method, HttpConnector connector, String resourceUrl, ParameterShuttle infoShuttle) throws ConnectionException, URIException, MalformedURLException {
		int statuscode = -1;
		
		HostConfiguration hostConfiguration = connector.hostConfiguration;
		
		// Tells the method to automatically handle authentication.
		method.setDoAuthentication(true);
		
		// Tells the method to automatically handle redirection.
		method.setFollowRedirects(false);
		
		try {
	        // Display the cookies
			if (handleCookie) {
		        Cookie[] cookies = httpState.getCookies();
		        if (Engine.logEngine.isTraceEnabled())
		        	Engine.logEngine.trace("(HttpClient) request cookies:" + Arrays.asList(cookies).toString());
			}
			
			Engine.logEngine.debug("(HttpClient) executing method...");
			statuscode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
			Engine.logEngine.debug("(HttpClient) end of method successfull");
			
	        // Display the cookies
			if (handleCookie) {
		        Cookie[] cookies = httpState.getCookies();
		        if (Engine.logEngine.isTraceEnabled())
		        	Engine.logEngine.trace("(HttpClient) response cookies:" + Arrays.asList(cookies).toString());
			}
		}
		catch(IOException e) {
			try {
				Engine.logEngine.warn("(HttpClient) connection error to " + resourceUrl + ": " + e.getMessage() + "; retrying method");
				statuscode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
				Engine.logEngine.debug("(HttpClient) end of method successfull");
			}
			catch(IOException ee) {
				throw new ConnectionException("Connection error to " + resourceUrl, ee);
			}
		}
		return statuscode;
	}
	
}