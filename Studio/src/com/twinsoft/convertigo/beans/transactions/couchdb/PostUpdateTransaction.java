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
import java.util.Collection;
import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.engine.enums.CouchExtraVariable;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class PostUpdateTransaction extends AbstractDatabaseTransaction implements ITagsProperty, ICouchParametersExtra {

	private static final long serialVersionUID = -7606732916561433615L;

	private String updatename = "";

	private String p_ddoc = "";
	private String p_func = "";
	private String p_json_base = "";

	public PostUpdateTransaction() {
		super();
	}

	@Override
	public PostUpdateTransaction clone() throws CloneNotSupportedException {
		PostUpdateTransaction clonedObject =  (PostUpdateTransaction) super.clone();
		return clonedObject;
	}
		
	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		String ddoc;
		String func;
		
		Matcher mSplitUpdatename = DesignDocument.splitFunctionName.matcher(updatename);
		
		if (mSplitUpdatename.matches()) {
			ddoc = mSplitUpdatename.group(1);
			func = mSplitUpdatename.group(2);
		} else {
			ddoc = getParameterStringValue(CouchParam.ddoc);
			func = getParameterStringValue(CouchParam.func);
		}
		
		JSONObject jsonBase;
		
		try {
			jsonBase = new JSONObject(getParameterStringValue(CouchParam.json_base));
		} catch (Throwable t) {
			jsonBase = new JSONObject();
		}
		
		JSONObject jsonDocument = getJsonBody(jsonBase);
		
		JSONObject response = getCouchClient().postUpdate(db, ddoc, func, jsonDocument);
		
		return response;
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

	public String getP_ddoc() {
		return p_ddoc;
	}

	public void setP_ddoc(String p_ddoc) {
		this.p_ddoc = p_ddoc;
	}

	public String getP_func() {
		return p_func;
	}

	public void setP_func(String p_func) {
		this.p_func = p_func;
	}

	public String getP_json_base() {
		return p_json_base;
	}

	public void setP_json_base(String p_json_base) {
		this.p_json_base = p_json_base;
	}

	@Override
	public Collection<CouchExtraVariable> getCouchParametersExtra() {
		return Arrays.asList(CouchExtraVariable._id, CouchExtraVariable.data);
	}

}
