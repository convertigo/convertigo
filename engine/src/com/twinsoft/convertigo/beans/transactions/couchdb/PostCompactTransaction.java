/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

public class PostCompactTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = 5234777656083833697L;
	
	public PostCompactTransaction() {
		super();
	}

	@Override
	public PostCompactTransaction clone() throws CloneNotSupportedException {
		PostCompactTransaction clonedObject =  (PostCompactTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		
		JSONObject response = getCouchClient().postCompact(db);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postCompactType");
	}
}
