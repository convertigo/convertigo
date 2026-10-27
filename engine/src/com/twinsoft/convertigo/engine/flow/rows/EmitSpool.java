/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.LocalWorkDirectory;
import com.twinsoft.convertigo.engine.util.DetachedJson;
import com.twinsoft.convertigo.engine.util.DetachedJsonWriter;
import com.twinsoft.convertigo.engine.util.JsonValues;

/**
 * The rows a script emits for a streamed result field, encoded as they come, one JSON row per line: a few megabytes
 * in memory, then a temporary file of the local working directory. The emitted rows are not retained as objects. The
 * response reads the spool once, as a row source, then deletes it.
 * <p>
 * With the commit of a servlet response, the spool is a relay: past its precommit buffer it commits the response from
 * the thread of the run and writes the rows as they come, with a bounded buffer; the end of the run writes the other
 * keys of the result, or its error. Below the precommit buffer, the response is the one of an ordinary spool.
 */
public final class EmitSpool implements ResultDocument.RawRowSource {
	private static final int MEMORY = 4 * 1024 * 1024;
	private static final int FLUSH = 64 * 1024;
	// as the native JSON responses: an error of a smaller result still answers alone, with its status
	private static final int PRECOMMIT = 64 * 1024;
	private static final int WRITE = 16 * 1024;
	private final long maxRows;
	private final long maxChars;
	// the encoded rows, one per line: all of them until the spill, then the part not yet flushed to the file
	private final StringBuilder memory = new StringBuilder(64 * 1024);
	// rows are encoded straight into memory: one producer, no lock per character
	private final Writer encoder = new Writer() {
		@Override public void write(int c) { memory.append((char) c); }
		@Override public void write(char[] buffer, int offset, int length) { memory.append(buffer, offset, length); }
		@Override public void write(String text, int offset, int length) { memory.append(text, offset, offset + length); }
		@Override public Writer append(CharSequence text) { memory.append(text); return this; }
		@Override public void flush() { }
		@Override public void close() { }
	};
	// the end of each row kept in memory, before the spill
	private int[] ends = new int[1024];
	private int memoryRows, memoryRead;
	private File file;
	private Writer fileWriter;
	private BufferedReader reader;
	private long rows, chars;
	private boolean reading, closed;
	// the relay to a servlet response
	private RowStreamCommit commit;
	private com.twinsoft.convertigo.engine.Context context;
	private String field;
	private Writer response;
	private boolean ended;

	public EmitSpool(long maxRows, long maxBytes) {
		this.maxRows = maxRows;
		this.maxChars = maxBytes;
	}

	/** The spool of a result field for the request of a context: a relay when its servlet can stream rows. */
	public static EmitSpool open(com.twinsoft.convertigo.engine.Context context, String field, long maxRows, long maxBytes) {
		var request = context == null ? null : context.httpServletRequest;
		if (request != null && request.getAttribute(RowStreamCommit.ATTRIBUTE) instanceof RowStreamCommit commit && !commit.committed()) {
			return relay(commit, context, field, maxRows, maxBytes);
		}
		return new EmitSpool(maxRows, maxBytes);
	}

	/** The relay of a result field to the response a commit gives; the context, if any, follows its progress. */
	public static EmitSpool relay(RowStreamCommit commit, com.twinsoft.convertigo.engine.Context context, String field, long maxRows, long maxBytes) {
		var spool = new EmitSpool(maxRows, maxBytes);
		spool.commit = commit;
		spool.context = context;
		spool.field = field;
		if (context != null) context.rowStream = spool;
		return spool;
	}

	private void progress() {
		if (context != null) context.rowStreamProgress = System.currentTimeMillis();
	}

	/** true once the rows are written to the response, which the end of the run completes with finish or fail */
	public boolean committed() {
		return response != null;
	}

	/** Appends emitted rows: a chunk, a script array or list of rows, or one row object. */
	public void append(Object value, Context cx, Scriptable scope) throws IOException, JSONException {
		if (reading || closed) throw new RowStreamException("ROW_STREAM_CONSUMED", 409);
		if (value instanceof RowChunk chunk) {
			for (var item : chunk.rows()) appendRow(item, cx, scope);
		} else if (value instanceof List<?> list) {
			for (var item : list) appendRow(item, cx, scope);
		} else if (value instanceof Scriptable array && cx != null && "Array".equals(array.getClassName())) {
			var length = (int) org.mozilla.javascript.ScriptRuntime.toUint32(array.get("length", array));
			for (int i = 0; i < length; i++) appendRow(array.get(i, array), cx, scope);
		} else {
			appendRow(value, cx, scope);
		}
	}

