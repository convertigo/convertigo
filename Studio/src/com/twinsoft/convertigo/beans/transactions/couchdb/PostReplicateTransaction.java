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

import java.util.List;

import javax.xml.namespace.QName;

public class PostReplicateTransaction extends AbstractDatabaseTransaction {
	
	private static final long serialVersionUID = -2917791679287718055L;

	public PostReplicateTransaction() {
		super();
	}

	@Override
	public PostReplicateTransaction clone() throws CloneNotSupportedException {
		PostReplicateTransaction clonedObject =  (PostReplicateTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return getDeclaredParameters(
			CouchDbParameter.Param_source, CouchDbParameter.Param_target, CouchDbParameter.Param_create_target,
			CouchDbParameter.Param_continuous, CouchDbParameter.Param_cancel, CouchDbParameter.Param_doc_ids, CouchDbParameter.Param_proxy
		);
	}

	@Override
	protected Object invoke() throws Exception {
		String source = getParameterStringValue(CouchDbParameter.Param_source);
		String target = getParameterStringValue(CouchDbParameter.Param_target);
		
		String _create_target = getParameterStringValue(CouchDbParameter.Param_create_target);
		boolean create_target = _create_target != null ? Boolean.parseBoolean(_create_target) : false;
		
		String _continuous = getParameterStringValue(CouchDbParameter.Param_continuous);
		boolean continuous = _continuous != null ? Boolean.parseBoolean(_continuous) : false;
		
		String _cancel = getParameterStringValue(CouchDbParameter.Param_cancel);
		boolean cancel = _cancel != null ? Boolean.parseBoolean(_cancel) : false;
		
		String[] doc_ids = null;
		Object _doc_ids = getParameterValue(CouchDbParameter.Param_doc_ids);
		if (_doc_ids instanceof String[]) {
			doc_ids = (String[]) _doc_ids;
		}
		
		String proxy = getParameterStringValue(CouchDbParameter.Param_proxy);	
		
		return getCouchClient().postReplicate(source, target, create_target, continuous, cancel, doc_ids, proxy);
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postReplicateType");
	}

}
