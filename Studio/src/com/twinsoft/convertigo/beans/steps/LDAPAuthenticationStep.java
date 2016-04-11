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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

			TWSLDAP ldap = new TWSLDAP();

			String ldap_server = server.getSingleString(this);
			String ldap_adminLogin = adminLogin.getSingleString(this);
			String ldap_adminPassword = adminPassword.getSingleString(this);
			String ldap_userLogin = login.getSingleString(this);
			String ldap_userPassword = password.getSingleString(this);
			
			Boolean authenticated = ldap.bind(ldap_server, ldap_userLogin, ldap_userPassword);
			Engine.logBeans.debug("LDAP User \""+ldap_userLogin+"\" authenticated="+ authenticated.toString());
			if (authenticated) {
				getSequence().context.setAuthenticatedUser(ldap_userLogin);
			}
			
			Node text = doc.createTextNode(authenticated.toString());
			stepNode.appendChild(text);
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
