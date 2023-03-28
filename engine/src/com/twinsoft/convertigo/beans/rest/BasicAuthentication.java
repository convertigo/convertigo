/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.beans.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.Base64;

public class BasicAuthentication extends UrlAuthentication {

	private static final long serialVersionUID = 4461767732159075719L;

	public BasicAuthentication() {
		super();
	}

	@Override
	public AuthenticationType getType() {
		return AuthenticationType.Basic;
	}

	@Override
	public BasicAuthentication clone() throws CloneNotSupportedException {
		BasicAuthentication clonedObject = (BasicAuthentication)super.clone();
		return clonedObject;
	}
	
	@Override
	public String handleAuthRequest(HttpServletRequest request, HttpServletResponse response) throws EngineException {
		String authRequestableQName = getAuthRequestable();
		if (authRequestableQName.isEmpty()) {
			throw new EngineException("Authentication \""+ getName() +"\" has no auth requestable defined");
		}
		
		HttpSession httpSession = request.getSession();
		
		StringTokenizer st = new StringTokenizer(authRequestableQName,".");
		int count = st.countTokens();
		String projectName = st.nextToken();
		String sequenceName = count == 2 ? st.nextToken():"";
		String connectorName = count == 3 ? st.nextToken():"";
		String transactionName = count == 3 ? st.nextToken():"";
		String contextName = request.getParameter(Parameter.Context.getName());
		
		try {
			String authorization = request.getHeader(HeaderName.Authorization.value());
			if (authorization != null) {
				Engine.logEngine.debug("(BasicAuthentication) Authorization header found: " + authorization);
				
				// Retrieve credentials
				String credentials = authorization.split("\\s")[1];
				String[] decoded = Base64.decodeToString(credentials).split(":");
				String user = decoded.length > 0 ? decoded[0]:null;
				String password = decoded.length > 1 ? decoded[1]:null;
				
				// Check user is authenticated with same credentials
				String authenticatedUser = SessionAttribute.authenticatedUser.string(httpSession);
				if (authenticatedUser != null && authenticatedUser.equals(user)) {
					if (authorization.equals(httpSession.getAttribute("basic-authorization"))) {
						Engine.logEngine.debug("(BasicAuthentication) User already authenticated");
						return null;
					}
				}
				
				// Prepare Auth requestable
				Map<String, Object> map = new HashMap<String, Object>();
		    	map.put(Parameter.Context.getName(), new String[] { contextName });
		    	map.put(Parameter.Project.getName(), new String[] { projectName });
				if (sequenceName.isEmpty()) {
					map.put(Parameter.Connector.getName(), new String[] { connectorName });
					map.put(Parameter.Transaction.getName(), new String[] { transactionName });
				}
				else {
					map.put(Parameter.Sequence.getName(), new String[] { sequenceName });
				}
				if (user != null) {
					map.put("user", user);
				}
				if (password != null) {
					map.put("password", password);
				}
				
				// Execute Auth requestable
				Engine.logBeans.debug("(BasicAuthentication) Executing requestable \""+ authRequestableQName +"\"");
	        	InternalRequester internalRequester = new InternalRequester(map, request);
				request.setAttribute("convertigo.requester", internalRequester);
	    		internalRequester.processRequest();
	    		
				// Authentication failed
    			String authUser = SessionAttribute.authenticatedUser.string(httpSession);	    		
	    		if (authUser == null) {
	    			Map<Integer, String> status = RequestAttribute.responseStatus.get(request); // custom status
	    			if (status.isEmpty()) {
	    				// Set response status code
	    				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    			}
	    		}
	    		// Store Authorization
	    		else {
	    			if (!authUser.equals(user)) {
	    				Engine.logEngine.warn("(BasicAuthentication) Session "+ httpSession.getId() + 
	    								" has been authenticated with "+ authUser + " instead of "+ user);
	    			}
	    			httpSession.setAttribute("basic-authorization", authorization);
	    		}
			} else {
				Engine.logEngine.debug("(BasicAuthentication) Authorization header NOT found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setHeader(HeaderName.Authenticate.value(), "Basic realm=\""+ projectName +" access\"");
			}
			
			return null;
		}
		catch (Throwable t) {
			request.setAttribute("convertigo.requireEndOfContext", true);
			throw new EngineException("Authentication \""+ getName() +"\" failed to retrieve data", t);
		}
	}

}
