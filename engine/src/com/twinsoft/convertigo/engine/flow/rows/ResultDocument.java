/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.util.DetachedJson;
import com.twinsoft.convertigo.engine.util.DetachedJsonWriter;

/** One-shot response owner. No input page, complete row array or original execution realm is retained. */
public final class ResultDocument implements AutoCloseable {
	public record Limits(long rows, long bytes, int rowBytes, int precommitRows, int precommitBytes) {
		public Limits {
			if (rows < 1 || rows > 1_000_000 || bytes < 1 || bytes > 256L * 1024 * 1024 || rowBytes < 1
					|| rowBytes > 1024 * 1024 || precommitRows < 1 || precommitRows > 100
					|| precommitBytes < rowBytes || precommitBytes > 1024 * 1024) throw new IllegalArgumentException("Invalid row stream limits");
		}
	}
	public record Completion(boolean complete, boolean committed, long rows, long bytes, long elapsedMillis, int status) { }
	private enum State { NEW, PREPARING, PREPARED, WRITING, COLLECTING, CLOSED }
	private final RowSource source;
	private final String field;
	private final JSONObject fields;
	private final Limits limits;
	private final ConcurrentLinkedDeque<String> staged = new ConcurrentLinkedDeque<>();
	private volatile State state = State.NEW;
	private boolean ended, committed, complete, sourceClosed, completionRegistered, failed;
	private long rows, bytes, started;
	private int status = 200;
	private String preparedMetadata;
	private Consumer<Completion> completion;

