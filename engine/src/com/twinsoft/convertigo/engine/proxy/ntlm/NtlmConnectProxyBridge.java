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

package com.twinsoft.convertigo.engine.proxy.ntlm;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ResponseContent;
import org.apache.log4j.Logger;

import com.jivesoftware.authHelper.customescheme.ntlm2.CustomNTLM2Engine;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.ProxyManager;

/** Loopback HTTP/CONNECT proxy for child processes. Upstream credentials stay in Java. */
public class NtlmConnectProxyBridge implements Closeable {
	private static final Logger LOGGER = Logger.getLogger("cems.ProxyManager");
	private static final int MAX_HEADER_SIZE = 64 * 1024;
	private static final int CONNECT_TIMEOUT_MS = 10000;
	private static final byte[] HEADER_DELIMITER = {'\r', '\n', '\r', '\n'};
	private static final Object SHARED_LOCK = new Object();
	private static NtlmConnectProxyBridge sharedBridge;
	private volatile Config config;
	private final ServerSocket serverSocket;
	private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();

	public NtlmConnectProxyBridge(Config config) throws IOException {
		this.config = config;
		InetAddress listen = InetAddress.getByName(config.listenHost);
		if (!listen.isLoopbackAddress()) throw new IOException("The child-process proxy must listen on loopback");
		serverSocket = new ServerSocket(config.listenPort, 50, listen);
	}

	public static String getLocalProxyUrl() throws IOException {
		return getLocalProxyUrl(Config.fromEngineProperties());
	}

	public static String getLocalProxyUrl(Config config) throws IOException {
		synchronized (SHARED_LOCK) {
			if (sharedBridge == null || sharedBridge.serverSocket.isClosed() || !config.sameEndpoint(sharedBridge.config)) {
				closeQuietly(sharedBridge);
				var bridge = new NtlmConnectProxyBridge(config);
				sharedBridge = bridge;
				daemon(() -> {
					try { bridge.serve(); }
					catch (IOException e) { if (!bridge.serverSocket.isClosed()) LOGGER.warn("(LocalProxy) Listener stopped", e); }
				}, "c8o-local-proxy");
			} else {
				sharedBridge.config = config;
			}
			// Keep this port for the engine lifetime: long-lived children retain the URL.
			String host = sharedBridge.serverSocket.getInetAddress().getHostAddress();
			return "http://" + (host.contains(":") ? "[" + host + "]" : host) + ":" + sharedBridge.serverSocket.getLocalPort();
		}
	}

	public static void closeShared() {
		synchronized (SHARED_LOCK) { closeQuietly(sharedBridge); }
	}

	public static void main(String[] args) throws Exception {
		try (var bridge = new NtlmConnectProxyBridge(Config.parse(args))) { bridge.serve(); }
	}

	public void serve() throws IOException {
		while (!serverSocket.isClosed()) {
			Socket client = serverSocket.accept();
			sockets.add(client);
			daemon(() -> handleClient(client), "c8o-local-proxy-request");
		}
	}

	private void handleClient(Socket client) {
		boolean[] committed = {false};
		Socket local = client;
		Path bodyFile = null;
		try {
			local.setSoTimeout(CONNECT_TIMEOUT_MS);
			local.setTcpNoDelay(true);
			HeaderBlock headers = readHeaderBlock(local.getInputStream());
			String[] line = headers.text.split("\r\n", 2)[0].split(" ", 3);
			if (line.length != 3) throw new IOException("Invalid proxy request");
			boolean connect = "CONNECT".equalsIgnoreCase(line[0]);
			URI destination = URI.create(connect ? "https://" + line[1] + "/" : line[1]);
			if (destination.getHost() == null || destination.getUserInfo() != null || destination.getFragment() != null
					|| (!connect && !"http".equalsIgnoreCase(destination.getScheme()))) throw new IOException("Invalid proxy destination");
			URI pacDestination = destination;
			if (connect) {
				// CONNECT exposes only an authority. Match the conventional HTTP/HTTPS
				// schemes and omit their default ports in the URL supplied to the PAC.
				int port = destination.getPort();
				String scheme = port == 80 ? "http" : "https";
				pacDestination = new URI(scheme, null, destination.getHost(), port == 80 || port == 443 ? -1 : port, "/", null, null);
			} else {
				bodyFile = spoolBody(local, headers.headers);
			}
			Config settings = config;
			IOException failure = null;
			for (Proxy route : settings.routes(pacDestination)) {
				try {
					Config credentials = route.type() == Proxy.Type.DIRECT ? null : settings.credentials();
					if (connect) {
						try (Socket upstream = openTunnel(destination, route, credentials)) {
							sockets.add(upstream);
							local.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
							local.getOutputStream().flush();
							committed[0] = true;
							local.setSoTimeout(0);
							try { tunnel(local, upstream); } finally { sockets.remove(upstream); }
						}
					} else {
						forwardHttp(local, line[0], destination, headers.headers, bodyFile, route, credentials, committed);
					}
					return;
				} catch (ConnectException | UnknownHostException | NoRouteToHostException | org.apache.http.conn.ConnectTimeoutException e) {
					if (committed[0]) throw e;
					failure = e; // Try only the next route explicitly returned by the PAC.
				} catch (SocketTimeoutException e) {
					if (!connect || committed[0]) throw e;
					failure = e;
				}
			}
			throw failure == null ? new IOException("No proxy route available") : failure;
		} catch (Exception e) {
			LOGGER.warn("(LocalProxy) Request failed: " + e.getClass().getSimpleName());
			LOGGER.debug("(LocalProxy) Request failure detail", e);
			if (!committed[0]) try { writeError(client.getOutputStream(), 502, "Bad Gateway"); } catch (IOException ignored) { }
		} finally {
			sockets.remove(client); closeQuietly(client);
			if (bodyFile != null) try { Files.deleteIfExists(bodyFile); } catch (IOException e) { LOGGER.warn("(LocalProxy) Cannot remove temporary request body", e); }
		}
	}

