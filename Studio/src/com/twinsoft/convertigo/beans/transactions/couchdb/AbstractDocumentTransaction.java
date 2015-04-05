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

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.enums.CouchKey;

public abstract class AbstractDocumentTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = 3030579754950212900L;
	
	protected static final CouchDbParameter var_id 			= CouchDbParameter.Private_id;
	protected static final CouchDbParameter var_ids			= CouchDbParameter.Private_ids;
	protected static final CouchDbParameter var_rev 		= CouchDbParameter.Private_rev;
	
	protected static final CouchDbParameter var_filepath 	= CouchDbParameter.Param_filepath;
	protected static final CouchDbParameter var_filename 	= CouchDbParameter.Path_filename;
	protected static final CouchDbParameter var_datas 		= CouchDbParameter.Param_datas;
	protected static final CouchDbParameter var_data 		= CouchDbParameter.Param_data;
	protected static final CouchDbParameter var_dest 		= CouchDbParameter.Param_destination;
	protected static final CouchDbParameter var_destrev		= CouchDbParameter.Param_destination_rev;
	protected static final CouchDbParameter var_docid 		= CouchDbParameter.Path_docid;
	protected static final CouchDbParameter var_docrev 		= CouchDbParameter.Param_docrev;
	protected static final CouchDbParameter var_updatename 	= CouchDbParameter.Path_updatename;
	protected static final CouchDbParameter var_viewname 	= CouchDbParameter.Path_viewname;
	protected static final CouchDbParameter var_view_endkey	= CouchDbParameter.Param_view_endkey;
	protected static final CouchDbParameter var_view_key	= CouchDbParameter.Param_view_key;
	protected static final CouchDbParameter var_view_limit	= CouchDbParameter.Param_view_limit;
	protected static final CouchDbParameter var_view_reduce	= CouchDbParameter.Param_view_reduce;
	protected static final CouchDbParameter var_view_skip	= CouchDbParameter.Param_view_skip;
	protected static final CouchDbParameter var_view_startkey	= CouchDbParameter.Param_view_startkey;
	
	protected static final String doc_base_path 	= "";
	protected static final String doc_design_path 	= CouchKey._design.key();
	protected static final String doc_global_path 	= CouchKey._global.key();
	
	private String p_docid = "";
	
	public AbstractDocumentTransaction() {
		super();
	}
	
	@Override
	public AbstractDocumentTransaction clone() throws CloneNotSupportedException {
		AbstractDocumentTransaction clonedObject = (AbstractDocumentTransaction) super.clone();
		return clonedObject;
	}

	protected void removeRevFromDoc(JSONObject jsonDocument) {
		if (jsonDocument != null) {
			CouchKey._rev.remove(jsonDocument);
		}
	}

	public String getP_docid() {
		return p_docid;
	}

	public void setP_docid(String p_docid) {
		this.p_docid = p_docid;
	}
}
