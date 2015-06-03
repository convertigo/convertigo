package com.twinsoft.convertigo.engine.enums;

import javax.servlet.http.HttpSession;

public enum SessionAttribute {
	authenticatedUser;
	
	public void set(HttpSession session, Object value) {
		if (session != null) {
			session.setAttribute(name(), value);
		}
	}
	
	public Object get(HttpSession session) {
		if (session != null) {
			return session.getAttribute(name());
		}
		return null;
	}
	
	public void remove(HttpSession session) {
		if (session != null) {
			session.removeAttribute(name());
		}
	}
	
	public String string(HttpSession session) {
		if (session != null && session.getAttribute(name()) != null) {
			return session.getAttribute(name()).toString();
		} else {
			return null;
		}
	}
}
