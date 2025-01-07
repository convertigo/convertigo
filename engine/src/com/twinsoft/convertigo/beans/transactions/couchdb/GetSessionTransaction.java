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

public class GetSessionTransaction extends AbstractCouchDbTransaction {

	private static final long serialVersionUID = -4535060854117693921L;	
	
	public GetSessionTransaction() {
		super();
	}

	@Override
	public GetSessionTransaction clone() throws CloneNotSupportedException {
		GetSessionTransaction clonedObject =  (GetSessionTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		return getCouchClient().getSession();
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "getSessionType");
	}
}
