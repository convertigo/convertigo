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

import java.util.LinkedHashMap;
import java.util.Map;

import com.twinsoft.convertigo.engine.Engine;

/**
 * Reachability of the Redis session store from this instance, as seen by the startup and the
 * instance heartbeat. Only transitions are logged, so an outage gives one warning when it starts
 * and one message when it ends, with its duration.
 */
public final class RedisHealth {
	private static final Object MUTEX = new Object();

	private static Boolean reachable;
	private static long since;
	private static long lastCheck;
	private static String lastError;

	private RedisHealth() {
	}

	static void success() {
		synchronized (MUTEX) {
			long now = System.currentTimeMillis();
			lastCheck = now;
			if (Boolean.TRUE.equals(reachable)) {
				return;
			}
			if (Boolean.FALSE.equals(reachable)) {
				Engine.logEngine.info("(RedisHealth) Redis is reachable again from this instance after "
						+ ((now - since) / 1000L) + " s");
				if (ConvertigoHttpSessionManager.isRedisMode()
						&& !"RedisSessionProvider".equals(ConvertigoHttpSessionManager.getInstance().getProviderName())) {
					Engine.logEngine.warn("(RedisHealth) This instance started while Redis was unreachable: its HTTP sessions"
							+ " stay local and are not shared with the other instances. Restart it to use Redis.");
				}
			}
			reachable = Boolean.TRUE;
			since = now;
			lastError = null;
		}
	}

	static void failure(Throwable t) {
		synchronized (MUTEX) {
			long now = System.currentTimeMillis();
			lastCheck = now;
			lastError = describe(t);
			if (Boolean.FALSE.equals(reachable)) {
				return;
			}
			reachable = Boolean.FALSE;
			since = now;
			Engine.logEngine.warn("(RedisHealth) Redis is unreachable from this instance: " + lastError
					+ ". Requests that use Redis sessions wait or fail until it is reachable again.");
		}
	}

	public static Map<String, Object> toMap() {
		synchronized (MUTEX) {
			var map = new LinkedHashMap<String, Object>();
			map.put("reachable", reachable == null ? "unknown" : reachable.toString());
			map.put("since", since);
			map.put("lastCheck", lastCheck);
			map.put("lastError", lastError != null ? lastError : "");
			return map;
		}
	}

	/**
	 * Most telling cause, as "ExceptionClass: message": a network or TLS error from the chain when there is one
	 * (refused, timeout, unknown host, handshake), otherwise the root cause, without Redisson command dumps.
	 */
	static String describe(Throwable t) {
		if (t == null) {
			return "unknown error";
		}
		Throwable chosen = null;
		var root = t;
		int depth = 0;
		for (var c = t; c != null && depth++ < 20; c = c.getCause() == c ? null : c.getCause()) {
			root = c;
			var name = c.getClass().getName();
			if (chosen == null && (name.startsWith("java.net.") || name.startsWith("javax.net.ssl.")
					|| name.startsWith("java.nio.channels.") || name.startsWith("io.netty.channel."))) {
				chosen = c;
			}
		}
		if (chosen == null) {
			chosen = root;
		}
		var message = chosen.getMessage();
		if (message != null) {
			for (var cut : new String[] { " java.util.concurrent.", "Node source:", ", command:" }) {
				int idx = message.indexOf(cut);
				if (idx > 0) {
					message = message.substring(0, idx);
				}
			}
			message = message.trim();
			if (message.length() > 300) {
				message = message.substring(0, 300) + "...";
			}
		}
		return chosen.getClass().getSimpleName() + (message != null && !message.isBlank() ? ": " + message : "");
	}
}
