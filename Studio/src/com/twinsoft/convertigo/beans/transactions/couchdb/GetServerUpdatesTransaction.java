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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.List;

import javax.xml.namespace.QName;

public class GetServerUpdatesTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = -4009927560249117525L;
	
	private String q_feed = "";
	private String q_timeout = "";
	private String q_heartbeat = "";
	
	public GetServerUpdatesTransaction() {
		super();
	}

	@Override
	public GetServerUpdatesTransaction clone() throws CloneNotSupportedException {
		GetServerUpdatesTransaction clonedObject =  (GetServerUpdatesTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return getDeclaredParameters(var_feed, var_timeout, var_heartbeat);
	}
	
	@Override
	protected Object invoke() throws Exception {
		String feed = getParameterStringValue(var_feed);
		
		String sTimeout = getParameterStringValue(var_timeout);
		Integer timeout = (sTimeout == null) ? null : Integer.valueOf(sTimeout);
		
		String sHeartbeat = getParameterStringValue(var_heartbeat);
		Boolean heartbeat = (sHeartbeat == null) ? null : Boolean.valueOf(sHeartbeat);
		
		return getCouchClient().getDbUpdates(feed, timeout, heartbeat);
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "getServerUpdatesType");
	}

	public String getQ_feed() {
		return q_feed;
	}

	public void setQ_feed(String q_feed) {
		this.q_feed = q_feed;
	}

	public String getQ_timeout() {
		return q_timeout;
	}

	public void setQ_timeout(String q_timeout) {
		this.q_timeout = q_timeout;
	}

	public String getQ_heartbeat() {
		return q_heartbeat;
	}

	public void setQ_heartbeat(String q_heartbeat) {
		this.q_heartbeat = q_heartbeat;
	}
}
