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

package com.twinsoft.convertigo.beans.steps;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.HttpUtils;

public class RemoveSessionStep extends Step {

	private static final long serialVersionUID = 8705533301402437269L;
	
	public RemoveSessionStep() {
		super();
	}

	@Override
	public RemoveSessionStep clone() throws CloneNotSupportedException {
		RemoveSessionStep clonedObject = (RemoveSessionStep) super.clone();
		return clonedObject;
	}

	@Override
	public RemoveSessionStep copy() throws CloneNotSupportedException {
		RemoveSessionStep copiedObject = (RemoveSessionStep) super.copy();
		return copiedObject;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.stepExecute(javascriptContext, scope) && Engine.isEngineMode()) {
				try {
					if (sequence.context != null && sequence.context.httpSession != null) {
						HttpUtils.terminateSession(sequence.context.httpSession);
						sequence.context.requireEndOfContext = true;
					} else {
						Engine.logBeans.warn("(RemoveSessionStep) null httpSession, cannot be removed");
					}
				} catch (Exception e) {
					Engine.logBeans.warn("(RemoveSessionStep) failure on remove: " + e.getMessage());
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "removeSession";
	}

	@Override
	public String toJsString() {
		return "";
	}
}
