/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

import javax.servlet.ServletRequest;

public enum RequestAttribute {
	corsOrigin,
	responseHeader,
	responseStatus;
	
	private RequestAttribute() {
		value = "convertigo." + name();
	}
	
	String value;
	
	public String value() {
		return value;
	}
	
	public boolean has(ServletRequest request) {
		if (request != null) {
			return request.getAttribute(value()) != null;
		}
		return false;
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
