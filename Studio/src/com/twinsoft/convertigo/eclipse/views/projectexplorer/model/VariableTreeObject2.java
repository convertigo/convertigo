/*
* Copyright (c) 2014 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.EngineException;

public class VariableTreeObject2 extends DatabaseObjectTreeObject implements IOrderableTreeObject {

	public VariableTreeObject2(Viewer viewer, Variable object) {
		this(viewer, object, false);
	}
	
	public VariableTreeObject2(Viewer viewer, Variable object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public Variable getObject(){
		return (Variable) super.getObject();
	}
	
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		return super.testAttribute(target, name, value);
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			String propertyName = treeObjectEvent.propertyName;
			propertyName = ((propertyName == null) ? "" : propertyName);
			
			// If a variable name has changed
			if (propertyName.equals("name")) {
				handlesBeanNameChanged(treeObjectEvent);
			}
			else if (propertyName.equals("visibility")) {
				if (treeObject.equals(this)) {
					reloadDescriptors();
				}
			}
		}
	}

	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		int update = treeObjectEvent.update;
		
		// Updates variables references
		if (update != TreeObjectEvent.UPDATE_NONE) {
			boolean isLocalProject = false;
			boolean isSameValue = false;
			boolean shouldUpdate = false;
			try {
				
				if (getObject() instanceof Variable) {
					Variable variable = (Variable)getObject();
					
					if (databaseObject instanceof RequestableVariable) {
						
						isLocalProject = variable.getProject().equals(databaseObject.getProject());
						isSameValue = variable.getName().equals(oldValue);
						shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
						
						// Verify if parent of databaseObject is a transaction
						if (databaseObject.getParent() instanceof Transaction) {
							Transaction transaction = (Transaction) databaseObject.getParent();
							
							// Case of rename for Call Transaction
							if (variable.getParent() instanceof TransactionStep) {
								TransactionStep transactionStep = (TransactionStep) variable.getParent();
								
								if (transactionStep.getSourceTransaction().equals(transaction.getProject()+"."+transaction.getConnector()+"."+transaction.getName())) {
									updateNameReference(isSameValue, shouldUpdate, variable, newValue);									
								}
							}
							
							/*
							 * propagation to testCases of variable renaming in a transaction
							 */
							if (variable.getParent() instanceof TransactionWithVariables) {
								propagateVariableRename(true, true, treeObjectEvent, ((TransactionWithVariables)transaction).getTestCasesList(), transaction.getName());
							}
						}
						
						// Verify if parent of databaseObject is a sequence
						if (databaseObject.getParent() instanceof Sequence) {
							Sequence sequence = (Sequence) databaseObject.getParent();
						
							//Case of rename for Call Sequence
							if (variable.getParent() instanceof SequenceStep) {
								SequenceStep sequenceStep = (SequenceStep) variable.getParent();
								
								if (sequenceStep.getSourceSequence().equals(sequence.getProject()+"."+sequence.getName())) {
									updateNameReference(isSameValue, shouldUpdate, variable, newValue);
								}
							}
							
							/*
							 * propagation to testCases of variable renaming in a sequence
							 */
							if (variable.getParent() instanceof Sequence) {
								propagateVariableRename(true, true, treeObjectEvent, sequence.getTestCasesList(), sequence.getName());								
							}
						}
					}
				}	
				
			} catch (EngineException e) {
				ConvertigoPlugin.logException(e, "Unable to rename the variable references of '" + databaseObject.getName() + "'!");
			}
		}
	}	

	private void propagateVariableRename(boolean isSameValue, boolean shouldUpdate,TreeObjectEvent treeObjectEvent, List<TestCase> list, String parentName) {
		try {
			/*
			 * get the testcases list
			 */
			if (list != null) {
				TestCase testCase;
				TestCaseVariable testCaseVar;
				
				/*
				 * locate testcase belonging to same sequence
				 */
				for (int i=0; i<list.size(); i++) {
					if (((testCase = (TestCase)list.get(i)) != null) && (testCase.getParent().getName().equals(parentName))) {
						/*
						 * get the testcase variables list
						 */
						List<TestCaseVariable> varList = testCase.getAllVariables();
						for (int v=0; v<varList.size(); v++) {
							if ((testCaseVar = (TestCaseVariable)varList.get(v)) != null) {
								/*
								 * if variables have same name, then update the value
								 */
								if (testCaseVar.getName().equalsIgnoreCase((String)treeObjectEvent.oldValue)) {
									testCaseVar.setName((String)treeObjectEvent.newValue);
									testCaseVar.hasChanged = true;
									
									updateNameReference(isSameValue, shouldUpdate, testCaseVar, (String)treeObjectEvent.newValue);
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e) {
			ConvertigoPlugin.logException(e, "Unable to propagate variable " + (String)treeObjectEvent.oldValue + " renaming to testcase in '" + parentName + "'!");
		}
	}
	
	private void updateNameReference(boolean isSameValue, boolean shouldUpdate, Variable var, Object newValue) throws EngineException{
		if (isSameValue && shouldUpdate) {		// should be !isSameValue instead of isSameValue to reflect change
			var.setName(newValue.toString());
			hasBeenModified(true);
			viewer.refresh();
			getDescriptors();// refresh editors (e.g labels in combobox)
		}
	}
}
