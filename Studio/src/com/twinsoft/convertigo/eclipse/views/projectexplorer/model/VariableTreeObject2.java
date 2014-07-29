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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.EngineException;

public class VariableTreeObject2 extends DatabaseObjectTreeObject implements IOrderableTreeObject {

	public boolean isChildOfJavelinTransaction = false;
	public boolean isMultiValued = false;
	
	public VariableTreeObject2(Viewer viewer, Variable object) {
		this(viewer, object, false);
	}
	
	public VariableTreeObject2(Viewer viewer, Variable object, boolean inherited) {
		super(viewer, object, inherited);
		isMultiValued = object.isMultiValued();
	}

	@Override
	public Variable getObject(){
		return (Variable) super.getObject();
	}
	
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isChildOfJavelinTransaction")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isChildOfJavelinTransaction));
		}
		if (name.equals("isMultiValued")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isMultiValued));
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
						}
						
					}
				}	
				
			} catch (EngineException e) {
				ConvertigoPlugin.logException(e, "Unable to rename the variable references of '" + databaseObject.getName() + "'!");
			}

		}
	}	
	
	private void updateNameReference(boolean isSameValue, boolean shouldUpdate, Variable var, Object newValue) throws EngineException{
		if (isSameValue && shouldUpdate) {
			var.setName(newValue.toString());
			hasBeenModified(true);
			viewer.refresh();
			getDescriptors();// refresh editors (e.g labels in combobox)
		}
	}
}
