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

import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;

public class IfStep extends BlockStep {

	private static final long serialVersionUID = -2543504392257804533L;

	public IfStep() {
		super();
	}
	
	public IfStep(String condition) {
		super(condition);
	}

    public Object clone() throws CloneNotSupportedException {
    	IfStep clonedObject = (IfStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	IfStep copiedObject = (IfStep) super.copy();
        return copiedObject;
    }
	
	public String toString() {
		String text = this.getComment();
		return "if("+ (condition.equals("")?"??":condition) +")" + (!text.equals("") ? " // "+text:"");
	}
	
	public String toJsString() {
		String code = "";
		if (!condition.equals("")) {
			code += " if ("+ condition +") {\n";
			code += super.toString();
			code += " \n}\n";
		}
		return code;
	}
	
	protected boolean workOnSource() {
		return false;
	}
	
	protected StepSource getSource() {
		return null;
	}
	
	protected boolean hasToEvaluateBeforeNextStep() throws EngineException {
		return true;
	}

	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			boolean test = evaluateStep(javascriptContext, scope);
			return super.executeNextStep(test, javascriptContext, scope);
		}
		return false;
	}
}
