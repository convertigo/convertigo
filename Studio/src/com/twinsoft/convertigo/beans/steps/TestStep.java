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

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class TestStep extends BlockStep implements IStepSourceContainer {

	private static final long serialVersionUID = -9065196463100156249L;

	protected XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	private transient StepSource source = null;
	
	public TestStep() {
		super();
	}

    public Object clone() throws CloneNotSupportedException {
    	TestStep clonedObject = (TestStep) super.clone();
    	clonedObject.source = null;
        return clonedObject;
    }
	
	public Object copy() throws CloneNotSupportedException {
		TestStep copiedObject = (TestStep)super.copy();
		return copiedObject;
	}
    
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":" @(??)";
		} catch (EngineException e) {}
		return name + label + (!text.equals("") ? " // "+text:"");
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

	protected abstract boolean executeTest(Context javascriptContext, Scriptable scope) throws EngineException;
	
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (inError()) {
				Engine.logBeans.info("(TestStep) Skipping step "+ this +" ("+ hashCode()+") because its source is in error");
				return true;
			}
			boolean test = executeTest(javascriptContext, scope);
			return super.executeNextStep(test, javascriptContext, scope);
		}
		return false;
	}
	
}
