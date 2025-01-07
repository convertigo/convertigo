/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.io.File;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class GetDocumentAttachmentTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = -1731027540919633324L;

	private String p_attname = "";
	private String p_attpath = "";
	private String q_rev = "";
	
	public GetDocumentAttachmentTransaction() {
		super();
	}

	@Override
	public GetDocumentAttachmentTransaction clone() throws CloneNotSupportedException {
		GetDocumentAttachmentTransaction clonedObject =  (GetDocumentAttachmentTransaction) super.clone();
		return clonedObject;
	}
		
	@Override
	protected Object invoke() throws Exception {
		File attfile = null;
		String db = getTargetDatabase();
		String docid = getParameterStringValue(CouchParam.docid);
		String attname = getParameterStringValue(CouchParam.attname);
		String attpath = getParameterStringValue(CouchParam.attpath);
		if (StringUtils.isNotBlank(attpath)) {
			attpath = Engine.theApp.filePropertyManager.getFilepathFromProperty(attpath, getProject().getName());
			attfile = new File(attpath);
		}
		
		Map<String, String> query = getQueryVariableValues();
		
		JSONObject response = getCouchClient().getDocumentAttachment(db, docid, attname, query, attfile);
		if (CouchKey._c8oEntity.has(response)) {
			CouchKey.data.put(response, CouchKey._c8oEntity.String(response));
			CouchKey._c8oEntity.remove(response);
		}
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "getDocumentAttachmentType");
	}

	public String getP_attname() {
		return p_attname;
	}

	public void setP_attname(String p_attname) {
		this.p_attname = p_attname;
	}

	public String getP_attpath() {
		return p_attpath;
	}

	public void setP_attpath(String p_attpath) {
		this.p_attpath = p_attpath;
	}

	public String getQ_rev() {
		return q_rev;
	}

	public void setQ_rev(String q_rev) {
		this.q_rev = q_rev;
	}
}

