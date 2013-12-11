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

public class SetContextStep extends StepWithExpressions implements IComplexTypeAffectation {

	private static final long serialVersionUID = -1894558458026853410L;

	private String key = "";
	private SmartType expression = new SmartType();
	
	private String value;
	
	public SetContextStep() {
		super();
		setOutput(false);
		this.xml = true;
	}
	
	@Override
    public SetContextStep clone() throws CloneNotSupportedException {
    	SetContextStep clonedObject = (SetContextStep) super.clone();
        return clonedObject;
    }

	@Override
    public SetContextStep copy() throws CloneNotSupportedException {
    	SetContextStep copiedObject = (SetContextStep) super.copy();
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
		return "context";
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (getSequence().context != null) {
				getSequence().context.set(key, expression.getExpression());
				value = (String) getSequence().context.get(key);			
				return super.stepExecute(javascriptContext, scope);
			}
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String string = key;
		if (string != null && string.length() > 0) {
			stepNode.setAttribute("key", string);
		}
		
		string = value;
		if (string != null && string.length() > 0) {
			stepNode.setAttribute("expression", string);
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
}
