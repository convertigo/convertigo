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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ParameterUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public abstract class TransactionWithVariables extends Transaction implements IVariableContainer, ITestCaseContainer, IContainerOrdered {
    
	private static final long serialVersionUID = -7348846395918560818L;
	
	transient private List<RequestableVariable> vVariables = new ArrayList<RequestableVariable>();
	transient private List<RequestableVariable> vAllVariables = null;
	
	transient private List<TestCase> vTestCases = new ArrayList<TestCase>();
	
    /**
     * Constructs a TransactionWithVariables object.
     */
    public TransactionWithVariables() {
        super();
        
		orderedVariables = new XMLVector<XMLVector<Long>>();
		orderedVariables.add(new XMLVector<Long>());
    }
    
    @Override
	public TransactionWithVariables clone() throws CloneNotSupportedException {
		TransactionWithVariables clonedObject = (TransactionWithVariables) super.clone();
		clonedObject.variables = new HashMap<String, Object>();
		clonedObject.vVariables = new ArrayList<RequestableVariable>();
		clonedObject.vTestCases = new ArrayList<TestCase>();
		clonedObject.vAllVariables = null;
		clonedObject.originalVariables = null;
		clonedObject.needRestoreVariables = false;
		return clonedObject;
	}
    
	@Override
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        if (databaseObject instanceof RequestableVariable) {
            addVariable((RequestableVariable) databaseObject, after);
        }
        else if (databaseObject instanceof TestCase) {
            addTestCase((TestCase) databaseObject);
        }
        else {
            super.add(databaseObject);
        }		
	}
    
    @Override
    public void add(DatabaseObject databaseObject) throws EngineException {
    	add(databaseObject, null);
    }
	
    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof RequestableVariable) {
            removeVariable((RequestableVariable) databaseObject);
        }
        else if (databaseObject instanceof TestCase) {
            removeTestCase((TestCase) databaseObject);
        }
        else {
        	super.remove(databaseObject);
        }
    }
    
    public void addVariable(RequestableVariable variable, Long after) throws EngineException {
    	checkSubLoaded();
    	
		String newDatabaseObjectName = getChildBeanName(vVariables, variable.getName(), variable.bNew);
		variable.setName(newDatabaseObjectName);
        
		vVariables.add(variable);
        
		variable.setParent(this);
		
		insertOrderedVariable(variable, after);
    }
    
    public void addVariable(RequestableVariable variable) throws EngineException {
    	addVariable(variable, null);
    }
    
    private void insertOrderedVariable(Variable variable, Long after) {
    	XMLVector<Long> ordered = orderedVariables.get(0);
    	int size = ordered.size();
    	
    	Long value = Long.valueOf(variable.priority);
    	
    	if (ordered.contains(value))
    		return;
    	
    	if (after == null) {
    		after = 0L;
    		if (size > 0)
    			after = (Long)ordered.lastElement();
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, value);
    	hasChanged = !isImporting;
    }
    
    public void removeVariable(RequestableVariable variable) {
    	checkSubLoaded();
    	
    	vVariables.remove(variable);
    	variable.setParent(null);
    	
    	Long value = Long.valueOf(variable.priority);
        removeOrderedVariable(value);
        
        hasChanged = true;
    }
    
    private void removeOrderedVariable(Long value) {
    	XMLVector<Long> ordered = orderedVariables.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, priority);
	}
    
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	XMLVector<Long> ordered = null;
    	Long value = Long.valueOf(databaseObject.priority);
    	
    	if (databaseObject instanceof Variable)
    		ordered = orderedVariables.get(0);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = (Long)ordered.get(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.add(pos1, value);
    	ordered.remove(pos+1);
    	hasChanged = true;
    }
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	XMLVector<Long> ordered = null;
    	Long value = Long.valueOf(databaseObject.priority);
    	
    	if (databaseObject instanceof Variable)
    		ordered = orderedVariables.get(0);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = (Long)ordered.get(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.add(pos1+1, value);
    	ordered.remove(pos);
    	hasChanged = true;
    }
    
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Variable)
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Variable)
			decreaseOrder(databaseObject,null);
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
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof Variable) {
        	List<Long> ordered = orderedVariables.get(0);
        	long time = ((Variable)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted variable for transaction \""+ getName() +"\". Variable \""+ ((Variable)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        } else return super.getOrder(object);
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
    			v.add(variable.getName());
    			v.add(variable.getDescription());
    			v.add(variable.getDefaultValue());
    			v.add(variable.isWsdl());
    			v.add(variable.isMultiValued());
    			v.add(variable.isPersonalizable());
    			v.add(variable.isCachedKey());
    			
    			xmlv.add(v);
    		}
    	}
    	return xmlv;
    }
    
    @Deprecated
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
		
		for (int i=0; i < vVariables.size(); i++) {
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
	
	/** Holds value of property orderedVariables. */
	transient private XMLVector<XMLVector<Long>> orderedVariables = new XMLVector<XMLVector<Long>>();
	
	/** Stores value of property orderedVariables. */
	//private transient XMLVector originalVariablesDefinition = null;
	private transient List<RequestableVariable> originalVariables = null;
    
	/** Getter for property orderedVariables.
	 * @return Value of property orderedVariables.
	 */
	public XMLVector<XMLVector<Long>> getOrderedVariables() {
		return orderedVariables;
	}
    
	/** Setter for property orderedVariables.
	 * @param orderedVariables New value of property orderedVariables.
	 */
	public void setOrderedVariables(XMLVector<XMLVector<Long>> orderedVariables) {
		this.orderedVariables = orderedVariables;
	}
    
	/**
	 * Resets variables to original value
	 */
	protected void restoreVariables() {
		checkSubLoaded();
		
		if (needRestoreVariables) {
			if (originalVariables != null) {
				vVariables = new ArrayList<RequestableVariable>(originalVariables);
				originalVariables = null;
			}
			needRestoreVariables = false;
		}
	}

	private void setDynamicVariable(String variableName, Object variableValue, Boolean multi, String variableMethod) {
		checkSubLoaded();
		
		needRestoreVariables = true;
		if (originalVariables == null) {
			originalVariables = new ArrayList<RequestableVariable>(vVariables);
		}
		
		RequestableVariable variable = (RequestableVariable) getVariable(variableName);
		try {
			// variable definition does not exist, creates it
			if (variable == null) {
				if ("POST".equals(variableMethod) || "GET".equals(variableMethod)) {
					variable = (multi ? new RequestableHttpMultiValuedVariable():new RequestableHttpVariable());
					((RequestableHttpVariable) variable).setHttpMethod(variableMethod);
					((RequestableHttpVariable) variable).setHttpName(variableMethod);
				} else {
					variable = new RequestableVariable();
				}
				variable.bNew = true;
				variable.hasChanged = true;
			}
			
			// override existing variable definition with dynamic one
			variable.setName(variableName);
			variable.setDescription(variableName);
			variable.setValueOrNull(variableValue);
			variable.setWsdl(Boolean.FALSE);
			variable.setPersonalizable(Boolean.FALSE);
			variable.setCachedKey(Boolean.TRUE);
			
			if (variable.bNew)
				addVariable(variable);
		}
		catch (EngineException e) {
		}
	}
	
	public Object getParameterValue(String parameterName) throws EngineException {
		Object variableValue = null;
		
		int variableVisibility = getVariableVisibility(parameterName);
		
		// Transaction parameter
		variableValue = variables.get(parameterName);
		if (variableValue != null)
			Engine.logBeans.trace("(TransactionWithVariables) parameter value: " + Visibility.Logs.printValue(variableVisibility,variableValue));

		// Otherwise context parameter
		if (variableValue == null && context != null) {
			variableValue = (context.get(parameterName) == null ? null : context.get(parameterName));
			if (variableValue != null)
				Engine.logBeans.trace("(TransactionWithVariables) context value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
		}

		// Otherwise default transaction parameter value
		if (variableValue == null) {
			variableValue = getVariableValue(parameterName);
			if (variableValue != null)
				Engine.logBeans.trace("(TransactionWithVariables) default value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
		}

		if (variableValue == null) {
			Engine.logBeans.trace("(TransactionWithVariables) none value found");
		} else if (variableValue.getClass().isArray()) {
			Engine.logBeans.trace("(TransactionWithVariables) transform the array value as a collection");
			if (variableValue.getClass().getComponentType().isPrimitive()) {
				int len = Array.getLength(variableValue);
				Collection<Object> newVariableValue = new ArrayList<Object>(len);
				for (int i = 0; i < len; i++) {
					newVariableValue.add(Array.get(variableValue, i));
				}
				variableValue = newVariableValue;
			} else {
				variableValue = Arrays.asList((Object[]) variableValue);
			}
		}
		
		return variableValue;
	}
	
	public String getParameterStringValue(String parameterName) throws EngineException {
		return ParameterUtils.toString(getParameterValue(parameterName));
	}
	
	@Override
	public Object getVariableValue(String requestedVariableName) throws EngineException {
		// Request parameter value (see parseInputDocument())
		Object value = ((variables == null) ? null: variables.get(requestedVariableName));
		
		// If no value is found, find the default value and return it.
		if (value == null) {
			Object valueToPrint = null;
			RequestableVariable variable = (RequestableVariable)getVariable(requestedVariableName);
			if (variable != null) {
				value = variable.getValueOrNull();// new 5.0.3 (may return null)
				valueToPrint = Visibility.Logs.printValue(variable.getVisibility(), value);
				if (Engine.logBeans.isDebugEnabled()) {
					if ((value != null) && (value instanceof String))
						Engine.logBeans.debug("Default value: " + requestedVariableName + " = \"" + valueToPrint + "\"");
					else
						Engine.logBeans.debug("Default value: " + requestedVariableName + " = " + valueToPrint);
				}
				
				if (value == null && variable.isRequired()) {
					throw new EngineException("Variable named \""+requestedVariableName+"\" is required for transaction \""+getName()+"\"");
				}
			}
		}
		
		if ((value != null) && (value instanceof List)) {
			List<?> lValue = GenericUtils.cast(value);
			value = lValue.toArray(new Object[lValue.size()]);
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
	
	protected transient boolean needRestoreVariables = false;
	
	@Override
	public void parseInputDocument(Context context) throws EngineException {
		super.parseInputDocument(context);
		if (context.inputDocument != null && Engine.logContext.isInfoEnabled()) {
			Document printDoc = (Document) Visibility.Logs.replaceVariables(getVariablesList(), context.inputDocument);
			XMLUtils.logXml(printDoc, Engine.logContext, "Input document");
		}
		
		NodeList variableNodes = context.inputDocument.getElementsByTagName("variable");
		Element variableNode;
		Attr valueAttrNode;
		int len = variableNodes.getLength();
		String variableName, variableValue, variableMethod;
		RequestableVariable variable;
		boolean bMulti;
		
		// TODO: handle persistent variables

		variables.clear();
		
		for (int i = 0 ; i < len ; i++) {
			bMulti = false;
			variableNode = (Element) variableNodes.item(i);
			variableName = variableNode.getAttribute("name");
			variableValue = (variableNode.hasAttribute("value") ? variableNode.getAttribute("value") : null);
			valueAttrNode = variableNode.getAttributeNode("value");
			
			variableMethod = null;
			
			// Test case for transaction
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
					Engine.logBeans.warn("Transaction: there's no testcase named '"+variableValue+"' for '"+ getName() +"' transaction");
				}
				continue;
			}
			// May be a dynamic transaction variable definition
			else if ((variableName.indexOf(Parameter.DynamicVariablePost.getName()) == 0) || (variableName.indexOf(Parameter.DynamicVariableGet.getName()) == 0)) {
				bMulti = variableNode.getAttribute("multi").equalsIgnoreCase("true");
				
				if (variableName.indexOf(Parameter.DynamicVariablePost.getName()) == 0) {
					variableName = variableName.substring(Parameter.DynamicVariablePost.getName().length());
					variableMethod = "POST";
				}
				else if (variableName.indexOf(Parameter.DynamicVariableGet.getName()) == 0) {
					variableName = variableName.substring(Parameter.DynamicVariableGet.getName().length());
					variableMethod = "GET";
				}
				
				// retrieve variable definition
				variable = (RequestableVariable)getVariable(variableName);
				
				if (variableMethod != null) {
					setDynamicVariable(variableName, variableValue, Boolean.valueOf(bMulti), variableMethod);
				}
			}
			// Serialized variables definition
			else {
				variable = (RequestableVariable)getVariable(variableName);
			}
			
			// Structured value?
			Object scopeValue = null;
			if (getProject().isStrictMode()) {
				scopeValue = (variableValue != null) ? variableValue : variableNode.getChildNodes();
			}
			else {
				if (variableValue != null) {
					scopeValue = variableValue;
				}
				else {
					String sValue = XMLUtils.prettyPrintElement(variableNode, true, false);
					sValue = sValue.replaceAll("<variable name=\""+variableName+"\">", "");
					sValue = sValue.replaceAll("</variable>", "");
					scopeValue = sValue;
				}
			}
			
			// Multivalued variable ?
			if ((variable != null) && (variable.isMultiValued())) {
				List<Object> current = GenericUtils.cast(variables.get(variableName));
				if (current == null) {
					current = new ArrayList<Object>();
					variables.put(variableName, current);
				}
				if (variableValue == null || valueAttrNode != null) {
					current.add(scopeValue);
				}
			} else {
				variables.put(variableName, scopeValue);
			}
		}
		
		// Enumeration of all transaction variables
		if (Engine.logBeans.isDebugEnabled())
			Engine.logBeans.debug("Transaction variables: " + (variables == null ? "none" : Visibility.Logs.replaceVariables(getVariablesList(), variables)));
	}
	
	@Override
	protected void initializeConnector(Context context) throws EngineException {
		// Initialize connector if transaction is not considered as a void transaction
		String value = getParameterStringValue(Parameter.VoidTransaction.getName());
		if (!"true".equals(value)) {
			super.initializeConnector(context);
		} else {
			Engine.logBeans.debug("(TransactionWithVariables) Connector won't be initialized");
		}
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
			if ((variableValue != null) && (variableValue instanceof String))
				Engine.logBeans.debug("(TransactionWithVariables) Declared but not provided transaction variable " + variableName + "=\"" + variableValueToPrint + "\" added to the scripting scope");
			else
				Engine.logBeans.debug("(TransactionWithVariables) Declared but not provided transaction variable " + variableName + "=" + variableValueToPrint + " added to the scripting scope");
		}

		for (String variableName2 : variables.keySet()) {
			if (variableName2.startsWith("__")) continue;
			variableVisibility = getVariableVisibility(variableName2);
			variableValue = getVariableValue(variableName2);
			jsObject = ((variableValue == null) ? null:org.mozilla.javascript.Context.toObject(variableValue, scope));
			scope.put(variableName2, scope, jsObject);
			variableValueToPrint = Visibility.Logs.printValue(variableVisibility,variableValue);
			if ((variableValue != null) && (variableValue instanceof String))
				Engine.logBeans.debug("(TransactionWithVariables) Provided transaction variable " + variableName2 + "=\"" + variableValueToPrint + "\" added (or overridden) to the scripting scope");
			else
				Engine.logBeans.debug("(TransactionWithVariables) Provided transaction variable " + variableName2 + "=" + variableValueToPrint + " added (or overridden) to the scripting scope");
		}
    }

	public String getRequestString(Context context) {
		checkSubLoaded();
		
		List<String> vVariables = new ArrayList<String>(variables.size());
		//Use authenticated user as cache key
		if ( isAuthenticatedUserAsCacheKey() )
			vVariables.add("userID="+context.getAuthenticatedUser());
		for (String variableName : variables.keySet()) {
			if (includeVariableIntoRequestString(variableName)) {
				String variableValueAsString = ParameterUtils.toString(variables.get(variableName));
				vVariables.add(variableName + "=" + variableValueAsString);
			}
		}
		
		if (bIncludeCertificateGroup) {
			try {
				CertificateManager certificateManager = ((HttpConnector) getParent()).certificateManager;
				certificateManager.collectStoreInformation(context);
				
				if ((certificateManager.keyStoreGroup == null) || (certificateManager.keyStoreGroup.length() == 0)) {
					vVariables.add("certificateGroup=" + certificateManager.keyStoreName);
				}
				else {
					vVariables.add("certificateGroup=" + certificateManager.keyStoreGroup);
				}
			}
			catch(EngineException e) {
				vVariables.add("certificateGroup=exception");
			}
		}
		
		Collections.sort(vVariables);
		
		String requestString = context.projectName + " " + context.transactionName + " " + vVariables.toString();
		
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
		String prefix = getXsdTypePrefix();
    	String xsdRequestData = null;
    	RequestableVariable variable = null;
    	var doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
    	var root = doc.createElement("xsd:complexType");
    	doc.appendChild(root);
    	root.setAttribute("name", prefix + getName() + "RequestData");
    	var seq = doc.createElement("xsd:sequence");
    	root.appendChild(seq);
		for (int i=0; i<numberOfVariables(); i++) {
			variable = (RequestableVariable)getVariable(i);
			if (variable.isWsdl()) {
				if (variable.isMultiValued()) {
					var el = doc.createElement("xsd:element");
					el.setAttribute("minOccurs", "1");
					el.setAttribute("maxOccurs", "1");
					el.setAttribute("name", variable.getName());
					seq.appendChild(el);
					var elAnnot = doc.createElement("xsd:annotation");
					el.appendChild(elAnnot);
					var elDoc = doc.createElement("xsd:documentation");
					elDoc.setTextContent(variable.getComment());
					elAnnot.appendChild(elDoc);
					var elAppinfo = doc.createElement("xsd:appinfo");
					elAppinfo.setTextContent(variable.getDescription());
					elAnnot.appendChild(elAppinfo);
					var elType = doc.createElement("xsd:complexType");
					el.appendChild(elType);
					var elSeq = doc.createElement("xsd:sequence");
					elType.appendChild(elSeq);
					var elItem = doc.createElement("xsd:element");
					elItem.setAttribute("minOccurs", "0");
					elItem.setAttribute("maxOccurs", "unbounded");
					elItem.setAttribute("name", "item");
					elItem.setAttribute("type", variable.getSchemaType());
					elSeq.appendChild(elItem);
				}
				else {
					var el = doc.createElement("xsd:element");
					el.setAttribute("minOccurs", "1");
					el.setAttribute("maxOccurs", "1");
					el.setAttribute("name", variable.getName());
					el.setAttribute("type", variable.getSchemaType());
					seq.appendChild(el);
					var elAnnot = doc.createElement("xsd:annotation");
					el.appendChild(elAnnot);
					var elDoc = doc.createElement("xsd:documentation");
					elDoc.setTextContent(variable.getComment());
					elAnnot.appendChild(elDoc);
					var elAppinfo = doc.createElement("xsd:appinfo");
					elAppinfo.setTextContent(variable.getDescription());
				}
			}
		}
		xsdRequestData = XMLUtils.prettyPrintDOM(root).replaceFirst(".*?(<xsd)","$1");
    	return xsdRequestData;
    }
    
	@Override
	protected String generateXsdResponseData(Document document, boolean extract) throws Exception {
    	StringEx sx = new StringEx(extract ? extractXsdType(document):generateWsdlType(document));
    	//sx.replace(getName() + "Response", getName() + "ResponseData");
    	sx.replace(getName() + "Response\"", getName() + "ResponseData\"");
    	sx.replaceAll("\"p_ns:", "\""+ getProject().getName() + "_ns:");
    	String xsdResponseData = "  " + sx.toString();
    	return xsdResponseData;
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
				Engine.logBeans.warn("[TransactionWithVariables] The object \""+objectName+"\" has been updated to version 4.6.0 (property \"variablesDefinition\" changed to \"orderedVariables\")");
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		String version = element.getAttribute("version");
		
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}

		if (VersionUtils.compare(version, "3.1.8") < 0) {
			int len = orderedVariables.size();
			XMLVector line;
			for (int i = 0 ; i < len ; i++) {
				line = (XMLVector) orderedVariables.get(i);
				if (line.size()>0) {
					// Include in WSDL by default
					line.add(Boolean.TRUE);
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("[TransactionWithVariables] The object \"" + getName() + "\" has been updated to version 3.1.8");
		}

		if (VersionUtils.compare(version, "3.2.4") < 0) {
			int len = orderedVariables.size();
			XMLVector line;
			for (int i = 0 ; i < len ; i++) {
				line = (XMLVector) orderedVariables.get(i);
				if (line.size()>0) {
					// Defaults to non multivalued variable
					line.add(3, Boolean.FALSE);
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("[TransactionWithVariables] The object \"" + getName() + "\" has been updated to version 3.2.4");
		}
		
		if (VersionUtils.compare(version, "4.2.0") < 0) {
			int len = orderedVariables.size();
			XMLVector line;
			for (int i = 0 ; i < len ; i++) {
				line = (XMLVector) orderedVariables.get(i);
				if (line.size()>0) {
					// Do not set as Personalizable by default
					line.add(4, Boolean.FALSE);
					// Sets description to variable name by default
					line.add(1, line.get(0));
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("[TransactionWithVariables] The object \"" + getName() + "\" has been updated to version 4.2.0");
		}
		
		if (VersionUtils.compare(version, "4.3.0") < 0) {
			int len = orderedVariables.size();
			XMLVector line;
			for (int i = 0 ; i < len ; i++) {
				line = (XMLVector) orderedVariables.get(i);
				if (line.size()>0) {
					// Set cached key
					line.add(6, Boolean.TRUE);
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("[TransactionWithVariables] The object \"" + getName() + "\" has been updated to version 4.3.0");
		}
		
	}

	/** Holds value of property bIncludeCertificateGroup. */
	private boolean bIncludeCertificateGroup = false;

	/** Getter for property bIncludeCertificateGroup.
     * @return Value of property bIncludeCertificateGroup.
     */
    public boolean includeCertificateGroup() {
        return this.bIncludeCertificateGroup;
    }
    
    /** Setter for property bIncludeCertificateGroup.
     * @param billable New value of property bIncludeCertificateGroup.
     */
    public void setIncludeCertificateGroup(boolean bIncludeCertificateGroup) {
        this.bIncludeCertificateGroup = bIncludeCertificateGroup;
    }
    
    @Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep=super.getAllChildren();
		List<RequestableVariable> variables=getVariablesList();		
		for(Variable variable:variables){
			rep.add(variable);
		}
		List<TestCase> testCases=getTestCasesList();	
		for(TestCase testCase:testCases){
			rep.add(testCase);
		}		
		return rep;
	}
    
	@Override
	protected XmlSchemaComplexType addSchemaRequestDataType(XmlSchema xmlSchema) {
		XmlSchemaComplexType xmlSchemaComplexType = super.addSchemaRequestDataType(xmlSchema);
		int len = numberOfVariables();
		if (len > 0) {
			XmlSchemaSequence xmlSchemaSequence = new XmlSchemaSequence();
			for (int i = 0; i < len; i++) {
				RequestableVariable variable = (RequestableVariable)getVariable(i);
				if (variable.isWsdl()) {
					XmlSchemaElement xmlSchemaElement = new XmlSchemaElement();
					xmlSchemaElement.setName(variable.getName());
					
					if (variable.isMultiValued()) {
						xmlSchemaElement.setMaxOccurs(Long.MAX_VALUE);
					}
					
					//String[] qn = variable.getSchemaType().split(":");
					//QName typeName = new QName(xmlSchema.getNamespaceContext().getNamespaceURI(qn[0]), qn[1], qn[0]);
					//xmlSchemaElement.setSchemaTypeName(typeName);
					xmlSchemaElement.setSchemaTypeName(variable.getTypeAffectation());
					
					addSchemaCommentAnnotation(xmlSchemaElement, variable.getComment(), variable.getDescription());
					
					xmlSchemaSequence.getItems().add(xmlSchemaElement);
				}
			}
			xmlSchemaComplexType.setParticle(xmlSchemaSequence);
		}
		return xmlSchemaComplexType;
	}

}
