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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToXMLElementStepAction.java $
 * $Author: julienda $
 * $Revision: 31165 $
 * $Date: 2013-07-23 09:48:54 +0200 (lun., 23 juil. 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;

public class ChangeToXMLElementStepAction extends MyAbstractAction {

	public ChangeToXMLElementStepAction() {
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
				if (DatabaseObjectsManager.acceptDatabaseObjects(dboParent, new XMLElementStep())) {
					enable = (dbo instanceof XMLConcatStep) ||
								(dbo instanceof XMLAttributeStep) ||
									(dbo instanceof ElementStep);
				}
			}
			action.setEnabled(enable);
		}
		catch (Exception e) {}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.popup.actions.MyAbstractAction#run()
	 */
	@SuppressWarnings("unchecked")
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
    			
    			// XML Concat step
    			if ((databaseObject != null) && (databaseObject instanceof XMLConcatStep)) {
    				XMLConcatStep concatStep = (XMLConcatStep)databaseObject;
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
	        			// New XMLElementStep step
	        			XMLElementStep elementStep = new XMLElementStep();
	        			if ( DatabaseObjectsManager.acceptDatabaseObjects(concatStep.getParent(), elementStep) ) {

		        			if ( concatStep.getSourcesDefinition().toString().equals("[[]]") ) {
		        				elementStep.setSourceDefinition(new XMLVector<String>());
		        			} else {
		        				// Set properties (Default value and Source)
		        				XMLVector<XMLVector<Object>> sources = concatStep.getSourcesDefinition();
		        				XMLVector<String> sourceDefinition = new XMLVector<String>();
		        				String defaultValue = "";
		        				for ( XMLVector<Object> source : sources ) {
		        					if ( sources.lastElement() == source ) {
		        						defaultValue += source.get(2);
		        					} else {
		        						defaultValue += source.get(2) + concatStep.getSeparator();
		        					}
		        					if (sourceDefinition.toString().equals("[]") 
		        							&& (!source.get(1).toString().equals("") && !source.get(1).toString().equals("[]") ) ) {
		        						sourceDefinition = (XMLVector<String>) source.get(1);
		        					}
		        				}
		        				elementStep.setOutput(concatStep.isOutput());
			        			elementStep.setEnable(concatStep.isEnable());
			        			elementStep.setComment(concatStep.getComment());
			        			elementStep.setNodeName(concatStep.getNodeName());
		        				elementStep.setNodeText(defaultValue);
		        				elementStep.setSourceDefinition(sourceDefinition);
		        				
		        			}
		        			
		        			elementStep.bNew = true;
		        			elementStep.hasChanged = true;
							
							// Add new XMLElementStep step to parent
							DatabaseObject parentDbo = concatStep.getParent();
							parentDbo.add(elementStep);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,concatStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,concatStep.priority);
						
							// Add new XMLElementStep step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,elementStep);
							treeParent.addChild(stepTreeObject);
							
			   				// Delete XMLConcatStep step
							long oldPriority = concatStep.priority;
							concatStep.delete();
			   				elementStep.getSequence().fireStepMoved(new StepEvent(elementStep,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(elementStep));
	        			} else {
							throw new EngineException("You cannot paste to a " + concatStep.getParent().getClass().getSimpleName() + " a database object of type " + elementStep.getClass().getSimpleName());
						}
	        		}
				}
    			
    			// XML Attribute
    			if ((databaseObject != null) && (databaseObject instanceof XMLAttributeStep)) {
    				XMLAttributeStep attributeStep = (XMLAttributeStep)databaseObject;
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
	        			
						// New XMLElement step
	        			XMLElementStep elementStep = new XMLElementStep();
	        			
		        		if ( DatabaseObjectsManager.acceptDatabaseObjects(attributeStep.getParent(), elementStep) ) {
	        				// Set properties	        			
		        			elementStep.setOutput(attributeStep.isOutput());
		        			elementStep.setEnable(attributeStep.isEnable());
		        			elementStep.setComment(attributeStep.getComment());
		        			elementStep.setSourceDefinition(attributeStep.getSourceDefinition());
		        			elementStep.setNodeText(attributeStep.getNodeText());
		        			elementStep.setNodeName(attributeStep.getNodeName());
		        			
		        			elementStep.bNew = true;
		        			elementStep.hasChanged = true;
							
							// Add new XMLElement step to parent
							DatabaseObject parentDbo = attributeStep.getParent();
						
							parentDbo.add(elementStep);
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,attributeStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,attributeStep.priority);
						
							// Add new XMLElement step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,attributeStep);
							treeParent.addChild(stepTreeObject);
							
			   				// Delete XMLAttribute step
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
    			
    			// JElement
    			if ((databaseObject != null) && (databaseObject instanceof ElementStep)) {
    				ElementStep jelementStep = (ElementStep)databaseObject;
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
	        			
						// New XMLElement step
	        			XMLElementStep elementStep = new XMLElementStep();
	        			
		        		if ( DatabaseObjectsManager.acceptDatabaseObjects(jelementStep.getParent(), elementStep) ) {
	        				// Set properties	        			
		        			elementStep.setOutput(jelementStep.isOutput());
		        			elementStep.setEnable(jelementStep.isEnable());
		        			elementStep.setComment(jelementStep.getComment());
		        			//elementStep.setSourceDefinition(jelementStep.getSourceDefinition());
		        			elementStep.setNodeText(jelementStep.getNodeText());
		        			elementStep.setNodeName(jelementStep.getNodeName());
		        			
		        			elementStep.bNew = true;
		        			elementStep.hasChanged = true;
							
							// Add new XMLElement step to parent
							DatabaseObject parentDbo = jelementStep.getParent();
						
							parentDbo.add(elementStep);
							
							for (Step step : jelementStep.getAllSteps()) {
								try {
									elementStep.addStep(step);
								} catch (Throwable t) {}
							}
							
							// Set correct order
							if (parentDbo instanceof StepWithExpressions)
								((StepWithExpressions)parentDbo).insertAtOrder(elementStep,jelementStep.priority);
							else if (parentDbo instanceof Sequence)
								((Sequence)parentDbo).insertAtOrder(elementStep,jelementStep.priority);
						
							// Add new XMLElement step in Tree
							StepTreeObject stepTreeObject = new StepTreeObject(explorerView.viewer,jelementStep);
							treeParent.addChild(stepTreeObject);
							
			   				// Delete XMLAttribute step
							long oldPriority = jelementStep.priority;
							jelementStep.delete();
							elementStep.getSequence().fireStepMoved(new StepEvent(elementStep,String.valueOf(oldPriority)));
							
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(elementStep));
						} else {
							throw new EngineException("You cannot paste to a " + jelementStep.getParent().getClass().getSimpleName() + " a database object of type " + elementStep.getClass().getSimpleName());
						}
	        		}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change step to XMLElement step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
