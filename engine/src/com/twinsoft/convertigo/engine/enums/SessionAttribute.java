/*
 * Copyright (c) 2001-2025 Convertigo SA.
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
	authenticatedUser(String.class),
	authenticatedUserGrp(String.class),
	authenticatedUserGrpCheck(String.class),
	clientIP(String.class, KeepMode.NEVER),
	contexts(java.util.List.class, KeepMode.ON_DEMAND),
	deviceUUID(String.class),
	exception(Throwable.class),
	fullSyncRequests(Object.class),
	isNew(Boolean.class, KeepMode.NEVER),
	httpClient3("__httpClient3__", Object.class),
	httpClient4("__httpClient4__", Object.class),
	sessionListener(com.twinsoft.convertigo.engine.requesters.HttpSessionListener.class, KeepMode.NEVER),
	xsrfToken(String.class),
	userAgent(String.class, KeepMode.NEVER);
	
	String value;
	Class<?> expectedClass;
	KeepMode keepMode;
	
	SessionAttribute() {
		this(null, null, KeepMode.ALWAYS, true);
	}
	
	SessionAttribute(Class<?> expectedClass) {
		this(null, expectedClass, KeepMode.ALWAYS, true);
	}
	
	SessionAttribute(Class<?> expectedClass, KeepMode keepMode) {
		this(null, expectedClass, keepMode, true);
	}
	
	SessionAttribute(String value) {
		this(value, null, KeepMode.ALWAYS, false);
	}

	SessionAttribute(String value, Class<?> expectedClass) {
		this(value, expectedClass, KeepMode.ALWAYS, false);
	}

	SessionAttribute(String value, Class<?> expectedClass, KeepMode keepMode) {
		this(value, expectedClass, keepMode, false);
	}

	SessionAttribute(String value, Class<?> expectedClass, KeepMode keepMode, boolean autoPrefix) {
		this.value = autoPrefix ? "__c8o:" + this.name() + "__" : value;
		this.expectedClass = expectedClass;
		this.keepMode = keepMode == null ? KeepMode.ALWAYS : keepMode;
	}
	
	public String value() {
		return value;
	}

	public Class<?> expectedClass() {
		return expectedClass;
	}

	public KeepMode keepMode() {
		return keepMode;
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

	public static SessionAttribute fromValue(String value) {
		if (value == null) {
			return null;
		}
		for (var attr : values()) {
			if (attr.value.equals(value)) {
				return attr;
			}
		}
		return null;
	}

	public enum KeepMode {
		ALWAYS, NEVER, ON_DEMAND;

		public boolean shouldKeep(boolean enableOnDemand) {
			return this == ALWAYS || (this == ON_DEMAND && enableOnDemand);
		}
	}
}
