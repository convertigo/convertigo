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

import java.util.StringTokenizer;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.rest.FormParameter;
import com.twinsoft.convertigo.beans.rest.PostOperation;
import com.twinsoft.convertigo.beans.rest.PutOperation;
import com.twinsoft.convertigo.beans.rest.QueryParameter;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;

public class OperationImportParametersFromVariablesAction extends MyAbstractAction {

	public OperationImportParametersFromVariablesAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof UrlMappingOperation)) {
    				UrlMappingOperation operation = (UrlMappingOperation)databaseObject;
    				String targetRequestable = operation.getTargetRequestable();
    				if (!targetRequestable.isEmpty()) {
	    				StringTokenizer st = new StringTokenizer(targetRequestable,".");
	    				int count = st.countTokens();
	    				String projectName = st.nextToken();
	    				String sequenceName = count == 2 ? st.nextToken():"";
	    				String connectorName = count == 3 ? st.nextToken():"";
	    				String transactionName = count == 3 ? st.nextToken():"";
	    				
	    				Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
	    				RequestableObject requestableObject = null;
	    				if (sequenceName.isEmpty()) {
	    					requestableObject = project.getConnectorByName(connectorName).getTransactionByName(transactionName);
	    				}
	    				else {
	    					requestableObject = project.getSequenceByName(sequenceName);
	    				}
	    				
	    				if (requestableObject != null && requestableObject instanceof IVariableContainer) {
	    					IVariableContainer variableContainer = (IVariableContainer)requestableObject;
	    					for (Variable variable: variableContainer.getVariables()) {
	    						String variableName = variable.getName();
	    						UrlMappingParameter parameter = null;
	    						try {
	    							parameter = operation.getParameterByName(variableName);
	    						} catch (Exception e) {}
	    						
	    						if (parameter == null) {
		    						if (operation instanceof PostOperation || operation instanceof PutOperation)
		    							parameter = new FormParameter();
		    						else
			    						parameter = new QueryParameter();
	    							parameter.setName(variableName);
	    	        				parameter.setComment(variable.getComment());
	    	        				parameter.setArray(false);
	    	        				parameter.setMultiValued(variable.isMultiValued());
	    	        				//parameter.setRequired(variable.isRequired());
	    	        				parameter.setMappedVariableName(variableName);
	    	        				parameter.bNew = true;
	    	        				parameter.hasChanged = true;
	    	        				
	    							operation.add(parameter);
	    							operation.hasChanged =  true;
	    						}
	    					}
	    					
		    				if (operation.hasChanged) {
		    					explorerView.reloadTreeObject(treeObject);
								StructuredSelection structuredSelection = new StructuredSelection(treeObject);
								ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)explorerView, structuredSelection);
		    				}
	    				}
    				}
    				else {
    					throw new ConvertigoException("Operation has no target requestable : please select one first.");
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to import variables as new parameters in operation!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
