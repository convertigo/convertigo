package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.twinsoft.convertigo.engine.admin.services.logs.Add;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.util.ContentTypeDecoder;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;

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

		try {
			HttpRequestBase newRequest;

			HttpMethodType method = HttpMethodType.valueOf(request.getMethod());

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
				httpSession.setAttribute(Add.class.getCanonicalName(), logParameters = new LogParameters());
				logParameters.put(mdcKeys.ContextID.toString().toLowerCase(), httpSession.getId());
			}

			Log4jHelper.mdcSet(logParameters);
			
			logParameters.put(mdcKeys.ClientIP.toString().toLowerCase(), request.getRemoteAddr());

			if (EnginePropertiesManager.getProperty(PropertyName.NET_REVERSE_DNS).equalsIgnoreCase("true")) {
				Log4jHelper.mdcPut(mdcKeys.ClientHostName, request.getRemoteHost());
			}

			String authenticatedUser = SessionAttribute.authenticatedUser.string(request.getSession());
			Log4jHelper.mdcPut(mdcKeys.User, authenticatedUser == null ? "(anonymous)" : "'" + authenticatedUser + "'");
			
			debug.append("Authenticated user: ").append(authenticatedUser).append('\n');
			URI uri;
			
			try {
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
			
			debug.append(method.name() + " URI: " + uri.toString() + "\n");

			for (String headerName: Collections.list(request.getHeaderNames())) {
				if (!HeaderName.TransferEncoding.is(headerName)
						&& !HeaderName.ContentLength.is(headerName)) {
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
					newRequest.addHeader(authBasicHeader);
				}
			}
			
			
			String dbName = requestParser.getDbName();
			
			String requestStringEntity = null;
			HttpEntity httpEntity = null;
			JSONObject bulkDocsRequest = null;
			
			String special = requestParser.getSpecial();
			
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
								Engine.theApp.couchDbManager.handleDocRequest(dbName, docRequest, authenticatedUser);
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
					requestStringEntity = IOUtils.toString(request.getInputStream(), "UTF-8");
					debug.append("request Entity:\n" + requestStringEntity + "\n");
				}
			}
			
			if (method == HttpMethodType.POST && "_bulk_docs".equals(special)) {
				try {
					bulkDocsRequest = new JSONObject(requestStringEntity);
					Engine.theApp.couchDbManager.handleBulkDocsRequest(dbName, bulkDocsRequest, authenticatedUser);
					requestStringEntity = bulkDocsRequest.toString();
				} catch (JSONException e) {
					debug.append("failed to parse [ " + e.getMessage() + "]: " + requestStringEntity);						
				}
			} else if ("_changes".equals(special)) {
				uri = Engine.theApp.couchDbManager.handleChangesUri(dbName, uri, authenticatedUser);
				debug.append("Changed to " + method.name() + " URI: " + uri.toString() + "\n");
			}
			
			if (newRequest instanceof HttpEntityEnclosingRequest) {
				if (httpEntity != null) {
					// already exists
				} else if (requestStringEntity != null) {
					debug.append("request new Entity:\n" + requestStringEntity + "\n");
					httpEntity = new StringEntity(requestStringEntity, "UTF-8");
				} else {
					httpEntity = new InputStreamEntity(request.getInputStream());
				}
				
				((HttpEntityEnclosingRequest) newRequest).setEntity(httpEntity);
			}
			
			Map<AbstractFullSyncListener, JSONArray> listeners = Engine.theApp.couchDbManager.handleBulkDocsRequest(dbName, bulkDocsRequest);
			
			newRequest.setURI(uri);

			CloseableHttpResponse newResponse = httpClient.get().execute(newRequest);
			int code = newResponse.getStatusLine().getStatusCode();
			debug.append("response Code: " + code + "\n");
			response.setStatus(code);
			
			for (Header header: newResponse.getAllHeaders()) {
				if (!HeaderName.TransferEncoding.is(header)
						&& !HeaderName.ContentLength.is(header)) {
					response.addHeader(header.getName(), header.getValue());
					debug.append("response Header: " + header.getName() + "=" + header.getValue() + "\n");
				} else {
					debug.append("skip response Header: " + header.getName() + "=" + header.getValue() + "\n");
				}
			}
			
			HttpEntity responseEntity = newResponse.getEntity();
			ContentTypeDecoder contentType = new ContentTypeDecoder(responseEntity == null || responseEntity.getContentType() == null  ? "" : responseEntity.getContentType().getValue());
			
			boolean continuous = code == 200;
			if (continuous) {
				String query = uri.getQuery();
				continuous = query != null && (query.contains("feed=continuous") || query.contains("feed=longpoll"));
			}
			
			OutputStream os = response.getOutputStream();
			
			String responseStringEntity = null;
			if (responseEntity != null) {
				InputStream is = null;
				try {
					is = responseEntity.getContent();
					
					if (!continuous && (contentType.mimeType().in(MimeType.Plain, MimeType.Json))) {
						String charset = contentType.getCharset("UTF-8");
						responseStringEntity = IOUtils.toString(responseEntity.getContent(), charset);
		
						debug.append("response Entity:\n" + responseStringEntity + "\n");
						
						Engine.theApp.couchDbManager.handleDocResponse(method, requestParser.getSpecial(), requestParser.getDocId(), authenticatedUser, responseStringEntity);
						
						IOUtils.write(responseStringEntity, os, charset);
					} else if (continuous) {
						Engine.logCouchDbManager.info("(FullSyncServlet) Entering in continuous loop:\n" + debug);
						
						int read = is.read();
						while (read >= 0) {
							os.write(read);
							os.flush();
							read = is.read();
						}
					} else {
						IOUtils.copy(is, os);
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
