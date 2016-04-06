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

/**
 * @author nathalieh
 *
 */
public class ExceptionStatement extends SimpleStatement {

	private static final long serialVersionUID = -5980402985444974404L;

	public ExceptionStatement() {
		super("");
	}

	public ExceptionStatement(String expression) {
		super(expression);
	}
	
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				if (evaluated != null) {
					String message = "A statement exception has been raised: ";
					Throwable t = new Throwable(evaluated.toString());
					EngineException ee = new EngineException(message,t);
					throw ee;
				}
				return true;
			}
		}
		return false;
	}

	public String toString() {
		return "throw Exception;";
	}	

}
