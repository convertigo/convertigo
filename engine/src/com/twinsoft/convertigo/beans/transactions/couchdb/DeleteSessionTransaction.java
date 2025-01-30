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

public class DeleteSessionTransaction extends AbstractCouchDbTransaction {

	private static final long serialVersionUID = -6300800378590874080L;

	public DeleteSessionTransaction() {
		super();
	}

	@Override
	public DeleteSessionTransaction clone() throws CloneNotSupportedException {
		DeleteSessionTransaction clonedObject =  (DeleteSessionTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		Object res = getCouchClient().deleteSession(); 
		getConnector().setCouchClient(null);
		return res;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "deleteSessionType");
	}
	
}
