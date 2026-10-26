/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import org.apache.commons.httpclient.Header;

/** A diagnostic view only: never mutate the headers sent to the server. */
public final class HttpHeaderSanitizer {
	private HttpHeaderSanitizer() { }

	public static String value(String name, String value) {
		return "Authorization".equalsIgnoreCase(name) || "Proxy-Authorization".equalsIgnoreCase(name)
				? "[redacted]" : value;
	}

	public static String describe(Header[] headers) {
		var text = new StringBuilder();
		for (var header : headers) {
			text.append(header.getName()).append(": ").append(value(header.getName(), header.getValue())).append('\n');
		}
		return text.toString();
	}
}
