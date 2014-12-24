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

import java.io.File;
import java.net.URI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.providers.couchdb.api.Document;

public abstract class AbstractDocumentTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = 3030579754950212900L;
	
	protected static final CouchDbParameter var_id 			= CouchDbParameter.Private_id;
	protected static final CouchDbParameter var_ids			= CouchDbParameter.Private_ids;
	protected static final CouchDbParameter var_rev 		= CouchDbParameter.Private_rev;
	
	protected static final CouchDbParameter var_filepath 	= CouchDbParameter.Param_filepath;
	protected static final CouchDbParameter var_filename 	= CouchDbParameter.Path_filename;
	protected static final CouchDbParameter var_datas 		= CouchDbParameter.Param_datas;
	protected static final CouchDbParameter var_data 		= CouchDbParameter.Param_data;
	protected static final CouchDbParameter var_docid 		= CouchDbParameter.Path_docid;
	protected static final CouchDbParameter var_docrev 		= CouchDbParameter.Param_docrev;
	protected static final CouchDbParameter var_viewname 	= CouchDbParameter.Path_viewname;
	protected static final CouchDbParameter var_view_limit	= CouchDbParameter.Param_view_limit;
	protected static final CouchDbParameter var_view_skip	= CouchDbParameter.Param_view_skip;
	
	protected static final String doc_id 		= "_id";
	protected static final String doc_rev 		= "_rev";

	protected static final String doc_base_path 	= "";
	protected static final String doc_design_path 	= "_design/";
	protected static final String doc_global_path 	= "_global/";
	
	public AbstractDocumentTransaction() {
		super();
	}
	
	@Override
	public AbstractDocumentTransaction clone() throws CloneNotSupportedException {
		AbstractDocumentTransaction clonedObject = (AbstractDocumentTransaction) super.clone();
		return clonedObject;
	}
	
	protected Document getCouchDBDocument() {
		return getCouchDBDatabase().document();
	}

	protected String generateID() {
		return Document.generateID(doc_base_path);
	}
	
	protected String getIdFromDoc(JsonObject jsonDocument) {
		if (jsonDocument == null) return null;
		JsonElement jsonDocId = jsonDocument.get(doc_id);
		if (jsonDocId != null) {
			return jsonDocId.getAsString();
		}
		return null;
	}
	
	protected void addIdToDoc(JsonObject jsonDocument) {
		if (jsonDocument == null) return;
		if (getIdFromDoc(jsonDocument) == null) {
			jsonDocument.addProperty(doc_id, generateID());
		}
	}
	
	protected void addRevToDoc(JsonObject jsonDocument) {
		if (jsonDocument == null) return;
		String docId = getIdFromDoc(jsonDocument);
		if (docId != null) {
			String docRev = getDocLastRev(docId);
			if (docRev != null) {
				jsonDocument.remove(doc_rev);
				jsonDocument.addProperty(doc_rev, docRev);
			}
		}
	}
	
	protected String getDocLastRev(String docId) {
		if (docId == null) return null;
		JsonElement jsonDocRev = getCouchDBDocument().exist(docId).getAsJsonObject().get("ETag");
		if (jsonDocRev != null) {
			return jsonDocRev.getAsString();
		}
		return null;
	}
		
	protected void removeRevFromDoc(JsonObject jsonDocument) {
		if (jsonDocument == null) return;
		jsonDocument.remove(doc_rev);
	}
	
	protected URI getFileURI(String filePath) {
		String projectName = getProject().getName();
		File file = Engine.theApp.filePropertyManager.getFileFromProperty(filePath, projectName);
		if (file != null) {
			return file.toURI();
		}
		return null;
	}
}
