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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.variables.RequestableVariable;

public class BulkDocumentsTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = -7182426831481176387L;

	private boolean handleUpdate = true;
	
	public BulkDocumentsTransaction() {
		super();
	}
	
	public boolean isHandleUpdate() {
		return handleUpdate;
	}

	public void setHandleUpdate(boolean handleUpdate) {
		this.handleUpdate = handleUpdate;
	}

	@Override
	public BulkDocumentsTransaction clone() throws CloneNotSupportedException {
		BulkDocumentsTransaction clonedObject =  (BulkDocumentsTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return Arrays.asList(new CouchDbParameter[] {var_database, var_ids, var_datas});
	}
	
	@Override
	protected Object invoke() throws Exception {
		JSONArray jsonDocuments = new JSONArray();
		
		// add document members from variables
		for (RequestableVariable variable : getVariablesList()) {
			if (variable.isMultiValued()) {
				String variableName = variable.getName();
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
		
		if (isHandleUpdate()) {
			return getCouchClient().updateBulkDocs(getTargetDatabase(), jsonDocuments);
		} else {
			return getCouchClient().postBulkDocs(getTargetDatabase(), jsonDocuments);
		}
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "docBulkType");
	}
}

