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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute.KeepMode;

final class SessionAttributeFilter {
	private static final Set<String> TECHNICAL_PREFIXES = Set.of("session:");
	private static final boolean ENABLE_ON_DEMAND = Boolean
			.parseBoolean(System.getProperty("convertigo.engine.session.keep.on_demand", "false"));

	private SessionAttributeFilter() {
	}

	static Object sanitizeValue(RedisHttpSession session, String name, Object value) {
		if (name == null || value == null) {
			return null;
		}
		if (SessionAttribute.contexts.value().equals(name) && value instanceof List<?> list) {
			persistContexts(session, list);
			return null;
		}
		if (isTechnicalAttribute(name)) {
			return null;
		}
		if (!shouldKeep(name)) {
			return null;
		}
		if (value instanceof Document) {
			return null;
		}
		return value;
	}

	private static void persistContexts(RedisHttpSession session, List<?> list) {
		try {
			int ttl = resolveContextTtl(session, list);
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug(
						"(SessionAttributeFilter) Persist contexts separately, ttl=" + ttl + ", sessionTtl="
								+ session.getMaxInactiveInterval());
			}
				var contexts = new ArrayList<Context>(list.size());
				for (var item : list) {
					if (item instanceof Context ctx) {
						if (ctx.isMarkedForRemoval()) {
							continue;
						}
						contexts.add(ctx);
					}
				}
			if (!contexts.isEmpty()) {
				Engine.theApp.contextManager.saveContexts(contexts, ttl);
			}
		} catch (Exception e) {
			if (Engine.logEngine != null) {
				Engine.logEngine.warn("(SessionAttributeFilter) Failed to persist contexts for redis store", e);
			}
		}
	}

	private static int resolveContextTtl(RedisHttpSession session, List<?> list) {
		int sessionTtl = session.getMaxInactiveInterval();
		int projectContextTtl = 0;
		if (!list.isEmpty()) {
			Object first = list.get(0);
			String projectName = null;
			if (first instanceof Context ctx) {
				projectName = ctx.projectName;
				if (ctx.project != null) {
					projectContextTtl = ctx.project.getContextTimeout();
				}
			}
			if (projectName != null && projectContextTtl <= 0) {
				try {
					var project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
					if (project != null) {
						projectContextTtl = project.getContextTimeout();
					}
				} catch (Exception ignore) {
					// ignore resolution error
				}
			}
		}
		if (sessionTtl > 0 && projectContextTtl > 0) {
			return Math.min(sessionTtl, projectContextTtl);
		}
		return sessionTtl > 0 ? sessionTtl : projectContextTtl;
	}

	static boolean isTechnicalAttribute(String name) {
		if (name == null) {
			return false;
		}
		for (String prefix : TECHNICAL_PREFIXES) {
			if (name.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static boolean shouldKeep(String name) {
		var attr = SessionAttribute.fromValue(name);
		if (attr == null) {
			return true;
		}
		KeepMode mode = attr.keepMode();
		return mode == null || mode.shouldKeep(ENABLE_ON_DEMAND);
	}
}
