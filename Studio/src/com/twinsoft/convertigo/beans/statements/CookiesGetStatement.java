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

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CookiesUtils;

public class CookiesGetStatement extends Statement {
	private static final long serialVersionUID = -8541325182087905783L;

	private String variable = "var";
	private String separator = "|";

	public CookiesGetStatement() {
		super();
	}

	public CookiesGetStatement(String key, String variable) {
		super();
		this.variable = variable;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		return variable + "=cookies.get()" + (!text.equals("") ? " // " + text : "");
	}

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				if (this.getConnector().handleCookie) {
					HttpState httpState = this.getParentTransaction().context.httpState;
					if (httpState != null) {
						Cookie[] cookies = httpState.getCookies();
						String code = variable + "=";
						boolean array = separator.length() == 0;
						if (cookies.length == 0) {
							code += array ? "[]" : "''";
						} else {
							String sep = array ? "\",\"" : separator;
							code += (array ? "[\"" : "\"") + CookiesUtils.formatCookie(cookies[0]).replace("\"", "\\\"");
							for (int i = 1; i < cookies.length; i++) {
								code += sep + CookiesUtils.formatCookie(cookies[i]).replace("\"", "\\\"");
							}
							code += (array ? "\"]" : "\"");
						}
						evaluate(javascriptContext, scope, code, "CookiesGet", true);
					} else {
						Engine.logBeans.debug("(CookiesGetStatement) No httpState for cookies");
					}
				}
				return true;
			}
		}
		return false;
	}

	public String toJsString() {
		return "";
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}
}