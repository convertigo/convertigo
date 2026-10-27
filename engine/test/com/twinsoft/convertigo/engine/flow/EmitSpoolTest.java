/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.twinsoft.convertigo.engine.flow.rows.EmitSpool;
import com.twinsoft.convertigo.engine.flow.rows.ResultDocument;
import com.twinsoft.convertigo.engine.flow.rows.RowStreamException;
import com.twinsoft.convertigo.engine.util.JsonValues;

public class EmitSpoolTest {
	@org.junit.Before public void logs() { com.twinsoft.convertigo.engine.Engine.logBeans = org.apache.log4j.Logger.getLogger("emit-test"); }

	private static ResultDocument document(EmitSpool spool) throws Exception {
		return new ResultDocument(spool, "data", JsonValues.object("colDef", null, "count", 3),
				new ResultDocument.Limits(1_000_000, 256L * 1024 * 1024, 256 * 1024, 2, 512 * 1024));
	}

	private static String[] spoolFiles() {
		var names = new File(System.getProperty("java.io.tmpdir")).list((dir, name) -> name.startsWith("c8o-emit-") && name.endsWith(".jsonl"));
		return names == null ? new String[0] : names;
	}

	@Test public void emittedRowsAreWrittenInOrderBeforeTheOtherKeys() throws Exception {
		var spool = new EmitSpool(100, 1_000_000);
		spool.append(List.of(JsonValues.object("id", 1, "text", "é<&/"), JsonValues.object("id", 2)), null, null);
		spool.append(JsonValues.object("id", 3), null, null);
		assertEquals(3, spool.rows());
		var out = new StringWriter();
		try (var doc = document(spool)) { doc.writeTo(out); }
		var text = out.toString();
		var json = new JSONObject(text);
		assertEquals(3, json.getJSONArray("data").length());
		assertEquals("é<&/", json.getJSONArray("data").getJSONObject(0).getString("text"));
		assertEquals(3, json.getJSONArray("data").getJSONObject(2).getInt("id"));
		assertTrue(json.getBoolean("complete"));
		assertTrue("the emitted field comes first: " + text, text.indexOf("\"data\"") < text.indexOf("\"colDef\""));
	}

	@Test public void largeEmissionsSpillToATemporaryFileDeletedOnClose() throws Exception {
		var before = spoolFiles().length;
		var spool = new EmitSpool(1_000_000, 256L * 1024 * 1024);
		var filler = "x".repeat(1000);
		int count = 6000;
		for (int i = 0; i < count; i += 100) {
			var batch = new ArrayList<Object>();
			for (int j = i; j < i + 100; j++) batch.add(JsonValues.object("id", j, "filler", filler));
			spool.append(batch, null, null);
		}
		assertEquals("more than the memory part went to a file", before + 1, spoolFiles().length);
		var out = new StringWriter();
		try (var doc = document(spool)) { doc.writeTo(out); }
		var rows = new JSONObject(out.toString()).getJSONArray("data");
		assertEquals(count, rows.length());
		for (int i = 0; i < count; i += 997) assertEquals(i, rows.getJSONObject(i).getInt("id"));
		assertEquals(count - 1, rows.getJSONObject(count - 1).getInt("id"));
		assertEquals("the spool file is deleted", before, spoolFiles().length);
	}

	@Test public void scriptArraysAreDetached() throws Exception {
		var cx = org.mozilla.javascript.Context.enter();
		try {
			var scope = cx.initStandardObjects();
			var rows = cx.evaluateString(scope, "[{id: 1, tags: ['a', 'b']}, {id: 2, nested: {ok: true}}]", "rows", 1, null);
			var spool = new EmitSpool(100, 1_000_000);
			spool.append(rows, cx, scope);
			assertEquals(2, spool.rows());
			var first = spool.next();
			assertEquals(1, ((Number) first.get("id")).intValue());
			assertEquals(List.of("a", "b"), JsonValues.list(first.get("tags")));
			assertEquals(Boolean.TRUE, JsonValues.map(spool.next().get("nested")).get("ok"));
			assertNull(spool.next());
			spool.close();
		} finally {
			org.mozilla.javascript.Context.exit();
		}
	}

	@Test public void chunksAreAppendedWithoutConversion() throws Exception {
		var cx = org.mozilla.javascript.Context.enter();
		try {
			var scope = cx.initStandardObjects();
			List<Object> rows = List.of(JsonValues.object("id", 1, "tags", List.of("a")), JsonValues.object("id", 2));
			var chunk = new com.twinsoft.convertigo.engine.flow.rows.RowChunk(rows, scope);
			org.mozilla.javascript.ScriptableObject.putProperty(scope, "chunk", chunk);
			assertEquals(2.0, cx.evaluateString(scope, "chunk.length", "length", 1, null));
			assertEquals(Boolean.TRUE, cx.evaluateString(scope, "Array.isArray(chunk) && chunk[0].tags[0] === 'a'", "read", 1, null));
			assertEquals("[{\"id\":1,\"tags\":[\"a\"]},{\"id\":2}]", cx.evaluateString(scope, "JSON.stringify(chunk)", "json", 1, null));
			try { cx.evaluateString(scope, "chunk[0] = {}", "write", 1, null); fail("read-only chunk"); }
			catch (org.mozilla.javascript.EvaluatorException expected) { assertTrue(expected.getMessage().contains("read-only")); }
			var spool = new EmitSpool(100, 1_000_000);
			spool.append(chunk, cx, scope);
			assertEquals(2, spool.rows());
			assertEquals(List.of("a"), JsonValues.list(spool.next().get("tags")));
			assertEquals(2, ((Number) spool.next().get("id")).intValue());
			spool.close();
			assertEquals(rows, com.twinsoft.convertigo.engine.util.DetachedJson.map(cx.evaluateString(scope, "({rows: chunk})", "detach", 1, null), cx, scope).get("rows"));
		} finally {
			org.mozilla.javascript.Context.exit();
		}
	}

