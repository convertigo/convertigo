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

import com.twinsoft.convertigo.engine.EngineException;

public class WhileStatement extends BlockStatement {

	private static final long serialVersionUID = 287416814455697780L;
	
	public WhileStatement() {
		super(true);
	}

	public WhileStatement(String condition) {
		super(condition,true);
	}

	public String toString() {
		String text = this.getComment();
		return "while("+ condition +")"+ (!text.equals("") ? " // "+text:"");
	}

	public String toJsString() {
		String code = "";
		if (!condition.equals("")) {
			code += " while ("+ condition +") {\n";
			code += super.toString();
			code += " \n}\n";
		}
		return code;
	}
	
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, condition, "condition", true);
				if (evaluated instanceof Boolean) {
					if (evaluated.equals(Boolean.TRUE)) {
						return executeNextStatement(javascriptContext, scope);
					}
					return true;
				}
				else {
					EngineException ee = new EngineException(
							"Invalid statement condition.\n" +
							"WhileStatement: \"" + getName()+ "\"");
					throw ee;
				}
			}
		}
		return false;
	}

	public boolean doLoop(Context javascriptContext, Scriptable scope) throws EngineException {
		return execute(javascriptContext, scope);
	}

}
