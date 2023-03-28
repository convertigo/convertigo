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

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

public class PutDatabaseTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = 6809890813346670619L;
	
	private String q_q = "";
	private String q_n = "";
	
	public PutDatabaseTransaction() {
		super();
	}

	@Override
	public PutDatabaseTransaction clone() throws CloneNotSupportedException {
		PutDatabaseTransaction clonedObject =  (PutDatabaseTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject response = getCouchClient().putDatabase(db, query);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "putDatabaseType");
	}

	public String getQ_q() {
		return q_q;
	}

	public void setQ_q(String q_q) {
		this.q_q = q_q;
	}

	public String getQ_n() {
		return q_n;
	}

	public void setQ_n(String q_n) {
		this.q_n = q_n;
	}
}
