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

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.EngineException;

public class ContextAddTextNodeStatement extends Statement {
	private static final long serialVersionUID = 1473841735963382690L;
	
	private String node = "";
	private String tagname = "tag";
	private String expression = "//todo";
	
	public ContextAddTextNodeStatement() {
		super();
	}

	public ContextAddTextNodeStatement(String tag, String expression, String node) {
		super();
		this.tagname = tag;
		this.expression = expression;
		this.node = node;
	}
	
	public String toString() {
		String name = "<" + tagname + ">eval('" + StringUtils.abbreviate(expression, 15) + "')</" + tagname + ">";
		return name;
	}
	
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, expression, "ContextAddTextNode", true);
				scope.put("__tmp__ContextAddTextNode", scope, evaluated);
				evaluate(javascriptContext, scope, "context.addTextNodeUnderRoot('"+tagname+"',__tmp__ContextAddTextNode)", "ContextSet", true);
				scope.delete("__tmp__ContextAddTextNode");
				
				return true;
			}
		}
		return false;
	}

	public String toJsString() {
		return expression;
	}

	public String getTagName() {
		return tagname;
	}

	public void setTagName(String tag) {
		this.tagname = tag;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}
}
