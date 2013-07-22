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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class BlockStatement extends StatementWithExpressions {
	private static final long serialVersionUID = 5517319866039313466L;
	
	private String condition = "";
	transient public boolean isLoop = false;
	
	public BlockStatement() {
		super();
	}

	public BlockStatement(String condition) {
		super();
		this.condition = condition;
	}

	public BlockStatement(boolean isLoop) {
		super();
		this.isLoop = isLoop;
	}

	public BlockStatement(String condition, boolean isLoop) {
		super();
		this.isLoop = isLoop;
		this.condition = condition;
	}

	/**
	 * @return Returns the condition.
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * @param condition The condition to set.
	 */
	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	public String toString() {
		String text = this.getComment();
		return getName() + (!text.equals("") ? " // "+text:"");
	}

	public boolean executeNextStatement(Context javascriptContext, Scriptable scope) throws EngineException
    {
    	if (isEnable()) {
	    	if (hasStatements()) {
	    		if (currentChildStatement < numberOfStatements()) {
	        		Statement st = (Statement) getStatements().get(currentChildStatement);
	        		executeNextStatement(st, javascriptContext, scope);
	        		if (bContinue)
	        			return executeNextStatement(javascriptContext, scope);
	    		}
	    		else {
	    			if (isLoop) {
	    				currentChildStatement = 0;
	    				if (bContinue)
	    					return doLoop(javascriptContext, scope);
	    			}
	    		}
	    	}
	    	return true;
    	}
    	return false;
    }
	
    public boolean doLoop(Context javascriptContext, Scriptable scope) throws EngineException {
    	return isLoop;
    }
}
