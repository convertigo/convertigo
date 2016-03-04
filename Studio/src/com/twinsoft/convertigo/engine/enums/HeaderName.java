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

public enum HeaderName {
	Accept("Accept"),
	ContentType("Content-Type"),
	ContentLength("Content-Length"),
	Location("Location"),
	ContentLocation("Content-Location"),
	ContentEncoding("Content-Encoding"),
	Destination("Destination"),
	UserAgent("User-Agent"),
	TransferEncoding("Transfer-Encoding"),
	Cookie("Cookie"),
	SetCookie("Set-Cookie"),
	LastModified("Last-Modified"),
	CacheControl("Cache-Control"),
	ETag("ETag"),
	IfMatch("If-Match"),
	IfModifiedSince("If-Modified-Since"),
	IfRange("If-Range"),
	IfNoneMatch("If-None-Match"),
	IfUnmodifiedSince("If-Unmodified-Since"),
	XConvertigoRequestURI("x-convertigo-request-uri"),
	XConvertigoRequestHost("x-convertigo-request-host"),
	XConvertigoHttpsState("x-convertigo-https-state"),
	XConvertigoFrontal("x-convertigo-frontal"),
	XConvertigoSDK("x-convertigo-sdk"),
	VOID("");
	
	String value;
	
	HeaderName(String value) {
		this.value = value;
	}
	
	public String value() {
		return value;
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