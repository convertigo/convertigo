/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sessions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Provider used while the instance waits for Redis: requests without session keep working, requests that
 * need a session (to create one or to use the one of their cookie) are refused, and answered with 503.
 */
final class UnavailableSessionProvider implements SessionProvider {
	@Override
	public HttpSession getSession(HttpServletRequest request, boolean create) {
		if (create || (request != null && request.getRequestedSessionId() != null)) {
			throw new SessionStoreUnavailableException("The Redis session store is not reachable yet");
		}
		return null;
	}
}
