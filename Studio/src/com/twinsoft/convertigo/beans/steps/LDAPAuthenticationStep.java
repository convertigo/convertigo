/*
* Copyright (c) 2001-2014 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/editors/completion/CtfCompletionProposalsComputer.java $
 * $Author: jmc $
 * $Revision: 37416 $
 * $Date: 2014-06-24 15:45:16 +0200 (Tue, 24 Jun 2014) $
 */

package com.twinsoft.convertigo.beans.steps;

import java.net.URL;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.util.TWSLDAP;

public class LDAPAuthenticationStep extends Step implements IComplexTypeAffectation {

	private static final long serialVersionUID = -1894558458026853410L;

	private SmartType server = new SmartType();
	private SmartType login = new SmartType();
	private SmartType password = new SmartType();
	private SmartType adminLogin = new SmartType();
	private SmartType adminPassword = new SmartType();
	private SmartType basePath = new SmartType();
	
	public LDAPAuthenticationStep() {
		super();
		setOutput(false);
		this.xml = true;
	}

	@Override
    public LDAPAuthenticationStep clone() throws CloneNotSupportedException {
    	LDAPAuthenticationStep clonedObject = (LDAPAuthenticationStep) super.clone();
        return clonedObject;
    }

	@Override
    public LDAPAuthenticationStep copy() throws CloneNotSupportedException {
    	LDAPAuthenticationStep copiedObject = (LDAPAuthenticationStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toJsString() {
		return null;
	}

	@Override
	public String getStepNodeName() {
		return "LDAPAuthenticated";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			evaluate(javascriptContext, scope, server);
			evaluate(javascriptContext, scope, basePath);
			evaluate(javascriptContext, scope, login);
			evaluate(javascriptContext, scope, password);
			evaluate(javascriptContext, scope, adminLogin);
			evaluate(javascriptContext, scope, adminPassword);
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String serverUrl = null, userDn = null, userLogin = null, userPassword = null;	
		Boolean authenticated = false;
		
		// Remove currently authenticated user from session
		getSequence().context.removeAuthenticatedUser();
		
		serverUrl = server.getSingleString(this);
		if (serverUrl != null && !serverUrl.isEmpty()) {
			TWSLDAP twsLDAP = new TWSLDAP();
			
			// Search LDAP database for given user
			userLogin = login.getSingleString(this);
			userPassword = password.getSingleString(this);
			if (userLogin != null) {
				if (!isDistinguishedName(userLogin)) {
					String searchLogin = adminLogin.getSingleString(this);
					String searchPassword = adminPassword.getSingleString(this);
					String searchBase = basePath.getSingleString(this);
					
					if (searchLogin != null) {
						int countLimit = 0, timeLimit = 0;
						String searchHost = getHost(serverUrl);
						String searchFilter = getFilter(userLogin);
						String[] searchAttributes = null;
						
						if (searchHost.isEmpty()) {
							Engine.logBeans.warn("LDAP host is empty !");
						}
						searchPassword = searchPassword == null ? "":searchPassword;
						
						// Search database for given user
						Engine.logBeans.trace("Start LDAP search");
						twsLDAP.search(searchHost, searchLogin, searchPassword, searchBase, searchFilter, searchAttributes, timeLimit, countLimit);
						Engine.logBeans.trace("End LDAP search");
						
						boolean bFound = twsLDAP.hasMoreResults();
						Engine.logBeans.debug("LDAP User "+ (bFound ? "found":"NOT found") +" by database search; searchBase:"+searchBase+", filter:"+ searchFilter);
						if (bFound) {
							userDn = twsLDAP.getNextResult() + (searchBase == null ? "":"," + searchBase);
						}
					}
					else {
						Engine.logBeans.warn("Invalid LDAP admin Login \""+searchLogin+"\" !");
					}
				}
				
				userPassword = userPassword == null ? "":userPassword; //avoid NPE in bind
				
				// Bind database with given/found user
				Engine.logBeans.trace("Start LDAP bind");
				authenticated = twsLDAP.bind(serverUrl, userDn == null ? userLogin:userDn, userPassword);
				Engine.logBeans.trace("End LDAP bind");
				
				Engine.logBeans.debug("LDAP User \""+userLogin+"\" authenticated="+ authenticated.toString());
				
				// Set authenticated user on session
				if (authenticated) {
					getSequence().context.setAuthenticatedUser(userLogin);
				}
			}
			else {
				Engine.logBeans.warn("Invalid LDAP user Login \""+userLogin+"\" !");
			}
		}
		else {
			Engine.logBeans.warn("Invalid LDAP server \""+serverUrl+"\" !");
		}
		
		stepNode.setAttribute("userDn", userDn == null ? "":userDn);
		if (userLogin != null) {
			stepNode.appendChild(doc.createTextNode(userLogin));
		}
	}

	private Boolean isDistinguishedName(String username) {
		return username != null && username.toLowerCase().indexOf("cn=") != -1;
	}
	
	private String getFilter(String username) {
		if (username != null && !isDistinguishedName(username)) {
			return "cn="+ username;
		}
		return username;
	}

	private String getHost(String ldap_url) {
		try {
			URL ldapURL = new URL(ldap_url.replaceFirst("ldap:", "http:"));
			return ldapURL.getHost();
		} catch (Exception e) {
		}
		return null;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(getSimpleTypeAffectation());
		return element;
	}
	
	public SmartType getServer() {
		return server;
	}
	
	public void setServer(SmartType server) {
		this.server = server;
	}
	
	public SmartType getLogin() {
		return login;
	}
	
	public void setLogin(SmartType login) {
		this.login = login;
	}
	
	
	public SmartType getPassword() {
		return password;
	}
	
	public void setPassword(SmartType password) {
		this.password = password;
	}	

	public SmartType getAdminLogin() {
		return adminLogin;
	}

	public void setAdminLogin(SmartType adminLogin) {
		this.adminLogin = adminLogin;
	}

	public SmartType getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(SmartType adminPassword) {
		this.adminPassword = adminPassword;
	}

	public SmartType getBasePath() {
		return basePath;
	}

	public void setBasePath(SmartType basePath) {
		this.basePath = basePath;
	}
}
