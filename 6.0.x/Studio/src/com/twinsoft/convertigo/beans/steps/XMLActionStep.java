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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

abstract public class XMLActionStep extends Step {
	private static final long serialVersionUID = -3582328787633662760L;
	
	protected XMLVector<XMLVector<Object>> sourcesDefinition = new XMLVector<XMLVector<Object>>();
	protected String nodeName = "element";
	
	public XMLActionStep() {
		super();
		this.output = true;
		this.xml = true;
	}

    public Object clone() throws CloneNotSupportedException {
    	XMLActionStep clonedObject = (XMLActionStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	XMLActionStep copiedObject = (XMLActionStep) super.copy();
        return copiedObject;
    }
    
    protected String getSpecificLabel() throws EngineException {
		if (sourcesDefinition.size() > 0) {
			for (int i=0; i<sourcesDefinition.size();i++) {
				StepSource stepSource = getDefinitionsSource(i);
				if (stepSource != null) {
					if (stepSource.getLabel().equals("! broken source !"))
						return " (! broken source in variable !)";
				}
			}
		}
		
		StepSource source = getDefinitionsSource(0);
		if (source != null) {
			String label = source.getLabel();
			if (!label.equals("")) label = "@("+label+")";
			else label = "\"" + getDefinitionsDefaultValue(0) + "\"";
			return "("+label+",...)";
		}
		else {
			return "(??)";
		}
	}
    
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {}
		return "<"+ nodeName +"> " + getActionName() + label + (!text.equals("") ? " // "+text:"");
	}
    
	public String toJsString() {
		return "";
	}
	
	protected boolean workOnSource() {
		return false;
	}
	
	protected StepSource getSource() {
		return null;
	}
	
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
		
	public XMLVector<XMLVector<Object>> getSourcesDefinition() {
		return sourcesDefinition;
	}

	public void setSourcesDefinition(XMLVector<XMLVector<Object>> sourcesDefinition) {
		this.sourcesDefinition = sourcesDefinition;
	}
	
	public int getSourcesDefinitionSize() {
		return sourcesDefinition.size();
	}
	
	public List<String> getSourceDefinition(int index) {
		return GenericUtils.cast(sourcesDefinition.get(index).get(1));
	}
	
	public String getStepNodeName() {
		return getNodeName();
	}
	
	protected StepSource getDefinitionsSource(int index) {
		if (sourcesDefinition.size() > 0) {
			List<Object> xmlv = sourcesDefinition.elementAt(index);
			if (xmlv.size() > 0) {
				StepSource source = new StepSource(this, GenericUtils.<XMLVector<String>>cast(xmlv.get(1)));
				return source;
			}
		}
		return null;
	}
	
	protected void setDefinitionsSource(int index, XMLVector<String> nxmlv) {
		if (sourcesDefinition.size() > 0) {
			List<Object> xmlv = sourcesDefinition.elementAt(index);
			xmlv.set(1, nxmlv);
		}
	}
	
	protected String getDefinitionsDefaultValue(int index) {
		if (sourcesDefinition.size() > 0) {
			List<Object> xmlv = sourcesDefinition.elementAt(index);
			if (xmlv != null)
				return (String) xmlv.get(2);
		}
		return null;
	}

	public void stepMoved(StepEvent stepEvent) {
		if (sourcesDefinition.size() > 0) {
			for (int i=0; i<sourcesDefinition.size();i++) {
				StepSource stepSource = getDefinitionsSource(i);
				if (stepSource != null) {
					stepSource.updateTargetStep((Step)stepEvent.getSource(), (String)stepEvent.data);
				}
			}
		}
	}

	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		String nodeValue = getActionValue();
		stepNode.appendChild(doc.createTextNode(nodeValue));
	}
	
	protected String getActionValue() throws EngineException {
		return "";
	}
	
	abstract protected String getActionName();
	
}
