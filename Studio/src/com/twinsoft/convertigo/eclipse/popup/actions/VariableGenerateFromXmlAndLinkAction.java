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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.util.StepUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class VariableGenerateFromXmlAndLinkAction extends MyAbstractAction {

	public VariableGenerateFromXmlAndLinkAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();
    			if ((databaseObject != null) && ((databaseObject instanceof StepVariable))) {
    				StepVariable stepVariable = (StepVariable)databaseObject;
    				RequestableStep requestableStep = (RequestableStep)stepVariable.getParent();
    				if (requestableStep != null) {
        				DatabaseObjectTreeObject parentDboTreeObject = ((DatabaseObjectTreeObject)treeObject).
    							getParentDatabaseObjectTreeObject().getParentDatabaseObjectTreeObject();
        				
    					RequestableObject requestableObject = null;
    					if (requestableStep instanceof SequenceStep) {
    						requestableObject = ((SequenceStep)requestableStep).getTargetSequence();
    					}
    					else if (requestableStep instanceof TransactionStep) {
    						requestableObject = ((TransactionStep)requestableStep).getTargetTransaction();
    					}
    					
    					if ((requestableObject != null) && (requestableObject instanceof IVariableContainer)) {
							String variableName = stepVariable.getName();
							IVariableContainer container = (IVariableContainer)requestableObject;
							RequestableVariable variable = (RequestableVariable)container.getVariable(variableName);
							
							// generate dom model
							Document document = null;
							try {
								String description = variable.getDescription();
								document = XMLUtils.parseDOMFromString(description);
							}
							catch (Exception e) {}
							
							if (document != null) {
				        		Element root = document.getDocumentElement();
				        		if (root != null) {
				        			// create step's structure from dom
				        			DatabaseObject parentObject = requestableStep.getParent();
				        			Step step = StepUtils.createStepFromXmlDomModel(parentObject, root);
	    							
				        			// add step's structure to parent of requestableStep
				        			if (parentObject instanceof Sequence) {
	    								Sequence parentSequence = (Sequence)parentObject;
	    								parentSequence.addStep(step);
	    								parentSequence.insertAtOrder(step,requestableStep.priority);
	    							}
	    							else {
	    								StepWithExpressions parentSwe = (StepWithExpressions)parentObject;
	    								parentSwe.addStep(step);
	    								parentSwe.insertAtOrder(step,requestableStep.priority);
	    							}
				        			
				        			// set source definition of variable
				        			XMLVector<String> sourceDefinition = new XMLVector<String>();
				        			sourceDefinition.add(String.valueOf(step.priority));
				        			sourceDefinition.add(".");
									stepVariable.setSourceDefinition(sourceDefinition);
									stepVariable.hasChanged = true;
									
									
	    							// Reload parent dbo in tree
	    							explorerView.reloadTreeObject(parentDboTreeObject);
	    							
	    							// Select variable dbo in tree
	    							explorerView.objectSelected(new CompositeEvent(databaseObject));
				        		}
    						}
    					}
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to generate and link variable!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
