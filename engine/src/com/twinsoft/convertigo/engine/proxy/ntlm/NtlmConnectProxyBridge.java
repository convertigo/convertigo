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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.jivesoftware.authHelper.customescheme.ntlm2.CustomNTLM2Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * Standalone proof-of-concept bridge for CONNECT-only child-process traffic.
 * It terminates an anonymous local HTTP proxy and authenticates upstream
 * against an NTLM proxy using the libs already shipped by Convertigo engine.
 */
public class NtlmConnectProxyBridge implements Closeable {

	private static final Logger LOGGER = Logger.getLogger("cems.ProxyManager");
	private static final int MAX_HEADER_SIZE = 64 * 1024;
	private static final byte[] HEADER_DELIMITER = new byte[] { '\r', '\n', '\r', '\n' };
	private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000L;
	private static final Object SHARED_LOCK = new Object();

	private static NtlmConnectProxyBridge sharedBridge;
	private static Config sharedConfig;

	private final Config config;
	private final ServerSocket serverSocket;
	private volatile long lastAccess = System.currentTimeMillis();

	public NtlmConnectProxyBridge(Config config) throws IOException {
		this.config = config;
		this.serverSocket = new ServerSocket(config.listenPort, 50, InetAddress.getByName(config.listenHost));
		startIdleWatcher();
	}

	public static String getLocalProxyUrl() throws IOException {
		return getLocalProxyUrl(Config.fromEngineProperties());
	}

	public static String getLocalProxyUrl(Config config) throws IOException {
		synchronized (SHARED_LOCK) {
			if (sharedBridge == null || !config.sameEndpoint(sharedConfig) || sharedBridge.serverSocket.isClosed()) {
				LOGGER.info("(NtlmConnectProxyBridge) Start local bridge for upstream " + config.upstreamHost + ":" + config.upstreamPort
					+ " as " + config.domain + "\\" + config.username);
				closeQuietly(sharedBridge);
				sharedBridge = new NtlmConnectProxyBridge(config);
				sharedConfig = config;
				Thread thread = new Thread(() -> {
					try {
						sharedBridge.serve();
					} catch (IOException e) {
						if (!sharedBridge.serverSocket.isClosed()) {
							LOGGER.warn("(NtlmConnectProxyBridge) Bridge server stopped unexpectedly", e);
						}
					}
				}, "c8o-ntlm-bridge");
				thread.setDaemon(true);
				thread.start();
			}
			sharedBridge.touch();
			String proxyUrl = "http://" + config.listenHost + ":" + sharedBridge.serverSocket.getLocalPort();
			LOGGER.debug("(NtlmConnectProxyBridge) Use local proxy " + proxyUrl);
			return proxyUrl;
		}
	}

