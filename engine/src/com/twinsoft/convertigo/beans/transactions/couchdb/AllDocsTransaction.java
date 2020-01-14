/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;

public class AllDocsTransaction extends AbstractDatabaseTransaction implements ICouchParametersExtra {

	private static final long serialVersionUID = -3684374492418313635L;
	
	private String q_conflicts = "";
	private String q_descending = "";
	private String q_endkey = "";
	private String q_endkey_docid = "";
	private String q_include_docs = "";
	private String q_inclusive_end = "";
	private String q_key = "";
	private String q_keys = "";
	private String q_limit = "";
	private String q_skip = "";
	private String q_sorted = "";
	private String q_stable = "";
	private String q_stale = "";
	private String q_startkey = "";
	private String q_startkey_docid = "";
	private String q_update_seq = "";

	public AllDocsTransaction() {
		super();
	}

	@Override
	public AllDocsTransaction clone() throws CloneNotSupportedException {
		AllDocsTransaction clonedObject = (AllDocsTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		Map<String, String> query = getQueryVariableValues();
		
		for (Entry<String, String> entry: query.entrySet()) {
			if (entry.getKey().contains("key")) {
				String value = entry.getValue();
				value = CouchClient.quoteValue(value);
				entry.setValue(value);
			}
		}
		
		JSONObject response = getCouchClient().getAllDocs(db, query);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		if (getXmlComplexTypeAffectation().isEmpty()) {
			return new QName(COUCHDB_XSD_NAMESPACE, "allDocsType");
		} else {
			return super.getComplexTypeAffectation();
		}
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

	public String getQ_include_docs() {
		return q_include_docs;
	}

	public void setQ_include_docs(String q_include_docs) {
		this.q_include_docs = q_include_docs;
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

	public String getQ_keys() {
		return q_keys;
	}

	public void setQ_keys(String q_keys) {
		this.q_keys = q_keys;
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

	public String getQ_sorted() {
		return q_sorted;
	}

	public void setQ_sorted(String q_sorted) {
		this.q_sorted = q_sorted;
	}

	public String getQ_stable() {
		return q_stable;
	}

	public void setQ_stable(String q_stable) {
		this.q_stable = q_stable;
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

	@Override
	public Collection<CouchExtraVariable> getCouchParametersExtra() {
		return Arrays.asList(
			CouchExtraVariable.keys
		);
	}
}