	public ResultDocument(RowSource source, String field, JSONObject fields, Limits limits) throws IOException {
		if (source == null || field == null || field.isBlank() || field.length() > 160
				|| field.equals("error") || field.equals("complete") || field.equals("attr")) throw new IllegalArgumentException("Invalid row result field");
		this.source = source; this.field = field; this.fields = fields; this.limits = limits;
		if (fields.has(field) || fields.has("error") || fields.has("complete")) throw new IllegalArgumentException("Reserved row result field");
		encode(fields, limits.precommitBytes()); // Reject unbounded metadata before ownership is published.
	}
	public synchronized void onClose(Consumer<Completion> callback) {
		if (completionRegistered) throw new IllegalStateException("Completion already registered");
		completionRegistered = true;
		completion = callback;
		if (state == State.CLOSED) notifyCompletion();
	}
	private synchronized void begin(State next) throws IOException {
		if (state != State.NEW) throw new RowStreamException("ROW_STREAM_CONSUMED", 409);
		state = next; started = System.nanoTime();
		bytes = 24 + utf8(encode(field, 1024));
	}
	/** Must run before acquiring/committing the servlet writer, on its eventual owner thread. */
	public void prepare() throws IOException {
		begin(State.PREPARING);
		try {
			int stagedBytes = 0;
			while (staged.size() < limits.precommitRows() && stagedBytes <= limits.precommitBytes() - limits.rowBytes()) {
				var row = pull(); if (row == null) { preparedMetadata = finalMetadata(); closeSource(); break; }
				staged.add(row); stagedBytes += utf8(row);
			}
			synchronized (this) {
				if (state != State.PREPARING) throw new RowStreamException("ROW_STREAM_CANCELLED",499);
				state = State.PREPARED;
			}
		} catch (Exception e) { failed = true; status = RowStreamException.safe(e).status(); closeQuietly(); throw RowStreamException.safe(e); }
	}
	private String pull() throws IOException {
		if (state == State.CLOSED) throw new RowStreamException("ROW_STREAM_CANCELLED",499);
		if (ended) return null;
		var row = source.next();
		if (state == State.CLOSED) throw new RowStreamException("ROW_STREAM_CANCELLED",499);
		if (row == null) { ended = true; return null; }
		if (++rows > limits.rows()) throw new RowStreamException("ROW_STREAM_ROW_LIMIT", 413);
		var encoded = encode(row, limits.rowBytes());
		bytes += utf8(encoded) + (rows > 1 ? 1 : 0);
		if (bytes > limits.bytes()) throw new RowStreamException("ROW_STREAM_BYTE_LIMIT", 413);
		return encoded;
	}
	/** The caller distinguishes a precommit exception from a completed partial-error document. */
	public void writeTo(Writer output) throws IOException { writeTo(output, ""); }
	public void writeTo(Writer output, String suffix) throws IOException {
		if (state == State.NEW) prepare();
		synchronized (this) {
			if (state != State.PREPARED) throw new RowStreamException("ROW_STREAM_CONSUMED", 409);
			state = State.WRITING;
		}
		try {
			output.write('{'); DetachedJsonWriter.quote(field, output, true); output.write(":["); committed = true;
			boolean separator = false;
			RowStreamException failure = null;
			String metadata = null;
			while (true) {
				String row;
				if (state == State.CLOSED) throw new IOException("Row response cancelled");
				try { row = staged.pollFirst(); if (row == null) row = pull(); }
				catch (Exception e) {
					if (state == State.CLOSED) throw new IOException("Row response cancelled");
					failure = RowStreamException.safe(e); break;
				}
				if (state == State.CLOSED) throw new IOException("Row response cancelled");
				if (row == null) break;
				// I/O failures are deliberately outside the recoverable producer catch.
				if (separator) output.write(','); output.write(row); separator = true;
				output.flush(); // backpressure and prompt PrintWriter/disconnect detection
			}
			if (failure == null) {
				try { metadata = preparedMetadata == null ? finalMetadata() : preparedMetadata; closeSource(); }
				catch (Exception e) { failure = RowStreamException.safe(e); }
			}
			output.write(']');
			if (failure == null) {
				if (metadata.length() > 2) { output.write(','); output.write(metadata, 1, metadata.length() - 2); }
				output.write(",\"complete\":true}"); output.write(suffix); output.flush(); complete = true;
			} else {
				failed = true; status = failure.status(); output.write(",\"error\":");
				try { failure.error().write(output); } catch (JSONException impossible) { throw new IOException(impossible); }
				output.write(",\"complete\":false}"); output.write(suffix);
				output.flush();
			}
		} catch (IOException connection) { status = 499; throw connection; }
		finally { closeQuietly(); }
	}
	/** XML/internal callers explicitly materialize under the same limits, exactly once. */
	public JSONObject collect() throws IOException {
		begin(State.COLLECTING);
		try {
			var result = new DetachedJson.ObjectValue(); var values = new DetachedJson.ArrayValue();
			while (true) {
				var row = source.next(); if (row == null) break;
				if (++rows > limits.rows()) throw new RowStreamException("ROW_STREAM_ROW_LIMIT", 413);
				bytes += utf8(encode(row, limits.rowBytes())) + (rows > 1 ? 1 : 0);
				if (bytes > limits.bytes()) throw new RowStreamException("ROW_STREAM_BYTE_LIMIT", 413);
				values.put(row);
			}
			var metadata = metadata(); accountMetadata(encode(metadata, limits.precommitBytes())); closeSource();
			for (var keys = metadata.keys(); keys.hasNext();) { var key = (String) keys.next(); result.put(key, metadata.get(key)); }
			result.put(field, values); complete = true; return result;
		} catch (Exception e) { failed = true; status = RowStreamException.safe(e).status(); throw RowStreamException.safe(e); }
		finally { closeQuietly(); }
	}
	private JSONObject metadata() throws IOException, JSONException {
		var tail = source.metadata();
		var merged = new DetachedJson.ObjectValue();
		for (var object : new JSONObject[] {fields, tail}) {
			for (var keys = object.keys(); keys.hasNext();) {
				var key = (String) keys.next();
				if (key.equals(field) || key.equals("error") || key.equals("complete") || key.equals("attr")) throw new RowStreamException("ROW_STREAM_METADATA", 502);
				merged.put(key, object.get(key));
			}
		}
		return merged;
	}
	private String finalMetadata() throws IOException, JSONException { return accountMetadata(encode(metadata(), limits.precommitBytes())); }
	private String accountMetadata(String value) throws IOException {
		bytes += Math.max(0, utf8(value) - 1);
		if (bytes > limits.bytes()) throw new RowStreamException("ROW_STREAM_BYTE_LIMIT",413);
		return value;
	}
	public static int encodedBytes(Object value, int limit) throws IOException { return utf8(encode(value, limit)); }
	private static int utf8(String text) { return text.getBytes(StandardCharsets.UTF_8).length; }
	private static String encode(Object value, int limit) throws IOException {
		var target = new StringWriter();
		var bounded = new Writer() {
			private int length;
			@Override public void write(char[] data, int off, int len) throws IOException {
				if ((long) length + len > limit) throw new RowStreamException("ROW_STREAM_ROW_BYTES", 413);
				length += len; target.write(data, off, len);
			}
			@Override public void flush() { }
			@Override public void close() { }
		};
		try { DetachedJsonWriter.write(value, bounded); } catch (JSONException e) { throw new RowStreamException("ROW_STREAM_INVALID_VALUE", 502); }
		var text = target.toString();
		if (utf8(text) > limit) throw new RowStreamException("ROW_STREAM_ROW_BYTES", 413);
		return text;
	}
	private synchronized void closeSource() throws IOException { if (!sourceClosed) { sourceClosed = true; source.close(); } }
	private void closeQuietly() { try { close(); } catch (IOException ignored) { } }
	@Override public synchronized void close() throws IOException {
		if (state == State.CLOSED) return;
		if (!complete && !failed && status == 200) status = 499;
		state = State.CLOSED; staged.clear(); preparedMetadata = null;
		try { closeSource(); } finally { notifyCompletion(); }
	}
	private void notifyCompletion() {
		var callback = completion; completion = null;
		if (callback != null) callback.accept(new Completion(complete, committed, rows, bytes,
				started == 0 ? 0 : (System.nanoTime() - started) / 1_000_000, status));
	}
}
