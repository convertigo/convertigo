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
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Sequence.OutputFilter;
import com.twinsoft.convertigo.beans.core.Sequence.OutputOption;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ParameterUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SessionSetObjectStep extends Step implements IStepSmartTypeContainer {

	private static final long serialVersionUID = -1894558458026853410L;

	private String key = "";
	private SmartType value = new SmartType();
	
	public SessionSetObjectStep() {
		super();
		setOutput(false);
		this.xml = true;
	}
	
	@Override
	public SessionSetObjectStep clone() throws CloneNotSupportedException {
		SessionSetObjectStep clonedObject = (SessionSetObjectStep) super.clone();
		clonedObject.smartTypes = null;
		clonedObject.value = value;
		return clonedObject;
	}

	@Override
	public SessionSetObjectStep copy() throws CloneNotSupportedException {
		SessionSetObjectStep copiedObject = (SessionSetObjectStep) super.copy();
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
				evaluate(javascriptContext, scope, value);
				return super.stepExecute(javascriptContext, scope);
			}
		}
		return false;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {		
		stepNode.setAttribute("key", key.isEmpty() ? "empty_key":StringUtils.normalize(key));
		if (!key.isEmpty()) {
			Object object = value.getObject(this);
			if (object != null) {
				if ((object instanceof NodeList) && value.isUseSource()) {
					OutputFilter outputFilter = sequence.new OutputFilter(OutputOption.UsefullOnly);
					object = Sequence.ouputDomView((NodeList) object,outputFilter);
				}
				getSequence().context.httpSession.setAttribute(key, object);
				
				// Simple objects
				if ((object instanceof Boolean) || (object instanceof Integer) || (object instanceof Double)
						|| (object instanceof Float) || (object instanceof Character) || (object instanceof Long)
						|| (object instanceof Short) || (object instanceof Byte) || (object instanceof String)) {
					stepNode.setTextContent(ParameterUtils.toString(object));
				}
				// Complex objects
				else {
					CDATASection cDATASection = doc.createCDATASection(ParameterUtils.toString(object));
					stepNode.appendChild(cDATASection);
				}
			}
		}
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		element.setName(getStepNodeName());
		
		QName qname = new QName(schema.getTargetNamespace(), "SessionSetObjectType");
		if (schema.getTypeByName(qname) == null) {
			XmlSchemaComplexType eType = new XmlSchemaComplexType(schema);
			eType.setName("SessionSetObjectType");
			
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

	public SmartType getValue() {
		return value;
	}

	public void setValue(SmartType value) {
		this.value = value;
	}

	@Override
	public String toJsString() {
		try {
			String string = ParameterUtils.toString(value.getObject(this));
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
		smartTypes.add(value);
		return smartTypes;
	}
}