	private Socket openTunnel(URI destination, Proxy route, Config credentials) throws IOException {
		Socket socket = new Socket(Proxy.NO_PROXY);
		boolean established = false;
		try {
			InetSocketAddress endpoint = route.type() == Proxy.Type.DIRECT
					? new InetSocketAddress(destination.getHost(), destination.getPort() < 0 ? 443 : destination.getPort())
					: new InetSocketAddress(((InetSocketAddress) route.address()).getHostString(), ((InetSocketAddress) route.address()).getPort());
			socket.connect(endpoint, CONNECT_TIMEOUT_MS);
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(CONNECT_TIMEOUT_MS);
			if (route.type() != Proxy.Type.DIRECT) {
				String authority = destination.getRawAuthority();
				String authorization = credentials.method == ProxyMethod.basic ? "Basic " + Base64.getEncoder().encodeToString(
						(credentials.username + ":" + credentials.password).getBytes(StandardCharsets.ISO_8859_1)) : null;
				CustomNTLM2Engine ntlm = new CustomNTLM2Engine();
				for (int step = 0; step < 3; step++) {
					writeConnect(socket.getOutputStream(), authority, authorization);
					ProxyResponse response = readResponse(socket.getInputStream());
					if (response.statusCode == 200) { established = true; break; }
					if (response.statusCode != 407 || credentials.method != ProxyMethod.ntlm) throw new IOException("Upstream proxy status " + response.statusCode);
					String challenge = extractNtlmChallenge(response);
					if (challenge == null || (authorization != null && challenge.isEmpty())) throw new IOException("Invalid NTLM proxy challenge");
					authorization = "NTLM " + (challenge.isEmpty() ? ntlm.generateType1Msg(credentials.domain, credentials.workstation)
							: ntlm.generateType3Msg(credentials.username, credentials.password, credentials.domain, credentials.workstation, challenge));
				}
				if (!established) throw new IOException("Proxy authentication failed");
			} else { established = true; }
			socket.setSoTimeout(0);
			return socket;
		} finally { if (!established) closeQuietly(socket); }
	}

	private static Path spoolBody(Socket local, Map<String, List<String>> headers) throws IOException {
		Path bodyFile = null;
		try {
			String length = firstHeader(headers, "Content-Length"), encoding = firstHeader(headers, "Transfer-Encoding");
			if (length != null || encoding != null) {
				if (length != null && encoding != null) throw new IOException("Ambiguous request body framing");
				var input = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 8192);
				input.bind(local.getInputStream());
				InputStream body;
				if (encoding != null) {
					if (!"chunked".equalsIgnoreCase(encoding)) throw new IOException("Unsupported body encoding");
					body = new ChunkedInputStream(input);
				} else {
					long count = Long.parseLong(length);
					if (count < 0) throw new IOException("Invalid content length");
					body = new ContentLengthInputStream(input, count);
				}
				if ("100-continue".equalsIgnoreCase(firstHeader(headers, "Expect"))) {
					local.getOutputStream().write("HTTP/1.1 100 Continue\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
					local.getOutputStream().flush();
				}
				// Proxy authentication can replay plain HTTP requests. Spool bodies without
				// unbounded heap buffering, once for all PAC routes. CONNECT stays streamed.
				bodyFile = Files.createTempFile("c8o-proxy-", ".body");
				try (body) { Files.copy(body, bodyFile, StandardCopyOption.REPLACE_EXISTING); }
			}

			return bodyFile;
		} catch (IOException | RuntimeException e) {
			if (bodyFile != null) Files.deleteIfExists(bodyFile);
			throw e;
		}
	}

