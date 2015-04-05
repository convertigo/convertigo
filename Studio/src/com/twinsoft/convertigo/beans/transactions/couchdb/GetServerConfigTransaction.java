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

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class GetServerConfigTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = 7019930439389954999L;
	
	private String p_section = "";
	private String p_key = "";
	
	public GetServerConfigTransaction() {
		super();
	}

	@Override
	public GetServerConfigTransaction clone() throws CloneNotSupportedException {
		GetServerConfigTransaction clonedObject =  (GetServerConfigTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String section = getParameterStringValue(CouchParam.section);
		String key = getParameterStringValue(CouchParam.key);
		
		JSONObject response = getCouchClient().getConfig(section, key);
		response = GetServerConfigTransaction.handleConfigResponse(response, section, key);
		
		return response;
	}
	
	static JSONObject handleConfigResponse(JSONObject json, String section, String key) throws Exception {
		if (section != null && "success".equals(CouchKey._c8oMeta.JSONObject(json).getString("status"))) {// modify json for schema compliance
			JSONObject s = new JSONObject();
			if (key == null) {
				s.put(section, json);
			} else {
				JSONObject k = new JSONObject();
				k.put(key, json.get("data"));
				s.put(section, k);
			}
			s.put(CouchKey._c8oMeta.key(), json.remove(CouchKey._c8oMeta.key()));
			return s;
		}
		return json;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "getServerConfigType");
	}

	public String getP_section() {
		return p_section;
	}

	public void setP_section(String p_section) {
		this.p_section = p_section;
	}

	public String getP_key() {
		return p_key;
	}

	public void setP_key(String p_key) {
		this.p_key = p_key;
	}
}
