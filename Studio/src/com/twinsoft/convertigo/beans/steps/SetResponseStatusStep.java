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

public class SetResponseStatusStep extends Step implements IStepSmartTypeContainer, IComplexTypeAffectation {

	private static final long serialVersionUID = 6969133147091617758L;

	public SetResponseStatusStep() {
		super();
		setOutput(false);
		this.xml = true;
	}

	@Override
	public SetResponseStatusStep clone() throws CloneNotSupportedException {
		SetResponseStatusStep clonedObject = (SetResponseStatusStep) super.clone();
		clonedObject.smartTypes = null;
		return clonedObject;
	}
	
	private SmartType statusCode = new SmartType();
	
	public SmartType getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(SmartType statusCode) {
		this.statusCode = statusCode;
	}

	private SmartType statusText = new SmartType();
	
	public SmartType getStatusText() {
		return statusText;
	}

	public void setStatusText(SmartType statusText) {
		this.statusText = statusText;
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
		smartTypes.add(statusCode);
		smartTypes.add(statusText);
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
			evaluate(javascriptContext, scope, statusCode);
			evaluate(javascriptContext, scope, statusText);
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	public String getStepNodeName() {
		return "status";
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String sCode = (sCode = statusCode.getSingleString(this)) == null ? "" : sCode;
		if (sCode != null && sCode.length() > 0) {
			String sText = (sText = statusText.getSingleString(this)) == null ? "" : sText;
			
			Integer iCode = -1;
			try {
				iCode = Integer.parseInt(sCode, 10);
			}
			catch (Exception e) {
				throw new EngineException("Unable to set status code", e);
			}
			
			getSequence().context.setResponseStatus(iCode, sText);
			
			Element en = (Element) stepNode.appendChild(doc.createElement("code"));
			en.setTextContent(sCode);
			
			Element ev = (Element) stepNode.appendChild(doc.createElement("text"));
			ev.setTextContent(sText);
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
		elt.setName("code");
		elt.setMinOccurs(1);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("text");
		elt.setMinOccurs(1);
		elt.setMaxOccurs(1);
		elt.setSchemaTypeName(Constants.XSD_STRING);
		
		return element;
	}
	
}
