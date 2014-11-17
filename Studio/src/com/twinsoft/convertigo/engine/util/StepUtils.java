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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.engine.util;

import java.util.Hashtable;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.IteratorStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLComplexStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.engine.EngineException;

public class StepUtils {

	public static Step createStepFromSchemaDomModel(Object parent, Node node) throws EngineException {
		return createStepFromXmlModel(parent, node, true);
	}
	
	public static Step createStepFromXmlDomModel(Object parent, Node node) throws EngineException {
		return createStepFromXmlModel(parent, node, false);
	}

	private static Step createStepFromXmlModel(Object parent, Node node, boolean deepClone) throws EngineException {
		Hashtable<String, Step> stepsMap = new Hashtable<String, Step>(50);
		Step step = createStep(stepsMap, parent, node, deepClone);
		stepsMap.clear();
		return step;
	}
	
	private static Step createStep(Hashtable<String, Step> stepsMap, Object parent, Node node, boolean deepClone) throws EngineException {
		Step step = null;
		
		int nodeType = node.getNodeType();
		switch (nodeType) {
			case Node.ELEMENT_NODE:
				Element element = (Element)node;
				String tagname = element.getTagName();
				if (deepClone && stepsMap.containsKey(tagname)) {
					step = deepClone(parent, (Step)stepsMap.get(tagname));
				}
				
				if (step == null) {
					step = createElementStep(parent,element);
					if (step != null) {
						// Add attributes
						NamedNodeMap map = element.getAttributes();
						for (int i=0; i<map.getLength(); i++) {
							createStep(stepsMap, step, map.item(i), deepClone);
						}
						// Add elements
						NodeList children = element.getChildNodes();
						for (int i=0; i<children.getLength(); i++) {
							createStep(stepsMap, step, children.item(i), deepClone);
						}
						
						if (parent != null) stepsMap.put(tagname, step);
					}
				}
				break;
			case Node.ATTRIBUTE_NODE:
				step = createAttributeStep(parent,(Attr)node);
				break;
			default:
				break;
		}
		
		return step;
	}
	
	private static Step deepClone(Object parent, Step step) throws EngineException {
		Step cloned = null;
		try {
			cloned = (Step)step.clone();
			cloned.priority = cloned.getNewOrderValue();
			cloned.bNew = true;
			addStepToParent(parent, cloned);
			
			if (step instanceof StepWithExpressions) {
				StepWithExpressions swe = (StepWithExpressions)step;
				for (Step child: swe.getSteps()) {
					deepClone(cloned, child);
				}
			}
			
		} catch (CloneNotSupportedException e) {}
		return cloned;
	}
	
	private static Step createElementStep(Object parent, Element element) throws EngineException {
		Step step = null;
		if (element != null) {
			if (parent != null) {
				String occurs = element.getAttribute("maxOccurs");//element.getAttribute(xsd.getXmlGenerationDescription().getOccursAttribute());
				if (!occurs.equals("")) {
					if (occurs.equals("unbounded"))
						occurs = "10";
					if (Long.parseLong(occurs, 10) > 1) {
						parent = createIteratorStep(parent, element);
					}
				}
			}
			
			String tagName = element.getTagName();
			String localName = element.getLocalName();
			String elementNodeName = (localName == null) ? tagName:localName;
			Node firstChild = element.getFirstChild();
			boolean isComplex = ((firstChild != null) && (firstChild.getNodeType() != Node.TEXT_NODE));
			
			if (isComplex){
				step = new XMLComplexStep();
				((XMLComplexStep)step).setNodeName(elementNodeName);
			}
			else {
				step = new XMLElementStep();
				((XMLElementStep)step).setNodeName(elementNodeName);
			}
			step.bNew = true;
			addStepToParent(parent, step);
		}
		return step;
	}
	
	private static void addStepToParent(Object parent, Step step) throws EngineException {
		if (step != null) {
			if (parent instanceof Sequence)
				step.setSequence((Sequence)parent);
			else
				((StepWithExpressions)parent).addStep(step);
		}
	}
	
	private static Step createIteratorStep(Object parent, Element element) throws EngineException {
		Step step = (Step)parent;
		if (parent != null) {
			step = new IteratorStep();
			step.bNew = true;
			addStepToParent(parent, step);
		}
		return step;
	}
	
	private static Step createAttributeStep(Object parent, Attr attr) throws EngineException {
		XMLAttributeStep step = null;
		if (attr != null) {
			String attrName = attr.getName();
			String localName = attr.getLocalName();
			String attributeNodeName = (localName == null) ? attrName:localName;
			if (!attributeNodeName.equals("done"/*xsd.getXmlGenerationDescription().getDoneAttribute()*/) &&
				!attributeNodeName.equals("occurs"/*xsd.getXmlGenerationDescription().getOccursAttribute()*/)) {
				step = new XMLAttributeStep();
				step.setNodeName(attributeNodeName);
				step.bNew = true;
				addStepToParent(parent, step);
			}
		}
		return step;
	}
	
//	public static void main(String[] args) {
//
//	}

}
