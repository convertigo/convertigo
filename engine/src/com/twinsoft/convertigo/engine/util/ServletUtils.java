/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

public class ServletUtils {
	private static final Pattern p_mobile = Pattern.compile("(.*/DisplayObjects/(?:mobile|pwas/.*?)/)(.*)");
	private static final Pattern p_base = Pattern.compile("(<base\\s+[^>]*href\\s*=\\s*)(['\"])([^'\"]*)(\\2)([^>]*>)", Pattern.CASE_INSENSITIVE);
	private static final Pattern APP_TEMPLATE_VERSION_PATTERN = Pattern.compile("\\s*(\\d+)(?:\\.(\\d+))?.*");
	private static final String ATTR_BASE_DEPTH = ServletUtils.class.getName() + ".baseDepth";
	private static final Pattern RANGE_PATTERN = Pattern.compile("bytes\\s*=\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern RANGE_ITEM_PATTERN = Pattern.compile("(\\d*)-(\\d*)");
	private static final Pattern P_DISPLAY_OBJECTS_SPA = Pattern.compile("^/(?:system/)?projects/[^/]+/DisplayObjects/(?:mobile|pwas/[^/]+)(?:/.*)?$");
	private static final Pattern HASHED_DISPLAY_OBJECTS_FILE = Pattern.compile(
			".*-[A-Za-z0-9_-]{8,}\\.(?:css|js|mjs|map|png|apng|jpe?g|gif|svg|webp|avif|ico|woff2?|ttf|otf|eot|wasm)$",
			Pattern.CASE_INSENSITIVE);
	private static final String CACHE_CONTROL_IMMUTABLE = "public, max-age=31536000, immutable";
	private static final String CACHE_CONTROL_REVALIDATE = "no-cache, must-revalidate";
	private static final Set<String> MOBILE_STATIC_EXTENSIONS = Set.of("js", "mjs", "css", "map", "json", "webmanifest", "png", "jpg", "jpeg", "gif", "svg", "ico", "webp", "avif", "woff", "woff2", "ttf", "eot", "otf", "wasm");
	
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
				if ("index.html".equalsIgnoreCase(file.getName()) && matcher.matches() && isBaseHrefRewriteSupported(file)) {
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
				var staticCacheControl = getStaticCacheControl(request, file);
				if (staticCacheControl != null) {
					HeaderName.CacheControl.setHeader(response, staticCacheControl);
				}
				if (!HeaderName.CacheControl.has(response)) {
					HeaderName.CacheControl.setHeader(response, "max-age=" + maxAge);
				}
				writeFile(request, response, file, rewritten, mimeType);
			}
		} else {
			Matcher m = p_mobile.matcher(file.getPath().replace('\\', '/'));
			if (m.matches() && shouldFallbackToMobileIndex(m.group(2))) {
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

	/**
	 * Serve a static file with optional rewritten content. Supports Range (single range) and 304.
	 */
	public static void serveFile(File file, HttpServletRequest request, HttpServletResponse response, String mimeType) throws IOException {
		if (file == null || !file.exists()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		HttpUtils.applyCorsHeaders(request, response);
		writeFile(request, response, file, null, mimeType);
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

	public static void applyCustomStatus(HttpServletRequest request, HttpServletResponse response) {
		Map<Integer, String> status = RequestAttribute.responseStatus.get(request);
		if (status != null) {
			Engine.logContext.debug("Setting custom response status");
			if (!status.isEmpty()) {
				Entry<Integer, String> entry = status.entrySet().iterator().next();
				Engine.logContext.debug("Setting custom response status: " + entry.getKey() + "=" + entry.getValue());
				// Reason phrases were removed from the Servlet API in Jakarta Servlet 6.
				response.setStatus(entry.getKey());
			}
		}
	}

	private static String getStaticCacheControl(HttpServletRequest request, File file) {
		var path = request.getRequestURI().substring(request.getContextPath().length());
		if (!P_DISPLAY_OBJECTS_SPA.matcher(path).matches()) {
			return null;
		}
		if ("index.html".equalsIgnoreCase(file.getName())) {
			return CACHE_CONTROL_REVALIDATE;
		}
		if (HASHED_DISPLAY_OBJECTS_FILE.matcher(file.getName()).matches()) {
			return CACHE_CONTROL_IMMUTABLE;
		}
		return null;
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

	private static boolean shouldFallbackToMobileIndex(String relativePath) {
		return !isKnownMobileStaticResource(relativePath);
	}

	private static boolean isKnownMobileStaticResource(String relativePath) {
		if (relativePath == null || relativePath.isEmpty()) {
			return false;
		}
		if (relativePath.endsWith("/")) {
			relativePath = relativePath.substring(0, relativePath.length() - 1);
		}
		if (relativePath.isEmpty()) {
			return false;
		}
		var lastSlash = relativePath.lastIndexOf('/');
		var lastSegment = lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
		var lastDot = lastSegment.lastIndexOf('.');
		if (lastDot <= 0 || lastDot == lastSegment.length() - 1) {
			return false;
		}
		var extension = lastSegment.substring(lastDot + 1).toLowerCase();
		return MOBILE_STATIC_EXTENSIONS.contains(extension);
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

	private static boolean isBaseHrefRewriteSupported(File indexFile) {
		var envFile = new File(indexFile.getParentFile(), "env.json");
		if (!envFile.isFile()) {
			return false;
		}
		try {
			var content = Files.readString(envFile.toPath(), StandardCharsets.UTF_8);
			var version = new JSONObject(content).optString("appTemplateVersion");
			return isAppTemplateVersionAtLeast84(version);
		} catch (Exception e) {
			Engine.logContext.debug("Unable to read appTemplateVersion from " + envFile.getAbsolutePath(), e);
			return false;
		}
	}

	private static boolean isAppTemplateVersionAtLeast84(String version) {
		var matcher = APP_TEMPLATE_VERSION_PATTERN.matcher(version == null ? "" : version);
		if (!matcher.matches()) {
			return false;
		}
		try {
			var major = Integer.parseInt(matcher.group(1));
			var minor = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
			return major > 8 || major == 8 && minor >= 4;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static void writeFile(HttpServletRequest request, HttpServletResponse response, File file, byte[] rewritten, String mimeType) throws IOException {
		long fileLength = rewritten != null ? rewritten.length : file.length();

		// Only honor If-Modified-Since when not a range request.
		var rangeHeader = HeaderName.Range.getHeader(request);
		boolean hasRange = rangeHeader != null && !rangeHeader.isBlank();
		ByteRange singleRange = null;
		List<ByteRange> ranges = null;
		String multipartBoundary = null;

		if (!hasRange) {
			long clientDate = request.getDateHeader("If-Modified-Since") / 1000;
			long fileDate = file.lastModified() / 1000;
			if (clientDate == fileDate && fileDate > 0) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
		}

		if (hasRange) {
			if (fileLength <= 0) {
				response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				HeaderName.ContentRange.setHeader(response, "bytes */" + fileLength);
				return;
			}
			ranges = parseRanges(rangeHeader, fileLength);
			if (ranges == null || ranges.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				HeaderName.ContentRange.setHeader(response, "bytes */" + fileLength);
				return;
			}
			if (ranges.size() == 1) {
				singleRange = ranges.get(0);
			} else {
				multipartBoundary = "C8O-" + Long.toHexString(System.nanoTime());
			}
		}

		if (multipartBoundary != null) {
			HeaderName.ContentType.setHeader(response, "multipart/byteranges; boundary=" + multipartBoundary);
		} else if (mimeType != null) {
			HeaderName.ContentType.setHeader(response, mimeType);
		}
		long maxAge = EnginePropertiesManager.getPropertyAsLong(PropertyName.NET_MAX_AGE);
		HeaderName.CacheControl.setHeader(response, "max-age=" + maxAge);
		response.setDateHeader(HeaderName.LastModified.value(), file.lastModified());
		HeaderName.AcceptRanges.setHeader(response, "bytes");

		if (singleRange != null) {
			long rangeLength = singleRange.length();
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
			HeaderName.ContentRange.setHeader(response, "bytes " + singleRange.start + "-" + singleRange.end + "/" + fileLength);
			HeaderName.ContentLength.setHeader(response, Long.toString(rangeLength));
		} else if (multipartBoundary != null) {
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
			long bodyLength = computeMultipartBodyLength(ranges, multipartBoundary, mimeType, fileLength);
			HeaderName.ContentLength.setHeader(response, Long.toString(bodyLength));
		} else {
			HeaderName.ContentLength.setHeader(response, Long.toString(fileLength));
		}

		OutputStream output = response.getOutputStream();
		if (multipartBoundary != null) {
			writeMultipartBody(output, file, rewritten, ranges, multipartBoundary, mimeType, fileLength);
			return;
		}

		if (rewritten != null) {
			if (singleRange != null) {
				int offset = (int) singleRange.start;
				int len = (int) singleRange.length();
				output.write(rewritten, offset, len);
			} else {
				output.write(rewritten);
			}
			return;
		}

		try (var fis = new FileInputStream(file)) {
			if (singleRange != null && singleRange.start > 0) {
				long skipped = 0;
				while (skipped < singleRange.start) {
					long justSkipped = fis.skip(singleRange.start - skipped);
					if (justSkipped <= 0) {
						break;
					}
					skipped += justSkipped;
				}
			}
			byte[] buffer = new byte[8192];
			long remaining = singleRange != null ? singleRange.length() : Long.MAX_VALUE;
			int read;
			while (remaining > 0 && (read = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
				output.write(buffer, 0, read);
				remaining -= read;
			}
		}
	}

	private static List<ByteRange> parseRanges(String rangeHeader, long fileLength) {
		var matcher = RANGE_PATTERN.matcher(rangeHeader);
		if (!matcher.matches()) {
			return null;
		}

		var parsed = new ArrayList<ByteRange>();
		var rangeSet = matcher.group(1);
		for (var rawItem: rangeSet.split(",")) {
			var item = rawItem.trim();
			if (item.isEmpty()) {
				return null;
			}
			var itemMatcher = RANGE_ITEM_PATTERN.matcher(item);
			if (!itemMatcher.matches()) {
				return null;
			}

			var startGroup = itemMatcher.group(1);
			var endGroup = itemMatcher.group(2);
			long start;
			long end;
			try {
				if (startGroup.isEmpty()) {
					if (endGroup.isEmpty()) {
						return null;
					}
					long suffixLength = Long.parseLong(endGroup);
					if (suffixLength <= 0) {
						return null;
					}
					start = Math.max(fileLength - suffixLength, 0);
					end = fileLength - 1;
				} else {
					start = Long.parseLong(startGroup);
					if (endGroup.isEmpty()) {
						end = fileLength - 1;
					} else {
						end = Long.parseLong(endGroup);
					}
				}
			} catch (NumberFormatException e) {
				return null;
			}

			if (start < 0 || end < 0 || end < start) {
				return null;
			}
			if (start >= fileLength) {
				continue;
			}
			if (end >= fileLength) {
				end = fileLength - 1;
			}

			parsed.add(new ByteRange(start, end));
		}

		return parsed;
	}

	private static long computeMultipartBodyLength(List<ByteRange> ranges, String boundary, String mimeType, long fileLength) {
		long total = 0;
		for (var range: ranges) {
			total += boundaryLine(boundary).getBytes(StandardCharsets.ISO_8859_1).length;
			if (mimeType != null) {
				total += contentTypeLine(mimeType).getBytes(StandardCharsets.ISO_8859_1).length;
			}
			total += contentRangeLine(range, fileLength).getBytes(StandardCharsets.ISO_8859_1).length;
			total += 2; // CRLF before body
			total += range.length();
			total += 2; // CRLF after body
		}
		total += closingBoundaryLine(boundary).getBytes(StandardCharsets.ISO_8859_1).length;
		return total;
	}

	private static void writeMultipartBody(OutputStream output, File file, byte[] rewritten, List<ByteRange> ranges, String boundary, String mimeType, long fileLength) throws IOException {
		byte[] buffer = new byte[8192];
		try (var raf = rewritten == null ? new RandomAccessFile(file, "r") : null) {
			for (var range: ranges) {
				output.write(boundaryLine(boundary).getBytes(StandardCharsets.ISO_8859_1));
				if (mimeType != null) {
					output.write(contentTypeLine(mimeType).getBytes(StandardCharsets.ISO_8859_1));
				}
				output.write(contentRangeLine(range, fileLength).getBytes(StandardCharsets.ISO_8859_1));
				output.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));

				if (rewritten != null) {
					output.write(rewritten, (int) range.start, (int) range.length());
				} else if (raf != null) {
					raf.seek(range.start);
					long remaining = range.length();
					while (remaining > 0) {
						int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
						if (read == -1) {
							break;
						}
						output.write(buffer, 0, read);
						remaining -= read;
					}
				}
				output.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
			}
		}
		output.write(closingBoundaryLine(boundary).getBytes(StandardCharsets.ISO_8859_1));
	}

	private static String boundaryLine(String boundary) {
		return "--" + boundary + "\r\n";
	}

	private static String closingBoundaryLine(String boundary) {
		return "--" + boundary + "--\r\n";
	}

	private static String contentTypeLine(String mimeType) {
		return "Content-Type: " + mimeType + "\r\n";
	}

	private static String contentRangeLine(ByteRange range, long fileLength) {
		return "Content-Range: bytes " + range.start + "-" + range.end + "/" + fileLength + "\r\n";
	}

	private static class ByteRange {
		private final long start;
		private final long end;

		private ByteRange(long start, long end) {
			this.start = start;
			this.end = end;
		}

		private long length() {
			return end - start + 1;
		}
	}
}
