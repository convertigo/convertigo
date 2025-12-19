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

import javax.servlet.http.HttpSession;

public final class StatefulSessionAttributes {
	private StatefulSessionAttributes() {
	}

	public static Object getStatefulAttribute(HttpSession session, String name) {
		if (session == null || name == null) {
			return null;
		}
		if (session instanceof RedisHttpSession redisSession) {
			return redisSession.getStatefulAttribute(name);
		}
		return session.getAttribute(name);
	}

	public static void setStatefulAttribute(HttpSession session, String name, Object value) {
		if (session == null || name == null) {
			return;
		}
		if (session instanceof RedisHttpSession redisSession) {
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
		if (session instanceof RedisHttpSession redisSession) {
			redisSession.removeStatefulAttribute(name);
			return;
		}
		session.removeAttribute(name);
	}
}
