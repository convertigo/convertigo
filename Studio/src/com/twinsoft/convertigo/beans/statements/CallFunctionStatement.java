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

package com.twinsoft.convertigo.beans.statements;

import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.EngineException;

public class CallFunctionStatement extends Statement implements ITagsProperty{
	private static final long serialVersionUID = -2138787100798764583L;
	
	String functionName = "";

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope))
				for(FunctionStatement function : getFunctions()){
					if(function.getName().equals(functionName)){
						Scriptable curScope = javascriptContext.initStandardObjects();
						curScope.setParentScope(scope);
						boolean ret = function.execute(javascriptContext, curScope);
						Object returnedValue = function.getReturnedValue();
						if(returnedValue!=null)
							ReturnStatement.returnLoop(this.parent, returnedValue);
						return ret;
					}
			}
		}
		return false;
	}

    @Override
	public String toString(){
		String text = this.getComment();
		return "call "+("".equals(functionName)?"??":functionName+"()") +
				("".equals(text)? "":" // "+text);
	}

    @Override
	public String toJsString() {
		return "";
	}
    
    public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	private List<FunctionStatement> getFunctions(){
		List<FunctionStatement> functions = new LinkedList<FunctionStatement>();
		for(Statement statement : getParentTransaction().getStatements())
			if(statement.getClass().equals(FunctionStatement.class))
				functions.add((FunctionStatement) statement);
		return functions;
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("functionName")){
			List<String> functionsNames = new LinkedList<String>();
			for(FunctionStatement function : getFunctions())
				functionsNames.add(function.getName());
			return functionsNames.toArray(new String[functionsNames.size()]);
		}
		return new String[0];
	}
}