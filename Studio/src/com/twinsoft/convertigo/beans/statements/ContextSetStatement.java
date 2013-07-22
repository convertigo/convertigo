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
import com.twinsoft.convertigo.engine.EngineException;

public class ContextSetStatement extends Statement {
	private static final long serialVersionUID = 4017811795869683136L;
	
	private String key = "keyname";
	private String expression = "//todo";
	
	public ContextSetStatement() {
		super();
	}

	public ContextSetStatement(String key, String expression) {
		super();
		this.key = key;
		this.expression = expression;
	}
	
	public String toString() {
		String text = this.getComment();
		return "set("+key+")="+(expression.length()>20?expression.substring(0,20)+"...":expression)+(!text.equals("") ? " // "+text:"");
	}
	
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, expression, "ContextSet", true);
				scope.put("__tmp__ContextSetStatement", scope, evaluated);
				evaluate(javascriptContext, scope, "context.set('"+key+"',__tmp__ContextSetStatement)", "ContextSet", true);
				scope.delete("__tmp__ContextSetStatement");
				return true;
			}
		}
		return false;
	}

	public String toJsString() {
		return expression;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
}
