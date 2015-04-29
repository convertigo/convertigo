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

public class GetServerTasksTransaction extends AbstractCouchDbTransaction {

	private static final long serialVersionUID = -5031957098726842599L;

	public GetServerTasksTransaction() {
		super();
	}

	@Override
	public GetServerTasksTransaction clone() throws CloneNotSupportedException {
		GetServerTasksTransaction clonedObject =  (GetServerTasksTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected Object invoke() throws Exception {
		return getCouchClient().getActiveTasks();
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "getServerTasksType");
	}
}
