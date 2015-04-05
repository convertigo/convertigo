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

import javax.xml.namespace.QName;

import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;

public class PostSessionTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = -1385969696979516586L;
	
	private String p_name = "";
	private String p_password = "";
	
	public PostSessionTransaction() {
		super();
	}

	@Override
	public PostSessionTransaction clone() throws CloneNotSupportedException {
		PostSessionTransaction clonedObject =  (PostSessionTransaction) super.clone();
		return clonedObject;
	}

	@Override
	protected Object invoke() throws Exception {
		String name = getParameterStringValue(CouchParam.name);
		String password = getParameterStringValue(CouchParam.password);
		
		getConnector().setCouchClient(new CouchClient(getCouchClient().getServerUrl(), name, password));
		
		return getCouchClient().getSession();
	}

	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "postSessionType");
	}

	public String getP_name() {
		return p_name;
	}

	public void setP_name(String p_name) {
		this.p_name = p_name;
	}

	public String getP_password() {
		return p_password;
	}

	public void setP_password(String p_password) {
		this.p_password = p_password;
	}
}
