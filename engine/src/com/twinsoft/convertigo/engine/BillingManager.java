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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.spi.NOPLogger;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.GoogleAnalyticsTicketManager;
import com.twinsoft.convertigo.engine.billing.HibernateTicketManager;
import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.billing.TicketQueue;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.RequestScopedHttpSession;
import com.twinsoft.convertigo.engine.util.Crypto2;

public class BillingManager implements AbstractManager, PropertyChangeEventListener {

	static private final String[] keys = Crypto2
			.decodeFromHexString(EnginePropertiesManager.PropertyName.CRYPTO_PASSPHRASE.getDefaultValue(),
					"xead864b734a0400bf0dac127fe32013367869fb7fb429bf22bf27e695dc52f270828ce7b5384765f0ed7c25a623e9c50")
			.split(":");
	static private final String dkey = Crypto2.decodeFromHexString("xffd37e28952823f1be24b330ad153a72");

	private volatile String customer_name = "";

	/**
	 * One queue and one writer per ticket manager, replaced as a whole by renewManager: the requests bill without a lock,
	 * only reading it. Empty when no ticket manager is active.
	 */
	private volatile List<TicketQueue> queues = List.of();

	private boolean serverMonitor = false;

	public void init() throws EngineException {
		customer_name = Engine.isCloudMode() ? Engine.cloud_customer_name
				: (Engine.isStudioMode() ? "CONVERTIGO Studio" : "CONVERTIGO Server");
		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);

