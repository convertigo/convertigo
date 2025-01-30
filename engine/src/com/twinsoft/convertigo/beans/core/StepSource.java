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

package com.twinsoft.convertigo.beans.core;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.steps.IteratorStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class StepSource {

	private Step owner = null;
	private XMLVector<String> definition = null;
	
	private Step sourceStep = null;
	private int sourceLoop = 1;
	
	public StepSource(Step owner, XMLVector<String> definition) {
		this.owner = owner;
		this.definition = definition;
	}
	
	public boolean isEmpty() {
		return definition.isEmpty();
	}
	public XMLVector<String> getDefinition() {
		return definition;
	}
	
	public String getPriority() {
		if (definition.size() > 0)
			return definition.get(0);
		return null;
	}
	
	public void setPriority(String priority) {
		if (definition.size() > 0)
			definition.set(0, priority);
	}

	public String getXpath() {
		if (definition.size() > 1)
			return definition.get(1);
		return null;
	}
	
	public void setXpath(String xpath) {
		if (definition.size() > 0)
			definition.set(1, xpath);
	}
	
	public void updateTargetStep(Step step, String oldPriority) {
		if ((step != null) && (oldPriority != null)) {
			String priority = getPriority();
			if ((priority != null) && (priority.equals(oldPriority))) {
				setPriority(String.valueOf(step.priority));
				owner.hasChanged = true;
			}
		}
	}
	
	public int getLoop() throws EngineException {
		if (sourceStep == null)
			getStep();
		return sourceLoop;
	}
	
	public Step getStep() throws EngineException {
		if (definition.size() > 0) {
			if (sourceStep == null) {
				Long key = Long.valueOf(getPriority());
				Step step = null;
				String sourceExecuteTimeID = null;
				if (owner.executedSteps != null)
					sourceExecuteTimeID = (String)owner.executedSteps.get(key);
				if (sourceExecuteTimeID != null)
					step = (Step)owner.getSequence().getCopy(sourceExecuteTimeID);
				
				if (step == null) {
					if (sourceExecuteTimeID != null) {
						if (Engine.logBeans.isInfoEnabled())
							Engine.logBeans.warn("Did not find source copy ("+getPriority()+") for step \""+ owner.getName()+"\". Retrieving original source.", null);
					}
					step = (Step)owner.getSequence().loadedSteps.get(key);
					if (step == null)
						throw new SourceNotFoundException("Did not find source ("+getPriority()+") for step \""+ owner.getName()+"\"");
				}
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(ISourceContainer) "+ owner+" ["+owner.hashCode()+"] using source "+step+" ["+step.hashCode()+"]");
				
				if (sourceExecuteTimeID != null) {
					int index = sourceExecuteTimeID.lastIndexOf(Step.loopSeparator);  
					if (index != -1)
						sourceLoop = Integer.parseInt(sourceExecuteTimeID.substring(index+Step.loopSeparator.length()),10);
				}
				//return step;
				sourceStep = step;
			}
			return sourceStep;
		}
		return null;
	}
	 
	public String getAnchor() throws EngineException {
		if (definition.size() > 0) {
			String anchor = getStep().getAnchor();
			anchor += getXpath().substring(1);
			return anchor;
		}
		return "//document";
	}
	
	public boolean isXml() throws EngineException {
		if (definition.size() > 0)
			return getStep().isXml();
		return false;
	}
	
	public NodeList getContextValues() throws EngineException {
		NodeList nodeList = null;
		if (definition.size() > 0) {
			Step step = getStep();
			String xpath = getXpath();
			if (Engine.logBeans.isTraceEnabled()) {
				Engine.logBeans.trace("(ISourceContainer) "+ owner+" ["+owner.executeTimeID+"] retreiving value from source "+step+" ["+step.executeTimeID+"]");
				Engine.logBeans.trace("(ISourceContainer) "+ owner+" ["+owner.hashCode()+"] retreiving value from source "+step+" ["+step.hashCode()+"]");
			}
			nodeList = step.getContextValues(xpath, sourceLoop);
		}
		return nodeList;
	}
	
	public NodeList getContextOutputNodes() throws EngineException {
		NodeList list = getContextValues();
		return list;
	}

	
	public Node getContextNode() throws EngineException {
		if (definition.size() > 0)
			return getStep().getContextNode(getXpath(), sourceLoop);
		return null;
	}
	
	public boolean inError() throws EngineException {
		if (definition.size() > 0) {
			try {
				return getStep().inError();
			} catch (SourceNotFoundException e) {
				if (Engine.logBeans.isDebugEnabled())
					Engine.logBeans.debug("StepSource inError because a SourceNotFoundException : " + e.getMessage());
				return true;
			}
		}
		return false;
	}
	
	public String getLabel() throws EngineException {
		String label = "";
		if (definition.size() > 0) {
			Step step = (Step)owner.getParentSequence().loadedSteps.get(Long.valueOf(getPriority()));
			if (step != null) {
				label = step.getContextXpath(getXpath());
				if (label.equals(".")) {
					if (step.isXml()) {
						label = step.getStepNodeName();
						if (step instanceof XMLCopyStep)
							label = step.toString();
					}
					else {
						label = step.getName();
						if (step instanceof IteratorStep) {
							label = step.getLabel().substring(step.getLabel().lastIndexOf("/")+1);
						}
					}
				}
			}
			else label = "! broken source !";
		}
		if (label.startsWith(".//")) label = label.substring(3);
		else if (label.startsWith("./")) label = label.substring(2);
		else if (label.startsWith(".")) label = label.substring(1);
		return label;
	}

	public boolean isBroken() {
		return owner.getParentSequence().loadedSteps.get(Long.valueOf(getPriority())) == null;
	}
}