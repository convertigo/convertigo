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

package com.twinsoft.convertigo.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface;

/**
 * Engine-local "context" HTTP pools, by context ID, used with a shared session store: a context is then read from the
 * store at each request, so a pool kept by the context object would be lost after each request.
 * A pool is closed once unused for the context timeout.
 */
final class ContextHttpPools {
	static final long SWEEP_INTERVAL_MILLIS = 60_000L;
	private static final long MIN_IDLE_MILLIS = 60_000L;

	private static final Map<String, Entry> pools = new ConcurrentHashMap<>();
	private static final AtomicLong nextSweep = new AtomicLong();

	private static final class Entry {
		private HttpClient httpClient3;
		private HttpClientInterface httpClient4;
		private long lastUse;
		private long idleMillis;
	}

	private ContextHttpPools() {
	}

	static HttpClient httpClient3(String contextID, long idleMillis) {
		return httpClient3(contextID, idleMillis, System.currentTimeMillis(), () -> HttpUtils.makeHttpClient3(true));
	}

	static HttpClientInterface httpClient4(String contextID, long idleMillis) {
		return httpClient4(contextID, idleMillis, System.currentTimeMillis(), () -> HttpUtils.makeHttpClient(true));
	}

	static HttpClient httpClient3(String contextID, long idleMillis, long now, Supplier<HttpClient> factory) {
		return touch(contextID, idleMillis, now, e -> {
			if (e.httpClient3 == null) {
				if (Engine.logContext != null) {
					Engine.logContext.debug("Create a new context HTTP pool for " + contextID);
				}
				e.httpClient3 = factory.get();
			}
		}).httpClient3;
	}

	static HttpClientInterface httpClient4(String contextID, long idleMillis, long now, Supplier<HttpClientInterface> factory) {
		return touch(contextID, idleMillis, now, e -> {
			if (e.httpClient4 == null) {
				e.httpClient4 = factory.get();
			}
		}).httpClient4;
	}

	private static Entry touch(String contextID, long idleMillis, long now, java.util.function.Consumer<Entry> init) {
		sweep(now);
		return pools.compute(contextID, (key, current) -> {
			Entry e = current == null ? new Entry() : current;
			init.accept(e);
			e.lastUse = now;
			e.idleMillis = Math.max(idleMillis, MIN_IDLE_MILLIS);
			return e;
		});
	}

	static void release(String contextID) {
		Entry e = contextID == null ? null : pools.remove(contextID);
		if (e != null) {
			shutdown(e);
		}
	}

	static void sweep(long now) {
		long next = nextSweep.get();
		if (now < next || !nextSweep.compareAndSet(next, now + SWEEP_INTERVAL_MILLIS)) {
			return;
		}
		for (String contextID : pools.keySet()) {
			Entry[] removed = new Entry[1];
			pools.computeIfPresent(contextID, (key, e) -> {
				if (now - e.lastUse > e.idleMillis) {
					removed[0] = e;
					return null;
				}
				return e;
			});
			if (removed[0] != null) {
				shutdown(removed[0]);
			}
		}
	}

	static int size() {
		return pools.size();
	}

	static void clear() {
		pools.values().forEach(ContextHttpPools::shutdown);
		pools.clear();
		nextSweep.set(0);
	}

	private static void shutdown(Entry e) {
		try {
			if (e.httpClient3 != null && e.httpClient3.getHttpConnectionManager() instanceof MultiThreadedHttpConnectionManager manager) {
				manager.shutdown();
			}
		} catch (Exception ex) {
			if (Engine.logContext != null) {
				Engine.logContext.debug("Failed to close a context HTTP pool: " + ex.getMessage());
			}
		}
	}
}
