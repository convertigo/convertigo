/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 */
public final class EmitSpool implements ResultDocument.RawRowSource {
	private static final int MEMORY = 4 * 1024 * 1024;
	private final long maxRows;
	private final long maxChars;
	private final StringBuilder memory = new StringBuilder(64 * 1024);
	private final StringWriter row = new StringWriter(1024);
	private File file;
	private Writer fileWriter;
	private BufferedReader reader;
	private int memoryRead;
	private long rows, chars;
	private boolean reading, closed;

	public EmitSpool(long maxRows, long maxBytes) {
		this.maxRows = maxRows;
		this.maxChars = maxBytes;
	}

	/** Appends emitted rows: a script array or list of rows, or one row object. */
	public void append(Object value, Context cx, Scriptable scope) throws IOException, JSONException {
		if (reading || closed) throw new RowStreamException("ROW_STREAM_CONSUMED", 409);
		if (value instanceof List<?> list) {
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
		row.getBuffer().setLength(0);
		DetachedJsonWriter.write(json, row);
		var text = row.getBuffer();
		chars += text.length() + 1;
		if (chars > maxChars) throw new RowStreamException("ROW_STREAM_BYTE_LIMIT", 413);
		if (fileWriter == null && memory.length() + text.length() + 1 > MEMORY) spill();
		if (fileWriter != null) { fileWriter.append(text).append('\n'); }
		else { memory.append(text).append('\n'); }
	}

	private void spill() throws IOException {
		var directory = LocalWorkDirectory.getDirectory("emit");
		file = directory == null ? File.createTempFile("c8o-emit-", ".jsonl") : File.createTempFile("emit-", ".jsonl", directory);
		file.deleteOnExit();
		fileWriter = new BufferedWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8), 64 * 1024);
	}

	public long rows() { return rows; }

	@Override public boolean nextRaw(StringBuilder target) throws IOException {
		if (closed) throw new RowStreamException("ROW_STREAM_CANCELLED", 499);
		if (!reading) {
			reading = true;
			if (fileWriter != null) { fileWriter.close(); fileWriter = null; }
		}
		if (memoryRead < memory.length()) {
			int end = memory.indexOf("\n", memoryRead);
			target.append(memory, memoryRead, end);
			memoryRead = end + 1;
			return true;
		}
		if (memory.length() > 0) { memory.setLength(0); memory.trimToSize(); memoryRead = 0; }
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
