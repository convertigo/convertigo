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

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.ISchemaAttributeGenerator;
import com.twinsoft.convertigo.beans.core.ISimpleTypeAffectation;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class XMLAttributeStep extends Step implements IStepSourceContainer, ISchemaAttributeGenerator, ISimpleTypeAffectation {

	private static final long serialVersionUID = 61436680158858545L;

	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private String nodeName = "attribute";
	
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

	private String nodeText = "";
	
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
							
							stepNode = ((Step)parent).appendChildNode(stepNode);
						}
					}
				}
			}
			if (useDefaultValue) {
				String namespace = getNodeNameSpace();
				if (namespace.equals("")) {
					doc.getDocumentElement().setAttribute(getStepNodeName(), getNodeText());
					stepNode = doc.getDocumentElement().getAttributeNode(getStepNodeName());
					stepNode = ((Step)parent).appendChildNode(stepNode);
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

				stepNode = ((Step)parent).appendChildNode(stepNode);
			}
		}
		return stepNode;
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
