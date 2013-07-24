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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToXMLAttributeStepAction.java $
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
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;

public class ChangeToXMLAttributeStepAction extends MyAbstractAction {

	public ChangeToXMLAttributeStepAction() {
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
    			// XML Element
    			if ((databaseObject != null) && (databaseObject instanceof XMLElementStep)) {
    				XMLElementStep elementStep = (XMLElementStep)databaseObject;
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
	        			
						// New XMLAttribute step
	        			XMLAttributeStep attributeStep = new XMLAttributeStep();
	        			
		        		if ( DatabaseObjectsManager.acceptDatabaseObjects(elementStep.getParent(), attributeStep) ) {
	        				// Set properties
		        			attributeStep.setOutput(elementStep.isOutput());
		        			attributeStep.setEnable(elementStep.isEnable());
		        			attributeStep.setComment(elementStep.getComment());
		        			attributeStep.setSourceDefinition(elementStep.getSourceDefinition());
		        			attributeStep.setNodeText(elementStep.getNodeText());
		        			attributeStep.setNodeName(elementStep.getNodeName());
		        			
		        			attributeStep.bNew = true;
		        			attributeStep.hasChanged = true;
							
							// Add new XMLAttribute step to parent
							DatabaseObject parentDbo = elementStep.getParent();
						
							parentDbo.add(attributeStep);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(attributeStep,elementStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(attributeStep,elementStep.priority);
						
							// Add new XMLAttribute step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,elementStep);
							treeParent.addChild(stepTreeObject);
							
			   				// Delete XMLElement step
							long oldPriority = elementStep.priority;
							elementStep.delete();
							attributeStep.getSequence().fireStepMoved(new StepEvent(attributeStep,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(attributeStep));
						} else {
							throw new EngineException("You cannot paste to a " + elementStep.getParent().getClass().getSimpleName() + " a database object of type " + attributeStep.getClass().getSimpleName());
						}
	        		}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change to XMLAttribute step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
