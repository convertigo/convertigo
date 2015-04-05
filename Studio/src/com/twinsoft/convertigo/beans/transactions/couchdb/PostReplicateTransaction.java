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

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.enums.CouchParam;

public class PostReplicateTransaction extends AbstractDatabaseTransaction {
	
	private static final long serialVersionUID = -2917791679287718055L;

	public PostReplicateTransaction() {
		super();
	}
	
	private String p_cancel = "";
	private String p_continuous = "";
	private String p_create_target = "";
	private String p_doc_ids = "";
	private String p_proxy = "";
	private String p_source = "";
	private String p_target = "";

	@Override
	public PostReplicateTransaction clone() throws CloneNotSupportedException {
		PostReplicateTransaction clonedObject =  (PostReplicateTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String source = getParameterStringValue(CouchParam.source);
		String target = getParameterStringValue(CouchParam.target);
		
		String _create_target = getParameterStringValue(CouchParam.create_target);
		boolean create_target = _create_target != null ? Boolean.parseBoolean(_create_target) : false;
		
		String _continuous = getParameterStringValue(CouchParam.continuous);
		boolean continuous = _continuous != null ? Boolean.parseBoolean(_continuous) : false;
		
		String _cancel = getParameterStringValue(CouchParam.cancel);
		boolean cancel = _cancel != null ? Boolean.parseBoolean(_cancel) : false;
		
		JSONArray doc_ids = null;
		String _doc_ids = getParameterStringValue(CouchParam.doc_ids);
		try {
			doc_ids = new JSONArray(_doc_ids);
		} catch (JSONException e) {
			//TODO: log
		}
		
		String proxy = getParameterStringValue(CouchParam.proxy);
		
		JSONObject response = getCouchClient().postReplicate(source, target, create_target, continuous, cancel, doc_ids, proxy);
		
		return response;
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postReplicateType");
	}

	public String getP_cancel() {
		return p_cancel;
	}

	public void setP_cancel(String p_cancel) {
		this.p_cancel = p_cancel;
	}

	public String getP_continuous() {
		return p_continuous;
	}

	public void setP_continuous(String p_continuous) {
		this.p_continuous = p_continuous;
	}

	public String getP_create_target() {
		return p_create_target;
	}

	public void setP_create_target(String p_create_target) {
		this.p_create_target = p_create_target;
	}

	public String getP_doc_ids() {
		return p_doc_ids;
	}

	public void setP_doc_ids(String p_doc_ids) {
		this.p_doc_ids = p_doc_ids;
	}

	public String getP_proxy() {
		return p_proxy;
	}

	public void setP_proxy(String p_proxy) {
		this.p_proxy = p_proxy;
	}

	public String getP_source() {
		return p_source;
	}

	public void setP_source(String p_source) {
		this.p_source = p_source;
	}

	public String getP_target() {
		return p_target;
	}

	public void setP_target(String p_target) {
		this.p_target = p_target;
	}
}
