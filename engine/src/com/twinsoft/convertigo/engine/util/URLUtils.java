/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;

public class URLUtils {
	static private Pattern scheme_host_pattern = Pattern.compile("(^https?://.*?)(?:/.*|$)");
	static private Pattern splitEqual = Pattern.compile("=");
	static private Pattern splitAnd = Pattern.compile("&");
	static private Pattern pathPart = Pattern.compile("/([^/]*)");
	
	public static String encodePath(String path, String urlEncoding) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		Matcher m = pathPart.matcher(path);
		while (m.find()) {
			sb.append('/').append(URLEncoder.encode(m.group(1), urlEncoding));
		}
		return sb.toString();
	}
	
	public static String encodeAbsoluteURL(String url, String urlEncoding) {
		String sUrl = url;
		try {
			int index = ((url.startsWith("http://")) ? 7 : ((url.startsWith("https://") ? 8 : -1)));
			if (index != -1) {
				sUrl = (index == 7) ? "http://" : "https://";
				String[] fields = url.substring(index).split("/");
				int len = fields.length;
				if (len > 0) {
					index = fields[0].indexOf(":");
					sUrl += ((index != -1) ? encodeField(fields[0].substring(0, index), urlEncoding) + fields[0].substring(index) : encodeField(fields[0], urlEncoding)) + ((0 < len - 1) ? "/":"");
				}
				for (int i = 1; i < len; i++) {
					sUrl += encodeField(fields[i], urlEncoding) + ((i<len-1) ? "/" : "");

				}
			}
		} catch (UnsupportedEncodingException e) {
			return url;
		}
		if (url.endsWith("/")) {
			sUrl+="/";
		}
		return sUrl;
	}

	static private Pattern alreadyPattern = Pattern.compile("%[0-9a-fA-F]{2}");
	public static String encodeComponentIfNot(String component, String urlEncoding) throws UnsupportedEncodingException {
		Matcher alreadyMatcher = alreadyPattern.matcher(component);
		return encodeComponentIfNot(alreadyMatcher, component, urlEncoding);
	}
	
	private static String encodeComponentIfNot(Matcher alreadyMatcher, String component, String urlEncoding) throws UnsupportedEncodingException {
		if (alreadyMatcher.find()) {
			try {
				URLDecoder.decode(component, urlEncoding);
				return component;
			} catch (Exception e) {
			}
		}
		return URLEncoder.encode(component, urlEncoding);
	}

	static private Pattern fieldSeparatorsPattern = Pattern.compile("[?&=]");
	public static String encodeField(String field, String urlEncoding) throws UnsupportedEncodingException {
		String encodedField = field;
		if ((field != null) && (!field.equals(""))) {
			Matcher myMatcher = fieldSeparatorsPattern.matcher(field);
			Matcher alreadyMatcher = alreadyPattern.matcher("");
			
			List<String> splitString = new LinkedList<String>();
			int beginIndex = 0, startIndex, endIndex;
			while (myMatcher.find()) {
				startIndex = myMatcher.start();
				endIndex = myMatcher.end();
				if ((beginIndex != startIndex) && (startIndex != -1) && (beginIndex <= startIndex)) {
					String component = field.substring(beginIndex, startIndex);
					alreadyMatcher.reset(component);
					splitString.add(encodeComponentIfNot(alreadyMatcher, component, urlEncoding));
				}
				splitString.add(field.substring(startIndex, endIndex));
				beginIndex = endIndex;
			}
			if (beginIndex != field.length()) {
				String component = field.substring(beginIndex, field.length());
				alreadyMatcher.reset(component);
				splitString.add(encodeComponentIfNot(alreadyMatcher, component, urlEncoding));
			}
			
			if (splitString.size() > 0) {
				encodedField = "";
			}
			for (String splitted : splitString) {
				if ((splitted != null) && (!splitted.equals(""))) {
					encodedField += splitted;
				}
			}
		}
		return encodedField;
	}
	
	public static String extractPathReferer(String url){
		int id = url.indexOf('?');
		if (id!=-1) {
			url = url.substring(0, id);
		}
		if (!url.endsWith("/")) {
			url = url.substring(0, url.lastIndexOf('/')+1);
		}
		return url;
	}
	
	public static String extractHost(String url, String def){
		if (def == null) {
			def = url;
		}
		int firstSlashAfterHttp = url.indexOf('/', url.indexOf("://") + 3);
		String host = (firstSlashAfterHttp == -1) ? def : url.substring(0, firstSlashAfterHttp);
		if (host.length() == 0) {
			host = def;
		}
		return host;
	}
	
	public static String extractHost(String url){
		return extractHost(url, null);
	}
	
	public static String getFullpathRessources(Class<?> c, String ressource) {
		URL url = c.getResource(ressource);
		if (url == null) {
			return null;
		}
		try {
			Class<?> fl = Class.forName("org.eclipse.core.runtime.FileLocator");
			url = (URL) fl.getMethod("toFileURL", URL.class).invoke(null, url);
		} catch (Exception e) {} // ENGINE MODE, getResource works directly
		String f = null;
		try {
			f = url.toURI().getPath();
		} catch (Throwable e) {
		}
		if (f == null) {
			f = url.getFile();
		}
		return f;
	}
	
	public static Map<String, String[]> queryToMap(String query, Pattern andPattern, Pattern equalPattern) {
		Map<String, String[]> parameters = new HashMap<String, String[]>();
		for (String part : andPattern.split(query)) {
			String[] pair = equalPattern.split(part, 2);
			try {
				String key = URLDecoder.decode(pair[0], "UTF-8");
				String value = pair.length > 1 ? URLDecoder.decode(pair[1], "UTF-8") : ""; 
				String[] list = parameters.get(key);
				if (list != null) {
					list = GenericUtils.copyOf(list, list.length + 1);
					list[list.length - 1] = value;
				} else {
					list = new String[]{value};
				}
				parameters.put(key, list);
			} catch (UnsupportedEncodingException e) {
				Engine.logEngine.error("Unable to parse '" + part + "' as parameter", e);
			}
		}
		return parameters;
	}
	
	public static Map<String, String[]> queryToMap(String query) {
		return queryToMap(query, splitAnd, splitEqual);
	}
	
	public static String mapToQuery(Map<String, String[]> map, String andString, String equalString) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String[]> entry : map.entrySet()) {
			try {
				String key = URLEncoder.encode(entry.getKey(), "UTF-8");
				String[] values = entry.getValue();
				if (values != null && values.length > 0) {
					for (String value : values) {
						sb.append(key).append(equalString).append(URLEncoder.encode(value, "UTF-8")).append(andString);
					}
				} else {
					sb.append(key).append(andString);
				}
			} catch (UnsupportedEncodingException e) {
				Engine.logEngine.info("(URLUtils) mapToQuery failed to encode '" + entry.getKey() +"' = '" + entry.getValue() + "'", e);
			}
		}
		if (sb.length() > 0) {
			return sb.substring(0, sb.length() - 1);
		} else {
			return "";
		}
	}
	
	public static String mapToQuery(Map<String, String[]> map) {
		return mapToQuery(map, "&", "=");
	}
	
	public static String getSchemeAndHost(String url) {
		Matcher m = scheme_host_pattern.matcher(url);
		if (m.matches()) {
			return m.group(1);
		}
		return null;
	}
	
	public static String encodeUTF8(String part) {
		try {
			return URLEncoder.encode(part, "UTF-8").replace("+", "%20");
		} catch (Throwable t) {
			Engine.logEngine.warn("(URLUtils) encodeUTF8 failed", t);
			return part;
		}
	}
	
	public static String encodePart(String key, String value) {
		return encodePart(key, value, "UTF-8");
	}
	
	public static String encodePart(String key, String value, String encoding) {
		try {
			return URLEncoder.encode(key, encoding) + "=" + URLEncoder.encode(value, encoding);
		} catch (Throwable t) {
			Engine.logEngine.warn("(URLUtils) encodePart failed", t);
			return key + "=" + value;
		}
	}
}
