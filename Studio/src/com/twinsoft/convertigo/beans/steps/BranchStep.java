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
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class BranchStep extends StepWithExpressions {

	private static final long serialVersionUID = 2146267225465203569L;

	protected boolean synchronous = true;
	
	protected String maxNumberOfThreads = "";
	protected transient Integer maxNumberOfThreadsInteger = -1;

	public BranchStep() {
		super();
	}

	public BranchStep(boolean synchronous) {
		super();
		this.synchronous = synchronous;
	}

    @Override
    public BranchStep clone() throws CloneNotSupportedException {
    	BranchStep clonedObject = (BranchStep) super.clone();
    	clonedObject.maxNumberOfThreadsInteger = -1;
        return clonedObject;
    }

    @Override
    public BranchStep copy() throws CloneNotSupportedException {
    	BranchStep copiedObject = (BranchStep) super.copy();
        return copiedObject;
    }

    @Override
    public boolean isSynchronous() {
		return synchronous;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}
	
	public String getMaxNumberOfThreads() {
		return maxNumberOfThreads;
	}

	public void setMaxNumberOfThreads(String maxNumberOfThreads) {
		this.maxNumberOfThreads = maxNumberOfThreads;
	}

    @Override
	public String toString() {
		String text = this.getComment();
		return getName() + (!text.equals("") ? " // "+text:"");
	}

	
	protected boolean workOnSource() {
		return false;
	}
	
	protected StepSource getSource() {
		return null;
	}
	
	private void evaluateMaxNumberOfThreads(Context javascriptContext, Scriptable scope) throws EngineException {
		maxNumberOfThreadsInteger = evaluateToInteger(javascriptContext, scope, maxNumberOfThreads, "maxNumberOfThreads", true);
	}

    @Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException
    {
    	if (isEnable) {
	    	if (hasSteps()) {
	    		int num = numberOfSteps();
	    		for (int i=0; i < num; i++) {
	    			if (bContinue && sequence.isRunning()) {
		        		Step step = (Step) getSteps().get(i);
		        		// Case of Serial Step currently executing
		        		if (synchronous) {
		        			executeNextStep(step, javascriptContext, scope);
		        		}
		        		// Case of ParallelStep currently executing
		        		else {
		        			evaluateMaxNumberOfThreads(javascriptContext, scope);
		        			if ((maxNumberOfThreadsInteger == -1) || (maxNumberOfThreadsInteger > 0)) {
		        				invokeNextStep(step, javascriptContext, scope);
		        			}
		        		}
	    			}
	    			else break;
	    		}
	    	}
	    	return true;
    	}
    	return false;
    }

    @Override
	protected void stepDone() {
		super.stepDone();
	}
}
