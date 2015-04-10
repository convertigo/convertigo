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
import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchParam;

public class PutDocumentAttachmentTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = -689772083455858427L;

	private String p_attname = "";
	private String p_attpath = "";
	private String q_rev = "";
	
	public PutDocumentAttachmentTransaction() {
		super();
	}

	@Override
	public PutDocumentAttachmentTransaction clone() throws CloneNotSupportedException {
		PutDocumentAttachmentTransaction clonedObject =  (PutDocumentAttachmentTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	protected Object invoke() throws Exception {
		String db = getTargetDatabase();
		String docid = getParameterStringValue(CouchParam.docid);
		String attname = getParameterStringValue(CouchParam.attname);
		String attpath = getParameterStringValue(CouchParam.attpath);
		Map<String, String> query = getQueryVariableValues();
		
		attpath = Engine.theApp.filePropertyManager.getFilepathFromProperty(attpath, getProject().getName());
		String contentType = context.httpServletRequest.getServletContext().getMimeType(attpath);
		
		JSONObject response = getCouchClient().putDocumentAttachment(db, docid, attname, query, new File(attpath), contentType);
		
		return response;
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "putDocumentAttachmentType");
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

