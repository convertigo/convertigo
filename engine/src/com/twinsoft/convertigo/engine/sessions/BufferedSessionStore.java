package com.twinsoft.convertigo.engine.sessions;

import java.util.HashMap;
import java.util.Map;

/**
 * Decorator that buffers reads/writes for the duration of a thread (request)
 * to avoid multiple serializations and round-trips. Pattern: 1 read at first
 * access, N get/set in memory, 1 write on flush.
 */
final class BufferedSessionStore implements SessionStore {
	private final SessionStore delegate;
	private final ThreadLocal<Map<String, Buffer>> local = ThreadLocal.withInitial(HashMap::new);

	BufferedSessionStore(SessionStore delegate) {
		this.delegate = delegate;
	}

	@Override
	public SessionData read(String sessionId) {
		var map = local.get();
		var buf = map.get(sessionId);
		if (buf != null) {
			return buf.data;
		}
		var data = delegate.read(sessionId);
		if (data != null) {
			map.put(sessionId, new Buffer(data));
		}
		return data;
	}

	@Override
	public void save(SessionData session) {
		if (session == null) {
			return;
		}
		var map = local.get();
		var existing = map.get(session.getId());
		if (existing != null) {
			existing.data = session;
			existing.dirty = true;
		} else {
			map.put(session.getId(), new Buffer(session, true));
		}
	}

	@Override
	public void delete(String sessionId) {
		local.get().remove(sessionId);
		delegate.delete(sessionId);
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	/**
	 * Flush buffered sessions for current thread. Called at end of request.
	 */
	void flushThread() {
		var map = local.get();
		if (map.isEmpty()) {
			return;
		}
		for (var entry : map.entrySet()) {
			var buf = entry.getValue();
			if (buf.dirty) {
				delegate.save(buf.data);
			}
		}
		map.clear();
	}

	private static final class Buffer {
		SessionData data;
		boolean dirty;

		Buffer(SessionData data) {
			this(data, false);
		}

		Buffer(SessionData data, boolean dirty) {
			this.data = data;
			this.dirty = dirty;
		}
	}
}
