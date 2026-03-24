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
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

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
		
		boolean doGZip = false;
		try {
			doGZip = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.NET_GZIP);
			if (doGZip) {
				String acceptEncoding = HeaderName.AcceptEncoding.getHeader(request);
				if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
					String uri = request.getRequestURI();
					uri = uri.substring(request.getContextPath().length());
					uri = uri.replaceFirst("^/(?:system/)?projects/[^/]+/\\.services(?:/|$)", "/services/");
					uri = uri.replaceFirst("^/(?:system/)?projects/[^/]+/\\.fullsync(?:/|$)", "/fullsync/");
					boolean isKO = pKO.matcher(uri).find();
					boolean isOK = pOK.matcher(uri).find();
					if (!isOK && isDisplayObjectsSpaRoute(uri)) {
						isOK = true;
					}
					doGZip = !isKO && isOK;
				} else {
					doGZip = false;
				}
			}
		} catch (Exception e) {
		}
		
		if (doGZip) {
			GZipServletResponseWrapper gzipResponse = new GZipServletResponseWrapper(response);
			request.setAttribute("response", gzipResponse);
			filterChain.doFilter(request, gzipResponse);
			gzipResponse.close();
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
		String lastSegment = uri.substring(uri.lastIndexOf('/') + 1);
		return !lastSegment.contains(".");
	}

	private boolean isCompressibleContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return true;
		}
		String mimeType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
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

	class GZipServletResponseWrapper extends HttpServletResponseWrapper {
		private enum Mode {
			undecided,
			gzip,
			identity
		}

		private ServletOutputStream     outputStream      = null;
		private GZipServletOutputStream gzipOutputStream = null;
		private PrintWriter             printWriter      = null;
		private boolean                 closed           = false;
		private Mode                    mode             = Mode.undecided;
		private String                  pendingContentLength = null;

		private GZipServletResponseWrapper(HttpServletResponse response) throws IOException {
			super(response);
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
			mode = Mode.gzip;
			pendingContentLength = null;
			HeaderName.ContentEncoding.setHeader(this, "gzip");
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
			if (mode == Mode.gzip && gzipOutputStream != null) {
				gzipOutputStream.finish();
				gzipOutputStream.close();
				if (printWriter != null) {
					printWriter.close();
				}
			} else if (printWriter != null) {
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
				if (decideMode() == Mode.gzip) {
					gzipOutputStream = new GZipServletOutputStream(getResponse().getOutputStream());
					outputStream = gzipOutputStream;
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
			if (mode == Mode.gzip) {
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
			if (mode == Mode.gzip) {
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
				if (mode == Mode.gzip) {
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
				if (mode == Mode.gzip) {
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
				if (mode == Mode.gzip) {
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
				if (mode == Mode.gzip) {
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

	class GZipServletOutputStream extends ServletOutputStream {
		private final ServletOutputStream output;
		private final GZIPOutputStream gzipOutputStream;

		private GZipServletOutputStream(OutputStream output) throws IOException {
			super();
			this.output = (ServletOutputStream) output;
			/*
			 * Tomcat 11 flushes dynamic responses earlier than previous versions.
			 * Keep pending deflate data visible on flush to avoid sending only the
			 * gzip header before the response is committed.
			 */
			gzipOutputStream = new GZIPOutputStream(output, true);
		}

		@Override
		public void close() throws IOException {
			gzipOutputStream.close();
		}

		private void finish() throws IOException {
			gzipOutputStream.finish();
		}

		@Override
		public void flush() throws IOException {
			gzipOutputStream.flush();
		}

		@Override
		public void write(byte b[]) throws IOException {
			gzipOutputStream.write(b);
		}

		@Override
		public void write(byte b[], int off, int len) throws IOException {
			gzipOutputStream.write(b, off, len);
		}

		@Override
		public void write(int b) throws IOException {
			gzipOutputStream.write(b);
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
