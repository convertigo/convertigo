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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;

public class SourceStep extends Step implements IStepSourceContainer {

	private static final long serialVersionUID = 3915732415195665643L;

	protected XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	public String variableName = "myNodeList";
	
	private transient StepSource source = null;
	
	public SourceStep() {
		super();
	}

    public Object clone() throws CloneNotSupportedException {
    	SourceStep clonedObject = (SourceStep) super.clone();
    	clonedObject.source = null;
        return clonedObject;
    }
	
	public Object copy() throws CloneNotSupportedException {
		SourceStep copiedObject = (SourceStep)super.copy();
		return copiedObject;
	}
    
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":" @(??)";
		} catch (EngineException e) {}
		return variableName + label + (!text.equals("") ? " // "+text:"");
	}

	protected boolean workOnSource() {
		return true;
	}
	
	protected StepSource getSource() {
		if (source == null) source = new StepSource(this,sourceDefinition);
		return source;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
		source = new StepSource(this,sourceDefinition);
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				NodeList list = getSource().getContextValues();
				if (list != null) {
					scope.put(variableName, scope, list);
					return true;
				}
			}
		}
		return false;
	}
	
	public String toJsString() {
		return "";
	}
	
}
