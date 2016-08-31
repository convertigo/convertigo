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

package com.twinsoft.convertigo.engine.enums;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.http.Header;
import org.apache.http.HttpRequest;

public enum HeaderName {
	Accept("Accept"),
	AcceptEncoding("Accept-Encoding"),
	CacheControl("Cache-Control"),
	Connection("Connection"),
	ContentDisposition("Content-Disposition"),
	ContentEncoding("Content-Encoding"),
	ContentLength("Content-Length"),
	ContentLocation("Content-Location"),
	ContentType("Content-Type"),
	Cookie("Cookie"),
	Destination("Destination"),
	ETag("ETag"),
	Expect("Expect"),
	Expires("Expires"),
	Host("Host"),
	IfMatch("If-Match"),
	IfModifiedSince("If-Modified-Since"),
	IfNoneMatch("If-None-Match"),
	IfRange("If-Range"),
	IfUnmodifiedSince("If-Unmodified-Since"),
	LastModified("Last-Modified"),
	Location("Location"),
	OAuthKey("oAuthKey"),
	OAuthSecret("oAuthSecret"),
	OAuthToken("oAuthToken"),
	OAuthTokenSecret("oAuthTokenSecret"),
	Pragma("Pragma"),
	Referer("Referer"),
	SetCookie("Set-Cookie"),
	TransferEncoding("Transfer-Encoding"),
	UserAgent("User-Agent"),
	XConvertigoFrontal("x-convertigo-frontal"),
	XConvertigoHttpsState("x-convertigo-https-state"),
	XConvertigoRequestURI("x-convertigo-request-uri"),
	XConvertigoRequestHost("x-convertigo-request-host"),
	XConvertigoSDK("x-convertigo-sdk"),
	VOID("");
	
	String value;
	
	HeaderName(String value) {
		this.value = value;
	}
	
	public String value() {
		return value;
	}
	
	public boolean is(String headerName) {
		return value.equalsIgnoreCase(headerName);
	}
	
	public boolean is(Header header) {
		return is(header.getName());
	}

	public boolean is(org.apache.commons.httpclient.Header header) {
		return is(header.getName());
	}
	
	public void setRequestHeader(HttpMethod method, String headerValue) {
		method.setRequestHeader(value, headerValue);
	}
	
	public String getHeader(HttpServletRequest request) {
		return request.getHeader(value);
	}
	
	public void addHeader(HttpServletResponse response, String headerValue) {
		response.addHeader(value, headerValue);
	}
	
	public void addHeader(HttpRequest request, String headerValue) {
		request.addHeader(value, headerValue);
	}	
	
	public void setHeader(HttpServletResponse response, String headerValue) {
		response.setHeader(value, headerValue);
	}
	
	private static Map<String, HeaderName> cache = new HashMap<String, HeaderName>();
	static {
		for (HeaderName headerName : HeaderName.values()) {
			cache.put(headerName.value.toLowerCase(), headerName);
		}
	}
	
	public static HeaderName parse(String name) {
		HeaderName headerName = cache.get(name.toLowerCase());
		return headerName != null ? headerName : VOID;
	}
}