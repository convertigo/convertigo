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

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.Engine;

public class ParallelStep extends BranchStep {

	private static final long serialVersionUID = 3787172105946604754L;

	private transient int totalAsyncThreadRunning = 0;
	
	public ParallelStep() {
		super(false);
	}

	@Override
    public ParallelStep clone() throws CloneNotSupportedException {
    	ParallelStep clonedObject = (ParallelStep) super.clone();
    	clonedObject.totalAsyncThreadRunning = 0;
        return clonedObject;
    }

	@Override
    public ParallelStep copy() throws CloneNotSupportedException {
    	ParallelStep copiedObject = (ParallelStep) super.copy();
        return copiedObject;
    }

	@Override
	public synchronized void increaseAsyncThreadRunning() {
		nbAsyncThreadRunning++;
		//System.out.println("Incr step "+ name + " ("+executeTimeID+") threads :" + nbAsyncThreadRunning);
		
		totalAsyncThreadRunning = sequence.setAsyncThreadRunningNumber(priority, true);
		
		if (parent instanceof Sequence)
			((Sequence)parent).increaseAsyncThreadRunning();
		else {
			if (parent instanceof StepWithExpressions)
				((StepWithExpressions)parent).increaseAsyncThreadRunning();
		}
		// Case this have to wait : this have more children than maxNumberOfThreads
		if (haveToWait == Boolean.FALSE) {
			//if (!maxNumberOfThreads.equals("") && (nbAsyncThreadRunning >= Integer.parseInt(maxNumberOfThreads,10))) {
			if ((maxNumberOfThreadsInteger > 0) && (nbAsyncThreadRunning >= maxNumberOfThreadsInteger)) {
				Engine.logBeans.debug("(ParallelStep) Max number of threads exceded for step '"+ name +"' ("+executeTimeID+"), waiting for available...");
				this.shouldWait(true);
			}
		}
		// Case parent have to wait : this have less children than maxNumberOfThreads
		if (haveToWait == Boolean.FALSE) {
			//if (!maxNumberOfThreads.equals("") && (totalAsyncThreadRunning >= Integer.parseInt(maxNumberOfThreads,10))) {
			if ((maxNumberOfThreadsInteger > 0) && (totalAsyncThreadRunning >= maxNumberOfThreadsInteger)) {
				Engine.logBeans.debug("(ParallelStep) Max number of threads exceded for step '"+ name +"' ("+executeTimeID+"), waiting for available...");
				((StepWithExpressions)parent).shouldWait(true);
			}
		}
	}

	@Override
	public synchronized void decreaseAsyncThreadRunning() {
		if (nbAsyncThreadRunning > 0) nbAsyncThreadRunning--;
		//System.out.println("Decr step "+ name + " ("+executeTimeID+") threads : " + nbAsyncThreadRunning);

		totalAsyncThreadRunning = sequence.setAsyncThreadRunningNumber(priority, false);
		
		if (parent instanceof Sequence)
			((Sequence)parent).decreaseAsyncThreadRunning();
		else {
			if (parent instanceof StepWithExpressions)
				((StepWithExpressions)parent).decreaseAsyncThreadRunning();
		}
		
		// Case this is waiting
		if (haveToWait == Boolean.TRUE) {
			//if (!maxNumberOfThreads.equals("") && (nbAsyncThreadRunning < Integer.parseInt(maxNumberOfThreads,10))) {
			if ((maxNumberOfThreadsInteger > 0) && (nbAsyncThreadRunning < maxNumberOfThreadsInteger)) {
				Engine.logBeans.debug("(ParallelStep) New thread available for step '"+ name +"' ("+executeTimeID+")");
				this.shouldWait(false);
			}
		}
		// Case parent is waiting
		else {
			//if (!maxNumberOfThreads.equals("") && (totalAsyncThreadRunning < Integer.parseInt(maxNumberOfThreads,10))) {
			if ((maxNumberOfThreadsInteger > 0) && (totalAsyncThreadRunning < maxNumberOfThreadsInteger)) {
				Engine.logBeans.debug("(ParallelStep) New thread available for step '"+ name +"' ("+executeTimeID+")");
				((StepWithExpressions)parent).shouldWait(false);
			}
		}
	}
}
