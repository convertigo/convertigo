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

package com.twinsoft.convertigo.engine.sessions;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.util.HttpServletRequestTwsWrapper;

public final class StatefulSessionAttributes {
	private StatefulSessionAttributes() {
	}

	public static Object getStatefulAttribute(HttpSession session, String name) {
		if (session == null || name == null) {
			return null;
		}
		if (HttpServletRequestTwsWrapper.unwrapSession(session) instanceof RedisHttpSession redisSession) {
			return redisSession.getStatefulAttribute(name);
		}
		return session.getAttribute(name);
	}

	/**
	 * The stateful attribute of the session, created by the factory once for all the requests of the session on this
	 * engine. Without a session, or with an invalidated session seen through the request wrapper (which ignores it),
	 * the factory value is returned without being kept.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T computeStatefulAttributeIfAbsent(HttpSession session, String name, Supplier<T> factory) {
		if (session == null || name == null) {
			return factory.get();
		}
		var unwrapped = HttpServletRequestTwsWrapper.unwrapSession(session);
		if (unwrapped instanceof RedisHttpSession redisSession) {
			try {
				return (T) redisSession.computeStatefulAttributeIfAbsent(name, factory);
			} catch (IllegalStateException e) {
				if (unwrapped == session) {
					throw e;
				}
				return factory.get();
			}
		}
		synchronized (unwrapped) {
			var value = (T) session.getAttribute(name);
			if (value == null) {
				value = factory.get();
				session.setAttribute(name, value);
			}
			return value;
		}
	}

	public static void setStatefulAttribute(HttpSession session, String name, Object value) {
		if (session == null || name == null) {
			return;
		}
		if (HttpServletRequestTwsWrapper.unwrapSession(session) instanceof RedisHttpSession redisSession) {
			redisSession.setStatefulAttribute(name, value);
			return;
		}
		if (value == null) {
			session.removeAttribute(name);
		} else {
			session.setAttribute(name, value);
		}
	}

	public static void removeStatefulAttribute(HttpSession session, String name) {
		if (session == null || name == null) {
			return;
		}
		if (HttpServletRequestTwsWrapper.unwrapSession(session) instanceof RedisHttpSession redisSession) {
			redisSession.removeStatefulAttribute(name);
			return;
		}
		session.removeAttribute(name);
	}
}
