/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.json;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.engine.json.JsonRowStream.StreamFailure;

/**
 * A grid read in stream (lib_baserow.stream_rows): the page that Baserow announces in next is read, or the stream
 * fails. An HTTP error, an empty or not JSON answer, a page without rows or a connection error used to end the rows
 * silently with the ones read so far (1000 rows of 2000 under load).
 */
public class JsonRowStreamPageFailureTest {
	private HttpServer server;
	private volatile int status;
	private volatile String body;

	@Before
	public void start() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/database/rows/table/1/", exchange -> {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
	}

	@After
	public void stop() {
		server.stop(0);
	}

	private String baseUrl() {
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	/** the first page (one row) given by the sequence, the second one read in stream at baseUrl */
	private String read(String baseUrl) throws Exception {
		List<Object> firstRows = new ArrayList<Object>();
		Map<String, Object> row = new LinkedHashMap<String, Object>();
		row.put("id", 1);
		row.put("Nom", "a");
		firstRows.add(row);
		JsonRowStream stream = new JsonRowStream(new BaserowCellFormat(null, null, null), null, null, null, false, null, null,
				null, null, 1000, firstRows, "1", "https://baserow/api/database/rows/table/1/?page=2&size=1", baseUrl, "JWT x",
				null, null);
		StringBuilder out = new StringBuilder();
		stream.writeArray(out, json -> {});
		return out.toString();
	}

	private void failsOnPage2(int status, String body, String expected) throws Exception {
		this.status = status;
		this.body = body;
		try {
			read(baseUrl());
			fail("HTTP " + status + " " + body + ": the stream must fail instead of ending with the first page");
		} catch (StreamFailure e) {
			assertTrue(e.getMessage(), e.getMessage().contains(expected));
		}
	}

	@Test
	public void aReadPageGivesItsRows() throws Exception {
		status = 200;
		body = "{\"count\":2,\"next\":null,\"previous\":null,\"results\":[{\"id\":2,\"Nom\":\"b\"}]}";
		String rows = read(baseUrl());
		assertTrue(rows, rows.contains("\"a\"") && rows.contains("\"b\""));
		assertTrue(rows, rows.startsWith("[") && rows.endsWith("]"));
	}

	@Test
	public void anEmptyLastPageIsNotAnError() throws Exception {
		status = 200;
		body = "{\"count\":1,\"next\":null,\"previous\":null,\"results\":[]}";
		String rows = read(baseUrl());
		assertTrue(rows, rows.contains("\"a\""));
	}

	@Test
	public void anUnreadPageFailsTheStream() throws Exception {
		failsOnPage2(502, "", "Baserow page 2 could not be read (HTTP 502)");
		failsOnPage2(200, "", "Baserow page 2 could not be read");
		failsOnPage2(500, "{\"detail\":\"boom\"}", "Baserow page 2 could not be read (HTTP 500)");
		failsOnPage2(200, "{\"count\":2,\"next\":null}", "Baserow page 2 could not be read");
		failsOnPage2(200, "[]", "Baserow page 2 could not be read");
		failsOnPage2(502, "<html>Bad Gateway</html>", "Baserow did not return JSON for the page 2");
		// the error of Baserow itself
		failsOnPage2(400, "{\"error\":\"ERROR_INVALID_PAGE\",\"detail\":\"x\"}", "ERROR_INVALID_PAGE");
	}

	@Test
	public void aConnectionErrorFailsTheStream() throws Exception {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
			closedPort = socket.getLocalPort();
		}
		try {
			read("http://127.0.0.1:" + closedPort);
			fail("a connection error must fail the stream");
		} catch (StreamFailure e) {
			assertTrue(e.getMessage(), e.getMessage().startsWith("Baserow page 2 could not be read"));
		}
	}
}
