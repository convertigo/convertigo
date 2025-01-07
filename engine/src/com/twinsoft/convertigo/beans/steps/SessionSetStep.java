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

import java.util.HashSet;
import java.util.Set;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SessionSetStep extends Step implements IStepSmartTypeContainer, IComplexTypeAffectation {

	private static final long serialVersionUID = -1894558458026853410L;

	private String key = "";
	private SmartType expression = new SmartType();
	
	public SessionSetStep() {
		super();
		setOutput(false);
		this.xml = true;
	}
	
	@Override
	public SessionSetStep clone() throws CloneNotSupportedException {
		SessionSetStep clonedObject = (SessionSetStep) super.clone();
		clonedObject.smartTypes = null;
		clonedObject.expression = expression.clone();
		return clonedObject;
	}

	@Override
	public SessionSetStep copy() throws CloneNotSupportedException {
		SessionSetStep copiedObject = (SessionSetStep) super.copy();
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
				evaluate(javascriptContext, scope, "expression", expression);
				return super.stepExecute(javascriptContext, scope);
			}
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {		
		String string = key;
		if (string != null && string.length() > 0) {
			Element keyElement = doc.createElement("key");
			keyElement.setTextContent(string);
			stepNode.appendChild(keyElement);
		}

		string = expression.getSingleString(this);
		getSequence().context.httpSession.setAttribute(key, string);	
		if (string != null && string.length() > 0) {
			Element expressionElement = doc.createElement("expression");
			expressionElement.setTextContent(string);
			stepNode.appendChild(expressionElement);
		}
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);

		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		SchemaMeta.setContainerXmlSchemaGroupBase(element, sequence);
		
		XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("key");
		elt.setMinOccurs(0);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("expression");
		elt.setMinOccurs(0);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		return element;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public SmartType getExpression() {
		return expression;
	}

	public void setExpression(SmartType expression) {
		this.expression = expression;
	}

	@Override
	public String toJsString() {
		try {
			String string = expression.getSingleString(this);
			if (string != null && string.length() > 0) {
				return string;
			}
			return "";
		} catch(EngineException e) {
			return "";
		}
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
		smartTypes.add(expression);
		return smartTypes;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(key)) {
			key = StringUtils.normalize(newName);
			hasChanged = true;
		}
	}
}
