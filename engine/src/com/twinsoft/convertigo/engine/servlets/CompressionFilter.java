/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class CompressionFilter implements Filter {

	Pattern pKO = Pattern.compile(
		"^/qrcode|^/webclipper|^/rproxy/|\\.proxy$|\\.siteclipper/|^/fullsync/.+?/.+?/.+?|"
		+ "^/admin/services/(?:.*GetIcon|logs.Download|mobiles.GetPackage|"
		+ "mobiles.GetSourcePackage|projects.Export|store.DownloadStoreFolder)");
	Pattern pOK = Pattern.compile(
		"^/fullsync/|^/api/|^/openapi/|^/admin/services/|\\.js$|\\.xml$|\\.pxml$|\\.cxml$|\\.css$|\\.html$|"
		+ "\\.json$|\\.jsonp$|\\.txt$|\\.csv$|\\.htm$|\\.map$");
	
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
					boolean isKO = pKO.matcher(uri).find();
					boolean isOK = pOK.matcher(uri).find();
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

	class GZipServletResponseWrapper extends HttpServletResponseWrapper {

		private GZipServletOutputStream gzipOutputStream = null;
		private PrintWriter             printWriter      = null;

		private GZipServletResponseWrapper(HttpServletResponse response) throws IOException {
			super(response);
		}

		private void close() throws IOException {
			//PrintWriter.close does not throw exceptions.
			//Hence no try-catch block.
			if (printWriter != null) {
				printWriter.close();
			}

			if (gzipOutputStream != null) {
				gzipOutputStream.close();
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

			IOException exception1 = null;
			try {
				if (gzipOutputStream != null) {
					gzipOutputStream.flush();
				}
			} catch(IOException e) {
				exception1 = e;
			}

			IOException exception2 = null;
			try {
				super.flushBuffer();
			} catch(IOException e){
				exception2 = e;
			}

			if (exception1 != null) {
				throw exception1;
			}
			if (exception2 != null) {
				throw exception2;
			}
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (printWriter != null) {
				throw new IllegalStateException("PrintWriter obtained already - cannot get OutputStream");
			}
			if (gzipOutputStream == null) {
				HeaderName.ContentEncoding.addHeader(this, "gzip");
				gzipOutputStream = new GZipServletOutputStream(getResponse().getOutputStream());
			}
			return this.gzipOutputStream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (printWriter == null && gzipOutputStream != null) {
				throw new IllegalStateException("OutputStream obtained already - cannot get PrintWriter");
			}

			if (printWriter == null) {
				HeaderName.ContentEncoding.addHeader(this, "gzip");
				gzipOutputStream = new GZipServletOutputStream(getResponse().getOutputStream());
				printWriter = new PrintWriter(new OutputStreamWriter(gzipOutputStream, getResponse().getCharacterEncoding()));
			}
			return this.printWriter;
		}

		@Override
		public void setContentLength(int len) {
			//ignore, since content length of zipped content
			//does not match content length of unzipped content.
		}		

		@Override
		public void addHeader(String name, String value) {
			if (!HeaderName.ContentLength.is(name)) {
				super.addHeader(name, value);
			}
		}

		@Override
		public void addIntHeader(String name, int value) {
			if (!HeaderName.ContentLength.is(name)) {
				super.addIntHeader(name, value);
			}
		}

		@Override
		public void setHeader(String name, String value) {
			if (!HeaderName.ContentLength.is(name)) {
				super.setHeader(name, value);
			}
		}

		@Override
		public void setIntHeader(String name, int value) {
			if (!HeaderName.ContentLength.is(name)) {
				super.setIntHeader(name, value);
			}
		}
		
		
	}

	class GZipServletOutputStream extends ServletOutputStream {
		private GZIPOutputStream gzipOutputStream = null;

		private GZipServletOutputStream(OutputStream output) throws IOException {
			super();
			gzipOutputStream = new GZIPOutputStream(output);
		}

		@Override
		public void close() throws IOException {
			gzipOutputStream.close();
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
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setWriteListener(WriteListener listener) {
			// TODO Auto-generated method stub
			
		}
	}
}