		serverMonitor = !dkey.equals(EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_GOOGLE_ID))
				&& Engine.isEngineMode();

		if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_PERSISTENCE_ENABLED)
				|| EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_GOOGLE_ENABLED)
				|| serverMonitor) {
			renewManager(false);
		}
	}

	/** Stops the billing: the tickets already made are written first, for a few seconds at most. */
	public void destroy() throws EngineException {
		stop(true);
	}

	private void stop(boolean wait) throws EngineException {
		Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
		renewManager(true, wait);
	}

	public boolean hasActiveManagers() {
		return !queues.isEmpty();
	}

	/** tickets made and not written yet by all the ticket managers */
	int pendingTickets() {
		int pending = 0;
		for (TicketQueue queue : queues) {
			pending += queue.size();
		}
		return pending;
	}

	/** Uses these ticket managers instead of the configured ones (tests and benchmarks). */
	synchronized void useTicketManagers(List<? extends ITicketManager> managers) {
		var previous = queues;
		queues = startQueues(managers);
		close(previous, false);
	}

	private static List<TicketQueue> startQueues(List<? extends ITicketManager> managers) {
		int capacity;
		try {
			capacity = Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_QUEUE_CAPACITY).trim());
		} catch (Exception e) {
			capacity = Integer.parseInt(PropertyName.ANALYTICS_QUEUE_CAPACITY.getDefaultValue());
		}
		var queues = new ArrayList<TicketQueue>(managers.size());
		for (ITicketManager manager : managers) {
			queues.add(new TicketQueue(manager, capacity, Engine.logBillers));
		}
		return List.copyOf(queues);
	}

	private static void close(List<TicketQueue> queues, boolean wait) {
		// all of them stop taking tickets and write their last ones at the same time
		for (TicketQueue queue : queues) {
			queue.close(false);
		}
		if (wait) {
			for (TicketQueue queue : queues) {
				queue.close(true);
			}
		}
	}

	public void insertBilling(Context context) throws EngineException {
		insertBilling(context, null, null);
	}

	public void insertBilling(Context context, Long responseTime, Long score) throws EngineException {
		if (!hasActiveManagers()) {
			return;
		}
		if (context == null) {
			return;
		}
		if (context.requestedObject == null) {
			return;
		}

		try {
			String username = context.getAuthenticatedUser();
			username = username == null ? (String) context.get("username") : username;
			username = username == null ? context.tasUserName : username;
			username = username == null ? "user" : username;

			var ticket = createTicket(new com.twinsoft.convertigo.engine.requesters.RequestableInvocation.Billing(
                context.remoteAddr, username, context.projectName, context.connectorName == null ? "" : context.connectorName,
                context.connector == null ? "" : context.connector.getClass().getSimpleName(),
                context.requestedObject.getName(), context.requestedObject.getClass().getSimpleName(),
                responseTime == null ? context.statistics.getLatestDuration(EngineStatistics.GET_DOCUMENT) : responseTime,
                score == null ? context.requestedObject.getScore() : score, context.httpSession.getId(), context.userAgent,
                (String) context.httpSession.getAttribute(Parameter.UIid.name()),
                (String) context.httpSession.getAttribute(Parameter.DeviceUUID.name())));

			if (context.nativeRows != null) {
				// Capture only a ticket. The request Context/session may be reset before
				// the servlet consumes its response; late work still belongs to this ticket.
				com.twinsoft.convertigo.engine.billing.RowBilling.defer(context.nativeRows, ticket, this::enqueueTicket);
			} else enqueueTicket(ticket);
		} catch (Exception e) {
			throw new EngineException("Ticket create failed", e);
		}
	}

    /** Lightweight invocations use the same ticket construction and queue as managed requesters. */
    public void insertBilling(com.twinsoft.convertigo.engine.requesters.RequestableInvocation.Billing value)
            throws EngineException {
        if (!hasActiveManagers() || value == null) return;
        try { enqueueTicket(createTicket(value)); }
        catch (Exception e) { throw new EngineException("Ticket create failed", e); }
    }

    protected Ticket createTicket(com.twinsoft.convertigo.engine.requesters.RequestableInvocation.Billing value) {
        var ticket = new Ticket();
        ticket.setCreationDate(System.currentTimeMillis());
        ticket.setCustomerName(customer_name);
        ticket.setClientIp(value.clientIp());
        ticket.setUserName(value.userName());
        ticket.setProjectName(value.projectName());
        ticket.setConnectorName(value.connectorName());
        ticket.setConnectorType(value.connectorType());
        ticket.setRequestableName(value.requestableName());
        ticket.setRequestableType(value.requestableType());
        ticket.setResponseTime(value.responseTime());
        ticket.setScore(value.score());
        ticket.setSessionID(value.sessionId());
        ticket.setUserAgent(value.userAgent());
        if (value.uiId() != null) ticket.setUiID(value.uiId());
        if (value.deviceUuid() != null) ticket.setDeviceUUID(value.deviceUuid());
        return ticket;
    }

	/** Hands a ticket to the ticket managers. */
	protected void enqueueTicket(Ticket ticket) {
		for (TicketQueue queue : queues) {
			queue.offer(ticket);
		}
	}

	public void insertBilling(HttpSessionListener sessionListener, String operation)
			throws EngineException {
		if (!hasActiveManagers()) {
			return;
		}
		int score;
		if (!ConvertigoHttpSessionManager.isRedisMode()) {
			score = HttpSessionListener.countSessions();
		} else if (RequestScopedHttpSession.isRequestScopedId(sessionListener.getSessionID())) {
			score = ConvertigoHttpSessionManager.getInstance().cachedCountedSessions();
		} else {
			score = ConvertigoHttpSessionManager.getInstance().countCountedSessions();
		}
		insertBillingSession(operation, sessionListener.getCreationTime(), sessionListener.getSessionID(),
				sessionListener.getClientIP(), sessionListener.getUserAgent(), sessionListener.getAuthenticatedUser(),
				sessionListener.getUuid(), score);
	}

	public void insertBillingSession(String operation, long creationTime, String sessionId, String clientIp,
			String userAgent, String authenticatedUser, String deviceUUID, int score) throws EngineException {
		if (!hasActiveManagers()) {
			return;
		}

		try {
			long now = System.currentTimeMillis();
			long time = now - creationTime;
			Ticket ticket = new Ticket();
			ticket.setCreationDate(now);
			ticket.setClientIp(clientIp);
			ticket.setCustomerName(customer_name);
			ticket.setUserName(authenticatedUser);
			ticket.setConnectorType("session");
			ticket.setConnectorName(operation);
			ticket.setResponseTime(time);
			ticket.setScore(score);
			ticket.setSessionID(sessionId);
			ticket.setUserAgent(userAgent);
			ticket.setDeviceUUID(deviceUUID);

			enqueueTicket(ticket);
		} catch (Exception e) {
			throw new EngineException("Ticket create failed", e);
		}
	}

	private void renewManager(boolean justDestroy) throws EngineException {
		renewManager(justDestroy, false);
	}

	/**
	 * The new ticket managers take the next tickets, then the previous ones write the tickets they have and are
	 * destroyed, waiting for them with {@code wait} and in the background otherwise.
	 */
	private synchronized void renewManager(boolean justDestroy, boolean wait) throws EngineException {
		var managers = new ArrayList<ITicketManager>(2);
		try {
			if (!justDestroy) {
				addManagers(managers);
			}
		} finally {
			// the ones created before a failure are used, as before
			var previous = queues;
			queues = startQueues(managers);
			close(previous, wait);
		}
	}

	private void addManagers(List<ITicketManager> managers) throws EngineException {
		if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_PERSISTENCE_ENABLED)) {
			try {
				managers.add(new HibernateTicketManager(Engine.logBillers));
			} catch (Throwable t) {
				throw new EngineException("TicketManager instanciation failed", t);
			}
		}

		if (serverMonitor) {
			try {
				managers.add(new GoogleAnalyticsTicketManager(keys[0], keys[1], new NOPLogger(null, "void")));
			} catch (Throwable t) {
			}
		}

		if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_GOOGLE_ENABLED)) {
			var id = EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_GOOGLE_ID);
			if (StringUtils.isEmpty(id) && StringUtils.isNoneEmpty(EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_OLD_GOOGLE_ID)) ) {
				Engine.logBillers.error("Your Google Analytics ID is deprecated, please configure your Measurement ID and API Secret. Google Analytics is disabled.");
			} else if ((!keys[0].equals(id))
					|| !serverMonitor) {
				try {
					managers.add(new GoogleAnalyticsTicketManager(
							id,
							EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_GOOGLE_SECRET),
							Engine.logBillers));
				} catch (Throwable t) {
					throw new EngineException("TicketManager instanciation failed", t);
				}
			}
		}
	}

	public void onEvent(PropertyChangeEvent event) {
		PropertyName name = event.getKey();
		switch (name) {
		case ANALYTICS_PERSISTENCE_ENABLED:
		case ANALYTICS_GOOGLE_ENABLED:
			try {
				// the previous ticket managers write their last tickets in the background
				stop(false);
			} catch (EngineException e) {
				Engine.logBillers.error("Error on BillingManager.destroy: [" + e.getClass().getName() + "] " + e.getMessage());
			}
			try {
				init();
			} catch (EngineException e) {
				Engine.logBillers.error("Error on BillingManager.init: [" + e.getClass().getName() + "] " + e.getMessage());
			}
			break;
		case ANALYTICS_PERSISTENCE_DIALECT:
		case ANALYTICS_PERSISTENCE_JDBC_DRIVER:
		case ANALYTICS_PERSISTENCE_JDBC_PASSWORD:
		case ANALYTICS_PERSISTENCE_JDBC_URL:
		case ANALYTICS_PERSISTENCE_JDBC_USERNAME:
		case ANALYTICS_GOOGLE_ID:
		case ANALYTICS_GOOGLE_SECRET:
			try {
				renewManager(false);
			} catch (EngineException e) {
				Engine.logBillers.error("Error on BillingManager.renewManager [" + e.getClass().getName() + "] " + e.getMessage());
			}
		default:
			break;
		}
	}
}
