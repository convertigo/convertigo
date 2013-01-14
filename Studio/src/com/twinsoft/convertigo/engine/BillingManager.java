/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import org.hibernate.exception.JDBCConnectionException;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.BillingException;
import com.twinsoft.convertigo.engine.billing.HibernateTicketManager;
import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;

public class BillingManager implements AbstractManager, PropertyChangeEventListener {
	private boolean isDestroying = true;
	
	private String customer_name = ""; 
	
	private Collection<Ticket> tickets;
	
	private ITicketManager manager;
	
	private Thread consumer;
	
	public BillingManager() throws EngineException {
		
	}
	
	public void init() throws EngineException {
		customer_name = Engine.isCloudMode() ? Engine.cloud_customer_name : (Engine.isStudioMode() ? "CONVERTIGO Studio" : "CONVERTIGO Server");
		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
		if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.BILLING_ENABLED)) {
			isDestroying = false;
			tickets = new LinkedList<Ticket>();
			consumer = new Thread(new Runnable() {
				public void run() {
					while (!isDestroying && consumer == Thread.currentThread()) {
						Ticket ticket = null;
						try {
							synchronized (tickets) {
								if (tickets.isEmpty()) {
									tickets.wait();
								} else {
									Iterator<Ticket> iTicketData = tickets.iterator();
									ticket = iTicketData.next();
									iTicketData.remove();
								}
							}
							if (ticket != null) {
								manager.addTicket(ticket);
							}
						} catch (JDBCConnectionException e) {
							Throwable cause = e.getCause();
							Engine.logBillers.info("JDBCConnectionException on ticket insertion" + (cause == null ? "" : " (cause by " + cause.getClass().getSimpleName() + ")") + ": " + ticket);
						} catch (Exception e) {
							Engine.logBillers.error("Something failed in ticket insertion : " + ticket, e);
						}
					}
				}
			});
			
			renewManager(false);
			consumer.setName("BillingManager consumer");
			consumer.setDaemon(true);
			consumer.start();
		}
	}

	public void destroy() throws EngineException {
		Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
		
		isDestroying = true;
		tickets = null;
		consumer = null;
		renewManager(true);
	}

	public void insertBilling(Context context) throws EngineException {
		insertBilling(context, null, null);
	}
	
	public synchronized void insertBilling(Context context, Long responseTime, Long score) throws EngineException {
		if (isDestroying) return;
		if (manager == null) return;
		if (context == null) return;
		if (context.requestedObject == null) return;
		
		try {
			String username = (String)context.get("username");
			username = (username == null) ? context.tasUserName:username;
			username = (username == null) ? "user":username;
			
			Ticket ticket = manager.newTicket();
			ticket.setCreationDate(System.currentTimeMillis());
			ticket.setClientIp(context.remoteAddr);
			ticket.setCustomerName(customer_name);
			ticket.setUserName(username);
			ticket.setProjectName(context.projectName);
			ticket.setConnectorName((context.connectorName == null) ? "":context.connectorName);
			ticket.setConnectorType((context.connector == null) ? "":context.connector.getClass().getSimpleName());
			ticket.setRequestableName(context.requestedObject.getName());
			ticket.setRequestableType(context.requestedObject.getClass().getSimpleName());
			ticket.setResponseTime((responseTime == null) ? context.statistics.getLatestDuration(EngineStatistics.GET_DOCUMENT):responseTime);
			ticket.setScore((score == null) ? context.requestedObject.getScore():score);
			
			synchronized (tickets) {
				tickets.add(ticket);
				tickets.notify();
			}
		} catch (BillingException e) {
			throw new EngineException("Ticket create failed", e);
		}
	}

	private synchronized void renewManager(boolean justDestroy) throws EngineException {
		if (manager != null) {
			try {
				manager.destroy();
			} catch (BillingException e) {
				throw new EngineException("TicketManager failed to destroy", e);
			}
			manager = null;
		}
		if (!justDestroy) {
			try {
				Properties configuration = new Properties();
				configuration.setProperty("hibernate.connection.driver_class", EnginePropertiesManager.getProperty(PropertyName.BILLING_PERSISTENCE_JDBC_DRIVER));
				configuration.setProperty("hibernate.connection.url", EnginePropertiesManager.getProperty(PropertyName.BILLING_PERSISTENCE_JDBC_URL));
				configuration.setProperty("hibernate.connection.username", EnginePropertiesManager.getProperty(PropertyName.BILLING_PERSISTENCE_JDBC_USERNAME));
				configuration.setProperty("hibernate.connection.password", EnginePropertiesManager.getProperty(PropertyName.BILLING_PERSISTENCE_JDBC_PASSWORD));
				configuration.setProperty("hibernate.dialect", EnginePropertiesManager.getProperty(PropertyName.BILLING_PERSISTENCE_DIALECT));
				manager = new HibernateTicketManager(configuration, Engine.logBillers);
			} catch (Throwable t) {
				throw new EngineException("TicketManager instanciation failed", t);
			}
		}
	}
	
	public void onEvent(PropertyChangeEvent event) {
		PropertyName name = event.getKey();
		switch (name) {
			case BILLING_ENABLED:
				try {
					destroy();
				} catch(EngineException e) {
					Engine.logBillers.error("Error on BillingManager.destroy", e);
				}
				try {
					init();
				} catch(EngineException e) {
					Engine.logBillers.error("Error on BillingManager.init", e);
				}
				break;
			case BILLING_PERSISTENCE_DIALECT:
			case BILLING_PERSISTENCE_JDBC_DRIVER:
			case BILLING_PERSISTENCE_JDBC_PASSWORD:
			case BILLING_PERSISTENCE_JDBC_URL:
			case BILLING_PERSISTENCE_JDBC_USERNAME:
				try {
					renewManager(false);
				} catch (EngineException e) {
					Engine.logBillers.error("Error on BillingManager.renewManager", e);
				}
		}
	}
}