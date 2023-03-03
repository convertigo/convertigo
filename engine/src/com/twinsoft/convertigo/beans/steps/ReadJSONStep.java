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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class ReadJSONStep extends ReadFileStep implements IStepSmartTypeContainer {

	private static final long serialVersionUID = 9145615088577678134L;
	
	private SmartType key = new SmartType();
	private String jsonSample = "";

	public ReadJSONStep() {
		super();
	}

	@Override
	public ReadJSONStep clone() throws CloneNotSupportedException {
		ReadJSONStep clonedObject = (ReadJSONStep) super.clone();
		clonedObject.key = key.clone();
		return clonedObject;
	}

	@Override
	public ReadJSONStep copy() throws CloneNotSupportedException {
		ReadJSONStep copiedObject = (ReadJSONStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {
		}

		return "ReadJSON: " + label;
	}

	public SmartType getKey() {
		return key;
	}

	public void setKey(SmartType key) {
		this.key = key;
	}

	public String getJsonSample() {
		return jsonSample;
	}

	public void setJsonSample(String jsonSample) {
		this.jsonSample = jsonSample;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			evaluate(javascriptContext, scope, key);
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}
	
	@Override
	public String getStepNodeName() {
		if (replaceStepElement) {
			return getFileNodeName();
		} else {
			return super.getStepNodeName();
		}
	}
	
	protected String getFileNodeName() {
		String name = key.getMode().equals(SmartType.Mode.PLAIN) ? key.getExpression() : getName();
		name = name.isBlank() ? getName() : StringUtils.normalize(name);
		return name;
	}
	
	protected Document read(String filePath, boolean schema) {
		Document xmlDoc = null;

		try {
			File jsonFile = new File(getAbsoluteFilePath(filePath));
			if (!jsonFile.exists()) {
				Engine.logBeans.warn("(ReadJSON) JSON File '" + filePath + "' does not exist.");

				xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				xmlDoc.appendChild(xmlDoc.createElement("readjson_error"));
				Element myEl = xmlDoc.createElement("message");
				myEl.appendChild(xmlDoc.createTextNode("File '" + filePath + "' not found." ));
				xmlDoc.getDocumentElement().appendChild(myEl);
			} else {
				String jsonSource = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
				jsonSource = FileUtils.removeBOM(jsonSource);
				jsonSource = jsonSource.trim();
				xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				Element elt = xmlDoc.createElement("root");

				try {
					Object o;
					if (jsonSource.startsWith("{")) {
						o = new JSONObject(jsonSource);
					} else if (jsonSource.startsWith("[")) {
						o = new JSONArray(jsonSource);
					} else {
						o = RhinoUtils.jsonParse(jsonSource);
					}
					
					XMLUtils.jsonToXml(o, getFileNodeName(), elt, true, false, "item");
					elt = (Element) elt.getFirstChild();
					elt.setAttribute("originalKeyName", key.getSingleString(this));
					xmlDoc.appendChild(elt);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				
				if (Engine.logBeans.isDebugEnabled()) {
					String xmlContent = XMLUtils.prettyPrintDOM(xmlDoc);
					if (Engine.logBeans.isTraceEnabled()) {
						Engine.logBeans.trace("(ReadXML) JSON File converted [" + xmlContent.length() + "] content '" + xmlContent + "'");
					} else {
						Engine.logBeans.debug("(ReadXML) JSON File converted [" + xmlContent.length() + ", show max 10000] content '" + xmlContent.substring(0, Math.min(xmlContent.length(), 10000)) + "'");
					}
				}
			}
		} catch (Exception e1) {
			Engine.logBeans.warn("(ReadJSON) Error while trying to parse JSON file : " + e1.toString());
			try {
				xmlDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
				xmlDoc.appendChild(xmlDoc.createElement("document"));
				Element myEl = xmlDoc.createElement("message");
				myEl.appendChild(xmlDoc.createTextNode("Unable to parse file '" + filePath + "'." ));
				xmlDoc.getDocumentElement().appendChild(myEl);
			}
			catch (Exception e2) {
				Engine.logBeans.warn("(ReadXML) An error occured while building error xml document: " + e1.toString());
			}
		}

		return xmlDoc;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		XmlSchemaElement base = element;
		if (!replaceStepElement) {
			base = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
			XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
			base.setType(cType);
			XmlSchemaSequence seq = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
			cType.setParticle(seq);
			seq.getItems().add(element);
		}

		element.setName(getFileNodeName());
		try {
			String s = getJsonSample().trim();
			if (!s.startsWith("{") && !s.startsWith("[")) {
				return base;
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

		return base;
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
		return "file.json";
	}
}
