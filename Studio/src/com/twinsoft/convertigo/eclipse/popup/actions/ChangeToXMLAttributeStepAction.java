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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
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

	public void selectionChanged(IAction action, ISelection selection) {
		try {
			boolean enable = false;
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject dbo = (DatabaseObject)treeObject.getObject();
				DatabaseObject dboParent = dbo.getParent();
				if (DatabaseObjectsManager.acceptDatabaseObjects(dboParent, new XMLAttributeStep())) {
					enable = (dbo instanceof AttributeStep) ||
									(dbo instanceof XMLElementStep);
				}
			}
			action.setEnabled(enable);
		}
		catch (Exception e) {}
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
	        			
						// New XMLAttribute step
	        			XMLAttributeStep xmlAttributeStep = new XMLAttributeStep();
	        			
		        		if ( DatabaseObjectsManager.acceptDatabaseObjects(attributeStep.getParent(), xmlAttributeStep) ) {
	        				// Set properties
		        			xmlAttributeStep.setOutput(attributeStep.isOutput());
		        			xmlAttributeStep.setEnabled(attributeStep.isEnabled());
		        			xmlAttributeStep.setComment(attributeStep.getComment());
		        			//xmlAttributeStep.setSourceDefinition(xmlElementStep.getSourceDefinition());
		        			xmlAttributeStep.setNodeText(attributeStep.getNodeText());
		        			xmlAttributeStep.setNodeName(attributeStep.getNodeName());
		        			
		        			xmlAttributeStep.bNew = true;
		        			xmlAttributeStep.hasChanged = true;
							
							// Add new XMLAttribute step to parent
							DatabaseObject parentDbo = attributeStep.getParent();
						
							parentDbo.add(xmlAttributeStep);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(xmlAttributeStep,attributeStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(xmlAttributeStep,attributeStep.priority);
						
							// Add new XMLAttribute step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,attributeStep);
							treeParent.addChild(stepTreeObject);
							
			   				// Delete XMLElement step
							long oldPriority = attributeStep.priority;
							attributeStep.delete();
							xmlAttributeStep.getSequence().fireStepMoved(new StepEvent(xmlAttributeStep,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(xmlAttributeStep));
						} else {
							throw new EngineException("You cannot paste to a " + attributeStep.getParent().getClass().getSimpleName() + " a database object of type " + xmlAttributeStep.getClass().getSimpleName());
						}
	        		}
				}
    			
    			// XML Element
    			if ((databaseObject != null) && (databaseObject instanceof XMLElementStep)) {
    				XMLElementStep xmlElementStep = (XMLElementStep)databaseObject;
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
	        			
						// New XMLAttribute step
	        			XMLAttributeStep xmlAttributeStep = new XMLAttributeStep();
	        			
		        		if ( DatabaseObjectsManager.acceptDatabaseObjects(xmlElementStep.getParent(), xmlAttributeStep) ) {
	        				// Set properties
		        			xmlAttributeStep.setOutput(xmlElementStep.isOutput());
		        			xmlAttributeStep.setEnabled(xmlElementStep.isEnabled());
		        			xmlAttributeStep.setComment(xmlElementStep.getComment());
		        			xmlAttributeStep.setSourceDefinition(xmlElementStep.getSourceDefinition());
		        			xmlAttributeStep.setNodeText(xmlElementStep.getNodeText());
		        			xmlAttributeStep.setNodeName(xmlElementStep.getNodeName());
		        			
		        			xmlAttributeStep.bNew = true;
		        			xmlAttributeStep.hasChanged = true;
							
							// Add new XMLAttribute step to parent
							DatabaseObject parentDbo = xmlElementStep.getParent();
						
							parentDbo.add(xmlAttributeStep);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(xmlAttributeStep,xmlElementStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(xmlAttributeStep,xmlElementStep.priority);
						
							// Add new XMLAttribute step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,xmlElementStep);
							treeParent.addChild(stepTreeObject);
							
			   				// Delete XMLElement step
							long oldPriority = xmlElementStep.priority;
							xmlElementStep.delete();
							xmlAttributeStep.getSequence().fireStepMoved(new StepEvent(xmlAttributeStep,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(xmlAttributeStep));
						} else {
							throw new EngineException("You cannot paste to a " + xmlElementStep.getParent().getClass().getSimpleName() + " a database object of type " + xmlAttributeStep.getClass().getSimpleName());
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
