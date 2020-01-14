/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.twinsoft.convertigo.engine.Engine;

@SuppressWarnings("deprecation")
public class HttpSessionTwsWrapper implements HttpSession {
	HttpSession session;
	String id = "sessionTerminated";

	private HttpSessionTwsWrapper(HttpSession session) {
		this.session = session;
	}

	@Override
	public Object getAttribute(String arg0) {
		try {
			return session.getAttribute(arg0);
		} catch (Exception e) {
			onException(e);
			return null;
		}
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		try {
			return session.getAttributeNames();
		} catch (Exception e) {
			onException(e);
			return Collections.emptyEnumeration();
		}
	}

	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}

	@Override
	public String getId() {
		try {
			return id = session.getId();
		} catch (Exception e) {
			onException(e);
			return id;
		}
	}

	@Override
	public long getLastAccessedTime() {
		try {
			return session.getLastAccessedTime();
		} catch (Exception e) {
			onException(e);
			return 1;
		}
	}

	@Override
	public int getMaxInactiveInterval() {
		try {
			return session.getMaxInactiveInterval();
		} catch (Exception e) {
			onException(e);
			return 1;
		}
	}

	@Override
	public ServletContext getServletContext() {
		try {
			return session.getServletContext();
		} catch (Exception e) {
			onException(e);
			return null;
		}
	}

	@Override
	public HttpSessionContext getSessionContext() {
		try {
			return session.getSessionContext();
		} catch (Exception e) {
			onException(e);
			return null;
		}
	}

	@Override
	public Object getValue(String arg0) {
		try {
			return session.getValue(arg0);
		} catch (Exception e) {
			onException(e);
			return null;
		}
	}

	@Override
	public String[] getValueNames() {
		try {
			return session.getValueNames();
		} catch (Exception e) {
			onException(e);
			return new String[0];
		}
	}

	@Override
	public void invalidate() {
		try {
			session.invalidate();
		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public boolean isNew() {
		try {
			return session.isNew();
		} catch (Exception e) {
			onException(e);
			return false;
		}
	}

	@Override
	public void putValue(String arg0, Object arg1) {
		try {
			session.putValue(arg0, arg1);
		} catch (Exception e) {
			onException(e);
		}

	}

	@Override
	public void removeAttribute(String arg0) {
		try {
			session.removeAttribute(arg0);
		} catch (Exception e) {
			onException(e);
		}

	}

	@Override
	public void removeValue(String arg0) {
		try {
			session.removeValue(arg0);
		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		try {
			session.setAttribute(arg0, arg1);
		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public void setMaxInactiveInterval(int arg0) {
		try {
			session.setMaxInactiveInterval(arg0);
		} catch (Exception e) {
			onException(e);
		}
	}

	protected void onException(Exception e) {
		try {
			if (Engine.logEngine.isTraceEnabled()) {
				Engine.logEngine.trace("(HttpSessionTwsWrapper) onException [" + id + "]", e);
			} else {
				Engine.logEngine.debug("(HttpSessionTwsWrapper) onException [" + id + "]: " + e.getMessage());
			}
		} catch (Exception ex) {
			// ignore if log fail
		}
	}
	
	public static HttpSession wrap(HttpSession session) {
		if (session == null || session instanceof HttpSessionTwsWrapper) {
			return session;
		}
		return new HttpSessionTwsWrapper(session);
	}
}
