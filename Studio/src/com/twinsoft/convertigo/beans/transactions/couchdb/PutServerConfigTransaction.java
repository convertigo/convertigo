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

import com.twinsoft.convertigo.engine.enums.CouchParam;

public class PutServerConfigTransaction extends AbstractServerTransaction {
	private static final long serialVersionUID = -3775078867489864436L;
	
	private String p_section = "";
	private String p_key = "";
	private String p_value = "";
	
	public PutServerConfigTransaction() {
		super();
	}

	@Override
	public PutServerConfigTransaction clone() throws CloneNotSupportedException {
		PutServerConfigTransaction clonedObject =  (PutServerConfigTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected Object invoke() throws Exception {
		String section = getParameterStringValue(CouchParam.section);
		String key = getParameterStringValue(CouchParam.key);
		String value = getParameterStringValue(CouchParam.value);
		
		JSONObject response = getCouchClient().putConfig(section, key, value);
		response = GetServerConfigTransaction.handleConfigResponse(response, section, key);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "putServerConfigType");
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

	public String getP_value() {
		return p_value;
	}

	public void setP_value(String p_value) {
		this.p_value = p_value;
	}
}
