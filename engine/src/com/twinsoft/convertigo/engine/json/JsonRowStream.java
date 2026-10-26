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
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

/**
 * The rows of a Baserow table read in stream, page after page, each row formatted, deduplicated, its columns
 * selected and written as JSON right away, then dropped: the memory does not depend on the number of rows. The first
 * page comes from the sequence (already read); the next ones are read by following the "next" URL Baserow returns,
 * with a JSON parser on the HTTP stream. The JSON written is the one the native steps and the JavaScript of
 * lib_BaseRow give, byte for byte.
 */
public class JsonRowStream {
	private static final Logger log = Logger.getLogger("cems.Beans");
	private static final JsonFactory JSON_FACTORY = new JsonFactory();
	private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NEVER).build();
	private static final Pattern PAGE = Pattern.compile("[?&]page=(\\d+)(?:[&#]|$)");
	private static final int FLUSH = 16 * 1024;

	/** an error of Baserow (or of its reading) on a page after the first one */
	public static class StreamFailure extends Exception {
		private static final long serialVersionUID = 1L;

		public StreamFailure(String message) {
			super(message);
		}
	}

	/** where the written JSON goes, each time the buffer grows past a few kilobytes */
	public interface Sink {
		void write(StringBuilder json) throws IOException;
	}

	private static final class Column {
		Object field;
		boolean hidden;
		Boolean leanTyped;
		String prefix;
	}

	// configuration
	private final BaserowCellFormat format;
	private final Map<String, Object> fieldsByName = new HashMap<String, Object>();
	private final boolean hasSchema;
	private final Set<String> exposed;
	private final List<String> hiddenList;
	private final String hiddenText;
	private final boolean lean;
	private final List<String> distinctColumns;
	private final List<String> firstColumns;
	private final List<String> outputColumns;
	private final List<String> columnsOrder;
	private final int maxRows;
	private final String baseUrl;
	private final String authorization;
	private final String failPage;
	private String startUrl;

	// state
	private List<Object> firstRows;
	private String nextUrl;
	private String lastUrl;
	private final Set<String> fetchedPages = new HashSet<String>();
	private int rowsRead;
	private boolean firstWritten;
	private final Set<String> seen;
	private final Map<String, Column> columns = new HashMap<String, Column>();
	private final Map<String, Object> leanFields = new LinkedHashMap<String, Object>();

	// one row, reused
	private String[] keys = new String[32];
	private Object[] values = new Object[32];
	private int size;
	private Object[] cellValues = new Object[32];
	private int[] htmlStart = new int[32];
	private int[] htmlEnd = new int[32];
	private final StringBuilder html = new StringBuilder(4096);
	private String[] orderKeys;
	private int[] order;
	private int orderSize;
	private boolean[] placed = new boolean[32];

	public JsonRowStream(BaserowCellFormat format, List<Object> schema, List<String> exposedColumns, Object hidden, boolean lean,
			List<String> distinctColumns, List<String> firstColumns, List<String> outputColumns, List<String> columnsOrder,
			int maxRows, List<Object> firstRows, String firstPage, String nextUrl, String baseUrl, String authorization, String failPage,
			String restartPageSize) {
		this.format = format;
		this.hasSchema = schema != null;
		if (schema != null) {
			for (Object field : schema) {
				fieldsByName.put(JsonData.toStr(JsonData.get(field, "name")), field);
			}
		}
		this.exposed = exposedColumns == null ? null : new HashSet<String>(exposedColumns);
		this.hiddenList = JsonData.isArray(hidden) ? JsonData.strings(hidden) : null;
		this.hiddenText = hidden instanceof CharSequence ? hidden.toString() : null;
		this.lean = lean;
		this.distinctColumns = distinctColumns;
		this.seen = distinctColumns == null ? null : new HashSet<String>();
		this.firstColumns = firstColumns;
		this.outputColumns = outputColumns;
		this.columnsOrder = columnsOrder;
		this.maxRows = maxRows;
		this.firstRows = firstRows;
		if (firstPage != null) {
			fetchedPages.add(firstPage);
		}
		this.nextUrl = nextUrl;
		this.baseUrl = baseUrl;
		this.authorization = authorization;
		this.failPage = failPage;
		if (restartPageSize != null && nextUrl != null && firstPage != null) {
			// the sequence read a first page of one row (its request, its errors, an empty table): the stream reads
			// everything again from the first page, with the page size of the source
			String url = pageUrl(nextUrl, firstPage);
			if (url != null) {
				startUrl = url.replaceFirst("([?&]page=)\\d+", "$1" + firstPage).replaceFirst("([?&]size=)\\d+", "$1" + restartPageSize);
				this.firstRows = null;
				this.nextUrl = null;
			}
		}
	}

	/** the options of the columns met with a value, with values only (lean_cells) */
	public Map<String, Object> getLeanFields() {
		return leanFields;
	}

	/** writes the array of the rows: the first page, then the next ones read from Baserow */
	public void writeArray(StringBuilder out, Sink sink) throws StreamFailure, IOException {
		out.append('[');
		if (firstRows != null) {
			for (Object row : firstRows) {
				size = 0;
				for (String key : JsonData.keys(row)) {
					add(key, JsonData.get(row, key));
				}
				writeRow(out);
				flush(out, sink);
			}
			firstRows = null;
		}
		if (startUrl != null) {
			String url = startUrl;
			startUrl = null;
			readPage(url, fetchedPages.iterator().next(), out, sink);
		}
		while (nextUrl != null && rowsRead < maxRows) {
			String page = nextPageNumber(nextUrl);
			if (page == null || !fetchedPages.add(page)) {
				break;
			}
			String url = pageUrl(nextUrl, page);
			nextUrl = null;
			readPage(url, page, out, sink);
		}
		out.append(']');
	}

	private static void flush(StringBuilder out, Sink sink) throws IOException {
		if (out.length() >= FLUSH) {
			sink.write(out);
		}
	}

	/** getNextPageNumber of formssource_GetTableData */
	static String nextPageNumber(String next) {
		String text = JsonData.trim(next);
		if (text.isEmpty() || text.equals("null")) {
			return null;
		}
		if (text.chars().allMatch(c -> c >= '0' && c <= '9')) {
			return text;
		}
		Matcher m = PAGE.matcher(text);
		return m.find() ? m.group(1) : null;
	}

	/** the URL of a page: the path and query Baserow gives in next, on the address the connector uses */
	private String pageUrl(String next, String page) {
		String text = JsonData.trim(next);
		if (text.chars().allMatch(c -> c >= '0' && c <= '9')) {
			// a page number only: the previous request with this page
			return lastUrl == null ? null : lastUrl.replaceFirst("([?&]page=)\\d+", "$1" + page);
		}
		int scheme = text.indexOf("://");
		int path = scheme < 0 ? 0 : text.indexOf('/', scheme + 3);
		return baseUrl + (path < 0 ? "/" : text.substring(path));
	}

	private void readPage(String url, String page, StringBuilder out, Sink sink) throws StreamFailure, IOException {
		if (url == null) {
			return;
		}
		lastUrl = url;
		if (page.equals(failPage)) {
			throw new StreamFailure("C8O_STREAM_TEST_FAILURE");
		}
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).GET()
				.header("Accept", "application/json");
		if (authorization != null && !authorization.isEmpty()) {
			request.header("Authorization", authorization);
		}
		HttpResponse<InputStream> response;
		long timingStart = com.twinsoft.convertigo.engine.util.TimingTrace.start();
		try {
			response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
			com.twinsoft.convertigo.engine.util.TimingTrace.end(timingStart, "stream-send", "page" + page);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new StreamFailure("Baserow page " + page + " interrupted");
		} catch (IOException e) {
			throw new StreamFailure("Baserow page " + page + " could not be read: " + e.getMessage());
		}
		// a page that was asked is read, or the stream fails: an HTTP error, an empty or not JSON answer, a page without
		// rows must not end the rows silently with the ones read so far
		int status = response.statusCode();
		boolean results = false;
		try (InputStream body = response.body(); JsonParser parser = JSON_FACTORY.createParser(body)) {
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				throw new StreamFailure("Baserow page " + page + " could not be read" + (status >= 400 ? " (HTTP " + status + ")" : ""));
			}
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String name = parser.getCurrentName();
				JsonToken token = parser.nextToken();
				if (name.equals("results") && token == JsonToken.START_ARRAY && status < 400) {
					results = true;
					while (parser.nextToken() != JsonToken.END_ARRAY) {
						if (rowsRead >= maxRows || parser.currentToken() != JsonToken.START_OBJECT) {
							parser.skipChildren();
							continue;
						}
						readRow(parser);
						writeRow(out);
						flush(out, sink);
					}
				} else if (name.equals("error") && token != JsonToken.VALUE_NULL) {
					throw new StreamFailure(JsonData.toStr(readValue(parser)));
				} else if (name.equals("next")) {
					nextUrl = token == JsonToken.VALUE_NULL ? null : JsonData.toStr(readValue(parser));
				} else {
					parser.skipChildren();
				}
			}
			if (!results) {
				throw new StreamFailure("Baserow page " + page + " could not be read" + (status >= 400 ? " (HTTP " + status + ")" : ""));
			}
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			// an unreadable page: formssource_GetTableData would read no row of it and stop
			log.warn("(JsonRowStream) Baserow page " + page + " is not JSON: " + e.getOriginalMessage());
			throw new StreamFailure("Baserow did not return JSON for the page " + page);
		}
	}

	private void ensure(int capacity) {
		if (capacity > keys.length) {
			int length = Math.max(capacity, keys.length * 2);
			keys = Arrays.copyOf(keys, length);
			values = Arrays.copyOf(values, length);
			cellValues = Arrays.copyOf(cellValues, length);
			htmlStart = Arrays.copyOf(htmlStart, length);
			htmlEnd = Arrays.copyOf(htmlEnd, length);
			placed = Arrays.copyOf(placed, length);
		}
	}

	private void add(String key, Object value) {
		ensure(size + 1);
		keys[size] = key;
		values[size] = value;
		size++;
	}

	/** one row object of the stream into keys and values, in the order a JavaScript object gives them */
	private void readRow(JsonParser parser) throws IOException {
		size = 0;
		boolean index = false;
		while (parser.nextToken() == JsonToken.FIELD_NAME) {
			String key = parser.getCurrentName();
			parser.nextToken();
			index |= JsonData.isIndexKey(key);
			add(key, readValue(parser));
		}
		if (index) {
			Map<String, Object> byKey = new HashMap<String, Object>();
			List<String> list = new ArrayList<String>(size);
			for (int i = 0; i < size; i++) {
				byKey.put(keys[i], values[i]);
				list.add(keys[i]);
			}
			size = 0;
			for (String key : JsonData.jsOrder(list)) {
				add(key, byKey.get(key));
			}
		}
	}

	/** a JSON value as JSON.parse gives it: maps, lists, texts, numbers as doubles, booleans, null */
	private static Object readValue(JsonParser parser) throws IOException {
		switch (parser.currentToken()) {
		case START_OBJECT:
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String key = parser.getCurrentName();
				parser.nextToken();
				map.put(key, readValue(parser));
			}
			return map;
		case START_ARRAY:
			List<Object> list = new ArrayList<Object>();
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				list.add(readValue(parser));
			}
			return list;
		case VALUE_STRING:
			return parser.getText();
		case VALUE_NUMBER_INT:
		case VALUE_NUMBER_FLOAT:
			return parser.getDoubleValue();
		case VALUE_TRUE:
			return Boolean.TRUE;
		case VALUE_FALSE:
			return Boolean.FALSE;
		default:
			return null;
		}
	}

	private Column column(String key) {
		Column column = columns.get(key);
		if (column == null) {
			column = new Column();
			column.field = hasSchema && (exposed == null || exposed.contains(key)) ? fieldsByName.get(key) : null;
			column.hidden = hiddenList != null ? hiddenList.contains(key) : hiddenText != null && hiddenText.contains(key);
			StringBuilder prefix = new StringBuilder();
			RhinoUtils.writeJsonString(prefix, key);
			prefix.append(":{\"displayName\":");
			RhinoUtils.writeJsonString(prefix, key);
			prefix.append(",\"type\":\"html\"");
			column.prefix = prefix.toString();
			columns.put(key, column);
		}
		return column;
	}

	private int indexOf(String key) {
		for (int i = 0; i < size; i++) {
			if (key.equals(keys[i])) {
				return i;
			}
		}
		return -1;
	}

	/** the order of the output columns, computed again only when the keys of the row change */
	private void computeOrder() {
		if (orderKeys != null && orderKeys.length == size) {
			boolean same = true;
			for (int i = 0; i < size && same; i++) {
				same = orderKeys[i] == keys[i] || orderKeys[i].equals(keys[i]);
			}
			if (same) {
				return;
			}
		}
		orderKeys = Arrays.copyOf(keys, size);
		order = new int[size];
		orderSize = 0;
		Arrays.fill(placed, 0, size, false);
		if (firstColumns != null) {
			for (String key : firstColumns) {
				place(indexOf(key));
			}
		}
		if (outputColumns != null) {
			for (String key : outputColumns) {
				place(indexOf(key));
			}
		} else {
			if (columnsOrder != null) {
				for (String key : columnsOrder) {
					place(indexOf(key));
				}
			}
			for (int i = 0; i < size; i++) {
				place(i);
			}
		}
	}

	private void place(int index) {
		if (index >= 0 && !placed[index]) {
			placed[index] = true;
			order[orderSize++] = index;
		}
	}

	private void writeRow(StringBuilder out) {
		rowsRead++;
		html.setLength(0);
		for (int i = 0; i < size; i++) {
			Column column = column(keys[i]);
			Object current = values[i];
			if (JsonData.isNullish(current)) {
				cellValues[i] = null;
				htmlStart[i] = -1;
			} else if (lean) {
				if (column.leanTyped == null) {
					column.leanTyped = BaserowCellFormat.isLeanTyped(column.field);
					if (column.field != null) {
						leanFields.put(keys[i], format.leanFieldOptions(column.field));
					}
				}
				cellValues[i] = BaserowCellFormat.leanValue(current, column.leanTyped);
				htmlStart[i] = -1;
			} else {
				htmlStart[i] = html.length();
				html.append(BaserowCellFormat.BASE_HTML);
				try {
					cellValues[i] = format.format(current, column.field, false, html);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
				htmlEnd[i] = html.length();
			}
		}
		if (distinctColumns != null) {
			StringBuilder signature = new StringBuilder(64);
			for (int c = 0; c < distinctColumns.size(); c++) {
				String key = distinctColumns.get(c);
				int i = indexOf(key);
				if (c > 0) {
					signature.append('|');
				}
				signature.append(key).append('=').append(JsonData.stringify(i < 0 ? null : cellValues[i]));
			}
			if (!seen.add(signature.toString())) {
				return;
			}
		}
		computeOrder();
		if (firstWritten) {
			out.append(',');
		}
		firstWritten = true;
		out.append('{');
		for (int o = 0; o < orderSize; o++) {
			int i = order[o];
			Column column = columns.get(keys[i]);
			if (o > 0) {
				out.append(',');
			}
			out.append(column.prefix);
			Object value = cellValues[i];
			if (value != JsonData.UNDEFINED) {
				out.append(",\"value\":");
				RhinoUtils.writeJsonData(out, value);
			}
			if (htmlStart[i] >= 0) {
				out.append(",\"displayValue\":");
				RhinoUtils.writeJsonString(out, html, htmlStart[i], htmlEnd[i]);
			}
			if (column.hidden) {
				out.append(",\"hidden\":true");
			}
			out.append('}');
		}
		out.append('}');
	}
}
