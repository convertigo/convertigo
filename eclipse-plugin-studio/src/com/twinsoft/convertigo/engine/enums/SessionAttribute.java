package com.twinsoft.convertigo.engine.enums;

import javax.servlet.http.HttpSession;

public enum SessionAttribute {
	authenticatedUser,
	authenticatedUserGrp,
	authenticatedUserGrpCheck,
	httpClient3("__httpClient3__"),
	httpClient4("__httpClient4__");
	
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
