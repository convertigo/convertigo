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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XMLAttributeStep extends Step implements IStepSourceContainer {

	private static final long serialVersionUID = 61436680158858545L;

	protected XMLVector<String> sourceDefinition = new XMLVector<String>();
	protected String nodeName = "attribute";
	
	private String nodeNameSpace = "";
	public String getNodeNameSpace() {
		return nodeNameSpace;
	}

	public void setNodeNameSpace(String nodeNameSpace) {
		this.nodeNameSpace = nodeNameSpace;
	}

	private String nodeNameSpaceURI = "";
	public String getNodeNameSpaceURI() {
		return nodeNameSpaceURI;
	}

	public void setNodeNameSpaceURI(String nodeNameSpaceURI) {
		this.nodeNameSpaceURI = nodeNameSpaceURI;
	}

	protected String nodeText = "";
	
	private transient StepSource source = null;
	
	public XMLAttributeStep() {
		super();
		setOutput(true);
		this.xml = true;
	}
	
	@Override
    public XMLAttributeStep clone() throws CloneNotSupportedException {
    	XMLAttributeStep clonedObject = (XMLAttributeStep) super.clone();
    	clonedObject.source = null;
        return clonedObject;
    }

	@Override
    public XMLAttributeStep copy() throws CloneNotSupportedException {
    	XMLAttributeStep copiedObject = (XMLAttributeStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":"";
		} catch (EngineException e) {}
		return "@"+ nodeName + label + (!text.equals("") ? " // "+text:"");
	}

	protected boolean workOnSource() {
		return true;
	}

	protected StepSource getSource() {
		if (source == null) source = new StepSource(this,sourceDefinition);
		return source;
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

	public String toJsString() {
		return "";
	}

	@Override
	public String getStepNodeName() {
		return getNodeName();
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
		return "//document/@"+ getStepNodeName();
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
		Document doc = getOutputDocument();
		if (!inError()) {
			boolean useDefaultValue = true;
			NodeList list = getContextValues();
			if (list != null) {
				int len = list.getLength();
				useDefaultValue = (len == 0);
				if (!useDefaultValue) {
					for (int i = 0; i < len; i++) {
						Node node = list.item(i);
						if (node != null) {
							String snodeName = ((len==1) ? getStepNodeName():node.getNodeName());
							String snodeValue = getNodeValue(node);
							
							String namespace = getNodeNameSpace();
							if (namespace.equals("")) {
								doc.getDocumentElement().setAttribute(snodeName, (snodeValue == null) ? getNodeText():snodeValue);
								stepNode = doc.getDocumentElement().getAttributeNode(snodeName);
							}
							else {
								String namespaceURI = getNodeNameSpaceURI();
								if (namespaceURI.equals(""))
									throw new EngineException("Blank namespace URI is not allowed (using namespace '"
											+ namespace + "' in XMLAttribute step '" + getName() + "')");
	
								doc.getDocumentElement().setAttributeNS(
										namespaceURI,
										namespace + ":" + snodeName,
										(snodeValue == null) ? getNodeText() : snodeValue);
								stepNode = doc.getDocumentElement().getAttributeNode(namespace + ":" + snodeName);
							}
							
							((Step)parent).appendChildNode(stepNode);
						}
					}
				}
			}
			if (useDefaultValue) {
				String namespace = getNodeNameSpace();
				if (namespace.equals("")) {
					doc.getDocumentElement().setAttribute(getStepNodeName(), getNodeText());
					stepNode = doc.getDocumentElement().getAttributeNode(getStepNodeName());
					((Step)parent).appendChildNode(stepNode);
				}
				else {
					String namespaceURI = getNodeNameSpaceURI();
					if (namespaceURI.equals(""))
						throw new EngineException("Blank namespace URI is not allowed (using namespace '"
								+ namespace + "' in XMLAttribute step '" + getName() + "')");

					doc.getDocumentElement().setAttributeNS(
							namespaceURI,
							namespace + ":" + getStepNodeName(),
							getNodeText());
					doc.getDocumentElement().setAttribute(namespace + ":" + getStepNodeName(), getNodeText());
					stepNode = doc.getDocumentElement().getAttributeNode(namespace + ":" + getStepNodeName());
				}

				((Step)parent).appendChildNode(stepNode);
			}
		}
		return stepNode;
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
