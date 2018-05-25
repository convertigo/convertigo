/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

package com.twinsoft.convertigo.engine.servlets;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncListener;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.LogParameters;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager.FullSyncAuthentication;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.util.ContentTypeDecoder;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;
import com.twinsoft.convertigo.engine.util.StreamUtils;

public class FullSyncServlet extends HttpServlet {
	private static final long serialVersionUID = -5147185931965387561L;
	
	private static final Pattern replace2F = Pattern.compile("(/_design)%2[fF]");
	
	transient private final static ThreadLocal<CloseableHttpClient> httpClient = new ThreadLocal<CloseableHttpClient>() {
		@Override
		protected CloseableHttpClient initialValue() {
			return HttpUtils.makeHttpClient4(false);
		}
	};

	@Override
	protected void service(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer debug = new StringBuffer();
		String c8oSDK = null;	
		HttpMethodType method;
		try {
			HttpUtils.checkCV(request);
			
			String corsOrigin = HttpUtils.applyCorsHeaders(request, response);
			if (corsOrigin != null) {
				debug.append("Add CORS header for: " + corsOrigin + "\n");
			}
			
			method = HttpMethodType.valueOf(request.getMethod());
			
			if (method == HttpMethodType.OPTIONS) {
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				return;
			}
			
			if (method != HttpMethodType.HEAD) {
				HttpSessionListener.checkSession(request);
			}
		} catch (Throwable e) {
			throw new ServletException(e);
		}
		
		try {
			HttpRequestBase newRequest;
			
			switch (method) {
//			case DELETE: newRequest = new HttpDelete(); break; //disabled to prevent db delete
			case GET: newRequest = new HttpGet(); break;
			case HEAD: newRequest = new HttpHead(); break;
			case OPTIONS: newRequest = new HttpOptions(); break;
			case POST: newRequest = new HttpPost(); break;
			case PUT: newRequest = new HttpPut(); break;
			case TRACE: newRequest = new HttpTrace(); break;
			default: throw new ServletException("Invalid HTTP method");
			}
			
			
			RequestParser requestParser = new RequestParser(request);
			
			Engine.theApp.couchDbManager.checkRequest(requestParser.getPath(), requestParser.getSpecial(), requestParser.getDocId());
			
			HttpSession httpSession = request.getSession();
			
			LogParameters logParameters = GenericUtils.cast(httpSession.getAttribute(FullSyncServlet.class.getCanonicalName()));
			
			if (logParameters == null) {
				httpSession.setAttribute(FullSyncServlet.class.getCanonicalName(), logParameters = new LogParameters());
				logParameters.put(mdcKeys.ContextID.toString().toLowerCase(), httpSession.getId());
			}

			Log4jHelper.mdcSet(logParameters);
			
			logParameters.put(mdcKeys.ClientIP.toString().toLowerCase(), request.getRemoteAddr());

			if (EnginePropertiesManager.getProperty(PropertyName.NET_REVERSE_DNS).equalsIgnoreCase("true")) {
				Log4jHelper.mdcPut(mdcKeys.ClientHostName, request.getRemoteHost());
			}
			
			FullSyncAuthentication fsAuth = Engine.theApp.couchDbManager.getFullSyncAuthentication(request.getSession());
			if (fsAuth == null) {
				Log4jHelper.mdcPut(mdcKeys.User, "(anonymous)");
				debug.append("Anonymous user\n");
			} else {
				Log4jHelper.mdcPut(mdcKeys.User, "'" + fsAuth.getAuthenticatedUser() + "'");
				debug.append("Authenticated user: ").append(fsAuth.getAuthenticatedUser()).append('\n')
					.append("Authenticated groups: ").append(fsAuth.getGroups()).append('\n');
			}
			
			URI uri;
			try {
				// needed for PouchDB replication
				uri = URI.create(
						Engine.theApp.couchDbManager.getFullSyncUrl() + requestParser.getPath() + 
						(request.getQueryString() == null ? "" : "?" + request.getQueryString())
						);
			} catch (Exception e) {
				URIBuilder builder = new URIBuilder(Engine.theApp.couchDbManager.getFullSyncUrl() + requestParser.getPath());
				if (request.getQueryString() != null) {
					builder.setCustomQuery(request.getQueryString());
				}
				uri = builder.build();
			}
			
			newRequest.setURI(uri);
			debug.append(method.name() + " URI: " + uri.toString() + "\n");
			
			String dbName = requestParser.getDbName();
			
			String requestStringEntity = null;
			HttpEntity httpEntity = null;
			JSONObject bulkDocsRequest = null;
			
			String special = requestParser.getSpecial();
			
			boolean isChanges = "_changes".equals(special);
			boolean isCBL = false;
			{
				String agent = HeaderName.UserAgent.getHeader(request);
				isCBL = agent != null && agent.startsWith("CouchbaseLite/1.");
				if (isCBL) {
					String version = Engine.theApp.couchDbManager.getFullSyncClient().getServerVersion();
					isCBL = version != null && version.compareTo("1.7") >= 0;
				}
			}
			for (String headerName: Collections.list(request.getHeaderNames())) {
				if (!(HeaderName.TransferEncoding.is(headerName)
						|| HeaderName.ContentLength.is(headerName)
						|| HeaderName.UserAgent.is(headerName)
						|| HeaderName.Expect.is(headerName)
						|| HeaderName.Connection.is(headerName)
						|| HeaderName.Host.is(headerName)
						|| HeaderName.Cookie.is(headerName)
						|| HeaderName.ContentEncoding.is(headerName)
						|| HeaderName.Origin.is(headerName)
						|| (isChanges && (HeaderName.IfNoneMatch.is(headerName)
								|| HeaderName.IfModifiedSince.is(headerName)
								|| HeaderName.CacheControl.is(headerName)
								|| HeaderName.AcceptEncoding.is(headerName)
								)))) {
					for (String headerValue: Collections.list(request.getHeaders(headerName))) {
						debug.append("request Header: " + headerName + "=" + headerValue + "\n");
						newRequest.addHeader(headerName, headerValue);
					}
				} else {
					debug.append("skip request Header: " + headerName + "=" + request.getHeader(headerName)+ "\n");
				}
			}
			
			{
				Header authBasicHeader = Engine.theApp.couchDbManager.getFullSyncClient().getAuthBasicHeader();
				if (authBasicHeader != null) {
					debug.append("request add BasicHeader");
					newRequest.addHeader(authBasicHeader);
				}
			}
			
			debug.append("dbName=" + dbName + " special=" + special + "\n");
			
			if (request.getInputStream() != null) {
				String reqContentType = request.getContentType(); 
				if (reqContentType != null && reqContentType.startsWith("multipart/related;")) {
					final MimeMultipart mp = new MimeMultipart(new ByteArrayDataSource(request.getInputStream(), reqContentType));

					int count = mp.getCount();
					final int[] size = {request.getIntHeader(HeaderName.ContentLength.value())};
					debug.append("handle multipart/related: " + reqContentType + "; " + count + " parts; original size of " + size[0]);

					bulkDocsRequest = new JSONObject();
					JSONArray bulkDocsArray = new JSONArray();
					CouchKey.docs.put(bulkDocsRequest, bulkDocsArray);
					
					for (int i = 0; i < count; i++) {
						BodyPart part = mp.getBodyPart(i);
						ContentTypeDecoder contentType = new ContentTypeDecoder(part.getContentType());
						
						if (contentType.mimeType() == MimeType.Json) {
							String charset = contentType.getCharset("UTF-8");
							
							List<javax.mail.Header> headers = Collections.list(GenericUtils.<Enumeration<javax.mail.Header>>cast(part.getAllHeaders()));
							
							byte[] buf = IOUtils.toByteArray(part.getInputStream());
							size[0] -= buf.length;
							
							String json = new String(buf, charset);
							try {
								JSONObject docRequest = new JSONObject(json);
								Engine.theApp.couchDbManager.handleDocRequest(dbName, docRequest, fsAuth);
								bulkDocsArray.put(docRequest);
								json = docRequest.toString();
							} catch (JSONException e) {
								debug.append("failed to parse [ " + e.getMessage() + "]: " + json);
							}
							
							part.setContent(buf = json.getBytes(charset), part.getContentType());
							size[0] += buf.length;
							
							for (javax.mail.Header header: headers) {
								part.setHeader(header.getName(), header.getValue());
							}
						}
					}
					debug.append("; new size of " + size[0] + "\n");
					
					httpEntity = new AbstractHttpEntity() {
						
						@Override
						public void writeTo(OutputStream arg0) throws IOException {
							try {
								mp.writeTo(arg0);
							} catch (MessagingException e) {
								new IOException(e);
							}
						}
						
						@Override
						public boolean isStreaming() {
							return false;
						}
						
						@Override
						public boolean isRepeatable() {
							return true;
						}
						
						@Override
						public long getContentLength() {
							return size[0];
						}
						
						@Override
						public InputStream getContent() throws IOException, IllegalStateException {
							return null;
						}
					};
				} else {
					InputStream is = null;
					try {
						if ("gzip".equals(HeaderName.ContentEncoding.getHeader(request))) {
							is = new GZIPInputStream(request.getInputStream());
						} else {
							is = request.getInputStream();
						}
						requestStringEntity = IOUtils.toString(is, "UTF-8");
						debug.append("request Entity:\n" + requestStringEntity + "\n");
					} finally {
						if (is != null) {
							is.close();
						}
					}
				}
			}
			
			if (method == HttpMethodType.POST && "_bulk_docs".equals(special)) {
				try {
					bulkDocsRequest = new JSONObject(requestStringEntity);
					Engine.theApp.couchDbManager.handleBulkDocsRequest(dbName, bulkDocsRequest, fsAuth);
					requestStringEntity = bulkDocsRequest.toString();
				} catch (JSONException e) {
					debug.append("failed to parse [ " + e.getMessage() + "]: " + requestStringEntity);						
				}
			} else if (isChanges) {
				uri = Engine.theApp.couchDbManager.handleChangesUri(dbName, uri, requestStringEntity, fsAuth, c8oSDK);
				newRequest.setURI(uri);
				debug.append("Changed to " + newRequest.getMethod() + " URI: " + uri + "\n");
			}
			
			if (newRequest instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest entityRequest = ((HttpEntityEnclosingRequest) newRequest);
				
				if (entityRequest.getEntity() == null) {
					if (httpEntity != null) {
						// already exists
					} else if (requestStringEntity != null) {
						debug.append("request new Entity:\n" + requestStringEntity + "\n");
						httpEntity = new StringEntity(requestStringEntity, "UTF-8");
					} else {
						httpEntity = new InputStreamEntity(request.getInputStream());
					}
					
					entityRequest.setEntity(httpEntity);
				}
			}
			
			Map<AbstractFullSyncListener, JSONArray> listeners = Engine.theApp.couchDbManager.handleBulkDocsRequest(dbName, bulkDocsRequest);
			
			long requestTime = System.currentTimeMillis();
			
			CloseableHttpResponse newResponse = null;
			try {
				newResponse = httpClient.get().execute(newRequest);
			} catch (IOException e) {
				debug.append("retry request because: " + e.getMessage());
				newResponse = httpClient.get().execute(newRequest);
			}
			
			requestTime = System.currentTimeMillis() - requestTime;
			
			int code = newResponse.getStatusLine().getStatusCode();
			debug.append("response Code: " + code + " in " + requestTime + " ms\n");
			
			response.setStatus(code);
			
			boolean isCblBulkGet = isCBL && "_bulk_get".equals(special);
			
			if (!isCblBulkGet) {
				for (Header header: newResponse.getAllHeaders()) {
					if (isCBL && HeaderName.Server.is(header)) {
						response.addHeader("Server", "Couchbase Sync Gateway/0.81");
					} else if (!(HeaderName.TransferEncoding.is(header)
							|| HeaderName.ContentLength.is(header)
							|| HeaderName.AccessControlAllowOrigin.is(header)
							|| (isChanges && (HeaderName.ETag.is(header)
									|| HeaderName.LastModified.is(header)
									|| HeaderName.CacheControl.is(header)
							)))) {
						response.addHeader(header.getName(), header.getValue());
						debug.append("response Header: " + header.getName() + "=" + header.getValue() + "\n");
					} else {
						debug.append("skip response Header: " + header.getName() + "=" + header.getValue() + "\n");
					}
				}
			}
			
			HttpEntity responseEntity = newResponse.getEntity();
			ContentTypeDecoder contentType = new ContentTypeDecoder(responseEntity == null || responseEntity.getContentType() == null  ? "" : responseEntity.getContentType().getValue());
			debug.append("response ContentType charset=" + contentType.getCharset("n/a") + " mime=" + contentType.getMimeType() + "\n");
			
			OutputStream os = response.getOutputStream();
			
			String responseStringEntity = null;
			if (responseEntity != null) {
				InputStream is = null;
				try {
					is = responseEntity.getContent();
					
					if (contentType.mimeType().in(MimeType.Plain, MimeType.Json)) {
						String charset = contentType.getCharset("UTF-8");
						
						if (isChanges && code == 200) {
							Engine.logCouchDbManager.info("(FullSyncServlet) Entering in continuous loop:\n" + debug);
							BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(is), charset));
							OutputStreamWriter writer = new OutputStreamWriter(os, charset);
							Engine.theApp.couchDbManager.filterChanges(httpSession.getId(), dbName, uri, fsAuth, br, writer);
						} else {
							int id = 0;
							byte[] buf = new byte[64];
							StringBuilder sb = new StringBuilder();
							CharsetDecoder cd = Charset.forName(charset).newDecoder();
							cd.onMalformedInput(CodingErrorAction.REPORT);
							debug.append("response Entity:\n");

							int read = is.read();
							while (read >= 0) {
								buf[id++] = (byte) read;
								try {
									CharBuffer cb = cd.decode(ByteBuffer.wrap(buf, 0, id));
									id = 0;
									sb.append(cb);
									//								debug.append(cb);
								} catch (Throwable t) {
									Engine.logCouchDbManager.trace("(FullSyncServlet) Buffer not decoded, retry with more byte. " + t);
								}
								read = is.read();
							}
							debug.append("\n");
							responseStringEntity = sb.toString();

							JSONObject document = Engine.theApp.couchDbManager.handleDocResponse(method, requestParser.getSpecial(), requestParser.getDocId(), fsAuth, responseStringEntity);
							if (!isCblBulkGet) {
								IOUtils.write(responseStringEntity, os, charset);
							} else {
								Engine.theApp.couchDbManager.handleCblBulkGet(response, document);
							}
						}
					} else {
						String contentLength = HeaderName.ContentLength.getHeader(newResponse);
						if (contentLength != null) {
							HeaderName.ContentLength.addHeader(response, contentLength);
							debug.append("response Header: " + HeaderName.ContentLength.value() + "=" + contentLength + "\n");
						}
						StreamUtils.copyAutoFlush(is, os);
					}
				} finally {
					newResponse.close();
				}
			}

