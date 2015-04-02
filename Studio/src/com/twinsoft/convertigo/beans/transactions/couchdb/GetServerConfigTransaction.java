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

import java.util.List;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.enums.CouchKey;

public class GetServerConfigTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = 7019930439389954999L;
	
	private String u_section = "";
	private String u_key = "";
	
	public GetServerConfigTransaction() {
		super();
	}

	@Override
	public GetServerConfigTransaction clone() throws CloneNotSupportedException {
		GetServerConfigTransaction clonedObject =  (GetServerConfigTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return getDeclaredParameters(var_section, var_key);
	}

	@Override
	protected Object invoke() throws Exception {
		String section = getParameterStringValue(var_section);
		String key = getParameterStringValue(var_key);
		
		JSONObject json = getCouchClient().getConfig(section, key);
		return handleConfigResponse(json, section, key);
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

	public String getU_section() {
		return u_section;
	}

	public void setU_section(String u_section) {
		this.u_section = u_section;
	}

	public String getU_key() {
		return u_key;
	}

	public void setU_key(String u_key) {
		this.u_key = u_key;
	}
}
