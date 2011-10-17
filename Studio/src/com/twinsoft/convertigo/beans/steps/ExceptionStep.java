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

import com.twinsoft.convertigo.engine.EngineException;

/**
 * @author nathalieh
 *
 */
public class ExceptionStep extends SimpleStep {

	private static final long serialVersionUID = 7505194897465946697L;

	public ExceptionStep() {
		super("");
	}

	public ExceptionStep(String expression) {
		super(expression);
	}
	
    public Object clone() throws CloneNotSupportedException {
    	ExceptionStep clonedObject = (ExceptionStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	ExceptionStep copiedObject = (ExceptionStep) super.copy();
        return copiedObject;
    }
	
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				if (evaluated != null) {
					String message = "A step exception has been raised";
					Throwable t = new Throwable(evaluated.toString());
					EngineException ee = new EngineException(message,t);
					throw ee;
				}
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		String text = this.getComment();
		return "throw Exception; "+ (!text.equals("") ? " // "+text:"");
	}	

}
