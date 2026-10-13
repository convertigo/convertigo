/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.couchdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.Context;

public class DesignDocumentReduceTest {
	private static final String OLD = "function (keys, values, rereduce) {\r\n\ttry {\r\n\t\treturn values.length;\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}";

	@Test
	public void countHandlesInitialAndPartialResults() {
		try (Context context = Context.enter()) {
			var scope = context.initStandardObjects();
			context.evaluateString(scope, "function sum(values) { var result = 0; for (var i = 0; i < values.length; i++) result += values[i]; return result; }"
					+ "var reduce = (" + DesignDocument.DEFAULT_REDUCE + ");", "count reduce", 1, null);
			assertEquals(0.0, Context.toNumber(context.evaluateString(scope, "reduce([], [], false)", "empty", 1, null)), 0.0);
			assertEquals(3.0, Context.toNumber(context.evaluateString(scope, "reduce([], [100, 200, 300], false)", "values", 1, null)), 0.0);
			assertEquals(6000.0, Context.toNumber(context.evaluateString(scope, "reduce(null, [1000, 2000, 3000], true)", "partial counts", 1, null)), 0.0);
			assertEquals(6000.0, Context.toNumber(context.evaluateString(scope,
					"reduce(null, [reduce(null, [1000, 2000], true), 3000], true)", "nested partial counts", 1, null)), 0.0);
		}
	}

	@Test
	public void couchDbCombinesCountsWithAndWithoutGrouping() throws Exception {
		String url = System.getProperty("couchDbReduceTestUrl");
		assumeNotNull(url);
		String database = "c8o_reduce_1223_" + UUID.randomUUID().toString().replace("-", "");
		try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
			request(client, url, "PUT", database, "{}", 201);
			try {
				var views = new JSONObject();
				String map = "function(doc) { if (doc.group) emit(doc.group, 1); }";
				views.put("correct", new JSONObject().put("map", map).put("reduce", DesignDocument.DEFAULT_REDUCE));
				views.put("legacy", new JSONObject().put("map", map).put("reduce", OLD));
				views.put("count", new JSONObject().put("map", map).put("reduce", "_count"));
				request(client, url, "PUT", database + "/_design/test", new JSONObject().put("views", views).toString(), 201);
				var docs = new JSONArray();
				for (int i = 0; i < 6000; i++) {
					docs.put(new JSONObject().put("_id", "doc" + i).put("group", i < 3000 ? "a" : "b"));
				}
				request(client, url, "POST", database + "/_bulk_docs", new JSONObject().put("docs", docs).toString(), 201);
				for (String name : List.of("correct", "count")) {
					var result = new JSONObject(request(client, url, "GET", database + "/_design/test/_view/" + name, null, 200));
					assertEquals(6000, result.getJSONArray("rows").getJSONObject(0).getInt("value"));
					var grouped = new JSONObject(request(client, url, "GET", database + "/_design/test/_view/" + name + "?group=true", null, 200));
					assertEquals(2, grouped.getJSONArray("rows").length());
					for (int i = 0; i < 2; i++) {
						assertEquals(3000, grouped.getJSONArray("rows").getJSONObject(i).getInt("value"));
					}
				}
				var legacy = new JSONObject(request(client, url, "GET", database + "/_design/test/_view/legacy", null, 200));
				assertTrue(legacy.getJSONArray("rows").getJSONObject(0).getInt("value") < 6000);
				var rows = new JSONObject(request(client, url, "GET", database + "/_design/test/_view/correct?reduce=false", null, 200));
				assertEquals(6000, rows.getJSONArray("rows").length());
			} finally {
				request(client, url, "DELETE", database, null, 200);
			}
		}
	}

	// Credentials belong only to the disposable integration-test container.
	private String request(HttpClient client, String root, String method, String path, String json, int expected) throws Exception {
		var request = HttpRequest.newBuilder(URI.create(root + "/" + path)).timeout(Duration.ofSeconds(45))
				.header("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:issue-1223-test".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
				.header("Content-Type", "application/json")
				.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json)).build();
		var response = client.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(response.body(), expected, response.statusCode());
		return response.body();
	}

	@Test
	public void recognizesOnlyTheOldTemplateIgnoringFormatting() {
		assertTrue(DesignDocument.isLegacyReduce(OLD));
		assertTrue(DesignDocument.isLegacyReduce(OLD.replace("\r\n", "\n").replace("\t", "    ")));
		assertTrue(DesignDocument.isLegacyReduce(" \n" + OLD + "\n "));
		for (String reduce : new String[] {null, "", "_count", "_sum", "_stats", DesignDocument.DEFAULT_REDUCE,
				OLD.replace("values.length", "sum(values)"), OLD.replace("try {", "try { /* customized */"),
				OLD.replace("function (", "function count(")}) {
			assertFalse(DesignDocument.isLegacyReduce(reduce));
		}
	}

	@Test
	public void listsOnlyAffectedViewsWithoutChangingTheDocument() throws Exception {
		var document = new DesignDocument();
		var json = new JSONObject("{views:{z:{reduce:'_count'},a:{map:'function(doc) {}'},invalid:42}}");
		var views = json.getJSONObject("views");
		views.put("old", new JSONObject().put("reduce", OLD));
		views.put("other", new JSONObject().put("reduce", OLD.replace("\r\n", "\n")));
		views.put("corrected", new JSONObject().put("reduce", DesignDocument.DEFAULT_REDUCE));
		document.setJSONObject(json);
		var before = json.toString();
		assertEquals(List.of("old", "other"), document.getLegacyReduceViews());
		assertEquals(before, document.getJSONObject().toString());
		views.getJSONObject("old").put("reduce", DesignDocument.DEFAULT_REDUCE);
		assertEquals(List.of("other"), document.getLegacyReduceViews());
		views.remove("other");
		assertTrue(document.getLegacyReduceViews().isEmpty());
	}

	@Test
	public void toleratesEmptyOrMalformedViewData() throws Exception {
		var document = new DesignDocument();
		assertTrue(document.getLegacyReduceViews().isEmpty());
		for (String data : new String[] {"{}", "{views:[]}", "{views:{missing:{},nullReduce:{reduce:null},numericReduce:{reduce:42}}}"}) {
			document.setJSONObject(new JSONObject(data));
			assertTrue(document.getLegacyReduceViews().isEmpty());
		}
		document.setJSONObject(null);
		assertTrue(document.getLegacyReduceViews().isEmpty());
	}
}
