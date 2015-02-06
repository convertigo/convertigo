package com.twinsoft.convertigo.engine.cdbproxy;

import java.io.IOException;
import java.net.URISyntaxException;

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
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.ContentTypeDecoder;

public class CouchDbProxyServlet extends HttpServlet {
	private static final long serialVersionUID = -5147185931965387561L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		StringBuffer debug = new StringBuffer();
		
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
		
		try {
			StringBuilder sb = new StringBuilder(EnginePropertiesManager.getProperty(PropertyName.CDB_URL));
			if (sb.length() != 0 &&  sb.charAt(sb.length() - 1) == '/') {
				sb.deleteCharAt(sb.length() - 1);
			}
			sb.append(request.getPathInfo());
			
			debug.append(method.name() + " URI: " + sb.toString() + "\n");
			newRequest.setURI(new URIBuilder(sb.toString())
				.setCustomQuery(request.getQueryString())
				.build());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		for (String headerName: Collections.list(request.getHeaderNames())) {
//			for (String headerValue: Collections.list(request.getHeaders(headerName))) {
//				debug.append("request Header: " + headerName + "=" + headerValue + "\n");
//				newRequest.addHeader(headerName, headerValue);
//			}
//		}
		
		if (newRequest instanceof HttpEntityEnclosingRequest) {
			if (request.getHeader(HeaderName.ContentType.value()).equals("application/json")) {
				String requestEntity = IOUtils.toString(request.getInputStream(), "UTF-8");
				debug.append("request Entity:\n" + requestEntity + "\n");
				try {
					JSONObject json = new JSONObject(requestEntity);
					json.put("c8oACL", "good");
					requestEntity = json.toString();
					debug.append("request new Entity:\n" + requestEntity + "\n");
				} catch (Exception e) {}
				((HttpEntityEnclosingRequest) newRequest).setEntity(new StringEntity(requestEntity, "UTF-8"));
			} else {
				((HttpEntityEnclosingRequest) newRequest).setEntity(new InputStreamEntity(request.getInputStream()));
			}
		}
		
		HttpResponse newResponse = Engine.theApp.httpClient4.execute(newRequest);
		
//		debug.append("CT value: " + newResponse.getEntity().getContentType().getValue() + "\n");
		ContentTypeDecoder contentType = new ContentTypeDecoder(newResponse.getEntity().getContentType().getValue());
		
		if (contentType.mimeType() == MimeType.Plain) {
			String charset = contentType.getCharset("UTF-8");
			String responseEntity = IOUtils.toString(newResponse.getEntity().getContent(), charset);
			
			debug.append("response Entity:\n" + responseEntity + "\n");
			
			try {
				JSONObject json = new JSONObject(responseEntity);
				Object removed = json.remove("c8oACL");
				if (removed != null) {
					responseEntity = json.toString();
					debug.append("response new Entity:\n" + responseEntity + "\n");
				}
			} catch (Exception e) {}
			
			IOUtils.write(responseEntity, response.getOutputStream(), charset);
		} else {
			IOUtils.copy(newResponse.getEntity().getContent(), response.getOutputStream());
		}
		
		for (Header header: newResponse.getAllHeaders()) {
			if (!HeaderName.TransferEncoding.value().equalsIgnoreCase(header.getName())
					&& !HeaderName.ContentLength.value().equalsIgnoreCase(header.getName())) {
//				debug.append("response Header: " + header.getName() + "=" + header.getValue() + "\n");
				response.addHeader(header.getName(), header.getValue());
			}
		}
		
		if (contentType.mimeType() == MimeType.Plain) {
			System.err.println(debug.toString());
		}
	}	
}
