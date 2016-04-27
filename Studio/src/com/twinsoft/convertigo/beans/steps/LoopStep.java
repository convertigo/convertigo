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

import java.util.ArrayList;
import java.util.List;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class LoopStep extends BlockStep {

	private static final long serialVersionUID = 7593754675505081925L;

	transient public int loop = 1;
	
	transient private List<String> executedLoops = new ArrayList<String>();
	
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
    	clonedObject.executedLoops = new ArrayList<String>();
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
			String timeID = executedLoops.get(i);
			sequence.removeCopy(timeID, new Long(priority));
		}
		executedLoops.clear();
		super.cleanCopy();
	}

	@Override
	protected String getExecuteTimeID() {
		String timeID = super.getExecuteTimeID() + loopSeparator + loop;
		executedLoops.add(timeID);
		return timeID;
	}

	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.executeNextStep(javascriptContext, scope)) {
				doLoop(javascriptContext, scope);
				return true;
			}
		}
		return false;
	}
	
	protected void doLoop(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (nbAsyncThreadRunning == 0) {
				cleanChildren();
			}
			Engine.logBeans.debug("Step "+ getName() + " ("+executeTimeID+") : loop "+ loop +" done");
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
	
	@Override
	public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaParticle particle = super.getXmlSchemaObject(collection, schema);
		particle.setMaxOccurs(Long.MAX_VALUE);
		return particle;
	}
}
