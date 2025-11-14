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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.twinsoft.convertigo.engine.Engine;

final class RedisSessionStore implements SessionStore {
	private final RedissonClient client;
	private final RedisSessionConfiguration configuration;

	RedisSessionStore(RedisSessionConfiguration configuration) {
		this.configuration = configuration;
		this.client = createClient(configuration);
	}

	private RedissonClient createClient(RedisSessionConfiguration cfg) {
		var config = new Config();
		var singleServer = config.useSingleServer()
				.setAddress(cfg.getAddress())
				.setDatabase(cfg.getDatabase())
				.setTimeout(cfg.getTimeoutMillis());
		var username = cfg.getUsername();
		if (username != null) {
			singleServer.setUsername(username);
		}
		var password = cfg.getPassword();
		if (password != null) {
			singleServer.setPassword(password);
		}
		return Redisson.create(config);
	}

	@Override
	public SessionData read(String sessionId) {
		try {
			var bucket = bucket(sessionId);
			var payload = bucket.get();
			if (payload == null) {
				debug("MISS " + sessionId);
				return null;
			}
			try (var bais = new ByteArrayInputStream(payload); var ois = new ObjectInputStream(bais)) {
				debug("HIT " + sessionId);
				return (SessionData) ois.readObject();
			}
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to read session " + sessionId, e);
			return null;
		}
	}

	@Override
	public void save(SessionData session) {
		if (session == null) {
			return;
		}
		try {
			var sanitized = SessionAttributeSanitizer.sanitize(session);
			var bucket = bucket(session.getId());
			var ttlSeconds = session.getMaxInactiveInterval() > 0
					? session.getMaxInactiveInterval()
					: configuration.getDefaultTtlSeconds();
			var lifespan = ttlSeconds > 0 ? Duration.ofSeconds(ttlSeconds) : null;
			try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
				oos.writeObject(sanitized);
				oos.flush();
				if (lifespan != null) {
					bucket.set(baos.toByteArray(), lifespan);
				} else {
					bucket.set(baos.toByteArray());
				}
				debug("SAVE " + session.getId() + ", ttl=" + ttlSeconds);
			}
		} catch (IOException e) {
			log("(RedisSessionStore) Failed to serialize session " + session.getId(), e);
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to save session " + session.getId(), e);
		}
	}

	@Override
	public void delete(String sessionId) {
		try {
			bucket(sessionId).delete();
			debug("DEL " + sessionId);
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to delete session " + sessionId, e);
		}
	}

	@Override
	public void shutdown() {
		try {
			client.shutdown();
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to shutdown Redis client", e);
		}
	}

	private RBucket<byte[]> bucket(String sessionId) {
		return client.getBucket(configuration.key(sessionId));
	}

	private void log(String message, Exception e) {
		try {
			if (Engine.logEngine != null) {
				Engine.logEngine.warn(message, e);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	private void debug(String message) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(RedisSessionStore) " + message);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}
}
