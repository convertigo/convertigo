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

package com.twinsoft.convertigo.beans.transactions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.EngineException;

public class SapJcoLogonTransaction extends SapJcoTransaction {

	private static final long serialVersionUID = 215416864195402157L;

	public static final String jco_user = "jcoUser";
	public static final String jco_pwd = "jcoPassword";
	public static final String jco_client = "jcoClient";
	
	public SapJcoLogonTransaction() {
		this.bapiName = "";
	}

	@Override
	public SapJcoTransaction clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String getJcoUser() {
		String jcoUser = ((SapJcoConnector)parent).getUser();
		try {
			Object ob = getVariableValue(jco_user);
			if (ob != null) {
				jcoUser = (String)ob;
			}
		}
		catch (Exception e) {
			// case variable does not exist or has been deleted
			e.printStackTrace();
		}
		return jcoUser;
	}

	public String getJcoPassword() {
		String jcoPassword = ((SapJcoConnector)parent).getPassword();
		try {
			Object ob = getVariableValue(jco_pwd);
			if (ob != null) {
				jcoPassword = (String)ob;
			}
		}
		catch (Exception e) {
			// case variable does not exist or has been deleted
			e.printStackTrace();
		}
		return jcoPassword;
	}

	public String getJcoClient() {
		String jcoClient = ((SapJcoConnector)parent).getClient();
		try {
			Object ob = getVariableValue(jco_client);
			if (ob != null) {
				jcoClient = (String)ob;
			}
		}
		catch (Exception e) {
			// case variable does not exist or has been deleted
			e.printStackTrace();
		}
		return jcoClient;
	}
	
	public void addCredentialsVariables() throws EngineException {
		if (getVariable(jco_user) == null) {
			RequestableVariable varUser = new RequestableVariable();
			varUser.bNew = true;
			varUser.setName(jco_user);
			addVariable(varUser);
			hasChanged = true;
		}
		
		if (getVariable(jco_pwd) == null) {
			RequestableVariable varPwd = new RequestableVariable();
			varPwd.bNew = true;
			varPwd.setName(jco_pwd);
			addVariable(varPwd);
			hasChanged = true;
		}
		
		if (getVariable(jco_client) == null) {
			RequestableVariable varClient = new RequestableVariable();
			varClient.bNew = true;
			varClient.setName(jco_client);
			addVariable(varClient);
			hasChanged = true;
		}
	}

	@Override
	public Object getVariableValue(String requestedVariableName) throws EngineException {
		Object value =  super.getVariableValue(requestedVariableName);
		if (value instanceof Document) {
			Document var_doc = (Document)value;
			NodeList nodelist = var_doc.getElementsByTagName(requestedVariableName);
			if (nodelist.getLength() > 0) {
				value = ((Element) nodelist.item(0)).getTextContent();
			}
		}
		return value;
	}
	
	
}
