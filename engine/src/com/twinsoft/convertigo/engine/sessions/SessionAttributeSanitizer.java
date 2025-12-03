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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute.KeepMode;

final class SessionAttributeSanitizer {
	private static final Set<String> TECHNICAL_PREFIXES = Set.of("session:");
	private static final boolean ENABLE_ON_DEMAND = Boolean
			.parseBoolean(System.getProperty("convertigo.engine.session.keep.on_demand", "false"));

	private SessionAttributeSanitizer() {
	}

	static SessionData sanitize(SessionData session) {
		if (session == null) {
			return null;
		}
		try {
			var copy = SessionData.copyOf(session);
			var attributes = copy.getAttributes();
			attributes.keySet().removeIf(SessionAttributeSanitizer::isTechnicalAttribute);
			attributes.keySet().removeIf(name -> !shouldKeep(name));
			sanitizeContexts(attributes);
			sanitizeDom(attributes);
			attributes.remove("__c8o:sessionListener__");
			attributes.remove("__c8o:clientIP__");
			attributes.remove("__c8o:userAgent__");
			attributes.remove("__c8o:isNew__");
			return copy;
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

	private static void sanitizeContexts(Map<String, Object> attributes) {
		var ctxValue = attributes.get("__c8o:contexts__");
		if (ctxValue instanceof List<?> list) {
			var lightContexts = new ArrayList<ContextSummary>(list.size());
			for (var item : list) {
				if (item instanceof Context ctx) {
					var light = new ContextSummary();
					light.contextId = ctx.contextID;
					light.name = ctx.name;
					light.projectName = ctx.projectName;
					light.connectorName = ctx.connectorName;
					light.transactionName = ctx.transactionName;
					light.sequenceName = ctx.sequenceName;
					light.isDestroying = false; // avoid propagating destroy flag
					light.isErrorDocument = ctx.isErrorDocument;
					light.isNewSession = ctx.isNewSession;
					if (ctx.httpState != null) {
						light.httpStateCookiePolicy = ctx.httpState.getCookiePolicy();
						var cookies = ctx.httpState.getCookies();
						light.httpStateCookies = cookies != null ? cookies.length : 0;
					}
					light.trimNulls();
					lightContexts.add(light);
				}
			}
			attributes.put("__c8o:contexts__", lightContexts);
		}
	}

	private static void sanitizeDom(Map<String, Object> attributes) {
		attributes.entrySet().removeIf(entry -> entry.getValue() instanceof org.w3c.dom.Document);
	}

	private static boolean shouldKeep(String name) {
		var attr = SessionAttribute.fromValue(name);
		if (attr == null) {
			return true; // unknown attributes: keep by default
		}
		KeepMode mode = attr.keepMode();
		return mode == null || mode.shouldKeep(ENABLE_ON_DEMAND);
	}

	private static final class ContextSummary implements Serializable {
	@Serial
		private static final long serialVersionUID = 1L;

		// Public fields to allow lightweight JSON serialization without Java
		// serialization overhead.
		public String contextId;
		public String name;
		public String projectName;
		public String connectorName;
		public String transactionName;
		public String sequenceName;
		public boolean isDestroying;
		public boolean isErrorDocument;
		public boolean isNewSession;
		public int httpStateCookiePolicy;
		public int httpStateCookies;

		void trimNulls() {
			if (connectorName != null && connectorName.isEmpty()) connectorName = null;
			if (transactionName != null && transactionName.isEmpty()) transactionName = null;
			if (sequenceName != null && sequenceName.isEmpty()) sequenceName = null;
			if (name != null && name.isEmpty()) name = null;
			if (projectName != null && projectName.isEmpty()) projectName = null;
		}
	}
}
