package com.twinsoft.convertigo.engine.cdbproxy;

import java.io.IOException;
import java.net.URISyntaxException;
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
import org.apache.http.client.HttpClient;
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
import org.apache.http.impl.client.HttpClients;

import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;

public class CouchDbProxyServlet extends HttpServlet {
	private static final long serialVersionUID = -5147185931965387561L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpClient client = HttpClients.createDefault();
		Pattern pSplitDB = Pattern.compile("/(.*?)(/.*)");
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
		
		Matcher mSplitDB = pSplitDB.matcher(request.getPathInfo());
		
		if (!mSplitDB.matches()) {
			
		}
		
		try {
			debug.append("URI: " + mSplitDB.group(2) + "\n");
			newRequest.setURI(new URIBuilder()
				.setScheme("http")
				.setHost("127.0.0.1")
				.setPort(5984)
				.setPath(mSplitDB.group(2))
				.setCustomQuery(request.getQueryString())
				.build());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (newRequest instanceof HttpEntityEnclosingRequest) {
			((HttpEntityEnclosingRequest) newRequest).setEntity(new InputStreamEntity(request.getInputStream()));
		}
		
		HttpResponse newResponse = client.execute(newRequest);
		IOUtils.copy(newResponse.getEntity().getContent(), response.getOutputStream());
		
		for (Header header: newResponse.getAllHeaders()) {
			if (!HeaderName.TransferEncoding.value().equalsIgnoreCase(header.getName())) {
				debug.append("Header: " + header.getName() + "=" + header.getValue() + "\n");
				response.addHeader(header.getName(), header.getValue());
			}
		}
		
		System.err.println(debug.toString());
	}	
}
