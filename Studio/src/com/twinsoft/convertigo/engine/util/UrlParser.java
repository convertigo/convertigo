package com.twinsoft.convertigo.engine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlParser {
	final static Pattern scheme_host_pattern = Pattern.compile("(.*?)://(.*?)(?::([\\d]*))?(/.*|$)");
	
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
