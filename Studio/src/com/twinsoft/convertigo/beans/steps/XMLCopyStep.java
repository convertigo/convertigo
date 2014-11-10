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

import java.util.HashMap;
import java.util.Map;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class XMLCopyStep extends Step implements IStepSourceContainer {

	private static final long serialVersionUID = 4871778624030668414L;
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	public XMLCopyStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	@Override
    public XMLCopyStep clone() throws CloneNotSupportedException {
    	XMLCopyStep clonedObject = (XMLCopyStep) super.clone();
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
	
	public String toJsString() {
		return "";
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
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
	protected Node createStepNode() throws EngineException {
		Document doc = getOutputDocument();
		Element stepNode = doc.getDocumentElement();
		stepNode.setAttribute("step_id", this.executeTimeID);
		stepNode.setAttribute("step_copy", "true");
		
		if (!inError()) {
			createStepNodeValue(doc, stepNode);
			if (parent instanceof Step)
				stepNode = ((Step)parent).appendChildNode(stepNode);
		}
		return stepNode;
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		createCopy(this, doc, stepNode);
	}
	
	static protected void createCopy(Step step, Document doc, Element stepNode) throws EngineException {
		NodeList list = step.getContextValues();
		if (list != null) {
			int len = list.getLength();
			for (int i=0; i<len;i++) {
				Node node = list.item(i);
				if (node != null) {
					Node imported = doc.importNode(node, true);
					if (imported.getNodeType() == Node.ELEMENT_NODE) {
						stepNode.appendChild((Element) imported);
					} else if (imported.getNodeType() == Node.ATTRIBUTE_NODE) {
						stepNode.setAttribute(imported.getNodeName(),imported.getNodeValue());
					}
				}
			}
		}
		removeUselessAttributes(stepNode);
	}

	protected StepSource getTargetSource() throws EngineException {
		StepSource source = getSource();
		if (!source.isEmpty()) {
			Step sourceStep = source.getStep();
			if (sourceStep instanceof IteratorStep) {
				source = ((IteratorStep) sourceStep).getSource();
			}
		}
		return source;
	}
	
	protected String getTargetXPath() throws EngineException {
		String xpath = "";
		StepSource source = getSource();
		if (!source.isEmpty()) {
			Step sourceStep = source.getStep();
			if (sourceStep instanceof IteratorStep) {
				xpath = source.getXpath().substring(1);
			}
		}
		return xpath;
	}
	
	@Override
	public XmlSchemaObject getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		try {
			StepSource source = getTargetSource();
			if (!source.isEmpty()) {
				XmlSchemaObject object = SchemaMeta.getXmlSchemaObject(schema, source.getStep());
				if (object != null) {
					SchemaMeta.setSchema(object, schema);
					String xpath = source.getXpath();
					String anchor = source.getAnchor() + getTargetXPath();
					if (!".".equals(xpath)) {
						Map<Node, XmlSchemaObject> references = new HashMap<Node, XmlSchemaObject>();
						Document doc = XmlSchemaUtils.getDomInstance(object, references);
						NodeList list = getXPathAPI().selectNodeList(doc.getDocumentElement(), anchor);
						if (list != null) {
							boolean isList = false;
							if (list.getLength() > 1) {
								isList = true;
								object = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
							}
							
							for (int i = 0; i < list.getLength(); i++) {
								Node node = list.item(i);
								XmlSchemaObject referenced = references.get(node);
								if (referenced != null) {
									if (isList) {
										XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence)object;
										xmlSchemaSequence.getItems().add(referenced);
									}
									else {
										object = referenced;
									}
								}
							}
						}
					}
					return object;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return super.getXmlSchemaObject(collection, schema);
	}
}
