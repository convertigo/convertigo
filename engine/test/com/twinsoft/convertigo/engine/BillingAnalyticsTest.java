/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.engine.billing.GoogleAnalyticsTicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;

/**
 * The HTTP ticket manager, against a local collector: the tickets handed together are posted 25 events at most per
 * request, by user, and the collector receives the same events as with one request per ticket.
 */
public class BillingAnalyticsTest {
	private HttpServer collector;
	private final ConcurrentLinkedQueue<JSONObject> requests = new ConcurrentLinkedQueue<>();
	private GoogleAnalyticsTicketManager manager;

	@BeforeClass
	public static void setUpEngine() throws Exception {
		// as in the cloud: the ticket manager makes no request of its own
		System.setProperty("convertigo.cloud.customer_name", "billing-analytics-test");
		BillingBench.setUpEngine();
	}

	@Before
	public void setUp() throws Exception {
		collector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		collector.setExecutor(Executors.newFixedThreadPool(4));
		collector.createContext("/mp/collect", exchange -> {
			try (InputStream in = exchange.getRequestBody()) {
				requests.add(new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
				exchange.sendResponseHeaders(204, -1);
			} catch (Exception e) {
				exchange.sendResponseHeaders(400, -1);
			} finally {
				exchange.close();
			}
		});
		collector.start();
		manager = new GoogleAnalyticsTicketManager("G-TEST", "test", Engine.logBillers);
		Field url = GoogleAnalyticsTicketManager.class.getDeclaredField("url");
		url.setAccessible(true);
		url.set(manager, new URI("http://127.0.0.1:" + collector.getAddress().getPort() + "/mp/collect?measurement_id=G-TEST&api_secret=test"));
	}

	@After
	public void tearDown() throws Exception {
		manager.destroy();
		collector.stop(0);
	}

	private static Ticket ticket(int i, String user) {
		var ticket = new Ticket();
		ticket.setCreationDate(1000 + i);
		ticket.setClientIp("10.0.0." + (i % 7));
		ticket.setUserName(user);
		ticket.setProjectName("Project");
		ticket.setConnectorName(i % 2 == 0 ? "" : "Connector");
		ticket.setConnectorType(i % 2 == 0 ? "" : "HttpConnector");
		ticket.setRequestableName("Requestable" + i);
		ticket.setRequestableType(i % 2 == 0 ? "GenericSequence" : "HttpTransaction");
		ticket.setResponseTime(i);
		ticket.setScore(i % 3);
		ticket.setSessionID("SESSION" + (i % 5));
		ticket.setUserAgent("agent" + (i % 4));
		ticket.setDeviceUUID("device" + (i % 3));
		return ticket;
	}

	private static List<Ticket> tickets() {
		var tickets = new ArrayList<Ticket>();
		String[] users = { "alice", "user", "bob", "", null };
		for (int i = 0; i < 160; i++) {
			tickets.add(ticket(i, users[i % users.length]));
		}
		return tickets;
	}

	/** each event received, with the user of its request, sorted */
	private TreeMap<String, Integer> received() throws Exception {
		var events = new TreeMap<String, Integer>();
		for (var request : requests) {
			var user = request.optString("user_id", "-");
			var array = request.getJSONArray("events");
			for (int i = 0; i < array.length(); i++) {
				var event = array.getJSONObject(i);
				var params = new TreeMap<String, Object>();
				var p = event.getJSONObject("params");
				for (var keys = p.keys(); keys.hasNext();) {
					var key = (String) keys.next();
					params.put(key, p.get(key));
				}
				events.merge(user + " " + request.getString("client_id") + " " + event.getString("name") + " " + params, 1, Integer::sum);
			}
		}
		return events;
	}

	@Test
	public void theTicketsHandedTogetherAreTheSameEventsInFewerRequests() throws Exception {
		for (var ticket : tickets()) {
			manager.addTicket(ticket);
		}
		assertEquals(160, requests.size());
		var alone = received();
		requests.clear();

		assertTrue(manager.addTickets(tickets()));
		var together = received();
		assertEquals(alone, together);
		// alice and bob 32 each, no user 96: 2 + 2 + 4 requests
		assertEquals(8, requests.size());
		for (var request : requests) {
			assertTrue(request.getJSONArray("events").length() <= 25);
		}
	}

	@Test
	public void aTicketThatCannotBeSentIsTheOnlyOneLost() throws Exception {
		var tickets = tickets().subList(0, 10);
		tickets.get(3).setClientIp(null);
		assertTrue(manager.addTickets(new ArrayList<>(tickets)));
		int events = 0;
		for (var request : requests) {
			events += request.getJSONArray("events").length();
		}
		assertEquals(9, events);
	}
}
