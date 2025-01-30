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
	BasicPreemptive,
	NTLM;
	
	static private Credentials ac = new Credentials() {};
	AuthenticationMode() {
	}

	static public AuthenticationMode get(String mode) {
		if (Anonymous.name().equalsIgnoreCase(mode)) {
			return Anonymous;
		} else if (Basic.name().equalsIgnoreCase(mode)) {
			return Basic;
		} else if (BasicPreemptive.name().equalsIgnoreCase(mode)) {
			return BasicPreemptive;
		} else if (NTLM.name().equalsIgnoreCase(mode)) {
			return NTLM;
		}
		return None;
	}
	
	private int getType() {
		if (this.equals(Anonymous)) {
			return 0;
		} else if (this.equals(Basic)) {
			return 1;
		} else if (this.equals(NTLM)) {
			return 2;
		} else if (this.equals(BasicPreemptive)) {
			return 3;
		}
		return -1;
	}

	@SuppressWarnings("deprecation")
	public boolean setCredentials(HttpState httpState, String user, String password, String host, String domain) {
		if (httpState == null)
			return false;
		if (host == null)
			return false;
		
		AuthScope authScope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
		
		Credentials credentials = null;
		int type = getType();
		try {
			boolean preemptive = false;
			switch (type) {
				case 0: // Anonymous
					credentials = ac;
					break;
				case 1: // Basic
					credentials = new UsernamePasswordCredentials(user, password);
					break;
				case 2: // NTLM
					credentials = new NTCredentials(user, password, host, domain);
					break;
				case 3: // Basic Preemptive
					credentials = new UsernamePasswordCredentials(user, password);
					preemptive = true;
					break;
				default: // None
				case -1:
					break;
			}
			
			httpState.setAuthenticationPreemptive(preemptive);
			
			Credentials curCred = httpState.getCredentials(authScope);
			int needChange = compare(curCred, credentials);
			switch (needChange) {
				case -1:
					httpState.setCredentials(authScope, null);
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
			
			return needChange != 0;
			
		} catch (Exception e) {
			Engine.logEngine.error("Unable to set "+ name() +" credentials for user", e);
			return false;
		}
	}
	
	private int compare(Credentials cred1, Credentials cred2) {
		if (cred1 == null && cred2 == null) {
			return 0;
		}
		else if (cred2 == null) {
			return -1;
		}
		else if (cred1 == null) {
			return 1;
		}
		
		if (!cred1.getClass().equals(cred2.getClass())) {
			return 1;
		}
		else if (cred1.equals(cred2)) {
			return 0;
		}
		else {
			return 1;
		}
	}
}
