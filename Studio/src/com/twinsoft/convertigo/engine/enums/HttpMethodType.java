package com.twinsoft.convertigo.engine.enums;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;

public enum HttpMethodType {
	GET(HttpGet.class),
	POST(HttpPost.class),
	PUT(HttpPut.class),
	DELETE(HttpDelete.class),
	HEAD(HttpHead.class),
	TRACE(HttpTrace.class),
	OPTIONS(HttpOptions.class);
	
	Class<? extends HttpRequestBase> methodClass;
	
	HttpMethodType(Class<? extends HttpRequestBase> methodClass) {
		this.methodClass = methodClass;
	}
	
	public HttpRequestBase newInstance() {
		try {
			return methodClass.newInstance();
		} catch (Exception e) {
			return null;
		}
	}
}