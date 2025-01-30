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

package com.twinsoft.convertigo.beans.steps;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.LdapBindingPolicy;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.TWSLDAP;

public class LDAPAuthenticationStep extends Step implements IStepSmartTypeContainer, IComplexTypeAffectation {

	private static final long serialVersionUID = -1894558458026853410L;
	private static final Pattern pRDN = Pattern.compile("(\\w+)=([^,]*),?");

	private SmartType server = new SmartType();
	private SmartType login = new SmartType();
	private SmartType password = new SmartType();
	private SmartType adminLogin = new SmartType();
	private SmartType adminPassword = new SmartType();
	private SmartType basePath = new SmartType();
	private SmartType attributes = new SmartType();
	private LdapBindingPolicy bindingPolicy = LdapBindingPolicy.Bind;
	
	public LDAPAuthenticationStep() {
		super();
		setOutput(false);
		this.xml = true;
	}

	@Override
	public LDAPAuthenticationStep clone() throws CloneNotSupportedException {
		LDAPAuthenticationStep clonedObject = (LDAPAuthenticationStep) super.clone();
		clonedObject.smartTypes = null;
		clonedObject.server = server.clone();
		clonedObject.login = login.clone();
		clonedObject.password = password.clone();
		clonedObject.adminLogin = adminLogin.clone();
		clonedObject.adminPassword = adminPassword.clone();
		clonedObject.basePath = basePath.clone();
		clonedObject.attributes = attributes.clone();
		return clonedObject;
	}

	@Override
	public LDAPAuthenticationStep copy() throws CloneNotSupportedException {
		LDAPAuthenticationStep copiedObject = (LDAPAuthenticationStep) super.copy();
		return copiedObject;
	}

	private transient Set<SmartType> smartTypes = null;
	
	@Override
	public Set<SmartType> getSmartTypes() {
		if (smartTypes != null) {
			if (!hasChanged) {
				return smartTypes;
			} else {
				smartTypes.clear();
			}
		} else {
			smartTypes = new HashSet<SmartType>();
		}
		smartTypes.add(server);
		smartTypes.add(login);
		smartTypes.add(password);
		smartTypes.add(adminLogin);
		smartTypes.add(adminPassword);
		smartTypes.add(basePath);
		smartTypes.add(attributes);
		return smartTypes;
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
		if (isEnabled()) {
			evaluate(javascriptContext, scope, "server", server);
			evaluate(javascriptContext, scope, "basePath", basePath);
			evaluate(javascriptContext, scope, "login", login);
			evaluate(javascriptContext, scope, "password", password);
			evaluate(javascriptContext, scope, "adminLogin", adminLogin);
			evaluate(javascriptContext, scope, "adminPassword", adminPassword);
			evaluate(javascriptContext, scope, "attributes", attributes);
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
						String[] searchAttributes = attributes.getStringArray(this);
						
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
							Matcher mRDN = pRDN.matcher(userDn);
							
							while (mRDN.find()) {
								Element rdn = doc.createElement("rdn");
								rdn.setAttribute("name", mRDN.group(1));
								rdn.setTextContent(mRDN.group(2));
								stepNode.appendChild(rdn);
							}
							
							for (String attribute: searchAttributes) {
								if (StringUtils.isNotBlank(attribute)) {
									String[] values = twsLDAP.getResultEx(attribute);
									if (values != null) {
										for (String value: values) {
											Element attr = doc.createElement("attribute");
											attr.setAttribute("name", attribute);
											attr.setTextContent(value);
											stepNode.appendChild(attr);
										}
									}
								}
							}
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
		

		Element user = doc.createElement("userDn");
		user.setTextContent(userDn == null ? "" : userDn);
		stepNode.appendChild(user);
		
		if (userLogin != null && authenticated) {
			user = doc.createElement("authenticatedUserID");
			user.setTextContent(userLogin);
			stepNode.appendChild(user);
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
		
		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		
		XmlSchemaElement subElement = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(subElement);
		
		subElement.setName("rdn");
		subElement.setMinOccurs(0);
		subElement.setMaxOccurs(Long.MAX_VALUE);
		cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		subElement.setType(cType);
		
		XmlSchemaSimpleContent sContent = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContent());
		cType.setContentModel(sContent);
		
		XmlSchemaSimpleContentExtension sContentExt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContentExtension());
		sContent.setContent(sContentExt);
		sContentExt.setBaseTypeName(Constants.XSD_STRING);
		
		XmlSchemaAttribute attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("name");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		sContentExt.getAttributes().add(attr);
		
		subElement = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(subElement);
		
		subElement.setName("attribute");
		subElement.setMinOccurs(0);
		subElement.setMaxOccurs(Long.MAX_VALUE);
		cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		subElement.setType(cType);
		
		sContent = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContent());
		cType.setContentModel(sContent);
		
		sContentExt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContentExtension());
		sContent.setContent(sContentExt);
		sContentExt.setBaseTypeName(Constants.XSD_STRING);
		
		attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("name");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		sContentExt.getAttributes().add(attr);
		
		subElement = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(subElement);
		subElement.setName("userDn");
		subElement.setMinOccurs(0);
		subElement.setMaxOccurs(1);
		subElement.setSchemaTypeName(Constants.XSD_STRING);
		
		subElement = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(subElement);
		subElement.setName("authenticatedUserID");
		subElement.setMinOccurs(0);
		subElement.setMaxOccurs(1);
		subElement.setSchemaTypeName(Constants.XSD_STRING);
		/*
		cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		subElement.setType(cType);
		
		attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("name");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		cType.getAttributes().add(attr);
		
		sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		sequence.setMinOccurs(0);
		sequence.setMaxOccurs(Long.MAX_VALUE);
		cType.setParticle(sequence);
		
		subElement = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(subElement);
		subElement.setName("value");
		subElement.setSchemaTypeName(Constants.XSD_STRING);
		*/
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

	public SmartType getAttributes() {
		return attributes;
	}

	public void setAttributes(SmartType attributes) {
		this.attributes = attributes;
	}
}
