/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.EngineException;

public class ContextGetStatement extends Statement {
	private static final long serialVersionUID = 7617831008788984139L;
	
	private String key = "keyname";
	private String variable = "varname";
	
	public ContextGetStatement() {
		super();
	}

	public ContextGetStatement(String key, String variable) {
		super();
		this.key = key;
		this.variable = variable;
	}
	
	public String toString() {
		return variable + "=get(" + key + ")";
	}
	
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.execute(javascriptContext, scope)) {
				String code = variable+"=context.get('"+key+"')";
				evaluate(javascriptContext, scope, code, "ContextGet", true);
				return true;
			}
		}
		return false;
	}

	public String toJsString() {
		return "";
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}
}
