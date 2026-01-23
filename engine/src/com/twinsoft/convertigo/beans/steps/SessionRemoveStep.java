/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import javax.xml.namespace.QName;

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

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SessionRemoveStep extends Step {

	private static final long serialVersionUID = -5313027203893865112L;

	private String key = "";
	
	public SessionRemoveStep() {
		super();
		setOutput(false);
		this.xml = true;
	}
	
	@Override
    public SessionRemoveStep clone() throws CloneNotSupportedException {
    	SessionRemoveStep clonedObject = (SessionRemoveStep) super.clone();
        return clonedObject;
    }

	@Override
    public SessionRemoveStep copy() throws CloneNotSupportedException {
    	SessionRemoveStep copiedObject = (SessionRemoveStep) super.copy();
        return copiedObject;
    }
	
	@Override
	public String getStepNodeName() {
		return "session";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (getSequence().context != null) {
				return super.stepExecute(javascriptContext, scope);
			}
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {		
		if (key != null) {
			stepNode.setAttribute("key", key.isEmpty() ? "empty_key":StringUtils.normalize(key));			
			if (!key.isEmpty()) {
				getSequence().context.httpSession.removeAttribute(key);
			}
		}
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		element.setName(getStepNodeName());
		
		QName qname = new QName(schema.getTargetNamespace(), "SessionRemoveObjectType");
		if (schema.getTypeByName(qname) == null) {
			XmlSchemaComplexType eType = new XmlSchemaComplexType(schema);
			eType.setName("SessionRemoveObjectType");
			
			XmlSchemaSimpleContent sContent = new XmlSchemaSimpleContent();
			eType.setContentModel(sContent);
	
			XmlSchemaSimpleContentExtension sContentExt = new XmlSchemaSimpleContentExtension();
			sContentExt.setBaseTypeName(Constants.XSD_STRING);
			sContent.setContent(sContentExt);
			
			XmlSchemaAttribute attribute = new XmlSchemaAttribute();
			attribute.setName("key");
			attribute.setSchemaTypeName(Constants.XSD_STRING);
			sContentExt.getAttributes().add(attribute);
			
			schema.addType(eType);
			schema.getItems().add(eType);
		}
		
		element.setSchemaTypeName(qname);
		
		return element;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toJsString() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(key)) {
			key = StringUtils.normalize(newName);
			hasChanged = true;
		}
	}
}
