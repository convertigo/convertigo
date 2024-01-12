/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class GetAuthenticatedUserStep extends Step implements IComplexTypeAffectation {
	
	private static final long serialVersionUID = 1430960819073513105L;
	
	private String nodeName = getName();
	
	public GetAuthenticatedUserStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
    public GetAuthenticatedUserStep clone() throws CloneNotSupportedException {
    	GetAuthenticatedUserStep clonedObject = (GetAuthenticatedUserStep) super.clone();
        return clonedObject;
    }

	@Override
    public GetAuthenticatedUserStep copy() throws CloneNotSupportedException {
    	GetAuthenticatedUserStep copiedObject = (GetAuthenticatedUserStep) super.copy();
        return copiedObject;
    }

	@Override
	public String getStepNodeName() {
		return getNodeName();
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
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(nodeName)) {
			nodeName = StringUtils.normalize(newName);
			hasChanged = true;
		}
	}
	
	@Override
	protected String defaultBeanName(String displayName) {
		return "authenticatedUserID";
	}
}
