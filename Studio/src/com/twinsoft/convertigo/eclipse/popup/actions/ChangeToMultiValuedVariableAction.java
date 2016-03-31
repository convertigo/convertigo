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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.variables.HttpStatementMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject2;

public class ChangeToMultiValuedVariableAction extends MyAbstractAction {

	public ChangeToMultiValuedVariableAction() {
		super();
	}

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
				TreeParent treeParent = treeObject.getParent();
				DatabaseObjectTreeObject parentTreeObject = null;
				if (treeParent instanceof DatabaseObjectTreeObject)
					parentTreeObject = (DatabaseObjectTreeObject)treeParent;
				else
					parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
				
        		if (parentTreeObject != null) {
        			Object databaseObject = treeObject.getObject();
        			if (databaseObject != null) {
        				Variable simple = (Variable)databaseObject;
        				Variable multi = null;
        				if (databaseObject instanceof TestCaseVariable)
        					multi = new TestCaseMultiValuedVariable();
        				if (databaseObject instanceof StepVariable)
        					multi = new StepMultiValuedVariable();
        				if (databaseObject instanceof RequestableVariable)
        					multi = new RequestableMultiValuedVariable();
        				if (databaseObject instanceof RequestableHttpVariable)
        					multi = new RequestableHttpMultiValuedVariable();
        				if (databaseObject instanceof HttpStatementVariable)
        					multi = new HttpStatementMultiValuedVariable();
        				
        				if (multi != null) {
        					if (multi instanceof StepVariable){
        						((StepVariable)multi).setSourceDefinition(((StepVariable)simple).getSourceDefinition());
        					}
        					if (multi instanceof RequestableVariable){
        						((RequestableVariable)multi).setXmlTypeAffectation(((RequestableVariable)simple).getXmlTypeAffectation());
        					}
        					if (multi instanceof RequestableHttpVariable){
        						// HttpName
        						((RequestableHttpVariable)multi).setHttpName(((RequestableHttpVariable)simple).getHttpName());
        						// HttpMethod
        						((RequestableHttpVariable)multi).setHttpMethod(((RequestableHttpVariable)simple).getHttpMethod());
        						
        					}
        					Object value = simple.getValueOrNull();
        					multi.setValueOrNull(value);
        					multi.setVisibility(simple.getVisibility());
        					
        					// Comment
        					multi.setComment(simple.getComment());
    						// Description
        					multi.setDescription(simple.getDescription());
        					// Required
        					multi.setRequired(simple.isRequired());
        					
        					multi.bNew = true;
        					multi.hasChanged = true;
        					
    						// Add new variable to parent
    						DatabaseObject parentDbo = simple.getParent();
    						parentDbo.add(multi);
    						
    						// Set correct order
    						if (parentDbo instanceof TestCase)
    							((TestCase)parentDbo).insertAtOrder(multi,simple.priority);
    						if (parentDbo instanceof RequestableStep)
    							((RequestableStep)parentDbo).insertAtOrder(multi,simple.priority);
    						if (parentDbo instanceof Sequence)
    							((Sequence)parentDbo).insertAtOrder(multi,simple.priority);
    						if (parentDbo instanceof TransactionWithVariables)
    							((TransactionWithVariables)parentDbo).insertAtOrder(multi,simple.priority);
    						if (parentDbo instanceof HTTPStatement)
    							((HTTPStatement)parentDbo).insertAtOrder(multi,simple.priority);
    						
    						// Add new variable in Tree
    						VariableTreeObject2 varTreeObject = new VariableTreeObject2(explorerView.viewer,multi);
    						treeParent.addChild(varTreeObject);
    						
    						    						
    						//Delete simple variable
    						simple.delete();
    						
    		   				// Set correct name
    		   				multi.setName(simple.getName());
    		   				
    		   				   		   				
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(multi));
    						
        				}
        			}
        		}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change simple variable to multi valuated variable!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
