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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;

import com.twinsoft.convertigo.engine.json.JsonRowStream.StreamFailure;
import com.twinsoft.convertigo.engine.util.JsonNativeOutput;

/**
 * The JSON answer of a sequence whose native outputs are written in stream by the servlet: the JSON of its XML
 * output, then each native output, as XMLUtils.XmlToJson gives them in one text.
 *
 * An error of Baserow while the rows are read: as long as less than COMMIT characters are written, nothing has left
 * and the answer is the error alone, exactly as formssource_GetTableData answers it ({"error": "..."}); beyond, the
 * array of the rows is closed and the answer ends with an "error" key, so the client gets a valid JSON with its
 * error.
 */
public class JsonStreamedResponse {
	static final int COMMIT = 64 * 1024;

	private final String head;
	private final boolean empty;
	private final List<JsonNativeOutput> outputs;
	private final String charset;

	public JsonStreamedResponse(String head, boolean empty, Collection<JsonNativeOutput> outputs, String charset) {
		this.head = head;
		this.empty = empty;
		this.outputs = new ArrayList<JsonNativeOutput>(outputs);
		this.charset = charset;
	}

	public String getCharset() {
		return charset;
	}

	private static final class Target {
		final Writer writer;
		StringBuilder pending = new StringBuilder(COMMIT + 16 * 1024);
		boolean committed;
		final char[] chunk = new char[8192];

		Target(Writer writer) {
			this.writer = writer;
		}

		void write(StringBuilder json) throws IOException {
			if (!committed) {
				pending.append(json);
				json.setLength(0);
				if (pending.length() >= COMMIT) {
					committed = true;
					copy(pending);
					pending = null;
				}
				return;
			}
			copy(json);
		}

		/** the target as a writer: kept while nothing is committed, then written as is */
		Writer asWriter() {
			return new Writer() {
				@Override
				public void write(char[] cbuf, int off, int len) throws IOException {
					if (committed) {
						writer.write(cbuf, off, len);
						return;
					}
					pending.append(cbuf, off, len);
					if (pending.length() >= COMMIT) {
						committed = true;
						copy(pending);
						pending = null;
					}
				}

				@Override
				public void write(String str, int off, int len) throws IOException {
					if (committed) {
						writer.write(str, off, len);
						return;
					}
					pending.append(str, off, off + len);
					if (pending.length() >= COMMIT) {
						committed = true;
						copy(pending);
						pending = null;
					}
				}

				@Override
				public void flush() {
				}

				@Override
				public void close() {
				}
			};
		}

		private void copy(StringBuilder json) throws IOException {
			int length = json.length();
			for (int start = 0; start < length; start += chunk.length) {
				int end = Math.min(length, start + chunk.length);
				json.getChars(start, end, chunk, 0);
				writer.write(chunk, 0, end - start);
			}
			json.setLength(0);
		}
	}

	/** writes the answer; the Baserow pages of the outputs in stream are read meanwhile */
	public void writeTo(Writer writer) throws IOException {
		Context.enter();
		try {
			Target target = new Target(writer);
			StringBuilder out = new StringBuilder(32 * 1024);
			out.append(head);
			boolean first = empty;
			StreamFailure failure = null;
			for (JsonNativeOutput output : outputs) {
				out.append(first ? "\n " : ",\n ").append(JSONObject.quote(output.getKey())).append(": ");
				first = false;
				if (output instanceof JsonStreamOutput && failure == null) {
					try {
						((JsonStreamOutput) output).writeTo(out, target::write);
					} catch (StreamFailure e) {
						if (!target.committed) {
							// nothing has left: the error alone, as the sequence answers it
							writer.write("{\n \"error\": " + JSONObject.quote(e.getMessage()) + "\n}");
							writer.flush();
							return;
						}
						failure = e;
						out.append(']');
					}
				} else if (output instanceof JsonStreamOutput) {
					((JsonStreamOutput) output).discard();
					out.append("null");
				} else {
					// the JSON of the output written directly, by chunks, without holding its whole text
					target.write(out);
					output.writeTo(target.asWriter());
				}
			}
			if (failure != null) {
				out.append(",\n ").append(JSONObject.quote("error")).append(": ").append(JSONObject.quote(failure.getMessage()));
			}
			out.append("\n}");
			target.write(out);
			if (!target.committed) {
				target.copy(target.pending);
			}
			writer.flush();
		} finally {
			Context.exit();
		}
	}

	/** the whole answer as a text */
	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			writeTo(writer);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return writer.toString();
	}
}
