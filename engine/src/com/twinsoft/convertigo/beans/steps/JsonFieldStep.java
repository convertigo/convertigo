/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.JsonFieldType;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class JsonFieldStep extends Step implements IStepSmartTypeContainer {
	private static final long serialVersionUID = -700241502764617513L;
	
	private SmartType key = new SmartType();
	private SmartType value = new SmartType();
	private JsonFieldType type = JsonFieldType.string;
	
	public JsonFieldStep() {
		super();
		setOutput(true);
		xml = true;
		key.setExpression("field");
	}

	public JsonFieldStep clone() throws CloneNotSupportedException {
		JsonFieldStep clonedObject = (JsonFieldStep) super.clone();
		clonedObject.key = key.clone();
		clonedObject.value = value.clone();
		return clonedObject;
	}

	@Override
	public JsonFieldStep copy() throws CloneNotSupportedException {
		JsonFieldStep copiedObject = (JsonFieldStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String name, val;
		switch (key.getMode()) {
		case JS: name = key.getExpression(); break;
		case PLAIN: name = "\"" + key.getExpression() + "\""; break;
		default: name = "(" + getName() + ")" ; break;
		}
		switch (value.getMode()) {
		case JS: val = value.getExpression(); break;
		case PLAIN:
			val = type.equals(JsonFieldType.string) ?
					("\"" + value.getExpression() + "\"") :
					value.getExpression();
			break;
		default: val = "?" ; break;
		}
		return name + " : " + val;
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		try {
			String sKey = (sKey = key.getSingleString(this)) == null ? "" : sKey;
			String sValue = (sValue = value.getSingleString(this)) == null ? "" : sValue;
			stepNode.setAttribute("type", type.toString());
			stepNode.setAttribute("originalKeyName", sKey);
			stepNode.setTextContent(sValue);
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
	
	public SmartType getValue() {
		return value;
	}

	public void setValue(SmartType value) {
		this.value = value;
	}
	
	public JsonFieldType getType() {
		return type;
	}

	public void setType(JsonFieldType type) {
		this.type = type;
	}

	@Override
	public String getStepNodeName() {
		return getName();
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			evaluate(javascriptContext, scope, key);
			evaluate(javascriptContext, scope, value);
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);
		
		XmlSchemaSimpleContent simpleContent = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContent());
		cType.setContentModel(simpleContent);
		
		XmlSchemaSimpleContentExtension simpleContentExtension = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSimpleContentExtension());
		simpleContent.setContent(simpleContentExtension);
		
		simpleContentExtension.setBaseTypeName(getSimpleTypeAffectation());
		
		XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attribute.setName("type");
		attribute.setSchemaTypeName(Constants.XSD_STRING);
		attribute.setDefaultValue(type.toString());
		simpleContentExtension.getAttributes().add(attribute);
		
		attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attribute.setName("originalKeyName");
		attribute.setSchemaTypeName(Constants.XSD_STRING);
		if (key.getMode().equals(SmartType.Mode.PLAIN)) {
			attribute.setDefaultValue(key.getExpression());
		}
		simpleContentExtension.getAttributes().add(attribute);
		
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
	public String toJsString() {
		return null;
	}	
}