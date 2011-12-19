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

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * 
 * @author nathalieh
 *
 */public class SimpleStep extends Step {

	private static final long serialVersionUID = -3775791876054667383L;

	private String expression = "\"//todo\"";
	
	public SimpleStep() {
		super();
	}

	public SimpleStep(String expression) {
		super();
		this.expression = expression;
	}
	
    public Object clone() throws CloneNotSupportedException {
    	SimpleStep clonedObject = (SimpleStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	SimpleStep copiedObject = (SimpleStep) super.copy();
        return copiedObject;
    }
    
	public String toString() {
		return this.getName();
	}

	protected boolean workOnSource() {
		return false;
	}
	
	protected StepSource getSource() {
		return null;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, getExpression(), "expression", true);
				return true;
			}
		}
		return false;
	}
	
	public String toJsString() {
		return expression;
	}

}
