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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class CredentialsStatement extends Statement {

	private static final long serialVersionUID = 1113097533588326257L;

	private String user = "";
	
	private String password = "";
	
	private boolean forceBasic = false;
	
	public CredentialsStatement() {
		super();
	}
	
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
    public boolean isForceBasic() {
		return forceBasic;
	}

	public void setForceBasic(boolean forceBasic) {
		this.forceBasic = forceBasic;
	}

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		String _user = null;
		String _password = null;
		
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				if (user != null && user.length() != 0) {
					// evaluate user
					evaluate(javascriptContext, scope, user, "user", true);
					try {
						_user = evaluated.toString();
					} catch (Exception e) {
						EngineException ee = new EngineException(
								"Invalid user.\n" +
								"CredentialsStatement: \"" + getName()+ "\"",e);
						throw ee;
					}
				}
				if (password != null && password.length() != 0) {
					// evaluate password
					evaluate(javascriptContext, scope, password, "password", true);
					try {
						_password = evaluated.toString();
					}
					catch (Exception e) {
						EngineException ee = new EngineException(
								"Invalid password.\n" +
								"CredentialsStatement: \"" + getName()+ "\"",e);
						throw ee;
					}
				}

				// Set basic credentials on connector
				HtmlConnector htmlConnector = (HtmlConnector) getParentTransaction().getParent();
				htmlConnector.setGivenBasicUser(_user);
				Engine.logBeans.debug("(CredentialsStatement) User '" + _user + "' has been set on http connector.");
				htmlConnector.setGivenBasicPassword(_password);
				Engine.logBeans.debug("(CredentialsStatement) Password '" + _password + "' has been set on http connector.");
				htmlConnector.getHtmlParser().setCredentials(htmlConnector.context, _user, _password, forceBasic);
				
				return true;
			}
		}
		return false;
	}

    @Override
	public String toString() {
		String text = this.getComment();
		return "[user,password]" + (forceBasic?"*":"") + (!text.equals("") ? " // " + text : "");
	}

    @Override
	public String toJsString() {
		return "";
	}
}