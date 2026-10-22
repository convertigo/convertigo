/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.twinsoft.convertigo.engine.util.DetachedJson;

/** Pull parser over HTTP entities. Page continuation never grants a new origin/path authority. */
public final class PagedJsonRows implements RowSource {
	public record Bounds(int pages, long rows, long inputBytes, int rowNodes, int stringCharacters, int depth, long millis) {
		public Bounds {
			if (pages < 1 || pages > 10_000 || rows < 1 || rows > 1_000_000 || inputBytes < 1 || inputBytes > 512L * 1024 * 1024
					|| rowNodes < 1 || rowNodes > 100_000 || stringCharacters < 1 || stringCharacters > 1024 * 1024
					|| depth < 1 || depth > 128 || millis < 1 || millis > 600_000) throw new IllegalArgumentException("Invalid HTTP row bounds");
		}
	}
	private final com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface client;
	private final boolean ownClient;
	private final URI first;
	private final Map<String,String> headers;
	private final String itemsPointer, nextPointer, pageParameter;
	private final Bounds bounds;
	private final int connectTimeout, readTimeout;
	private final long take;
	private final List<String> redactions;
	private final Set<String> droppedKeys;
	private final Set<String> pagesSeen = new HashSet<>();
	private final JsonFactory factory;
	private CloseableHttpResponse response;
	private volatile HttpRequestBase request;
	private JsonParser parser;
	private URI next;
	private volatile boolean closed;
	private boolean ended, inItems, sawItems, rootStarted, rootEnded;
	private int pages, rowNodes;
	private long rows, inputBytes, started;

