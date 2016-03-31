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

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class StatementAddVariableToTransactionAction extends MyAbstractAction {

	public StatementAddVariableToTransactionAction() {
		super();
	}

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
    			if ((databaseObject != null) && (databaseObject instanceof HTTPStatement)) {
    				HTTPStatement httpStatement = (HTTPStatement)databaseObject;
    				HtmlTransaction htmlTransaction = (HtmlTransaction)httpStatement.getParentTransaction();
    				
    				Vector<String> variables = new Vector<String>();
    				int i, size;
    				
    				/*size = htmlTransaction.getVariablesDefinitionSize();
    				for (i = 0 ; i < size ; i++) {
    					variables.add(htmlTransaction.getVariableDefinitionHttpName(i));
    				}*/
    				size = htmlTransaction.numberOfVariables();
    				for (i = 0 ; i < size ; i++) {
    					RequestableHttpVariable httpVariable = (RequestableHttpVariable)htmlTransaction.getVariable(i);
    					variables.add(httpVariable.getHttpName());
    				}
    				
    				String variableName, variableDescription, variableMethod;
    				Boolean variableType, variableRequired;
    				Object variableValue;
    				int variableVisibility;
    				size = httpStatement.numberOfVariables();
    				for (i = 0 ; i < size ; i++) {
    					HttpStatementVariable httpStatementVariable = (HttpStatementVariable)httpStatement.getVariable(i);
    					if (httpStatementVariable != null) {
        					variableName = httpStatementVariable.getName();
        					variableDescription = httpStatementVariable.getDescription();
        					variableRequired = httpStatementVariable.isRequired();
        					variableValue = httpStatementVariable.getValueOrNull();
        					variableType = httpStatementVariable.isMultiValued();
        					variableMethod = httpStatementVariable.getHttpMethod();
        					variableVisibility = httpStatementVariable.getVisibility();
        					
        					if (!variables.contains(variableName)) {
        						RequestableHttpVariable requestableVariable = (variableType ? new RequestableHttpMultiValuedVariable():new RequestableHttpVariable());
        						requestableVariable.setName(variableName);
        						requestableVariable.setDescription(variableDescription);
        						requestableVariable.setRequired(variableRequired);
        						requestableVariable.setValueOrNull(variableValue);
        						requestableVariable.setWsdl(Boolean.TRUE);
        						requestableVariable.setPersonalizable(Boolean.FALSE);
        						requestableVariable.setCachedKey(Boolean.TRUE);
        						requestableVariable.setHttpMethod(variableMethod);
        						requestableVariable.setHttpName("");
        						requestableVariable.setVisibility(variableVisibility);
        						
        						requestableVariable.bNew = true;
        						requestableVariable.hasChanged = true;
        						
        						htmlTransaction.add(requestableVariable);
        					}
    					}
    				}
    				
					htmlTransaction.hasChanged = true;
					
					explorerView.reloadDatabaseObject(htmlTransaction);
    			}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to add HTTP variables to transaction!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
