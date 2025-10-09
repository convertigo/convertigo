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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

public class ServletUtils {
	private static final Pattern p_mobile = Pattern.compile("(.*/DisplayObjects/(?:mobile|pwas/.*?)/)(.*)");
	private static final Pattern p_base = Pattern.compile("(<base\\s+[^>]*href\\s*=\\s*)(['\"])([^'\"]*)(\\2)([^>]*>)", Pattern.CASE_INSENSITIVE);
	private static final String ATTR_BASE_DEPTH = ServletUtils.class.getName() + ".baseDepth";
	
	public static void handleFileFilter(File file, HttpServletRequest request, HttpServletResponse response, FilterConfig filterConfig, FilterChain chain) throws IOException, ServletException {
		if (file.exists()) {
			Engine.logContext.debug("Static file");
			HttpUtils.applyCorsHeaders(request, response);

			var normalizedPath = file.getPath().replace('\\', '/');
			byte[] rewritten = null;
			var matcher = p_mobile.matcher(normalizedPath);
			Integer depthOverride = null;
			var depthAttr = request.getAttribute(ATTR_BASE_DEPTH);
			if (depthAttr instanceof Integer depthValue) {
				depthOverride = depthValue;
			}

			// Warning date comparison: 'If-Modified-Since' header precision is second,
			// although file date precision is milliseconds on Windows
			long clientDate = request.getDateHeader("If-Modified-Since") / 1000;
			Engine.logContext.debug("If-Modified-Since: " + clientDate);
			long fileDate = file.lastModified() / 1000;
			Engine.logContext.debug("File date: " + fileDate);
			if (clientDate == fileDate && fileDate > 0) {
				Engine.logContext.debug("Returned HTTP 304 Not Modified");
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			}
			else {
				long maxAge = EnginePropertiesManager.getPropertyAsLong(PropertyName.NET_MAX_AGE);

				// Serve static files if they exist in the projects repository.
			if ("index.html".equalsIgnoreCase(file.getName()) && matcher.matches()) {
				var depth = depthOverride != null ? depthOverride : computeDepth(request);
				if (depth != null) {
					var baseHref = buildBaseHref(depth);
					var content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
					var baseMatcher = p_base.matcher(content);
					if (baseMatcher.find()) {
						var buffer = new StringBuffer();
						var attrs = baseMatcher.group(5);
						if (!attrs.contains("data-c8o-mode")) {
							attrs = attrs.replaceFirst("/?>", " data-c8o-mode=\"web\"$0");
						}
						baseMatcher.appendReplacement(buffer, baseMatcher.group(1) + baseMatcher.group(2) + Matcher.quoteReplacement(baseHref) + baseMatcher.group(4) + attrs);
						baseMatcher.appendTail(buffer);
						rewritten = buffer.toString().getBytes(StandardCharsets.UTF_8);
					}
				}
			}

				String mimeType = filterConfig.getServletContext().getMimeType(file.getName());
				Engine.logContext.debug("Found MIME type: " + mimeType);
				HeaderName.ContentType.setHeader(response, mimeType);
				HeaderName.CacheControl.setHeader(response, "max-age=" + maxAge	);
				var length = rewritten != null ? rewritten.length : file.length();
				HeaderName.ContentLength.setHeader(response, "" + length);
				response.setDateHeader(HeaderName.LastModified.value(), file.lastModified());

				FileInputStream fileInputStream = null;
				OutputStream output = response.getOutputStream();
				try {
					if (rewritten != null) {
						output.write(rewritten);
					} else {
						fileInputStream = new FileInputStream(file);
						IOUtils.copy(fileInputStream, output);
					}
				}
				finally {
					if (fileInputStream != null) {
						fileInputStream.close();
					}
				}
			}
		} else {
			Matcher m = p_mobile.matcher(file.getPath().replace('\\', '/'));
			if (m.matches()) {
				var depth = computeDepth(request);
				if (depth != null) {
					request.setAttribute(ATTR_BASE_DEPTH, depth);
				}
				File index = new File(m.group(1), "index.html");
				if (!index.equals(file)) {
					handleFileFilter(index, request, response, filterConfig, chain);
					request.removeAttribute(ATTR_BASE_DEPTH);
					return;
				}
			}
			Engine.logContext.debug("Convertigo request => follow the normal filter chain");
			chain.doFilter(request, response);
		}
	}

	public static void applyCustomHeaders(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> headers = RequestAttribute.responseHeader.get(request);
		String user = SessionAttribute.authenticatedUser.string(request.getSession(false));
		if (user != null) {
			user = Base64.encodeBase64String(DigestUtils.sha1(request.getSession().getId() + user));
			HeaderName.XConvertigoAuthenticated.addHeader(response, user);
		}
		if (headers != null) {
			Engine.logContext.debug("Setting custom response headers (" + headers.size() + ")");
			for (Entry<String, String> header : headers.entrySet()) {
				Engine.logContext.debug("Setting custom response header: " + header.getKey() + "=" + header.getValue());
				response.setHeader(header.getKey(), header.getValue());
			}
		}
		if (HeaderName.Origin.has(request)) {
			StringBuilder sb = new StringBuilder();
			for (String header: response.getHeaderNames()) {
				if (header.toLowerCase().startsWith("x-")) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(header);
				}
			}
			if (sb.length() > 0) {
				HeaderName.AccessControlExposeHeaders.addHeader(response, sb.toString());
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void applyCustomStatus(HttpServletRequest request, HttpServletResponse response) {
		Map<Integer, String> status = RequestAttribute.responseStatus.get(request);
		if (status != null) {
			Engine.logContext.debug("Setting custom response status");
			if (!status.isEmpty()) {
				Entry<Integer, String> entry = status.entrySet().iterator().next();
				Engine.logContext.debug("Setting custom response status: " + entry.getKey() + "=" + entry.getValue());
				response.setStatus(entry.getKey(), entry.getValue());
				//response.setStatus(entry.getKey());
			}
		}
	}

private static Integer computeDepth(HttpServletRequest request) {
		var uri = request.getRequestURI();
		var matcher = p_mobile.matcher(uri);
		if (!matcher.matches()) {
			return null;
		}
		var suffix = uri.substring(matcher.group(1).length());
		if (suffix.isEmpty()) {
			return 0;
		}
		if (suffix.endsWith("/")) {
			suffix = suffix.substring(0, suffix.length() - 1);
		}
		if (suffix.isEmpty()) {
			return 0;
		}
		var segments = suffix.split("/");
		return Math.max(segments.length - 1, 0);
	}

	private static String buildBaseHref(int depth) {
		if (depth <= 0) {
			return "./";
		}
		var builder = new StringBuilder(depth * 3);
		for (var i = 0; i < depth; i++) {
			builder.append("../");
		}
		return builder.toString();
	}
}
