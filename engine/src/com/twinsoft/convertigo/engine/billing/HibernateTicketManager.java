/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import org.apache.log4j.Logger;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.helpers.HibernateHelper;

public class HibernateTicketManager implements ITicketManager {
	
	private HibernateHelper hibernateHelper;
	private Logger log;
	
	public HibernateTicketManager(Logger log) throws BillingException {
		this.log = log;
		
		try {
			hibernateHelper = new HibernateHelper(
				log,
				EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_PERSISTENCE_DIALECT),
				EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_DRIVER),
				EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_URL),
				EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_USERNAME),
				EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_PERSISTENCE_JDBC_PASSWORD),
				EnginePropertiesManager.getPropertyAsLong(PropertyName.ANALYTICS_PERSISTENCE_MAX_RETRY),
				com.twinsoft.convertigo.engine.billing.Ticket.class
			);
		} catch (EngineException e) {
			throw new BillingException("Hibernate session factory creation failed", e);
		}
		
		this.log.info("(HibernateTicketManager) initialized");
	}

	public synchronized void addTicket(Ticket ticket) throws BillingException {
		if (log.isDebugEnabled()) {
			log.debug("(HibernateTicketManager) addTicket " + ticket);
		}
		hibernateHelper.insert(ticket);
	}

	public synchronized void removeTicket(Ticket ticket) throws BillingException {
		if (log.isDebugEnabled()) {
			log.debug("(HibernateTicketManager) removeTicket " + ticket);
		}
		hibernateHelper.delete(ticket);
	}
	
	public Ticket newTicket() throws BillingException {
		return new Ticket();
	}
	
	public synchronized void destroy() throws BillingException {
		if (hibernateHelper != null) {
			hibernateHelper.destroy();
		}
	}
}
