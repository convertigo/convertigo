/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.oauth;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.twinsoft.convertigo.engine.Engine;

class HttpMethodAdapter implements oauth.signpost.http.HttpRequest {

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

    @Override
    public String getHeader(String name) {
        Header header = request.getRequestHeader(name);
        if (header == null) {
            return null;
        }
        String value = header.getValue();
        return value;
    }

    @Override
    public String getMethod() {
    	String name = request.getName();
        return name;
    }

    @Override
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

    @Override
    public String getContentType() {
        if (entity == null) {
            return null;
        }
        String contentType = entity.getContentType(); 
        return (contentType);
    }

    @Override
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

    @Override
	public Map<String, String> getAllHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public void setRequestUrl(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public Object unwrap() {
		// TODO Auto-generated method stub
		return request;
	}
}
