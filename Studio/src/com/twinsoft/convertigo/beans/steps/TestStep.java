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
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class TestStep extends BlockStep implements IStepSourceContainer {

	private static final long serialVersionUID = -9065196463100156249L;

	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	public TestStep() {
		super();
	}

	@Override
    public TestStep clone() throws CloneNotSupportedException {
    	TestStep clonedObject = (TestStep) super.clone();
        return clonedObject;
    }

	@Override
	public TestStep copy() throws CloneNotSupportedException {
		TestStep copiedObject = (TestStep)super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":" @(??)";
		} catch (EngineException e) {}
		return getName() + label;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}

	protected abstract boolean executeTest(Context javascriptContext, Scriptable scope) throws EngineException;

	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
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
