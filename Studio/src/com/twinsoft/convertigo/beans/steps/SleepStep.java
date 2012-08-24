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
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class SleepStep extends Step {

	private static final long serialVersionUID = 2373281545608880428L;

	private long delay = 500;
	
	public SleepStep() {
		super();
	}
	
	
	@Override
	public SleepStep clone() throws CloneNotSupportedException {
		SleepStep clonedObject = (SleepStep)super.clone();
		return clonedObject;
	}


	@Override
	public SleepStep copy() throws CloneNotSupportedException {
		SleepStep copiedObject = (SleepStep)super.copy();
		return copiedObject;
	}


	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					long t = (getSequence().getResponseTimeout() * 1000) - 500;
					long l = (delay<=0) ? 0:(delay>t ? t:delay);
					Engine.logBeans.debug("Step "+ getName() + " ("+executeTimeID+") sleeping for "+l+"ms ...");
					Thread.sleep(l);
					Engine.logBeans.debug("Step "+ getName() + " ("+executeTimeID+") ends sleep");
				} catch (InterruptedException e) {
					Engine.logBeans.debug("Step "+ getName() + " ("+executeTimeID+") has been interrupted");
				}
			}
			return true;
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
		return "Sleep("+delay+")" + (!text.equals("") ? " // "+text:"");
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
