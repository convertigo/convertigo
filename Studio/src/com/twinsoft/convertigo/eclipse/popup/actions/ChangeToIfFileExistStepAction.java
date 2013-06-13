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
 * $URL: http://sourceus/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToIfStepAction.java $
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

public class ChangeToIfFileExistStepAction extends MyAbstractAction {

	public ChangeToIfFileExistStepAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof IfFileExistThenElseStep)) {
    				IfFileExistThenElseStep ifFileExistThenElseStep = (IfFileExistThenElseStep)databaseObject;
    				if (ifFileExistThenElseStep.hasThenElseSteps()) {
    					ThenStep thenStep = ifFileExistThenElseStep.getThenStep();
    					List<Step> list = thenStep.getSteps();
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
    						// New jIf step
    						IfFileExistStep ifFileExistStep = new IfFileExistStep(ifFileExistThenElseStep.getCondition());
    						ifFileExistStep.bNew = true;
    						ifFileExistStep.hasChanged = true;
    						
    						// Add new jIf step to parent
    						DatabaseObject parentDbo = ifFileExistThenElseStep.getParent();
    						parentDbo.add(ifFileExistStep);
    						
    						// Set correct order
    						if (parentDbo instanceof StepWithExpressions)
    							((StepWithExpressions)parentDbo).insertAtOrder(ifFileExistStep,ifFileExistThenElseStep.priority);
    						else if (parentDbo instanceof Sequence)
    							((Sequence)parentDbo).insertAtOrder(ifFileExistStep,ifFileExistThenElseStep.priority);
    						
    						// Add new jIf step in Tree
    						StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,ifFileExistStep);
    						treeParent.addChild(stepTreeObject);

    						// Cut/Paste steps under jIf step
    						if (selectedPaths.length > 0) {
    							new ClipboardAction(ConvertigoPlugin.clipboardManagerDND).cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
	    						for (int i = 0 ; i < ConvertigoPlugin.clipboardManagerDND.objects.length ; i++) {
	    							ConvertigoPlugin.clipboardManagerDND.cutAndPaste(ConvertigoPlugin.clipboardManagerDND.objects[i], stepTreeObject);
	    						}
	    						ConvertigoPlugin.clipboardManagerDND.reset();
    						}
    						
    		   				// Delete IfThenElse step
    						long oldPriority = ifFileExistThenElseStep.priority;
    						ifFileExistThenElseStep.delete();
    		   				
    		   				// Simulate move of IfThenElse to If
    		   				ifFileExistStep.getSequence().fireStepMoved(new StepEvent(ifFileExistStep,String.valueOf(oldPriority)));
    						
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifFileExistStep));
		        		}
    				}
    			}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to IfFileExist step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
