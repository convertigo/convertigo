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
import com.twinsoft.convertigo.beans.steps.IfExistStep;
import com.twinsoft.convertigo.beans.steps.IfExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IsInThenElseStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;

public class ChangeToIfExistThenElseStepAction extends MyAbstractAction {

	public ChangeToIfExistThenElseStepAction() {
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
    			//For IfExist step
    			if ((databaseObject != null) && (databaseObject instanceof IfExistStep)) {
    				IfExistStep ifStep = (IfExistStep)databaseObject;
					List<Step> list = ifStep.getSteps();
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
	        			IfExistThenElseStep ifThenElseStep = new IfExistThenElseStep();
	        			ifThenElseStep.setSourceDefinition(ifStep.getSourceDefinition());
						ifThenElseStep.bNew = true;
						ifThenElseStep.hasChanged = true;
						
						// Add new IfThenElseStep step to parent
						DatabaseObject parentDbo = ifStep.getParent();
						parentDbo.add(ifThenElseStep);
						
						// Set correct order
						if (parentDbo instanceof StepWithExpressions)
							((StepWithExpressions)parentDbo).insertAtOrder(ifThenElseStep,ifStep.priority);
						else if (parentDbo instanceof Sequence)
							((Sequence)parentDbo).insertAtOrder(ifThenElseStep,ifStep.priority);
						
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
							ClipboardAction.cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
							for (int i = 0 ; i < ConvertigoPlugin.clipboardManager2.objects.length ; i++) {
								ConvertigoPlugin.clipboardManager2.cutAndPaste(ConvertigoPlugin.clipboardManager2.objects[i], thenTreeObject);
							}
							ConvertigoPlugin.clipboardManager2.reset();
						}
						
		   				// Delete If step
						long oldPriority = ifStep.priority;
		   				ifStep.delete();
		   				
		   				// Simulate move of If to IfThenElse
		   				ifThenElseStep.getSequence().fireStepMoved(new StepEvent(ifThenElseStep,String.valueOf(oldPriority)));
						
	        			parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifThenElseStep));
	        		}
				}
    			
    			//For IsInThenElse step
    			if ((databaseObject != null) && (databaseObject instanceof IsInThenElseStep)) {
    				IsInThenElseStep isInThenElseStep = (IsInThenElseStep)databaseObject;
    				if (isInThenElseStep.hasThenElseSteps()) {
    					
    					TreeParent treeParent = treeObject.getParent();
						DatabaseObjectTreeObject parentTreeObject = null;
						if (treeParent instanceof DatabaseObjectTreeObject)
							parentTreeObject = (DatabaseObjectTreeObject)treeParent;
						else
							parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
						
		        		if (parentTreeObject != null) {
							// New IfExistThenElse step
		        			IfExistThenElseStep ifThenElseStep = new IfExistThenElseStep();
		        			ifThenElseStep.setSourceDefinition(isInThenElseStep.getSourceDefinition());
							ifThenElseStep.bNew = true;
							ifThenElseStep.hasChanged = true;
							
							// Add new IfExistThenElse step to parent
							DatabaseObject parentDbo = isInThenElseStep.getParent();
							parentDbo.add(ifThenElseStep);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(ifThenElseStep,isInThenElseStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(ifThenElseStep,isInThenElseStep.priority);
							
							// Add Then/Else steps
							ThenStep thenStep = isInThenElseStep.getThenStep();
							ElseStep elseStep = isInThenElseStep.getElseStep();
							thenStep.bNew = true;
							elseStep.bNew = true;
							ifThenElseStep.addStep(thenStep);
							ifThenElseStep.addStep(elseStep);
						
							// Add new IfExistThenElse step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,ifThenElseStep);
							treeParent.addChild(stepTreeObject);
							stepTreeObject.addChild(new StepTreeObject(explorerView.viewer,thenStep));
							stepTreeObject.addChild(new StepTreeObject(explorerView.viewer,elseStep));
							
							// Delete IsInThenElse step
							long oldPriority = isInThenElseStep.priority;
							isInThenElseStep.delete();
			   				
			   				// Simulate move of IsInThenElse to IfExistThenElse step
			   				ifThenElseStep.getSequence().fireStepMoved(new StepEvent(ifThenElseStep,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifThenElseStep));
		        		}
    				}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to IfExistThenElse step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