	static final class Commit implements com.twinsoft.convertigo.engine.flow.rows.RowStreamCommit {
		final StringWriter out = new StringWriter();
		int commits;
		@Override public java.io.Writer commit(com.twinsoft.convertigo.engine.Context context) { commits++; return out; }
		// a context needs a started engine: the relay follows its progress only when it has one
		@Override public boolean committed() { return commits > 0; }
	}

	private static List<Object> rows(int from, int to) throws Exception {
		var rows = new java.util.ArrayList<Object>();
		for (int i = from; i < to; i++) rows.add(JsonValues.object("id", i, "text", "é<&/ " + "x".repeat(80)));
		return rows;
	}

	@Test public void aSmallEmissionDoesNotCommitTheResponse() throws Exception {
		var commit = new Commit();
		var spool = EmitSpool.relay(commit, null, "data", 100_000, 256L * 1024 * 1024);
		spool.append(rows(0, 50), null, null);
		assertFalse(spool.committed());
		assertEquals(0, commit.commits);
		var out = new StringWriter();
		try (var doc = document(spool)) { doc.writeTo(out); }
		assertEquals(50, new JSONObject(out.toString()).getJSONArray("data").length());
	}

	@Test public void aLargeEmissionIsWrittenToTheResponseAsItComes() throws Exception {
		var commit = new Commit();
		var spool = EmitSpool.relay(commit, null, "data", 100_000, 256L * 1024 * 1024);
		spool.append(rows(0, 800), null, null);
		assertTrue("past the precommit buffer, the response is committed", spool.committed());
		int written = commit.out.getBuffer().length();
		assertTrue("the rows kept so far are written at the commit", written > 64 * 1024);
		spool.append(rows(800, 2000), null, null);
		assertTrue("the next rows follow, by bounded blocks", commit.out.getBuffer().length() > written + 100_000);
		var fields = new java.util.LinkedHashMap<String, Object>();
		fields.put("colDef", null);
		fields.put("count", 2000);
		spool.finish(fields);
		assertEquals(1, commit.commits);
		var text = commit.out.toString();
		var json = new JSONObject(text);
		var data = json.getJSONArray("data");
		assertEquals(2000, data.length());
		for (int i = 0; i < 2000; i += 97) assertEquals(i, data.getJSONObject(i).getInt("id"));
		assertEquals("é<&/ " + "x".repeat(80), data.getJSONObject(1999).getString("text"));
		assertTrue(json.isNull("colDef"));
		assertEquals(2000, json.getInt("count"));
		assertTrue(json.getBoolean("complete"));
		assertTrue("the field first, the other keys after the rows: " + text.substring(0, 20), text.startsWith("{\"data\":[{") && text.indexOf("\"colDef\"") > text.lastIndexOf("\"id\""));
	}

	@Test public void anErrorAfterTheCommitEndsTheResponseIncomplete() throws Exception {
		var commit = new Commit();
		var spool = EmitSpool.relay(commit, null, "rows", 100_000, 256L * 1024 * 1024);
		spool.append(rows(0, 1000), null, null);
		assertTrue(spool.committed());
		spool.fail(JsonValues.object("code", "SOURCE_DOWN", "message", "The source failed.", "status", 502));
		var json = new JSONObject(commit.out.toString());
		assertEquals(1000, json.getJSONArray("rows").length());
		assertEquals("SOURCE_DOWN", json.getJSONObject("error").getString("code"));
		assertFalse(json.getBoolean("complete"));
		spool.fail(JsonValues.object("code", "TWICE"));
		assertEquals("a second end writes nothing", json.toString(), new JSONObject(commit.out.toString()).toString());
	}

	@Test public void limitsAndConsumptionAreEnforced() throws Exception {
		var spool = new EmitSpool(2, 1_000_000);
		spool.append(List.of(JsonValues.object("id", 1), JsonValues.object("id", 2)), null, null);
		try { spool.append(JsonValues.object("id", 3), null, null); fail("row limit"); }
		catch (RowStreamException expected) { assertEquals(413, expected.status()); }
		spool.close();
		var bytes = new EmitSpool(100, 20);
		try { bytes.append(JsonValues.object("text", "a text longer than the byte budget"), null, null); fail("byte limit"); }
		catch (RowStreamException expected) { assertEquals(413, expected.status()); }
		bytes.close();
		var read = new EmitSpool(100, 1_000_000);
		read.append(JsonValues.object("id", 1), null, null);
		assertNotNull(read.next());
		try { read.append(JsonValues.object("id", 2), null, null); fail("append after reading"); }
		catch (RowStreamException expected) { assertEquals(409, expected.status()); }
		read.close();
		try { read.next(); fail("read after close"); }
		catch (RowStreamException expected) { assertEquals(499, expected.status()); }
	}
}
