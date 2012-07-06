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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.EngineException;

public class XMLTransformStep extends XMLElementStep {

	private static final long serialVersionUID = 6828884684092984710L;

	protected XMLVector<XMLVector<String>> replacements = new XMLVector<XMLVector<String>>();
	
	public XMLTransformStep() {
		super();
	}

	@Override
    public XMLTransformStep clone() throws CloneNotSupportedException {
    	XMLTransformStep clonedObject = (XMLTransformStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLTransformStep copy() throws CloneNotSupportedException {
    	XMLTransformStep copiedObject = (XMLTransformStep) super.copy();
        return copiedObject;
    }
	
	public XMLVector<XMLVector<String>> getReplacements() {
		return replacements;
	}

	public void setReplacements(XMLVector<XMLVector<String>> replacements) {
		this.replacements = replacements;
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
			Node text = doc.createTextNode(transform(getNodeText()));
			stepNode.appendChild(text);
		}
	}
	
	@Override
	protected String getNodeValue(Node node) {
		String nodeValue = super.getNodeValue(node);
		return transform(nodeValue);
	}

	protected String transform(String nodeValue) {
		String transformed = nodeValue;
		if (transformed != null) {
			for (int i=0; i<replacements.size(); i++) {
				XMLVector<String> xmlv = replacements.elementAt(i);
				String regexp = xmlv.elementAt(0);
				String replacement = xmlv.elementAt(1);
				if (!regexp.equals(""))
					transformed = transformed.replaceAll(regexp, replacement);
			}
		}
		return transformed;
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? "@("+ getLabel()+")":"\""+getNodeText()+"\"";
		} catch (EngineException e) {
		}
		return "<"+ nodeName +">" + "Transform("+ label +")"+ (!text.equals("") ? " // "+text:"");
	}
	
}
