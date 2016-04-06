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

public class ContainerStatement extends StatementWithExpressions {

	private static final long serialVersionUID = -1396001442815390629L;

	public ContainerStatement() {
		super();
	}
	
	public String toString() {
		return "container";
	}
		
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {		
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				return executeNextStatement(javascriptContext, scope);
			}
		}
		return false;
	}

	public boolean executeNextStatement(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
	    	if (hasStatements()) {
	    		if (currentChildStatement < numberOfStatements()) {
	        		Statement st = (Statement) getStatements().get(currentChildStatement);
	        		executeNextStatement(st, javascriptContext, scope);
	        		if (bContinue)
	        			return executeNextStatement(javascriptContext, scope);
	    		}
	    	}
	    	return true;
    	}
    	return false;
   	}

}
