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
package com.twinsoft.convertigo.engine.enums;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import com.twinsoft.convertigo.engine.Engine;

public enum AuthenticationMode {
	None,
	Anonymous,
	Basic,
	NTLM;
	
	AuthenticationMode() {
	}

	static public AuthenticationMode get(String mode) {
		if (Anonymous.name().equalsIgnoreCase(mode))
			return Anonymous;
		if (Basic.name().equalsIgnoreCase(mode))
			return Basic;
		if (NTLM.name().equalsIgnoreCase(mode))
			return NTLM;
		return None;
	}
	
	private int getType() {
		if (this.equals(Anonymous))
			return 0;
		if (this.equals(Basic))
			return 1;
		if (this.equals(NTLM))
			return 2;
		return -1;
	}
	
	public void setCredentials(HttpState httpState, String user, String password, String host, String domain) {
		if (httpState == null)
			return;
		
		AuthScope authScope = null;
		Credentials credentials = null;
		int type = getType();
		try {
			switch (type) {
				case 0:
					authScope = new AuthScope(AuthScope.ANY);
					credentials = new Credentials() {};
					break;
				case 1:
					authScope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
					credentials = new UsernamePasswordCredentials(user, password);
					break;
				case 2:
					authScope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
					credentials = new NTCredentials(user, password, host, domain);
					break;
				default:
				case -1:
					break;
			}
			
			Credentials curCred = httpState.getCredentials(new AuthScope(AuthScope.ANY));
			int needChange = compare(curCred, credentials);
			switch (needChange) {
				case -1:
					httpState.clearCredentials();
					Engine.logEngine.debug("(AuthenticationMode) credentials cleared");
					break;
				case 1:
					httpState.setCredentials(authScope, credentials);
					Engine.logEngine.debug("(AuthenticationMode) "+ name() +" credentials: " + user + ": ******");
					break;
				case 0:
					Engine.logEngine.debug("(AuthenticationMode) reusing credentials");
					break;
				}
		} catch (Exception e) {
			Engine.logEngine.error("Unable to set "+ name() +" credentials for user", e);
		}
	}
	
	private int compare(Credentials cred1, Credentials cred2) {
		if (cred2 == null)
			return -1; // clear
		
		if (cred1 == null)
			return 1; // set
		
		if (!cred1.getClass().equals(cred2.getClass())) {
			return 1; // set
		}
		else {
			if (cred1 instanceof NTCredentials) {
				NTCredentials ntc1 = (NTCredentials)cred1;
				NTCredentials ntc2 = (NTCredentials)cred2;
				if (ntc1.getUserName().equals(ntc2.getUserName()) &&
					ntc1.getPassword().equals(ntc2.getPassword()) &&
					ntc1.getDomain().equals(ntc2.getDomain()) &&
					ntc1.getHost().equals(ntc2.getHost()))
				{
					return 0; // nothing to do
				}
			}
			else if (cred1 instanceof UsernamePasswordCredentials) {
				UsernamePasswordCredentials ntc1 = (UsernamePasswordCredentials)cred1;
				UsernamePasswordCredentials ntc2 = (UsernamePasswordCredentials)cred2;
				if (ntc1.getUserName().equals(ntc2.getUserName()) &&
					ntc1.getPassword().equals(ntc2.getPassword()))
				{
					return 0; // nothing to do
				}
			}
			else {
				return 0; // nothing to do
			}
			
			return 1; // set
		}
	}
}
