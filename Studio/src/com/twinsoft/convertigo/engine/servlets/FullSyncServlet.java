package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.ContentTypeDecoder;

public class FullSyncServlet extends HttpServlet {
	private static final long serialVersionUID = -5147185931965387561L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer debug = new StringBuffer();

		try {
			HttpRequestBase newRequest;

			HttpMethodType method = HttpMethodType.valueOf(request.getMethod());

			switch (method) {
			case DELETE: newRequest = new HttpDelete(); break;
			case GET: newRequest = new HttpGet(); break;
			case HEAD: newRequest = new HttpHead(); break;
			case OPTIONS: newRequest = new HttpOptions(); break;
			case POST: newRequest = new HttpPost(); break;
			case PUT: newRequest = new HttpPut(); break;
			case TRACE: newRequest = new HttpTrace(); break;
			default: throw new ServletException("Invalid HTTP method");
			}

			RequestParser requestParser = new RequestParser(request.getPathInfo());
			String token = requestParser.getId();
			URI uri = URI.create(
					Engine.theApp.couchDbManager.getFullSyncUrl() + requestParser.getPath() + 
					(request.getQueryString() == null ? "" : "?" + request.getQueryString())
					);

			debug.append(method.name() + " URI: " + uri.toString() + "\n");

			for (String headerName: Collections.list(request.getHeaderNames())) {
				if (!HeaderName.TransferEncoding.value().equalsIgnoreCase(headerName)
						&& !HeaderName.ContentLength.value().equalsIgnoreCase(headerName)) {
					for (String headerValue: Collections.list(request.getHeaders(headerName))) {
						debug.append("request Header: " + headerName + "=" + headerValue + "\n");
						newRequest.addHeader(headerName, headerValue);
					}
				} else {
					debug.append("skip request Header: " + headerName + "=" + request.getHeader(headerName)+ "\n");
				}
			}

			String requestEntity = null;
			if (token != null) {
				String dbName = requestParser.getDbName();
				String special = requestParser.getSpecial();
				if (method == HttpMethodType.POST && "_bulk_docs".equals(special)) {
					requestEntity = IOUtils.toString(request.getInputStream(), "UTF-8");
					debug.append("request Entity:\n" + requestEntity + "\n");
					requestEntity = Engine.theApp.couchDbManager.handleBulkDocs(dbName, requestEntity, token);
				} else if (method == HttpMethodType.GET && "_changes".equals(special)) {
					uri = Engine.theApp.couchDbManager.handleChangesUri(dbName, uri, token);
					debug.append("Changed to " + method.name() + " URI: " + uri.toString() + "\n");
				}
			}

			if (requestEntity != null) {
				debug.append("request new Entity:\n" + requestEntity + "\n");
				((HttpEntityEnclosingRequest) newRequest).setEntity(new StringEntity(requestEntity, "UTF-8"));
			} else if (newRequest instanceof HttpEntityEnclosingRequest) {
				((HttpEntityEnclosingRequest) newRequest).setEntity(new InputStreamEntity(request.getInputStream()));
			}

			newRequest.setURI(uri);

			HttpResponse newResponse = Engine.theApp.httpClient4.execute(newRequest);
			
			for (Header header: newResponse.getAllHeaders()) {
				if (!HeaderName.TransferEncoding.value().equalsIgnoreCase(header.getName())
						&& !HeaderName.ContentLength.value().equalsIgnoreCase(header.getName())) {
					response.addHeader(header.getName(), header.getValue());
					debug.append("response Header: " + header.getName() + "=" + header.getValue() + "\n");
				} else {
					debug.append("skip response Header: " + header.getName() + "=" + header.getValue() + "\n");
				}
			}
			
			ContentTypeDecoder contentType = new ContentTypeDecoder(newResponse.getEntity() == null ? "" : newResponse.getEntity().getContentType().getValue());

			if (contentType.mimeType() == MimeType.Plain || contentType.mimeType() == MimeType.Json) {
				String charset = contentType.getCharset("UTF-8");
				String responseEntity = IOUtils.toString(newResponse.getEntity().getContent(), charset);

				debug.append("response Entity:\n" + responseEntity + "\n");

				IOUtils.write(responseEntity, response.getOutputStream(), charset);
			} else if (newResponse.getEntity() != null) {
				IOUtils.copy(newResponse.getEntity().getContent(), response.getOutputStream());
			}

			response.setStatus(newResponse.getStatusLine().getStatusCode());

			Engine.logCouchDbManager.info("(FullSyncServlet) Success to process request:\n" + debug);
		} catch (Exception e) {
			Engine.logCouchDbManager.error("(FullSyncServlet) Failed to process request:\n" + debug, e);
		}
	}
	
	static class RequestParser {
		static final private Pattern pPath = Pattern.compile("^(?:/~([^/]*))?((?:/(_[^/]*))?(?:/([^/]*))?(?:/(_[^/]*))?(?:/([^/]*))?(.*))$");
		
		private String id;
		private String path;
		private String special;
		private String dbName;
		private String docId;
		
		RequestParser(String request_path) {
			Matcher mPath = pPath.matcher(request_path);
			if (mPath.matches()) {
				id = mPath.group(1);
				path = mPath.group(2);
				special = mPath.group(3);
				if (special == null) {
					dbName = mPath.group(4);
					special = mPath.group(5);
					docId = mPath.group(6);
				}
			}
		}

		public String getId() {
			return id;
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
