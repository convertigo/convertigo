/*
 * Copyright (c) 2001-2023 Convertigo SA.
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
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;

public class GetServerDatabasesTransaction extends AbstractCouchDbTransaction {

	private static final long serialVersionUID = -665777000227521501L;
	
	private String q_descending = "";
	private String q_endkey = "";
	private String q_limit = "";
	private String q_skip = "";
	private String q_startkey = "";
	
	public GetServerDatabasesTransaction() {
		super();
	}

	@Override
	public GetServerDatabasesTransaction clone() throws CloneNotSupportedException {
		GetServerDatabasesTransaction clonedObject =  (GetServerDatabasesTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		Map<String, String> query = getQueryVariableValues();
		
		for (Entry<String, String> entry: query.entrySet()) {
			if (entry.getKey().contains("key")) {
				String value = entry.getValue();
				value = CouchClient.quoteValue(value);
				entry.setValue(value);
			}
		}
		
		return getCouchClient().getAllDbs(query);
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "getServerDatabasesType");
	}
	
	public String getQ_descending() {
		return q_descending;
	}

	public void setQ_descending(String q_descending) {
		this.q_descending = q_descending;
	}

	public String getQ_endkey() {
		return q_endkey;
	}

	public void setQ_endkey(String q_endkey) {
		this.q_endkey = q_endkey;
	}

	public String getQ_limit() {
		return q_limit;
	}

	public void setQ_limit(String q_limit) {
		this.q_limit = q_limit;
	}

	public String getQ_skip() {
		return q_skip;
	}

	public void setQ_skip(String q_skip) {
		this.q_skip = q_skip;
	}

	public String getQ_startkey() {
		return q_startkey;
	}

	public void setQ_startkey(String q_startkey) {
		this.q_startkey = q_startkey;
	}
}
