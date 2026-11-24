/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.startup.Tomcat;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * Actual HTTP requests through Tomcat: a "relay" context forwards to a "target" context with
 * {@link AdminInstanceForwardFilter#forward}, no running engine nor Redis required.
 */
public class AdminInstanceForwardFilterTest {
	private static final long MIB = 1L << 20;
	private static final String TARGET_INSTANCE = "target-1";

	@Rule public TemporaryFolder folder = new TemporaryFolder();

	private static final Map<String, CompletableFuture<String>> outcomes = new ConcurrentHashMap<>();
	private static final AtomicInteger targetHits = new AtomicInteger();
	private static final AtomicLong targetReceived = new AtomicLong();
	private static volatile CountDownLatch targetStarted = new CountDownLatch(0);
	private static volatile long targetStartedAfter = Long.MAX_VALUE;
	private static volatile long maxBytes = -1;
	private static volatile String targetBase;

	private Tomcat tomcat;
	private int port;
	private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(5)).build();

	@Before public void setup() throws Exception {
		Engine.logContext = Engine.logEngine = Logger.getLogger("admin-forward-test");
		EnginePropertiesManager.initProperties();
		outcomes.clear();
		targetHits.set(0);
		targetReceived.set(0);
		targetStarted = new CountDownLatch(0);
		targetStartedAfter = Long.MAX_VALUE;
		maxBytes = -1;

		tomcat = new Tomcat();
		tomcat.setBaseDir(folder.newFolder("tomcat").getAbsolutePath());
		tomcat.setPort(0);
		tomcat.getConnector().setProperty("address", "127.0.0.1");

		var relay = tomcat.addContext("/relay", folder.newFolder("relay").getAbsolutePath());
		Tomcat.addServlet(relay, "relay", new HttpServlet() {
			private static final long serialVersionUID = 1L;
			@Override protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
					javax.servlet.ServletException {
				var query = request.getQueryString();
				var url = targetBase + "/upload" + (query == null ? "" : "?" + query);
				AdminInstanceForwardFilter.forward(request, response, url, targetBase, TARGET_INSTANCE, "relay-1", null, maxBytes);
			}
		});
		relay.addServletMappingDecoded("/*", "relay");

		var target = tomcat.addContext("/target", folder.newFolder("target").getAbsolutePath());
		Tomcat.addServlet(target, "target", new TargetServlet());
		target.addServletMappingDecoded("/*", "target");

		tomcat.start();
		port = tomcat.getConnector().getLocalPort();
		targetBase = "http://127.0.0.1:" + port + "/target";
	}

	@After public void cleanup() throws Exception {
		if (tomcat != null) {
			tomcat.stop();
			tomcat.destroy();
		}
	}

	@Test public void streamsALargeBodyToTheTargetWhileItIsStillUploaded() throws Exception {
		var size = 32 * MIB;
		var before = proxyTempFiles();
		// the client stops in the middle of its upload until the target has received a part of it: a forward that
		// stores the body before contacting the target never gets there
		targetStartedAfter = 4 * MIB;
		targetStarted = new CountDownLatch(1);
		var id = id();
		var response = client.send(post(id, "echo", BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(
				() -> new PatternStream(size, size / 2, targetStarted)), size)).header("Content-Type", "application/octet-stream").build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(response.body(), 200, response.statusCode());
		assertEquals("complete:" + size, outcome(id));
		assertTrue(response.body(), response.body().contains("sha=" + sha(size) + ";"));
		assertTrue(response.body(), response.body().contains("cl=" + size + ";"));
		assertTrue(response.body(), response.body().contains("ct=application/octet-stream;"));
		assertTrue(response.body(), response.body().contains("fwd=relay-1;"));
		assertEquals(TARGET_INSTANCE, response.headers().firstValue("X-C8O-Instance").orElse(null));
		assertEquals(before, proxyTempFiles());
	}

	@Test public void forwardsABodyOfUnknownLengthInChunks() throws Exception {
		var size = 3 * MIB + 17;
		var id = id();
		var response = client.send(post(id, "echo", BodyPublishers.ofInputStream(() -> new PatternStream(size, -1, null))).build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(response.body(), 200, response.statusCode());
		assertEquals("complete:" + size, outcome(id));
		assertTrue(response.body(), response.body().contains("sha=" + sha(size) + ";"));
		assertTrue(response.body(), response.body().contains("te=chunked;"));
	}

	@Test public void forwardsAFormThatTheTargetParses() throws Exception {
		var id = id();
		var response = client.send(post(id, "form", BodyPublishers.ofString("a=1&b=%C3%A9t%C3%A9&a=2"))
				.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8").build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, response.statusCode());
		assertEquals("a=[1, 2];b=été", response.body());
	}

	@Test public void refusesADeclaredLengthOverTheLimitBeforeContactingTheTarget() throws Exception {
		maxBytes = MIB;
		var response = client.send(post(id(), "echo", BodyPublishers.ofByteArray(new byte[(int) MIB + 100])).build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(413, response.statusCode());
		assertTrue(response.body(), response.body().contains("net.upload.admin.max_size"));
		assertEquals(TARGET_INSTANCE, response.headers().firstValue("X-C8O-Instance").orElse(null));
		assertFalse(response.headers().firstValue("X-C8O-Instance-Unreachable").isPresent());
		assertEquals(0, targetHits.get());
	}

	@Test public void countsABodyOfUnknownLengthAndLeavesTheTargetWithAnIncompleteRequest() throws Exception {
		maxBytes = MIB;
		var id = id();
		var size = MIB + MIB / 2;
		var response = client.send(post(id, "echo", BodyPublishers.ofInputStream(() -> new PatternStream(size, -1, null))).build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(413, response.statusCode());
		assertTrue(response.body(), response.body().contains("net.upload.admin.max_size"));
		assertFalse(response.headers().firstValue("X-C8O-Instance-Unreachable").isPresent());
		var outcome = outcome(id);
		assertTrue(outcome, outcome.startsWith("incomplete:"));
		assertTrue(outcome, Long.parseLong(outcome.substring("incomplete:".length())) <= MIB);
	}

	@Test public void aClientThatStopsInAChunkedBodyNeverEndsTheRequestOfTheTarget() throws Exception {
		clientStopsInTheMiddle(true);
	}

	@Test public void aClientThatStopsBeforeItsContentLengthNeverEndsTheRequestOfTheTarget() throws Exception {
		clientStopsInTheMiddle(false);
	}

	@Test public void aTargetThatFailsDuringTheUploadIsReportedUnreachableToTheClient() throws Exception {
		try (var dying = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
			targetBase = "http://127.0.0.1:" + dying.getLocalPort() + "/target";
			var thread = new Thread(() -> {
				try (var socket = dying.accept()) {
					var in = socket.getInputStream();
					readHeaders(in);
					in.readNBytes(256 * 1024);
					socket.setSoLinger(true, 0);
				} catch (IOException ignore) {
					// the test checks the answer of the relay
				}
			});
			thread.start();
			// over the 2 MiB that Tomcat reads by itself after an early answer: the relay reads the rest
			var response = client.send(post(id(), "echo", BodyPublishers.ofByteArray(new byte[(int) (8 * MIB)])).build(),
					HttpResponse.BodyHandlers.ofString());
			thread.join(5_000);
			assertEquals(502, response.statusCode());
			assertEquals(TARGET_INSTANCE, response.headers().firstValue("X-C8O-Instance-Unreachable").orElse(null));
			assertEquals("relay-1", response.headers().firstValue("X-C8O-Instance-Local").orElse(null));
		}
	}

	@Test public void anUnreachableTargetIsReportedToAClientThatSendsALargeBody() throws Exception {
		int closed;
		try (var socket = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
			closed = socket.getLocalPort();
		}
		targetBase = "http://127.0.0.1:" + closed + "/target";
		var response = client.send(post(id(), "echo", BodyPublishers.ofByteArray(new byte[(int) (8 * MIB)])).build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(502, response.statusCode());
		assertEquals(TARGET_INSTANCE, response.headers().firstValue("X-C8O-Instance-Unreachable").orElse(null));
		assertTrue(response.headers().firstValue("X-C8O-Instance-Error").orElse("").length() > 0);
	}

	@Test public void anEarlyRefusalOfTheTargetIsForwarded() throws Exception {
		// the target refuses a file over its own limit while the rest of the request, under its multipart
		// envelope of 1 MiB, still comes: the target reads it and the relay forwards its answer
		var response = client.send(post(id(), "refuse", BodyPublishers.ofByteArray(new byte[(int) MIB])).build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(413, response.statusCode());
		assertEquals("refused by the target", response.body());
		assertFalse(response.headers().firstValue("X-C8O-Instance-Unreachable").isPresent());
	}

	private void clientStopsInTheMiddle(boolean chunked) throws Exception {
		var id = id();
		targetStartedAfter = 256 * 1024;
		targetStarted = new CountDownLatch(1);
		try (var socket = new Socket("127.0.0.1", port)) {
			var out = socket.getOutputStream();
			var headers = "POST /relay/upload?mode=echo&id=" + id + " HTTP/1.1\r\nHost: 127.0.0.1\r\n"
					+ "Content-Type: application/octet-stream\r\n"
					+ (chunked ? "Transfer-Encoding: chunked\r\n" : "Content-Length: " + (4 * MIB) + "\r\n") + "\r\n";
			out.write(headers.getBytes(StandardCharsets.US_ASCII));
			var chunk = new byte[64 * 1024];
			for (int i = 0; i < 16; i++) {
				if (chunked) {
					out.write((Integer.toHexString(chunk.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
					out.write(chunk);
					out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
				} else {
					out.write(chunk);
				}
			}
			out.flush();
			assertTrue("the target received nothing while the client was sending", targetStarted.await(10, TimeUnit.SECONDS));
			// a front end that gives up closes its connection: the relay reads the end of the stream in the middle of
			// the body (a reset without it may only be seen at the read timeout of the connector, as before)
			socket.shutdownOutput();
		}
		var outcome = outcome(id);
		assertTrue(outcome, outcome.startsWith("incomplete:"));
	}

	private HttpRequest.Builder post(String id, String mode, HttpRequest.BodyPublisher body) {
		return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/relay/upload?mode=" + mode + "&id=" + id))
				.timeout(Duration.ofSeconds(30)).POST(body);
	}

	private static String id() {
		var id = UUID.randomUUID().toString();
		outcomes.put(id, new CompletableFuture<>());
		return id;
	}

	private static String outcome(String id) throws Exception {
		return outcomes.get(id).get(10, TimeUnit.SECONDS);
	}

	private static List<Path> proxyTempFiles() throws IOException {
		try (var files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
			return files.filter(f -> f.getFileName().toString().startsWith("c8o-admin-proxy-")).sorted().toList();
		}
	}

	private static void readHeaders(InputStream in) throws IOException {
		int state = 0;
		while (state < 4) {
			int b = in.read();
			if (b < 0) {
				throw new IOException("end of the headers not found");
			}
			state = (b == '\r' && (state == 0 || state == 2)) || (b == '\n' && (state == 1 || state == 3)) ? state + 1 : 0;
		}
	}

	private static byte pattern(long i) {
		return (byte) (i ^ (i >>> 8) ^ (i >>> 16));
	}

	private static String sha(long size) throws Exception {
		var digest = MessageDigest.getInstance("SHA-256");
		try (var in = new PatternStream(size, -1, null)) {
			var buffer = new byte[64 * 1024];
			int r;
			while ((r = in.read(buffer)) >= 0) {
				digest.update(buffer, 0, r);
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	/** A deterministic body, that can wait at a given offset until the target has started to receive it. */
	private static final class PatternStream extends InputStream {
		private final long size;
		private final long pauseAt;
		private final CountDownLatch resume;
		private long position;

		PatternStream(long size, long pauseAt, CountDownLatch resume) {
			this.size = size;
			this.pauseAt = pauseAt;
			this.resume = resume;
		}

		@Override public int read() throws IOException {
			var one = new byte[1];
			return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
		}

		@Override public int read(byte[] b, int off, int len) throws IOException {
			if (position >= size) {
				return -1;
			}
			if (position == pauseAt && resume != null) {
				try {
					if (!resume.await(10, TimeUnit.SECONDS)) {
						throw new IOException("the target received nothing while the client was sending: the body is stored first");
					}
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
			}
			var limit = pauseAt > position ? pauseAt : size;
			var n = (int) Math.min(len, limit - position);
			for (int i = 0; i < n; i++) {
				b[off + i] = pattern(position + i);
			}
			position += n;
			return n;
		}
	}

	private static final class TargetServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		@Override protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
			targetHits.incrementAndGet();
			var query = request.getQueryString();
			var mode = query.replaceAll(".*mode=([a-z]+).*", "$1");
			var id = query.replaceAll(".*id=([0-9a-f-]+).*", "$1");
			var outcome = outcomes.computeIfAbsent(id, k -> new CompletableFuture<>());
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			switch (mode) {
			case "form" -> {
				outcome.complete("form");
				response.getWriter().write("a=" + List.of(request.getParameterValues("a")) + ";b=" + request.getParameter("b"));
			}
			case "refuse" -> {
				request.getInputStream().readNBytes(64 * 1024);
				outcome.complete("refused");
				response.setStatus(413);
				response.getWriter().write("refused by the target");
			}
			default -> {
				long count = 0;
				MessageDigest digest;
				try {
					digest = MessageDigest.getInstance("SHA-256");
				} catch (Exception e) {
					throw new IOException(e);
				}
				try (var in = request.getInputStream()) {
					var buffer = new byte[64 * 1024];
					int r;
					while ((r = in.read(buffer)) >= 0) {
						digest.update(buffer, 0, r);
						count += r;
						if (targetReceived.addAndGet(r) >= targetStartedAfter) {
							targetStarted.countDown();
						}
					}
				} catch (IOException e) {
					outcome.complete("incomplete:" + count);
					throw e;
				}
				outcome.complete("complete:" + count);
				var body = new ByteArrayOutputStream();
				body.writeBytes(("count=" + count + ";sha=" + HexFormat.of().formatHex(digest.digest())
						+ ";te=" + request.getHeader("Transfer-Encoding") + ";cl=" + request.getHeader("Content-Length")
						+ ";ct=" + request.getContentType() + ";fwd=" + request.getHeader("X-C8O-Forwarded-By") + ";")
						.getBytes(StandardCharsets.UTF_8));
				response.getOutputStream().write(body.toByteArray());
			}
			}
		}
	}
}
