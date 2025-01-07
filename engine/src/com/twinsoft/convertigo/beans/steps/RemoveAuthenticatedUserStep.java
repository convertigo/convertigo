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
import com.twinsoft.convertigo.engine.EngineException;

public class RemoveAuthenticatedUserStep extends Step implements IComplexTypeAffectation {
	
	private static final long serialVersionUID = 1430960819073513105L;
	
	public RemoveAuthenticatedUserStep() {
		super();
		setOutput(false);
		this.xml = true;
	}

	@Override
    public RemoveAuthenticatedUserStep clone() throws CloneNotSupportedException {
    	RemoveAuthenticatedUserStep clonedObject = (RemoveAuthenticatedUserStep) super.clone();
        return clonedObject;
    }

	@Override
    public RemoveAuthenticatedUserStep copy() throws CloneNotSupportedException {
    	RemoveAuthenticatedUserStep copiedObject = (RemoveAuthenticatedUserStep) super.copy();
        return copiedObject;
    }

	@Override
	public String getStepNodeName() {
		return "authenticatedUserID";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			
			getSequence().context.removeAuthenticatedUser();
			
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

	@Override
	public String toJsString() {
		String nodeValue = getSequence().context.getAuthenticatedUser();
		if (nodeValue != null && nodeValue.length() > 0) {
			return nodeValue;
		}
		return "";
	}
}
