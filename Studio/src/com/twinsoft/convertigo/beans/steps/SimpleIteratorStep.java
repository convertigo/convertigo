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
 * $HeadURL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;

import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class SimpleIteratorStep extends LoopStep {

	private static final long serialVersionUID = -9093650239163532022L;

	protected String expression = "";
	protected String startIndex = "0";
	
	private transient Iterator iterator = null;
	private transient Integer iterations = null;
	
	public SimpleIteratorStep() {
		super();
	}

	@Override
    public SimpleIteratorStep clone() throws CloneNotSupportedException {
    	SimpleIteratorStep clonedObject = (SimpleIteratorStep) super.clone();
    	clonedObject.iterator = null;
    	clonedObject.iterations = null;
        return clonedObject;
    }

	@Override
	public SimpleIteratorStep copy() throws CloneNotSupportedException {
		SimpleIteratorStep copiedObject = (SimpleIteratorStep)super.copy();
		return copiedObject;
	}
    
	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	@Override
	protected String getSpecificLabel() throws EngineException {
		return expression.equals("")? "@??":"@"+expression;
	}

	public String getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(String startIndex) {
		this.startIndex = startIndex;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += " " + getLabel();
		} catch (EngineException e) {}
		return name + label + (!text.equals("") ? " // "+text:"");
	}

	@Override
	protected boolean workOnSource() {
		return false;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			try {
				evaluate(javascriptContext, scope, getExpression(), "expression", true);
				if (evaluated instanceof org.mozilla.javascript.Undefined) {
					throw new Exception("Step "+ name +" has none expression defined." );
				}
			}
			catch (Exception e) {
				evaluated = null;
				Engine.logBeans.warn(e.getMessage());
			}
			
			try {
				iterator.init();
			} catch (Exception e) {
				throw new EngineException("Unable to initialize iterator",e);
			}
			
			for (int i=0; i < iterator.size(); i++) {
				if (inError()) {
					Engine.logBeans.warn("(SimpleIteratorStep) Skipping step "+ this +" ("+ hashCode()+") because its source is in error");
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
		private List<Object> list = null;
		private int index = 0;
		
		public Iterator() {
		}
		
		private void init() {
			if ((evaluated != null) && (list == null)) {
				if (evaluated instanceof NodeList) {
					list = new ArrayList<Object>();
					NodeList nodeList = (NodeList)evaluated;
					for (int i=0; i<nodeList.getLength(); i++)
						list.add(nodeList.item(i));
				}
				else if (evaluated instanceof Collection<?>) {
					list = new ArrayList<Object>((Collection<?>) evaluated);
				}
				else if (evaluated instanceof NativeJavaArray) {
					Object object = ((NativeJavaArray)evaluated).unwrap();
					list = Arrays.asList((Object[])object);
				}
				else if (evaluated instanceof NativeArray) {
					list = new ArrayList<Object>();
					NativeArray array = (NativeArray)evaluated;
					for (int i=0; i<array.getLength(); i++)
						list.add(array.get(i,array));
				}
//				else if (evaluated instanceof NativeJavaObject) {
//					list = Arrays.asList(new String[] {(String) ((NativeJavaObject)evaluated).getDefaultValue(String.class)});
//				}
				else if (evaluated.getClass().isArray()) {
					list = Arrays.asList((Object[])evaluated);
				}
				else
					list = Arrays.asList(new Object[] {evaluated.toString()});
			}
		}
		
		private int numberOfIterations() {
			return index;
		}
		
		private boolean hasMoreElements() {
			try {
				init();
				if (list != null) {
					return (list.size() - index > 0);
				}
			} catch (Exception e) {reset();}
			
			return false;
		}

		private int size() {
			try {
				init();
				if (list != null) {
					return list.size();
				}
			} catch (Exception e) {reset();}
			
			return 0;
		}
		
		private Object nextElement() {
			try {
				init();
				if (list != null) {
					return list.get(index++);
				}
			} catch (Exception e) {}
			
			return null;
		}
		
		@SuppressWarnings("unused")
		private Object getItem(int loop) {
			try {
				init();
				if (list != null) {
					return list.get(loop-1);
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
