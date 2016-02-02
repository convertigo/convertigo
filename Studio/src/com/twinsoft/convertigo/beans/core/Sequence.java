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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.swing.event.EventListenerList;
import javax.xml.namespace.QName;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.StepWithExpressions.AsynchronousStepThread;
import com.twinsoft.convertigo.beans.steps.BranchStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.RequestableEngineEvent;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.requesters.DefaultRequester;
import com.twinsoft.convertigo.engine.requesters.GenericRequester;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public abstract class Sequence extends RequestableObject implements IVariableContainer, ITestCaseContainer, IContextMaintainer, IContainerOrdered, ISchemaParticleGenerator, IComplexTypeAffectation {

	private static final long serialVersionUID = 8218719500689068156L;
	
    public static final String EVENT_SEQUENCE_STARTED = "SequenceStarted";
    public static final String EVENT_SEQUENCE_FINISHED = "SequenceFinished";

    transient protected TwsCachedXPathAPI xpathApi = null;
    
	transient private Hashtable<String, Step> copies = null;

    transient public Hashtable<String, Project> loadedProjects = new Hashtable<String, Project>(10);
    
    transient public Hashtable<Long, Step> loadedSteps = new Hashtable<Long, Step>(10);
    
    transient protected Hashtable<String, Long> childrenSteps = null;
    
    transient public Hashtable<Long, String> executedSteps = null;
    
	transient private Hashtable<String, Node> workerElementMap = null;
	
    transient private List<Step> vAllSteps = null;
    
	transient private List<Step> vSteps = new Vector<Step>();

	transient public Step currentStep = null;
	
	transient protected int currentChildStep = 0;
	
	transient public boolean handlePriorities = true;
	
	transient private HttpState stepHttpState = null;
	
	transient private String transactionSessionId = null;
	
	transient private int nbAsyncThreadRunning = 0;
	
	transient private List<String> stepContextNames = new Vector<String>();
	
	transient private int cloneNumber = 0;
	
	transient private boolean arborting = false;
	
	transient private boolean skipSteps = false;
	
	transient private List<RequestableVariable> vAllVariables = null;
	transient private List<RequestableVariable> vVariables = new Vector<RequestableVariable>();
	
	transient private List<TestCase> vTestCases = new Vector<TestCase>();
	
//	transient private Element workerRootElement = null;
	
    /** The vector of ordered step objects which can be applied on the Sequence. */
	private XMLVector<XMLVector<Long>> orderedSteps = null;
	
	/** The vector of ordered variables objects of Sequence. */
	private XMLVector<XMLVector<Long>> orderedVariables = new XMLVector<XMLVector<Long>>();
	
	private boolean includeResponseElement = true;
	
	public Sequence() {
        super();
		orderedSteps = new XMLVector<XMLVector<Long>>();
		orderedSteps.addElement(new XMLVector<Long>());
        
		orderedVariables = new XMLVector<XMLVector<Long>>();
		orderedVariables.addElement(new XMLVector<Long>());

		databaseType = "Sequence";
	}

	@Override
    public Sequence clone() throws CloneNotSupportedException {
    	Sequence clonedObject = (Sequence) super.clone();
    	clonedObject.variables = new HashMap<String, Object>();
    	clonedObject.cloneNumber = ++cloneNumber;
    	clonedObject.vVariables = new Vector<RequestableVariable>();
    	clonedObject.vTestCases = new Vector<TestCase>();
    	clonedObject.vAllVariables = null;
    	clonedObject.nbAsyncThreadRunning = 0;
    	clonedObject.xpathApi = null;
    	clonedObject.stepHttpState = null;
    	clonedObject.transactionSessionId = null;
    	clonedObject.copies = null;
    	clonedObject.loadedProjects = new Hashtable<String, Project>(10);
    	clonedObject.loadedSteps = new Hashtable<Long, Step>(10);
    	clonedObject.executedSteps = null;
    	clonedObject.childrenSteps = null;
    	clonedObject.workerElementMap = null;
    	clonedObject.vSteps = new Vector<Step>();
        clonedObject.vAllSteps = null;
        clonedObject.handlePriorities = handlePriorities;
        clonedObject.currentStep = null;
        clonedObject.currentChildStep = 0;
        clonedObject.stepContextNames = new Vector<String>();
        clonedObject.arborting = false;
        clonedObject.skipSteps = false;
        clonedObject.sequenceListeners = new EventListenerList();
        clonedObject.stepListeners = new EventListenerList();
        return clonedObject;
    }
	
    public Sequence cloneKeepParent() throws CloneNotSupportedException {
    	Sequence clonedObject = clone();
    	clonedObject.parent = parent;
        return clonedObject;
    }
	
	public XMLVector<XMLVector<Long>> getOrderedSteps() {
		return orderedSteps;
	}

	public void setOrderedSteps(XMLVector<XMLVector<Long>> orderedSteps) {
		this.orderedSteps = orderedSteps;
	}
	
	public int getCurrentChildStep() {
		return currentChildStep;
	}
	
	/** Getter for property variablesDefinition.
	 * @return Value of property variablesDefinition.
	 */
	public XMLVector<XMLVector<Long>> getOrderedVariables() {
		return orderedVariables;
	}
    
	/** Setter for property variablesDefinition.
	 * @param variables New value of property variablesDefinition.
	 */
	public void setOrderedVariables(XMLVector<XMLVector<Long>> orderedVariables) {
		this.orderedVariables = orderedVariables;
	}

	public Object getVariableValue(String requestedVariableName) {
		// Request parameter value (see parseInputDocument())
		Object value = variables.get(requestedVariableName);
		
		// If no value is found, find the default value and return it.
		if (value == null) {
			Object valueToPrint = null;
			Variable variable = getVariable(requestedVariableName);
			if (variable != null) {
				value = variable.getValueOrNull();// new 5.0.3 (may return null)
				valueToPrint = Visibility.Logs.printValue(variable.getVisibility(), value);
				if (Engine.logBeans.isDebugEnabled()) {
				if ((value != null) && (value instanceof String))
					Engine.logBeans.debug("Default value: " + requestedVariableName + " = \"" + valueToPrint + "\"");
				else
					Engine.logBeans.debug("Default value: " + requestedVariableName + " = " + valueToPrint);
				}
			}
		}

		if ((value != null) && (value instanceof List)) {
			List<?> lst = GenericUtils.cast(value);
			value = lst.toArray(new Object[lst.size()]);
		}
		return value;
	}
	
	public int getVariableVisibility(String requestedVariableName) {
		Variable variable = getVariable(requestedVariableName);
		if (variable != null)
			return variable.getVisibility();
		return 0;
	}
	
	public transient Map<String, Object> variables = new HashMap<String, Object>();

	@Override
	public void parseInputDocument(Context context) {
		super.parseInputDocument(context);

		if (context.inputDocument != null && Engine.logContext.isInfoEnabled()) {
			Document printDoc = (Document) Visibility.Logs.replaceVariables(getVariablesList(), context.inputDocument);
			XMLUtils.logXml(printDoc, Engine.logContext, "Input document");
		}
		
		NodeList variableNodes = context.inputDocument.getElementsByTagName("variable");
		int len = variableNodes.getLength();
		
		variables.clear();
		
		for (int i = 0 ; i < len ; i++) {
			Element variableNode = (Element) variableNodes.item(i);
			String variableName = variableNode.getAttribute("name");
			String variableValue = (variableNode.hasAttribute("value") ? variableNode.getAttribute("value") : null);
			Attr valueAttrNode = variableNode.getAttributeNode("value");
			
			// Test case for sequence
			if (variableName.indexOf(Parameter.Testcase.getName()) == 0) {
				TestCase testcase = getTestCaseByName(variableValue);
				if (testcase != null) {
					String testCaseVariableName;
					Object testCaseVariableValue;
					// Add test case variables default value(s)
					for (TestCaseVariable testCaseVariable: testcase.getVariables()) {
						testCaseVariableName = testCaseVariable.getName();
						testCaseVariableValue = testcase.getVariableValue(testCaseVariableName);
						if (testCaseVariableValue != null) {
							variables.put(testCaseVariableName, testCaseVariableValue);
						}
					}
				}
				else {
					if (Engine.logBeans.isInfoEnabled())
						Engine.logBeans.warn("Sequence: there's no testcase named '" + variableValue + "' for '" +  getName() + "' sequence");
				}
				continue;
			}
			
			// Standard variable case
			RequestableVariable variable = (RequestableVariable) getVariable(variableName);
			
			// Structured value?
			Object scopeValue = (variableValue != null) ? variableValue : variableNode.getChildNodes();
			
			// Multivalued variable ?
			if ((variable != null) && (variable.isMultiValued())) {
				List<Object> current = GenericUtils.cast(variables.get(variableName));
				if (current == null) {
					current = new LinkedList<Object>();
					variables.put(variableName, current);
				}
				if (variableValue == null || valueAttrNode != null) {
					current.add(scopeValue);
				}
			} else {
				variables.put(variableName, scopeValue);
			}
		}
		
		// Enumeration of all sequence variables
		if (Engine.logBeans.isDebugEnabled())
			Engine.logBeans.debug("Sequence variables: " + (variables == null ? "none" : Visibility.Logs.replaceVariables(getVariablesList(), variables)));
	}

	@Override
    protected void insertObjectsInScope() throws EngineException {
    	super.insertObjectsInScope();
    	
		// Insert variables into the scripting context: first insert the explicit
		// (declared) variables (with or not default values), and then insert the
		// variables (that may eventually be the same).
		String variableName;
		Object variableValue;
		Object variableValueToPrint;
		int variableVisibility;
		Scriptable jsObject;
		
		checkSubLoaded();
		
		for (RequestableVariable variable : vVariables) {
			variableName = variable.getName();
			variableVisibility = variable.getVisibility();
			if (variableName.startsWith("__")) continue;
			if (variables.containsKey(variableName)) continue;
			variableValue = getVariableValue(variableName);
			jsObject = ((variableValue == null) ? null:org.mozilla.javascript.Context.toObject(variableValue, scope));
			scope.put(variableName, scope, jsObject);
			variableValueToPrint = Visibility.Logs.printValue(variableVisibility,variableValue);
			if (Engine.logBeans.isDebugEnabled()) {
			if ((variableValue != null) && (variableValue instanceof String))
				Engine.logBeans.debug("(Sequence) Declared but not provided sequence variable " + variableName + "=\"" + variableValueToPrint + "\" added to the scripting scope");
			else
				Engine.logBeans.debug("(Sequence) Declared but not provided sequence variable " + variableName + "=" + variableValueToPrint + " added to the scripting scope");
			}
		}
		

		for (String variableName2 : variables.keySet()) {
			if (variableName2.startsWith("__")) continue;
			variableVisibility = getVariableVisibility(variableName2);
			variableValue = getVariableValue(variableName2);
			jsObject = ((variableValue == null) ? null:org.mozilla.javascript.Context.toObject(variableValue, scope));
			scope.put(variableName2, scope, jsObject);
			variableValueToPrint = Visibility.Logs.printValue(variableVisibility,variableValue);
			if (Engine.logBeans.isDebugEnabled()) {
			if ((variableValue != null) && (variableValue instanceof String))
				Engine.logBeans.debug("(Sequence) Provided sequence variable " + variableName2 + "=\"" + variableValueToPrint + "\" added (or overridden) to the scripting scope");
			else
				Engine.logBeans.debug("(Sequence) Provided sequence variable " + variableName2 + "=" + variableValueToPrint + " added (or overridden) to the scripting scope");
			}
		}
    }

	@Override
	public String getRequestString(Context context) {
		checkSubLoaded();
		
		List<String> vVariables = new Vector<String>(variables.size());		
		//Use authenticated user as cache key
		if ( isAuthenticatedUserAsCacheKey() )
			vVariables.add("userID="+context.getAuthenticatedUser());
		for (String variableName : variables.keySet()) {
			if (includeVariableIntoRequestString(variableName)) {
				vVariables.add(variableName + "=" + variables.get(variableName));
			}
		}
		
		Collections.sort(vVariables);
		
		String requestString = context.projectName + " " + context.sequenceName + " " + vVariables.toString();
		
		return requestString;
	}
	
	public boolean includeVariableIntoRequestString(String variableName) {
		RequestableVariable variable = (RequestableVariable)getVariable(variableName);
		if (variable != null) {
			return variable.isCachedKey();
		}
		return false;
	}

	@Override
	public String generateXsdArrayOfData() throws Exception {
		String xsdArrayData = "";
		RequestableVariable variable = null;
		for (int i=0; i<numberOfVariables(); i++) {
			variable = (RequestableVariable)getVariable(i);
			if (variable.isWsdl()) {
				if (variable.isMultiValued()) {
					xsdArrayData += Engine.getArrayOfSchema(variable.getSchemaType());
				}
			}
		}
		return xsdArrayData;
	}
	
	@Override
	public String generateXsdRequestData() throws Exception {
		return null;
    }

	@Override
	protected String generateXsdResponseData(Document document, boolean extract) throws Exception {
		return null;
    }

	@Override
	protected String extractXsdType(Document document) throws Exception {
		return null;
	}

	@Override
	public String generateWsdlType(Document document) throws Exception {
		return null;
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof RequestableVariable) {
            addVariable((RequestableVariable) databaseObject);
        }
        else if (databaseObject instanceof TestCase) {
            addTestCase((TestCase) databaseObject);
        }
        else if (databaseObject instanceof Step) {
			addStep((Step) databaseObject);
		}
        else {
            super.add(databaseObject);
        }
	}

	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RequestableVariable) {
			removeVariable((RequestableVariable) databaseObject);
		}
        else if (databaseObject instanceof TestCase) {
            removeTestCase((TestCase) databaseObject);
        }
		else if (databaseObject instanceof Step) {
			removeStep((Step) databaseObject);
		}
        else {
        	super.remove(databaseObject);
        }
		
	}

    public void init() {
    	vAllSteps = null;
    }
    
    public Set<String> getLoadedProjectNames() {
    	synchronized (loadedProjects) {
        	return new HashSet<String>(loadedProjects.keySet());			
		}
    }
    
	public Project getLoadedProject(String projectName) throws EngineException {
		Project project = getProject();
		
		synchronized (loadedProjects) {
			if (Engine.isStudioMode() || (Engine.isEngineMode() && loadedProjects.isEmpty()))
				loadedProjects.put(project.getName(), project);
			
			Project loadedProject = (Project) loadedProjects.get(projectName);
			if (loadedProject != null) {
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("Current project name : " + project + ", requested projectName :" + projectName + " already loaded");
			}
			else {
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("Current project name : " + project + ", loading requested projectName :" + projectName);
				loadedProject = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
				loadedProjects.put(projectName, loadedProject);
			}
			return loadedProject;
		}
	}
    
	public void setLoadedProject(Project project) {
		if (project != null) {
			String projectName = project.getName();
			synchronized (loadedProjects) {
				Project p = (Project)loadedProjects.get(projectName);
				if ((p == null) || ((p != null) && (!p.equals(project)))) {
					loadedProjects.put(projectName, project);
					if (Engine.logBeans.isTraceEnabled())
						Engine.logBeans.trace("Updated sequence '"+getName()+"' with project "+ projectName +"("+project.hashCode()+")");
				}
			}
		}
	}
	
	public void removeLoaded(String projectName) {
		synchronized (loadedProjects) {
			loadedProjects.remove(projectName);
		}
	}
	
	public Step getStep(String stepName) {
		checkSubLoaded();
		
		Step step = null;
		for (int i=0;i<vSteps.size();i++) {
			Object ob = vSteps.get(i);
			if (ob instanceof Step) {
				step = (Step)ob;
				if (step.getName().equals(stepName)) {
					break;
				}
			}
		}
		return step;
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
        	else throw new EngineException("Corrupted step for Sequence \""+ getName() +"\". Step \""+ ((Step)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        } else if (object instanceof Variable) {
        	List<Long> ordered = orderedVariables.get(0);
        	long time = ((Variable)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted variable for Sequence \""+ getName() +"\". Variable \""+ ((Variable)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
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

    public List<RequestableVariable> getVariables(boolean reset) {
    	if (reset)
    		vAllVariables = null;
    	return getVariablesList();
    }
    
    /** Compatibility for version older than 4.6.0 **/
    @Deprecated
    public XMLVector<XMLVector<Object>> getVariablesDefinition() {
    	XMLVector<XMLVector<Object>> xmlv = new XMLVector<XMLVector<Object>>();
    	getVariablesList();
    	if (hasVariables()) {
    		for (int i=0; i<numberOfVariables(); i++) {
    			RequestableVariable variable = (RequestableVariable)getVariable(i);
    			
    			XMLVector<Object> v = new XMLVector<Object>();
    			v.addElement(variable.getName());
    			v.addElement(variable.getDescription());
    			v.addElement(variable.getDefaultValue());
    			v.addElement(variable.isWsdl());
    			v.addElement(variable.isMultiValued());
    			v.addElement(variable.isPersonalizable());
    			v.addElement(variable.isCachedKey());
    			
    			xmlv.addElement(v);
    		}
    	}
    	return xmlv;
    }
    
    public Vector<RequestableVariable> getVariables() {
    	return new Vector<RequestableVariable>(getVariablesList());
    }
    
    public List<RequestableVariable> getVariablesList() {
    	checkSubLoaded();
    	
    	if ((vAllVariables == null) || hasChanged)
    		vAllVariables = getAllVariables();
    	return vAllVariables;
    }
    
    public List<RequestableVariable> getAllVariables() {
    	checkSubLoaded();
    	
    	return sort(vVariables);
    }

	public Variable getVariable(int index) {
		checkSubLoaded();
		
		try {
			return (RequestableVariable)vVariables.get(index);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public Variable getVariable(String variableName) {
		checkSubLoaded();
		
		for (int i=0; i<vVariables.size(); i++) {
			RequestableVariable variable = (RequestableVariable)vVariables.get(i);
			if (variable.getName().equals(variableName)) {
				return variable;
			}
		}
		return null;
	}
	
	public boolean hasVariables() {
		checkSubLoaded();
		
		return (vVariables.size() > 0);
	}
    
	public int numberOfVariables() {
		checkSubLoaded();
		
		return vVariables.size();
	}
	
    public void addVariable(RequestableVariable variable) throws EngineException {
    	checkSubLoaded();
    	
		String newDatabaseObjectName = getChildBeanName(vVariables, variable.getName(), variable.bNew);
		variable.setName(newDatabaseObjectName);
        
		vVariables.add(variable);
        
        variable.setParent(this);
        
        insertOrderedVariable(variable,null);
    }
    
	public void addStep(Step step) throws EngineException {
		checkSubLoaded();
		
		String newDatabaseObjectName = getChildBeanName(vSteps, step.getName(), step.bNew);
		step.setName(newDatabaseObjectName);

        vSteps.add(step);
        
        step.setParent(this);// do not call super.add otherwise it will generate an exception
        step.sequence = this;
        
        loadedSteps.put(new Long(step.priority), step);
        addStepListener(step);
        
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
	
    private void insertOrderedVariable(Variable variable, Long after) {
    	XMLVector<Long> ordered = orderedVariables.elementAt(0);
    	int size = ordered.size();
    	
    	Long value = new Long(variable.priority);
    	
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

    public void removeVariable(RequestableVariable variable) {
    	checkSubLoaded();
    	
    	vVariables.remove(variable);
    	variable.setParent(null);

    	Long value = new Long(variable.priority);
        removeOrderedVariable(value);
    }
    
    public void removeStep(Step step) {
    	checkSubLoaded();
    	
    	vSteps.remove(step);
    	step.setParent(null);// Do not call super.remove otherwise it will generate an exception
    	step.sequence = null;
    	
    	Long value = new Long(step.priority);
        removeOrderedStep(value);
        
        loadedSteps.remove(new Long(step.priority));
        removeStepListener(step);
    }
    
    private void removeOrderedStep(Long value) {
    	XMLVector<Long> ordered = orderedSteps.elementAt(0);
        ordered.removeElement(value);
        hasChanged = true;
    }

    private void removeOrderedVariable(Long value) {
    	XMLVector<Long> ordered = orderedVariables.elementAt(0);
        ordered.removeElement(value);
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
	
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if ((databaseObject instanceof Step) || (databaseObject instanceof Variable))
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if ((databaseObject instanceof Step) || (databaseObject instanceof Variable))
			decreaseOrder(databaseObject,null);
	}
	
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, new Long(priority));
	}
	
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	XMLVector<Long> ordered = null;
    	Long value = new Long(databaseObject.priority);
    	
    	if (databaseObject instanceof Step)
    		ordered = orderedSteps.elementAt(0);
    	else if (databaseObject instanceof Variable)
    		ordered = orderedVariables.elementAt(0);
    	
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
    	XMLVector<Long> ordered = null;
    	Long value = new Long(databaseObject.priority);
    	
    	if (databaseObject instanceof Step)
    		ordered = orderedSteps.elementAt(0);
    	else if (databaseObject instanceof Variable)
    		ordered = orderedVariables.elementAt(0);
    	
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
	
    public void addTestCase(TestCase testCase) throws EngineException {
    	checkSubLoaded();
    	
		String newDatabaseObjectName = getChildBeanName(vTestCases, testCase.getName(), testCase.bNew);
		testCase.setName(newDatabaseObjectName);
        
		vTestCases.add(testCase);
        
		testCase.setParent(this);
    }
	
    public void removeTestCase(TestCase testCase) {
    	checkSubLoaded();
    	
    	vTestCases.remove(testCase);
    	testCase.setParent(null);
    }
    
	public TestCase getTestCaseByName(String testCaseName) {
		checkSubLoaded();
		for (TestCase testCase : vTestCases) {
			if (testCase.getName().equalsIgnoreCase(testCaseName)) return testCase;
		}
		return null;
	}
    
	public List<TestCase> getTestCasesList() {
		checkSubLoaded();
		return sort(vTestCases);
	}
    
	public HttpState getHttpState() {
		HttpState httpState = null;
		if (context.httpState == null) {
			Engine.logBeans.trace("Creating new HttpState for context id "+ context.contextID);
			context.httpState = httpState = new HttpState();			
		} else {
			Engine.logBeans.trace("Using HttpState of context id "+ context.contextID);
			httpState = context.httpState;
		}
		return httpState;
	}
    
	public HttpState getStepHttpState() {
		if (stepHttpState == null)
			stepHttpState = getNewHttpState();
		return stepHttpState;
	}

	public HttpState getNewHttpState() {
		// Uses Sequence HttpState
		if (useSameJSessionForSteps())
			return getHttpState();
		// Uses new HttpState : will create a new JSP session
		return new HttpState();
	}
	
	public String getInheritedContextName() {
		if (useSameJSessionForSteps())
			return (String) context.get("inheritedContext");
		return null;
	}
	
	public String getContextName() {
		if (useSameJSessionForSteps())
			return "Container-"+ getProject().getName() + "-" + getName() + "." + cloneNumber;
		else
			return "Container-"+ getName() + "." + cloneNumber;
	}
	
	public String getSessionId() {
		String sessionId = null;
		try {
			// Case of internal requester
			if (context.httpSession == null) {
				try {
					InternalRequester requester = (InternalRequester) context.requestedObject.requester;
					Map<String, String[]> request = GenericUtils.cast(requester.inputData);
					sessionId = request.get(Parameter.SessionId.getName())[0];
					Engine.logBeans.debug("Sequence session ID (internal requester case): " + sessionId);
				} catch (Exception e) {
					// Exception case
					sessionId = context.contextID.substring(0,context.contextID.indexOf("_"));
					Engine.logBeans.debug("Sequence session ID (internal requester case, but with exception): " + sessionId);
					Engine.logBeans.debug(e.getMessage());
				}				
			}
			// Case of servlet requester
			else {
				sessionId = context.httpSession.getId();
				Engine.logBeans.debug("Sequence session ID (servlet requester case): " + sessionId);
			}
		}
		catch (Exception e) {
			Engine.logBeans.error("Unable to retrieve sessionID of sequence", e);
		}
		
		return sessionId;
	}

	public String getTransactionSessionId() {
		return transactionSessionId;
	}
	
	public void setTransactionSessionId(String sessionId) {
		if ((transactionSessionId == null) && (sessionId != null)) {
			transactionSessionId = sessionId;
			Engine.logBeans.trace("(Sequence) setting transactionSessionId: "+ transactionSessionId);
		}
		else if (transactionSessionId != null) {
			Engine.logBeans.trace("(Sequence) transactionSessionId/JSESSIONID: "+ transactionSessionId +"/"+sessionId);
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
						Engine.logBeans.trace("(Sequence) setting transactionSessionId: "+ transactionSessionId);
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
							Engine.logBeans.trace("(Sequence) transactionSessionId/JSESSIONID: "+ transactionSessionId +"/"+cookie.getValue());
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void fireRequestableEvent(String eventType) {
    	if (eventType.equalsIgnoreCase(RequestableObject.EVENT_REQUESTABLE_STARTED)) {
    		Engine.theApp.fireSequenceStarted(new RequestableEngineEvent(this, context.projectName, context.sequenceName, context.connectorName));
    	}
    	else if (eventType.equalsIgnoreCase(RequestableObject.EVENT_REQUESTABLE_FINISHED)) {
    		Engine.theApp.fireSequenceFinished(new RequestableEngineEvent(this, context.projectName, context.sequenceName, context.connectorName));
    	}
	}

	@Override
	public void handleRequestableEvent(String eventType, org.mozilla.javascript.Context javascriptContext) throws EngineException {
		
	}

	@Override
	public boolean hasToRunCore() {
		return true;
	}

	@Override
	public void prepareForRequestable(Context context, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		currentChildStep = 0;
		xpathApi = new TwsCachedXPathAPI();
		copies = new Hashtable<String, Step>(100);
		childrenSteps = new Hashtable<String, Long>(100);
		executedSteps = new Hashtable<Long, String>(1000);
		workerElementMap = new Hashtable<String, Node>(1000);
		
		insertObjectsInScope();
	}

	private void clean() {
    	cleanCopies();

		Enumeration<Long> e = loadedSteps.keys();
		while (e.hasMoreElements()) {
			Long stepPriority = (Long)e.nextElement();
			resetLoadedStepAsyncThreadRunning(stepPriority);
		}
		
    	if (vAllSteps != null) {
	    	vAllSteps.clear();
	    	vAllSteps = null;
    	}
    	if (childrenSteps != null) {
    		childrenSteps.clear();
    		childrenSteps = null;
    	}
    	if (executedSteps != null) {
	    	executedSteps.clear();
	    	executedSteps = null;
    	}
    	if (workerElementMap != null) {
        	workerElementMap.clear();
        	workerElementMap = null;
    	}
    	if (loadedProjects != null) {
    		if (Engine.isEngineMode()) {
    			synchronized (loadedProjects) {
    				loadedProjects.clear();
    			}
    		}
    	}
    	if (xpathApi != null) {
    		xpathApi.release();
        	xpathApi = null;
    	}
    	stepHttpState = null;
	}
	
	private void resetLoadedStepAsyncThreadRunning(Long stepPriority) {
		if (stepPriority != null) {
			Step step = (Step)loadedSteps.get(stepPriority);
			if ((step != null) && (step instanceof StepWithExpressions)) {
				((StepWithExpressions)step).nbAsyncThreadRunning = 0;
			}
		}
	}

	private void cleanCopie(String timeID) {
		if (timeID != null) {
			Long stepPriority = null;
			Step step = getCopy(timeID);
			if (step != null) {
				stepPriority = new Long(step.priority);
				step.cleanCopy();
				//step.copiesOfInstance = 0;
			}
			if (!timeID.equals(""))
				removeCopy(timeID, stepPriority);
		}
	}
	
	private void cleanCopies() {
		Enumeration<String> e;
		
		e = childrenSteps.keys();
		while (e.hasMoreElements()) {
			String timeID = (String)e.nextElement();
			cleanCopie(timeID);
		}
		
		//System.out.println("Sequence copies :" + copies.size());
		e = copies.keys();
		while (e.hasMoreElements()) {
			String timeID = (String)e.nextElement();
			//System.out.println("Sequence needs to clean copy of "+step.name+" ("+timeID+")");
			cleanCopie(timeID);
		}
		
		copies.clear();
	}
	
	public void addCopy(String executeTimeID, Step step) {
		synchronized (copies) {
			if ((executeTimeID != null) && (step != null))  {
				copies.put(executeTimeID, step);
				//System.out.println("Sequence add copy of "+step.name+" ("+executeTimeID+") :"+ copies.size());
			}
		}
	}
	
	public void removeCopy(String executeTimeID, Long stepPriority) {
		synchronized (copies) {
			if (executeTimeID != null)  {
				copies.remove(executeTimeID);
				//System.out.println("Sequence remove copy ("+executeTimeID+") :"+ copies.size());
			}
		}
	}
	
	public Step getCopy(String executeTimeID) {
		if (executeTimeID != null)  {
			return (Step)copies.get(executeTimeID);
		}
		return null;
	}
	
	public synchronized void increaseAsyncThreadRunning() {
		nbAsyncThreadRunning++;
		//System.out.println("Incr sequence threads :" + nbAsyncThreadRunning);
	}
	
	public synchronized void decreaseAsyncThreadRunning() {
		if (nbAsyncThreadRunning > 0) nbAsyncThreadRunning--;
		//System.out.println("Decr sequence threads :" + nbAsyncThreadRunning);
	}
	
	public synchronized int setAsyncThreadRunningNumber(long priority, boolean increase) {
		Step step = ((Step)loadedSteps.get(new Long(priority)));
		if ((step != null) && (step instanceof StepWithExpressions)) {
			StepWithExpressions stepWE = (StepWithExpressions)step;
			if (increase) {
				stepWE.nbAsyncThreadRunning++;
				//System.out.println("Incr step '"+ step.getName() +"' threads :" + nbAsyncThreadRunning);
			}
			else {
				if (stepWE.nbAsyncThreadRunning > 0) stepWE.nbAsyncThreadRunning--;
				//System.out.println("Decr step '"+ step.getName() +"' threads :" + nbAsyncThreadRunning);
			}
			return stepWE.nbAsyncThreadRunning;
		}
		return 0;
	}
	
	public void skipNextSteps(boolean skip) {
		skipSteps = skip;
	}
	
	public boolean isRunning() {
		return !arborting && runningThread.bContinue;
	}
	
	//TODO: see how to synchronize if context.arbortRequestable() is used trough
	// javascript under parallel steps
	@Override
	public void abort() {
		if (isRunning()) {
			if (Engine.logBeans.isDebugEnabled())
				Engine.logBeans.debug("Sequence '"+ getName() + "' is aborting...");
			
			// Sets abort flag
			arborting = true;
        	
			// Abort children's contexts
			if (this.useSameJSessionForSteps()) {
				try {
					Collection<Context> contexts = Engine.theApp.contextManager.getContexts();
					for (Context ctx : contexts) {
						if (ctx.parentContext == context) {
							ctx.abortRequestable();
						}
//						if (!this.context.equals(ctx)) {
//							if (ctx.contextID.startsWith(getSessionId())) {
//								ctx.abortRequestable();
//							}
//						}
					}
				}
				catch(Exception e) {}
			}
			else {
	    		String contextName;
	    		for (int i=0; i<stepContextNames.size(); i++) {
	    			contextName = (String)stepContextNames.get(i);
	        		Context ctx = Engine.theApp.contextManager.getContextByName(contextName);
	        		if (ctx != null) {
	        			try {
	        				if (Engine.logBeans.isDebugEnabled())
	        					Engine.logBeans.debug("(Sequence) Aborting requestable for context ("+contextName+") "+ ctx.contextID);
	        				ctx.abortRequestable();
	                	}
	                	catch (Exception e) {}
	        		}
	    		}
			}
		}
	}
	
	public synchronized String addStepContextName(String contextName) {
		if (contextName.equals(getInheritedContextName()))
			return contextName;
		
		if (!stepContextNames.contains(contextName)) {
			stepContextNames.add(contextName);
		}
		return contextName;
	}
	
	public boolean useSameJSessionForSteps() {
		return new Boolean(EnginePropertiesManager.getProperty(PropertyName.SEQUENCE_STEPS_USE_SAME_JSESSION)).booleanValue();
	}
	
	public void onCachedResponse() {
		removeSequenceContext();
	}
	
	protected boolean executeNextStep(org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException
    {
		arborting = false;
		skipSteps = false;
    	nbAsyncThreadRunning = 0;
    	stepContextNames = new Vector<String>();
    	
    	// Retrieves HttpState of sequence
    	getHttpState();
		
    	// Retrieves HttpState of steps
    	getStepHttpState();
		
    	//workerRootElement = context.outputDocument.createElement("worker");
    	//context.outputDocument.getDocumentElement().appendChild(workerRootElement);
    	
    	try {
	    	if (hasSteps()) {
	    		Long t1 = System.currentTimeMillis();
	    		
	    		// Generate sequence working dom (see also appendStepNode(Step))
	    		for (int i=0; i < numberOfSteps(); i++) {
	        		if (isRunning()) {
        				executeNextStep((Step)getSteps().get(i), javascriptContext, scope);

	            		try {
	            			boolean hasWait = false;
	            			while (nbAsyncThreadRunning > 0) {
	            				// If this sequence contains ParallelSteps, waits until child's threads finish
	            				if (Engine.logBeans.isTraceEnabled())
	            					Engine.logBeans.trace("Sequence '"+ getName() + "' waiting...");
	            				Thread.sleep(500);
	            				hasWait = true;
	            			}
	            			if (hasWait) Engine.logBeans.trace("Sequence '"+ getName() + "' ends wait");
	            		} catch (InterruptedException e) {
	            			if (Engine.logBeans.isTraceEnabled())
	            				Engine.logBeans.trace("Sequence '"+ getName() + "' has been interrupted");
	            		}
	        		}
	        		if (skipSteps)
	        			break;
	    		}
	    		
	    		// Finally modify sequence working dom to output dom
	    		Element root = context.outputDocument.getDocumentElement();
	    		OutputFilter outputFilter = new OutputFilter(OutputOption.VisibleOnly);
	    		buildOutputDom(root, outputFilter);
	    		
	            Long t2 = System.currentTimeMillis();
	    		if (Engine.logBeans.isDebugEnabled())
	    			Engine.logBeans.debug("(Sequence) ended executing steps in :" + (t2-t1) + "ms");
	    	}
	    	return true;
    	}
    	finally {
    		arborting = false;
    		skipSteps = false;
    		if (Engine.logBeans.isDebugEnabled())
    			Engine.logBeans.debug("Sequence '"+ getName() + "' done");
    		
        	try {
        		// Cleans all steps
        		clean();
        	}
        	catch (Exception e) {}
        	
        	try {
        		// Removes contexts
        		removeContexts();
        	}
        	catch (Exception e) {
    			Engine.logBeans.error("Unexpected exception while removing context", e);
        	}
    	}
    }
	
	public static NodeList ouputDomView(NodeList nodeList, OutputFilter outputFilter) {
		if (nodeList != null) {
			int len = nodeList.getLength();
			if (len > 0) {
				Node node = nodeList.item(0);
				
				Document doc = node.getOwnerDocument();
				Element fake = doc.createElement("fake");
				Element root = (Element) doc.getDocumentElement().appendChild(fake);
				
				for (int i=0; i<len ; i++) {
					node = nodeList.item(i);
					root.appendChild(node.cloneNode(true)); // clone removes any userdata
				}
				
	    		buildOutputDom(root, outputFilter);
				
				doc.getDocumentElement().removeChild(fake);
				return fake.getChildNodes();
			}
			return nodeList;
		}
		return null;
	}
	
	private static void buildOutputDom(Element root, OutputFilter outputFilter) {
		DocumentTraversal traversal = (DocumentTraversal)root.getOwnerDocument();
		TreeWalker walker = traversal.createTreeWalker(root, NodeFilter.SHOW_ELEMENT, outputFilter, false);	    		
		traverseLevel(walker,null,"");
        outputFilter.doOutPut();
	}
	
	public enum OutputOption {
		VisibleOnly,
		UsefullOnly
	}
	
	public class OutputFilter implements NodeFilter {
		private final Map<Element, List<List<Element>>> map = new LinkedHashMap<Element, List<List<Element>>>();
		private OutputOption option;
		
		public OutputFilter(OutputOption option) {
			this.option = option;
		}
		
		private List<Element> getToRemoveList(Element key) {
			List<List<Element>> l = map.get(key);
			if (l == null) {
				l = new ArrayList<List<Element>>();
				l.add(new LinkedList<Element>());
				l.add(new LinkedList<Element>());
				map.put(key, l);
			}
			return l.get(0);
		}
		
		private List<Element> getToAddList(Element key) {
			List<List<Element>> l = map.get(key);
			if (l == null) {
				l = new ArrayList<List<Element>>();
				l.add(new LinkedList<Element>());
				l.add(new LinkedList<Element>());
				map.put(key, l);
			}
			return l.get(1);
		}
		
		private void doOutPut() {
    		Iterator<Entry<Element, List<List<Element>>>> it = map.entrySet().iterator();
    		while (it.hasNext()) {
    			Entry<Element, List<List<Element>>> entry = it.next();
    			Element key = entry.getKey();
    			List<List<Element>> value = entry.getValue();
    			List<Element> l0 = value.get(0);
    			List<Element> l1 = value.get(1);
    			//System.out.println("For " + key.getTagName());
    			//System.out.println("\ttobeAdded "+ l1);
    			//System.out.println("\ttobeRemoved "+ l0);
    			
    			Element firstToRemove = l0.size() > 0 ? l0.get(0):null;
    			for (Element e : l1) {
    				try {
    					key.insertBefore(e, firstToRemove);
    				}
    				catch (Exception ex) {
    					if (Engine.logBeans.isDebugEnabled()) {
    						Engine.logBeans.debug("(Sequence.OutputFilter) Could not move \""+ e.getTagName() 
    								+"\" element in \""+ key.getTagName() +"\" element", ex);
    					}
    				}
    			}
    			for (Element e : l0) {
    				if (e.getParentNode() != null) {
    					try {
    						key.removeChild(e);
        				}
        				catch (Exception ex) {
        					if (Engine.logBeans.isDebugEnabled()) {
        						Engine.logBeans.debug("(Sequence.OutputFilter) Could not remove \""+ e.getTagName() 
        								+"\" element from \""+ key.getTagName() +"\" element", ex);
        					}
        				}
    				}
    			}
    		}
		}
		
	    public short acceptNode(Node thisNode) { 
	    	if (thisNode.getNodeType() == Node.ELEMENT_NODE) { 
	    		Element e = (Element)thisNode;
	    		if (option.equals(OutputOption.VisibleOnly)) {
		    		if ("false".equals(e.getUserData(Step.NODE_USERDATA_OUTPUT))) {
		            	Element p = (Element) e.getParentNode();
		            	getToRemoveList(p).add(e);
		            	if (e.getTagName().equals("sequence") || e.getTagName().equals("transaction")) {
		            		return NodeFilter.FILTER_REJECT;
		            	}
		            	return NodeFilter.FILTER_SKIP;
		            }
	    		}
	    		else if (option.equals(OutputOption.UsefullOnly)) {
	    			if (e.getTagName().equals("sequence") || e.getTagName().equals("transaction")) {
		            	Element p = (Element) e.getParentNode();
		            	getToRemoveList(p).add(e);
		            	return NodeFilter.FILTER_REJECT;
	    			}
	    		}
	        }
	        return NodeFilter.FILTER_ACCEPT; 
	    }
	}
	
	private static void traverseLevel(TreeWalker walker, Element topParent, String indent) {
	    // describe current node:
	    Element current = (Element) walker.getCurrentNode();
	    //System.out.println(indent + "- " + ((Element) current).getTagName());
	    
	    OutputFilter outputFilter = (OutputFilter)walker.getFilter();
	    if (topParent != null) {
	    	Element parent = (Element) current.getParentNode();
	    	if (parent != null && !topParent.equals(parent))
	    		outputFilter.getToAddList(topParent).add(current);
	    }
	    
	    // traverse children:
	    for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
	      traverseLevel(walker, current, indent + '\t');
	    }
	    
	    // return position to the current (level up):
	    walker.setCurrentNode(current);
	}
	
	private void removeContexts() {
		// Remove transaction's context if needed
		removeTransactionContexts();
		
		//Removes sequence context
		removeSequenceContext();
		
		stepContextNames.clear();
	}
	
	private void removeTransactionContexts() {
		if (Engine.isEngineMode()) {
			if (useSameJSessionForSteps()) {
	    		String sessionID, contextName;
	    		if (Engine.logBeans.isDebugEnabled())
	    			Engine.logBeans.debug("(Sequence) Executing deletion of transaction's context for sequence \""+ getName() +"\"");
	    		sessionID = getSessionId();
	    		for (int i=0; i<stepContextNames.size(); i++) {
	    			contextName = (String)stepContextNames.get(i);
    				String contextID = sessionID + "_" + contextName;
	    			if (contextName.startsWith("Container-")) { // Only remove context automatically named
	    				if (Engine.logBeans.isDebugEnabled())
	    					Engine.logBeans.debug("(Sequence) Removing context \""+ contextID +"\"");
	    				Engine.theApp.contextManager.remove(contextID);
	    			}
	    			else {
	    				if (Engine.logBeans.isDebugEnabled())
	    					Engine.logBeans.debug("(Sequence) Keeping context \""+ contextID +"\"");
	    			}
	    		}
	    		if (Engine.logBeans.isDebugEnabled())
	    			Engine.logBeans.debug("(Sequence) Deletion of transaction's context for sequence \""+ getName() +"\" done");
			}
			else {
				if (transactionSessionId != null) {
					if (Engine.logBeans.isDebugEnabled())
						Engine.logBeans.debug("(Sequence) Executing deletion of transaction's context for sequence \""+ getName() +"\"");
					Engine.theApp.contextManager.removeAll(transactionSessionId);
					if (Engine.logBeans.isDebugEnabled())
						Engine.logBeans.debug("(Sequence) Deletion of transaction's context for sequence \""+ getName() +"\" done");
				}
			}
		}
	}
	
	private void removeSequenceContext() {
		if (Engine.isEngineMode()) {
			if (!context.isAsync) {
				if (Engine.logBeans.isDebugEnabled())
					Engine.logBeans.debug("(Sequence) Requires its context removal");
				context.requireRemoval(true);
			}
		}
	}
	
	private void executeNextStep(Step step, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
    	Step stepToExecute = getStepCopyToExecute(step);
    	if (stepToExecute != null) {
			stepToExecute.parent = this;
			stepToExecute.transactionContextMaintainer = this;
			stepToExecute.xpathApi = xpathApi;
			stepToExecute.httpState = ((stepToExecute instanceof BranchStep) ? getNewHttpState():getStepHttpState());
			stepToExecute.executedSteps.putAll(executedSteps);
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(Sequence) "+step+" ["+step.hashCode()+"] has been copied into "+stepToExecute+" ["+stepToExecute.hashCode()+"]");
    		stepToExecute.checkSymbols();
			
    		if (stepToExecute.execute(javascriptContext, scope)) {
    			//childrenSteps.put(new Long(stepToExecute.priority), stepToExecute.executeTimeID);
    			childrenSteps.put(stepToExecute.executeTimeID, new Long(stepToExecute.priority));
       			executedSteps.putAll(stepToExecute.executedSteps);
    		}
    		else {
    			stepToExecute.cleanCopy();
    		}
    	}
   		currentChildStep++;
    }
    
    public synchronized void setCurrentStep(Step step) {
   		currentStep = step;
    }
    
    private Step getStepCopyToExecute(Step step) throws EngineException {
		step.checkSubLoaded();
		
		Step stepToExecute = null;
		if (step.isEnable()) {
			Object ob = null;
			try {
				ob = step.copy();
			} catch (CloneNotSupportedException e) {
				throw new EngineException("Unable to get a copy of step \""+ step.getName()+"\" ("+step+")",e);
			}
			stepToExecute = (Step)ob;
		}
		return stepToExecute;
	}

	public Document createDOM() throws EngineException {
        Document doc = null;
        try {
	        if (requester == null)
	        	doc = new DefaultRequester().createDomWithNoXMLDeclaration(getEncodingCharSet());
	        else
	        	doc = ((GenericRequester)requester).createDomWithNoXMLDeclaration(getEncodingCharSet());
			Element rootElement = doc.createElement("document");
			doc.appendChild(rootElement);
        }
        catch (Exception e) {}
		return doc;
    }
	
	public void appendStepNode(Step step) throws EngineException {
		if (Thread.currentThread() instanceof AsynchronousStepThread) {
			AsynchronousStepThread thread = (AsynchronousStepThread) Thread.currentThread();
			thread.wakeTurn(step);
		}

		synchronized (this) {
			Node stepNode = step.getStepNode();
			if (stepNode != null) {
				/*if (step.isOutput()) {
					Node node = null;
					Element stepParentElement = findParentStepElement(step);
					if (stepNode.getNodeType() == Node.ELEMENT_NODE) {
						String sCopy = ((Element) stepNode).getAttribute("step_copy");
						boolean isCopy = sCopy != null && sCopy.equals("true");
						if (isCopy) {
							NodeList list = ((Element) stepNode).getChildNodes();
							if (list != null) {
								int len = list.getLength();
								for (int i = 0 ; i < len ; i++) {
									Node child = list.item(i);
									if ((child != null) && ((child.getNodeType() == Node.ELEMENT_NODE) || (child.getNodeType() == Node.TEXT_NODE))) {
										node = context.outputDocument.importNode(child, true);
										stepParentElement.appendChild(node);
									}
								}
							}
							NamedNodeMap map = ((Element)stepNode).getAttributes();
							if (map != null) {
								int len = map.getLength();
								for (int i = 0 ; i < len ; i++) {
									Node child = map.item(i);
									if ((child != null) && ((child.getNodeType() == Node.ATTRIBUTE_NODE))) {
										stepParentElement.setAttribute(child.getNodeName(),child.getNodeValue());
									}
								}
							}

							stepParentElement.removeAttribute("step_id");
							stepParentElement.removeAttribute("step_copy");
							node = stepParentElement;
						} else {
							node = context.outputDocument.importNode(stepNode, true);
							((Element) node).removeAttribute("step_id");
							stepParentElement.appendChild(node);
						}
					} else if (stepNode.getNodeType() == Node.ATTRIBUTE_NODE) {
						node = context.outputDocument.importNode(stepNode, true);
						stepParentElement.setAttributeNode((Attr) node);
					}

					if (node != null) {
						appendedStepElements.put(step.executeTimeID, node);
					}
				}*/
				
				workerElementMap.put(step.executeTimeID, stepNode);
				
//				Element stepParentElement = findParentStepElement(step);
//				if (stepParentElement != null) {
//					if (stepNode.getNodeType() == Node.ELEMENT_NODE) {
//						stepParentElement.appendChild(stepNode);
//					} else if (stepNode.getNodeType() == Node.ATTRIBUTE_NODE) {
//						stepParentElement.setAttributeNode((Attr) stepNode);
//					}
//				}
				if (step.isXmlOrOutput()) {
					Element stepParentElement = findParentStepElement(step);
					if (stepParentElement != null) {
						if (!step.isOutput() && stepNode.getNodeType() == Node.ELEMENT_NODE) {
							boolean recurse = !(step instanceof StepWithExpressions);
							// set output mode userdata (used by the TreeWalker OutputFilter)
							setOutputUserData((Element) stepNode, "false", recurse);
						}
						
						if (step instanceof XMLCopyStep) {
							NodeList children = stepNode.getChildNodes();
							for (int i=0; i<children.getLength(); i++) {
								Node copied = children.item(i).cloneNode(true);
								// set again user data because clone does not preserve it
								setOutputUserData(copied, String.valueOf(step.isOutput()), true);
								append(stepParentElement, copied);
							}
						}
						else {
							append(stepParentElement, stepNode);
						}
					}
				}
			}
		}
	}
    
	private static Node setOutputUserData(Node node, Object value, boolean recurse) {
		if (node != null) {
			// set output mode as userdata (element or attribute)
			node.setUserData(Step.NODE_USERDATA_OUTPUT, value, null);
			
			// recurse on element child nodes only
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (recurse && node.hasChildNodes()) {
					NodeList list = node.getChildNodes();
					for (int i=0; i<list.getLength(); i++) {
						setOutputUserData(list.item(i), value, recurse);
					}
				}
			}
		}
		return node;
	}
	
	private static void append(Element parent, Node node) {
		if (parent != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				parent.appendChild(node);
			} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
				parent.setAttributeNode((Attr) node);
			}
		}
	}
	
	public synchronized void flushStepDocument(String executeTimeID , Document doc) {
		Element stepElement = findStepElement(executeTimeID);
		if (stepElement == null) {
			stepElement = context.outputDocument.getDocumentElement();
		}
		
		Node imported = context.outputDocument.importNode(doc.getDocumentElement(), true);
		setOutputUserData(imported, stepElement.getUserData(Step.NODE_USERDATA_OUTPUT), true);
		stepElement.appendChild(imported);
	}

	@Override
	public String getXsdTypePrefix() {
		return "";
	}

	@Override
	public String getXsdTypePrefix(DatabaseObject parentObject) {
		return "";
	}

	@Override
	public String getXsdExtractPrefix() {
		return getName() + "_";
	}
	
	protected Element findStepElement(String executeTimeID) {
		Element stepElement = (Element)workerElementMap.get(executeTimeID);
		if (stepElement == null) {
			//stepElement = workerRootElement;
			stepElement = context.outputDocument.getDocumentElement();
		}
		return stepElement;
	}

	private Element findParentStepElement(Step step) {
		Step parentStep = step;
		try {
			do {
				parentStep = (Step)parentStep.getParent();
			} while (!parentStep.isXmlOrOutput());
			
		}
		catch (ClassCastException e) {
			//return workerRootElement;
			return context.outputDocument.getDocumentElement();
		}
		
		Element parentStepElement = (Element)workerElementMap.get(parentStep.executeTimeID);
		if (!parentStep.isOutput()) {
			//parentStepElement.setAttribute("step_output", "false");
			setOutputUserData(parentStepElement, "false", false);
		}
		
		if (parentStepElement == null) {
			return findParentStepElement(parentStep);
		}
		return parentStepElement;
	}
	
	transient private EventListenerList sequenceListeners = new EventListenerList();
    
    public void addSequenceListener(SequenceListener sequenceListener) {
    	sequenceListeners.add(SequenceListener.class, sequenceListener);
    }
    
    public void removeSequenceListener(SequenceListener sequenceListener) {
    	sequenceListeners.remove(SequenceListener.class, sequenceListener);
    }
    
    public void fireDataChanged(SequenceEvent sequenceEvent) {
        // Guaranteed to return a non-null array
        Object[] listeners = sequenceListeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
            //if (listeners[i] == EngineListener.class) {
        	if (listeners[i] == SequenceListener.class) {
                ((SequenceListener) listeners[i+1]).dataChanged(sequenceEvent);
            }
        }
    }
	
    transient private EventListenerList stepListeners = new EventListenerList();
    
    public void addStepListener(StepListener stepListener) {
    	stepListeners.add(StepListener.class, stepListener);
    }
    
    public void removeStepListener(StepListener stepListener) {
    	stepListeners.remove(StepListener.class, stepListener);
    }
    
    public void fireStepMoved(StepEvent stepEvent) {
        Object[] listeners = stepListeners.getListenerList();
        for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
        	if (listeners[i] == StepListener.class) {
                ((StepListener) listeners[i+1]).stepMoved(stepEvent);
            }
        }
    }
    
    public void fireStepCopied(StepEvent stepEvent) {
        Object[] listeners = stepListeners.getListenerList();
        for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
        	if (listeners[i] == StepListener.class) {
                ((StepListener) listeners[i+1]).stepCopied(stepEvent);
            }
        }
    }

    @Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
		
		if (VersionUtils.compare(version, "4.6.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
			String objectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propName, Node.ELEMENT_NODE));
			
			Element propVarDef = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "variablesDefinition");
			if (propVarDef != null) {
				propVarDef.setAttribute("name", "orderedVariables");
				hasChanged = true;
				Engine.logBeans.warn("[Sequence] The object \""+objectName+"\" has been updated to version 4.6.0 (property \"variablesDefinition\" changed to \"orderedVariables\")");
			}
		}
	}
    
    /* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#configure(org.w3c.dom.Element)
	 */
    @Override
    public void configure(Element element) throws Exception {
        super.configure(element);
    }
    
    /* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toXml(org.w3c.dom.Document)
	 */
    @Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        // Storing the sequence "handlePriorities" flag
        element.setAttribute("handlePriorities", new Boolean(handlePriorities).toString());
		
		return element;
	}
    
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep=super.getAllChildren();
		List<Step> steps=getAllSteps();		
		for(Step step:steps){
			rep.add(step);
		}
		List<RequestableVariable> variables=getAllVariables();	
		for(RequestableVariable variable:variables){
			rep.add(variable);
		}
		List<TestCase> testCases=getTestCasesList();	
		for(TestCase testCase:testCases){
			rep.add(testCase);
		}	
		return rep;
	}

	public boolean isIncludeResponseElement() {
		return includeResponseElement;
	}

	public void setIncludeResponseElement(boolean includeResponseElement) {
		this.includeResponseElement = includeResponseElement;
	}

	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement eSequence = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaElement());
		eSequence.setName(getName() + "Response");
		eSequence.setQName(new QName(schema.getTargetNamespace(), eSequence.getName()));

		XmlSchemaElement eResponse = null;
		if (isIncludeResponseElement()) {
			XmlSchemaComplexType tSequence = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
			eSequence.setSchemaType(tSequence);
	
			XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSequence());
			tSequence.setParticle(sequence);
	
			eResponse = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaElement());
			eResponse.setName("response");
	
			sequence.getItems().add(eResponse);
	
			SchemaMeta.setContainerXmlSchemaElement(eSequence, eResponse);
		}
		
		XmlSchemaComplexType cResponseDataType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
		cResponseDataType.setName(getName() + "ResponseData");
		XmlSchemaUtils.add(schema, cResponseDataType);
		XmlSchemaObjectCollection attributes = cResponseDataType.getAttributes();
		for (DOC_ATTR attr : DOC_ATTR.values()) {
			XmlSchemaAttribute attribute = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaAttribute());
			attribute.setName(attr.name());
			attribute.setSchemaTypeName(Constants.XSD_STRING);
			//attribute.setUse(XmlSchemaUtils.attributeUseRequired);
			attributes.add(attribute);
		}
		
		if (eResponse != null)
			eResponse.setSchemaTypeName(cResponseDataType.getQName());
		else
			eSequence.setSchemaTypeName(cResponseDataType.getQName());
		
		XmlSchemaComplexType cResponseDocType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
		cResponseDocType.setName(getName() + "ResponseType");
		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSequence());
		cResponseDocType.setParticle(sequence);
		XmlSchemaElement eDocument = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaElement());
		eDocument.setName("document");
		eDocument.setSchemaTypeName(new QName(schema.getTargetNamespace(), getComplexTypeAffectation().getLocalPart()));
		sequence.getItems().add(eDocument);
		XmlSchemaUtils.add(schema, cResponseDocType);

		XmlSchemaComplexType cRequestDataType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
		cRequestDataType.setName(getName() + "RequestData");
		XmlSchemaUtils.add(schema, cRequestDataType);

		XmlSchemaElement eRequest = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaElement());
		eRequest.setName(getName());
		eRequest.setQName(new QName(schema.getTargetNamespace(), eRequest.getName()));
		eRequest.setSchemaTypeName(cRequestDataType.getQName());
		XmlSchemaUtils.add(schema, eRequest);

		List<RequestableVariable> variables = getAllVariables();
		if (variables.size() > 0) {
			XmlSchemaSequence s = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSequence());
			cRequestDataType.setParticle(s);
			
			for (RequestableVariable variable : variables) {
				if (variable.isWsdl()) {
					XmlSchemaElement element = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaElement());
					s.getItems().add(element);
					element.setName(variable.getName());
					String description = variable.getDescription();
					if (description != null && description.length() > 0) {
						XmlSchemaAnnotation annotation = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaAnnotation());
						element.setAnnotation(annotation);
						XmlSchemaAppInfo appInfo = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaAppInfo());
						annotation.getItems().add(appInfo);
						appInfo.setMarkup(XMLUtils.asNodeList(description));
					}
					if (variable.isMultiValued()) {
						if (variable.isSoapArray()) {
							cRequestDataType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
							element.setType(cRequestDataType);
							XmlSchemaSequence items = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSequence());
							cRequestDataType.setParticle(items);
							element = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaElement());
							element.setName("item");
							items.getItems().add(element);
						}
						element.setMinOccurs(0);
						element.setMaxOccurs(Long.MAX_VALUE);
					}
					element.setSchemaTypeName(variable.getTypeAffectation());
				}
			}
		}
		
		return eSequence;
	}
	
	public QName getComplexTypeAffectation() {
		return new QName("", getName() + "ResponseData");
	}

	public boolean isGenerateSchema() {
		return true;
	}
	
	public boolean isGenerateElement() {
		return true;
	}
}
