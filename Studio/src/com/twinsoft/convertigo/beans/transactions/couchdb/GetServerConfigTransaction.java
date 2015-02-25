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

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GetServerConfigTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = 7019930439389954999L;
	
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
		return Arrays.asList(new CouchDbParameter[] {var_section, var_key});
	}

	@Override
	protected Object invoke() throws Exception {
		String section = getParameterStringValue(var_section);
		String key = getParameterStringValue(var_key);
		JsonElement json = getCouchDbContext().config().get(section, key);
		if (section != null) {// modify json for schema compliance
			JsonObject s = new JsonObject();
			if (key == null) {
				s.add(section, json);
			}
			else {
				JsonObject k = new JsonObject();
				k.add(key, json);
				s.add(section, k);
			}
			return s;
		}
		return json;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "svrConfigType");
	}
}
