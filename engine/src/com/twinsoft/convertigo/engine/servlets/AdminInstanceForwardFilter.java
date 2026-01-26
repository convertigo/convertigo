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

package com.twinsoft.convertigo.engine.servlets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.RedisInstanceDiscovery;

/**
 * For clustered admin usage (Redis session store), allows sticking an admin request to a specific
 * instance without relying on load balancer stickiness.
 * <p>
 * Target instance is selected via header {@code X-C8O-Instance} (preferred) or query param
 * {@code __instance}. Requests are proxied to the target instance base URL registered in Redis.
 */
public class AdminInstanceForwardFilter implements Filter {
	private static final String HEADER_INSTANCE = "X-C8O-Instance";
	private static final String PARAM_INSTANCE = "__instance";
	private static final String HEADER_FORWARDED_BY = "X-C8O-Forwarded-By";
	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

	private static final int CONNECT_TIMEOUT_MS = 5_000;
	private static final int READ_TIMEOUT_MS = 0; // keep long polling & log streaming working
	private static final long MAX_CACHED_BODY_BYTES = 256L * 1024L * 1024L;
	private static final int BODY_BUFFER_SIZE = 16 * 1024;
	private static final int FORM_PARSE_MAX_BYTES = 1 * 1024 * 1024;
	private static final Object SSL_MUTEX = new Object();
	private static volatile SSLSocketFactory trustAllSocketFactory;
	private static final HostnameVerifier TRUST_ALL_HOSTNAME = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};
	private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			// trust all
		}

		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			// trust all
		}
	};

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (!(req instanceof HttpServletRequest request) || !(res instanceof HttpServletResponse response)) {
			chain.doFilter(req, res);
			return;
		}

		if (!ConvertigoHttpSessionManager.isRedisMode()) {
			chain.doFilter(request, response);
			return;
		}

		RedisInstanceDiscovery.updateFromRequest(request);
		var localInstance = RedisInstanceDiscovery.getLocalInstanceId();
		if (localInstance != null && !localInstance.isBlank()) {
			response.setHeader(HEADER_INSTANCE, localInstance);
		}

		var requestedInstance = resolveRequestedInstance(request);
		if (requestedInstance == null) {
			chain.doFilter(request, response);
			return;
		}

		if (localInstance == null || localInstance.isBlank() || requestedInstance.equals(localInstance)) {
			chain.doFilter(request, response);
			return;
		}

		var forwardedBy = trimToNull(request.getHeader(HEADER_FORWARDED_BY));
		if (forwardedBy != null && containsForwardedId(forwardedBy, localInstance)) {
			chain.doFilter(request, response);
			return;
		}

		var baseUrl = RedisInstanceDiscovery.resolveBaseUrl(requestedInstance);
		if (baseUrl == null || baseUrl.isBlank()) {
			chain.doFilter(request, response);
			return;
		}

		var targetUrl = buildTargetUrl(baseUrl, request);
		if (targetUrl == null) {
			chain.doFilter(request, response);
			return;
		}

		CachedBody cachedBody = null;
		var proxyRequest = request;
		try {
			if (hasRequestBody(request.getMethod())) {
				cachedBody = CachedBody.capture(request);
				proxyRequest = new CachedBodyRequestWrapper(request, cachedBody);
			}

			response.setHeader(HEADER_INSTANCE, requestedInstance);
			proxy(proxyRequest, response, targetUrl, localInstance, forwardedBy);
		} catch (IOException e) {
			if (response.isCommitted()) {
				throw e;
			}
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(AdminInstanceForwardFilter) Forward failed, fallback to local instance", e);
			}
			response.reset();
			if (localInstance != null && !localInstance.isBlank()) {
				response.setHeader(HEADER_INSTANCE, localInstance);
			}
			chain.doFilter(proxyRequest, response);
		} catch (Exception e) {
			if (response.isCommitted()) {
				throw e;
			}
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(AdminInstanceForwardFilter) Unexpected forward error, fallback to local instance", e);
			}
			response.reset();
			if (localInstance != null && !localInstance.isBlank()) {
				response.setHeader(HEADER_INSTANCE, localInstance);
			}
			chain.doFilter(proxyRequest, response);
		} finally {
			if (cachedBody != null) {
				cachedBody.close();
			}
		}
	}

	@Override
	public void destroy() {
		// no-op
	}

	private static String resolveRequestedInstance(HttpServletRequest request) {
		var header = trimToNull(request.getHeader(HEADER_INSTANCE));
		if (header != null && !"auto".equalsIgnoreCase(header)) {
			return header;
		}
		var fromQuery = trimToNull(queryParam(request.getQueryString(), PARAM_INSTANCE));
		if (fromQuery != null && !"auto".equalsIgnoreCase(fromQuery)) {
			return fromQuery;
		}
		return null;
	}

	private static String buildTargetUrl(String baseUrl, HttpServletRequest request) {
		var uri = request.getRequestURI();
		if (uri == null) {
			return null;
		}
		var context = request.getContextPath();
		var path = uri;
		if (context != null && !context.isEmpty() && uri.startsWith(context)) {
			path = uri.substring(context.length());
		}
		var url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		url += path.startsWith("/") ? path : "/" + path;
		var qs = request.getQueryString();
		if (qs != null && !qs.isBlank()) {
			url += "?" + qs;
		}
		return url;
	}

	private static void proxy(HttpServletRequest request, HttpServletResponse response, String targetUrl, String localInstance,
			String forwardedBy) throws IOException {
		HttpURLConnection conn = null;
		try {
			var uri = URI.create(targetUrl);
			var scheme = uri.getScheme();
			if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
				throw new IOException("Invalid target URL scheme");
			}
			var url = uri.toURL();
			conn = (HttpURLConnection) url.openConnection();
			applyTrustAllSslIfRequested(conn);
			conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);

			var method = request.getMethod();
			conn.setRequestMethod(method);
			conn.setDoInput(true);

			copyRequestHeaders(request, conn, localInstance, forwardedBy);

			var hasBody = hasRequestBody(method);
			if (hasBody) {
				conn.setDoOutput(true);
				var contentLength = request.getContentLength();
				if (contentLength >= 0) {
					conn.setFixedLengthStreamingMode(contentLength);
				} else {
					conn.setChunkedStreamingMode(16 * 1024);
				}
				try (OutputStream out = conn.getOutputStream(); InputStream in = request.getInputStream()) {
					copy(in, out);
				}
			}

			var status = conn.getResponseCode();
			if (status == 502 || status == 503 || status == 504) {
				throw new IOException("Proxy target responded with HTTP " + status);
			}
			response.setStatus(status);
			copyResponseHeaders(conn, response);

			if (!"HEAD".equalsIgnoreCase(method)) {
				var in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
				if (in != null) {
					try (InputStream body = in; OutputStream out = response.getOutputStream()) {
						copy(body, out);
						out.flush();
					}
				}
			}
		} finally {
			if (conn != null) {
				try {
					conn.disconnect();
				} catch (Exception ignore) {
				}
			}
		}
	}

	private static void applyTrustAllSslIfRequested(HttpURLConnection conn) {
		if (!(conn instanceof HttpsURLConnection httpsConn)) {
			return;
		}
		if (!EnginePropertiesManager.getPropertyAsBoolean(PropertyName.SESSION_FORWARD_TRUST_ALL_SSL)) {
			return;
		}
		try {
			httpsConn.setSSLSocketFactory(getTrustAllSocketFactory());
			httpsConn.setHostnameVerifier(TRUST_ALL_HOSTNAME);
		} catch (Exception e) {
			try {
				if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
					Engine.logEngine.debug("(AdminInstanceForwardFilter) Failed to apply trust-all SSL settings", e);
				}
			} catch (Exception ignore) {
			}
		}
	}

	private static SSLSocketFactory getTrustAllSocketFactory() throws Exception {
		var factory = trustAllSocketFactory;
		if (factory != null) {
			return factory;
		}
		synchronized (SSL_MUTEX) {
			if (trustAllSocketFactory == null) {
				var context = SSLContext.getInstance("TLS");
				context.init(null, new TrustManager[] { TRUST_ALL_MANAGER }, new SecureRandom());
				trustAllSocketFactory = context.getSocketFactory();
			}
			return trustAllSocketFactory;
		}
	}

	private static void copyRequestHeaders(HttpServletRequest request, HttpURLConnection conn, String localInstance, String forwardedBy) {
		try {
			conn.setRequestProperty("Accept-Encoding", "identity");
		} catch (Exception ignore) {
		}

		var headerNames = request.getHeaderNames();
		while (headerNames != null && headerNames.hasMoreElements()) {
			var name = headerNames.nextElement();
			if (name == null) {
				continue;
			}
			var lower = name.toLowerCase();
			if (isHopByHopHeader(lower) || "host".equals(lower) || "content-length".equals(lower)) {
				continue;
			}
			var values = request.getHeaders(name);
			while (values != null && values.hasMoreElements()) {
				var value = values.nextElement();
				if (value != null) {
					conn.addRequestProperty(name, value);
				}
			}
		}

		var remoteAddr = trimToNull(request.getRemoteAddr());
		if (remoteAddr != null) {
			var xff = trimToNull(request.getHeader(HEADER_X_FORWARDED_FOR));
			conn.setRequestProperty(HEADER_X_FORWARDED_FOR, xff != null ? xff + ", " + remoteAddr : remoteAddr);
		}

		var nextForwardedBy = forwardedBy == null ? localInstance : forwardedBy + "," + localInstance;
		conn.setRequestProperty(HEADER_FORWARDED_BY, nextForwardedBy);
	}

	private static void copyResponseHeaders(HttpURLConnection conn, HttpServletResponse response) {
		try {
			for (var e : conn.getHeaderFields().entrySet()) {
				var name = e.getKey();
				if (name == null) {
					continue;
				}
				var lower = name.toLowerCase();
				if (isHopByHopHeader(lower) || "content-length".equals(lower) || HEADER_INSTANCE.toLowerCase().equals(lower)) {
					continue;
				}
				var values = e.getValue();
				if (values == null) {
					continue;
				}
				for (var v : values) {
					if (v != null) {
						response.addHeader(name, v);
					}
				}
			}
		} catch (Exception e) {
			try {
				if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
					Engine.logEngine.debug("(AdminInstanceForwardFilter) Failed to copy response headers", e);
				}
			} catch (Exception ignore) {
			}
		}
	}

	private static boolean isHopByHopHeader(String lowerName) {
		return "connection".equals(lowerName) || "keep-alive".equals(lowerName) || "proxy-authenticate".equals(lowerName)
				|| "proxy-authorization".equals(lowerName) || "te".equals(lowerName) || "trailer".equals(lowerName)
				|| "transfer-encoding".equals(lowerName) || "upgrade".equals(lowerName);
	}

	private static boolean hasRequestBody(String method) {
		if (method == null) {
			return false;
		}
		return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		var buffer = new byte[16 * 1024];
		int r;
		while ((r = in.read(buffer)) >= 0) {
			out.write(buffer, 0, r);
		}
	}

	private static String queryParam(String query, String key) {
		if (query == null || query.isBlank() || key == null || key.isBlank()) {
			return null;
		}
		try {
			for (var part : query.split("&")) {
				if (part.isEmpty()) {
					continue;
				}
				var idx = part.indexOf('=');
				var k = idx >= 0 ? part.substring(0, idx) : part;
				k = URLDecoder.decode(k, StandardCharsets.UTF_8.name());
				if (!key.equals(k)) {
					continue;
				}
				var v = idx >= 0 ? part.substring(idx + 1) : "";
				return URLDecoder.decode(v, StandardCharsets.UTF_8.name());
			}
		} catch (Exception ignore) {
			// ignore
		}
		return null;
	}

	private static String trimToNull(String s) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

	private static boolean containsForwardedId(String forwardedBy, String id) {
		if (forwardedBy == null || forwardedBy.isBlank() || id == null || id.isBlank()) {
			return false;
		}
		for (var part : forwardedBy.split(",")) {
			if (id.equals(part.trim())) {
				return true;
			}
		}
		return false;
	}

	private static final class CachedBody {
		private final byte[] bytes;
		private final Path file;
		private final long length;

		private CachedBody(byte[] bytes) {
			this.bytes = bytes;
			this.file = null;
			this.length = bytes == null ? 0 : bytes.length;
		}

		private CachedBody(Path file, long length) {
			this.bytes = null;
			this.file = file;
			this.length = length;
		}

		static CachedBody capture(HttpServletRequest request) throws IOException {
			var expected = request.getContentLengthLong();
			if (expected > MAX_CACHED_BODY_BYTES) {
				throw new IOException("Request body too large (" + expected + " bytes)");
			}

			Path tmp = null;
			OutputStream out = null;
			try (InputStream in = request.getInputStream()) {
				var initialSize = BODY_BUFFER_SIZE;
				if (expected > 0 && expected <= Integer.MAX_VALUE) {
					initialSize = (int) Math.min(expected, FORM_PARSE_MAX_BYTES);
				}
				var memory = new ByteArrayOutputStream(initialSize);

				long total = 0;
				var buffer = new byte[BODY_BUFFER_SIZE];
				int r;
				while ((r = in.read(buffer)) >= 0) {
					total += r;
					if (total > MAX_CACHED_BODY_BYTES) {
						throw new IOException("Request body too large (" + total + " bytes)");
					}
					if (tmp == null && total > FORM_PARSE_MAX_BYTES) {
						tmp = Files.createTempFile("c8o-admin-proxy-", ".body");
						out = Files.newOutputStream(tmp);
						memory.writeTo(out);
						memory = null;
					}
					if (tmp != null) {
						out.write(buffer, 0, r);
					} else {
						memory.write(buffer, 0, r);
					}
				}
				if (tmp != null) {
					out.close();
					out = null;
					return new CachedBody(tmp, total);
				}
				return new CachedBody(memory.toByteArray());
			} catch (IOException e) {
				if (tmp != null) {
					try {
						Files.deleteIfExists(tmp);
					} catch (Exception ignore) {
					}
				}
				throw e;
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Exception ignore) {
					}
				}
			}
		}

		long length() {
			return length;
		}

		InputStream openStream() throws IOException {
			if (bytes != null) {
				return new ByteArrayInputStream(bytes);
			}
			return Files.newInputStream(file);
		}

		byte[] getBytesIfSmall(long maxBytes) throws IOException {
			if (length > maxBytes) {
				return null;
			}
			if (bytes != null) {
				return bytes;
			}
			return Files.readAllBytes(file);
		}

		void close() {
			if (file != null) {
				try {
					Files.deleteIfExists(file);
				} catch (Exception ignore) {
				}
			}
		}
	}

	private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
		private final CachedBody cachedBody;
		private final Map<String, String[]> parameterMap;
		private final String characterEncoding;

		CachedBodyRequestWrapper(HttpServletRequest request, CachedBody cachedBody) throws IOException {
			super(request);
			this.cachedBody = cachedBody;
			this.characterEncoding = request.getCharacterEncoding();
			this.parameterMap = Collections.unmodifiableMap(parseParameterMap(request, cachedBody));
		}

		@Override
		public int getContentLength() {
			var length = cachedBody.length();
			return length > Integer.MAX_VALUE ? -1 : (int) length;
		}

		@Override
		public long getContentLengthLong() {
			return cachedBody.length();
		}

		@Override
		public String getCharacterEncoding() {
			return characterEncoding != null ? characterEncoding : StandardCharsets.UTF_8.name();
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return new RepeatableServletInputStream(cachedBody.openStream());
		}

		@Override
		public BufferedReader getReader() throws IOException {
			return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
		}

		@Override
		public String getParameter(String name) {
			var values = getParameterValues(name);
			return values != null && values.length > 0 ? values[0] : null;
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			return parameterMap;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			return Collections.enumeration(parameterMap.keySet());
		}

		@Override
		public String[] getParameterValues(String name) {
			return parameterMap.get(name);
		}
	}

	private static Map<String, String[]> parseParameterMap(HttpServletRequest request, CachedBody cachedBody) throws IOException {
		var charsetName = trimToNull(request.getCharacterEncoding());
		var charset = StandardCharsets.UTF_8;
		if (charsetName != null) {
			try {
				charset = Charset.forName(charsetName);
			} catch (Exception ignore) {
				charset = StandardCharsets.UTF_8;
			}
		}

		var accumulator = new LinkedHashMap<String, List<String>>();
		parseParamsInto(accumulator, request.getQueryString(), charset);

		var contentType = trimToNull(request.getContentType());
		if (contentType != null && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
			var bytes = cachedBody.getBytesIfSmall(FORM_PARSE_MAX_BYTES);
			if (bytes != null && bytes.length > 0) {
				parseParamsInto(accumulator, new String(bytes, charset), charset);
			}
		}

		var result = new LinkedHashMap<String, String[]>();
		for (var e : accumulator.entrySet()) {
			var values = e.getValue();
			result.put(e.getKey(), values.toArray(new String[0]));
		}
		return result;
	}

	private static void parseParamsInto(Map<String, List<String>> accumulator, String paramString, java.nio.charset.Charset charset) {
		if (paramString == null || paramString.isBlank()) {
			return;
		}
		for (var part : paramString.split("&")) {
			if (part.isEmpty()) {
				continue;
			}
			var idx = part.indexOf('=');
			var rawKey = idx >= 0 ? part.substring(0, idx) : part;
			var rawValue = idx >= 0 ? part.substring(idx + 1) : "";
			try {
				var key = URLDecoder.decode(rawKey, charset.name());
				if (key == null || key.isBlank()) {
					continue;
				}
				var value = URLDecoder.decode(rawValue, charset.name());
				accumulator.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
			} catch (Exception ignore) {
				// ignore malformed pairs
			}
		}
	}

	private static final class RepeatableServletInputStream extends ServletInputStream {
		private final InputStream delegate;
		private boolean finished = false;

		RepeatableServletInputStream(InputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public int read() throws IOException {
			var r = delegate.read();
			finished = r < 0;
			return r;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			var r = delegate.read(b, off, len);
			finished = r < 0;
			return r;
		}

		@Override
		public boolean isFinished() {
			return finished;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			// synchronous
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}
	}
}
