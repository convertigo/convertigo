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

import com.twinsoft.convertigo.beans.common.XMLVector;
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
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ChangeToSingleValuedVariableAction extends MyAbstractAction {

	public ChangeToSingleValuedVariableAction() {
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
        				Variable multi = (Variable)databaseObject;
        				Variable simple = null;
        				if (databaseObject instanceof TestCaseMultiValuedVariable)
        					simple = new TestCaseVariable();
        				if (databaseObject instanceof StepMultiValuedVariable)
        					simple = new StepVariable();
        				if (databaseObject instanceof RequestableMultiValuedVariable)
        					simple = new RequestableVariable();
        				if (databaseObject instanceof RequestableHttpMultiValuedVariable)
        					simple = new RequestableHttpVariable();
        				if (databaseObject instanceof HttpStatementMultiValuedVariable)
        					simple = new HttpStatementVariable();
        				
        				if (simple != null) {
        					if (multi instanceof StepMultiValuedVariable){
        						((StepVariable)simple).setSourceDefinition(((StepVariable)multi).getSourceDefinition());
        					}
        					if (multi instanceof RequestableVariable){
        						((RequestableVariable)simple).setXmlTypeAffectation(((RequestableVariable)multi).getXmlTypeAffectation());
        					}
        					if (multi instanceof RequestableHttpVariable){
        						// HttpName
        						((RequestableHttpVariable)simple).setHttpName(((RequestableHttpVariable)multi).getHttpName());
        						// HttpMethod
        						((RequestableHttpVariable)simple).setHttpMethod(((RequestableHttpVariable)multi).getHttpMethod());
        					}
        					XMLVector<Object> xmlv = GenericUtils.cast(multi.getValueOrNull());
        					Object value = (xmlv == null) ? null: (xmlv.isEmpty() ? "":xmlv.get(0).toString());
        					simple.setValueOrNull(value);
        					simple.setVisibility(multi.getVisibility());

        					// Comment
        					simple.setComment(multi.getComment());
    						// Description
        					simple.setDescription(multi.getDescription());
        					// Required
        					simple.setRequired(multi.isRequired());
        					
        					simple.bNew = true;
        					simple.hasChanged = true;
        					
    						// Add new variable to parent
    						DatabaseObject parentDbo = multi.getParent();
    						parentDbo.add(simple);
    						
    						// Set correct order
    						if (parentDbo instanceof TestCase)
    							((TestCase)parentDbo).insertAtOrder(simple,multi.priority);
    						if (parentDbo instanceof RequestableStep)
    							((RequestableStep)parentDbo).insertAtOrder(simple,multi.priority);
    						if (parentDbo instanceof Sequence)
    							((Sequence)parentDbo).insertAtOrder(simple,multi.priority);
    						if (parentDbo instanceof TransactionWithVariables)
    							((TransactionWithVariables)parentDbo).insertAtOrder(simple,multi.priority);
    						if (parentDbo instanceof HTTPStatement)
    							((HTTPStatement)parentDbo).insertAtOrder(simple,multi.priority);
    						
    						// Add new variable in Tree
    						VariableTreeObject2 varTreeObject = new VariableTreeObject2(explorerView.viewer,simple);
    						treeParent.addChild(varTreeObject);
    						
    		   				// Delete simple variable
    						multi.delete();
    		   				
    		   				// Set correct name
    		   				simple.setName(multi.getName());
    		   				
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(simple));
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
