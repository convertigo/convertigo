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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/ChangeToIfExistThenElseStepAction.java $
 * $Author: nicolasa $
 * $Revision: 31165 $
 * $Date: 2012-07-20 17:45:54 +0200 (ven., 20 juil. 2012) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;

public class ChangeToXMLElementStepAction extends MyAbstractAction {

	public ChangeToXMLElementStepAction() {
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
	        				elementStep.setSourceDefinition(sourceDefinition);
	        				elementStep.setNodeText(defaultValue);
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
	        		}
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change XMLConcat to XMLElement step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
