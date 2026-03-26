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

package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.github.luben.zstd.ZstdOutputStream;
import com.github.luben.zstd.util.Native;
import com.twinsoft.convertigo.engine.Engine;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class CompressionFilter implements Filter {
	private enum CompressionEncoding {
		gzip("gzip", 1) {
			@Override
			OutputStream wrap(OutputStream output) throws IOException {
				return new GZIPOutputStream(output, true);
			}

			@Override
			void finish(OutputStream output) throws IOException {
				((GZIPOutputStream) output).finish();
			}
		},
		br("br", 2) {
			@Override
			boolean checkAvailability() {
				Brotli4jLoader.ensureAvailability();
				return true;
			}

			@Override
			OutputStream wrap(OutputStream output) throws IOException {
				return new BrotliOutputStream(output, Encoder.Parameters.DEFAULT);
			}
		},
		zstd("zstd", 3) {
			@Override
			boolean checkAvailability() {
				if (!Native.isLoaded()) {
					Native.load();
				}
				return true;
			}

			@Override
			OutputStream wrap(OutputStream output) throws IOException {
				return new ZstdOutputStream(output).setCloseFrameOnFlush(true);
			}
		},
		identity("identity", Integer.MIN_VALUE) {
			@Override
			OutputStream wrap(OutputStream output) throws IOException {
				return output;
			}
		};

		private final String token;
		private final int priority;
		private Boolean available = null;
		private boolean availabilityLogged = false;

		CompressionEncoding(String token, int priority) {
			this.token = token;
			this.priority = priority;
		}

		String token() {
			return token;
		}

		int priority() {
			return priority;
		}

		synchronized boolean isAvailable() {
			if (this == identity) {
				return true;
			}
			if (available != null) {
				return available;
			}
			try {
				available = checkAvailability();
			} catch (Throwable t) {
				available = false;
				if (!availabilityLogged) {
					availabilityLogged = true;
					Engine.logEngine.warn("(CompressionFilter) Disable " + token + " response compression because the native library is unavailable", t);
				}
			}
			return available;
		}

		boolean checkAvailability() {
			return true;
		}

		abstract OutputStream wrap(OutputStream output) throws IOException;

		void finish(OutputStream output) throws IOException {
		}
	}

	Pattern pKO = Pattern.compile(
		"^/qrcode|^/webclipper|^/rproxy/|\\.proxy$|\\.siteclipper/|^/fullsync/.+?/.+?/.+?|"
		+ "^/(?:admin/services|services)/(?:.*GetIcon|logs.Download|mobiles.GetPackage|"
		+ "mobiles.GetSourcePackage|projects.Export|store.DownloadStoreFolder)");
	Pattern pOK = Pattern.compile(
		"^/fullsync/|^/api/|^/openapi/|^/(?:admin/services|services)/|\\.js$|\\.xml$|\\.pxml$|\\.cxml$|\\.css$|\\.html$|"
		+ "\\.json$|\\.jsonp$|\\.txt$|\\.csv$|\\.htm$|\\.map$|/$");
	Pattern pDisplayObjectsSpa = Pattern.compile("^/(?:system/)?projects/[^/]+/DisplayObjects/(?:mobile|pwas/[^/]+)(?:/.*)?$");
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) _request;
		HttpServletResponse response = (HttpServletResponse) _response;

		if (response instanceof CompressionServletResponseWrapper compressionResponse) {
			request.setAttribute("response", compressionResponse);
			filterChain.doFilter(request, compressionResponse);
			return;
		}
		
		CompressionEncoding selectedEncoding = CompressionEncoding.identity;
		boolean shouldNegotiate = false;
		try {
			shouldNegotiate = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.NET_GZIP);
			if (shouldNegotiate) {
				String uri = request.getRequestURI();
				uri = uri.substring(request.getContextPath().length());
				uri = uri.replaceFirst("^/(?:system/)?projects/[^/]+/\\.services(?:/|$)", "/services/");
				uri = uri.replaceFirst("^/(?:system/)?projects/[^/]+/\\.fullsync(?:/|$)", "/fullsync/");
				boolean isKO = pKO.matcher(uri).find();
				boolean isOK = pOK.matcher(uri).find();
				if (!isOK && isDisplayObjectsSpaRoute(uri)) {
					isOK = true;
				}
				shouldNegotiate = !isKO && isOK;
				if (shouldNegotiate) {
					addVaryAcceptEncoding(response);
					selectedEncoding = negotiateEncoding(HeaderName.AcceptEncoding.getHeader(request));
				}
			}
		} catch (Exception e) {
			Engine.logEngine.warn("(CompressionFilter) Failed to negotiate response compression", e);
		}
		
		if (shouldNegotiate && selectedEncoding != CompressionEncoding.identity) {
			CompressionServletResponseWrapper compressionResponse = new CompressionServletResponseWrapper(response, selectedEncoding);
			request.setAttribute("response", compressionResponse);
			filterChain.doFilter(request, compressionResponse);
			compressionResponse.close();
		} else {
			filterChain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	private boolean isDisplayObjectsSpaRoute(String uri) {
		if (!pDisplayObjectsSpa.matcher(uri).matches() || uri.endsWith("/")) {
			return false;
		}
		var lastSegment = uri.substring(uri.lastIndexOf('/') + 1);
		return !lastSegment.contains(".");
	}

	private void addVaryAcceptEncoding(HttpServletResponse response) {
		for (var value : response.getHeaders(HeaderName.Vary.value())) {
			if ("*".equals(value)) {
				return;
			}
			for (var token : value.split(",")) {
				if (HeaderName.AcceptEncoding.is(token.trim())) {
					return;
				}
			}
		}
		HeaderName.Vary.addHeader(response, HeaderName.AcceptEncoding.value());
	}

	private CompressionEncoding negotiateEncoding(String acceptEncoding) {
		if (acceptEncoding == null || acceptEncoding.isBlank()) {
			return CompressionEncoding.identity;
		}

		var qValues = parseAcceptEncoding(acceptEncoding);
		CompressionEncoding best = CompressionEncoding.identity;
		double bestQ = 0d;

		for (var encoding : CompressionEncoding.values()) {
			if (encoding == CompressionEncoding.identity || !encoding.isAvailable()) {
				continue;
			}
			double q = qValueFor(encoding.token(), qValues);
			if (q <= 0d) {
				continue;
			}
			if (q > bestQ || (q == bestQ && encoding.priority() > best.priority())) {
				best = encoding;
				bestQ = q;
			}
		}

		return bestQ > 0d ? best : CompressionEncoding.identity;
	}

	private Map<String, Double> parseAcceptEncoding(String acceptEncoding) {
		var qValues = new HashMap<String, Double>();
		for (var rawPart : acceptEncoding.split(",")) {
			var part = rawPart.trim();
			if (part.isEmpty()) {
				continue;
			}
			var segments = part.split(";");
			var token = segments[0].trim().toLowerCase(Locale.ROOT);
			if (token.isEmpty()) {
				continue;
			}
			double q = 1d;
			for (var i = 1; i < segments.length; i++) {
				var parameter = segments[i].trim();
				if (parameter.regionMatches(true, 0, "q=", 0, 2)) {
					try {
						q = Math.max(0d, Math.min(1d, Double.parseDouble(parameter.substring(2).trim())));
					} catch (NumberFormatException e) {
						q = 0d;
					}
					break;
				}
			}
			qValues.merge(token, q, Math::max);
		}
		return qValues;
	}

	private double qValueFor(String token, Map<String, Double> qValues) {
		var explicit = qValues.get(token);
		if (explicit != null) {
			return explicit;
		}
		var wildcard = qValues.get("*");
		return wildcard != null ? wildcard : 0d;
	}

	private boolean isCompressibleContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return true;
		}
		var mimeType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		return mimeType.startsWith("text/")
			|| mimeType.contains("json")
			|| mimeType.contains("xml")
			|| mimeType.contains("javascript")
			|| mimeType.contains("ecmascript")
			|| mimeType.contains("css")
			|| mimeType.contains("html")
			|| mimeType.contains("yaml")
			|| mimeType.contains("csv")
			|| "image/svg+xml".equals(mimeType);
	}

	class CompressionServletResponseWrapper extends HttpServletResponseWrapper {
		private enum Mode {
			undecided,
			compressed,
			identity
		}

		private final CompressionEncoding selectedEncoding;
		private ServletOutputStream outputStream = null;
		private CompressionServletOutputStream compressionOutputStream = null;
		private PrintWriter printWriter = null;
		private boolean closed = false;
		private Mode mode = Mode.undecided;
		private String pendingContentLength = null;

		private CompressionServletResponseWrapper(HttpServletResponse response, CompressionEncoding selectedEncoding) throws IOException {
			super(response);
			this.selectedEncoding = selectedEncoding;
		}

		private void applyPendingContentLength() {
			if (pendingContentLength != null) {
				super.setHeader(HeaderName.ContentLength.value(), pendingContentLength);
				pendingContentLength = null;
			}
		}

		private Mode decideMode() {
			if (mode != Mode.undecided) {
				return mode;
			}
			if (HeaderName.ContentDisposition.has(this)) {
				mode = Mode.identity;
				applyPendingContentLength();
				return mode;
			}
			String contentType = HeaderName.ContentType.getHeader(this);
			if (contentType != null && !isCompressibleContentType(contentType)) {
				mode = Mode.identity;
				applyPendingContentLength();
				return mode;
			}
			mode = Mode.compressed;
			pendingContentLength = null;
			HeaderName.ContentEncoding.setHeader(this, selectedEncoding.token());
			return mode;
		}

		private void close() throws IOException {
			if (closed) {
				return;
			}
			closed = true;
			if (mode == Mode.undecided) {
				mode = Mode.identity;
				applyPendingContentLength();
			}
			if (printWriter != null) {
				printWriter.flush();
			}
			if (mode == Mode.compressed && compressionOutputStream != null) {
				compressionOutputStream.finish();
			}
			if (printWriter != null) {
				printWriter.close();
			} else if (outputStream != null) {
				outputStream.close();
			}
		}

		/**
		 * Flush OutputStream or PrintWriter
		 *
		 * @throws IOException
		 */

		@Override
		public void flushBuffer() throws IOException {

			//PrintWriter.flush() does not throw exception
			if (printWriter != null) {
				printWriter.flush();
			}

			if (outputStream != null) {
				outputStream.flush();
			} else {
				super.flushBuffer();
			}
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (printWriter != null) {
				throw new IllegalStateException("PrintWriter obtained already - cannot get OutputStream");
			}
			if (outputStream == null) {
				if (decideMode() == Mode.compressed) {
					compressionOutputStream = new CompressionServletOutputStream(getResponse().getOutputStream(), selectedEncoding);
					outputStream = compressionOutputStream;
				} else {
					outputStream = getResponse().getOutputStream();
				}
			}
			return outputStream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (printWriter == null && outputStream != null) {
				throw new IllegalStateException("OutputStream obtained already - cannot get PrintWriter");
			}

			if (printWriter == null) {
				outputStream = getOutputStream();
				String characterEncoding = getResponse().getCharacterEncoding();
				if (characterEncoding == null || characterEncoding.isBlank()) {
					characterEncoding = "UTF-8";
				}
				printWriter = new PrintWriter(new OutputStreamWriter(outputStream, characterEncoding));
			}
			return this.printWriter;
		}

		@Override
		public void setContentLength(int len) {
			if (mode == Mode.compressed) {
				return;
			}
			if (mode == Mode.identity) {
				super.setContentLength(len);
			} else {
				pendingContentLength = String.valueOf(len);
			}
		}		

		@Override
		public void setContentLengthLong(long len) {
			if (mode == Mode.compressed) {
				return;
			}
			if (mode == Mode.identity) {
				super.setContentLengthLong(len);
			} else {
				pendingContentLength = String.valueOf(len);
			}
		}

		@Override
		public void addHeader(String name, String value) {
			if (HeaderName.ContentLength.is(name)) {
				if (mode == Mode.compressed) {
					return;
				}
				if (mode == Mode.identity) {
					super.addHeader(name, value);
				} else {
					pendingContentLength = value;
				}
			} else {
				super.addHeader(name, value);
			}
		}

		@Override
		public void addIntHeader(String name, int value) {
			if (HeaderName.ContentLength.is(name)) {
				if (mode == Mode.compressed) {
					return;
				}
				if (mode == Mode.identity) {
					super.addIntHeader(name, value);
				} else {
					pendingContentLength = String.valueOf(value);
				}
			} else {
				super.addIntHeader(name, value);
			}
		}

		@Override
		public void setHeader(String name, String value) {
			if (HeaderName.ContentLength.is(name)) {
				if (mode == Mode.compressed) {
					return;
				}
				if (mode == Mode.identity) {
					super.setHeader(name, value);
				} else {
					pendingContentLength = value;
				}
			} else {
				super.setHeader(name, value);
			}
		}

		@Override
		public void setIntHeader(String name, int value) {
			if (HeaderName.ContentLength.is(name)) {
				if (mode == Mode.compressed) {
					return;
				}
				if (mode == Mode.identity) {
					super.setIntHeader(name, value);
				} else {
					pendingContentLength = String.valueOf(value);
				}
			} else {
				super.setIntHeader(name, value);
			}
		}
		
		
	}

	class CompressionServletOutputStream extends ServletOutputStream {
		private final CompressionEncoding encoding;
		private final ServletOutputStream output;
		private final OutputStream compressedOutputStream;

		private CompressionServletOutputStream(OutputStream output, CompressionEncoding encoding) throws IOException {
			super();
			this.encoding = encoding;
			this.output = (ServletOutputStream) output;
			compressedOutputStream = encoding.wrap(output);
		}

		@Override
		public void close() throws IOException {
			compressedOutputStream.close();
		}

		private void finish() throws IOException {
			encoding.finish(compressedOutputStream);
		}

		@Override
		public void flush() throws IOException {
			compressedOutputStream.flush();
		}

		@Override
		public void write(byte b[]) throws IOException {
			compressedOutputStream.write(b);
		}

		@Override
		public void write(byte b[], int off, int len) throws IOException {
			compressedOutputStream.write(b, off, len);
		}

		@Override
		public void write(int b) throws IOException {
			compressedOutputStream.write(b);
		}

		@Override
		public boolean isReady() {
			return output.isReady();
		}

		@Override
		public void setWriteListener(WriteListener listener) {
			output.setWriteListener(listener);
		}
	}
}
