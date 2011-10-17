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

package com.twinsoft.convertigo.engine.billing;

import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class HibernateTicketManager implements ITicketManager {
	
	private SessionFactory sessionFactory;
	private Logger log;
	
	public HibernateTicketManager(Properties configuration, Logger log) throws BillingException {
		this.log = log;
		Configuration cfg = new Configuration();

		configuration.setProperty("hibernate.hbm2ddl.auto", "update");
		configuration.setProperty("hibernate.connection.autocommit", "true");
		configuration.setProperty("hibernate.show_sql", "true");
		
		cfg.addAnnotatedClass(com.twinsoft.convertigo.engine.billing.Ticket.class);

		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element session_factory = doc.createElement("session-factory");
			for (Entry<Object, Object> entry : configuration.entrySet()) {
				Element property = doc.createElement("property");
				property.setAttribute("name", entry.getKey().toString());
				property.setTextContent(entry.getValue().toString());
				session_factory.appendChild(property);
			};
			doc.appendChild(doc.createElement("hibernate-configuration")).appendChild(session_factory);
			cfg.configure(doc);
		} catch (ParserConfigurationException e) {
			throw new BillingException("Configuration Document creation failed", e);
		}
		
		try {
			sessionFactory = cfg.buildSessionFactory();
		}
		catch (HibernateException e) {
			throw new BillingException("Hibernate session factory creation failed", e);
		}
		
		this.log.info("(HibernateTicketManager) initialized");
	}

	public void addTicket(Ticket ticket) throws BillingException {
		if (log.isDebugEnabled()) {
			log.debug("(HibernateTicketManager) addTicket " + ticket);
		}
		StatelessSession session = getSession();
		session.insert(ticket);
		session.close();
	}

	public Ticket peekTicket() throws BillingException {
		StatelessSession session = getSession();
		Ticket ticket = (Ticket) session.createCriteria(Ticket.class).setMaxResults(1).uniqueResult();;
		if (log.isDebugEnabled()) {
			log.debug("(HibernateTicketManager) peekTicket " + ticket);
		}
		session.close();
		return ticket;
	}

	public void removeTicket(Ticket ticket) throws BillingException {
		if (log.isDebugEnabled()) {
			log.debug("(HibernateTicketManager) removeTicket " + ticket);
		}
		StatelessSession session = getSession();
		session.delete(ticket);
		session.close();
	}
	
	public Ticket newTicket() throws BillingException {
		return new Ticket();
	}
	
	public void destroy() throws BillingException {
		if (sessionFactory != null) {
			sessionFactory.close();
		}
	}
	
	private StatelessSession getSession() throws BillingException {
		if (sessionFactory == null)
			throw new BillingException("Unable to retrieve a session : factory is null");
		return sessionFactory.openStatelessSession();
	}
}
