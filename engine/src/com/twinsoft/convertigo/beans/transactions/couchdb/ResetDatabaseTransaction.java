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

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;

public class ResetDatabaseTransaction extends DeleteDatabaseTransaction {

	private static final long serialVersionUID = 5234193344083833697L;
	
	public ResetDatabaseTransaction() {
		super();
	}

	@Override
	public ResetDatabaseTransaction clone() throws CloneNotSupportedException {
		ResetDatabaseTransaction clonedObject =  (ResetDatabaseTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected JSONObject invoke() throws Exception {
		JSONObject response = super.invoke();
		if ((response.has("ok") && response.getBoolean("ok"))
			|| response.has("error") && "not_found".equals(response.getString("error"))) {
			CouchDbManager.syncDocument(getConnector());
			if (response.has("error")) {
				response.remove("error");
				response.remove("reason");
				response.put("ok", true);
			}
		}
		return response;
	}
}
