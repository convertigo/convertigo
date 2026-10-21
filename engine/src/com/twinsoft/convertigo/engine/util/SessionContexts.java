/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.util.ArrayList;
import java.util.List;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import jakarta.servlet.http.HttpSession;

/** Session facades need not have object identity; lock publication by session id, then the shared list. */
public final class SessionContexts {
	private static final Object[] locks = new Object[64];
	static { for (int i = 0; i < locks.length; i++) locks[i] = new Object(); }
	private SessionContexts() { }
	private static Object lock(HttpSession session) { return locks[session.getId().hashCode() & (locks.length - 1)]; }

	public static void add(HttpSession session, Context context) {
		synchronized (lock(session)) {
			List<Context> list = SessionAttribute.contexts.get(session);
			if (list == null) {
				list = new ArrayList<>();
				SessionAttribute.contexts.set(session, list);
			}
			synchronized (list) {
				list.removeIf(value -> value == null);
				if (!list.contains(context)) list.add(context);
			}
			context.contextListSession = session;
		}
	}

	public static void remove(HttpSession session, String contextId) {
		synchronized (lock(session)) {
			List<Context> list = SessionAttribute.contexts.get(session);
			if (list != null) synchronized (list) {
				list.removeIf(value -> value == null || contextId.equals(value.contextID));
			}
		}
	}

	public static List<Context> snapshot(HttpSession session) {
		synchronized (lock(session)) {
			List<Context> list = SessionAttribute.contexts.get(session);
			if (list == null) return List.of();
			synchronized (list) {
				var copy = new ArrayList<>(list);
				copy.removeIf(value -> value == null);
				return copy;
			}
		}
	}
}
