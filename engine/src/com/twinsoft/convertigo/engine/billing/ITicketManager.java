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

import java.util.List;

public interface ITicketManager {

	public void addTicket(Ticket ticket) throws BillingException;

	/**
	 * Writes tickets handed together, at once: all of them, or none of them when it throws. Returns false when the
	 * manager does not write tickets together: they are then handed one by one to {@link #addTicket(Ticket)}.
	 */
	public default boolean addTickets(List<Ticket> tickets) throws BillingException {
		return false;
	}

	public void removeTicket(Ticket ticket) throws BillingException;
	
	public Ticket newTicket() throws BillingException;
	
	public void destroy() throws BillingException;

}