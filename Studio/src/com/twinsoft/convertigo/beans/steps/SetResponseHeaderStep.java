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
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SetResponseHeaderStep extends Step implements IStepSmartTypeContainer, IComplexTypeAffectation {

	private static final long serialVersionUID = -8059806670476199008L;
	
	public SetResponseHeaderStep() {
		super();
		setOutput(false);
		this.xml = true;
	}

	@Override
	public SetResponseHeaderStep clone() throws CloneNotSupportedException {
		SetResponseHeaderStep clonedObject = (SetResponseHeaderStep) super.clone();
		clonedObject.smartTypes = null;
		return clonedObject;
	}
	
	private SmartType headerName = new SmartType();
	
	public SmartType getHeaderName() {
		return headerName;
	}

	public void setHeaderName(SmartType headerName) {
		this.headerName = headerName;
	}

	private SmartType headerValue = new SmartType();
	
	public SmartType getHeaderValue() {
		return headerValue;
	}

	public void setHeaderValue(SmartType headerValue) {
		this.headerValue = headerValue;
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
		smartTypes.add(headerName);
		smartTypes.add(headerValue);
		return smartTypes;
	}
	
	@Override
	public String toJsString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			evaluate(javascriptContext, scope, headerName);
			evaluate(javascriptContext, scope, headerValue);
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	public String getStepNodeName() {
		return "header";
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String hName = (hName = headerName.getSingleString(this)) == null ? "" : hName;
		if (hName != null && hName.length() > 0) {
			String hValue = (hValue = headerValue.getSingleString(this)) == null ? "" : hValue;
			
			getSequence().context.setResponseHeader(hName, hValue);
			
			Element en = (Element) stepNode.appendChild(doc.createElement("name"));
			en.setTextContent(hName);
			
			Element ev = (Element) stepNode.appendChild(doc.createElement("value"));
			ev.setTextContent(hValue);
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
		elt.setName("name");
		elt.setMinOccurs(1);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("value");
		elt.setMinOccurs(1);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		return element;
	}
	
}
