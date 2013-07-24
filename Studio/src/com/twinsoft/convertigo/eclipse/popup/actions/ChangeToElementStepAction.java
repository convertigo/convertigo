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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToElementStepAction.java $
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
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;

public class ChangeToElementStepAction extends MyAbstractAction {

	public ChangeToElementStepAction() {
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
    			
    			// Attribute
    			if ((databaseObject != null) && (databaseObject instanceof AttributeStep)) {
    				AttributeStep attributeStep = (AttributeStep)databaseObject;
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
	        			
						// New Element step
	        			ElementStep elementStep = new ElementStep();
	        			
		        		if ( DatabaseObjectsManager.acceptDatabaseObjects(attributeStep.getParent(), elementStep) ) {
	        				// Set properties
		        			elementStep.setOutput(attributeStep.isOutput());
		        			elementStep.setEnable(attributeStep.isEnable());
		        			elementStep.setComment(attributeStep.getComment());
		        			elementStep.setExpression(attributeStep.getExpression());
		        			elementStep.setNodeText(attributeStep.getNodeText());
		        			elementStep.setNodeName(attributeStep.getNodeName());
		        			
		        			elementStep.bNew = true;
		        			elementStep.hasChanged = true;
							
							// Add new Element step to parent
							DatabaseObject parentDbo = attributeStep.getParent();
						
							parentDbo.add(elementStep);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,attributeStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,attributeStep.priority);
						
							// Add new Element step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,attributeStep);
							treeParent.addChild(stepTreeObject);
							
			   				// Delete Attribute step
							long oldPriority = attributeStep.priority;
							attributeStep.delete();
							elementStep.getSequence().fireStepMoved(new StepEvent(elementStep,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(elementStep));
						} else {
							throw new EngineException("You cannot paste to a " + attributeStep.getParent().getClass().getSimpleName() + " a database object of type " + elementStep.getClass().getSimpleName());
						}
	        		}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to Element step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
