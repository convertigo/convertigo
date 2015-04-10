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
import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class GetViewTransaction extends AbstractDatabaseTransaction implements ITagsProperty {

	private static final long serialVersionUID = -3684374492418313635L;
	
	private String viewname = "";
	
	private String p_ddoc = "";
	private String p_view = "";
	private String q_conflicts = "";
	private String q_descending = "";
	private String q_endkey = "";
	private String q_endkey_docid = "";
	private String q_group = "";
	private String q_group_level = "";
	private String q_include_docs = "";
	private String q_attachments = "";
	private String q_att_encoding_info = "";
	private String q_inclusive_end = "";
	private String q_key = "";
	private String q_limit = "";
	private String q_reduce = "";
	private String q_skip = "";
	private String q_stale = "";
	private String q_startkey = "";
	private String q_startkey_docid = "";
	private String q_update_seq = "";

	public GetViewTransaction() {
		super();
	}

	@Override
	public GetViewTransaction clone() throws CloneNotSupportedException {
		GetViewTransaction clonedObject = (GetViewTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		String ddoc;
		String view;
		Map<String, String> query = getQueryVariableValues();
		
		Matcher mSplitViewname = DesignDocument.splitFunctionName.matcher(viewname);
		
		if (mSplitViewname.matches()) {
			ddoc = mSplitViewname.group(1);
			view = mSplitViewname.group(2);
		} else {
			ddoc = getParameterStringValue(CouchParam.ddoc);
			view = getParameterStringValue(CouchParam.view);
		}
		
		JSONObject response = getCouchClient().getView(db, ddoc, view, query);
		
		return response;
	}

	public String getViewname() {
		return viewname;
	}

	public void setViewname(String viewname) {
		this.viewname = viewname;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if ("viewname".equals(propertyName)) {
			return DesignDocument.getTags(this, CouchKey.views);
		}
		return null;
	}

	@Override
	public QName getComplexTypeAffectation() {
		if (getXmlComplexTypeAffectation().isEmpty()) {
			return new QName(COUCHDB_XSD_NAMESPACE, "getViewType");
		} else {
			return super.getComplexTypeAffectation();
		}
	}

	public String getP_ddoc() {
		return p_ddoc;
	}

	public void setP_ddoc(String p_ddoc) {
		this.p_ddoc = p_ddoc;
	}

	public String getP_view() {
		return p_view;
	}

	public void setP_view(String p_view) {
		this.p_view = p_view;
	}

	public String getQ_conflicts() {
		return q_conflicts;
	}

	public void setQ_conflicts(String q_conflicts) {
		this.q_conflicts = q_conflicts;
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

	public String getQ_endkey_docid() {
		return q_endkey_docid;
	}

	public void setQ_endkey_docid(String q_endkey_docid) {
		this.q_endkey_docid = q_endkey_docid;
	}

	public String getQ_group() {
		return q_group;
	}

	public void setQ_group(String q_group) {
		this.q_group = q_group;
	}

	public String getQ_group_level() {
		return q_group_level;
	}

	public void setQ_group_level(String q_group_level) {
		this.q_group_level = q_group_level;
	}

	public String getQ_include_docs() {
		return q_include_docs;
	}

	public void setQ_include_docs(String q_include_docs) {
		this.q_include_docs = q_include_docs;
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

	public String getQ_inclusive_end() {
		return q_inclusive_end;
	}

	public void setQ_inclusive_end(String q_inclusive_end) {
		this.q_inclusive_end = q_inclusive_end;
	}

	public String getQ_key() {
		return q_key;
	}

	public void setQ_key(String q_key) {
		this.q_key = q_key;
	}

	public String getQ_limit() {
		return q_limit;
	}

	public void setQ_limit(String q_limit) {
		this.q_limit = q_limit;
	}

	public String getQ_reduce() {
		return q_reduce;
	}

	public void setQ_reduce(String q_reduce) {
		this.q_reduce = q_reduce;
	}

	public String getQ_skip() {
		return q_skip;
	}

	public void setQ_skip(String q_skip) {
		this.q_skip = q_skip;
	}

	public String getQ_stale() {
		return q_stale;
	}

	public void setQ_stale(String q_stale) {
		this.q_stale = q_stale;
	}

	public String getQ_startkey() {
		return q_startkey;
	}

	public void setQ_startkey(String q_startkey) {
		this.q_startkey = q_startkey;
	}

	public String getQ_startkey_docid() {
		return q_startkey_docid;
	}

	public void setQ_startkey_docid(String q_startkey_docid) {
		this.q_startkey_docid = q_startkey_docid;
	}

	public String getQ_update_seq() {
		return q_update_seq;
	}

	public void setQ_update_seq(String q_update_seq) {
		this.q_update_seq = q_update_seq;
	}
}
