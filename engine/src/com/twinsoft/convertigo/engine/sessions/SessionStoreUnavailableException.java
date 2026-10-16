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

import jakarta.servlet.http.HttpServletResponse;

/** Raised when a request needs an HTTP session while the Redis session store is not reachable yet. */
public class SessionStoreUnavailableException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SessionStoreUnavailableException(String message) {
		super(message);
	}

	/**
	 * Answers 503 with Retry-After when the throwable comes from an unavailable session store and the response
	 * is not committed yet.
	 *
	 * @return true if the response was written
	 */
	public static boolean respond(HttpServletResponse response, Throwable t) {
		if (response == null || !isCause(t) || response.isCommitted()) {
			return false;
		}
		try {
			response.reset();
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			response.setHeader("Retry-After", "10");
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write("Session store unavailable, retry later");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** True if the throwable or one of its causes is a SessionStoreUnavailableException. */
	public static boolean isCause(Throwable t) {
		for (int depth = 0; t != null && depth < 20; depth++, t = t.getCause() == t ? null : t.getCause()) {
			if (t instanceof SessionStoreUnavailableException) {
				return true;
			}
		}
		return false;
	}
}
