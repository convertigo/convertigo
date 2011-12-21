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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;

public class ReturnStep extends SimpleStep {

	private static final long serialVersionUID = 3747751844918832857L;

	public ReturnStep() {
		super("");
	}

	public ReturnStep(String expression) {
		super(expression);
	}

    public Object clone() throws CloneNotSupportedException {
    	ReturnStep clonedObject = (ReturnStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	ReturnStep copiedObject = (ReturnStep) super.copy();
        return copiedObject;
    }
	
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExecute(javascriptContext, scope)) {
				DatabaseObject parentStep = this.parent;
				while (parentStep != null) {
					if (parentStep instanceof FunctionStep) {
						FunctionStep functionStep = (FunctionStep)parentStep;
						functionStep.setReturnedValue(this.evaluated);
						functionStep.bContinue = false;
						return true;
					}
					else {
						if (parentStep instanceof StepWithExpressions)
							((StepWithExpressions)parentStep).bContinue = false;
						parentStep = parentStep.getParent();
					}
				}
				sequence.skipNextSteps(true);
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		String text = this.getComment();
		return "return "+ getExpression() + (!text.equals("") ? " // "+text:"");
	}

}
