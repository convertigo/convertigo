/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of  the  GNU  Affero General Public
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

import java.util.Set;

import com.twinsoft.convertigo.engine.Engine;

final class SessionAttributeSanitizer {
	private static final Set<String> TECHNICAL_PREFIXES = Set.of("session:");

	private SessionAttributeSanitizer() {
	}

	static SessionData sanitize(SessionData session) {
		if (session == null) {
			return null;
		}
		try {
			var attributes = session.getAttributes();
			for (var name : attributes.keySet()) {
				if (isTechnicalAttribute(name)) {
					var copy = SessionData.copyOf(session);
					copy.getAttributes().keySet().removeIf(SessionAttributeSanitizer::isTechnicalAttribute);
					return copy;
				}
			}
			return session;
		} catch (Exception e) {
			if (Engine.logEngine != null) {
				Engine.logEngine.debug("(SessionAttributeSanitizer) Failed to sanitize session " + session.getId(), e);
			}
			return session;
		}
	}

	static boolean isTechnicalAttribute(String name) {
		if (name == null) {
			return false;
		}
		for (var prefix : TECHNICAL_PREFIXES) {
			if (name.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
