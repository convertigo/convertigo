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

public class GetDocumentTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = 110083227104023263L;
	
	public GetDocumentTransaction() {
		super();
	}

	@Override
	public GetDocumentTransaction clone() throws CloneNotSupportedException {
		GetDocumentTransaction clonedObject =  (GetDocumentTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return Arrays.asList(new CouchDbParameter[] {var_database, var_docid, var_docrev});
	}

	@Override
	protected Object invoke() throws Exception {
		if (getCouchClient() != null) {
			return getCouchClient().getDocument(getTargetDatabase(), getParameterStringValue(var_docid), getParameterStringValue(var_docrev));
		}
		
		String docId = getParameterStringValue(var_docid);
		String docRev = getParameterStringValue(var_docrev);
		return getCouchDBDocument().get(docId, docRev);
	}

	@Override
	public QName getComplexTypeAffectation() {
		if (getXmlComplexTypeAffectation().isEmpty())
			return new QName(COUCHDB_XSD_NAMESPACE, "docGetType");
		else
			return super.getComplexTypeAffectation();
	}
	
}
