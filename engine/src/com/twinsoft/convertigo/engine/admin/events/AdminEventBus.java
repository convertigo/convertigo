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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.events;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.redisson.api.RTopic;
import org.redisson.client.codec.StringCodec;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.RedisClients;
import com.twinsoft.convertigo.engine.util.InstanceIdentity;

/**
 * Process-local Admin event bus with optional Redis fan-out.
 * <p>
 * Topics are domain names rather than UI commands. The SSE transport is only
 * one consumer, so other Admin surfaces can reuse this bus later.
 */
public final class AdminEventBus {
	public static final String PROJECTS_CHANGED = "projects.changed";

	private static final Pattern TOPIC_PATTERN = Pattern.compile("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*");
	private static final String REDIS_TOPIC_SUFFIX = "admin:events";
	private static final List<LocalSubscription> subscriptions = new CopyOnWriteArrayList<>();
	private static final Object redisMutex = new Object();

	private static volatile RTopic redisTopic;
	private static volatile int redisListenerId = -1;

	private AdminEventBus() {
	}

	public interface Subscription extends AutoCloseable {
		@Override
		void close();
	}

	public static JSONObject publish(String topic, JSONObject payload) {
		var normalizedTopic = normalizeTopic(topic);
		var event = new AdminEvent(
				UUID.randomUUID().toString(),
				normalizedTopic,
				System.currentTimeMillis(),
				InstanceIdentity.getLocalInstanceId(),
				payload);
		dispatchLocal(event);
		publishRedis(event);
		return event.toJson();
	}

	public static JSONObject publishProjectChanged(String project, String qname, String scope, String reason) {
		try {
			var payload = new JSONObject()
					.put("project", trim(project))
					.put("qname", trim(qname))
					.put("scope", trim(scope))
					.put("reason", trim(reason));
			return publish(PROJECTS_CHANGED, payload);
		} catch (JSONException e) {
			throw new IllegalStateException("Unable to create a project change event", e);
		}
	}

	public static Subscription subscribe(Collection<String> topics, Consumer<AdminEvent> listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Admin event listener is required");
		}
		var subscription = new LocalSubscription(normalizeFilters(topics), listener);
		subscriptions.add(subscription);
		ensureRedisListener();
		return subscription;
	}

	static boolean matches(Collection<String> filters, String topic) {
		if (filters == null || filters.isEmpty()) {
			return true;
		}
		for (var filter : filters) {
			if ("*".equals(filter) || filter.equals(topic)) {
				return true;
			}
			if (filter.endsWith(".*") && topic.startsWith(filter.substring(0, filter.length() - 1))) {
				return true;
			}
		}
		return false;
	}

	private static List<String> normalizeFilters(Collection<String> topics) {
		if (topics == null || topics.isEmpty()) {
			return List.of("*");
		}
		return topics.stream()
				.map(AdminEventBus::normalizeFilter)
				.distinct()
				.toList();
	}

	private static String normalizeFilter(String topic) {
		var value = trim(topic).toLowerCase();
		if ("*".equals(value)) {
			return value;
		}
		if (value.endsWith(".*")) {
			normalizeTopic(value.substring(0, value.length() - 2));
			return value;
		}
		return normalizeTopic(value);
	}

	private static String normalizeTopic(String topic) {
		var value = trim(topic).toLowerCase();
		if (!TOPIC_PATTERN.matcher(value).matches()) {
			throw new IllegalArgumentException("Invalid Admin event topic: " + topic);
		}
		return value;
	}

	private static void dispatchLocal(AdminEvent event) {
		for (var subscription : subscriptions) {
			subscription.accept(event);
		}
	}

	private static void publishRedis(AdminEvent event) {
		if (!ConvertigoHttpSessionManager.isRedisMode()) {
			return;
		}
		try {
			getRedisTopic().publish(event.toJson().toString());
		} catch (Exception e) {
			Engine.logRedis.warn("(AdminEventBus) Failed to publish " + event.topic(), e);
		}
	}

	private static void ensureRedisListener() {
		if (!ConvertigoHttpSessionManager.isRedisMode() || redisListenerId != -1) {
			return;
		}
		synchronized (redisMutex) {
			if (redisListenerId != -1) {
				return;
			}
			try {
				var topic = getRedisTopic();
				redisListenerId = topic.addListener(String.class, (channel, message) -> {
					try {
						var event = AdminEvent.fromJson(message);
						if (!InstanceIdentity.getLocalInstanceId().equals(event.instance())) {
							dispatchLocal(event);
						}
					} catch (Exception e) {
						Engine.logRedis.warn("(AdminEventBus) Ignoring an invalid event", e);
					}
				});
			} catch (Exception e) {
				Engine.logRedis.warn("(AdminEventBus) Failed to subscribe to Redis", e);
			}
		}
	}

	private static RTopic getRedisTopic() {
		var topic = redisTopic;
		if (topic != null) {
			return topic;
		}
		synchronized (redisMutex) {
			if (redisTopic == null) {
				var key = RedisClients.getConfiguration().getContextKeyPrefix() + REDIS_TOPIC_SUFFIX;
				redisTopic = RedisClients.getClient().getTopic(key, StringCodec.INSTANCE);
			}
			return redisTopic;
		}
	}

	private static void releaseRedisListenerIfIdle() {
		if (!subscriptions.isEmpty() || redisListenerId == -1) {
			return;
		}
		synchronized (redisMutex) {
			if (!subscriptions.isEmpty() || redisListenerId == -1) {
				return;
			}
			try {
				if (redisTopic != null) {
					redisTopic.removeListener(redisListenerId);
				}
			} catch (Exception e) {
				Engine.logRedis.debug("(AdminEventBus) Failed to remove Redis listener", e);
			} finally {
				redisListenerId = -1;
				redisTopic = null;
			}
		}
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private static final class LocalSubscription implements Subscription {
		private final List<String> filters;
		private final Consumer<AdminEvent> listener;
		private final AtomicBoolean closed = new AtomicBoolean();

		private LocalSubscription(List<String> filters, Consumer<AdminEvent> listener) {
			this.filters = filters;
			this.listener = listener;
		}

		private void accept(AdminEvent event) {
			if (closed.get() || !matches(filters, event.topic())) {
				return;
			}
			try {
				listener.accept(event);
			} catch (Exception e) {
				Engine.logAdmin.debug("(AdminEventBus) Event consumer failed", e);
			}
		}

		@Override
		public void close() {
			if (closed.compareAndSet(false, true)) {
				subscriptions.remove(this);
				releaseRedisListenerIfIdle();
			}
		}
	}
}
