/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

import javax.servlet.http.HttpSession;

public enum SessionAttribute {
	authenticatedUser,
	authenticatedUserGrp,
	authenticatedUserGrpCheck,
	clientIP,
	contexts,
	deviceUUID,
	exception,
	fullSyncRequests,
	isNew,
	httpClient3("__httpClient3__"),
	httpClient4("__httpClient4__"),
	sessionListener,
	xsrfToken,
	userAgent;
	
	String value;
	
	SessionAttribute() {
		value = "__c8o:" + name() + "__";
	}
	
	SessionAttribute(String value) {
		this.value = value;
	}
	
	public String value() {
		return value;
	}
	
	public void set(HttpSession session, Object value) {
		if (session != null) {
			session.setAttribute(value(), value);
		}
	}
	

	public <E> E get(HttpSession session) {
		return get(session, null);
	}
	
	@SuppressWarnings("unchecked")
	public <E> E get(HttpSession session, E defaultValue) {
		E res = session == null ? null : (E) session.getAttribute(value());
		return res == null ? defaultValue : res;
	}
	
	public boolean has(HttpSession session) {
		if (session != null) {
			return session.getAttribute(value()) != null;
		}
		return false;
	}
	
	public void remove(HttpSession session) {
		if (session != null) {
			session.removeAttribute(value());
		}
	}
	
	public String string(HttpSession session) {
		return string(session, null);
	}
	
	public String string(HttpSession session, String defaultValue) {
		Object res = session == null ? null : session.getAttribute(value());
		return res == null ? defaultValue : res.toString();
	}
}
