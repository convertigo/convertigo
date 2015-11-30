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

package com.twinsoft.convertigo.beans.core;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.steps.BranchStep;
import com.twinsoft.convertigo.beans.steps.ParallelStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public abstract class StepWithExpressions extends Step implements IContextMaintainer, IContainerOrdered, ISchemaParticleGenerator {
	private static final long serialVersionUID = 6835033841635158551L;

	/**
     * The vector of ordered step objects which can be applied on the StepWithExpressions.
     */
    private XMLVector<XMLVector<Long>> orderedSteps = null;
	
    transient private List<Step> vSteps = new Vector<Step>();
    
    transient private List<Step> vAllSteps = null;
    
    transient private String transactionSessionId = null;
    
    transient protected Hashtable<String, Long> childrenSteps = null;
    
    transient protected int nbAsyncThreadRunning = 0;
    
    transient protected Boolean haveToWait = Boolean.FALSE;

    transient protected int currentChildStep;

    transient public boolean bContinue = true;
    
    transient public boolean handlePriorities = true;
    
    transient public long[] asyncCounters = null;
    
	public StepWithExpressions() {
		super();
		
		orderedSteps = new XMLVector<XMLVector<Long>>();
		orderedSteps.addElement(new XMLVector<Long>());
	}

	@Override
    public StepWithExpressions clone() throws CloneNotSupportedException {
    	StepWithExpressions clonedObject = (StepWithExpressions) super.clone();
    	clonedObject.nbAsyncThreadRunning = 0;
    	clonedObject.haveToWait = Boolean.FALSE;
    	clonedObject.currentChildStep = 0;
    	clonedObject.childrenSteps = null;
        clonedObject.vSteps = new Vector<Step>();
        clonedObject.vAllSteps = null;
        clonedObject.bContinue = true;
        clonedObject.handlePriorities = handlePriorities;
        clonedObject.transactionSessionId= null;
        clonedObject.asyncCounters = null;
        return clonedObject;
    }
	
    @Override
	public Object copy() throws CloneNotSupportedException {
		StepWithExpressions copiedObject = (StepWithExpressions)super.copy();
		copiedObject.childrenSteps = new Hashtable<String, Long>(10);
		copiedObject.vSteps = vSteps;
		return copiedObject;
	}
    
    public String getInheritedContextName() {
    	return null;
    }
    
	public String getContextName() {
		return "Container-"+executeTimeID;
	}
	
	public String getTransactionSessionId() {
		return transactionSessionId;
	}
	
	public void setTransactionSessionId(String sessionId) {
		if ((transactionSessionId == null) && (sessionId != null)) {
			transactionSessionId = sessionId;
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(StepWithExpression) setting transactionSessionId: "+ transactionSessionId);
		}
		else if (transactionSessionId != null) {
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(StepWithExpression) transactionSessionId/JSESSIONID: "+ transactionSessionId +"/"+sessionId);
		}
	}
	
	public void setTransactionSessionId(HttpState state) {
		if ((transactionSessionId == null) && (state != null)) {
			if (state != null) {
				Cookie[] httpCookies = state.getCookies();
				int len = httpCookies.length;
				Cookie cookie = null;
				for (int i=0; i<len; i++) {
					cookie = httpCookies[i];
					if (cookie.getName().equalsIgnoreCase("JSESSIONID")) {
						transactionSessionId = cookie.getValue();
						if (Engine.logBeans.isTraceEnabled())
							Engine.logBeans.trace("(StepWithExpression) setting transactionSessionId: "+ transactionSessionId);
						break;
					}
				}
			}
		}
		else if (transactionSessionId != null) {
			if (Engine.logBeans.isTraceEnabled()) {
				if (state != null) {
					Cookie[] httpCookies = state.getCookies();
					int len = httpCookies.length;
					Cookie cookie = null;
					for (int i=0; i<len; i++) {
						cookie = httpCookies[i];
						if (cookie.getName().equalsIgnoreCase("JSESSIONID")) {
							Engine.logBeans.trace("(StepWithExpression) transactionSessionId/JSESSIONID: "+ transactionSessionId +"/"+cookie.getValue());
							break;
						}
					}
				}
			}
		}
	}
	
	protected void cleanChildren() {
		if (childrenSteps != null) {
			//Enumeration e = childrenSteps.elements();
			Enumeration<String> e = childrenSteps.keys();
			while (e.hasMoreElements()) {
				String timeID = (String)e.nextElement();
				if (timeID != null) {
					Long stepPriority = null;
					Step step = sequence.getCopy(timeID);
					if (step != null) {
						stepPriority = new Long(step.priority);
						step.cleanCopy();
					}
					sequence.removeCopy(timeID, stepPriority);
				}
			}
			childrenSteps.clear();
		}
	}
	
	@Override
	protected void cleanCopy() {
		//System.out.println("Start Clean copy of step " + name + "("+executeTimeID+")");
		cleanChildren();
		if (childrenSteps != null) {
			childrenSteps.clear();
			childrenSteps = null;
		}
		super.cleanCopy();
		vSteps = null; // ! Do not clear()!
		if (vAllSteps != null) {
			vAllSteps.clear();
			vAllSteps = null;
		}
		//System.out.println("End Clean copy of step " + name + "("+executeTimeID+")");
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Step) {
        	addStep((Step) databaseObject);
        }
        else {
        	super.add(databaseObject);
        }
    }
	
	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Step) {
        	removeStep((Step) databaseObject);
        }
        else {
        	super.remove(databaseObject);
        }
	}

	public void addStep(Step step) throws EngineException {
		checkSubLoaded();
		
		String newDatabaseObjectName = getChildBeanName(vSteps, step.getName(), step.bNew);
		step.setName(newDatabaseObjectName);
        
        vSteps.add(step);
        step.setParent(this);// do not call super.add otherwise it will generate an exception
        step.sequence = getSequence();
        
        sequence.loadedSteps.put(new Long(step.priority), step);
        sequence.addStepListener(step);
        
       	insertOrderedStep(step,null);
    }

    public void insertOrderedStep(Step step, Long after) {
    	XMLVector<Long> ordered = orderedSteps.elementAt(0);
    	int size = ordered.size();
    	
    	Long value = new Long(step.priority);
    	
    	if (ordered.contains(value))
    		return;
    	
    	if (after == null) {
    		after = new Long(0);
    		if (size>0)
    			after = (Long)ordered.lastElement();
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.insertElementAt(value, order+1);
    	hasChanged = true;
    }
	
    public void removeStep(Step step) {
    	checkSubLoaded();
    	
    	vSteps.remove(step);
    	step.setParent(null); // Do not call super.remove otherwise it will generate an exception
    	step.sequence = null;
    	
    	Long value = new Long(step.priority);
        removeOrderedStep(value);
        
        sequence.loadedSteps.remove(new Long(step.priority));
        sequence.removeStepListener(step);
    }
    
    public void removeOrderedStep(Long value) {
    	XMLVector<Long> ordered = orderedSteps.elementAt(0);
        ordered.removeElement(value);
        hasChanged = true;
    }
    
	public List<Step> getSteps(boolean reset) {
    	if (reset)
    		vAllSteps = null;
    	return getSteps();
    }
    
    public List<Step> getSteps() {
    	checkSubLoaded();
    	
    	if ((vAllSteps == null) || hasChanged)
    		vAllSteps = getAllSteps();
    	return vAllSteps;
    }
    
    public List<Step> getAllSteps() {
    	checkSubLoaded();

        debugSteps();
    	return sort(vSteps);
    }
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
    @Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof Step) {
        	List<Long> ordered = orderedSteps.get(0);
        	long time = ((Step)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted step for StepWithExpressions \""+ getName() +"\". Step \""+ ((Step)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        } else return super.getOrder(object);
    }
    
    public boolean hasSteps()
    {
    	checkSubLoaded();
    	
    	return (vSteps.size()>0) ? true: false;
    }
    
    public int numberOfSteps()
    {
    	checkSubLoaded();
    	
    	return vSteps.size();
    }

    /**
	 * @return the orderedSteps
	 */
	public XMLVector<XMLVector<Long>> getOrderedSteps() {
		return orderedSteps;
	}

	/**
	 * @param orderedSteps the orderedSteps to set
	 */
	public void setOrderedSteps(XMLVector<XMLVector<Long>> orderedSteps) {
		this.orderedSteps = orderedSteps;
	}

	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Step)
			increaseOrder(databaseObject,null);
	}
	
	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Step)
			decreaseOrder(databaseObject,null);
	}
	
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, new Long(priority));
	}
	
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	XMLVector<Long> ordered = orderedSteps.elementAt(0);
    	Long value = new Long(databaseObject.priority);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = (Long)ordered.elementAt(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.insertElementAt(value, pos1);
    	ordered.remove(pos+1);
    	hasChanged = true;
    }
	
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	XMLVector<Long> ordered = orderedSteps.elementAt(0);
    	Long value = new Long(databaseObject.priority);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = (Long)ordered.elementAt(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.insertElementAt(value, pos1+1);
    	ordered.remove(pos);
    	hasChanged = true;
    }
    
	public void debugSteps() {
		if (Engine.logBeans.isTraceEnabled()) {
			String steps = "";
			if (orderedSteps.size() > 0) {
				XMLVector<Long> ordered = orderedSteps.elementAt(0);
				steps = Arrays.asList(ordered.toArray()).toString();
			}
			Engine.logBeans.trace("["+ getName() +"] Ordered Steps ["+ steps + "]");
		}
	}
	
	public String toJsString() {
		List<Step> v = getSteps();
    	String code = "";
    	if (hasSteps()) {
    		for (int i=0; i<v.size(); i++) {
    			Step step = (Step)v.get(i);
    			code += step.toJsString() + "\n";
    		}
    	}
    	return code;
    }
    
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (bContinue && sequence.isRunning()) {
				if (super.stepExecute(javascriptContext, scope)) {
					return executeNextStep(javascriptContext, scope);
				}
			}
		}
		return false;
	}
	
	@Override
	protected void stepInit() throws EngineException {
		super.stepInit();
		currentChildStep = 0;
	}
	
	@Override
	protected void reset() throws EngineException {
		super.reset();
		bContinue = true;
	}

	@Override
	protected void stepDone() {
		if (isSynchronous()) {
			try {
				boolean hasWait = false;
				while (nbAsyncThreadRunning > 0) {
					// If contains ParallelSteps, waits until child's threads finish
					if (Engine.logBeans.isTraceEnabled())
						Engine.logBeans.trace("Step "+ getName() + " ("+executeTimeID+") waiting...");
					Thread.sleep(500);
					hasWait = true;
				}
				if (hasWait) {
					if (Engine.logBeans.isTraceEnabled())
						Engine.logBeans.trace("Step "+ getName() + " ("+executeTimeID+") ends wait");
				}
			} catch (InterruptedException e) {
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("Step "+ getName() + " ("+executeTimeID+") has been interrupted");
			}
		}
		
		// Remove transaction's context if needed
		removeTransactionContext();
		
		super.stepDone();		
	}
	
	protected void removeTransactionContext() {
		if (Engine.isEngineMode()) {
			if (parent instanceof ParallelStep) {
				if (sequence.useSameJSessionForSteps()) {
					// TODO ??
				}
				else {
					if (Engine.logBeans.isDebugEnabled())
						Engine.logBeans.debug("Executing deletion of transaction's context for step \""+ getName() +"\"");
					
					Engine.theApp.contextManager.removeAll(transactionSessionId);
					
					if (Engine.logBeans.isDebugEnabled())
						Engine.logBeans.debug("Deletion of transaction's context for step \""+ getName() +"\" done");
				}
			}
		}
	}
	
	protected void waitForAvailableThread() {
		// does nothing
	}
	
	protected void notifyForAvailableThread() {
		// does nothing
	}
	
	public synchronized void increaseAsyncThreadRunning() {
		nbAsyncThreadRunning++;
		//System.out.println("Incr step "+ name + " ("+executeTimeID+") threads :" + nbAsyncThreadRunning);
	}
	
	public synchronized void decreaseAsyncThreadRunning() {
		if (nbAsyncThreadRunning > 0) nbAsyncThreadRunning--;
		//System.out.println("Decr step "+ name + " ("+executeTimeID+") threads : " + nbAsyncThreadRunning);
	}
		
	//protected synchronized void invokeNextStep(Step step, Context javascriptContext, Scriptable scope) throws EngineException {
	protected void invokeNextStep(Step step, Context javascriptContext, Scriptable scope) throws EngineException {
		Step stepToInvoke = getStepCopyToInvoke(step);
    	if (stepToInvoke != null) {
    		// Stack scope
    		// Note: variable must be declared with the 'var' keyword
    		Scriptable curScope = javascriptContext.initStandardObjects();
   			curScope.setParentScope(scope);
   			invokeStep(stepToInvoke, javascriptContext, curScope);
    	}
		currentChildStep++;
		
		performSynchronisation();
	}

	protected void invokeStep(Step step, Context javascriptContext, Scriptable scope) {
		if (Engine.logBeans.isDebugEnabled())
			Engine.logBeans.debug("Invoquing step named '"+ step +"' ("+ step.getName() +")");
		
		AsynchronousStepThread connectionStepThread = new AsynchronousStepThread(step, scope);
		increaseAsyncThreadRunning();
		connectionStepThread.setDaemon(true);
		connectionStepThread.start();
	}
	
	class AsynchronousStepThread extends Thread {
		private org.mozilla.javascript.Context javascriptContext = null;
        private Scriptable scope = null;
        private Sequence refSequence = null;
        public boolean bContinue = false;
        private Step step = null;
        private long asyncNum;
		
        public AsynchronousStepThread(Step step, Scriptable scope) {
        	this.step = step;
        	this.scope = scope;
            this.refSequence = sequence;
            asyncNum = asyncCounters[0]++;
            setName("AsynchronousStep #" + step.hashCode());
        }

        @Override
        public void run() {
            bContinue = true;
            try {
                javascriptContext = org.mozilla.javascript.Context.enter();
            	if (step != null) {
            		if (Engine.logBeans.isDebugEnabled())
            			Engine.logBeans.debug("(AsynchronousStepThread) \""+ AsynchronousStepThread.this.getName() +"\" executing step : "+ step.getName());
           			if (step.execute(javascriptContext, scope)) {
           				//childrenSteps.put(step.executeTimeID, new Long(step.priority));
       					//executedSteps.putAll(step.executedSteps);
           				if (step instanceof ParallelStep) {
           		    		try {
           		    			while (((ParallelStep)step).nbAsyncThreadRunning > 0) {
           		    				Thread.sleep(500);
           		    			}
           		    		} catch (InterruptedException e) {
           		    			if (Engine.logBeans.isDebugEnabled())
           		    				Engine.logBeans.debug("(AsynchronousStepThread) \""+ AsynchronousStepThread.this.getName() +"\" has been interrupted");
           		    		}
           				}
           			}
            	}
        			
            } catch (Exception e) {
                Engine.logBeans.error("An error occured while invoking connection step \""+ AsynchronousStepThread.this.getName() +"\"", e);
            } finally {
                bContinue = false;
				org.mozilla.javascript.Context.exit();
				javascriptContext = null;
               	decreaseAsyncThreadRunning();
               	step.cleanCopy();
               	refSequence.removeCopy(step.executeTimeID, new Long(step.priority));
               	if (Engine.logBeans.isDebugEnabled())
               		Engine.logBeans.debug("(AsynchronousStepThread) \""+ AsynchronousStepThread.this.getName() +"\" done");
            }
        }
        
        public void wakeTurn(Step step) {
        	if (asyncCounters != null) {
        		synchronized (asyncCounters) {
        			long next = asyncCounters[1];
        			
        			if (Engine.logBeans.isTraceEnabled())
        				Engine.logBeans.trace("(AsynchronousStepThread) \"" + AsynchronousStepThread.this.getName() + "\" (" + step.getName() + ") wakeTurn : is " + asyncNum + " and current is " + next);
        			
        			while (asyncNum > next && bContinue && sequence.isRunning()) {
        				try {
        					asyncCounters.wait(5000);
        				} catch (InterruptedException e) { }
        				next = asyncCounters[1];
        				if (Engine.logBeans.isDebugEnabled())
        					Engine.logBeans.debug("(AsynchronousStepThread) \"" + AsynchronousStepThread.this.getName() + "\" (" + step.getName() + ") wakeTurn retry : is " + asyncNum + " and current is " + next);
        			}
        			if (asyncNum == asyncCounters[1]) {
        				asyncCounters[1]++;
        				if (Engine.logBeans.isDebugEnabled())
        					Engine.logBeans.debug("(AsynchronousStepThread) \"" + AsynchronousStepThread.this.getName() + "\" (" + step.getName() + ") wakeTurn inc : next value is " + asyncCounters[1]);
        			}
        			asyncCounters.notifyAll();
        		}
        	}
        }
	}
	
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException
    {
    	if (isEnable()) {
	    	if (hasSteps()) {
	    		for (int i=0; i<numberOfSteps(); i++) {
	    			if (bContinue && sequence.isRunning())
	    				executeNextStep((Step)getSteps().get(i), javascriptContext, scope);
	    			else break;
	    		}
	    	}
	    	return true;
    	}
    	return false;
    }

	private Step getStepCopy(Step step) throws EngineException {
		step.checkSubLoaded();
		
    	Step stepCopy = null;
    	if (step.isEnable()) {
    		Object ob = null;
			try {
				ob = step.copy();
			} catch (CloneNotSupportedException e) {
				throw new EngineException("Unable to get a copy of step \""+ step.getName()+"\" ("+step+")",e);
			}
			stepCopy = (Step)ob;
		}
		return stepCopy;
	}
	
	private Step getStepCopyToExecute(Step step) throws EngineException {
		Step stepToExecute = getStepCopy(step);
		if (stepToExecute != null) {
			stepToExecute.parent = this;
			stepToExecute.transactionContextMaintainer = ((this.parent instanceof ParallelStep) ? this:transactionContextMaintainer);
			stepToExecute.xpathApi = xpathApi;
			stepToExecute.httpState = ((stepToExecute instanceof BranchStep) ? sequence.getNewHttpState():this.httpState);
			stepToExecute.executedSteps.putAll(executedSteps);
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(StepWithExpression) "+step+" ["+step.hashCode()+"] has been copied into "+stepToExecute+" ["+stepToExecute.hashCode()+"]");
		}
		return stepToExecute;
	}
	
	private Step getStepCopyToInvoke(Step step) throws EngineException {
		Step stepToInvoke = getStepCopy(step);
		if (stepToInvoke != null) {
			stepToInvoke.parent = this;
			stepToInvoke.transactionContextMaintainer = ((sequence.useSameJSessionForSteps()) ? this:null);
			stepToInvoke.xpathApi = xpathApi;
			stepToInvoke.httpState = sequence.getNewHttpState(); // require new HttpState!
			stepToInvoke.executedSteps.putAll(executedSteps);
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(StepWithExpression) "+step+" ["+step.hashCode()+"] has been copied into "+stepToInvoke+" ["+stepToInvoke.hashCode()+"]");
		}
		return stepToInvoke;
	}

	protected void executeNextStep(Step step, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
    	Step stepToExecute = getStepCopyToExecute(step);
    	if (stepToExecute != null) {
    		// Execute step
    		if (stepToExecute.execute(javascriptContext, scope)) {
    			childrenSteps.put(stepToExecute.executeTimeID, new Long(stepToExecute.priority));
   				executedSteps.putAll(stepToExecute.executedSteps);
    		}
    		else {
    			stepToExecute.cleanCopy();
    		}
    	}
   		currentChildStep++;
   	
   		// Makes current thread wait if needed
   		// (case maxNumberOfThread for a parallel step has been reached)
   		performSynchronisation();
    }
	
	private synchronized void performSynchronisation() {
		try {
			if (haveToWait.equals(Boolean.TRUE)) {
				if (bContinue && sequence.isRunning()) {
					if (Engine.logBeans.isTraceEnabled())
						Engine.logBeans.trace("Step '"+ getName() +"' ("+executeTimeID+") waiting...");
					
					wait();
					
					if (Engine.logBeans.isTraceEnabled())
						Engine.logBeans.trace("Step '"+ getName() +"' ("+executeTimeID+") going through...");
				}
			}
		} catch (InterruptedException e) {
			if (Engine.logBeans.isDebugEnabled())
				Engine.logBeans.debug("Step '"+ getName() +"' ("+executeTimeID+") has been interrupted");				
		}
	}
	
	public synchronized void shouldWait(boolean bWait) {
		if (bWait) {
			if (haveToWait.equals(Boolean.FALSE))
				haveToWait = Boolean.TRUE;
		}
		else {
			if (haveToWait.equals(Boolean.TRUE)) {
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("Step '"+ getName() +"' ("+executeTimeID+") has been notified");
				haveToWait = Boolean.FALSE;
				notify();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toXml(org.w3c.dom.Document)
	 */
	@Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        // Storing the transaction "handlePriorities" flag
        element.setAttribute("handlePriorities", new Boolean(handlePriorities).toString());
		
		return element;
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		List<Step> steps = getSteps();		
		for (Step step : steps) {
			rep.add(step);
		}		
		return rep;
	}
	
	protected XmlSchemaParticle getXmlSchemaParticle(XmlSchemaCollection collection, XmlSchema schema, XmlSchemaGroupBase group) {
		XmlSchemaParticle particle = group;
		if (isOutput()) {
			XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
			XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
			SchemaMeta.setContainerXmlSchemaGroupBase(element, group);
			element.setType(cType);
			cType.setParticle(group);
			particle = element;
		}
		return particle;
	}
	
	@Override
	public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		return (XmlSchemaParticle) super.getXmlSchemaObject(collection, schema);
	}
	
	public boolean isGenerateElement() {
		return isOutput();
	}
}
