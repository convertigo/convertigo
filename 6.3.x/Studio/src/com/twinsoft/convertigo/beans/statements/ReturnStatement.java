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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;

public class ReturnStatement extends SimpleStatement {

	private static final long serialVersionUID = -6990798453307484333L;

	public ReturnStatement() {
		super("");
	}

	public ReturnStatement(String expression) {
		super(expression);
	}

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				returnLoop(this.parent, this.evaluated);
				return true;
			}
		}
		return false;
	}

	static public void returnLoop(DatabaseObject parentStatement, Object returnedValue){
		while (parentStatement != null) {
			if (parentStatement instanceof FunctionStatement) {
				FunctionStatement functionStatement = (FunctionStatement)parentStatement;
				functionStatement.setReturnedValue(returnedValue);
				functionStatement.bContinue = false;
				break;
			}
			else {
				if (parentStatement instanceof BlockStatement)
					((BlockStatement)parentStatement).bContinue = false;
				parentStatement = parentStatement.getParent();
			}
		}
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		return "return "+ getExpression() + (!text.equals("") ? " // "+text:"");
	}

}
