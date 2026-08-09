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

package com.twinsoft.convertigo.engine.admin.services.events;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.events.AdminEvent;
import com.twinsoft.convertigo.engine.admin.events.AdminEventBus;
import com.twinsoft.convertigo.engine.admin.services.Service;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.InstanceIdentity;

@ServiceDefinition(
	name = "Subscribe",
	roles = { Role.WEB_ADMIN },
	parameters = {},
	returnValue = "a server-sent Admin event stream"
)
public class Subscribe implements Service {
	private static final int QUEUE_LIMIT = 256;
	private static final int HEARTBEAT_SECONDS = 15;
	private static final int MAX_STREAM_MINUTES = 10;
	private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
	private static final ConcurrentHashMap<String, StreamControl> activeStreams = new ConcurrentHashMap<>();

	@Override
	public void run(String serviceName, HttpServletRequest request, HttpServletResponse response)
			throws ServiceException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache, no-transform");
		response.setHeader("Connection", "keep-alive");
		response.setHeader("X-Accel-Buffering", "no");

		var topics = requestedTopics(request.getParameter("topics"));
		var control = new StreamControl();
		var streamKey = streamKey(request);
		if (streamKey != null) {
			var previous = activeStreams.put(streamKey, control);
			if (previous != null) {
				previous.cancel();
			}
		}
		try (var subscription = AdminEventBus.subscribe(topics, control::enqueue)) {
			var writer = response.getWriter();
			writer.write("retry: 2000\n\n");
			var reconnecting = request.getHeader("Last-Event-ID") != null
					&& !request.getHeader("Last-Event-ID").isBlank();
			writeEvent(writer, new AdminEvent(
					"ready-" + System.currentTimeMillis(),
					reconnecting ? "admin.resync.required" : "admin.ready",
					System.currentTimeMillis(),
					InstanceIdentity.getLocalInstanceId(),
					reconnecting ? payload("reason", "stream-reconnected") : readyPayload(topics)));
			writer.flush();
			if (writer.checkError()) {
				return;
			}

			var deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(MAX_STREAM_MINUTES);
			while (!control.cancelled.get() && !Thread.currentThread().isInterrupted()
					&& System.nanoTime() < deadline) {
				var event = control.queue.poll(HEARTBEAT_SECONDS, TimeUnit.SECONDS);
				if (control.cancelled.get()) {
					break;
				}
				if (event == null) {
					writer.write(": heartbeat " + System.currentTimeMillis() + "\n\n");
				} else {
					writeEvent(writer, event);
				}
				writer.flush();
				if (writer.checkError()) {
					break;
				}
			}
		} catch (IOException e) {
			// Browser disconnects are the normal way an EventSource subscription ends.
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new ServiceException("Unable to stream Admin events", e);
		} finally {
			if (streamKey != null) {
				activeStreams.remove(streamKey, control);
			}
		}
	}

	@Override
	public boolean isXsrfCheck() {
		return true;
	}

	private static Collection<String> requestedTopics(String raw) {
		if (raw == null || raw.isBlank()) {
			return java.util.List.of("*");
		}
		return Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.toList();
	}

	private static String streamKey(HttpServletRequest request) {
		var client = request.getParameter("client");
		if (client == null || !CLIENT_ID_PATTERN.matcher(client).matches()) {
			return null;
		}
		var session = request.getSession(false);
		return session == null ? null : session.getId() + ":" + client;
	}

	private static JSONObject readyPayload(Collection<String> topics) {
		return payload("topics", topics);
	}

	private static JSONObject payload(String key, Object value) {
		try {
			return new JSONObject().put(key, value);
		} catch (JSONException e) {
			throw new IllegalStateException("Unable to create an Admin event payload", e);
		}
	}

	private static void writeEvent(java.io.PrintWriter writer, AdminEvent event) {
		writer.write("id: " + event.id() + "\n");
		writer.write("data: " + event.toJson().toString().replace("\n", "") + "\n\n");
	}

	private static final class StreamControl {
		private final ArrayBlockingQueue<AdminEvent> queue = new ArrayBlockingQueue<>(QUEUE_LIMIT);
		private final AtomicBoolean cancelled = new AtomicBoolean();

		private void enqueue(AdminEvent event) {
			if (cancelled.get() || queue.offer(event)) {
				return;
			}
			queue.clear();
			queue.offer(new AdminEvent(
					"resync-" + System.currentTimeMillis(),
					"admin.resync.required",
					System.currentTimeMillis(),
					InstanceIdentity.getLocalInstanceId(),
					payload("reason", "subscriber-overflow")));
		}

		private void cancel() {
			cancelled.set(true);
			queue.offer(new AdminEvent("cancel", "admin.cancelled", 0, "", new JSONObject()));
		}
	}
}
