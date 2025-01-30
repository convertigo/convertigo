/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import javax.xml.transform.TransformerException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaParticle;
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
import com.twinsoft.convertigo.engine.enums.SchemaMeta;

public class IteratorStep extends LoopStep implements IStepSourceContainer {

	private static final long serialVersionUID = -5108986745479990736L;
	
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private String startIndex = "0";
	
	private transient Iterator iterator = null;
	private transient Long iStart = null;
	private transient Long iStop = null;
	private transient boolean needToEvaluateStart = true;
	private transient boolean needToEvaluateStop = true;
	
	public IteratorStep() {
		super();
	}

	@Override
    public IteratorStep clone() throws CloneNotSupportedException {
    	IteratorStep clonedObject = (IteratorStep) super.clone();
    	clonedObject.iterator = null;
    	clonedObject.needToEvaluateStart = needToEvaluateStart;
    	clonedObject.needToEvaluateStop = needToEvaluateStop;
    	clonedObject.iStart = iStart;
    	clonedObject.iStop = iStop;
        return clonedObject;
    }

	@Override
	public IteratorStep copy() throws CloneNotSupportedException {
		IteratorStep copiedObject = (IteratorStep) super.copy();
		return copiedObject;
	}
	
	@Override
	public boolean isPickable() {
		return isEnabled();
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":" @(??)";
		} catch (EngineException e) {}
		return getName() + label;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}

	public String getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(String startIndex) {
		this.startIndex = startIndex;
		if (isOriginal()) {
			this.iStart = stringToLong(startIndex);
			this.needToEvaluateStart = this.iStart == null;
		}
	}

	@Override
	public String getCondition() {
		return super.getCondition();
	}

	@Override
	public void setCondition(String condition) {
		super.setCondition(condition);
		if (isOriginal()) {
			this.iStop = stringToLong(condition);
			this.needToEvaluateStop = this.iStop == null;
		}
	}

	@Override
	public Node getContextNode(int loop) {
		Engine.logBeans.trace("(IteratorStep) Retrieve context node for loop :"+ loop);
		return iterator.getNode(loop);
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			try {
				iterator.init();
			} catch (TransformerException e) {
				throw new EngineException("Unable to initialize iterator",e);
			}
			
			if (inError()) {
				Engine.logBeans.warn("(IteratorStep) Skipping step "+ this +" ("+ hashCode()+") because its source is in error");
				return true;
			}
			
			long start = getLoopStartIndex(javascriptContext, scope);
			for (int i=0; i < iterator.size(); i++) {
				if (bContinue && sequence.isRunning()) {
					int index = iterator.numberOfIterations();
					Scriptable jsIndex = org.mozilla.javascript.Context.toObject(index, scope);
					scope.put("index", scope, jsIndex);
					
					Object item = iterator.nextElement();
					Scriptable jsItem = org.mozilla.javascript.Context.toObject(item, scope);
					scope.put("item", scope, jsItem);
					
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
	
	private Long getLoopStartIndex(Context javascriptContext, Scriptable scope) throws EngineException {
		if (iStart == null || needToEvaluateStart) {
			iStart = evaluateToLong(javascriptContext, scope, getStartIndex(), "startIndex", true);
		}
		return iStart;
	}

	private Long getLoopStopIndex(Context javascriptContext, Scriptable scope) throws EngineException {
		if (iStop == null || needToEvaluateStop) {
			iStop = evaluateToLong(javascriptContext, scope, getCondition(), "condition", true);
		}
		return iStop;
	}
	
	@Override
	protected void doLoop(Context javascriptContext, Scriptable scope) throws EngineException {
		super.doLoop(javascriptContext, scope);
		if (iterator.hasMoreElements()) {
			long stop = getLoopStopIndex(javascriptContext, scope);
			if (!((stop == -1) || (iterator.numberOfIterations() < stop))) {
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

	@Override
	public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaParticle particle = super.getXmlSchemaObject(collection, schema);
		long max = Long.MAX_VALUE;
		try {
			max = Integer.parseInt(getCondition());
			if (max < 0) {
				max = 0;
			}
			XmlSchemaGroupBase group = SchemaMeta.getContainerXmlSchemaGroupBase(particle);
			group.setMaxOccurs(max);
		} catch (Exception e) { }
		return particle;
	}
	
	@Override protected StepSource getSource() {
		return super.getSource();
	}
	
	class Iterator {
		private Step step = null;
		private int stepLoop = 1;
		private NodeList list = null;
		private int index = 0;
		private String xpath = null;
		
		public Iterator() throws EngineException {
			StepSource stepSource = getSource();
			step = stepSource.getStep();
			stepLoop = stepSource.getLoop();
			xpath = stepSource.getXpath();
		}
		
		private void init() throws TransformerException, EngineException {
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
			} catch (Exception e) {
				e.printStackTrace();
				reset();
			}
			
			return false;
		}

		private int size() {
			try {
				init();
				if (list != null) {
					return list.getLength();
				}
			} catch (Exception e) {
				e.printStackTrace();
				reset();
			}
			
			return 0;
		}
		
		private Object nextElement() {
			try {
				init();
				if (list != null) {
					return list.item(index++);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		private Node getNode(int loop) {
			try {
				init();
				if (list != null) {
					return list.item(loop-1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		private void reset() {
			list = null;
			index = 0;
		}
	}
}
