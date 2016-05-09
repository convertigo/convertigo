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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.LdapBindingPolicy;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.TWSLDAP;

public class LDAPAuthenticationStep extends Step implements IComplexTypeAffectation {

	private static final long serialVersionUID = -1894558458026853410L;

	private SmartType server = new SmartType();
	private SmartType login = new SmartType();
	private SmartType password = new SmartType();
	private SmartType adminLogin = new SmartType();
	private SmartType adminPassword = new SmartType();
	private SmartType basePath = new SmartType();
	private LdapBindingPolicy bindingPolicy = LdapBindingPolicy.Bind;
	
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
		// Remove currently authenticated user from session
		getSequence().context.removeAuthenticatedUser();
		
		String serverUrls = server.getSingleString(this);
		if (serverUrls == null || serverUrls.isEmpty()) {
			throw new EngineException("Invalid LDAP servers : null or empty");
		}
		
		// Server URL list
		StringTokenizer st = new StringTokenizer(serverUrls, "," , false);
		List<String> servers = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			servers.add(st.nextToken().trim());
		}
		
		// Search/Bind LDAP database for given user
		Boolean authenticated = false;
		String userDn = null;
		
		String userLogin = login.getSingleString(this);
		String userPassword = password.getSingleString(this);
		if (userLogin != null) {
			
			// Create TWSLDAP object
			TWSLDAP twsLDAP = new TWSLDAP();
			
			// Loop through server URLs
			int nbServers = servers.size();
			for (String serverUrl: servers) {
				userDn = null;
				
				if (serverUrl.isEmpty()) {
					Engine.logBeans.warn("(LDAPAuthenticationStep) Ignoring invalid LDAP server: empty URL");
					continue;
				}
				else if (!serverUrl.startsWith("ldap://") && !serverUrl.startsWith("ldaps://")) {
					Engine.logBeans.warn("(LDAPAuthenticationStep) Ignoring invalid LDAP server \""+serverUrl+"\": URL must start with \"ldap://\"");
					continue;
				}
				
				// Search database
				if (getBindingPolicy().equals(LdapBindingPolicy.SearchAndBind)) {
					String searchLogin = adminLogin.getSingleString(this);
					String searchPassword = adminPassword.getSingleString(this);
					String searchBase = basePath.getSingleString(this);
					
					if (searchLogin != null) {
						int countLimit = 0, timeLimit = 0;
						String searchHost = getHost(serverUrl);
						String searchFilter = getFilter(userLogin);
						String[] searchAttributes = null;

						// Avoid null password
						searchPassword = searchPassword == null ? "":searchPassword;
						
						// Search database for given user
						Engine.logBeans.trace("(LDAPAuthenticationStep) LDAP search start");
						twsLDAP.search(searchHost, searchLogin, searchPassword, searchBase, searchFilter, searchAttributes, timeLimit, countLimit);
						String errorMsg = twsLDAP.errorMessage != null ? " (Error: "+twsLDAP.errorMessage+")":"";
						boolean bFound = twsLDAP.hasMoreResults();
						Engine.logBeans.debug("(LDAPAuthenticationStep) LDAP search: host:"+searchHost+", searchBase:"+searchBase+", filter:"+ searchFilter + "; user "+ (bFound ? "found":"NOT found") + errorMsg);
						Engine.logBeans.trace("(LDAPAuthenticationStep) LDAP search end");
						
						if (bFound) {
							userDn = twsLDAP.getNextResult().toLowerCase() + (searchBase == null ? "":"," + searchBase.toLowerCase());
						}
						else {
							if (nbServers > 1) {
								continue; // loop
							}
						}
					}
					else {
						Engine.logBeans.warn("(LDAPAuthenticationStep) Invalid LDAP admin Login \""+searchLogin+"\" !");
					}
				}
				
				// Bind database with given/found user
				String errorMsg = "";
				Engine.logBeans.trace("(LDAPAuthenticationStep) LDAP bind start");
				String bindLogin = userDn == null ? userLogin:userDn;
				String bindPassword = userPassword;
				if (bindPassword != null && !bindPassword.isEmpty()) {
					authenticated = twsLDAP.bind(serverUrl, bindLogin, bindPassword);
					errorMsg = twsLDAP.errorMessage != null ? " (Error: "+twsLDAP.errorMessage+")":"";
				}
				else {
					authenticated = false;
					errorMsg = "; invalid password";
				}
				Engine.logBeans.debug("(LDAPAuthenticationStep) LDAP bind: user \""+bindLogin+"\"; authenticated="+ authenticated.toString() + errorMsg);
				Engine.logBeans.trace("(LDAPAuthenticationStep) LDAP bind end");
				
				// Set authenticated user on session
				if (authenticated) {
					// use given login
					String sessionLogin = userLogin;
					if (userDn != null && !isNTAccount(userLogin) && !isEMailAccount(userLogin) && !isDistinguishedName(userLogin)) {
						// use found distinguished name
						if (isFilter(userLogin)) {
							sessionLogin = userDn;
						}
					}
					getSequence().context.setAuthenticatedUser(sessionLogin);
					break; // exit loop
				}
				// else loop
			}
		}
		else {
			Engine.logBeans.warn("Invalid LDAP user Login \""+userLogin+"\" !");
		}
		
		stepNode.setAttribute("userDn", userDn == null ? "":userDn);
		if (userLogin != null && authenticated) {
			stepNode.appendChild(doc.createTextNode(userLogin));
		}
	}

	private static boolean isEMailAccount(String username) {
		boolean isEMail = false;
		if (username != null && !username.isEmpty()) {
			isEMail = username.indexOf("@") != -1
						&& username.indexOf(".") != -1;
		}
		return isEMail;
	}
	
	private static boolean isNTAccount(String username) {
		boolean isNT = false;
		if (username != null && !username.isEmpty()) {
			isNT = username.indexOf("\\") != -1;
		}
		return isNT;
	}
	
	private static boolean isDistinguishedName(String username) {
		boolean isDn = false;
		if (username != null && !username.isEmpty()) {
			String s = username.toLowerCase().replaceAll("\\s+","");
			isDn = s.indexOf("cn=") != -1 && s.indexOf(",") != -1;
		}
		return isDn;
	}
	
	private static boolean isFilter(String username) {
		boolean isFilter = false;
		if (username != null && !username.isEmpty()) {
			String s = username.toLowerCase().replaceAll("\\s+","");
			isFilter = 	s.indexOf("=") != -1;
		}
		return isFilter;
	}
	
	private static String getFilter(String username) {
		if (username != null && !isFilter(username)) {
			// check for EMail account or UPN
			if (isEMailAccount(username)) {
				return "userPrincipalName="+username;
			}
			// check for NT account
			if (isNTAccount(username)) {
				int index = username.indexOf("\\");
				return "samAccountName="+username.substring(index+1);
			}
			// simple username
			String s = username.toLowerCase().replaceAll("\\s+","");
			if (s.indexOf("cn=") == -1) {
				return "cn="+ username;
			}
		}
		return username;
	}

	private static String getHost(String ldap_url) {
		String host = "";
		try {
			URL ldapURL = new URL(ldap_url.toLowerCase().replaceFirst("ldap", "http"));
			host = ldapURL.getHost();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return host;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);
		
		XmlSchemaSimpleContent sContent = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContent());
		cType.setContentModel(sContent);
		
		XmlSchemaSimpleContentExtension sContentExt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContentExtension());
		sContent.setContent(sContentExt);
		sContentExt.setBaseTypeName(Constants.XSD_STRING);
		
		XmlSchemaAttribute attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("userDn");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		sContentExt.getAttributes().add(attr);
		
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

	public LdapBindingPolicy getBindingPolicy() {
		return bindingPolicy;
	}

	public void setBindingPolicy(LdapBindingPolicy bindingPolicy) {
		this.bindingPolicy = bindingPolicy;
	}
	
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("adminPassword".equals(propertyName) || "password".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("adminPassword".equals(propertyName) || "password".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}
}