			Engine.logCouchDbManager.info("(FullSyncServlet) Success to process request:\n" + debug);
			
			if (bulkDocsRequest != null && responseStringEntity != null && listeners != null) {
				Engine.theApp.couchDbManager.handleBulkDocsResponse(request, listeners, bulkDocsRequest, responseStringEntity);
			}
		} catch (SecurityException e) {
			Engine.logCouchDbManager.error("(FullSyncServlet) Failed to process request due to a security exception:\n" + e.getMessage() + "\n" + debug);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} catch (Exception e) {
			if ("ClientAbortException".equals(e.getClass().getSimpleName())) {
				Engine.logCouchDbManager.info("(FullSyncServlet) Client disconnected:\n" + debug);
			} else {
				Engine.logCouchDbManager.error("(FullSyncServlet) Failed to process request:\n" + debug, e);
			}
		} finally {
			Log4jHelper.mdcClear();
		}
	}
	
	static class RequestParser {
		static final private Pattern pPath = Pattern.compile("^((?:/(_[^/]*))?(?:/([^/]*))?(?:/(_[^/]*))?(?:/([^/]*))?(.*))$");
		
		private String path;
		private String special;
		private String dbName;
		private String docId;
		
		RequestParser(HttpServletRequest request) throws UnsupportedEncodingException {
			String requestURI = request.getRequestURI();
			String contextPath = request.getContextPath();
			requestURI = requestURI.substring(contextPath.length());
			
			String servletPath = request.getServletPath();			
			String request_path = requestURI.substring(requestURI.indexOf(servletPath) + servletPath.length());
			
			request_path = replace2F.matcher(request_path).replaceFirst("$1/");
			
			Matcher mPath = pPath.matcher(request_path);
			if (mPath.matches()) {
				path = mPath.group(1);
				special = mPath.group(2);
				if (special == null) {
					dbName = mPath.group(3);
					special = mPath.group(4);
					docId = mPath.group(5);
				}
			}
		}

		public String getPath() {
			return path;
		}

		public String getSpecial() {
			return special;
		}

		public String getDbName() {
			return dbName;
		}

		public String getDocId() {
			return docId;
		}
	}
}
