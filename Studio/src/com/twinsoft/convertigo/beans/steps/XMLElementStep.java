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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XMLElementStep extends StepWithExpressions implements IStepSourceContainer {

	private static final long serialVersionUID = -427374285639844989L;
	
	protected XMLVector<String> sourceDefinition = new XMLVector<String>();
	protected String nodeName = "element";
	protected String nodeText = "";
	
	private transient StepSource source = null;
	
	public XMLElementStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	@Override
    public XMLElementStep clone() throws CloneNotSupportedException {
    	XMLElementStep clonedObject = (XMLElementStep) super.clone();
    	clonedObject.source = null;
        return clonedObject;
    }

	@Override
    public XMLElementStep copy() throws CloneNotSupportedException {
    	XMLElementStep copiedObject = (XMLElementStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":" =\""+nodeText+"\"";
		} catch (EngineException e) {
		}
		return "<"+ nodeName +">" + label + (!text.equals("") ? " // "+text:"");
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
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		boolean useDefaultValue = true;
		NodeList list = getContextValues();
		if (list != null) {
			int len = list.getLength();
			useDefaultValue = (len == 0);
			if (!useDefaultValue) {
				for (int i = 0; i < len; i++) {
					Node node = list.item(i);
					if (node != null) {
						String nodeValue = getNodeValue(node);
						if (nodeValue != null) {
							Node text = doc.createTextNode(nodeValue);
							stepNode.appendChild(text);
						}
					}
				}
			}
		}
		if (useDefaultValue) {
			Node text = doc.createTextNode(getNodeText());
			stepNode.appendChild(text);
		}
	}

	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
    	if (isEnable) {
			if (inError()) {
				Engine.logBeans.info("Skipping step "+ this +" ("+ hashCode()+") because its source is in error");
				return true;
			}
    		return super.executeNextStep(javascriptContext, scope);
    	}
    	return false;
	}
	
	@Override
	public String getSchemaType(String tns) {
		return hasSteps() ? tns +":"+ getStepNodeName() + priority +"StepType":getSchemaDataType(tns);
	}
	
	@Override
	public String getSchema(String tns, String occurs) throws EngineException {
		schema = "";
		String maxOccurs = (occurs == null) ? "":"maxOccurs=\""+occurs+"\"";
		schema += "\t\t\t<xsd:element minOccurs=\"0\" "+maxOccurs+" name=\""+ getStepNodeName()+"\" type=\""+ getSchemaType(tns) +"\">\n";
		schema += "\t\t\t\t<xsd:annotation>\n";
		schema += "\t\t\t\t\t<xsd:documentation>"+ XMLUtils.getCDataXml(getComment()) +"</xsd:documentation>\n";
		schema += "\t\t\t\t</xsd:annotation>\n";
		schema += "\t\t\t</xsd:element>\n";
		
		return isEnable() && isOutput() ? schema:"";
	}

	@Override
	public void addSchemaType(HashMap<Long, String> stepTypes, String tns, String occurs) throws EngineException {
		if (hasSteps()) { // if has attributes
			String stepTypeSchema = "";
			stepTypeSchema += "\t<xsd:complexType name=\""+ getSchemaTypeName(tns) +"\">\n";
			stepTypeSchema += "\t\t<xsd:simpleContent>\n";
			stepTypeSchema += "\t\t\t\t<xsd:extension base=\""+ getSchemaDataType(tns) +"\">\n";
			// Adds attributes
			for (Step step: getSteps()) {
				step.addSchemaType(stepTypes, "p_ns");
				stepTypeSchema += step.getSchema(tns);
			}
			stepTypeSchema += "\t\t\t\t</xsd:extension>\n";
			stepTypeSchema += "\t\t</xsd:simpleContent>\n";
			stepTypeSchema += "\t</xsd:complexType>\n";
			
			stepTypes.put(new Long(priority), stepTypeSchema);
		}
	}
	
}
