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

import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.EngineException;

public class XMLTransformStep extends XMLElementStep {

	private static final long serialVersionUID = 6828884684092984710L;

	protected XMLVector<XMLVector<String>> replacements = new XMLVector<XMLVector<String>>();
	
	public XMLTransformStep() {
		super();
	}

    public Object clone() throws CloneNotSupportedException {
    	XMLTransformStep clonedObject = (XMLTransformStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	XMLTransformStep copiedObject = (XMLTransformStep) super.copy();
        return copiedObject;
    }
	
	public XMLVector<XMLVector<String>> getReplacements() {
		return replacements;
	}

	public void setReplacements(XMLVector<XMLVector<String>> replacements) {
		this.replacements = replacements;
	}
	
	protected String getNodeValue(Node node) {
		String nodeValue = super.getNodeValue(node);
		if (nodeValue != null) {
			for (int i=0; i<replacements.size(); i++) {
				XMLVector<String> xmlv = replacements.elementAt(i);
				String regexp = xmlv.elementAt(0);
				String replacement = xmlv.elementAt(1);
				if (!regexp.equals(""))
					nodeValue = nodeValue.replaceAll(regexp, replacement);
			}
		}
		return nodeValue;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? "@("+ getLabel()+")":"\""+nodeText+"\"";
		} catch (EngineException e) {
		}
		return "<"+ nodeName +">" + "Transform("+ label +")"+ (!text.equals("") ? " // "+text:"");
	}
	
}
