package com.twinsoft.convertigo.engine.oauth;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.twinsoft.convertigo.engine.Engine;

public class HttpMethodAdapter implements oauth.signpost.http.HttpRequest {

    private HttpMethod request;
    private RequestEntity entity;
    private HostConfiguration hc;

    public HttpMethodAdapter(HttpMethod request, HostConfiguration hostConfiguration) {
        this.request = request;
        if (request instanceof EntityEnclosingMethod) {
            entity = ((EntityEnclosingMethod) request).getRequestEntity();
        }
        hc = hostConfiguration;
    }

    public String getHeader(String name) {
        Header header = request.getRequestHeader(name);
        if (header == null) {
            return null;
        }
        String value = header.getValue();
        return value;
    }

    public String getMethod() {
    	String name = request.getName();
        return name;
    }

    public String getRequestUrl() {
        try {
        	String uri = request.getURI().toString();
        	String url = hc.getHostURL() + uri;
			return url;
		} catch (URIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }

    public void setHeader(String name, String value) {
    	Engine.logBeans.debug("(HttpConnector) oAuth set headers \"" + name + "\" to \"" + value + "\"");
        request.setRequestHeader(name, value);
    }

    public String getContentType() {
        if (entity == null) {
            return null;
        }
        String contentType = entity.getContentType(); 
        return (contentType);
    }

    public InputStream getMessagePayload() throws IOException {
        if (entity == null) {
            return null;
        }
        if (entity instanceof StringRequestEntity) {
        	String charset = ((StringRequestEntity) entity).getCharset();
        	InputStream is = new ByteArrayInputStream(((StringRequestEntity) entity).getContent().getBytes(charset));
        	return (is);
        }
        else
        	return null;
    }

	public Map<String, String> getAllHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setRequestUrl(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public Object unwrap() {
		// TODO Auto-generated method stub
		return request;
	}
}
