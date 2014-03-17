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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;

public class GetAuthenticatedUserStep extends StepWithExpressions implements IComplexTypeAffectation {
	
	private static final long serialVersionUID = 1430960819073513105L;
	
	private String nodeName = "authenticatedUserID";
	
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
	
	protected boolean workOnSource() {
		return false;
	}

	protected StepSource getSource() {
		return null;
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
}
