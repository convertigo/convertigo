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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.RedisHealth;
import com.twinsoft.convertigo.engine.sessions.RedisInstanceDiscovery;
import com.twinsoft.convertigo.engine.util.AdminUploadLimits;

/**
 * For clustered admin usage (Redis session store), allows sticking an admin request to a specific
 * instance without relying on load balancer stickiness.
 * <p>
 * Target instance is selected via header {@code X-C8O-Instance} (preferred) or query param
 * {@code __instance}. Requests are proxied to the target instance base URL registered in Redis.
 * <p>
 * The forward is internal traffic between instances: it never goes through the engine HTTP proxy settings.
 * When it fails, the response is an explicit error (HTTP 502 with the {@code X-C8O-Instance-Unreachable}
 * header) so that the console can tell it, instead of a silent answer from the local instance.
 * <p>
 * The request body is streamed to the target as it arrives: it is never stored on the instance that forwards it.
 * A request body over the optional administration upload limit is refused with HTTP 413, as the target would do:
 * from its Content-Length before contacting the target, or while it is counted when its length is unknown. Without
 * this limit (default), the body is only limited by the frontends and by the target instance.
 */
public class AdminInstanceForwardFilter implements Filter {
	private static final String HEADER_INSTANCE = "X-C8O-Instance";
	private static final String PARAM_INSTANCE = "__instance";
	private static final String HEADER_FORWARDED_BY = "X-C8O-Forwarded-By";
	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String HEADER_INSTANCE_UNREACHABLE = "X-C8O-Instance-Unreachable";
	private static final String HEADER_INSTANCE_LOCAL = "X-C8O-Instance-Local";
	private static final String HEADER_INSTANCE_ERROR = "X-C8O-Instance-Error";
	private static final long UNREACHABLE_WARN_INTERVAL_MS = 60_000L;
	private static final Map<String, Long> lastUnreachableWarn = new ConcurrentHashMap<>();

	private static final int CONNECT_TIMEOUT_MS = 5_000;
	private static final int READ_TIMEOUT_MS = 0; // keep long polling & log streaming working
	private static final int BODY_BUFFER_SIZE = 16 * 1024;
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

		if (!Engine.authenticatedSessionManager.isAuthenticated(request.getSession(false))) {
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
			respondUnreachable(response, requestedInstance, localInstance, baseUrl,
					new IOException("Target instance is not registered in Redis (discovery entry expired or unavailable)"));
			return;
		}

		var targetUrl = buildTargetUrl(baseUrl, request);
		if (targetUrl == null) {
			respondUnreachable(response, requestedInstance, localInstance, baseUrl,
					new IOException("Target instance has an invalid administration URL"));
			return;
		}

