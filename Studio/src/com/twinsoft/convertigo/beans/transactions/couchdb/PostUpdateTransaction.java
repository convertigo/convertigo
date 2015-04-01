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
import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.enums.CouchKey;

public class PostUpdateTransaction extends AbstractDocumentTransaction implements ITagsProperty {

	private static final long serialVersionUID = -7606732916561433615L;

	private String updatename = "";
	
	private String u_func = "";

	public PostUpdateTransaction() {
		super();
	}

	@Override
	public PostUpdateTransaction clone() throws CloneNotSupportedException {
		PostUpdateTransaction clonedObject =  (PostUpdateTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return getDeclaredParameters(var_database, var_docid, var_updatename, var_data);
	}
		
	@Override
	protected Object invoke() throws Exception {
		JSONObject jsonDocument = new JSONObject();
		String docId, updatename;
		
		Matcher mSplitUpdatename = DesignDocument.splitFunctionName.matcher(this.updatename);
		
		if (mSplitUpdatename.matches()) {
			docId = mSplitUpdatename.group(1);
			updatename = mSplitUpdatename.group(2);
		} else {
			docId = getParameterStringValue(var_docid);
			updatename = getParameterStringValue(var_updatename);
		}
		
		// add document members from variables
		for (RequestableVariable variable : getVariablesList()) {
			String variableName = variable.getName();

			if (!(variableName.equals(var_database.variableName()) ||
					variableName.equals(var_docid.variableName()) ||
					variableName.equals(var_updatename.variableName()))) {
				
				Object jsonElement = toJson(getParameterValue(variableName));
				addJson(jsonDocument, variableName, jsonElement);
			}
		}
		
		return getCouchClient().postUpdate(getTargetDatabase(), docId, updatename, jsonDocument);
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postUpdateType");
	}
	
	public String getUpdatename() {
		return updatename;
	}

	public void setUpdatename(String updatename) {
		this.updatename = updatename;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if ("updatename".equals(propertyName)) {
			return DesignDocument.getTags(this, CouchKey.updates);
		}
		return null;
	}

	public String getU_func() {
		return u_func;
	}

	public void setU_func(String u_func) {
		this.u_func = u_func;
	}
}
