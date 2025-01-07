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

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.FUNCTION)
public class FunctionStep extends StepWithExpressions {

	private static final long serialVersionUID = -3731110617950647801L;

	//private Object[] args = new Object[]{};
	
	protected transient Object returnedValue = null;
	
	public FunctionStep() {
		super();
	}
	
	@Override
    public FunctionStep clone() throws CloneNotSupportedException {
    	FunctionStep clonedObject = (FunctionStep) super.clone();
    	clonedObject.returnedValue = null;
        return clonedObject;
    }
	
	@Override
    public FunctionStep copy() throws CloneNotSupportedException {
    	FunctionStep copiedObject = (FunctionStep) super.copy();
        return copiedObject;
    }
    
	@Override
	public String toString() {
		return "function "+ getName() +"()";
	}
	
	@Override
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
