/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */


package com.twinsoft.convertigo.engine.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.twinsoft.convertigo.engine.json.JsonRowStream.Sink;
import com.twinsoft.convertigo.engine.json.JsonRowStream.StreamFailure;
import com.twinsoft.convertigo.engine.util.JsonNativeOutput;

/**
 * A native output written in stream into the response: either rows read from Baserow while the response is written
 * (direct), or rows already written, as they were read, into a spool that stays a few kilobytes in memory and goes to
 * a temporary file beyond (spooled).
 */
public class JsonStreamOutput extends JsonNativeOutput {
	private static final int MEMORY = 64 * 1024;

	private final JsonRowStream rows;
	private ByteArrayOutputStream memory;
	private Path file;
	private OutputStream fileOut;

	/** rows read and written while the response is written */
	public JsonStreamOutput(String key, String nodeName, JsonRowStream rows) {
		super(key, nodeName, "item");
		this.rows = rows;
	}

	/** rows read now, during the sequence, and kept in a spool for the response */
	public static JsonStreamOutput spool(String key, String nodeName, JsonRowStream rows) throws StreamFailure, IOException {
		JsonStreamOutput output = new JsonStreamOutput(key, nodeName, null);
		output.memory = new ByteArrayOutputStream(8192);
		StringBuilder out = new StringBuilder(32 * 1024);
		try {
			rows.writeArray(out, output::spoolWrite);
			output.spoolWrite(out);
			if (output.fileOut != null) {
				output.fileOut.close();
				output.fileOut = null;
			}
		} catch (StreamFailure | IOException | RuntimeException e) {
			output.discard();
			throw e;
		}
		return output;
	}

	private void spoolWrite(StringBuilder json) throws IOException {
		byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
		json.setLength(0);
		if (fileOut == null && memory.size() + bytes.length > MEMORY) {
			file = Files.createTempFile("c8o-json-stream", ".json");
			fileOut = Files.newOutputStream(file);
			memory.writeTo(fileOut);
			memory = null;
		}
		if (fileOut != null) {
			fileOut.write(bytes);
		} else {
			memory.write(bytes);
		}
	}

	/** writes the JSON of the output into out, handing it to sink as it grows */
	public void writeTo(StringBuilder out, Sink sink) throws StreamFailure, IOException {
		if (rows != null) {
			rows.writeArray(out, sink);
			return;
		}
		try (InputStream in = file != null ? Files.newInputStream(file) : new java.io.ByteArrayInputStream(memory.toByteArray());
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			char[] buffer = new char[8192];
			int n;
			while ((n = reader.read(buffer)) > 0) {
				out.append(buffer, 0, n);
				if (out.length() >= 16 * 1024) {
					sink.write(out);
				}
			}
		} finally {
			discard();
		}
	}

	public void discard() {
		try {
			if (fileOut != null) {
				fileOut.close();
			}
			if (file != null) {
				Files.deleteIfExists(file);
			}
		} catch (IOException e) {
			// a temporary file left behind
		}
		fileOut = null;
		file = null;
	}

	/** the whole JSON as a text, for an answer that cannot be written in stream */
	@Override
	public String toJSONString() {
		StringBuilder out = new StringBuilder();
		try {
			writeTo(out, json -> {});
		} catch (StreamFailure | IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return out.toString();
	}
}
