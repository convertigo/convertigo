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

import com.twinsoft.convertigo.engine.enums.CouchParam;

public class DeleteDocumentTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = 6392840891762384633L;
	
	private String q_rev = "";
	private String q_batch = "";
	
	public DeleteDocumentTransaction() {
		super();
	}

	@Override
	public DeleteDocumentTransaction clone() throws CloneNotSupportedException {
		DeleteDocumentTransaction clonedObject =  (DeleteDocumentTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		String docid = getParameterStringValue(CouchParam.docid);
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject response = getCouchClient().deleteDocument(db, docid, query);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "deleteDocumentType");
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
