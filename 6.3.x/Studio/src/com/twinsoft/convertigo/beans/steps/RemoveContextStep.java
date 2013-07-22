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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.steps;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class RemoveContextStep extends Step {

	private static final long serialVersionUID = 8704433301402437269L;

	private String contextName = "";
	
	public RemoveContextStep() {
		super();
	}

	@Override
	public RemoveContextStep clone() throws CloneNotSupportedException {
		RemoveContextStep clonedObject = (RemoveContextStep) super.clone();
		return clonedObject;
	}

	@Override
	public RemoveContextStep copy() throws CloneNotSupportedException {
		RemoveContextStep copiedObject = (RemoveContextStep) super.copy();
		return copiedObject;
	}

	/**
	 * @return the contextName
	 */
	public String getContextName() {
		return contextName;
	}

	/**
	 * @param contextName the context name to set
	 */
	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					evaluate(javascriptContext, scope, getContextName(), "contextName", true);
					if (evaluated instanceof org.mozilla.javascript.Undefined) {
						throw new Exception("Step "+ getName() +" has none context name defined." );
					}
					
					if (evaluated != null) {
						// contextID = JSESSIONID_contexName
						String contextID = sequence.getSessionId() + "_" + evaluated.toString();
						if (contextID.equals(sequence.context.contextID)) {
							throw new Exception("The removal of current sequence's context is forbidden.");
						}
						
	    				Engine.logBeans.debug("(RemoveContextStep) Removing context \""+ contextID +"\"");
	    				Engine.theApp.contextManager.remove(contextID);
	    				return true;
					}
						
				}
				catch (Exception e) {
					evaluated = null;
					Engine.logBeans.warn(e.getMessage());
				}
			}
		}
		return false;
	}

	@Override
	protected StepSource getSource() {
		return null;
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		return "removeContext("+ (contextName.equals("")?"??":contextName) +")" + (!text.equals("") ? " // "+text:"");
	}

	@Override
	public String toJsString() {
		return "";
	}

	@Override
	protected boolean workOnSource() {
		return false;
	}

}
