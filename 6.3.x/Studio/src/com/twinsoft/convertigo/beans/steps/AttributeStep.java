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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class AttributeStep extends Step {

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
	protected StepSource getSource() {
		return null;
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
	protected boolean workOnSource() {
		return false;
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
	protected Node createWsdlDom() throws EngineException {
		wsdlDom = getSequence().createDOM();
		wsdlDom.getDocumentElement().setAttribute(nodeName, "");
		Node attr = wsdlDom.getDocumentElement().getAttributeNode(nodeName);
		wsdlDomDirty = false;
		return attr;
	}

	@Override
	protected Node generateWsdlDom() throws EngineException {
		Attr attr = null;
    	try {
    		if (wsdlDomDirty || (wsdlDom == null)) {
    			attr = (Attr)createWsdlDom();
    		}
    		else
    			attr = (Attr)wsdlDom.getDocumentElement().getAttributeNode(getStepNodeName());
    		return attr;
    	}
    	catch (Exception e) {
    		wsdlDom = null;
    		throw new EngineException("Unable to generate WSDL document",e);
    	}
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
		if (!inError()) {
			String namespace = getNodeNameSpace();
			if (namespace.equals("")) {
				doc.getDocumentElement().setAttribute(getStepNodeName(), nodeValue);
				stepNode = doc.getDocumentElement().getAttributeNode(getStepNodeName());
				((Step)parent).appendChildNode(stepNode);
			}
			else {
				String namespaceURI = getNodeNameSpaceURI();
				if (namespaceURI.equals(""))
					throw new EngineException("Blank namespace URI is not allowed (using namespace '"
							+ namespace + "' in jAttribute step '" + getName() + "')");

				doc.getDocumentElement().setAttributeNS(
						namespaceURI,
						namespace + ":" + getStepNodeName(),
						nodeValue);
				doc.getDocumentElement().setAttribute(namespace + ":" + getStepNodeName(), nodeValue);
				stepNode = doc.getDocumentElement().getAttributeNode(namespace + ":" + getStepNodeName());
			}

			((Step)parent).appendChildNode(stepNode);
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
	public String getSchema(String tns, String occurs) throws EngineException {
		schema = "";
		schema += "\t\t\t<xsd:attribute use=\"optional\" name=\""+ getStepNodeName()+"\" type=\""+ getSchemaType(tns) +"\">\n";
		schema += "\t\t\t\t<xsd:annotation>\n";
		schema += "\t\t\t\t\t<xsd:documentation>"+ XMLUtils.getCDataXml(getComment()) +"</xsd:documentation>\n";
		schema += "\t\t\t\t</xsd:annotation>\n";
		schema += "\t\t\t</xsd:attribute>\n";
		
		return isEnable() && isOutput() ? schema:"";
	}
}
