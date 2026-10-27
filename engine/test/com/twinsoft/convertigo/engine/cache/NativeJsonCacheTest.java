/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.cache;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;

public class NativeJsonCacheTest {
	private static NativeJsonCache.Pending pending(String key) {
		return new NativeJsonCache.Pending(key, System.currentTimeMillis() + 60_000);
	}

	@Test public void aCompleteAnswerIsKeptAsWritten() throws Exception {
		var out = new StringWriter();
		var tee = new NativeJsonCache.Tee(out, pending("native json test complete"));
		tee.write("{\"data\":[");
		tee.write(new char[] {'1', ',', '2'}, 0, 3);
		tee.append("]");
		tee.write('}');
		tee.flush();
		assertNull("nothing is kept before the end", NativeJsonCache.get("native json test complete"));
		tee.store();
		assertEquals("{\"data\":[1,2]}", out.toString());
		assertEquals("the cache keeps the text the response received", out.toString(), NativeJsonCache.get("native json test complete"));
	}

	@Test public void anIncompleteOrTooLargeAnswerIsNotKept() throws Exception {
		var dropped = new NativeJsonCache.Tee(new StringWriter(), pending("native json test dropped"));
		dropped.write("{\"data\":[1],\"complete\":false}");
		dropped.discard();
		dropped.store();
		assertNull(NativeJsonCache.get("native json test dropped"));

		var out = new StringWriter();
		var large = new NativeJsonCache.Tee(out, pending("native json test large"));
		var block = "x".repeat(1024 * 1024);
		for (int i = 0; i <= NativeJsonCache.MAX_CHARS / block.length(); i++) large.write(block);
		large.store();
		assertTrue("the response receives everything", out.getBuffer().length() > NativeJsonCache.MAX_CHARS);
		assertNull("a larger answer is not kept", NativeJsonCache.get("native json test large"));
	}

	@Test public void anExpiredAnswerIsNotServed() throws Exception {
		var tee = new NativeJsonCache.Tee(new StringWriter(), new NativeJsonCache.Pending("native json test expired", System.currentTimeMillis() - 1));
		tee.write("{}");
		tee.store();
		assertNull(NativeJsonCache.get("native json test expired"));
	}
}
