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

import com.twinsoft.convertigo.engine.enums.CouchParam;

public class CopyDocumentTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = 110083227104023263L;
	
	private String p_destination = "";
	private String p_destination_rev = "";
	private String q_rev = "";
	private String q_batch = "";
	
	public CopyDocumentTransaction() {
		super();
	}

	@Override
	public CopyDocumentTransaction clone() throws CloneNotSupportedException {
		CopyDocumentTransaction clonedObject =  (CopyDocumentTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		String docid = getParameterStringValue(CouchParam.docid);
		String destination = getParameterStringValue(CouchParam.destination);
		String destination_rev = getParameterStringValue(CouchParam.destination);
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject response = getCouchClient().copyDocument(db, docid, destination, destination_rev, query);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "copyDocumentType");
	}

	public String getP_destination() {
		return p_destination;
	}

	public void setP_destination(String p_destination) {
		this.p_destination = p_destination;
	}

	public String getP_destination_rev() {
		return p_destination_rev;
	}

	public void setP_destination_rev(String p_destination_rev) {
		this.p_destination_rev = p_destination_rev;
	}
	
	public String getQ_rev() {
		return q_rev;
	}

	public void setQ_rev(String q_rev) {
		this.q_rev = q_rev;
	}

	public String getQ_batch() {
		return q_batch;
	}

	public void setQ_batch(String q_batch) {
		this.q_batch = q_batch;
	}
}
