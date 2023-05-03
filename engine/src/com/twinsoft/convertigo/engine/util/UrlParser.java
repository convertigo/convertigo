/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlParser {
	private final static Pattern scheme_host_pattern = Pattern.compile("(.*?)://(.*?)(?::([\\d]*))?(/.*|$)");
	
	static public class UrlFields {
		private String scheme;
		private String host;
		private String port;
		private String path;
		
		private UrlFields() {
		}
		
		public String getScheme() {
			return scheme;
		}
		
		public String getHost() {
			return host;
		}
		
		public String getPort() {
			return port;
		}
		
		public String getPath() {
			return path;
		}
	}

	private UrlParser() {
	}
	
	public static UrlFields parse(String url) {
		Matcher matcher = scheme_host_pattern.matcher(url);
		if (matcher.matches()) {
			UrlFields urlFields = new UrlFields();
			urlFields.scheme = matcher.group(1);
			urlFields.host = matcher.group(2);
			urlFields.port = matcher.group(3);
			urlFields.path = matcher.group(4);
			return urlFields;
		}
		return null;
	}
}