	public static void main(String[] args) throws Exception {
		Config config;
		try {
			config = Config.parse(args);
		} catch (IllegalArgumentException e) {
			LOGGER.error("(NtlmConnectProxyBridge) " + e.getMessage());
			LOGGER.error("(NtlmConnectProxyBridge) Usage: --upstream-host <host> --upstream-port <port> --domain <domain> --username <user> --password <password> [--listen-host 127.0.0.1] [--listen-port 19128] [--workstation name]");
			return;
		}
		NtlmConnectProxyBridge bridge = new NtlmConnectProxyBridge(config);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> closeQuietly(bridge), "c8o-ntlm-bridge-shutdown"));
		LOGGER.info("(NtlmConnectProxyBridge) Listening on http://" + config.listenHost + ":" + config.listenPort
			+ " -> " + config.upstreamHost + ":" + config.upstreamPort + " as " + config.domain + "\\" + config.username
			+ " (workstation=" + config.workstation + ")");
		bridge.serve();
	}

	public void serve() throws IOException {
		while (!serverSocket.isClosed()) {
			Socket client = serverSocket.accept();
			touch();
			Thread thread = new Thread(() -> handleClient(client), "c8o-ntlm-bridge-" + client.getPort());
			thread.setDaemon(true);
			thread.start();
		}
	}

	private void handleClient(Socket client) {
		try (Socket local = client) {
			local.setTcpNoDelay(true);
			OutputStream clientOutput = local.getOutputStream();
			HttpRequest request = null;
			try {
				request = readRequest(local.getInputStream());
				if (!"CONNECT".equalsIgnoreCase(request.method)) {
					LOGGER.debug("(NtlmConnectProxyBridge) ACCESS " + request.method + " " + request.authority + " -> 501 CONNECT only");
					LOGGER.debug("(NtlmConnectProxyBridge) Reject non-CONNECT request: " + request.method + " " + request.authority);
					writeError(clientOutput, 501, "CONNECT only");
					return;
				}

				try (Socket upstream = openUpstreamTunnel(request.authority)) {
					LOGGER.debug("(NtlmConnectProxyBridge) ACCESS CONNECT " + request.authority + " -> 200 tunnel");
					clientOutput.write(("HTTP/1.1 200 Connection Established\r\n" +
						"Proxy-Agent: Convertigo-NTLM-Bridge\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
					clientOutput.flush();
					tunnel(local, upstream);
				}
			} catch (Exception e) {
				if (request != null) {
					LOGGER.debug("(NtlmConnectProxyBridge) ACCESS " + request.method + " " + request.authority + " -> 502 " + e.getClass().getSimpleName());
				}
				LOGGER.warn("(NtlmConnectProxyBridge) Bridge error: " + e.getMessage(), e);
				writeError(clientOutput, 502, "Bad Gateway");
			}
		} catch (Exception ignored) {
		}
	}

	private Socket openUpstreamTunnel(String authority) throws IOException {
		touch();
		Socket upstream = new Socket(config.upstreamHost, config.upstreamPort);
		upstream.setTcpNoDelay(true);
		upstream.setSoTimeout(10000);

		InputStream input = new BufferedInputStream(upstream.getInputStream());
		OutputStream output = upstream.getOutputStream();
		CustomNTLM2Engine ntlm = new CustomNTLM2Engine();
		LOGGER.debug("(NtlmConnectProxyBridge) CONNECT " + authority + " via upstream " + config.upstreamHost + ":" + config.upstreamPort);

		String authorization = null;
		for (int step = 0; step < 3; step++) {
			writeConnect(output, authority, authorization);
			HttpResponse response = readResponse(input);
			if (response.statusCode == 200) {
				upstream.setSoTimeout(0);
				LOGGER.debug("(NtlmConnectProxyBridge) CONNECT " + authority + " established");
				return upstream;
			}
			if (response.statusCode != 407) {
				throw new IOException("Unexpected upstream proxy status: " + response.statusCode + " " + response.reasonPhrase);
			}
			LOGGER.debug("(NtlmConnectProxyBridge) Upstream 407 for " + authority);

			String challenge = extractNtlmChallenge(response);
			if (challenge == null) {
				throw new IOException("Proxy requested authentication without NTLM challenge");
			}

			if (authorization == null) {
				if (challenge.isEmpty()) {
					LOGGER.debug("(NtlmConnectProxyBridge) Send NTLM type1 for " + authority);
					authorization = "NTLM " + ntlm.generateType1Msg(config.domain, config.workstation);
				} else {
					LOGGER.debug("(NtlmConnectProxyBridge) Upstream sent direct NTLM type2 for " + authority);
					authorization = "NTLM " + ntlm.generateType3Msg(
						config.username, config.password, config.domain, config.workstation, challenge);
				}
				continue;
			}

			if (challenge.isEmpty()) {
				throw new IOException("Proxy did not send NTLM type2 challenge");
			}

			LOGGER.debug("(NtlmConnectProxyBridge) Send NTLM type3 for " + authority);
			authorization = "NTLM " + ntlm.generateType3Msg(
				config.username, config.password, config.domain, config.workstation, challenge);
		}

		throw new IOException("NTLM handshake did not establish a tunnel");
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

	private static String extractNtlmChallenge(HttpResponse response) {
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

	private static HttpRequest readRequest(InputStream input) throws IOException {
		HeaderBlock headers = readHeaderBlock(input);
		String[] lines = headers.text.split("\r\n");
		if (lines.length == 0) {
			throw new IOException("Empty request");
		}
		String[] requestLine = lines[0].split(" ", 3);
		if (requestLine.length < 2) {
			throw new IOException("Invalid request line: " + lines[0]);
		}
		return new HttpRequest(requestLine[0], requestLine[1], headers.headers);
	}

	private static HttpResponse readResponse(InputStream input) throws IOException {
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
		return new HttpResponse(statusCode, reasonPhrase, headers.headers);
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

	private void touch() {
		lastAccess = System.currentTimeMillis();
	}

	private void startIdleWatcher() {
		Thread thread = new Thread(() -> {
			while (!serverSocket.isClosed()) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				if (System.currentTimeMillis() - lastAccess > IDLE_TIMEOUT_MS) {
					LOGGER.info("(NtlmConnectProxyBridge) Stop idle local bridge");
					closeQuietly(this);
					return;
				}
			}
		}, "c8o-ntlm-bridge-idle");
		thread.setDaemon(true);
		thread.start();
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
		while (true) {
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

	private static void tunnel(Socket client, Socket upstream) throws IOException {
		Thread clientToUpstream = startPump(client.getInputStream(), upstream.getOutputStream());
		Thread upstreamToClient = startPump(upstream.getInputStream(), client.getOutputStream());
		try {
			clientToUpstream.join();
			upstream.shutdownOutput();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		try {
			upstreamToClient.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static Thread startPump(InputStream input, OutputStream output) {
		Thread thread = new Thread(() -> pump(input, output), "c8o-ntlm-bridge-pump");
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	private static void pump(InputStream input, OutputStream output) {
		byte[] buffer = new byte[16 * 1024];
		try {
			while (true) {
				int read = input.read(buffer);
				if (read < 0) {
					return;
				}
				output.write(buffer, 0, read);
				output.flush();
			}
		} catch (SocketTimeoutException e) {
			// ignore, the owning socket lifecycle will close the tunnel
		} catch (IOException e) {
			// ignore, the peer closed the tunnel
		}
	}

	private static void writeError(OutputStream output, int statusCode, String message) throws IOException {
		String body = message + "\n";
		String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n" +
			"Content-Type: text/plain; charset=utf-8\r\n" +
			"Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
			"Connection: close\r\n\r\n" + body;
		output.write(response.getBytes(StandardCharsets.UTF_8));
		output.flush();
	}

	private static void closeQuietly(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void close() throws IOException {
		serverSocket.close();
		LOGGER.info("(NtlmConnectProxyBridge) Bridge closed");
		synchronized (SHARED_LOCK) {
			if (sharedBridge == this) {
				sharedBridge = null;
				sharedConfig = null;
			}
		}
	}

	private static class HttpRequest {
		private final String method;
		private final String authority;
		@SuppressWarnings("unused")
		private final Map<String, List<String>> headers;

		private HttpRequest(String method, String authority, Map<String, List<String>> headers) {
			this.method = method;
			this.authority = authority;
			this.headers = headers;
		}
	}

	private static class HttpResponse {
		private final int statusCode;
		private final String reasonPhrase;
		private final Map<String, List<String>> headers;

		private HttpResponse(int statusCode, String reasonPhrase, Map<String, List<String>> headers) {
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
			config.upstreamHost = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_HOST);
			config.upstreamPort = Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_PORT));
			String proxyUser = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_USER);
			int separator = proxyUser.indexOf('\\');
			if (separator < 1 || separator == proxyUser.length() - 1) {
				throw new IllegalArgumentException("Wrong username, please indicate the domain name for ntlm authentication. (eg: domain\\\\user)");
			}
			config.domain = proxyUser.substring(0, separator);
			config.username = proxyUser.substring(separator + 1);
			config.password = EnginePropertiesManager.getProperty(PropertyName.PROXY_SETTINGS_PASSWORD);
			return config;
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
			return other != null
				&& listenHost.equals(other.listenHost)
				&& upstreamHost.equals(other.upstreamHost)
				&& upstreamPort == other.upstreamPort
				&& domain.equals(other.domain)
				&& username.equals(other.username)
				&& password.equals(other.password)
				&& workstation.equals(other.workstation);
		}
	}
}
