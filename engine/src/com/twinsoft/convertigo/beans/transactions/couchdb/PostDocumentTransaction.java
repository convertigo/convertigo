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
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.CouchPostDocumentPolicy;
import com.twinsoft.convertigo.engine.enums.FullSyncAclPolicy;
import com.twinsoft.convertigo.engine.providers.couchdb.FullSyncContext;

public class PostDocumentTransaction extends AbstractDatabaseTransaction implements ICouchParametersExtra{

	private static final long serialVersionUID = -7606732916561433014L;
	
	private CouchPostDocumentPolicy policy = CouchPostDocumentPolicy.none;

	private String p_json_base = "";
	private String p_merge = "";
	private String q_batch = "";
	private boolean useHash = false;
	private FullSyncAclPolicy fullSyncAclPolicy = FullSyncAclPolicy.fromAuthenticatedUser;
	
	public PostDocumentTransaction() {
		super();
	}

	@Override
	public PostDocumentTransaction clone() throws CloneNotSupportedException {
		PostDocumentTransaction clonedObject =  (PostDocumentTransaction) super.clone();
		return clonedObject;
	}
		
	@Override
	protected Object invoke() throws Exception {
		FullSyncContext.get().setFsAclPolicy(getFullSyncAclPolicy());
		String db = getTargetDatabase();
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject jsonBase;
		
		try {
			jsonBase = new JSONObject(getParameterStringValue(CouchParam.json_base));
		} catch (Throwable t) {
			jsonBase = new JSONObject();
		}
		
		JSONObject jsonDocument = getJsonBody(jsonBase);
		Map<List<String>, String> mergeRules = policy.mergeRules(getP_merge());
		
		JSONObject response = getCouchClient().postDocument(db, jsonDocument, query, policy, mergeRules, useHash);
		
		return response;
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postDocumentType");
	}
	
	public CouchPostDocumentPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(CouchPostDocumentPolicy policy) {
		this.policy = policy;
	}

	public String getP_json_base() {
		return p_json_base;
	}

	public void setP_json_base(String p_json_base) {
		this.p_json_base = p_json_base;
	}

	public String getP_merge() {
		return p_merge;
	}

	public void setP_merge(String p_merge) {
		this.p_merge = p_merge;
	}

	public String getQ_batch() {
		return q_batch;
	}

	public void setQ_batch(String q_batch) {
		this.q_batch = q_batch;
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
				CouchExtraVariable._id,
				CouchExtraVariable.data,
				CouchExtraVariable._rev,
				CouchExtraVariable._deleted,
				CouchExtraVariable._c8oAcl,
				CouchExtraVariable.c8oGrp
		) : Arrays.asList(
				CouchExtraVariable._id,
				CouchExtraVariable.data,
				CouchExtraVariable._rev,
				CouchExtraVariable._deleted
		);
	}	
}
