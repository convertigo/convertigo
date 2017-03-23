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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.IfExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IsInStep;
import com.twinsoft.convertigo.beans.steps.IsInThenElseStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ChangeToIsInThenElseStepAction extends MyAbstractAction {

	public ChangeToIsInThenElseStepAction() {
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.popup.actions.MyAbstractAction#run()
	 */
	@Override
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
    			// For IsInStep
    			if ((databaseObject != null) && (databaseObject instanceof IsInStep)) {
    				IsInStep isIn = (IsInStep)databaseObject;
					List<Step> list = isIn.getSteps();
					TreePath[] selectedPaths = new TreePath[list.size()];
					for (int i=0; i<list.size(); i++) {
						StepTreeObject stepTreeObject = (StepTreeObject)explorerView.findTreeObjectByUserObject(list.get(i));
						selectedPaths[i] = new TreePath(stepTreeObject);
					}
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
						// New IfThenElseStep step
	        			IsInThenElseStep ifThenElseStep = new IsInThenElseStep();
	        			
	        			// Set properties
	        			ifThenElseStep.setSourceDefinition(isIn.getSourceDefinition());
	        			ifThenElseStep.setTestDefinition(isIn.getTestDefinition());
	        			ifThenElseStep.setComment(isIn.getComment());
	        			ifThenElseStep.setCondition(isIn.getCondition());
	        			ifThenElseStep.setEnabled(isIn.isEnabled());
	        			ifThenElseStep.setOutput(isIn.isOutput());
	        			ifThenElseStep.setName(isIn.getName());
	        			
						ifThenElseStep.bNew = true;
						ifThenElseStep.hasChanged = true;
						
						// Add new IfThenElseStep step to parent
						DatabaseObject parentDbo = isIn.getParent();
						parentDbo.add(ifThenElseStep);
						
						// Set correct order
						if (parentDbo instanceof StepWithExpressions)
							((StepWithExpressions)parentDbo).insertAtOrder(ifThenElseStep,isIn.priority);
						else if (parentDbo instanceof Sequence)
							((Sequence)parentDbo).insertAtOrder(ifThenElseStep,isIn.priority);
						
						// Add Then/Else steps
						ThenStep thenStep = new ThenStep();
						thenStep.bNew = true;
						ifThenElseStep.addStep(thenStep);
						ElseStep elseStep = new ElseStep();
						elseStep.bNew = true;
						ifThenElseStep.addStep(elseStep);
					
						// Add new IfThenElseStep step in Tree
						StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,ifThenElseStep);
						treeParent.addChild(stepTreeObject);
						StepTreeObject thenTreeObject = new StepTreeObject(explorerView.viewer,thenStep);
						stepTreeObject.addChild(thenTreeObject);
						StepTreeObject elseTreeObject = new StepTreeObject(explorerView.viewer,elseStep);
						stepTreeObject.addChild(elseTreeObject);
						
						// Cut/Paste steps under Then step
						if (selectedPaths.length > 0) {
							new ClipboardAction(ConvertigoPlugin.clipboardManagerDND).cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
							for (int i = 0 ; i < ConvertigoPlugin.clipboardManagerDND.objects.length ; i++) {
								ConvertigoPlugin.clipboardManagerDND.cutAndPaste(ConvertigoPlugin.clipboardManagerDND.objects[i], thenTreeObject);
							}
							ConvertigoPlugin.clipboardManagerDND.reset();
						}
						
		   				// Delete If step
						long oldPriority = isIn.priority;
						// Save oldName
						String oldName = isIn.getName();
						// Now delete
						isIn.delete();    		   				
						// Set name after deletion
						ifThenElseStep.setName(oldName);						
		   				
		   				// Simulate move of If to IfThenElse
		   				ifThenElseStep.getSequence().fireStepMoved(new StepEvent(ifThenElseStep,String.valueOf(oldPriority)));
						
	        			parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifThenElseStep));
	        		}
				}
    			
    			//For IfExistThenElse step
    			if ((databaseObject != null) && (databaseObject instanceof IfExistThenElseStep)) {
    				IfExistThenElseStep ifExistThenElse = (IfExistThenElseStep)databaseObject;
    				if (ifExistThenElse.hasThenElseSteps()) {
    					
    					TreeParent treeParent = treeObject.getParent();
						DatabaseObjectTreeObject parentTreeObject = null;
						if (treeParent instanceof DatabaseObjectTreeObject)
							parentTreeObject = (DatabaseObjectTreeObject)treeParent;
						else
							parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
						
		        		if (parentTreeObject != null) {
							// New IsInThenElse step
		        			IsInThenElseStep isInThenElse = new IsInThenElseStep();
		        			
		        			// Set properties
		        			isInThenElse.setSourceDefinition(ifExistThenElse.getSourceDefinition());
		        			isInThenElse.setComment(ifExistThenElse.getComment());
		        			isInThenElse.setCondition(ifExistThenElse.getCondition());
		        			isInThenElse.setEnabled(ifExistThenElse.isEnabled());
		        			isInThenElse.setOutput(ifExistThenElse.isOutput());
		        			isInThenElse.setName(ifExistThenElse.getName());
		        			
		        			isInThenElse.bNew = true;
		        			isInThenElse.hasChanged = true;
							
							// Add new IsInThenElse step to parent
							DatabaseObject parentDbo = ifExistThenElse.getParent();
							parentDbo.add(isInThenElse);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(isInThenElse,ifExistThenElse.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(isInThenElse,ifExistThenElse.priority);
							
							// Add Then/Else steps
							ThenStep thenStep = ifExistThenElse.getThenStep();
							ElseStep elseStep = ifExistThenElse.getElseStep();
							thenStep.bNew = true;
							elseStep.bNew = true;
							isInThenElse.addStep(thenStep);
							isInThenElse.addStep(elseStep);
						
							// Add new IsInThenElse step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer, isInThenElse);							
							treeParent.addChild( new StepTreeObject(explorerView.viewer, isInThenElse));
							stepTreeObject.addChild(new StepTreeObject(explorerView.viewer, thenStep));
							stepTreeObject.addChild(new StepTreeObject(explorerView.viewer, elseStep));
 
							// Delete ifExistThenElse step
							long oldPriority = ifExistThenElse.priority;
							// Save oldName
							String oldName = ifExistThenElse.getName();
							// Now delete
							ifExistThenElse.delete();    		   				
							// Set name after deletion
							isInThenElse.setName(oldName);						
			   				
			   				// Simulate move of ifExistThenElse to IsInThenElse step
							isInThenElse.getSequence().fireStepMoved(new StepEvent(isInThenElse,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(isInThenElse));
		        		}
    				}
				}
			}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to IsInThenElse step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
