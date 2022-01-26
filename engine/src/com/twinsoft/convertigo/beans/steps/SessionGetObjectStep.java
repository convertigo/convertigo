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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.ParameterUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;


public class SessionGetObjectStep extends Step {

	private static final long serialVersionUID = 4907071994190554661L;

	private String key = "";
	
	public SessionGetObjectStep() {
		super();
		setOutput(true);		
		this.xml = true;
	}

	@Override
    public SessionGetObjectStep clone() throws CloneNotSupportedException {
    	SessionGetObjectStep clonedObject = (SessionGetObjectStep) super.clone();
        return clonedObject;
    }

	@Override
    public SessionGetObjectStep copy() throws CloneNotSupportedException {
    	SessionGetObjectStep copiedObject = (SessionGetObjectStep) super.copy();
        return copiedObject;
    }

	@Override
	public String getStepNodeName() {
		return key.isEmpty() ? "empty_key":StringUtils.normalize(key);
	}
	
	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		if (!key.isEmpty()) {
			Object value = getSequence().context.httpSession.getAttribute(key);
			if (value != null) {
				// Simple objects
				if ((value instanceof Boolean) || (value instanceof Integer) || (value instanceof Double)
						|| (value instanceof Float) || (value instanceof Character) || (value instanceof Long)
						|| (value instanceof Short) || (value instanceof Byte) || (value instanceof String)) {
					stepNode.setTextContent(ParameterUtils.toString(value));
				}
				// Complex objects
				else if (value instanceof NodeList) {
					NodeList nodeList = (NodeList)value;
					int len = nodeList == null ? 0 : nodeList.getLength();
					for (int i = 0; i < len; i++) {
						Node node = nodeList.item(i);
						boolean shouldImport = !node.getOwnerDocument().equals(doc);
						stepNode.appendChild(shouldImport ? doc.importNode(node, true):node.cloneNode(true));
					}
				}
				else if (value instanceof Node) {
					Node node = (Node)value;
					boolean shouldImport = !node.getOwnerDocument().equals(doc);
					stepNode.appendChild(shouldImport ? doc.importNode(node, true):node.cloneNode(true));
				}
				else {
					CDATASection cDATASection = doc.createCDATASection(ParameterUtils.toString(value));
					stepNode.appendChild(cDATASection);
				}
			}
		}
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		element.setName(getStepNodeName());
		
		if (!getXmlElementRefAffectation().isEmpty()) {
			XmlSchemaComplexType eType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
			element.setType(eType);
			XmlSchemaSequence eSequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
			eType.setParticle(eSequence);
			
			SchemaMeta.setContainerXmlSchemaGroupBase(element, eSequence);
			
			XmlSchemaElement el = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
			eSequence.getItems().add(el);
			el.setRefName(getElementRefAffectation());
			el.setMinOccurs(0);
			el.setMaxOccurs(1);
		}
		else {
			XmlQName xmlQName = getXmlComplexTypeAffectation();
			if (xmlQName.isEmpty()) {
				xmlQName = getXmlSimpleTypeAffectation();
				if (xmlQName.isEmpty()) {
					xmlQName = new XmlQName(Constants.XSD_STRING);
				}
			}
			element.setSchemaTypeName(xmlQName.getQName());			
		}
		
		return element;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toJsString() {
		String string = ParameterUtils.toString(getSequence().context.httpSession.getAttribute(key));
		if (string != null && string.length() > 0) {
			return string;
		}
		return "";
	}
}
