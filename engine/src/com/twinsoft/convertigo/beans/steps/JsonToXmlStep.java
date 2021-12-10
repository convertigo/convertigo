/*
 * Copyright (c) 2001-2021 Convertigo SA.
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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class JsonToXmlStep extends Step implements IStepSmartTypeContainer {

	private static final long serialVersionUID = 4426288799938289068L;
	
	private SmartType key = new SmartType();
	private SmartType jsonObject = new SmartType();
	private String jsonSample = "";
	transient private String jsonSource;

	public JsonToXmlStep() {
		super();
		setOutput(true);
		this.xml = true;
		key.setExpression("object");
	}

	@Override
	public JsonToXmlStep clone() throws CloneNotSupportedException {
		JsonToXmlStep clonedObject = (JsonToXmlStep) super.clone();
		clonedObject.key = key.clone();
		clonedObject.jsonObject = jsonObject.clone();
		return clonedObject;
	}

	@Override
	public JsonToXmlStep copy() throws CloneNotSupportedException {
		JsonToXmlStep copiedObject = (JsonToXmlStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String name;
		switch (key.getMode()) {
		case JS: name = key.getExpression(); break;
		case PLAIN: name = "\"" + key.getExpression() + "\""; break;
		default: name = "(" + getName() + ")" ; break;
		}
		return name + " : " + jsonObject.toString();
	}

	@Override
	public String getStepNodeName() {
		return getName();
	}
	
	public SmartType getKey() {
		return key;
	}

	public void setKey(SmartType key) {
		this.key = key;
	}

	public SmartType getJsonObject() {
		return jsonObject;
	}

	public void setJsonObject(SmartType jsonObject) {
		this.jsonObject = jsonObject;
	}

	public String getJsonSample() {
		return jsonSample;
	}

	public void setJsonSample(String jsonSample) {
		this.jsonSample = jsonSample;
	}

	@Override
	protected Node createStepNode() throws EngineException {
		Document doc = getOutputDocument();
		Element elt = doc.createElement("e");

		try {
			Object o;
			if (jsonSource.startsWith("{")) {
				o = new JSONObject(jsonSource);
			} else if (jsonSource.startsWith("[")) {
				o = new JSONArray(jsonSource);
			} else {
				o = RhinoUtils.jsonParse(jsonSource);
			}

			XMLUtils.jsonToXml(o, getStepNodeName(), elt, true, false, "item");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		elt = (Element) elt.getFirstChild();
		elt.setAttribute("originalKeyName", key.getSingleString(this));
		return elt;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			evaluate(javascriptContext, scope, key);
			evaluate(javascriptContext, scope, jsonObject);
			
			if (jsonObject.getMode() == Mode.JS) {
				jsonSource = RhinoUtils.jsonStringify(jsonObject.getEvaluated());
			} else {
				jsonSource = jsonObject.getSingleString(this).trim();
			}
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		try {
			String s = getJsonSample().trim();
			if (s.isEmpty() && jsonObject.getMode() == Mode.PLAIN) {
				s = jsonObject.getExpression().trim();
			}
			if (!s.startsWith("{") && !s.startsWith("[")) {
				return element;
			}
			Object json = s.startsWith("[") ? new JSONArray(s) : new JSONObject(s);
			Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element root = doc.createElement("root");
			doc.appendChild(root);
			XMLUtils.jsonToXml(json, root);
			XmlSchemaUtils.handleXsdElement(this, element, root, schema);
			
			XmlSchemaComplexType cType = (XmlSchemaComplexType) element.getSchemaType();
			XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
			attribute.setName("originalKeyName");
			attribute.setSchemaTypeName(Constants.XSD_STRING);
			if (key.getMode().equals(SmartType.Mode.PLAIN)) {
				attribute.setDefaultValue(key.getExpression());
			}
			attribute.setUse(XmlSchemaUtils.attributeUseOptional);
			cType.getAttributes().add(attribute);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
		smartTypes.add(jsonObject);
		return smartTypes;
	}

	@Override
	public String toJsString() {
		return null;
	}
}