		forward(request, response, targetUrl, baseUrl, requestedInstance, localInstance, forwardedBy, AdminUploadLimits.requestBytes());
	}

	/**
	 * Forwards the request to the target instance, its body streamed as it arrives.
	 *
	 * @param maxBytes the limit of the request body, or -1 without limit
	 */
	static void forward(HttpServletRequest request, HttpServletResponse response, String targetUrl, String baseUrl,
			String requestedInstance, String localInstance, String forwardedBy, long maxBytes) throws IOException, ServletException {
		var hasBody = hasRequestBody(request.getMethod());
		var declared = hasBody ? request.getContentLengthLong() : -1;
		if (maxBytes > 0 && declared > maxBytes) {
			// refused before contacting the target: nothing of the body is read
			respondTooLarge(request, response, requestedInstance, localInstance, declared, maxBytes);
			return;
		}
		ForwardedBody body = null;
		try {
			if (hasBody) {
				body = new ForwardedBody(request.getInputStream(), maxBytes);
			}
			response.setHeader(HEADER_INSTANCE, requestedInstance);
			proxy(request, body, response, targetUrl, localInstance, forwardedBy);
		} catch (BodyTooLargeException e) {
			// a body of unknown length counted over the limit: the target got an incomplete request and dropped it
			respondTooLarge(request, response, requestedInstance, localInstance, e.actual, e.permitted);
		} catch (ClientBodyException e) {
			// the client stopped sending its body (abort, timeout): the target got an incomplete request and dropped it
			if (response.isCommitted()) {
				throw e;
			}
			respondNotForwarded(request, response, requestedInstance, localInstance, HttpServletResponse.SC_BAD_REQUEST,
					"The request body could not be read until its end (" + e.getCause() + ")");
		} catch (Exception e) {
			if (response.isCommitted()) {
				throw e;
			}
			if (body != null) {
				// read the rest of the body, without storing it, so that the client receives the answer
				body.drain();
			}
			respondUnreachable(response, requestedInstance, localInstance, baseUrl, e);
		}
	}

	private static void respondTooLarge(HttpServletRequest request, HttpServletResponse response, String requestedInstance,
			String localInstance, long actual, long permitted) throws IOException, ServletException {
		respondNotForwarded(request, response, requestedInstance, localInstance, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
				"The request (" + actual + " bytes) exceeds the administration upload limit (" + permitted
				+ " bytes with its multipart envelope, engine property " + PropertyName.ADMIN_UPLOAD_MAX_SIZE.getKey() + ")");
	}

	private static void respondUnreachable(HttpServletResponse response, String requestedInstance, String localInstance,
			String baseUrl, Exception e) throws IOException {
		var cause = RedisHealth.describe(e);
		long now = System.currentTimeMillis();
		var last = lastUnreachableWarn.get(requestedInstance);
		if (last == null || now - last >= UNREACHABLE_WARN_INTERVAL_MS) {
			lastUnreachableWarn.put(requestedInstance, now);
			Engine.logEngine.warn("(AdminInstanceForwardFilter) Instance " + requestedInstance + " (" + baseUrl
					+ ") is unreachable from " + localInstance + ": " + cause);
		}
		if (Engine.logEngine.isDebugEnabled()) {
			Engine.logEngine.debug("(AdminInstanceForwardFilter) Forward to " + requestedInstance + " failed", e);
		}
		response.reset();
		response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
		response.setHeader(HEADER_INSTANCE, requestedInstance);
		response.setHeader(HEADER_INSTANCE_UNREACHABLE, requestedInstance);
		// printable ASCII only: header values cannot carry any character
		var headerCause = cause.replaceAll("[^\\x20-\\x7E]", " ");
		response.setHeader(HEADER_INSTANCE_ERROR, headerCause.length() > 300 ? headerCause.substring(0, 300) : headerCause);
		if (localInstance != null && !localInstance.isBlank()) {
			response.setHeader(HEADER_INSTANCE_LOCAL, localInstance);
		}
		response.setHeader("Cache-Control", "no-store");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		var body = new org.codehaus.jettison.json.JSONObject();
		try {
			body.put("error", "Instance " + requestedInstance + " is unreachable from " + localInstance);
			body.put("instance", requestedInstance);
			body.put("localInstance", localInstance);
			body.put("cause", cause);
		} catch (Exception ignore) {
			// the headers already carry the information
		}
		response.getWriter().write(body.toString());
	}

	/**
	 * The request body was refused or could not be read here: the target is not reported unreachable (a size
	 * problem or a client that stops sending never answers 502).
	 */
	private static void respondNotForwarded(HttpServletRequest request, HttpServletResponse response, String requestedInstance,
			String localInstance, int status, String message) throws IOException, ServletException {
		Engine.logEngine.warn("(AdminInstanceForwardFilter) Request to instance " + requestedInstance + " not forwarded by "
				+ localInstance + ": " + message);
		response.reset();
		// the selected instance stays the target of the console
		response.setHeader(HEADER_INSTANCE, requestedInstance);
		response.setHeader("Cache-Control", "no-store");
		try {
			ServiceUtils.handleError(message, status, request, response);
		} catch (ServiceException failure) {
			throw new ServletException(failure);
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

	private static void proxy(HttpServletRequest request, ForwardedBody body, HttpServletResponse response, String targetUrl,
			String localInstance, String forwardedBy) throws IOException {
		HttpURLConnection conn = null;
		try {
			var uri = URI.create(targetUrl);
			var scheme = uri.getScheme();
			if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
				throw new IOException("Invalid target URL scheme");
			}
			var url = uri.toURL();
			// instance to instance traffic stays inside the cluster: never through the engine HTTP proxy
			conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
			applyTrustAllSslIfRequested(conn);
			conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);

			var method = request.getMethod();
			conn.setRequestMethod(method);
			conn.setDoInput(true);

			copyRequestHeaders(request, conn, localInstance, forwardedBy);

			if (body != null) {
				conn.setDoOutput(true);
				var contentLength = request.getContentLengthLong();
				if (contentLength >= 0) {
					conn.setFixedLengthStreamingMode(contentLength);
				} else {
					conn.setChunkedStreamingMode(BODY_BUFFER_SIZE);
				}
				var out = conn.getOutputStream();
				try {
					copy(body, out);
				} catch (ClientBodyException | BodyTooLargeException e) {
					// never close the stream here, which would end a chunked body: the target must receive an
					// incomplete request, not a shorter one that looks complete
					conn.disconnect();
					throw e;
				}
				out.close();
			}

			var status = conn.getResponseCode();
			if (status == 407 || status == 502 || status == 503 || status == 504) {
				throw new IOException("Proxy target responded with HTTP " + status);
			}
			response.setStatus(status);
			copyResponseHeaders(conn, response);

			if (!"HEAD".equalsIgnoreCase(method)) {
				var in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
				if (in != null) {
					try (InputStream answer = in; OutputStream out = response.getOutputStream()) {
						copy(answer, out);
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

	/** A request body over the optional administration upload limit: not an unreachable instance. */
	private static final class BodyTooLargeException extends IOException {
		private static final long serialVersionUID = 1L;
		private final long actual;
		private final long permitted;

		BodyTooLargeException(long actual, long permitted) {
			super("Request body too large (" + actual + " bytes, limit " + permitted + " bytes)");
			this.actual = actual;
			this.permitted = permitted;
		}
	}

	/** The client stopped sending the request body (abort, timeout): not an unreachable instance. */
	private static final class ClientBodyException extends IOException {
		private static final long serialVersionUID = 1L;

		ClientBodyException(IOException cause) {
			super("Unable to read the request body: " + cause, cause);
		}
	}

	/**
	 * The request body, read as it is forwarded: counted against the optional limit, and its read errors told apart
	 * from those of the target instance. Nothing is kept.
	 */
	private static final class ForwardedBody extends InputStream {
		private final InputStream in;
		private final long maxBytes;
		private long count;

		/** @param maxBytes the limit of the body, or -1 without limit */
		ForwardedBody(InputStream in, long maxBytes) {
			this.in = in;
			this.maxBytes = maxBytes;
		}

		@Override
		public int read() throws IOException {
			var one = new byte[1];
			var r = read(one, 0, 1);
			return r < 0 ? -1 : one[0] & 0xFF;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int r;
			try {
				r = in.read(b, off, len);
			} catch (IOException e) {
				throw new ClientBodyException(e);
			}
			if (r > 0) {
				count += r;
				if (maxBytes > 0 && count > maxBytes) {
					throw new BodyTooLargeException(count, maxBytes);
				}
			}
			return r;
		}

		/** Reads and discards the rest of the body, until its end, the limit or a read error. */
		void drain() {
			var buffer = new byte[BODY_BUFFER_SIZE];
			try {
				while (read(buffer, 0, buffer.length) >= 0) {
					// discard
				}
			} catch (IOException ignore) {
				// the answer is sent anyway
			}
		}
	}
}
