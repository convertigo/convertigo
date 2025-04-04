/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

public class DeleteDatabaseTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = 5234196656083833697L;
	
	public DeleteDatabaseTransaction() {
		super();
	}

	@Override
	public DeleteDatabaseTransaction clone() throws CloneNotSupportedException {
		DeleteDatabaseTransaction clonedObject =  (DeleteDatabaseTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected JSONObject invoke() throws Exception {
		String db = getTargetDatabase();
		
		JSONObject response = getCouchClient().deleteDatabase(db);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "deleteDatabaseType");
	}
}
