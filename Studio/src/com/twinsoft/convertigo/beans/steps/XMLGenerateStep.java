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

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.EngineException;

abstract public class XMLGenerateStep extends Step {
	private static final long serialVersionUID = 1384625418225432309L;
	
	private String nodeName = "element";
	
	public XMLGenerateStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	@Override
    public XMLGenerateStep clone() throws CloneNotSupportedException {
    	XMLGenerateStep clonedObject = (XMLGenerateStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLGenerateStep copy() throws CloneNotSupportedException {
    	XMLGenerateStep copiedObject = (XMLGenerateStep) super.copy();
        return copiedObject;
    }

	@Override
    protected String getSpecificLabel() throws EngineException {
    	return "(...)";
    }

	@Override
	public String toString() {
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {}
		return "<" + nodeName + "> " + getActionName() + label;
	}
    
	public String toJsString() {
		return "";
	}
	
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public String getStepNodeName() {
		return getNodeName();
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String nodeValue = getGenerateValue();
		stepNode.appendChild(doc.createTextNode(nodeValue));
	}
	
	protected String getGenerateValue() throws EngineException {
		return "";
	}
	
	abstract protected String getActionName();
	
}
