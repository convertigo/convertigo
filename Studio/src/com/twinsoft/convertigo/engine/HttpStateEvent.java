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

package com.twinsoft.convertigo.engine;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

public class HttpStateEvent extends java.util.EventObject {

	private static final long serialVersionUID = -4722575737270897917L;

	private HttpState httpState = null;
	private String realm = null;
	private String host = null;
	private Context context = null;
	
	/** Creates new EngineEvent */
    public HttpStateEvent(Object source, Context context, String realm, String host, HttpState httpState) {
        super(source);
        this.context = context;
        this.httpState = httpState;
        this.realm = realm;
        this.host = host;
    }
	
    private Credentials getCredentials() {
    	Credentials credentials = null;
    	if (httpState != null) {
    		if (host != null) {
    			AuthScope authScope = new AuthScope(host, AuthScope.ANY_PORT, realm, AuthScope.ANY_SCHEME);
    			credentials = httpState.getCredentials(authScope);
    		}
    	}
    	return credentials;
    }
    
    public Context getContext() {
    	return context;
    }
    
    public String getUserName() {
    	String userName = null;
    	Credentials credentials = getCredentials();
    	if (credentials != null) {
    		if (credentials instanceof UsernamePasswordCredentials) {
    			userName = ((UsernamePasswordCredentials)credentials).getUserName();
    		}
    	}
    	
    	return userName;
    }
    
    public String getUserPassword() {
    	String userPassword = null;
    	Credentials credentials = getCredentials();
    	if (credentials != null) {
    		if (credentials instanceof UsernamePasswordCredentials) {
    			userPassword = ((UsernamePasswordCredentials)credentials).getPassword();
    		}
    	}
    	
    	return userPassword;
    }
}
