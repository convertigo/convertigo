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

public class GetDocumentTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = 110083227104023263L;
	
	private String q_attachments = "";
	private String q_att_encoding_info = "";
	private String q_atts_since = "";
	private String q_conflicts = "";
	private String q_deleted_conflicts = "";
	private String q_latest = "";
	private String q_local_seq = "";
	private String q_meta = "";
	private String q_open_revs = "";
	private String q_rev = "";
	private String q_revs = "";
	private String q_revs_info = "";
	
	public GetDocumentTransaction() {
		super();
	}

	@Override
	public GetDocumentTransaction clone() throws CloneNotSupportedException {
		GetDocumentTransaction clonedObject =  (GetDocumentTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		String docid = getParameterStringValue(CouchParam.docid);
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject response = getCouchClient().getDocument(db, docid, query);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		if (getXmlComplexTypeAffectation().isEmpty())
			return new QName(COUCHDB_XSD_NAMESPACE, "getDocumentType");
		else
			return super.getComplexTypeAffectation();
	}

	public String getQ_attachments() {
		return q_attachments;
	}

	public void setQ_attachments(String q_attachments) {
		this.q_attachments = q_attachments;
	}

	public String getQ_att_encoding_info() {
		return q_att_encoding_info;
	}

	public void setQ_att_encoding_info(String q_att_encoding_info) {
		this.q_att_encoding_info = q_att_encoding_info;
	}

	public String getQ_atts_since() {
		return q_atts_since;
	}

	public void setQ_atts_since(String q_atts_since) {
		this.q_atts_since = q_atts_since;
	}

	public String getQ_conflicts() {
		return q_conflicts;
	}

	public void setQ_conflicts(String q_conflicts) {
		this.q_conflicts = q_conflicts;
	}

	public String getQ_deleted_conflicts() {
		return q_deleted_conflicts;
	}

	public void setQ_deleted_conflicts(String q_deleted_conflicts) {
		this.q_deleted_conflicts = q_deleted_conflicts;
	}

	public String getQ_latest() {
		return q_latest;
	}

	public void setQ_latest(String q_latest) {
		this.q_latest = q_latest;
	}

	public String getQ_local_seq() {
		return q_local_seq;
	}

	public void setQ_local_seq(String q_local_seq) {
		this.q_local_seq = q_local_seq;
	}

	public String getQ_meta() {
		return q_meta;
	}

	public void setQ_meta(String q_meta) {
		this.q_meta = q_meta;
	}

	public String getQ_open_revs() {
		return q_open_revs;
	}

	public void setQ_open_revs(String q_open_revs) {
		this.q_open_revs = q_open_revs;
	}

	public String getQ_rev() {
		return q_rev;
	}

	public void setQ_rev(String q_rev) {
		this.q_rev = q_rev;
	}

	public String getQ_revs() {
		return q_revs;
	}

	public void setQ_revs(String q_revs) {
		this.q_revs = q_revs;
	}

	public String getQ_revs_info() {
		return q_revs_info;
	}

	public void setQ_revs_info(String q_revs_info) {
		this.q_revs_info = q_revs_info;
	}
}
