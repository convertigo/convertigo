/*
 * Copyright (c) 2001-2021 Convertigo SA.
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
	xsrfToken;
	
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
	
	@SuppressWarnings("unchecked")
	public <E> E get(HttpSession session) {
		if (session != null) {
			return (E) session.getAttribute(value());
		}
		return null;
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
		Object obj;
		if (session != null && (obj = session.getAttribute(value())) != null) {
			return obj.toString();
		} else {
			return null;
		}
	}
}
