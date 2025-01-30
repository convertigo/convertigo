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
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class JsonObjectStep extends StepWithExpressions implements IStepSmartTypeContainer {

	private static final long serialVersionUID = 7002348492812220725L;

	private SmartType key = new SmartType();
	
	public JsonObjectStep() {
		super();
		setOutput(true);
		xml = true;
		key.setExpression(getName());
	}

	public JsonObjectStep clone() throws CloneNotSupportedException {
		JsonObjectStep clonedObject = (JsonObjectStep) super.clone();
		clonedObject.key = key.clone();
		return clonedObject;
	}

	@Override
	public JsonObjectStep copy() throws CloneNotSupportedException {
		JsonObjectStep copiedObject = (JsonObjectStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String name;
		switch (key.getMode()) {
		case JS: name = key.getExpression(); break;
		case PLAIN: name = "\"" + key.getExpression() + "\""; break;
		case SOURCE: name = key.toString(this); break;
		default: name = "(" + getName() + ")" ; break;
		}
		return name + " : { ... }";
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		try {
			String sKey = (sKey = key.getSingleString(this)) == null ? "" : sKey;
			stepNode.setAttribute("type", "object");
			stepNode.setAttribute("originalKeyName", sKey);
		} catch (Exception e) {
			setErrorStatus(true);
			Engine.logBeans.error("An error occured while generating values from JsonObject", e);
		}
	}
	
	public SmartType getKey() {
		return key;
	}

	public void setKey(SmartType key) {
		this.key = key;
	}

	@Override
	public String getStepNodeName() {
		return getName();
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			evaluate(javascriptContext, scope, "key", key);
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}
	
	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (inError()) {
				Engine.logBeans.info("Skipping step "+ this +" ("+ hashCode()+") because its source is in error");
				return true;
			}
			return super.executeNextStep(javascriptContext, scope);
		}
		return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);
		
		XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attribute.setName("type");
		attribute.setSchemaTypeName(Constants.XSD_STRING);
		attribute.setDefaultValue("object");
		cType.getAttributes().add(attribute);
		
		attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attribute.setName("originalKeyName");
		attribute.setSchemaTypeName(Constants.XSD_STRING);
		if (key.getMode().equals(SmartType.Mode.PLAIN)) {
			attribute.setDefaultValue(key.getExpression());
		}
		cType.getAttributes().add(attribute);
		
		return element;
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
		smartTypes.add(key);
		return smartTypes;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (key != null && key.getMode() == Mode.PLAIN
				&& oldName.startsWith(StringUtils.normalize(key.getExpression()))) {
			key.setExpression(newName);
			hasChanged = true;
		}
	}
	
	@Override
	protected String defaultBeanName(String displayName) {
		return "object";
	}
}
