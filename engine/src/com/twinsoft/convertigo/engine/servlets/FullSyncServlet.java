/*
 * Copyright (c) 2001-2023 Convertigo SA.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncListener;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.LogParameters;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.FullSyncAnonymousReplication;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager.FullSyncAuthentication;
import com.twinsoft.convertigo.engine.providers.couchdb.FullSyncClient;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.util.ContentTypeDecoder;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;
import com.twinsoft.convertigo.engine.util.ServletUtils;
import com.twinsoft.convertigo.engine.util.StreamUtils;

public class FullSyncServlet extends HttpServlet {
	private static final long serialVersionUID = -5147185931965387561L;
	
	private static final Pattern replace2F = Pattern.compile("(/_design)%2[fF]");
	
	transient private final static ThreadLocal<HttpClientInterface> httpClient = new ThreadLocal<HttpClientInterface>() {
		@Override
		protected HttpClientInterface initialValue() {
			return HttpUtils.makeHttpClient(false);
		}
	};

	@Override
	protected void service(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer debug = new StringBuffer();
		HttpMethodType method;
		try {
			HttpUtils.checkCV(request);
			
			String corsOrigin = HttpUtils.applyCorsHeaders(request, response);
			if (corsOrigin != null) {
				debug.append("Added CORS header for: " + corsOrigin + "\n");
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
		
		HttpSession httpSession = request.getSession();
		
		try {
			FullSyncClient fsClient = Engine.theApp.couchDbManager.getFullSyncClient();
			RequestParser requestParser = new RequestParser(request, fsClient.getPrefix());
			
			boolean isUtilsSession = "true".equals(httpSession.getAttribute("__isUtilsSession"));
			boolean isUtilsRequest = false;
			String referer = request.getHeader("Referer");
			if (isUtilsSession || (referer != null &&  referer.endsWith("/admin/_utils/"))) {
				Engine.authenticatedSessionManager.checkRoles(httpSession, Role.WEB_ADMIN, Role.FULLSYNC_CONFIG, Role.FULLSYNC_VIEW);
				httpSession.setAttribute("__isUtilsSession", "true");
				isUtilsRequest = !"_all_dbs".equals(requestParser.getSpecial());
			} else {
				Engine.theApp.couchDbManager.checkRequest(requestParser.getPath(), requestParser.getSpecial(), requestParser.getDocId());
			}
			
			synchronized (httpSession) {
				Set<HttpServletRequest> set = SessionAttribute.fullSyncRequests.get(httpSession);
				if (set == null) {
					SessionAttribute.fullSyncRequests.set(httpSession, set = new HashSet<HttpServletRequest>());
				}
				set.add(request);
			}
			
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
			
			String dbName = requestParser.getDbName();
			
			FullSyncAuthentication fsAuth = Engine.theApp.couchDbManager.getFullSyncAuthentication(request.getSession());
			if (fsAuth == null) {
				Log4jHelper.mdcPut(mdcKeys.User, "(anonymous)");
				debug.append("Anonymous user\n");
				Boolean allowAnonymous = null;
				for (String projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
					try {
		    			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		    			for (Connector connector : project.getConnectorsList()) {
		    				if (connector instanceof FullSyncConnector) {
		    					FullSyncConnector fullSyncConnector = (FullSyncConnector) connector;
		    					if (fullSyncConnector.getDatabaseName().equals(dbName)) {
		    						allowAnonymous = fullSyncConnector.getAnonymousReplication() == FullSyncAnonymousReplication.allow;
		    						break;
		    					}
		    				}
		    			}
		    			if (allowAnonymous != null) {
		    				break;
		    			}
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				if (allowAnonymous == Boolean.FALSE) {
					throw new SecurityException("The '" + dbName + "' database deny pull synchronization for an anonymous session");
				}
			} else {
				Log4jHelper.mdcPut(mdcKeys.User, "'" + fsAuth.getAuthenticatedUser() + "'");
				debug.append("Authenticated user: ").append(fsAuth.getAuthenticatedUser()).append('\n')
					.append("Authenticated groups: ").append(fsAuth.getGroups()).append('\n');
			}
			
			String url = Engine.theApp.couchDbManager.getFullSyncUrl() + requestParser.getPath();
			URIBuilder builder = new URIBuilder(url);
			String query = request.getQueryString();
			if (query != null) {
				try {
					// needed for PouchDB replication
					URI uri = URI.create(url + "?" + request.getQueryString());
					query = uri.getQuery();
				} catch (Exception e) {
					debug.append("parse query failed (" + e.getMessage() +"), use as it; query=" + query + "\n");
				}
				builder.setCustomQuery(query);
			}
			
			String special = requestParser.getSpecial();
			
			boolean isChanges = "_changes".equals(special);
			
			String version = fsClient.getServerVersion();
			boolean isCouchDB = fsClient.isCouchDB();
			
			if (isChanges && fsClient.hasMango()) {
				method = HttpMethodType.POST;
			}
			
			debug.append("dbName=" + dbName + " special=" + special + " couchdb=" + version + (requestParser.hasAttachment() ? " attachment=true" : "") + "\n");
			
			HttpRequestBase newRequest;
			
			switch (method) {
			case DELETE:
				if (isUtilsRequest) {
					Engine.authenticatedSessionManager.checkRoles(httpSession, Role.WEB_ADMIN, Role.FULLSYNC_CONFIG);
					if (requestParser.getDocId() == null && StringUtils.isNotBlank(requestParser.getDbName()) && DelegateServlet.canDelegate()) {
						JSONObject instruction = new JSONObject();
						JSONObject variables = new JSONObject();
						try {
							instruction.put("action", "deleteDatabase");
							variables.put("db", fsClient.getPrefix() + requestParser.getDbName());
							instruction.put("variables", variables);
							JSONObject deleteResponse = DelegateServlet.delegate(instruction);
							if (deleteResponse != null) {
								JSONObject meta = CouchKey._c8oMeta.JSONObject(deleteResponse);
								CouchKey._c8oMeta.remove(deleteResponse);
								response.setStatus(meta.getInt("statusCode"));
								JSONObject headers = meta.getJSONObject("headers");
								for (java.util.Iterator<?> i = headers.keys(); i.hasNext();) {
									String key = (String) i.next();
									response.addHeader(key, headers.getString(key));
								}
								response.setCharacterEncoding("UTF-8");
								try (Writer w = response.getWriter()) {
									w.write(deleteResponse.toString());
								}
								return;
							}
						} catch (Exception e) {
						}
					}
					newRequest = new HttpDelete();
				} else {
					// disabled to prevent db delete
					throw new ServletException("Invalid HTTP method");
				}
				break;
			case GET: newRequest = new HttpGet(); break;
			case HEAD: newRequest = new HttpHead(); break;
			case OPTIONS: newRequest = new HttpOptions(); break;
			case POST:
				if (isUtilsRequest) {
					Engine.authenticatedSessionManager.checkRoles(httpSession, Role.WEB_ADMIN, Role.FULLSYNC_CONFIG);
				}
				newRequest = new HttpPost();
				break;
			case PUT:
				if (isUtilsRequest) {
					Engine.authenticatedSessionManager.checkRoles(httpSession, Role.WEB_ADMIN, Role.FULLSYNC_CONFIG);
				}
				newRequest = new HttpPut(); break;
			case TRACE: newRequest = new HttpTrace(); break;
			default: throw new ServletException("Invalid HTTP method");
			}
			
			if (method.equals(HttpMethodType.POST) && "_bulk_docs".equals(special)) {
				int n = fsClient.getN();
				if (n > 1) {
					for (NameValuePair kv: builder.getQueryParams()) {
						if ("w".equals(kv.getName())) {
							n = 0;
							break;
						}
					}
					if (n > 1) {
						builder.addParameter("w", Integer.toString(n));
					}
				}
			}
			URI uri = builder.build();
			newRequest.setURI(uri);
			debug.append(method.name() + " URI: " + uri.toString() + "\n");
			
			String requestStringEntity = null;
			HttpEntity httpEntity = null;
			JSONObject bulkDocsRequest = null;
			boolean isCBL = false;
			boolean isCBLiOS = false;
			{
				String agent = HeaderName.UserAgent.getHeader(request);
				isCBL = agent != null && agent.startsWith("CouchbaseLite/1.");
				if (isCBL) {
					isCBLiOS = agent.contains("iOS");
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
				Header authBasicHeader = fsClient.getAuthBasicHeader();
				if (authBasicHeader != null) {
					debug.append("request add BasicHeader");
					newRequest.addHeader(authBasicHeader);
				}
			}
			
			if (request.getInputStream() != null) {
				String reqContentType = request.getContentType(); 
				if (reqContentType != null && reqContentType.startsWith("multipart/related;")) {
					final MimeMultipart mp = new MimeMultipart(new ByteArrayDataSource(request.getInputStream(), reqContentType));
					final long[] size = {request.getIntHeader(HeaderName.ContentLength.value())};
					final boolean chunked = size[0] == -1;

					int count = mp.getCount();
					debug.append("handle multipart/related: " + reqContentType + "; " + count + " parts; original size of " + size[0]);

					final File mpTmp;
					if (chunked) {
						mpTmp = File.createTempFile("c8o", "mpTmp");
						mpTmp.deleteOnExit();
					} else {
						mpTmp = null;
					}
					try {

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
								if (!chunked) {
							size[0] -= buf.length;
								}
							
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
								if (!chunked) {
							size[0] += buf.length;
								}
							
							for (javax.mail.Header header: headers) {
									String name = header.getName();
									if (HeaderName.ContentLength.is(name)) {
										part.setHeader(name, Integer.toString(buf.length));
									} else {
										part.setHeader(name, header.getValue());
									}
								}
							}
						}

						if (chunked) {
							try (FileOutputStream fos = new FileOutputStream(mpTmp)) {
								mp.writeTo(fos);
					}
							size[0] = mpTmp.length();
						}

					debug.append("; new size of " + size[0] + "\n");
					
					httpEntity = new AbstractHttpEntity() {
						
						@Override
							public void writeTo(OutputStream output) throws IOException {
								if (chunked) {
									try (FileInputStream fis = new FileInputStream(mpTmp)) {
										IOUtils.copyLarge(fis, output);
									}								
								} else {
							try {
										mp.writeTo(output);
							} catch (MessagingException e) {
								new IOException(e);
							}
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

					} finally {
						if (mpTmp != null) {
							mpTmp.delete();
						}
					}
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
			boolean isNewStringEntity = false;
			
			if (method == HttpMethodType.POST && "_bulk_docs".equals(special)) {
				try {
					bulkDocsRequest = new JSONObject(requestStringEntity);
					Engine.theApp.couchDbManager.handleBulkDocsRequest(dbName, bulkDocsRequest, fsAuth);
					String newEntity = bulkDocsRequest.toString();
					if (!newEntity.equals(requestStringEntity)) {
						requestStringEntity = newEntity;
						isNewStringEntity = true;
					}
				} catch (JSONException e) {
					debug.append("failed to parse [ " + e.getMessage() + "]: " + requestStringEntity);
				}
			} else if (isChanges && newRequest instanceof HttpEntityEnclosingRequest) {
				requestStringEntity = Engine.theApp.couchDbManager.handleChangesUri(dbName, newRequest, requestStringEntity, fsAuth);
				if (requestStringEntity != null) {
					debug.append("request new Entity:\n" + requestStringEntity + "\n");
				}
				uri = newRequest.getURI();
				debug.append("Changed to " + newRequest.getMethod() + " URI: " + uri + "\n");
			}
			
			if (!isChanges && newRequest instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest entityRequest = ((HttpEntityEnclosingRequest) newRequest);
				
				if (entityRequest.getEntity() == null) {
					if (httpEntity != null) {
						// already exists
					} else if (requestStringEntity != null) {
						if (isNewStringEntity) {
						debug.append("request new Entity:\n" + requestStringEntity + "\n");
						}
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
			if (isCBLiOS && code == 400) {
				code = 500;
				debug.append("response changed Code to: " + code + " (for iOS CBL)\n");
			}
			response.setStatus(code);
			
			boolean isCblBulkGet = isCBL && version.compareTo("2.3.") < 0 && isCouchDB && "_bulk_get".equals(special);
			
			if (!isCblBulkGet) {
				for (Header header: newResponse.getAllHeaders()) {
					if (isCBL && HeaderName.Server.is(header)) {
						response.addHeader("Server", "Couchbase Sync Gateway/0.81");
						debug.append("response Header: Server=Couchbase Sync Gateway/0.81\n");
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
				ServletUtils.applyCustomHeaders(request, response);
			}
			
			HttpEntity responseEntity = newResponse.getEntity();
			ContentTypeDecoder contentType = new ContentTypeDecoder(responseEntity == null || responseEntity.getContentType() == null  ? "" : responseEntity.getContentType().getValue());
			debug.append("response ContentType charset=" + contentType.getCharset("n/a") + " mime=" + contentType.getMimeType() + "\n");
			
			OutputStream os = response.getOutputStream();
			
			String responseStringEntity = null;
			if (responseEntity != null) {
				//InputStream is = null;
				try (InputStream is = responseEntity.getContent()) {
					//is = responseEntity.getContent();
					
					if (code >= 200 && code < 300 &&
							!isUtilsRequest &&
							contentType.mimeType().in(MimeType.Plain, MimeType.Json) &&
							!"_design".equals(special) &&
							!requestParser.hasAttachment() && (
								(isChanges && !fsClient.hasMango()) ||
								"_bulk_get".equals(special) ||
								"_all_docs".equals(special) ||
								"_all_dbs".equals(special) ||
								StringUtils.isNotEmpty(requestParser.getDocId()) ||
								(bulkDocsRequest != null && listeners != null))) {
						String charset = contentType.getCharset("UTF-8");
						
						try (OutputStreamWriter writer = new OutputStreamWriter(os, charset); BufferedInputStream bis = new BufferedInputStream(is)) {
							if (isChanges) {
								Engine.logCouchDbManager.info("(FullSyncServlet) Entering in continuous loop:\n" + debug);
								try (BufferedReader br = new BufferedReader(new InputStreamReader(bis, charset))) {
									Engine.theApp.couchDbManager.filterChanges(httpSession.getId(), dbName, uri, fsAuth, br, writer);
								}
							} else if (bulkDocsRequest != null) {
								Engine.logCouchDbManager.info("(FullSyncServlet) Handle bulkDocsRequest:\n" + debug);
								responseStringEntity = IOUtils.toString(bis, charset);
								writer.write(responseStringEntity);
								writer.flush();
								if (listeners != null) {
									Engine.theApp.couchDbManager.handleBulkDocsResponse(request, listeners, bulkDocsRequest, responseStringEntity);
								}
							} else if (isCblBulkGet) {
								Engine.logCouchDbManager.info("(FullSyncServlet) Checking text response documents for CBL BulkGet:\n" + debug);
								Engine.theApp.couchDbManager.checkCblBulkGetResponse(special, fsAuth, bis, charset, response);
							} else {
								Engine.logCouchDbManager.info("(FullSyncServlet) Checking response documents:\n" + debug);
								Engine.theApp.couchDbManager.checkResponse(special, fsAuth, bis, charset, writer);
							}
						}
					} else if (code >= 200 && code < 300 &&
							contentType.mimeType() == MimeType.MultiPartRelated && 
							"_bulk_get".equals(special)) {
						Engine.logCouchDbManager.info("(FullSyncServlet) Checking multipart response documents for CBL BulkGet:\n" + debug);
						Engine.theApp.couchDbManager.checkCblBulkGetResponse(fsAuth, is, response);
					} else if (Pattern.matches(".*/bundle\\..*?\\.js", uri.getPath())) {
						StringBuilder sb = new StringBuilder();
						sb.append(IOUtils.toString(is, "UTF-8")
								.replace("json=function(e){var t", "json=function(e){e=e.replace('../../','../');var t"));
						sb.append("\n$(\"#primary-navbar\").remove();");
						byte[] b = sb.toString().getBytes("UTF-8");
						HeaderName.ContentLength.addHeader(response, Integer.toString(b.length));
						os.write(b);
					} else if (Pattern.matches(".*/bundle-.*?\\.js", uri.getPath())) {
						StringBuilder sb = new StringBuilder();
						sb.append(IOUtils.toString(is, "UTF-8")
								.replace("ajax:function(e,t){", "ajax:function(e,t){console.log('> ' + e.url);if(e.url.startsWith('/')){e.url='..'+e.url}else{e.url=e.url.replace('../..','..')}console.log('< ' + e.url);"));
//						sb.append("\n$(\"#primary-navbar\").remove();");
						byte[] b = sb.toString().getBytes("UTF-8");
						HeaderName.ContentLength.addHeader(response, Integer.toString(b.length));
						os.write(b);
					} else if (Pattern.matches(".*/styles\\..*?\\.css", uri.getPath())) {
						StringBuilder sb = new StringBuilder();
						sb.append(IOUtils.toString(is, "UTF-8"));
						sb.append("\n.closeMenu #dashboard { left: 0px; }");
						sb.append("\n.closeMenu .pusher { padding-right: 0px; }");
						sb.append("\nbutton.add-new-database-btn { display: none; }");
						sb.append("\n#notification-center-btn { display: none; }");
						sb.append("\na.fonticon-replicate { display: none; }");
						sb.append("\n.faux-header__doc-header-dropdown-itemwrapper a.faux-header__doc-header-dropdown-item[href*=\"replication\"] { display: none; }\n");
						byte[] b = sb.toString().getBytes("UTF-8");
						HeaderName.ContentLength.addHeader(response, Integer.toString(b.length));
						os.write(b);
					} else if (requestParser.docId == null && requestParser.special == null && !fsClient.getPrefix().isEmpty()) {
						String content = IOUtils.toString(is, "UTF-8");
						content = content.replace("\"db_name\":\"" + fsClient.getPrefix(), "\"db_name\":\"");
						byte[] bytes = content.getBytes("UTF-8");
						HeaderName.ContentLength.addHeader(response, Integer.toString(bytes.length));
						debug.append("response Header: " + HeaderName.ContentLength.value() + "=" + bytes.length + "\n");
						Engine.logCouchDbManager.info("(FullSyncServlet) Remove prefix from response:\n" + debug);
						os.write(bytes);
					} else {
						String contentLength = HeaderName.ContentLength.getHeader(newResponse);
						if (contentLength != null) {
							HeaderName.ContentLength.addHeader(response, contentLength);
							debug.append("response Header: " + HeaderName.ContentLength.value() + "=" + contentLength + "\n");
						}
						Engine.logCouchDbManager.info("(FullSyncServlet) Copying response stream:\n" + debug);
						StreamUtils.copyAutoFlush(is, os);
					}
				} finally {
					newResponse.close();
				}
			}
		} catch (SecurityException e) {
			Engine.logCouchDbManager.warn("(FullSyncServlet) Failed to process request due to a security exception:\n" + e.getMessage() + "\n" + debug);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			HttpUtils.terminateNewSession(httpSession);
		} catch (EngineException e) {
			String message = e.getMessage();
			if (message != null && message.contains("anonymous user")) {
				Engine.logCouchDbManager.warn("(FullSyncServlet) Failed to process request: " + message + "\n" + debug);
			} else {
				Engine.logCouchDbManager.error("(FullSyncServlet) Failed to process request:\n" + debug, e);
			}
			HttpUtils.terminateNewSession(httpSession);
		} catch (Exception e) {
			if ("ClientAbortException".equals(e.getClass().getSimpleName())) {
				Engine.logCouchDbManager.info("(FullSyncServlet) Client disconnected:\n" + debug);
			} else {
				Engine.logCouchDbManager.error("(FullSyncServlet) Failed to process request:\n" + debug, e);
			}
			HttpUtils.terminateNewSession(httpSession);
		} finally {
			Log4jHelper.mdcClear();
			synchronized (httpSession) {
				Set<HttpServletRequest> set = SessionAttribute.fullSyncRequests.get(httpSession);
				if (set != null) {
					set.remove(request);
				}
			}
		}
	}
	
	static class RequestParser {
		static final private Pattern pPath = Pattern.compile("^((?:/(_[^/]*))?(?:/([^/]*))?(?:/(_[^/]*))?(?:/([^/]*))?(?:/*)(.*))$");
		
		private String path;
		private String special;
		private String dbName;
		private String docId;
		private boolean attachment = false;
		
		RequestParser(HttpServletRequest request, String prefix) throws UnsupportedEncodingException {
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
					attachment = docId != null && !mPath.group(6).isEmpty();
					if (!dbName.isEmpty()) {
						path = path.replaceFirst("/", "/" + prefix);
					}
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
		
		public boolean hasAttachment() {
			return attachment;
		}
	}
}
