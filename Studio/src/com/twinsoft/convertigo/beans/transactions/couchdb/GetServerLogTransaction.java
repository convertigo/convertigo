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

import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

public class GetServerLogTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = 3117082545323969984L;
	
	private String q_bytes = "";
	private String q_offset = "";
	
	public GetServerLogTransaction() {
		super();
	}

	@Override
	public GetServerLogTransaction clone() throws CloneNotSupportedException {
		GetServerLogTransaction clonedObject =  (GetServerLogTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected Object invoke() throws Exception {
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject response = getCouchClient().getLog(query);
		
		return response;
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "getServerLogType");
	}

	public String getQ_bytes() {
		return q_bytes;
	}

	public void setQ_bytes(String q_bytes) {
		this.q_bytes = q_bytes;
	}

	public String getQ_offset() {
		return q_offset;
	}

	public void setQ_offset(String q_offset) {
		this.q_offset = q_offset;
	}
}
