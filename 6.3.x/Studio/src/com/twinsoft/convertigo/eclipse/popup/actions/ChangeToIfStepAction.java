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
import com.twinsoft.convertigo.beans.steps.IfStep;
import com.twinsoft.convertigo.beans.steps.IfThenElseStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreePath;

public class ChangeToIfStepAction extends MyAbstractAction {

	public ChangeToIfStepAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof IfThenElseStep)) {
    				IfThenElseStep ifThenElseStep = (IfThenElseStep)databaseObject;
    				if (ifThenElseStep.hasThenElseSteps()) {
    					ThenStep thenStep = ifThenElseStep.getThenStep();
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
    						IfStep ifStep = new IfStep(ifThenElseStep.getCondition());
    						ifStep.bNew = true;
    						ifStep.hasChanged = true;
    						
    						// Add new jIf step to parent
    						DatabaseObject parentDbo = ifThenElseStep.getParent();
    						parentDbo.add(ifStep);
    						
    						// Set correct order
    						if (parentDbo instanceof StepWithExpressions)
    							((StepWithExpressions)parentDbo).insertAtOrder(ifStep,ifThenElseStep.priority);
    						else if (parentDbo instanceof Sequence)
    							((Sequence)parentDbo).insertAtOrder(ifStep,ifThenElseStep.priority);
    						
    						// Add new jIf step in Tree
    						StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,ifStep);
    						treeParent.addChild(stepTreeObject);

    						// Cut/Paste steps under jIf step
    						if (selectedPaths.length > 0) {
	    						ClipboardAction.cut(explorerView, selectedPaths, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_STEP);
	    						for (int i = 0 ; i < ConvertigoPlugin.clipboardManager2.objects.length ; i++) {
	    							ConvertigoPlugin.clipboardManager2.cutAndPaste(ConvertigoPlugin.clipboardManager2.objects[i], stepTreeObject);
	    						}
	    						ConvertigoPlugin.clipboardManager2.reset();
    						}
    						
    		   				// Delete IfThenElse step
    						long oldPriority = ifThenElseStep.priority;
    						ifThenElseStep.delete();
    		   				
    		   				// Simulate move of IfThenElse to If
    						ifStep.getSequence().fireStepMoved(new StepEvent(ifStep,String.valueOf(oldPriority)));
    						
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(ifStep));
		        		}
    				}
    			}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to jIf step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
