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

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.enums.CouchParam;

public class PostFindTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = -7606732816561433014L;

	private String p_json_base = "";
	
	private String p_selector = "";
	private String p_limit = "";
	private String p_skip = "";
	private String p_sort = "";
	private String p_fields = "";
	private String p_use_index = "";
	private String p_bookmark = "";
	private String p_update = "";
	private String p_stable = "";
	private String p_execution_stats = "";
	
	public PostFindTransaction() {
		super();
	}

	@Override
	public PostFindTransaction clone() throws CloneNotSupportedException {
		PostFindTransaction clonedObject =  (PostFindTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();

		JSONObject jsonBase;
		org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
		try {
			try {
				jsonBase = (JSONObject) getEvaluatedParam(cx, CouchParam.json_base);
			} catch (Throwable t) {
				jsonBase = new JSONObject();
			}

			putEvaluatedParam(cx, jsonBase, CouchParam.selector);
			putLongParam(jsonBase, CouchParam.limit);
			putLongParam(jsonBase, CouchParam.skip);
			putEvaluatedParam(cx, jsonBase, CouchParam.sort);
			putEvaluatedParam(cx, jsonBase, CouchParam.fields);
			putEvaluatedParam(cx, jsonBase, CouchParam.use_index);
			putStringParam(jsonBase, CouchParam.bookmark);
			putBooleanParam(jsonBase, CouchParam.update);
			putBooleanParam(jsonBase, CouchParam.stable);
			putBooleanParam(jsonBase, CouchParam.execution_stats);
		} finally {
			org.mozilla.javascript.Context.exit();
		}

		JSONObject response = getCouchClient().postFind(db, jsonBase);

		return response;
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postFindType");
	}

	public String getP_json_base() {
		return p_json_base;
	}

	public void setP_json_base(String p_json_base) {
		this.p_json_base = p_json_base;
	}

	public String getP_selector() {
		return p_selector;
	}

	public void setP_selector(String p_selector) {
		this.p_selector = p_selector;
	}

	public String getP_limit() {
		return p_limit;
	}

	public void setP_limit(String p_limit) {
		this.p_limit = p_limit;
	}

	public String getP_skip() {
		return p_skip;
	}

	public void setP_skip(String p_skip) {
		this.p_skip = p_skip;
	}

	public String getP_sort() {
		return p_sort;
	}

	public void setP_sort(String p_sort) {
		this.p_sort = p_sort;
	}

	public String getP_fields() {
		return p_fields;
	}

	public void setP_fields(String p_fields) {
		this.p_fields = p_fields;
	}

	public String getP_use_index() {
		return p_use_index;
	}

	public void setP_use_index(String p_use_index) {
		this.p_use_index = p_use_index;
	}

	public String getP_bookmark() {
		return p_bookmark;
	}

	public void setP_bookmark(String p_bookmark) {
		this.p_bookmark = p_bookmark;
	}

	public String getP_update() {
		return p_update;
	}

	public void setP_update(String p_update) {
		this.p_update = p_update;
	}

	public String getP_stable() {
		return p_stable;
	}

	public void setP_stable(String p_stable) {
		this.p_stable = p_stable;
	}

	public String getP_execution_stats() {
		return p_execution_stats;
	}

	public void setP_execution_stats(String p_execution_stats) {
		this.p_execution_stats = p_execution_stats;
	}
	
}