	public PagedJsonRows(CloseableHttpClient client, boolean ownClient, URI first, Map<String,String> headers,
			String itemsPointer, String nextPointer, Bounds bounds, long take, int connectTimeout, int readTimeout,
			List<String> redactions, Set<String> droppedKeys) throws IOException {
		this(new com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface() {
			@Override public CloseableHttpResponse execute(org.apache.http.client.methods.HttpUriRequest request) throws IOException { return client.execute(request); }
			@Override public void close() throws IOException { client.close(); }
		}, ownClient, first, headers, itemsPointer, nextPointer, bounds, take, connectTimeout, readTimeout, redactions, droppedKeys);
	}
	public PagedJsonRows(com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface client, boolean ownClient, URI first, Map<String,String> headers,
			String itemsPointer, String nextPointer, Bounds bounds, long take, int connectTimeout, int readTimeout,
			List<String> redactions, Set<String> droppedKeys) throws IOException {
		this(client,ownClient,first,headers,itemsPointer,nextPointer,bounds,take,connectTimeout,readTimeout,redactions,droppedKeys,"");
	}
	public PagedJsonRows(com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface client, boolean ownClient, URI first, Map<String,String> headers,
			String itemsPointer, String nextPointer, Bounds bounds, long take, int connectTimeout, int readTimeout,
			List<String> redactions, Set<String> droppedKeys, String pageParameter) throws IOException {
		if (pageParameter == null || !pageParameter.isEmpty() && !pageParameter.matches("[A-Za-z][A-Za-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid page token parameter");
		this.pageParameter = pageParameter;
		this.client = client; this.ownClient = ownClient; this.first = valid(first); this.headers = Map.copyOf(headers);
		this.itemsPointer = pointer(itemsPointer); this.nextPointer = pointer(nextPointer); this.bounds = bounds;
		if (itemsPointer.equals(nextPointer) || itemsPointer.startsWith(nextPointer + "/") || nextPointer.startsWith(itemsPointer + "/")) throw new IllegalArgumentException("Overlapping row pointers");
		if (take < 0 || take > bounds.rows() || connectTimeout < 1 || connectTimeout > 60_000 || readTimeout < 1 || readTimeout > 60_000) throw new IllegalArgumentException("Invalid row request budget");
		this.take = take; this.connectTimeout = connectTimeout; this.readTimeout = readTimeout;
		this.redactions = redactions.stream().filter(v -> v != null && !v.isEmpty()).toList(); this.droppedKeys = Set.copyOf(droppedKeys);
		if (headers.size() > 100 || this.redactions.size() > 100 || droppedKeys.size() > 100) throw new IllegalArgumentException("Row request metadata limit");
		for (var entry : headers.entrySet()) {
			if (!entry.getKey().matches("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,160}") || entry.getValue().length() > 65_536
					|| entry.getValue().indexOf('\r') >= 0 || entry.getValue().indexOf('\n') >= 0
					|| Set.of("host","content-length","transfer-encoding","connection").contains(entry.getKey().toLowerCase(java.util.Locale.ROOT))) throw new IllegalArgumentException("Invalid row request header");
		}
		factory = JsonFactory.builder().disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
				.streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(bounds.depth()).maxStringLength(bounds.stringCharacters()).maxNameLength(bounds.stringCharacters()).maxNumberLength(1000).build()).build();
	}
	private static String pointer(String value) {
		if (value == null || !value.startsWith("/") || value.length() > 512 || value.split("/",-1).length > 32) throw new IllegalArgumentException("Invalid row JSON pointer");
		com.fasterxml.jackson.core.JsonPointer.compile(value); return value;
	}
	private static URI valid(URI uri) throws IOException {
		if (uri == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
				|| uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null || uri.toASCIIString().length() > 65_536
				|| !uri.normalize().getRawPath().equals(uri.getRawPath())) throw new RowStreamException("ROW_STREAM_URL", 400);
		return uri;
	}
	private static int port(URI uri) { return uri.getPort() == -1 ? "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80 : uri.getPort(); }
	private URI continuation(String token) throws IOException {
		if (!pageParameter.isEmpty()) return numberedPage(token);
		try {
			var uri = valid(token.startsWith("?") ? URI.create(first.toASCIIString().split("\\?",2)[0] + token) : first.resolve(token));
			if (!uri.getScheme().equalsIgnoreCase(first.getScheme()) || !uri.getHost().equalsIgnoreCase(first.getHost())
					|| port(uri) != port(first) || !java.util.Objects.equals(uri.getRawPath(), first.getRawPath())) throw new RowStreamException("ROW_STREAM_NEXT_ORIGIN", 502);
			return uri;
		} catch (IllegalArgumentException bad) { throw new RowStreamException("ROW_STREAM_NEXT_URL", 502); }
	}
	/** Treat the response as an opaque page token, never as a grant of URL authority. */
	private URI numberedPage(String token) throws IOException {
		try {
			if (token.length()>65_536) throw new IllegalArgumentException();
			String page = token.matches("[0-9]{1,7}") ? token : null;
			if (page == null) {
				var reference = new org.apache.http.client.utils.URIBuilder(token);
				if (reference.getUserInfo()!=null || reference.getFragment()!=null) throw new IllegalArgumentException();
				for (var pair : reference.getQueryParams()) if (pageParameter.equals(pair.getName())) {
					if (page != null) throw new IllegalArgumentException(); page=pair.getValue();
				}
			}
			if (page == null || !page.matches("[0-9]{1,7}") || Long.parseLong(page)<1 || Long.parseLong(page)>1_000_000) throw new IllegalArgumentException();
			// Scheme, origin, path, all filters and repeated query parameters come
			// exclusively from the authorized first request. Ignore all other next data.
			return new org.apache.http.client.utils.URIBuilder(first).setParameter(pageParameter,Long.toString(Long.parseLong(page))).build();
		} catch (Exception invalid) { throw new RowStreamException("ROW_STREAM_PAGE_TOKEN",502); }
	}

	private void check() throws IOException {
		if (closed || Thread.currentThread().isInterrupted()) throw new RowStreamException("ROW_STREAM_CANCELLED", 499);
		if (started == 0) started = System.nanoTime();
		if ((System.nanoTime()-started)/1_000_000 >= bounds.millis()) throw new RowStreamException("ROW_STREAM_TIMEOUT", 504);
	}
	@Override public JSONObject next() throws IOException {
		try {
			check();
			if (ended) return null;
			if (take != 0 && rows >= take) { ended = true; closePage(true); return null; }
			while (true) {
				check();
				if (parser == null) open(pages == 0 ? first : next);
				if (!inItems) seekItems();
				var token = parser.nextToken();
				if (token == JsonToken.END_ARRAY) {
					inItems = false; finishPage(); closePage(false);
					if (next == null) { ended = true; return null; }
					continue;
				}
				if (token != JsonToken.START_OBJECT) throw new RowStreamException("ROW_STREAM_ITEM", 502);
				if (++rows > bounds.rows()) throw new RowStreamException("ROW_STREAM_INPUT_ROWS", 413);
				rowNodes = 0;
				return (JSONObject) value(token);
			}
		} catch (StreamConstraintsException limit) { closeQuietly(); throw new RowStreamException("ROW_STREAM_INPUT_LIMIT", 413); }
		catch (java.io.InterruptedIOException timeout) {
			boolean cancelled = closed || Thread.currentThread().isInterrupted(); closeQuietly();
			throw new RowStreamException(cancelled ? "ROW_STREAM_CANCELLED" : "ROW_STREAM_TIMEOUT", cancelled ? 499 : 504);
		}
		catch (Exception error) { boolean cancelled = closed; closeQuietly(); throw cancelled ? new RowStreamException("ROW_STREAM_CANCELLED",499) : RowStreamException.safe(error); }
	}
	private void open(URI uri) throws IOException {
		if (++pages > bounds.pages()) throw new RowStreamException("ROW_STREAM_PAGE_LIMIT", 413);
		try {
			var hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8)));
			if (!pagesSeen.add(hash)) throw new RowStreamException("ROW_STREAM_REPEATED_PAGE", 502);
		} catch (java.security.NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
		var remaining = Math.max(1, bounds.millis() - (System.nanoTime()-started)/1_000_000);
		var builder = RequestBuilder.get(uri).setConfig(RequestConfig.custom().setRedirectsEnabled(false)
				.setConnectTimeout((int) Math.min(connectTimeout,remaining)).setConnectionRequestTimeout((int)Math.min(connectTimeout,remaining))
				.setSocketTimeout((int) Math.min(readTimeout,remaining)).build());
		for (var header : headers.entrySet()) builder.setHeader(header.getKey(),header.getValue());
		var outgoing = (HttpRequestBase) builder.build();
		synchronized (this) { check(); request = outgoing; }
		var received = client.execute(outgoing);
		synchronized (this) {
			if (closed) { received.close(); throw new RowStreamException("ROW_STREAM_CANCELLED",499); }
			response = received;
		}
		if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300 || response.getEntity() == null) throw new RowStreamException("ROW_STREAM_HTTP_STATUS", 502);
		InputStream counted = new FilterInputStream(response.getEntity().getContent()) {
			private void count(int n) throws IOException { check(); if (n>0 && (inputBytes += n)>bounds.inputBytes()) throw new RowStreamException("ROW_STREAM_INPUT_BYTES",413); }
			@Override public int read() throws IOException { int value=in.read(); count(value<0?0:1); return value; }
			@Override public int read(byte[] b,int o,int n) throws IOException { int read=in.read(b,o,n); count(read); return read; }
		};
		var opened = factory.createParser(counted);
		synchronized (this) {
			if (closed) { opened.close(); throw new RowStreamException("ROW_STREAM_CANCELLED",499); }
			parser = opened;
		}
		next = null; inItems = false; sawItems = false; rootStarted = rootEnded = false;
	}
	private void seekItems() throws IOException {
		while (scan()) { if (inItems) return; }
		throw new RowStreamException("ROW_STREAM_ITEMS_MISSING",502);
	}
	private void finishPage() throws IOException {
		while (scan()) { if (inItems) throw new RowStreamException("ROW_STREAM_DUPLICATE_ITEMS",502); }
	}
	private boolean scan() throws IOException {
		var token = parser.nextToken(); if (token == null) return false;
		if (rootEnded || !rootStarted && token != JsonToken.START_OBJECT) throw new RowStreamException("ROW_STREAM_DOCUMENT",502);
		rootStarted = true;
		if (token == JsonToken.END_OBJECT && parser.getParsingContext().inRoot()) rootEnded = true;
		if (token == JsonToken.FIELD_NAME || token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) return true;
		var path = parser.getParsingContext().pathAsPointer().toString();
		if (path.equals(itemsPointer)) {
			if (token != JsonToken.START_ARRAY || sawItems) throw new RowStreamException("ROW_STREAM_ITEMS",502);
			inItems = sawItems = true;
		} else if (path.equals(nextPointer)) {
			if (token == JsonToken.VALUE_NULL) next = null;
			else if (token == JsonToken.VALUE_STRING) next = continuation(parser.getText());
			else throw new RowStreamException("ROW_STREAM_NEXT_TOKEN",502);
		} else if ((token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY)
				&& !itemsPointer.startsWith(path + "/") && !nextPointer.startsWith(path + "/")) parser.skipChildren();
		return true;
	}
	private Object value(JsonToken token) throws IOException, JSONException {
		if (++rowNodes > bounds.rowNodes()) throw new RowStreamException("ROW_STREAM_ROW_NODES",413);
		if (token == JsonToken.START_OBJECT) {
			var object = new DetachedJson.ObjectValue();
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				if (parser.currentToken() != JsonToken.FIELD_NAME) throw new RowStreamException("ROW_STREAM_JSON",502);
				var key = parser.currentName(); var child = value(parser.nextToken());
				if (!droppedKeys.contains(key)) object.put(key,child);
			}
			return object;
		}
		if (token == JsonToken.START_ARRAY) {
			var array = new DetachedJson.ArrayValue(); JsonToken child;
			while ((child=parser.nextToken()) != JsonToken.END_ARRAY) array.put(value(child));
			return array;
		}
		if (token == JsonToken.VALUE_STRING) {
			var text = parser.getText();
			for (var secret : redactions) if (text.indexOf(secret)>=0) text=text.replace(secret,"[redacted]");
			return text;
		}
		if (token == JsonToken.VALUE_TRUE) return Boolean.TRUE;
		if (token == JsonToken.VALUE_FALSE) return Boolean.FALSE;
		if (token == JsonToken.VALUE_NULL) return JSONObject.EXPLICIT_NULL;
		if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT) {
			var number = parser.getDoubleValue(); return Double.isFinite(number) ? number : JSONObject.EXPLICIT_NULL;
		}
		throw new RowStreamException("ROW_STREAM_JSON",502);
	}
	private synchronized void closePage(boolean abort) throws IOException {
		try { if (abort && request != null) request.abort(); if (parser != null) parser.close(); }
		finally { parser=null; request=null; if(response!=null) { var current=response; response=null; current.close(); } }
	}
	private void closeQuietly() { try { close(); } catch (IOException ignored) { } }
	@Override public void close() throws IOException {
		synchronized (this) { if (closed) return; closed=true; }
		var active = request; if (active != null) active.abort();
		try { closePage(true); } finally { if (ownClient) client.close(); }
	}
}