	private static void forwardHttp(Socket local, String method, URI destination, Map<String, List<String>> headers,
			Path bodyFile, Proxy route, Config credentials, boolean[] committed) throws IOException, HttpException {
		var request = RequestBuilder.create(method).setUri(destination);
		Set<String> excluded = hopHeaders(headers);
		for (var header : headers.entrySet()) if (!excluded.contains(header.getKey().toLowerCase(Locale.ROOT))) {
			for (String value : header.getValue()) request.addHeader(header.getKey(), value);
		}
		if (bodyFile != null) request.setEntity(new FileEntity(bodyFile.toFile()));
		var builder = HttpClients.custom().disableAutomaticRetries().disableRedirectHandling().disableContentCompression()
				.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT_MS).setSocketTimeout(30000).setExpectContinueEnabled(true).build());
		if (route.type() != Proxy.Type.DIRECT) {
			var address = (InetSocketAddress) route.address();
			builder.setProxy(new HttpHost(address.getHostString(), address.getPort()));
			var provider = new BasicCredentialsProvider();
			if (credentials.method == ProxyMethod.basic) provider.setCredentials(new AuthScope(address.getHostString(), address.getPort(), AuthScope.ANY_REALM, "Basic"), new UsernamePasswordCredentials(credentials.username, credentials.password));
			if (credentials.method == ProxyMethod.ntlm) provider.setCredentials(new AuthScope(address.getHostString(), address.getPort(), AuthScope.ANY_REALM, "NTLM"), new NTCredentials(credentials.username, credentials.password, credentials.workstation, credentials.domain));
			builder.setDefaultCredentialsProvider(provider);
		}
		try (var client = builder.build(); var response = client.execute(request.build())) {
			var reply = new BasicHttpResponse(response.getStatusLine());
			Map<String, List<String>> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			for (Header header : response.getAllHeaders()) responseHeaders.computeIfAbsent(header.getName(), key -> new ArrayList<>()).add(header.getValue());
			Set<String> responseExcluded = hopHeaders(responseHeaders);
			for (Header header : response.getAllHeaders()) if (!responseExcluded.contains(header.getName().toLowerCase(Locale.ROOT))) reply.addHeader(header);
			reply.setEntity(response.getEntity());
			if ("HEAD".equalsIgnoreCase(method)) {
				if (response.containsHeader("Content-Length")) reply.setHeader(response.getFirstHeader("Content-Length"));
			} else {
				new ResponseContent().process(reply, new BasicHttpContext());
			}
			reply.setHeader("Connection", "close");
			var output = new DefaultBHttpServerConnection(8192);
			output.bind(local);
			committed[0] = true;
			output.sendResponseHeader(reply);
			output.sendResponseEntity(reply);
			output.flush();
		}
	}

	private static Set<String> hopHeaders(Map<String, List<String>> headers) {
		var result = new HashSet<>(List.of("connection", "proxy-connection", "keep-alive", "proxy-authorization", "proxy-authenticate", "te", "trailer", "transfer-encoding", "upgrade", "content-length", "host", "expect"));
		for (String value : headers.getOrDefault("Connection", List.of())) for (String token : value.split(",")) result.add(token.trim().toLowerCase(Locale.ROOT));
		return result;
	}

	private static void tunnel(Socket client, Socket upstream) throws IOException {
		Thread reader = daemon(() -> pump(upstream, client), "c8o-local-proxy-response");
		pump(client, upstream);
		try { reader.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	private static void pump(Socket source, Socket target) {
		try { source.getInputStream().transferTo(target.getOutputStream()); target.shutdownOutput(); }
		catch (IOException e) { closeQuietly(source); closeQuietly(target); }
	}

	private static Thread daemon(Runnable action, String name) {
		Thread thread = new Thread(action, name); thread.setDaemon(true); thread.start(); return thread;
	}

	private static void closeQuietly(Closeable resource) {
		if (resource != null) try { resource.close(); } catch (IOException ignored) { }
	}

	@Override public void close() throws IOException {
		serverSocket.close();
		for (Socket socket : sockets) closeQuietly(socket);
		synchronized (SHARED_LOCK) { if (sharedBridge == this) sharedBridge = null; }
	}

	private static void writeError(OutputStream output, int code, String text) throws IOException {
		output.write(("HTTP/1.1 " + code + " " + text + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
		output.flush();
	}
	private static void writeConnect(OutputStream output, String authority, String authorization) throws IOException {
		StringBuilder request = new StringBuilder();
		request.append("CONNECT ").append(authority).append(" HTTP/1.1\r\n");
		request.append("Host: ").append(authority).append("\r\n");
		request.append("Proxy-Connection: Keep-Alive\r\n");
		request.append("Connection: Keep-Alive\r\n");
		request.append("User-Agent: Convertigo-NTLM-Bridge/0\r\n");
		if (authorization != null) {
			request.append("Proxy-Authorization: ").append(authorization).append("\r\n");
		}
		request.append("\r\n");
		output.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
		output.flush();
	}

	private static String extractNtlmChallenge(ProxyResponse response) {
		for (String header : response.getHeaders("Proxy-Authenticate")) {
			String trimmed = header.trim();
			if ("NTLM".equalsIgnoreCase(trimmed)) {
				return "";
			}
			if (trimmed.regionMatches(true, 0, "NTLM ", 0, 5)) {
				return trimmed.substring(5).trim();
			}
		}
		return null;
	}

	private static ProxyResponse readResponse(InputStream input) throws IOException {
		HeaderBlock headers = readHeaderBlock(input);
		String[] lines = headers.text.split("\r\n");
		if (lines.length == 0) {
			throw new IOException("Empty response");
		}
		String[] statusLine = lines[0].split(" ", 3);
		if (statusLine.length < 2) {
			throw new IOException("Invalid status line: " + lines[0]);
		}

		int statusCode = Integer.parseInt(statusLine[1]);
		String reasonPhrase = statusLine.length >= 3 ? statusLine[2] : "";
		drainBody(input, headers.headers);
		return new ProxyResponse(statusCode, reasonPhrase, headers.headers);
	}

	private static HeaderBlock readHeaderBlock(InputStream input) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int matched = 0;
		while (buffer.size() < MAX_HEADER_SIZE) {
			int value = input.read();
			if (value < 0) {
				throw new IOException("Stream closed while reading headers");
			}
			buffer.write(value);
			if (value == HEADER_DELIMITER[matched]) {
				matched++;
				if (matched == HEADER_DELIMITER.length) {
					byte[] bytes = buffer.toByteArray();
					String text = new String(bytes, 0, bytes.length - HEADER_DELIMITER.length, StandardCharsets.ISO_8859_1);
					return new HeaderBlock(text, parseHeaders(text));
				}
			} else {
				matched = value == HEADER_DELIMITER[0] ? 1 : 0;
			}
		}
		throw new IOException("Headers too large");
	}

	private static Map<String, List<String>> parseHeaders(String text) {
		Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		String[] lines = text.split("\r\n");
		for (int i = 1; i < lines.length; i++) {
			int separator = lines[i].indexOf(':');
			if (separator <= 0) {
				continue;
			}
			String name = lines[i].substring(0, separator).trim();
			String value = lines[i].substring(separator + 1).trim();
			headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
		}
		return headers;
	}

	private static void drainBody(InputStream input, Map<String, List<String>> headers) throws IOException {
		String transferEncoding = firstHeader(headers, "Transfer-Encoding");
		if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
			drainChunkedBody(input);
			return;
		}

		String contentLength = firstHeader(headers, "Content-Length");
		if (contentLength == null) {
			return;
		}

		int remaining = Integer.parseInt(contentLength);
		byte[] buffer = new byte[4096];
		while (remaining > 0) {
			int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
			if (read < 0) {
				throw new IOException("Stream closed while draining response body");
			}
			remaining -= read;
		}
	}

	private static void drainChunkedBody(InputStream input) throws IOException {
		while (true) {
			String sizeLine = readAsciiLine(input);
			if (sizeLine == null) {
				throw new IOException("Unexpected end of chunked body");
			}
			int separator = sizeLine.indexOf(';');
			String hexSize = separator >= 0 ? sizeLine.substring(0, separator) : sizeLine;
			int chunkSize = Integer.parseInt(hexSize.trim(), 16);
			if (chunkSize == 0) {
				readAsciiLine(input);
				return;
			}
			skipFully(input, chunkSize + 2);
		}
	}

	private static String readAsciiLine(InputStream input) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		while (buffer.size() < MAX_HEADER_SIZE) {
			int value = input.read();
			if (value < 0) {
				return buffer.size() == 0 ? null : new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
			}
			if (value == '\r') {
				int next = input.read();
				if (next != '\n') {
					throw new IOException("Malformed CRLF sequence");
				}
				return new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
			}
			buffer.write(value);
		}
		throw new IOException("Line too large");
	}

	private static void skipFully(InputStream input, int count) throws IOException {
		int remaining = count;
		byte[] buffer = new byte[4096];
		while (remaining > 0) {
			int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
			if (read < 0) {
				throw new IOException("Unexpected end of stream");
			}
			remaining -= read;
		}
	}

	private static String firstHeader(Map<String, List<String>> headers, String name) {
		List<String> values = headers.get(name);
		return values == null || values.isEmpty() ? null : values.get(0);
	}

	private static class ProxyResponse {
		private final int statusCode;
		private final String reasonPhrase;
		private final Map<String, List<String>> headers;

		private ProxyResponse(int statusCode, String reasonPhrase, Map<String, List<String>> headers) {
			this.statusCode = statusCode;
			this.reasonPhrase = reasonPhrase;
			this.headers = headers;
		}

		private List<String> getHeaders(String name) {
			return headers.getOrDefault(name, Collections.emptyList());
		}
	}

	private static class HeaderBlock {
		private final String text;
		private final Map<String, List<String>> headers;

		private HeaderBlock(String text, Map<String, List<String>> headers) {
			this.text = text;
			this.headers = headers;
		}
	}

	public static class Config {
		private String listenHost = "127.0.0.1";
		private int listenPort = 0;
		private String upstreamHost;
		private int upstreamPort;
		private String domain;
		private String username;
		private String password;
		private String workstation = defaultWorkstation();
		private ProxyManager manager;
		private ProxyMethod method = ProxyMethod.ntlm;

		public static Config parse(String[] args) {
			Config config = new Config();
			Map<String, String> values = parseArgs(args);
			config.listenHost = values.getOrDefault("listen-host", config.listenHost);
			config.listenPort = Integer.parseInt(values.getOrDefault("listen-port", Integer.toString(config.listenPort)));
			config.upstreamHost = required(values, "upstream-host");
			config.upstreamPort = Integer.parseInt(required(values, "upstream-port"));
			config.domain = required(values, "domain");
			config.username = required(values, "username");
			config.password = required(values, "password");
			config.workstation = values.getOrDefault("workstation", config.workstation);
			return config;
		}

		public static Config fromEngineProperties() {
			Config config = new Config();
			config.manager = Engine.theApp == null ? null : Engine.theApp.proxyManager;
			if (config.manager == null) {
				config.manager = new ProxyManager();
				config.manager.getEngineProperties();
			}
			return config;
		}

		private Config credentials() {
			if (manager == null) return this;
			Config result = new Config();
			result.method = manager.proxyMethod;
			String user = manager.getProxyUser();
			result.username = user;
			result.password = manager.getProxyPassword();
			result.domain = "";
			if (result.method == ProxyMethod.ntlm) {
				int slash = user.indexOf('\\');
				if (slash < 1 || slash == user.length() - 1) throw new IllegalArgumentException("NTLM proxy authentication requires domain\\user");
				result.domain = user.substring(0, slash);
				result.username = user.substring(slash + 1);
			}
			return result;
		}

		private List<Proxy> routes(URI destination) {
			return manager == null ? List.of(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(upstreamHost, upstreamPort)))
					: manager.selectProxies(destination);
		}

		private static Map<String, String> parseArgs(String[] args) {
			Map<String, String> values = new LinkedHashMap<>();
			for (int i = 0; i < args.length; i++) {
				if (!args[i].startsWith("--")) {
					throw new IllegalArgumentException("Unexpected argument: " + args[i]);
				}
				String key = args[i].substring(2);
				if (i + 1 >= args.length) {
					throw new IllegalArgumentException("Missing value for --" + key);
				}
				values.put(key, args[++i]);
			}
			return values;
		}

		private static String required(Map<String, String> values, String key) {
			String value = values.get(key);
			if (value == null || value.isEmpty()) {
				throw new IllegalArgumentException("Missing --" + key);
			}
			return value;
		}

		private static String defaultWorkstation() {
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {
				return "convertigo";
			}
		}

		private boolean sameEndpoint(Config other) {
			return other != null && listenHost.equals(other.listenHost) && listenPort == other.listenPort
					&& ((manager != null && other.manager != null) || (manager == null && other.manager == null
					&& java.util.Objects.equals(upstreamHost, other.upstreamHost) && upstreamPort == other.upstreamPort));
		}
	}
}
