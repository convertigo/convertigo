/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.StringUtils;

@DboCategoryInfo(
		getCategoryId = "TestCase",
		getCategoryName = "Test case",
		getIconClassCSS = "convertigo-action-newTransactionTestCase"
		)
public class TestCase extends DatabaseObject implements IVariableContainer, IContainerOrdered {

	private static final long serialVersionUID = 8119436229667565326L;

	transient private XMLVector<XMLVector<Long>> orderedVariables = new XMLVector<XMLVector<Long>>();

	transient private List<TestCaseVariable> vVariables = new LinkedList<TestCaseVariable>();
	transient private List<TestCaseVariable> vAllVariables = null;

	public TestCase() {
		super();
		databaseType = "TestCase";

		orderedVariables = new XMLVector<XMLVector<Long>>();
		orderedVariables.add(new XMLVector<Long>());
	}

	@Override
	public TestCase clone() throws CloneNotSupportedException {
		TestCase clonedObject = (TestCase) super.clone();
		clonedObject.vVariables = new LinkedList<TestCaseVariable>();
		clonedObject.vAllVariables = null;
		return clonedObject;
	}

	public XMLVector<XMLVector<Long>> getOrderedVariables() {
		return orderedVariables;
	}

	public void setOrderedVariables(XMLVector<XMLVector<Long>> orderedVariables) {
		this.orderedVariables = orderedVariables;
	}

