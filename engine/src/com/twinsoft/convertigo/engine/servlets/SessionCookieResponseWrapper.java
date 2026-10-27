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
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Response of a request, as seen through {@link SessionSerializationFilter}: holds the session cookie until the
 * response is about to be written, and, with a session store that writes the sessions itself, runs
 * {@code beforeOutput} each time the response can reach the client (output stream or writer obtained, flushed or
 * closed, buffer flushed, error or redirect sent), so that the session is written before.
 */
final class SessionCookieResponseWrapper extends HttpServletResponseWrapper {
	private final String cookieName;
	private final Runnable beforeOutput;
	private Cookie pendingCookie;
	private ServletOutputStream outputStream;
	private PrintWriter writer;

	SessionCookieResponseWrapper(HttpServletResponse response, String cookieName) {
		this(response, cookieName, null);
	}

	SessionCookieResponseWrapper(HttpServletResponse response, String cookieName, Runnable beforeOutput) {
		super(response);
		this.cookieName = cookieName == null || cookieName.isEmpty() ? null : cookieName;
		this.beforeOutput = beforeOutput;
	}

	@Override
	public void addCookie(Cookie cookie) {
		if (cookie != null && cookieName != null && cookieName.equals(cookie.getName())) {
			pendingCookie = cloneCookie(cookie);
			return;
		}
		super.addCookie(cookie);
	}

	void flushSessionCookie() {
		commitSessionCookie();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		beforeOutput();
		commitSessionCookie();
		var stream = super.getOutputStream();
		if (beforeOutput == null) {
			return stream;
		}
		if (outputStream == null) {
			outputStream = new FlushHookOutputStream(stream);
		}
		return outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		beforeOutput();
		commitSessionCookie();
		var printWriter = super.getWriter();
		if (beforeOutput == null) {
			return printWriter;
		}
		if (writer == null) {
			writer = new FlushHookWriter(printWriter);
		}
		return writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		beforeOutput();
		commitSessionCookie();
		super.flushBuffer();
	}

	@Override
	public void sendError(int sc) throws IOException {
		beforeOutput();
		commitSessionCookie();
		super.sendError(sc);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		beforeOutput();
		commitSessionCookie();
		super.sendError(sc, msg);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		beforeOutput();
		commitSessionCookie();
		super.sendRedirect(location);
	}

	private void beforeOutput() {
		if (beforeOutput != null) {
			beforeOutput.run();
		}
	}

	private void commitSessionCookie() {
		if (pendingCookie == null) {
			return;
		}
		try {
			super.addCookie(pendingCookie);
		} catch (IllegalStateException ignored) {
			// Response already committed, we cannot send the cookie anymore
		} finally {
			pendingCookie = null;
		}
	}

	private static Cookie cloneCookie(Cookie source) {
		var copy = new Cookie(source.getName(), source.getValue());
		copy.setComment(source.getComment());
		if (source.getDomain() != null) {
			copy.setDomain(source.getDomain());
		}
		copy.setHttpOnly(source.isHttpOnly());
		copy.setMaxAge(source.getMaxAge());
		if (source.getPath() != null) {
			copy.setPath(source.getPath());
		}
		copy.setSecure(source.getSecure());
		copy.setVersion(source.getVersion());
		return copy;
	}

	/** The output stream of the response, which runs beforeOutput before it is flushed or closed. */
	private final class FlushHookOutputStream extends ServletOutputStream {
		private final ServletOutputStream out;

		private FlushHookOutputStream(ServletOutputStream out) {
			this.out = out;
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			beforeOutput();
			out.flush();
		}

		@Override
		public void close() throws IOException {
			beforeOutput();
			out.close();
		}

		@Override
		public boolean isReady() {
			return out.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			out.setWriteListener(writeListener);
		}
	}

	/** The writer of the response, which runs beforeOutput before it is flushed or closed. */
	private final class FlushHookWriter extends PrintWriter {
		private FlushHookWriter(PrintWriter out) {
			super(out);
		}

		@Override
		public void flush() {
			beforeOutput();
			super.flush();
		}

		@Override
		public void close() {
			beforeOutput();
			super.close();
		}
	}
}
