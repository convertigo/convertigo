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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.steps;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
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
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class XMLErrorStep extends StepWithExpressions implements IComplexTypeAffectation {

	private static final long serialVersionUID = 7008868210812220725L;
	
	private SmartType code = new SmartType();
	private SmartType message = new SmartType();
	private SmartType details = new SmartType();
	
	public XMLErrorStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	@Override
    public XMLErrorStep clone() throws CloneNotSupportedException {
    	XMLErrorStep clonedObject = (XMLErrorStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLErrorStep copy() throws CloneNotSupportedException {
    	XMLErrorStep copiedObject = (XMLErrorStep) super.copy();
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
		return "error";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			evaluate(javascriptContext, scope, code);
			evaluate(javascriptContext, scope, message);
			evaluate(javascriptContext, scope, details);
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		stepNode.setAttribute("type", "user");
		
		String string = code.getSingleString(this);
		if (string != null && string.length() > 0) {
			Element elt = doc.createElement("code");
			elt.setTextContent(string);
			stepNode.appendChild(elt);
		}
		
		string = message.getSingleString(this);
		if (string != null && string.length() > 0) {
			Element elt = doc.createElement("message");
			elt.setTextContent(string);
			stepNode.appendChild(elt);
		}
		
		string = details.getSingleString(this);
		if (string != null && string.length() > 0) {
			Element elt = doc.createElement("details");
			elt.setTextContent(string);
			stepNode.appendChild(elt);
		}
		
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
//		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence()); 
//		sequence.setMinOccurs(0);
//		XmlSchemaParticle particle = getXmlSchemaParticle(collection, schema, sequence);
//		
//		if (particle instanceof XmlSchemaElement)
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);

		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		SchemaMeta.setContainerXmlSchemaGroupBase(element, sequence);
		
		XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("code");
		elt.setMinOccurs(0);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("message");
		elt.setMinOccurs(0);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("details");
		elt.setMinOccurs(0);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		XmlSchemaAttribute attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("type");
		attr.setSchemaTypeName(Constants.XSD_STRING);
		cType.getAttributes().add(attr);
		
		return element;
	}
	
	public SmartType getCode() {
		return code;
	}

	public void setCode(SmartType code) {
		this.code = code;
	}
	
	public SmartType getMessage() {
		return message;
	}
	
	public void setMessage(SmartType message) {
		this.message = message;
	}

	public SmartType getDetails() {
		return details;
	}

	public void setDetails(SmartType details) {
		this.details = details;
	}
}
