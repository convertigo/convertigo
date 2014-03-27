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

package com.twinsoft.convertigo.engine.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.ProxyHttpConnector;
import com.twinsoft.convertigo.beans.connectors.ProxyHttpConnector.Replacements;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;
import com.twinsoft.convertigo.engine.plugins.AbstractBiller;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ReverseProxyServlet extends HttpServlet {

	private static final long serialVersionUID = -6850744063612272691L;

	private static final Pattern reg_fields = Pattern.compile("(.*/rproxy/([^/]*)/([^/]*)/([^/]*)/)(.*)");
	private static final Pattern reg_base = Pattern.compile("\\$base");
	private static final Pattern reg_connector = Pattern.compile("\\$connector=(.*?)\\$");

	// protected static FileCacheManager proxyCacheManager;

	// static {
	// try {
	// proxyCacheManager = new FileCacheManager();
	// proxyCacheManager.init();
	// }
	// catch(Exception e) {
	// Engine.logEngine.error("Unexpected exception", e);
	// }
	// }

	/**
	 * Key for redirect location header.
	 */
	private static final String STRING_LOCATION_HEADER = "Location";

	/**
	 * Key for content length header.
	 */
	private static final String STRING_CONTENT_LENGTH_HEADER_NAME = "Content-Length";

	/**
	 * Key for host header
	 */
	private static final String STRING_HOST_HEADER_NAME = "Host";

	public String getName() {
		return "ReverseProxyServlet";
	}

	public String getDefaultContentType() {
		return "text/html";
	}

	public String getServletInfo() {
		return "Convertigo ReverseProxyServlet";
	}

	public String getDocumentExtension() {
		return null;
	}

	private enum HttpMethodType {
		GET, POST
	};

	/**
	 * Performs an HTTP GET request
	 * 
	 * @param httpServletRequest
	 *            The {@link HttpServletRequest} object passed in by the servlet
	 *            engine representing the client request to be proxied
	 * @param httpServletResponse
	 *            The {@link HttpServletResponse} object by which we can send a
	 *            proxied response to the client
	 */
	public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException, ServletException {
		// Execute the proxy request
		this.doRequest(HttpMethodType.GET, httpServletRequest, httpServletResponse);
	}

	/**
	 * Performs an HTTP POST request
	 * 
	 * @param httpServletRequest
	 *            The {@link HttpServletRequest} object passed in by the servlet
	 *            engine representing the client request to be proxied
	 * @param httpServletResponse
	 *            The {@link HttpServletResponse} object by which we can send a
	 *            proxied response to the client
	 */
	public void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException, ServletException {
		// Execute the proxy request
		this.doRequest(HttpMethodType.POST, httpServletRequest, httpServletResponse);
	}

	/**
	 * Executes the {@link HttpMethod} passed in and sends the proxy response
	 * back to the client via the given {@link HttpServletResponse}
	 * 
	 * @param httpMethodProxyRequest
	 *            An object representing the proxy request to be made
	 * @param httpServletResponse
	 *            An object by which we can send the proxied response back to
	 *            the client
	 * @throws IOException
	 *             Can be thrown by the {@link HttpClient}.executeMethod
	 * @throws ServletException
	 *             Can be thrown to indicate that another error has occurred
	 * @throws EngineException
	 */
	private void doRequest(HttpMethodType httpMethodType, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws IOException, ServletException {
		try {
			Engine.logEngine.debug("(ReverseProxyServlet) Starting request handling");

			if (Boolean.parseBoolean(EnginePropertiesManager.getProperty(PropertyName.SSL_DEBUG))) {
				System.setProperty("javax.net.debug", "all");
				Engine.logEngine.trace("(ReverseProxyServlet) Enabling SSL debug mode");
			} else {
				System.setProperty("javax.net.debug", "");
				Engine.logEngine.debug("(ReverseProxyServlet) Disabling SSL debug mode");
			}

			String baseUrl;
			String projectName;
			String connectorName;
			String contextName;
			String extraPath;

			{
				String requestURI = httpServletRequest.getRequestURI();
				Engine.logEngine.trace("(ReverseProxyServlet) Requested URI : " + requestURI);
				Matcher m = reg_fields.matcher(requestURI);
				if (m.matches() && m.groupCount() >= 5) {
					baseUrl = m.group(1);
					projectName = m.group(2);
					connectorName = m.group(3);
					contextName = m.group(4);
					extraPath = m.group(5);
				} else {
					throw new MalformedURLException(
							"The request doesn't contains needed fields : projectName, connectorName and contextName");
				}
			}

			String sessionID = httpServletRequest.getSession().getId();

			Engine.logEngine.debug("(ReverseProxyServlet) baseUrl : " + baseUrl + " ; projectName : "
					+ projectName + " ; connectorName : " + connectorName + " ; contextName : " + contextName
					+ " ; extraPath : " + extraPath + " ; sessionID : " + sessionID);

			Context context = Engine.theApp.contextManager.get(null, contextName, sessionID, null,
					projectName, connectorName, null);

			Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			context.projectName = projectName;
			context.project = project;

			ProxyHttpConnector proxyHttpConnector = (ProxyHttpConnector) project
					.getConnectorByName(connectorName);
			context.connector = proxyHttpConnector;
			context.connectorName = proxyHttpConnector.getName();

			HostConfiguration hostConfiguration = proxyHttpConnector.hostConfiguration;

			// Proxy configuration
			String proxyServer = Engine.theApp.proxyManager.getProxyServer();
			String proxyUser = Engine.theApp.proxyManager.getProxyUser();
			String proxyPassword = Engine.theApp.proxyManager.getProxyPassword();
			int proxyPort = Engine.theApp.proxyManager.getProxyPort();
			
			
			if (!proxyServer.equals("")) {
				hostConfiguration.setProxy(proxyServer, proxyPort);
				Engine.logEngine.debug("(ReverseProxyServlet) Using proxy: " + proxyServer + ":" + proxyPort);
			} else {
				// Remove old proxy configuration
				hostConfiguration.setProxyHost(null);
			}

			String targetHost = proxyHttpConnector.getServer();
			Engine.logEngine.debug("(ReverseProxyServlet) Target host: " + targetHost);
			int targetPort = proxyHttpConnector.getPort();
			Engine.logEngine.debug("(ReverseProxyServlet) Target port: " + targetPort);

			// Configuration SSL
			Engine.logEngine.debug("(ReverseProxyServlet) Https: " + proxyHttpConnector.isHttps());
			CertificateManager certificateManager = proxyHttpConnector.certificateManager;
			boolean trustAllServerCertificates = proxyHttpConnector.isTrustAllServerCertificates();

			if (proxyHttpConnector.isHttps()) {
				Engine.logEngine.debug("(ReverseProxyServlet) Setting up SSL properties");
				certificateManager.collectStoreInformation(context);

				Engine.logEngine.debug("(ReverseProxyServlet) CertificateManager has changed: "
						+ certificateManager.hasChanged);
				if (certificateManager.hasChanged
						|| (!targetHost.equalsIgnoreCase(hostConfiguration.getHost()))
						|| (hostConfiguration.getPort() != targetPort)) {
					Engine.logEngine
							.debug("(ReverseProxyServlet) Using MySSLSocketFactory for creating the SSL socket");
					Protocol myhttps = new Protocol("https", MySSLSocketFactory.getSSLSocketFactory(
							certificateManager.keyStore, certificateManager.keyStorePassword,
							certificateManager.trustStore, certificateManager.trustStorePassword,
							trustAllServerCertificates), targetPort);

					hostConfiguration.setHost(targetHost, targetPort, myhttps);
				}

				Engine.logEngine.debug("(ReverseProxyServlet) Updated host configuration for SSL purposes");
			} else {
				hostConfiguration.setHost(targetHost, targetPort);
			}

			HttpMethod httpMethodProxyRequest;

			String targetPath = proxyHttpConnector.getBaseDir() + extraPath;

			// Handle the query string
			if (httpServletRequest.getQueryString() != null) {
				targetPath += "?" + httpServletRequest.getQueryString();
			}
			Engine.logEngine.debug("(ReverseProxyServlet) Target path: " + targetPath);

			Engine.logEngine.debug("(ReverseProxyServlet) Requested method: " + httpMethodType);

			if (httpMethodType == HttpMethodType.GET) {
				// Create a GET request
				httpMethodProxyRequest = new GetMethod();
			} else if (httpMethodType == HttpMethodType.POST) {
				// Create a standard POST request
				httpMethodProxyRequest = new PostMethod();
				((PostMethod) httpMethodProxyRequest).setRequestEntity(new InputStreamRequestEntity(
						httpServletRequest.getInputStream()));
			} else {
				throw new IllegalArgumentException("Unknown HTTP method: " + httpMethodType);
			}

			String charset = httpMethodProxyRequest.getParams().getUriCharset();
			URI targetURI;
			try {
				targetURI = new URI(targetPath, true, charset);
			} catch (URIException e) {
				// Bugfix #1484
				String newTargetPath = "";
				for (String part : targetPath.split("&")) {
					if (!newTargetPath.equals("")) {
						newTargetPath += "&";
					}
					String[] pair = part.split("=");
					try {
						newTargetPath += URLDecoder.decode(pair[0], "UTF-8")
								+ "="
								+ (pair.length > 1 ? URLEncoder.encode(URLDecoder.decode(pair[1], "UTF-8"),
										"UTF-8") : "");
					} catch (UnsupportedEncodingException ee) {
						newTargetPath = targetPath;
					}
				}

				targetURI = new URI(newTargetPath, true, charset);
			}
			httpMethodProxyRequest.setURI(targetURI);

			// Tells the method to automatically handle authentication.
			httpMethodProxyRequest.setDoAuthentication(true);

			HttpState httpState = getHttpState(proxyHttpConnector, context);

			String basicUser = proxyHttpConnector.getAuthUser();
			String basicPassword = proxyHttpConnector.getAuthPassword();
			String givenBasicUser = proxyHttpConnector.getGivenAuthUser();
			String givenBasicPassword = proxyHttpConnector.getGivenAuthPassword();

			// Basic authentication configuration
			String realm = null;
			if (!basicUser.equals("") || (basicUser.equals("") && (givenBasicUser != null))) {
				String userName = ((givenBasicUser == null) ? basicUser : givenBasicUser);
				String userPassword = ((givenBasicPassword == null) ? basicPassword : givenBasicPassword);
				httpState.setCredentials(new AuthScope(targetHost, targetPort, realm),
						new UsernamePasswordCredentials(userName, userPassword));
				Engine.logEngine.debug("(ReverseProxyServlet) Credentials: " + userName + ":******");
			}

			// Setting basic authentication for proxy
			if (!proxyServer.equals("") && !proxyUser.equals("")) {
				httpState.setProxyCredentials(new AuthScope(proxyServer, proxyPort),
						new UsernamePasswordCredentials(proxyUser, proxyPassword));
				Engine.logEngine.debug("(ReverseProxyServlet) Proxy credentials: " + proxyUser + ":******");
			}

			// Forward the request headers
			setProxyRequestHeaders(httpServletRequest, httpMethodProxyRequest, proxyHttpConnector);

			// Use the CEMS HttpClient
			HttpClient httpClient = Engine.theApp.httpClient;
			httpMethodProxyRequest.setFollowRedirects(false);

			// Execute the request
			int intProxyResponseCode = httpClient.executeMethod(hostConfiguration, httpMethodProxyRequest,
					httpState);

			// Check if the proxy response is a redirect
			// The following code is adapted from
			// org.tigris.noodle.filters.CheckForRedirect
			// Hooray for open source software
			if (intProxyResponseCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
					&& intProxyResponseCode < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {
				String stringStatusCode = Integer.toString(intProxyResponseCode);
				String stringLocation = httpMethodProxyRequest.getResponseHeader(STRING_LOCATION_HEADER)
						.getValue();
				if (stringLocation == null) {
					throw new ServletException("Received status code: " + stringStatusCode + " but no "
							+ STRING_LOCATION_HEADER + " header was found in the response");
				}
				// Modify the redirect to go to this proxy servlet rather that
				// the
				// proxied host
				String redirect = handleRedirect(stringLocation, baseUrl, proxyHttpConnector);
				httpServletResponse.sendRedirect(redirect);
				Engine.logEngine.debug("(ReverseProxyServlet) Send redirect (" + redirect + ")");
				return;
			} else if (intProxyResponseCode == HttpServletResponse.SC_NOT_MODIFIED) {
				// 304 needs special handling. See:
				// http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
				// We get a 304 whenever passed an 'If-Modified-Since'
				// header and the data on disk has not changed; server
				// responds w/ a 304 saying I'm not going to send the
				// body because the file has not changed.
				httpServletResponse.setIntHeader(STRING_CONTENT_LENGTH_HEADER_NAME, 0);
				httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				Engine.logEngine.debug("(ReverseProxyServlet) NOT MODIFIED (304)");
				return;
			}

			// Pass the response code back to the client
			httpServletResponse.setStatus(intProxyResponseCode);

			// Pass response headers back to the client
			Engine.logEngine.debug("(ReverseProxyServlet) Response headers back to the client:");
			Header[] headerArrayResponse = httpMethodProxyRequest.getResponseHeaders();
			for (Header header : headerArrayResponse) {
				String headerName = header.getName();
				String headerValue = header.getValue();
				if (!headerName.equalsIgnoreCase("Transfer-Encoding")
						&& !headerName.equalsIgnoreCase("Set-Cookie")) {
					httpServletResponse.setHeader(headerName, headerValue);
					Engine.logEngine.debug("   " + headerName + "=" + headerValue);
				}
			}

			String contentType = null;
			Header[] contentTypeHeaders = httpMethodProxyRequest.getResponseHeaders("Content-Type");
			for (Header contentTypeHeader : contentTypeHeaders) {
				contentType = contentTypeHeader.getValue();
				break;
			}

			String pageCharset = "UTF-8";
			if (contentType != null) {
				int iCharset = contentType.indexOf("charset=");
				if (iCharset != -1) {
					pageCharset = contentType.substring(iCharset + "charset=".length()).trim();
				}
				Engine.logEngine.debug("(ReverseProxyServlet) Using charset: " + pageCharset);
			}

			InputStream siteIn = httpMethodProxyRequest.getResponseBodyAsStream();

			// Handle gzipped content
			Header[] contentEncodingHeaders = httpMethodProxyRequest.getResponseHeaders("Content-Encoding");
			boolean bGZip = false, bDeflate = false;
			for (Header contentEncodingHeader : contentEncodingHeaders) {
				HeaderElement[] els = contentEncodingHeader.getElements();
				for (int j = 0; j < els.length; j++) {
					if ("gzip".equals(els[j].getName())) {
						Engine.logBeans.debug("(ReverseProxyServlet) Decode GZip stream");
						siteIn = new GZIPInputStream(siteIn);
						bGZip = true;
					} else if ("deflate".equals(els[j].getName())) {
						Engine.logBeans.debug("(ReverseProxyServlet) Decode Deflate stream");
						siteIn = new InflaterInputStream(siteIn, new Inflater(true));
						bDeflate = true;
					}
				}
			}

			byte[] bytesDataResult;

			ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);

			// String resourceUrl = projectName + targetPath;

			String t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);

			try {
				// Read either from the cache, either from the remote server
				// InputStream is = proxyCacheManager.getResource(resourceUrl);
				// if (is != null) {
				// Engine.logEngine.debug("(ReverseProxyServlet) Getting data from cache");
				// siteIn = is;
				// }
				int c = siteIn.read();
				while (c > -1) {
					baos.write(c);
					c = siteIn.read();
				}
				// if (is != null) is.close();
			} finally {
				context.statistics.stop(t, true);
			}

			bytesDataResult = baos.toByteArray();
			baos.close();
			Engine.logEngine.debug("(ReverseProxyServlet) Data retrieved!");

			// if (isDynamicContent(httpServletRequest.getPathInfo(),
			// proxyHttpConnector.getDynamicContentFiles())) {
			Engine.logEngine.debug("(ReverseProxyServlet) Dynamic content");

			bytesDataResult = handleStringReplacements(baseUrl, contentType, pageCharset, proxyHttpConnector,
					bytesDataResult);

			String billingClassName = context.getConnector().getBillingClassName();
			if (billingClassName != null) {
				try {
					Engine.logContext.debug("Billing class name required: " + billingClassName);
					AbstractBiller biller = (AbstractBiller) Class.forName(billingClassName).newInstance();
					Engine.logContext.debug("Executing the biller");
					biller.insertBilling(context);
				} catch (Throwable e) {
					Engine.logContext.warn("Unable to execute the biller (the billing is thus ignored): ["
							+ e.getClass().getName() + "] " + e.getMessage());
				}
			}
			// }
			// else {
			// Engine.logEngine.debug("(ReverseProxyServlet) Static content: " +
			// contentType);
			//				
			// // Determine if the resource has already been cached or not
			// CacheEntry cacheEntry =
			// proxyCacheManager.getCacheEntry(resourceUrl);
			// if (cacheEntry instanceof FileCacheEntry) {
			// FileCacheEntry fileCacheEntry = (FileCacheEntry) cacheEntry;
			// File file = new File(fileCacheEntry.fileName);
			// if (!file.exists())
			// proxyCacheManager.removeCacheEntry(cacheEntry);
			// cacheEntry = null;
			// }
			// if (cacheEntry == null) {
			// bytesDataResult = handleStringReplacements(contentType,
			// proxyHttpConnector, bytesDataResult);
			//
			// if (intProxyResponseCode == 200) {
			// Engine.logEngine.debug("(ReverseProxyServlet) Resource stored: "
			// + resourceUrl);
			// cacheEntry = proxyCacheManager.storeResponse(resourceUrl,
			// bytesDataResult);
			// cacheEntry.contentLength = bytesDataResult.length;
			// cacheEntry.contentType = contentType;
			// Engine.logEngine.debug("(ReverseProxyServlet) Cache entry: " +
			// cacheEntry);
			// }
			// }
			// }

			// Send the content to the client
			if (Engine.logEngine.isDebugEnabled() && contentType != null
					&& contentType.toLowerCase().contains("text/html")) {
				Engine.logEngine.debug("Data proxied:\n" + new String(bytesDataResult, pageCharset));
			}

			if (bGZip || bDeflate) {
				baos = new ByteArrayOutputStream();
				OutputStream compressedOutputStream = bGZip ? new GZIPOutputStream(baos)
						: new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION
								| Deflater.DEFAULT_STRATEGY, true));
				compressedOutputStream.write(bytesDataResult);
				compressedOutputStream.close();
				bytesDataResult = baos.toByteArray();
				baos.close();
			}

			httpServletResponse.setContentLength(bytesDataResult.length);
			OutputStream outputStreamClientResponse = httpServletResponse.getOutputStream();
			outputStreamClientResponse.write(bytesDataResult);

			Engine.logEngine.debug("(ReverseProxyServlet) End of document retransmission");
		} catch (Exception e) {
			Engine.logEngine.error("Error while trying to proxy page", e);
			throw new ServletException("Error while trying to proxy page", e);
		}
	}

	private byte[] handleStringReplacements(String baseUrl, String contentType, String charset,
			ProxyHttpConnector proxyHttpConnector, byte[] data) throws IOException {

		Engine.logEngine.debug("(ReverseProxyServlet) String replacements for content-type: " + contentType);

		if (contentType == null) {
			Engine.logEngine
					.warn("(ReverseProxyServlet) Aborting string replacements because of null mimetype!");
		} else {
			Replacements replacements = proxyHttpConnector.getReplacementsForMimeType(contentType);

			if (!replacements.isEmpty()) {
				String sData = new String(data, charset);

				Engine.logEngine.trace("(ReverseProxyServlet) Data before string replacements:\n" + sData);

				Engine.logEngine.debug("(ReverseProxyServlet) Replacements in progress");

				String strSearched, strReplacing;
				for (int i = 0; i < replacements.strReplacing.length; i++) {
					strSearched = replacements.strSearched[i];
					Engine.logEngine.debug("(ReverseProxyServlet) Replacing: " + strSearched);

					strReplacing = replacements.strReplacing[i];

					Matcher m_connector = reg_connector.matcher(strReplacing);
					if (m_connector.find() && m_connector.groupCount() >= 1) {
						String newConnector = m_connector.group(1);
						Engine.logEngine.trace("(ReverseProxyServlet) find connector : " + newConnector);

						// Bugfix for #1798 regression about #1718
						String newBaseUrl = switchConnector(baseUrl, newConnector) + '/';
						Engine.logEngine.trace("(ReverseProxyServlet) new baseUrl : " + newBaseUrl);
						strReplacing = m_connector.replaceAll(newBaseUrl);
					} else {
						strReplacing = reg_base.matcher(replacements.strReplacing[i]).replaceAll(baseUrl);
					}

					Engine.logEngine.debug("(ReverseProxyServlet) By: " + strReplacing);

					sData = sData.replaceAll(strSearched, strReplacing);
				}

				Engine.logEngine.debug("(ReverseProxyServlet) Replacements done!");

				Engine.logEngine.trace("(ReverseProxyServlet) Data after string replacements:\n" + sData);

				data = sData.getBytes(charset);
			}
		}
		return data;
	}

	/**
	 * Retrieves all of the headers from the servlet request and sets them on
	 * the proxy request
	 * 
	 * @param httpServletRequest
	 *            The request object representing the client's request to the
	 *            servlet engine
	 * @param httpMethodProxyRequest
	 *            The request that we are about to send to the proxy host
	 */
	private void setProxyRequestHeaders(HttpServletRequest httpServletRequest,
			HttpMethod httpMethodProxyRequest, ProxyHttpConnector proxyHttpConnector) {
		Collection<String> removableHeaders = proxyHttpConnector.getRemovableHeadersSet();
		// Get an Enumeration of all of the header names sent by the client
		Enumeration<String> enumerationOfHeaderNames = GenericUtils.cast(httpServletRequest.getHeaderNames());
		while (enumerationOfHeaderNames.hasMoreElements()) {
			String stringHeaderName = (String) enumerationOfHeaderNames.nextElement();
			if (stringHeaderName.equalsIgnoreCase(STRING_CONTENT_LENGTH_HEADER_NAME)
					|| stringHeaderName.equalsIgnoreCase("Cookie")
					|| removableHeaders.contains(stringHeaderName.toLowerCase())) {
				continue;
			}
			// As per the Java Servlet API 2.5 documentation:
			// Some headers, such as Accept-Language can be sent by clients
			// as several headers each with a different value rather than
			// sending the header as a comma separated list.
			// Thus, we get an Enumeration of the header values sent by the
			// client
			Enumeration<String> enumerationOfHeaderValues = GenericUtils.cast(httpServletRequest
					.getHeaders(stringHeaderName));
			while (enumerationOfHeaderValues.hasMoreElements()) {
				String stringHeaderValue = (String) enumerationOfHeaderValues.nextElement();
				// In case the proxy host is running multiple virtual servers,
				// rewrite the Host header to ensure that we get content from
				// the correct virtual server
				if (stringHeaderName.equalsIgnoreCase(STRING_HOST_HEADER_NAME)) {
					stringHeaderValue = getProxyHostAndPort(proxyHttpConnector);
				} else if (stringHeaderName.equalsIgnoreCase("Referer")) {
					stringHeaderValue = stringHeaderValue.replaceFirst("://[^/]*/[^/]*/", "://"
							+ getProxyHostAndPort(proxyHttpConnector) + proxyHttpConnector.getBaseDir()
							+ (proxyHttpConnector.getBaseDir().endsWith("/") ? "" : "/"));
				}
				Engine.logEngine.debug("(ReverseProxyServlet) Forwarding header: " + stringHeaderName + "="
						+ stringHeaderValue);
				Header header = new Header(stringHeaderName, stringHeaderValue);
				// Set the same header on the proxy request
				httpMethodProxyRequest.setRequestHeader(header);
			}
		}
	}

	private String getProxyHostAndPort(ProxyHttpConnector proxyHttpConnector) {
		int targetPort = proxyHttpConnector.getPort();
		String targetServer = proxyHttpConnector.getServer();
		if ((targetPort == 80) || (targetPort == 443)) {
			return targetServer;
		} else {
			return targetServer + ":" + targetPort;
		}
	}

	// private boolean isDynamicContent(String path, String dynamicContentFiles)
	// {
	// StringTokenizer stringTokenizer = new
	// StringTokenizer(dynamicContentFiles, " ");
	// String filePattern;
	// while (stringTokenizer.hasMoreTokens()) {
	// filePattern = stringTokenizer.nextToken();
	// if (path.endsWith(filePattern)) return true;
	// }
	//
	// return false;
	// }

	private HttpState getHttpState(HttpConnector connector, Context context) {
		if (context.httpState == null) {
			Engine.logEngine.debug("(ReverseProxyServlet) Creating new HttpState for context id "
					+ context.contextID);
			context.httpState = new HttpState();
		} else {
			Engine.logEngine.debug("(ReverseProxyServlet) Using HttpState of context id " + context.contextID);
		}
		return context.httpState;
	}

	private String handleRedirect(String stringLocation, String baseUrl,
			ProxyHttpConnector currentProxyHttpConnector) {
		Engine.logEngine.debug("ReverseProxyServlet:handleRedirect() requested redirect location: "
				+ stringLocation);
		Engine.logEngine.debug("ReverseProxyServlet:handleRedirect() reverse proxy base url: " + baseUrl);

		for (Connector connector : currentProxyHttpConnector.getProject().getConnectorsList()) {
			if (connector instanceof ProxyHttpConnector) {
				String redirectLocation = stringLocation;

				Engine.logEngine.debug("ReverseProxyServlet:handleRedirect() analyzing connector "
						+ connector.getName());

				ProxyHttpConnector proxyHttpConnector = (ProxyHttpConnector) connector;
				String baseDir = proxyHttpConnector.getBaseDir();

				// remove the trailing slash for host only connector, see
				// comment of #1798
				if ("/".equals(baseDir)) {
					baseDir = "";
				}
				Engine.logEngine.debug("ReverseProxyServlet:handleRedirect()   connector baseDir=" + baseDir);

				String proxyDomainAndPort = "http" + (proxyHttpConnector.isHttps() ? "s" : "") + "://"
						+ getProxyHostAndPort(proxyHttpConnector);
				String proxyBaseUrl = proxyDomainAndPort + baseDir;
				Engine.logEngine.debug("ReverseProxyServlet:handleRedirect()   proxyBaseUrl=" + proxyBaseUrl);

				// Bugfix for #1891
				if (redirectLocation.startsWith("/")) {
					redirectLocation = proxyDomainAndPort + redirectLocation;
					Engine.logEngine
							.debug("ReverseProxyServlet:handleRedirect()   asbolute path redirect => redirectLocation="
									+ redirectLocation);
				}

				// Bugfix for #1798 regression about #1718
				if (!redirectLocation.matches(proxyBaseUrl + "((/.*)|$)")) {
					// Bugfix #1718: it probably means the standard port number
					// (80 or 443) has been included
					proxyBaseUrl = "http" + (proxyHttpConnector.isHttps() ? "s" : "") + "://"
							+ proxyHttpConnector.getServer() + ":" + proxyHttpConnector.getPort() + baseDir;
					Engine.logEngine.debug("ReverseProxyServlet:handleRedirect()   update proxyBaseUrl="
							+ proxyBaseUrl);
				}

				// Bugfix for #1798 regression about #1718
				if (redirectLocation.matches(proxyBaseUrl + "((/.*)|$)")) {
					String newBaseUrl = switchConnector(baseUrl, connector.getName());
					Engine.logEngine.debug("ReverseProxyServlet:handleRedirect()   newBaseUrl=" + newBaseUrl);
					redirectLocation = redirectLocation.replace(proxyBaseUrl, newBaseUrl);
					Engine.logEngine.debug("ReverseProxyServlet:handleRedirect() final redirect location: "
							+ redirectLocation);
					return redirectLocation;
				}
			}
		}

		// Default case: no connector match
		return stringLocation;
	}

	private String switchConnector(String baseUrl, String connectorName) {
		String[] splittedBase = StringUtils.split(baseUrl, '/');
		for (int i = 0; i < splittedBase.length; i++) {
			if (splittedBase[i].equals("rproxy")) {
				splittedBase[i + 2] = connectorName; // /convertigo/rproxy(i)/project(i+1)/connector(i+2)/context/
				break;
			}
		}
		// Bugfix for #1798 regression about #1718
		return '/' + StringUtils.join(splittedBase, '/');
	}
}