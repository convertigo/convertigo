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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.IComplexTypeAffectation;
import com.twinsoft.convertigo.beans.core.IElementRefAffectation;
import com.twinsoft.convertigo.beans.core.ISimpleTypeAffectation;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ElementStep extends StepWithExpressions implements IComplexTypeAffectation, ISimpleTypeAffectation, IElementRefAffectation {

	private static final long serialVersionUID = 3276050659362959159L;
	
	private String expression = "";
	private String nodeName = getName();
	private String nodeText = "";
	
	public ElementStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	public ElementStep(String expression) {
		super();
		this.expression = expression;
	}

	@Override
    public ElementStep clone() throws CloneNotSupportedException {
    	ElementStep clonedObject = (ElementStep) super.clone();
        return clonedObject;
    }

	@Override
    public ElementStep copy() throws CloneNotSupportedException {
    	ElementStep copiedObject = (ElementStep) super.copy();
        return copiedObject;
    }

	@Override
	protected String getSpecificLabel() throws EngineException {
		if (!expression.equals(""))
			return "="+ ((expression.length()<10) ? expression : expression.substring(0, 10)+ "...");
		else
			return "=\""+ nodeText + "\"";
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label += " " + getLabel();
		} catch (EngineException e) {}
		XmlQName xmlQName = getXmlElementRefAffectation();
		xmlQName = xmlQName.isEmpty() ? getXmlComplexTypeAffectation() : xmlQName;
		return "<" + getStepNodeName() + ">" + label + " " + xmlQName.getQName();
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

	@Override
	public String getStepNodeName() {
		if (!getXmlElementRefAffectation().isEmpty())
			return getXmlElementRefAffectation().getQName().getLocalPart();		
		return getNodeName();
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String nodeValue = nodeText;
		if (evaluated != null) {
			nodeValue = "";
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
			else if (evaluated instanceof NativeArray) {
				nodeValue = (String)((NativeArray)evaluated).getDefaultValue(String.class);
			}
			else if (evaluated instanceof NativeJavaObject) {
				nodeValue = (String)((NativeJavaObject)evaluated).getDefaultValue(String.class);
			}
			else if (evaluated.getClass().isArray()) {
				nodeValue = Arrays.toString((Object[])evaluated);
			}
			else
				nodeValue = evaluated.toString();
		}
		Node text = doc.createTextNode(nodeValue);
		stepNode.appendChild(text);
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
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
	public String toJsString() {
		return expression;
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		element.setSchemaTypeName(getSimpleTypeAffectation());
		return element;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(nodeName)) {
			nodeName = StringUtils.normalize(newName);
			hasChanged = true;
		}
	}
	
	@Override
	protected String defaultBeanName(String displayName) {
		return "element";
	}
}
