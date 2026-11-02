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

package com.twinsoft.convertigo.engine.billing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;

public class GoogleAnalyticsTicketManager implements ITicketManager {
	/** the most events in a request */
	static final int MAX_EVENTS = 25;

	private Logger log;
	private HttpClientInterface http;
	private URI url;
	private String licenceId = null;
	private long nextCheck = 0;
	private volatile String serverHash = Engine.isStudioMode() ? "CONVERTIGO Studio" : "CONVERTIGO Server";

	// bounded timeouts: a dropped packet must never leave a request pending forever
	private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
			.setConnectionRequestTimeout(5000)
			.setConnectTimeout(5000)
			.setSocketTimeout(5000)
			.build();

	public GoogleAnalyticsTicketManager(String measurement_id, String api_secret, Logger log)
			throws BillingException, URISyntaxException {
		if (StringUtils.isAnyBlank(measurement_id, api_secret)) {
			throw new BillingException("Google Analytics ID must not be empty");
		}
		this.log = log;
		log.info("(GoogleAnalyticsTicketManager) initialized");
		http = HttpUtils.makeHttpClient(true);

		if (Engine.isCloudMode()) {
			serverHash = Engine.cloud_customer_name;
		} else {
			// resolved in the background so the engine startup never waits on it
			var resolver = new Thread(() -> {
				var client = http;
				if (client == null) {
					return;
				}
				var get = new HttpGet("http://ifconfig.io");
				get.setConfig(REQUEST_CONFIG);
				get.addHeader("user-agent", "curl");
				try (var response = client.execute(get)) {
					try (var is = response.getEntity().getContent()) {
						var res = IOUtils.toString(is, "UTF-8").trim();
						if (StringUtils.isNotEmpty(res)) {
							serverHash = res;
						}
					}
				} catch (Exception e) {
				}
			}, "GoogleAnalyticsTicketManager.serverHash");
			resolver.setDaemon(true);
			resolver.start();
		}

		url = new URIBuilder("https://www.google-analytics.com/mp/collect")
				.addParameter("measurement_id", measurement_id).addParameter("api_secret", api_secret).build();
	}

	public synchronized void addTicket(Ticket ticket) throws BillingException {
		post(userId(ticket), List.of(ticket));
	}

	/**
	 * The tickets of a same user in a single request, {@link #MAX_EVENTS} at most, the most the collector takes: the
	 * same events as one request each.
	 */
	@Override
	public synchronized boolean addTickets(List<Ticket> tickets) throws BillingException {
		var byUser = new LinkedHashMap<String, List<Ticket>>();
		for (Ticket ticket : tickets) {
			byUser.computeIfAbsent(userId(ticket), user -> new ArrayList<>()).add(ticket);
		}
		for (var entry : byUser.entrySet()) {
			var userTickets = entry.getValue();
			for (int i = 0; i < userTickets.size(); i += MAX_EVENTS) {
				post(entry.getKey(), userTickets.subList(i, Math.min(userTickets.size(), i + MAX_EVENTS)));
			}
		}
		return true;
	}

	private static String userId(Ticket ticket) {
		var user = ticket.getUserName();
		return !"user".equals(user) && StringUtils.isNotBlank(user) ? user : null;
	}

	private void post(String user, List<Ticket> tickets) {
		try {
			var post = new HttpPost(url);
			post.setConfig(REQUEST_CONFIG);
			var json = new JSONObject();
			json.put("client_id", serverHash);
			if (user != null) {
				json.put("user_id", user);
			}
			var events = new JSONArray();
			json.put("events", events);
			for (Ticket ticket : tickets) {
				try {
					events.put(event(ticket));
				} catch (Exception e) {
					// only this one is lost, as when sent alone
					log.error("(GoogleAnalyticsTicketManager) unexpected error: [" + e.getClass().getName() + "] " + e.getMessage());
				}
			}
			if (events.length() == 0) {
				return;
			}

			var body = json.toString();
			if (log.isInfoEnabled()) {
				log.info(body);
			}

			post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

			try (var response = http.execute(post)) {
				log.debug("[" + response.getStatusLine().getStatusCode() + "]");
				// read to its end, the connection is kept for the next request
				EntityUtils.consumeQuietly(response.getEntity());
			} catch (MalformedURLException e) {
				log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient, bad url: " + e.getClass().getName() + "] " + e.getMessage());
			} catch (HttpException e) {
				log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient, Error in HTTP request: " + e.getClass().getName() + "] " + e.getMessage());
			} catch (IOException e) {
				log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient, I/O error: " + e.getClass().getName() + "] " + e.getMessage());
			}
		} catch (Exception e) {
			log.error("(GoogleAnalyticsTicketManager) unexpected error: [" + e.getClass().getName() + "] " + e.getMessage());
		}
	}

	private JSONObject event(Ticket ticket) throws Exception {
		var event = new JSONObject();
		var params = new JSONObject();
		// params.put("debug_mode", 1);
		params.put("session_id", ticket.getSessionID());
		params.put("engagement_time_msec", ticket.getResponseTime());
		if ("session".equals(ticket.getConnectorType())) {
			event.put("name", "session");
			params.put("operation", ticket.getConnectorName());
			params.put("sessions", ticket.getScore());
		} else {
			event.put("name", "requestable");
			params.put("project", ticket.getProjectName());
			params.put("type", ticket.getRequestableType());
			if (StringUtils.isBlank(ticket.getConnectorName())) {
				params.put("requestable", ticket.getRequestableName());
			} else {
				params.put("requestable", ticket.getConnectorName() + "." + ticket.getRequestableName());
				params.put("connector", ticket.getConnectorType());
			}
		}
		params.put("duration", ticket.getResponseTime());
		params.put("ip_hash", DigestUtils.md5Hex(ticket.getClientIp()));
		params.put("server_id", serverHash);
		params.put("device_uuid", ticket.getDeviceUUID());
		params.put("user_agent", ticket.getUserAgent());
		var licence_id = getLicenceId();
		if (licence_id != null) {
			params.put("licence_id", licence_id);
		}
		event.put("params", params);
		return event;
	}

	public synchronized void removeTicket(Ticket ticket) throws BillingException {
		log.warn("(GoogleAnalyticsTicketManager) removeTicket not implemenented");
	}

	public Ticket newTicket() throws BillingException {
		return new Ticket();
	}

	public synchronized void destroy() throws BillingException {
		http = null;
	}

	private synchronized String getLicenceId() {
		long now = System.currentTimeMillis();
		if (now < nextCheck) {
			return licenceId;
		}

		licenceId = null;
		Iterator<?> iter = KeyManager.keys.values().iterator();
		Key seKey = null;
		while (iter.hasNext()) {
			Key key = (Key) iter.next();
			if (key.emulatorID == com.twinsoft.api.Session.EmulIDSE) {
				// check (unlimited key or currentKey expiration date later than previous)
				if ((seKey == null) || (key.expiration == 0) || (key.expiration >= seKey.expiration)) {
					seKey = key;
				}
			}
		}
		if (seKey != null) {
			licenceId = Integer.toString(990000000 + seKey.licence);
		}

		nextCheck = now + 600000;
		return licenceId;
	}
}
