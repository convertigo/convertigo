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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;

final class LegacySessionProvider implements SessionProvider {
	@Override
	public HttpSession getSession(HttpServletRequest request, boolean create) {
		if (request == null) {
			return null;
		}
		return request.getSession(create);
	}

	@Override
	public boolean terminateSession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return false;
		}
		boolean existed = HttpSessionListener.getHttpSession(sessionId) != null;
		HttpSessionListener.terminateSession(sessionId);
		return existed;
	}

	@Override
	public int terminateAllSessions() {
		int count = HttpSessionListener.countSessions();
		HttpSessionListener.removeAllSession();
		return count;
	}
}
