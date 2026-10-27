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
