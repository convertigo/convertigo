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

public class BreakStatement extends SimpleStatement {

	private static final long serialVersionUID = 3230375959217074041L;

	public BreakStatement() {
		super("");
	}

	public BreakStatement(String expression) {
		super(expression);
	}

	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				DatabaseObject parentStatement = this.parent;
				while (parentStatement != null) {
					if (parentStatement instanceof BlockStatement) {
						((BlockStatement)parentStatement).bContinue = false;
						if (((BlockStatement)parentStatement).isLoop)
							break;
					}
					parentStatement = parentStatement.getParent();
				}
				return true;
			}
		}
		return false;
	}

	public String toString() {
		String text = this.getComment();
		return "break;" + (!text.equals("") ? " // "+text:"");
	}

}
