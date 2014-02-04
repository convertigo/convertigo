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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/SetAuthenticatedUserStep.java $
 * $Author: julienda $
 * $Revision: 34210 $
 * $Date: 2013-12-09 16:37:15 +0200 (lun., 09 dec 2013) $
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
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;

public class SetAuthenticatedUserStep extends StepWithExpressions implements IComplexTypeAffectation {

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
        return clonedObject;
    }

	@Override
    public SetAuthenticatedUserStep copy() throws CloneNotSupportedException {
    	SetAuthenticatedUserStep copiedObject = (SetAuthenticatedUserStep) super.copy();
        return copiedObject;
    }
	
	protected boolean workOnSource() {
		return false;
	}

	protected StepSource getSource() {
		return null;
	}

	@Override
	public String getStepNodeName() {
		return "authenticatedUserID";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			evaluate(javascriptContext, scope, userid);
			
			getSequence().context.setAuthenticatedUser(userid.getSingleString(this));
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
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
}
