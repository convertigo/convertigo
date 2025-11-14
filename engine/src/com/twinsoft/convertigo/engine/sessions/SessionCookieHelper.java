/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the  Free Software Foundation;  either
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

package com.twinsoft.convertigo.engine.sessions;

import java.security.SecureRandom;
import java.util.HexFormat;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final class SessionCookieHelper {
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final HexFormat HEX = HexFormat.of();

	String resolveSessionId(HttpServletRequest request, String cookieName) {
		if (request == null) {
			return null;
		}
		var cookies = request.getCookies();
		if (cookies != null) {
			for (var cookie : cookies) {
				if (cookieName.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		var header = request.getHeader("X-Convertigo-Session");
		return header == null || header.isBlank() ? null : header.trim();
	}

	void ensureCookie(HttpServletRequest request, HttpServletResponse response, String sessionId, String cookieName) {
		if (response == null || sessionId == null || cookieName == null || cookieName.isEmpty()) {
			return;
		}
		if (request != null) {
			var attributeName = "convertigo.session.cookieSent." + cookieName;
			var already = request.getAttribute(attributeName);
			if (already instanceof String sent && sent.equals(sessionId)) {
				return;
			}
			request.setAttribute(attributeName, sessionId);
		}
		var path = request != null ? request.getContextPath() : "/";
		if (path == null || path.isEmpty()) {
			path = "/";
		}
		var cookie = new Cookie(cookieName, sessionId);
		cookie.setHttpOnly(true);
		if (request != null) {
			cookie.setSecure(request.isSecure());
		}
		if (sessionId.isEmpty()) {
			cookie.setMaxAge(0);
		} else {
			cookie.setMaxAge(-1);
		}
		cookie.setPath(path);
		response.addCookie(cookie);
	}

	String generateSessionId() {
		var bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return HEX.formatHex(bytes).toUpperCase();
	}
}
