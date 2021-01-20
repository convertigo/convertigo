/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.CouchPostDocumentPolicy;
import com.twinsoft.convertigo.engine.enums.FullSyncAclPolicy;
import com.twinsoft.convertigo.engine.providers.couchdb.FullSyncContext;

public class PostBulkDocumentsTransaction extends AbstractDatabaseTransaction implements ICouchParametersExtra{

	private static final long serialVersionUID = -7182426831481176387L;

	private CouchPostDocumentPolicy policy = CouchPostDocumentPolicy.none;
	
	private String p_all_or_nothing = "";
	private String p_new_edits = "";
	private String p_json_base = "";
	private boolean useHash = false;
	private FullSyncAclPolicy fullSyncAclPolicy = FullSyncAclPolicy.fromAuthenticatedUser;
	
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
		FullSyncContext.get().setFsAclPolicy(getFullSyncAclPolicy());
		
		JSONObject jsonDoc = null;
		JSONArray jsonDocuments;
		
		String json_base = getParameterStringValue(CouchParam.json_base);
		
		try {			
			jsonDocuments = new JSONArray(json_base);
		} catch (Throwable t1) {
			try {
				jsonDoc = new JSONObject(json_base);
			} catch (Throwable t2) {
				// ignore json_base
			}
			jsonDocuments = new JSONArray();
		}
		
		// add document members from variables
		for (RequestableVariable variable : getVariablesList()) {
			String variableName = variable.getName();
			if (variable.isMultiValued() &&
					!variableName.startsWith(CouchParam.prefix) &&
					!variableName.startsWith("__")) {
				Object jsonv = toJson(getParameterValue(variableName));
				
				if (jsonv != null && jsonv instanceof JSONArray) {
					JSONArray jsonArray = (JSONArray) jsonv;
					
					while (jsonDocuments.length() < jsonArray.length()) {
						jsonDocuments.put(jsonDoc == null ? new JSONObject() : new JSONObject(json_base));
					}
					
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonDocument = jsonDocuments.getJSONObject(i);
						addJson(jsonDocument, variableName, jsonArray.get(i), getParameterDataTypeClass(variableName));
					}
				}
			}
		}
		
		
		for (RequestableVariable variable : getVariablesList()) {
			String variableName = variable.getName();
			if (!variable.isMultiValued() &&
					!variableName.startsWith(CouchParam.prefix) &&
					!variableName.startsWith("__")) {
				Object jsonv = toJson(getParameterValue(variableName));
				
				for (int i = 0; i < jsonDocuments.length(); i++) {
					JSONObject jsonDocument = jsonDocuments.getJSONObject(i);
					addJson(jsonDocument, variableName, jsonv, getParameterDataTypeClass(variableName));
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

	public String getP_json_base() {
		return p_json_base;
	}

	public void setP_json_base(String p_json_base) {
		this.p_json_base = p_json_base;
	}

	public boolean isUseHash() {
		return useHash;
	}

	public void setUseHash(boolean useHash) {
		this.useHash = useHash;
	}

	public FullSyncAclPolicy getFullSyncAclPolicy() {
		return fullSyncAclPolicy;
	}

	public void setFullSyncAclPolicy(FullSyncAclPolicy fullSyncAclPolicy) {
		this.fullSyncAclPolicy = fullSyncAclPolicy;
	}

	@Override
	public Collection<CouchExtraVariable> getCouchParametersExtra() {
		return getConnector() instanceof FullSyncConnector ? Arrays.asList(
				CouchExtraVariable._ids,
				CouchExtraVariable._revs,
				CouchExtraVariable._deleted,
				CouchExtraVariable._deleteds,
				CouchExtraVariable.data,
				CouchExtraVariable.datas,
				CouchExtraVariable._c8oAcl,
				CouchExtraVariable._c8oAcls,
				CouchExtraVariable.c8oGrp,
				CouchExtraVariable.c8oGrps
		) : Arrays.asList(
				CouchExtraVariable._ids,
				CouchExtraVariable._revs,
				CouchExtraVariable._deleted,
				CouchExtraVariable._deleteds,
				CouchExtraVariable.data,
				CouchExtraVariable.datas
		);
	}
}
