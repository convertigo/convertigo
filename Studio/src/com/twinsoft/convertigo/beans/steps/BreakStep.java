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

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;

public class BreakStep extends SimpleStep {

	private static final long serialVersionUID = -1278006258142022953L;

	public BreakStep() {
		super("");
	}

	public BreakStep(String expression) {
		super(expression);
	}

	@Override
    public BreakStep clone() throws CloneNotSupportedException {
    	BreakStep clonedObject = (BreakStep) super.clone();
        return clonedObject;
    }

	@Override
    public BreakStep copy() throws CloneNotSupportedException {
    	BreakStep copiedObject = (BreakStep) super.copy();
        return copiedObject;
    }

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExecute(javascriptContext, scope)) {
				List<StepWithExpressions> parents = new ArrayList<StepWithExpressions>();
				DatabaseObject parentStep = this.parent;
				while (parentStep != null) {
					try {
						parents.add((StepWithExpressions) parentStep);
					} catch (Exception e) {};
					
					if (parentStep instanceof LoopStep) {
						for (StepWithExpressions swe : parents)
							swe.bContinue = false;
						break;
					}
					
					parentStep = parentStep.getParent();
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		return "break;" + (!text.equals("") ? " // "+text:"");
	}

}
