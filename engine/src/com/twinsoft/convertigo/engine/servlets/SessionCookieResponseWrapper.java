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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

final class SessionCookieResponseWrapper extends HttpServletResponseWrapper {
	private final String cookieName;
	private Cookie pendingCookie;

	SessionCookieResponseWrapper(HttpServletResponse response, String cookieName) {
		super(response);
		this.cookieName = cookieName;
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
		commitSessionCookie();
		return super.getOutputStream();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		commitSessionCookie();
		return super.getWriter();
	}

	@Override
	public void flushBuffer() throws IOException {
		commitSessionCookie();
		super.flushBuffer();
	}

	@Override
	public void sendError(int sc) throws IOException {
		commitSessionCookie();
		super.sendError(sc);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		commitSessionCookie();
		super.sendError(sc, msg);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		commitSessionCookie();
		super.sendRedirect(location);
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
}
