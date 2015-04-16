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

import java.util.Collection;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.CouchPostDocumentPolicy;

public class PostBulkDocumentsTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = -7182426831481176387L;

	private CouchPostDocumentPolicy policy = CouchPostDocumentPolicy.none;
	
	private String p_all_or_nothing = "";
	private String p_new_edits = "";
	private boolean useHash = false;
	
	public PostBulkDocumentsTransaction() {
		super();
	}

	@Override
	public PostBulkDocumentsTransaction clone() throws CloneNotSupportedException {
		PostBulkDocumentsTransaction clonedObject =  (PostBulkDocumentsTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected Object invoke() throws Exception {
		JSONArray jsonDocuments;
		
		if (getVariable("_json") != null) {
			Object value = getParameterValue("_json");
			
			if (value instanceof Collection) {
				value = ((Collection<?>) value).iterator().next();
			}
			
			jsonDocuments = new JSONArray(value.toString());
		} else {
			jsonDocuments = new JSONArray();
		}
		
		// add document members from variables
		for (RequestableVariable variable : getVariablesList()) {
			String variableName = variable.getName();
			if (variable.isMultiValued() && !variableName.startsWith(CouchParam.prefix) && !variableName.equals("_json")) {
				Object jsonv = toJson(getParameterValue(variableName));
				
				if (jsonv != null && jsonv instanceof JSONArray) {
					JSONArray jsonArray = (JSONArray) jsonv;
					
					while (jsonDocuments.length() < jsonArray.length()) {
						jsonDocuments.put(new JSONObject());
					}
					
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonDocument = jsonDocuments.getJSONObject(i);
						addJson(jsonDocument, variableName, jsonArray.get(i));
					}
				}
			}
		}
		
		boolean all_or_nothing = getParameterBooleanValue(CouchParam.all_or_nothing, false);
		boolean new_edits = getParameterBooleanValue(CouchParam.new_edits, true);
		
		return getCouchClient().postBulkDocs(getTargetDatabase(), jsonDocuments, all_or_nothing, new_edits, policy, useHash);
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postBulkDocumentsType");
	}
	
	public CouchPostDocumentPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(CouchPostDocumentPolicy policy) {
		this.policy = policy;
	}

	public String getP_all_or_nothing() {
		return p_all_or_nothing;
	}

	public void setP_all_or_nothing(String p_all_or_nothing) {
		this.p_all_or_nothing = p_all_or_nothing;
	}

	public String getP_new_edits() {
		return p_new_edits;
	}

	public void setP_new_edits(String p_new_edits) {
		this.p_new_edits = p_new_edits;
	}

	public boolean isUseHash() {
		return useHash;
	}

	public void setUseHash(boolean useHash) {
		this.useHash = useHash;
	}
}
