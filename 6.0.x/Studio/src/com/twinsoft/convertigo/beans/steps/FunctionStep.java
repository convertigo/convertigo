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

import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;

public class FunctionStep extends StepWithExpressions {

	private static final long serialVersionUID = -3731110617950647801L;

	//private Object[] args = new Object[]{};
	
	protected transient Object returnedValue = null;
	
	public FunctionStep() {
		super();
	}
	
    public Object clone() throws CloneNotSupportedException {
    	FunctionStep clonedObject = (FunctionStep) super.clone();
    	clonedObject.returnedValue = null;
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	FunctionStep copiedObject = (FunctionStep) super.copy();
        return copiedObject;
    }
    
	public String toString() {
		return "function "+ name +"()";
	}
	
	protected boolean workOnSource() {
		return false;
	}
	
	protected StepSource getSource() {
		return null;
	}
	
	protected void reset() throws EngineException {
		super.reset();
		returnedValue = null;
	}
	
	/**
	 * @return Returns the returnedValue.
	 */
	public Object getReturnedValue() {
		return returnedValue;
	}
	
	/**
	 * @param returnedValue The returnedValue to set.
	 */
	public void setReturnedValue(Object returnedValue) {
		this.returnedValue = returnedValue;
	}

}
