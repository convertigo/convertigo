/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import java.util.HashSet;
import java.util.Set;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;

public class SetAuthenticatedUserStep extends Step implements IStepSmartTypeContainer, IComplexTypeAffectation {

	private static final long serialVersionUID = -1894558458026853410L;

	private SmartType userid = new SmartType();
	
	public SetAuthenticatedUserStep() {
		super();
		setOutput(false);
		this.xml = true;
	}

	@Override
    public SetAuthenticatedUserStep clone() throws CloneNotSupportedException {
    	SetAuthenticatedUserStep clonedObject = (SetAuthenticatedUserStep) super.clone();
    	clonedObject.smartTypes = null;
        return clonedObject;
    }

	@Override
    public SetAuthenticatedUserStep copy() throws CloneNotSupportedException {
    	SetAuthenticatedUserStep copiedObject = (SetAuthenticatedUserStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toJsString() {
		return userid.toString();
	}

	@Override
	public String getStepNodeName() {
		return "authenticatedUserID";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			evaluate(javascriptContext, scope, userid);			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		getSequence().context.setAuthenticatedUser(userid.getSingleString(this));

		String nodeValue = getSequence().context.getAuthenticatedUser();
		if (nodeValue != null && nodeValue.length() > 0) {
			Node text = doc.createTextNode(nodeValue);
			stepNode.appendChild(text);
		}
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(getSimpleTypeAffectation());
		
		return element;
	}
	
	public SmartType getUserId() {
		return userid;
	}
	
	public void setUserId(SmartType userId) {
		this.userid = userId;
	}

	private transient Set<SmartType> smartTypes = null;
	
	@Override
	public Set<SmartType> getSmartTypes() {
		if (smartTypes != null) {
			if  (!hasChanged)
				return smartTypes;
			else
				smartTypes.clear();
		}
		else {
			smartTypes = new HashSet<SmartType>();
		}
		smartTypes.add(userid);
		return smartTypes;
	}
}
