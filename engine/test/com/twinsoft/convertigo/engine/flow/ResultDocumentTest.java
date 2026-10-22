/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicReference;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import com.twinsoft.convertigo.engine.flow.rows.ResultDocument;
import com.twinsoft.convertigo.engine.flow.rows.RowSource;
import com.twinsoft.convertigo.engine.flow.rows.RowStreamException;
import com.twinsoft.convertigo.engine.util.DetachedJson;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ResultDocumentTest {
	@Test public void utf8BudgetsMatchTheWriterWithoutEncodingTemporaryArrays()throws Exception{
		var random=new java.util.Random(37);
		for(int sample=0;sample<100;sample++){
			var text=new StringBuilder("é😀\u2028\uD800\uDC00\uD800x\uDC00");
			for(int i=0;i<50;i++)text.append((char)random.nextInt(65536));
			var value=text.toString();var encoded=new java.io.StringWriter();
			com.twinsoft.convertigo.engine.util.DetachedJsonWriter.write(value,encoded);
			int bytes=encoded.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
			assertEquals(bytes,ResultDocument.encodedBytes(value,bytes));
			try{ResultDocument.encodedBytes(value,bytes-1);fail("byte limit");}catch(java.io.IOException expected){}
		}
	}

	@org.junit.Before public void logs() { com.twinsoft.convertigo.engine.Engine.logBeans = org.apache.log4j.Logger.getLogger("row-test"); }
	static class Source implements RowSource {
		int pulls, closed, size = 20, failAt = -1; boolean failClose, badMetadata;
		@Override public JSONObject next() throws IOException {
			if (closed != 0) throw new AssertionError("pull after close");
			if (pulls++ == failAt) throw new RowStreamException("SYNTHETIC_PAGE_FAILURE", 502);
			if (pulls > size) return null;
			try { return new DetachedJson.ObjectValue().put("id", pulls).put("text", "é<&/"); }
			catch (Exception e) { throw new IOException(e); }
		}
		@Override public JSONObject metadata() throws IOException {
			try { return badMetadata ? new JSONObject().put("data", "collision") : new JSONObject().put("count", size); }
			catch (Exception e) { throw new IOException(e); }
		}
		@Override public void close() throws IOException { if (++closed != 1) throw new AssertionError("double source close"); if (failClose) throw new IOException("close"); }
	}
	private ResultDocument document(Source source) throws Exception {
		return new ResultDocument(source, "data", new DetachedJson.ObjectValue().put("colDef", JSONObject.EXPLICIT_NULL),
				new ResultDocument.Limits(100, 100_000, 1024, 2, 4096));
	}
	@Test public void normalEmptyAndManyRowsAreWrittenOnce() throws Exception {
		for (int size : new int[] {0,1,20}) {
			var source = new Source(); source.size = size; var doc = document(source); var out = new StringWriter();
			doc.writeTo(out); var json = new JSONObject(out.toString());
			assertEquals(size, json.getJSONArray("data").length()); assertTrue(json.getBoolean("complete"));
			assertEquals(size, json.getInt("count")); assertTrue(json.isNull("colDef")); assertEquals(1, source.closed);
			doc.close(); assertEquals(1, source.closed);
			try { doc.writeTo(new StringWriter()); fail("consumed"); } catch (RowStreamException expected) { assertEquals("ROW_STREAM_CONSUMED", expected.code()); }
		}
	}
	@Test public void earlyProducerFailureWritesNothingAndKeepsItsStatus() throws Exception {
		var source = new Source(); source.failAt = 1; var doc = document(source); var out = new StringWriter();
		try { doc.writeTo(out); fail("early error"); } catch (RowStreamException expected) { assertEquals(502, expected.status()); }
		assertEquals("", out.toString()); assertEquals(1, source.closed);
	}
	@Test public void lateProducerFailureClosesTheArrayAndMarksPartialData() throws Exception {
		var source = new Source(); source.failAt = 4; var doc = document(source); var out = new StringWriter();
		doc.writeTo(out); var json = new JSONObject(out.toString());
		assertEquals(4, json.getJSONArray("data").length()); assertFalse(json.getBoolean("complete"));
		assertEquals("SYNTHETIC_PAGE_FAILURE", json.getJSONObject("error").getString("code")); assertEquals(1, source.closed);
	}
	@Test public void disconnectAbortsImmediatelyAndDoesNotAppendAnError() throws Exception {
		var source = new Source(); var doc = document(source); var captured = new StringWriter();
		var completion = new AtomicReference<ResultDocument.Completion>(); doc.onClose(completion::set);
		var writer = new Writer() {
			@Override public void write(char[] c,int o,int n) { captured.write(c,o,n); }
			@Override public void flush() throws IOException { throw new IOException("disconnected"); }
			@Override public void close() { }
		};
		try { doc.writeTo(writer); fail("write failure"); } catch (IOException expected) { }
		assertEquals(2, source.pulls); assertEquals(1, source.closed); assertFalse(captured.toString().contains("error"));
		assertFalse(completion.get().complete()); assertEquals(499, completion.get().status());
	}
	@Test public void abandonmentDoesNotOpenTheProducerAndCompletesExactlyOnce() throws Exception {
		var source = new Source(); var doc = document(source); int[] completions = {0};
		doc.onClose(c -> { completions[0]++; assertFalse(c.complete()); assertFalse(c.committed()); });
		doc.close(); doc.close(); assertEquals(0, source.pulls); assertEquals(1, source.closed); assertEquals(1, completions[0]);
		try { doc.onClose(c -> completions[0]++); fail("duplicate completion"); } catch (IllegalStateException expected) { }
	}
	@Test public void boundedCollectionCanFeedXmlWithoutReexecution() throws Exception {
		var source = new Source(); source.size = 3; var doc = document(source); var value = doc.collect();
		var xml = XMLUtils.getDefaultDocumentBuilder().newDocument(); var root = xml.createElement("document"); xml.appendChild(root);
		XMLUtils.jsonToXml(value, null, root, true, true, false, "object");
		assertEquals(3, root.getElementsByTagName("object").getLength()); assertEquals(4, source.pulls); assertEquals(1, source.closed);
		try { doc.collect(); fail("second collection"); } catch (RowStreamException expected) { }
	}
	@Test public void rowAndByteLimitsStopBeforeAnotherPageCanBeRequested() throws Exception {
		for (boolean rows : new boolean[] {true,false}) {
			var source = new Source();
			var doc = new ResultDocument(source,"data",new JSONObject(),new ResultDocument.Limits(rows ? 3 : 100,rows ? 100_000 : 100,1024,1,2048));
			var out = new StringWriter(); doc.writeTo(out); var json = new JSONObject(out.toString());
			assertFalse(json.getBoolean("complete")); assertEquals(1,source.closed);
			assertTrue(source.pulls <= 4); assertTrue(json.getJSONObject("error").getString("code").contains(rows ? "ROW_LIMIT" : "BYTE_LIMIT"));
		}
	}
	@Test public void oversizedFirstRowFailsBeforeCommit() throws Exception {
		var source = new Source() { @Override public JSONObject next() throws IOException { pulls++; try { return new DetachedJson.ObjectValue().put("value","x".repeat(2048)); } catch (Exception e) { throw new IOException(e); } } };
		var doc = document(source); var out = new StringWriter();
		try { doc.writeTo(out); fail("row size"); } catch (RowStreamException expected) { assertEquals(413,expected.status()); }
		assertEquals("",out.toString()); assertEquals(1,source.closed);
	}
	@Test public void closeAndMetadataFailuresAreRecoverableBeforeTrailerWriting() throws Exception {
		for (boolean close : new boolean[] {true,false}) {
			var source = new Source(); source.failClose = close; source.badMetadata = !close;
			var out = new StringWriter(); document(source).writeTo(out); var json = new JSONObject(out.toString());
			assertFalse(json.getBoolean("complete")); assertTrue(json.has("error")); assertEquals(1,source.closed);
		}
	}
	@Test public void preparedResponseCanBeAbandonedWithoutFurtherPulls() throws Exception {
		var source = new Source(); var doc = document(source); doc.prepare(); assertEquals(2,source.pulls);
		doc.close(); assertEquals(2,source.pulls); assertEquals(1,source.closed);
	}
	@Test public void completionFollowsFinalFlushAndAlsoWorksWhenRegisteredAfterClose() throws Exception {
		var source = new Source(); var doc = document(source); var out = new StringWriter();
		doc.writeTo(out); var completion = new AtomicReference<ResultDocument.Completion>(); doc.onClose(completion::set);
		assertTrue(completion.get().complete()); assertEquals(20,completion.get().rows()); assertTrue(completion.get().bytes()>0);
	}
	@Test public void concurrentAbandonCancelsBlockedPreparationAndClosesOnce() throws Exception {
		var entered=new java.util.concurrent.CountDownLatch(1);
		var released=new java.util.concurrent.CountDownLatch(1);
		var closes=new java.util.concurrent.atomic.AtomicInteger();
		var source=new RowSource() {
			public JSONObject next() throws IOException {
				entered.countDown(); try { released.await(); } catch(InterruptedException e) { throw new IOException(e); }
				return new JSONObject();
			}
			public void close() { closes.incrementAndGet(); released.countDown(); }
		};
		var doc=new ResultDocument(source,"rows",new JSONObject(),new ResultDocument.Limits(10,10000,1000,2,2000));
		var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
		var worker=new Thread(()->{try{doc.prepare();}catch(Throwable e){failure.set(e);}});
		worker.start(); assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)); doc.close(); worker.join(5000);
		assertFalse(worker.isAlive()); assertTrue(failure.get() instanceof RowStreamException);
		assertEquals(1,closes.get()); doc.close(); assertEquals(1,closes.get());
	}

}
