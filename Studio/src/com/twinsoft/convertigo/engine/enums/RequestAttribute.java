package com.twinsoft.convertigo.engine.enums;

import javax.servlet.ServletRequest;

public enum RequestAttribute {
	responseHeader;
	
	private RequestAttribute() {
		value = "convertigo." + name();
	}
	
	String value;
	
	public String value() {
		return value;
	}
	
	public void set(ServletRequest request, Object value) {
		if (request != null) {
			request.setAttribute(value(), value);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <E> E get(ServletRequest request) {
		if (request != null) {
			return (E) request.getAttribute(value());
		}
		return null;
	}
	
	public void remove(ServletRequest request) {
		if (request != null) {
			request.removeAttribute(value());
		}
	}
	
	public String string(ServletRequest request) {
		if (request != null && request.getAttribute(value()) != null) {
			return request.getAttribute(value()).toString();
		} else {
			return null;
		}
	}
}
