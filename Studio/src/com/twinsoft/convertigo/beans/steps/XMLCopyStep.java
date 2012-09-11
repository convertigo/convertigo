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

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XMLCopyStep extends Step implements IStepSourceContainer {

	private static final long serialVersionUID = 4871778624030668414L;
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	private transient StepSource source = null;
	
	public XMLCopyStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	@Override
    public XMLCopyStep clone() throws CloneNotSupportedException {
    	XMLCopyStep clonedObject = (XMLCopyStep) super.clone();
    	clonedObject.source = null;
        return clonedObject;
    }

	@Override
    public XMLCopyStep copy() throws CloneNotSupportedException {
    	XMLCopyStep copiedObject = (XMLCopyStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":"";
		} catch (EngineException e) {
		}
		return "copyOf" + label + (!text.equals("") ? " // "+text:"");
	}

	protected boolean workOnSource() {
		return true;
	}

	protected StepSource getSource() {
		if (source == null) source = new StepSource(this,sourceDefinition);
		return source;
	}
	
	public String toJsString() {
		return "";
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
		source = new StepSource(this,sourceDefinition);
	}

	@Override
	public String getAnchor() throws EngineException {
		return "//document";
	}

	@Override
	public Node getContextNode(int loop) throws EngineException {
		return outputDocument.getDocumentElement();
	}

	@Override
	protected Node createWsdlDom() throws EngineException {
		wsdlDom = getSequence().createDOM();
		Element element = wsdlDom.getDocumentElement();
		try {
			Document doc = getSource().getWsdlDom();
			String anchor = getSource().getAnchor();
//			Step step = getSource().getStep();
//			String xpath = getSource().getXpath();
//			if (!(step instanceof RequestableStep)) {
//					xpath = xpath.replaceFirst(".", step.getStepNodeName());
//			}
//			NodeList list = getXPathAPI().selectNodeList(doc.getDocumentElement(), xpath);
			NodeList list = getXPathAPI().selectNodeList(doc.getDocumentElement(), anchor);
			if (list != null) {
				for (int i=0; i<list.getLength(); i++) {
					Node imported = wsdlDom.importNode(list.item(i), true);
					element.appendChild(imported);
				}
			}
		} catch (Exception e) {
			wsdlDom = null;
			throw new EngineException("Unable to create WSDL document",e);
		}
		wsdlDomDirty = false;
		return element;
	}

	@Override
	protected Node generateWsdlDom() throws EngineException {
		Element element = null;
		if (isXml()) {
	    	try {
	    		if (wsdlDomDirty || (wsdlDom == null)) {
	    			element = (Element)createWsdlDom();
	    		}
	    		else
	    			element = (Element)wsdlDom.getDocumentElement();
	    	}
	    	catch (Exception e) {
	    		wsdlDom = null;
	    		throw new EngineException("Unable to generate WSDL document",e);
	    	}
		}
		return element;
	}

	@Override
	protected Node createStepNode() throws EngineException {
		Document doc = getOutputDocument();
		Element stepNode = doc.getDocumentElement();
		stepNode.setAttribute("step_id", this.executeTimeID);
		stepNode.setAttribute("step_copy", "true");
		
		if (!inError()) {
			createStepNodeValue(doc, stepNode);
			if (parent instanceof Step)
				((Step)parent).appendChildNode(stepNode);
		}
		return stepNode;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		NodeList list = getContextValues();
		if (list != null) {
			int len = list.getLength();
			for (int i=0; i<len;i++) {
				Node node = list.item(i);
				if (node != null) {
					Node imported = doc.importNode(node, true);
					if (imported.getNodeType() == Node.ELEMENT_NODE)
						stepNode.appendChild((Element)imported);
					else if (imported.getNodeType() == Node.ATTRIBUTE_NODE) {
						stepNode.setAttribute(imported.getNodeName(),imported.getNodeValue());
					}
				}
				removeUselessAttributes(stepNode);
			}
		}
	}

	@Override
	public String getSchemaType(String tns) {
		if  (!getSource().isEmpty()) {
			try {
				if (isOutput()) {
					String schema = getTargetSchema(null);
					int j1 = schema.indexOf(">");
					int j2 = schema.indexOf("/>");
					if ((j1 != -1) && (j2 != -1) && (j2+1 == j1)) {
						int index = schema.indexOf("type=");
						if ((index != -1) && (index < j2)) {
							char c = schema.charAt(index+5);
							int i1 = index+6;
							int i2 = schema.indexOf(c,i1);
							String type = schema.substring(i1, i2);
							return type;
						}
					}
				}
			} catch (EngineException e) {}
		}
		return "";
	}
	
	@Override
	public String getSchema(String tns, String occurs) throws EngineException {
		schema = "";
		schema += getTargetSchema(occurs);
		
		return isEnable() && isOutput() ? schema:"";
	}

	private String getTargetSchema(String occurs) throws EngineException {
		String targetSchema = "";
		if  (!getSource().isEmpty()) {
			Document doc = getSource().getWsdlDom();
			String anchor = getSource().getAnchor();
			try {
				NodeList list = getXPathAPI().selectNodeList(doc.getDocumentElement(), anchor);
				if (list != null) {
					for (int i=0; i<list.getLength(); i++) {
						Node node = list.item(i);
						if (node.getNodeType()== Node.ELEMENT_NODE) {
				            NodeList childNodes = ((Element)node).getElementsByTagName("schema-type");
				            int len = childNodes.getLength();
				            if (len > 0) {
				                Node childNode = childNodes.item(0);
				                Node cdata = XMLUtils.findChildNode(childNode, Node.CDATA_SECTION_NODE);
				                if (cdata != null) {
				                	targetSchema += "\t\t\t" + cdata.getNodeValue()+ "\n";
				                }
				            }
						}
						else if (node.getNodeType()== Node.ATTRIBUTE_NODE) {
							targetSchema += "\t\t\t<xsd:attribute use=\"optional\" name=\""+ node.getNodeName()+"\" type=\"xsd:string\" />\n";
						}
					}
				}
			} catch (TransformerException e) {
				Engine.logBeans.warn("Unable to retrieve schema for XMLCopyStep \""+ getName() +"\"", e);
				targetSchema = "";
			}
		}
		
		return targetSchema;
	}
}
