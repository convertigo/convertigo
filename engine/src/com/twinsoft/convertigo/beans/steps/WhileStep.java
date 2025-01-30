/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.steps;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.EngineException;

public class WhileStep extends LoopStep {

	private static final long serialVersionUID = 7986232603413184830L;

	public WhileStep() {
		super();
	}

	public WhileStep(String condition) {
		super(condition);
	}

	@Override
    public WhileStep clone() throws CloneNotSupportedException {
    	WhileStep clonedObject = (WhileStep) super.clone();
        return clonedObject;
    }

	@Override
    public WhileStep copy() throws CloneNotSupportedException {
    	WhileStep copiedObject = (WhileStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String condition = getCondition();
		return "while(" + (condition.equals("") ? "??" : condition) + ")";
	}

	@Override
	public String toJsString() {
		String code = "";
		String condition = getCondition();
		if (!condition.equals("")) {
			code += " while ("+ condition +") {\n";
			code += super.toString();
			code += " \n}\n";
		}
		return code;
	}

	@Override
	protected boolean hasToEvaluateBeforeNextStep() throws EngineException {
		return true;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			do {
				if (!super.stepExecute(javascriptContext, scope)) {
					break;
				}
			}
			while (evaluated.equals(Boolean.TRUE));
			return true;
		}
		return false;
	}

	@Override
	protected void reset() throws EngineException {
		super.reset();
	}
	
}
