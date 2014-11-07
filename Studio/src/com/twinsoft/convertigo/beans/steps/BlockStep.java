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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public abstract class BlockStep extends StepWithExpressions {

	private static final long serialVersionUID = -3632998744076137441L;
	private transient ThenStep thenStep = null;
	private transient ElseStep elseStep = null;
		
	private String condition = "";
	
	public BlockStep() {
		super();
	}

	public BlockStep(String condition) {
		super();
		this.condition = condition;
	}

	public BlockStep(boolean isLoop) {
		super();
	}

	public BlockStep(String condition, boolean isLoop) {
		super();
		this.condition = condition;
	}

	@Override
    public BlockStep clone() throws CloneNotSupportedException {
    	BlockStep clonedObject = (BlockStep) super.clone();
    	clonedObject.thenStep = null;
    	clonedObject.elseStep = null;
        return clonedObject;
    }

	@Override
	public BlockStep copy() throws CloneNotSupportedException {
		BlockStep copiedObject = (BlockStep)super.copy();
    	copiedObject.thenStep = thenStep;
    	copiedObject.elseStep = elseStep;
		return copiedObject;
	}

	@Override
    protected void cleanCopy() {
    	super.cleanCopy();
    	thenStep = null;
    	elseStep = null;
    }
    
	/**
	 * @return Returns the condition.
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * @param condition The condition to set.
	 */
	public void setCondition(String condition) {
		this.condition = condition;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		return getName() + (!text.equals("") ? " // "+text:"");
	}

	public ThenStep getThenStep() {
		return thenStep;
	}

	public ElseStep getElseStep() {
		return elseStep;
	}

	public boolean hasThenElseSteps() {
		return false;
	}

	@Override
	public void addStep(Step step) throws EngineException {
		checkSubLoaded();
		
		if (hasThenElseSteps()) {
			if ((!(step instanceof ThenStep)) && (!(step instanceof ElseStep))) {
				throw new EngineException("You cannot add to this step a database object of type " + step.getClass().getName());
			}
			
			if ((thenStep == null) || (elseStep == null)) {
				if ((step instanceof ThenStep)) {
					if (thenStep == null) {
						super.addStep(step);
						thenStep = (ThenStep)step;
					}
					else
						throw new EngineException("You cannot add to this step another database object of type " + step.getClass().getName());
				}
				else if ((step instanceof ElseStep)) {
					if (elseStep == null) {
						super.addStep(step);
						elseStep = (ElseStep)step;
					}
					else
						throw new EngineException("You cannot add to this step another database object of type " + step.getClass().getName());
				}
			}
			else {
				throw new EngineException("You cannot add to this step another database object of type " + step.getClass().getName());
			}
		}
		else {
			super.addStep(step);
		}
	}

	@Override
	public void removeStep(Step step) {
		checkSubLoaded();
		
		super.removeStep(step);
		if (hasThenElseSteps()) {
			if (step.equals(thenStep)) {
				thenStep = null;
			}
			else if (step.equals(elseStep)) {
				elseStep = null;
			}
		}
	}

	protected boolean hasToEvaluateBeforeNextStep() throws EngineException {
		return false;
	}

	protected boolean hasToEvaluateAfterNextStep() throws EngineException {
		return false;
	}

	protected boolean evaluateStep(Context javascriptContext, Scriptable scope) throws EngineException {
		evaluate(javascriptContext, scope, condition, "condition", true);
		if (evaluated instanceof Boolean) {
			return evaluated.equals(Boolean.TRUE);
		}
		else {
			EngineException ee = new EngineException(
					"Invalid step condition.\n" +
					"Step: \"" + getName()+ "\"");
			throw ee;
		}
	}

	@Override
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (hasToEvaluateBeforeNextStep()) {// e.g. While case
				if (evaluateStep(javascriptContext, scope)) {
					return super.executeNextStep(javascriptContext, scope);
				}
				return true;
			}
			else if (hasToEvaluateAfterNextStep()) {// e.g. DoWhile case
				if (super.executeNextStep(javascriptContext, scope)) {
					return evaluateStep(javascriptContext, scope);
				}
				return true;
			}
			else {// other case
				return super.executeNextStep(javascriptContext, scope);
			}
		}
		return false;
	}
	
	protected boolean executeNextStep(boolean isTrue, Context javascriptContext, Scriptable scope) throws EngineException
    {
    	if (isEnable()) {
    		if (hasThenElseSteps()) {
    	    	if (hasSteps()) {
        			if (bContinue && sequence.isRunning()) {
        				if (isTrue) {
            				ThenStep thenStep = getThenStep();
            				if (thenStep != null)
            					super.executeNextStep(thenStep, javascriptContext, scope);
        				}
        				else {
            				ElseStep elseStep = getElseStep();
            				if (elseStep != null)
            					super.executeNextStep(elseStep, javascriptContext, scope);
        				}
        			}
    	    	}
    	    	return true;
    		}
    		else {
    			if (isTrue) {
    				return super.executeNextStep(javascriptContext, scope);
    			}
    			return true;
    		}
    	}
    	return false;
    }

	@Override
	protected void stepDone() {
		super.stepDone();
	}

	@Override
	protected void reset() throws EngineException {
		super.reset();
	}
	
	public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence()); 
		sequence.setMinOccurs(0);
		XmlSchemaParticle particle = getXmlSchemaParticle(collection, schema, sequence);
		return particle;
	}
	
	@Override
	public boolean isGenerateSchema() {
		return true;
	}
}