	@Override
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject instanceof TestCaseVariable) {
			addVariable((TestCaseVariable) databaseObject, after);
		}
		else {
			throw new EngineException("You cannot add to a test case a database object of type " + databaseObject.getClass().getName());
		}		
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
	}

	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof TestCaseVariable)
			removeVariable((TestCaseVariable) databaseObject);
		else throw new EngineException("You cannot remove from a test case a database object of type " + databaseObject.getClass().getName());
	}

	public List<TestCaseVariable> getVariables(boolean reset) {
		if (reset)
			vAllVariables = null;
		return getVariables();
	}

	public List<TestCaseVariable> getVariables() {
		checkSubLoaded();    	
		if ((vAllVariables == null) || hasChanged)
			vAllVariables = getAllVariables();
		return vAllVariables;
	}

	public List<TestCaseVariable> getAllVariables() {
		checkSubLoaded();
		return sort(vVariables);
	}

	public Variable getVariable(int index) {
		checkSubLoaded();
		try {
			return vVariables.get(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	public Variable getVariable(String variableName) {
		checkSubLoaded();
		for(TestCaseVariable variable : vVariables)
			if (variable.getName().equals(variableName))
				return variable;
		return null;
	}

	@Override
	public Object getVariableValue(String requestedVariableName) throws EngineException {
		Object value = null, valueToPrint = null;
		TestCaseVariable testCaseVariable = (TestCaseVariable)getVariable(requestedVariableName);
		if (testCaseVariable != null) {
			value = testCaseVariable.getValueOrNull();
			valueToPrint = Visibility.Logs.printValue(testCaseVariable.getVisibility(), value);
			if (Engine.logBeans.isDebugEnabled()) {
				if ((value != null) && (value instanceof String))
					Engine.logBeans.debug("Default value: " + requestedVariableName + " = \"" + valueToPrint + "\"");
				else
					Engine.logBeans.debug("Default value: " + requestedVariableName + " = " + valueToPrint);
			}

			if (value == null && testCaseVariable.isRequired()) {
				throw new EngineException("Variable named \""+requestedVariableName+"\" is required for testcase \""+getName()+"\"");
			}
		}
		return value;
	}

	public boolean hasVariables() {
		checkSubLoaded();
		return vVariables.size() > 0;
	}

	public int numberOfVariables() {
		checkSubLoaded();
		return vVariables.size();
	}

	public void addVariable(TestCaseVariable variable, Long after) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vVariables, variable.getName(), variable.bNew);
		variable.setName(newDatabaseObjectName);
		vVariables.add(variable);
		variable.setParent(this);
		insertOrderedVariable(variable, after);
	}

	public void addVariable(TestCaseVariable variable) throws EngineException {
		addVariable(variable, null);
	}

	private void insertOrderedVariable(Variable variable, Long after) {
		List<Long> ordered = orderedVariables.get(0);
		int size = ordered.size();

		if (ordered.contains(variable.priority))
			return;

		if (after == null) {
			after = 0L;
			if (size > 0)
				after = ordered.get(ordered.size()-1);
		}

		int order = ordered.indexOf(after);
		ordered.add(order+1, variable.priority);
		hasChanged = !isImporting;
	}

	public void removeVariable(TestCaseVariable variable) {
		checkSubLoaded();
		vVariables.remove(variable);
		variable.setParent(null);
		removeOrderedVariable(variable.priority);
	}

	private void removeOrderedVariable(Long value) {
		Collection<Long> ordered = orderedVariables.get(0);
		ordered.remove(value);
		hasChanged = true;
	}

	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, priority);
	}

	private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
		List<Long> ordered = null;
		Long value = Long.valueOf(databaseObject.priority);

		if (databaseObject instanceof Variable)
			ordered = orderedVariables.get(0);

		if (!ordered.contains(value))
			return;
		int pos = ordered.indexOf(value);
		if (pos == 0)
			return;

		if (before == null)
			before = ordered.get(pos-1);
		int pos1 = ordered.indexOf(before);

		ordered.add(pos1, value);
		ordered.remove(pos+1);
		hasChanged = true;
	}

	private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
		List<Long> ordered = null;
		long value = databaseObject.priority;

		if (databaseObject instanceof Variable)
			ordered = orderedVariables.get(0);

		if (!ordered.contains(value))
			return;
		int pos = ordered.indexOf(value);
		if (pos+1 == ordered.size())
			return;

		if (after == null)
			after = ordered.get(pos+1);
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
			else throw new EngineException("Corrupted variable for test case \""+ getName() +"\". Variable \""+ ((Variable)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
		}
		else return super.getOrder(object);
	}

	public void importRequestableVariables(RequestableObject requestable) throws EngineException {
		if (!(requestable instanceof IVariableContainer))
			return;

		IVariableContainer container = (IVariableContainer)requestable;

		int size = container.numberOfVariables();
		for (int i=0; i<size; i++) {
			RequestableVariable variable = (RequestableVariable)container.getVariable(i);
			if (variable != null) {
				String variableName = variable.getName();
				if (getVariable(variableName) == null) {
					if (!StringUtils.isNormalized(variableName))
						throw new EngineException("Variable name is not normalized : \""+variableName+"\".");

					TestCaseVariable testCaseVariable = variable.isMultiValued() ? new TestCaseMultiValuedVariable():new TestCaseVariable();
					testCaseVariable.setName(variableName);
					testCaseVariable.setDescription(variable.getDescription());
					testCaseVariable.setRequired(variable.isRequired());
					testCaseVariable.setValueOrNull(variable.getValueOrNull());
					testCaseVariable.setVisibility(variable.getVisibility());
					addVariable(testCaseVariable);

					testCaseVariable.bNew = true;
					testCaseVariable.hasChanged = true;
					hasChanged = true;
				}
			}
		}
	}

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep=super.getAllChildren();
		List<TestCaseVariable> variables=getAllVariables();
		for(TestCaseVariable variable:variables){
			rep.add(variable);
		}

		return rep;
	}

	@Override
	public FolderType getFolderType() {
		return FolderType.TESTCASE;
	}

	public static TestCase getTestCase(Map<String, Object> request, String projectName) throws EngineException {
		TestCase tc = null;
		if (request.containsKey("__testcase")) {
			projectName = ((String[]) request.getOrDefault("__project", new String[] {projectName}))[0];
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, true);
			if (request.containsKey("__sequence")) {
				tc = project.getSequenceByName(((String[]) request.get("__sequence"))[0]).getTestCaseByName(((String[]) request.get("__testcase"))[0]);
			} else {
				Connector connector = request.containsKey("__connector") ? project.getConnectorByName(((String[]) request.get("__connector"))[0]) : project.getDefaultConnector();
				Transaction transaction = request.containsKey("__transaction") ? connector.getTransactionByName(((String[]) request.get("__transaction"))[0]) : connector.getDefaultTransaction();
				if (transaction instanceof TransactionWithVariables) {
					tc = ((TransactionWithVariables) transaction).getTestCaseByName(((String[]) request.get("__testcase"))[0]);
				}
			}
		}
		return tc;
	}

	public static TestCase getTestCase(HttpServletRequest request, String projectName) throws EngineException {
		TestCase tc = null;
		if (request.getParameter("__testcase") != null) {
			projectName = request.getParameter("__project") != null ? request.getParameter("__project") : projectName;
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, true);
			if (request.getParameter("__sequence") != null) {
				tc = project.getSequenceByName(request.getParameter("__sequence")).getTestCaseByName(request.getParameter("__testcase"));
			} else {
				Connector connector = request.getParameter("__connector") != null ? project.getConnectorByName(request.getParameter("__connector")) : project.getDefaultConnector();
				Transaction transaction = request.getParameter("__transaction") != null ? connector.getTransactionByName(request.getParameter("__transaction")) : connector.getDefaultTransaction();
				if (transaction instanceof TransactionWithVariables) {
					tc = ((TransactionWithVariables) transaction).getTestCaseByName(request.getParameter("__testcase"));
				}
			}
		}
		return tc;
	}
}
