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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.ISchemaAttributeGenerator;
import com.twinsoft.convertigo.beans.core.ISimpleTypeAffectation;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class AttributeStep extends Step implements ISchemaAttributeGenerator, ISimpleTypeAffectation {

	private static final long serialVersionUID = 4426876799938289068L;

	private String expression = "";
	private String nodeName = "attribute";
	private String nodeText = "";
	
	private String nodeNameSpace = "";
	private String nodeNameSpaceURI = "";
	
	public AttributeStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	public AttributeStep(String expression) {
		super();
		this.expression = expression;
	}

	@Override
    public AttributeStep clone() throws CloneNotSupportedException {
    	AttributeStep clonedObject = (AttributeStep) super.clone();
        return clonedObject;
    }

	@Override
    public AttributeStep copy() throws CloneNotSupportedException {
    	AttributeStep copiedObject = (AttributeStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toJsString() {
		return expression;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		return "@"+ nodeName + (!text.equals("") ? " // "+text:"");
	}

	@Override
	public String getStepNodeName() {
		return getNodeName();
	}
	
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getNodeText() {
		return nodeText;
	}

	public void setNodeText(String nodeText) {
		this.nodeText = nodeText;
	}
	
	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	public String getNodeNameSpace() {
		return nodeNameSpace;
	}

	public void setNodeNameSpace(String nodeNameSpace) {
		this.nodeNameSpace = nodeNameSpace;
	}

	public String getNodeNameSpaceURI() {
		return nodeNameSpaceURI;
	}

	public void setNodeNameSpaceURI(String nodeNameSpaceURI) {
		this.nodeNameSpaceURI = nodeNameSpaceURI;
	}

	@Override
	protected Node createStepNode() throws EngineException {
		Attr stepNode = null;
		String nodeValue = nodeText;
		if (evaluated != null) {
			if (evaluated instanceof NodeList) {
				NodeList list = (NodeList)evaluated;
				nodeValue = list.toString();
			}
			else if (evaluated instanceof Collection) {
				List<String> list = GenericUtils.toString((Collection<?>)evaluated);
				nodeValue = list.toString();
			}
			else if (evaluated instanceof NativeJavaArray) {
				Object object = ((NativeJavaArray)evaluated).unwrap();
				List<String> list = GenericUtils.toString(Arrays.asList((Object[])object));
				nodeValue = list.toString();
			}
			else if (evaluated.getClass().isArray()) {
				nodeValue = Arrays.toString((Object[])evaluated);
			}
			else
				nodeValue = evaluated.toString();
		}
		
		Document doc = getOutputDocument();
		if (!inError() && isOutput()) {
			String namespace = getNodeNameSpace();
			if (namespace.equals("")) {
				stepNode = doc.createAttribute(getStepNodeName());
				stepNode.setNodeValue(nodeValue);
			}
			else {
				String namespaceURI = getNodeNameSpaceURI();
				if (namespaceURI.equals(""))
					throw new EngineException("Blank namespace URI is not allowed (using namespace '"
							+ namespace + "' in jAttribute step '" + getName() + "')");

				stepNode = doc.createAttributeNS(namespaceURI, namespace + ":" + getStepNodeName());
				stepNode.setNodeValue(nodeValue);
			}
		}
		return stepNode;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			try {
				evaluate(javascriptContext, scope, getExpression(), "expression", true);
				if (evaluated instanceof org.mozilla.javascript.Undefined) {
					throw new Exception("Step "+ getName() +" has none expression defined." );
				}
					
			}
			catch (Exception e) {
				evaluated = null;
				Engine.logBeans.warn(e.getMessage());
			}
			
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}
	
	@Override
	public XmlSchemaAttribute getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		String namespace = getNodeNameSpace();
		String namespaceURI = getNodeNameSpaceURI();
		boolean hasQName = !namespace.equals("") && !namespaceURI.equals("");
		
		XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attribute.setName(getStepNodeName());
		attribute.setSchemaTypeName(getSimpleTypeAffectation());
		if (hasQName) {
			attribute.setQName(new QName(namespaceURI,getStepNodeName(),namespace));
		}
		else {
			attribute.setUse(XmlSchemaUtils.attributeUseRequired);
		}
		addXmlSchemaAnnotation(attribute);
		return attribute;
	}
}
