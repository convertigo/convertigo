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

import java.util.Vector;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class LoopStep extends BlockStep {

	private static final long serialVersionUID = 7593754675505081925L;

	transient public int loop = 1;
	
	transient private Vector<String> executedLoops = new Vector<String>();
	
	public LoopStep() {
		super();
	}

	public LoopStep(String condition) {
		super(condition);
	}

	@Override
    public LoopStep clone() throws CloneNotSupportedException {
    	LoopStep clonedObject = (LoopStep) super.clone();
    	clonedObject.loop = 1;
    	clonedObject.executedLoops = new Vector<String>();
        return clonedObject;
    }

	@Override
	public LoopStep copy() throws CloneNotSupportedException {
		LoopStep copiedObject = (LoopStep)super.copy();
		return copiedObject;
	}

	@Override
	protected void cleanCopy() {
		for (int i=0; i<executedLoops.size(); i++) {
			String timeID = executedLoops.elementAt(i);
			sequence.removeCopy(timeID, new Long(priority));
		}
		executedLoops.removeAllElements();
		super.cleanCopy();
	}

	@Override
	protected StepSource getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean workOnSource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String getExecuteTimeID() {
		String timeID = super.getExecuteTimeID() + loopSeparator + loop;
		executedLoops.addElement(timeID);
		return timeID;
	}

	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.executeNextStep(javascriptContext, scope)) {
				doLoop(javascriptContext, scope);
				return true;
			}
		}
		return false;
	}
	
	protected void doLoop(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (nbAsyncThreadRunning == 0) {
				cleanChildren();
			}
			Engine.logBeans.debug("Step "+ name + " ("+executeTimeID+") : loop "+ loop +" done");
			loop++;
			currentChildStep = 0;
		}
	}

	@Override
	protected void stepDone() {
		super.stepDone();
	}
	
	@Override
	protected void reset() throws EngineException {
		super.reset();
	}
}
