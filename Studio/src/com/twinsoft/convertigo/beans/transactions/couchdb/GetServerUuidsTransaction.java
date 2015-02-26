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

public class GetServerUuidsTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = -7533090778648171161L;
	
	public GetServerUuidsTransaction() {
		super();
	}

	@Override
	public GetServerUuidsTransaction clone() throws CloneNotSupportedException {
		GetServerUuidsTransaction clonedObject =  (GetServerUuidsTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return Arrays.asList(new CouchDbParameter[] {var_count});
	}

	@Override
	protected Object invoke() throws Exception {
		String sCount = getParameterStringValue(var_count);
		long count = (sCount == null || sCount.isEmpty()) ? 1L : Long.valueOf(sCount);
		return getCouchClient().getUuids(count);
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "svrUuidsType");
	}
}
