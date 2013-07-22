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

import javax.xml.transform.TransformerException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class IteratorStep extends LoopStep implements IStepSourceContainer {

	private static final long serialVersionUID = -5108986745479990736L;
	
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private String startIndex = "0";
	
	private transient Iterator iterator = null;
	private transient StepSource source = null;
	private transient Integer iterations = null;
	
	public IteratorStep() {
		super();
	}

	@Override
    public IteratorStep clone() throws CloneNotSupportedException {
    	IteratorStep clonedObject = (IteratorStep) super.clone();
    	clonedObject.iterator = null;
    	clonedObject.source = null;
    	clonedObject.iterations = null;
        return clonedObject;
    }

	@Override
	public IteratorStep copy() throws CloneNotSupportedException {
		IteratorStep copiedObject = (IteratorStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":" @(??)";
		} catch (EngineException e) {}
		return getName() + label + (!text.equals("") ? " // "+text:"");
	}

	@Override
	protected boolean workOnSource() {
		return true;
	}

	@Override
	protected StepSource getSource() {
		if (source == null) source = new StepSource(this,sourceDefinition);
		return source;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
		source = new StepSource(this,sourceDefinition);
	}

	public String getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(String startIndex) {
		this.startIndex = startIndex;
	}

	@Override
	public Node getContextNode(int loop) {
		Engine.logBeans.trace("(IteratorStep) Retrieve context node for loop :"+ loop);
		return iterator.getNode(loop);
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			try {
				iterator.init();
			} catch (TransformerException e) {
				throw new EngineException("Unable to initialize iterator",e);
			}
			
			for (int i=0; i < iterator.size(); i++) {
				if (inError()) {
					Engine.logBeans.warn("(IteratorStep) Skipping step "+ this +" ("+ hashCode()+") because its source is in error");
					return true;
				}
				if (bContinue && sequence.isRunning()) {
					int index = iterator.numberOfIterations();
					Scriptable jsIndex = org.mozilla.javascript.Context.toObject(index, scope);
					scope.put("index", scope, jsIndex);
					
					Object item = iterator.nextElement();
					Scriptable jsItem = org.mozilla.javascript.Context.toObject(item, scope);
					scope.put("item", scope, jsItem);
					
					int start = evaluateToInteger(javascriptContext, scope, getStartIndex(), "startIndex", true);
					start = start<0 ? 0:start;
					if (start > index) {
						doLoop(javascriptContext, scope);
						continue;
					}
					
					if (!super.stepExecute(javascriptContext, scope))
						break;
				}
				else break;
			}
			return true;
		}
		return false;
	}

	@Override
	protected void stepInit() throws EngineException {
		super.stepInit();
		initialize();
	}

	@Override
	protected void stepDone() {
		iterator.reset();
		super.stepDone();
	}
	
	private Integer evaluateMaxIterationsInteger(Context javascriptContext, Scriptable scope) throws EngineException {
		String condition = getCondition();
		if (iterations == null)
			iterations = evaluateToInteger(javascriptContext, scope, condition, "condition", true);
		return iterations;
	}
	
	@Override
	protected void doLoop(Context javascriptContext, Scriptable scope) throws EngineException {
		super.doLoop(javascriptContext, scope);
		if (iterator.hasMoreElements()) {
			int maxIterations = evaluateMaxIterationsInteger(javascriptContext, scope);
			if (!((maxIterations == -1) || (iterator.numberOfIterations() < maxIterations))) {
				bContinue = false;
			}
		}
		else {
			bContinue = false;
		}
	}
	
	private void initialize() throws EngineException {
		if (iterator == null) {
			iterator = new Iterator();
		}
	}
	
	class Iterator {
		private Step step = null;
		private int stepLoop = 1;
		private NodeList list = null;
		private int index = 0;
		
		public Iterator() throws EngineException {
			step = getSource().getStep();
			stepLoop = getSource().getLoop();
		}
		
		private void init() throws TransformerException, EngineException {
			String xpath = getSource().getXpath();
			if ((list == null) && (step != null) && (xpath != null)) {
				list = getXPathAPI().selectNodeList(step.getContextNode(stepLoop), step.getContextXpath(xpath));
			}
		}
		
		private int numberOfIterations() {
			return index;
		}
		
		private boolean hasMoreElements() {
			try {
				init();
				if (list != null) {
					return (list.getLength() - index > 0);
				}
			} catch (Exception e) {reset();}
			
			return false;
		}

		private int size() {
			try {
				init();
				if (list != null) {
					return list.getLength();
				}
			} catch (Exception e) {reset();}
			
			return 0;
		}
		
		private Object nextElement() {
			try {
				init();
				if (list != null) {
					return list.item(index++);
				}
			} catch (Exception e) {}
			
			return null;
		}
		
		private Node getNode(int loop) {
			try {
				init();
				if (list != null) {
					return list.item(loop-1);
				}
			} catch (Exception e) {}
			
			return null;
		}
		
		private void reset() {
			list = null;
			index = 0;
			iterations = null;
		}
	}
}
