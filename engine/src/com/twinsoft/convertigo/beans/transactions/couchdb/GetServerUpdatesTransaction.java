/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

public class GetServerUpdatesTransaction extends AbstractCouchDbTransaction {

	private static final long serialVersionUID = -4009927560249117525L;
	
	private String q_feed = "";
	private String q_timeout = "";
	private String q_heartbeat = "";
	private String q_since = "";
	
	public GetServerUpdatesTransaction() {
		super();
	}

	@Override
	public GetServerUpdatesTransaction clone() throws CloneNotSupportedException {
		GetServerUpdatesTransaction clonedObject =  (GetServerUpdatesTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected Object invoke() throws Exception {
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject response = (JSONObject) getCouchClient().getDbUpdates(query);
		
		return response;
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

	public String getQ_since() {
		return q_since;
	}

	public void setQ_since(String q_since) {
		this.q_since = q_since;
	}
}