	private void appendRow(Object value, Context cx, Scriptable scope) throws IOException, JSONException {
		if (++rows > maxRows) throw new RowStreamException("ROW_STREAM_ROW_LIMIT", 413);
		Object json = value instanceof Map<?, ?> ? value : DetachedJson.map(value, cx, scope);
		if (response != null) {
			if (rows > 1) memory.append(',');
			int start = memory.length();
			DetachedJsonWriter.write(json, encoder);
			chars += memory.length() - start + 1;
			if (chars > maxChars) throw new RowStreamException("ROW_STREAM_BYTE_LIMIT", 413);
			if (memory.length() >= WRITE) write();
			return;
		}
		int start = memory.length();
		DetachedJsonWriter.write(json, encoder);
		int end = memory.length();
		chars += end - start + 1;
		if (chars > maxChars) throw new RowStreamException("ROW_STREAM_BYTE_LIMIT", 413);
		memory.append('\n');
		if (fileWriter == null) {
			if (memoryRows == ends.length) ends = Arrays.copyOf(ends, ends.length * 2);
			ends[memoryRows++] = end;
			if (commit != null && memory.length() > PRECOMMIT) relay();
			else if (memory.length() > MEMORY) spill();
		} else if (memory.length() >= FLUSH) {
			fileWriter.append(memory);
			memory.setLength(0);
		}
	}

	private void spill() throws IOException {
		var directory = LocalWorkDirectory.getDirectory("emit");
		file = directory == null ? File.createTempFile("c8o-emit-", ".jsonl") : File.createTempFile("emit-", ".jsonl", directory);
		file.deleteOnExit();
		fileWriter = new BufferedWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8), 64 * 1024);
		fileWriter.append(memory);
		memory.setLength(0);
		memoryRows = 0;
		ends = new int[0];
	}

	// commits the response and writes the rows kept so far
	private void relay() throws IOException {
		response = commit.commit(context);
		var head = new StringBuilder(memory.length() + field.length() + 8).append('{');
		DetachedJsonWriter.quote(field, encoderOf(head), true);
		head.append(":[");
		for (int i = 0; i < memoryRows; i++) {
			if (i > 0) head.append(',');
			head.append(memory, i == 0 ? 0 : ends[i - 1] + 1, ends[i]);
		}
		memory.setLength(0);
		memoryRows = 0;
		ends = new int[0];
		response.append(head);
		progress();
	}

	private void write() throws IOException {
		response.append(memory);
		memory.setLength(0);
		progress();
	}

	private static Writer encoderOf(StringBuilder target) {
		return new Writer() {
			@Override public void write(char[] buffer, int offset, int length) { target.append(buffer, offset, length); }
			@Override public void write(String text, int offset, int length) { target.append(text, offset, offset + length); }
			@Override public void flush() { }
			@Override public void close() { }
		};
	}

	/** Ends a committed response with the other keys of the result, after the rows. */
	public void finish(Map<String, Object> fields) throws IOException, JSONException {
		if (response == null || ended) throw new RowStreamException("ROW_STREAM_CONSUMED", 409);
		ended = true;
		memory.append(']');
		for (var entry : fields.entrySet()) {
			memory.append(',');
			DetachedJsonWriter.quote(entry.getKey(), encoder, true);
			memory.append(':');
			DetachedJsonWriter.write(entry.getValue(), encoder);
		}
		memory.append(",\"complete\":true}");
		write();
		response.flush();
		close();
	}

	/** Ends a committed response with the error of the run: its rows stay, incomplete. */
	public void fail(Map<String, Object> error) throws IOException {
		if (response == null || ended) return;
		ended = true;
		memory.append("],\"error\":");
		try {
			DetachedJsonWriter.write(error, encoder);
		} catch (JSONException e) {
			memory.append("{\"code\":\"ROW_STREAM_FAILED\",\"status\":500}");
		}
		memory.append(",\"complete\":false}");
		write();
		response.flush();
		close();
	}

	public long rows() { return rows; }

	@Override public boolean nextRaw(StringBuilder target) throws IOException {
		if (closed) throw new RowStreamException("ROW_STREAM_CANCELLED", 499);
		if (!reading) {
			reading = true;
			if (fileWriter != null) {
				fileWriter.append(memory);
				memory.setLength(0);
				fileWriter.close();
				fileWriter = null;
			}
		}
		if (memoryRead < memoryRows) {
			target.append(memory, memoryRead == 0 ? 0 : ends[memoryRead - 1] + 1, ends[memoryRead]);
			memoryRead++;
			return true;
		}
		if (memory.capacity() > 64 * 1024) { memory.setLength(0); memory.trimToSize(); }
		if (file == null) return false;
		if (reader == null) reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
		var line = reader.readLine();
		if (line == null) return false;
		target.append(line);
		return true;
	}

	@Override public Map<String, Object> next() throws IOException {
		var text = new StringBuilder();
		if (!nextRaw(text)) return null;
		return JsonValues.map(JsonValues.parse(text.toString()));
	}

	@Override public void close() throws IOException {
		if (closed) return;
		closed = true;
		if (context != null && context.rowStream == this) context.rowStream = null;
		try {
			if (fileWriter != null) fileWriter.close();
			if (reader != null) reader.close();
		} finally {
			fileWriter = null; reader = null;
			memory.setLength(0);
			if (file != null) { Files.deleteIfExists(file.toPath()); file = null; }
		}
	}
}
