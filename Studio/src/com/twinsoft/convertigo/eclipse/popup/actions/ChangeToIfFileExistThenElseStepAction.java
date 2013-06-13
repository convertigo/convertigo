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
 * $URL: http://sourceus/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToIfThenElseStepAction.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
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
import com.twinsoft.convertigo.beans.steps.IfFileExistStep;
import com.twinsoft.convertigo.beans.steps.IfFileExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ChangeToIfFileExistThenElseStepAction extends MyAbstractAction {

	public ChangeToIfFileExistThenElseStepAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof IfFileExistStep)) {
    				IfFileExistStep ifFileExistStep = (IfFileExistStep)databaseObject;
					List<Step> list = ifFileExistStep.getSteps();
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
						// New IfFileExistThenElseStep step
	        			IfFileExistThenElseStep ifFileExistThenElseStep = new IfFileExistThenElseStep(ifFileExistStep.getCondition());
	        			ifFileExistThenElseStep.bNew = true;
	        			ifFileExistThenElseStep.hasChanged = true;
						
						// Add new IfFileExistThenElseStep step to parent
						DatabaseObject parentDbo = ifFileExistStep.getParent();
						parentDbo.add(ifFileExistThenElseStep);
						
						// Set correct order
						if (parentDbo instanceof StepWithExpressions)
							((StepWithExpressions)parentDbo).insertAtOrder(ifFileExistThenElseStep,ifFileExistStep.priority);
						else if (parentDbo instanceof Sequence)
							((Sequence)parentDbo).insertAtOrder(ifFileExistThenElseStep,ifFileExistStep.priority);
						
						// Add Then/Else steps
						ThenStep thenStep = new ThenStep();
						thenStep.bNew = true;
						ifFileExistThenElseStep.addStep(thenStep);
						ElseStep elseStep = new ElseStep();
						elseStep.bNew = true;
						ifFileExistThenElseStep.addStep(elseStep);
					
						// Add new IfFileExistThenElseStep step in Tree
						StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,ifFileExistThenElseStep);
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
						long oldPriority = ifFileExistStep.priority;
		   				ifFileExistStep.delete();
		   				
		   				// Simulate move of If to IfThenElse
		   				ifFileExistThenElseStep.getSequence().fireStepMoved(new StepEvent(ifFileExistThenElseStep,String.valueOf(oldPriority)));
						
	        			parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifFileExistThenElseStep));
	        		}
				}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to IfFileExistThenElse step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
