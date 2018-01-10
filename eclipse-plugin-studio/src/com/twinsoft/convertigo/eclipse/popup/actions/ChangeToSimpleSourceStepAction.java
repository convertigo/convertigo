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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToSimpleSourceStepAction.java $
 * $Author: nicolasa $
 * $Revision: 31165 $
 * $Date: 2012-07-20 17:45:54 +0200 (ven., 20 juil. 2012) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.SimpleSourceStep;
import com.twinsoft.convertigo.beans.steps.SourceStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;

public class ChangeToSimpleSourceStepAction extends MyAbstractAction {

	public ChangeToSimpleSourceStepAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof SourceStep)) {
    				SourceStep jSourceStep = (SourceStep)databaseObject;    											
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
						// New jSimpleSource step
						SimpleSourceStep jSimpleSourceStep = new SimpleSourceStep();
						
						// Set properties
						jSimpleSourceStep.setSourceDefinition(jSourceStep.getSourceDefinition());
						jSimpleSourceStep.setComment(jSourceStep.getComment());
						jSimpleSourceStep.setVariableName(jSourceStep.getVariableName());
						jSimpleSourceStep.setEnabled(jSourceStep.isEnabled());
						jSimpleSourceStep.setOutput(jSourceStep.isOutput());
						jSimpleSourceStep.setName(jSourceStep.getName());
						
						jSimpleSourceStep.bNew = true;
						jSimpleSourceStep.hasChanged = true;
						
						// Add new jSource step to parent
						DatabaseObject parentDbo = jSourceStep.getParent();
						parentDbo.add(jSimpleSourceStep);
						
						// Set correct order
						if (parentDbo instanceof StepWithExpressions)
							((StepWithExpressions)parentDbo).insertAtOrder(jSimpleSourceStep,jSourceStep.priority);
						else if (parentDbo instanceof Sequence)
							((Sequence)parentDbo).insertAtOrder(jSimpleSourceStep,jSourceStep.priority);
						
						// Add new jSimpleSource step in Tree
						StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,jSimpleSourceStep);
						treeParent.addChild(stepTreeObject);
						
		   				// Delete jSource step
						long oldPriority = jSourceStep.priority;
						jSourceStep.delete();
		   				
		   				// Simulate move of jSource to jSimpleSource step
						jSimpleSourceStep.getSequence().fireStepMoved(new StepEvent(jSimpleSourceStep,String.valueOf(oldPriority)));
						
	        			parentTreeObject.hasBeenModified(true);
		                explorerView.reloadTreeObject(parentTreeObject);
		                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(jSimpleSourceStep));
	        		}
				}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to jSimpleSource step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
