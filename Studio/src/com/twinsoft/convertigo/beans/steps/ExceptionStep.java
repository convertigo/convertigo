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

package com.twinsoft.convertigo.beans.steps;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * @author nathalieh
 *
 */
public class ExceptionStep extends Step {

	private static final long serialVersionUID = 7505194897465946697L;

	private String message = "\"humanly readable message\"";
	private String details = "\"details\"";
	
	public ExceptionStep() {
		super();
	}

	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, getMessage(), "message", true);
				Object evMessage = evaluated;
				
				if (evMessage instanceof org.mozilla.javascript.Undefined) {
					throw new EngineException("Please fill the \"Message\" property field with a humanly readable message.");
				}
				
				evaluate(javascriptContext, scope, getDetails(), "details", true);
				Object evDetails = evaluated;
				
				StepException stepException = new StepException(evMessage.toString(), evDetails.toString());
				throw stepException;
			}
		}
		return false;
	}
	
	public String toString() {
		String text = this.getComment();
		return "throw Exception; "+ (!text.equals("") ? " // "+text:"");
	}

	@Override
	protected boolean workOnSource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected StepSource getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toJsString() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}	

}
