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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.rest.HeaderParameter;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingParameterTreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;

public class ChangeToHeaderParameterAction extends MyAbstractAction {

	public ChangeToHeaderParameterAction() {
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject != null) {
			DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
			ActionModel actionModel = DatabaseObjectsAction.selectionChanged(getClass().getName(), dbo);
			action.setEnabled(actionModel.isEnabled);
		}
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
    			Object databaseObject = treeObject.getObject();
    			
    			if ((databaseObject != null) && (databaseObject instanceof UrlMappingParameter)) {
    				UrlMappingParameter parameter = (UrlMappingParameter)databaseObject;
					
					TreeParent treeParent = treeObject.getParent();
					DatabaseObjectTreeObject parentTreeObject = null;
					if (treeParent instanceof DatabaseObjectTreeObject)
						parentTreeObject = (DatabaseObjectTreeObject)treeParent;
					else
						parentTreeObject = (DatabaseObjectTreeObject)treeParent.getParent();
					
	        		if (parentTreeObject != null) {
	        			// Create new Header parameter
	        			HeaderParameter headerParameter = new HeaderParameter();
	        			
	        			if (DatabaseObjectsManager.acceptDatabaseObjects(parameter.getParent(), headerParameter) ) {
	        				headerParameter.setComment(parameter.getComment());
	        				headerParameter.setInputType(parameter.getInputType());
	        				headerParameter.setArray(parameter.isArray());
	        				headerParameter.setMultiValued(parameter.isMultiValued());
	        				headerParameter.setRequired(parameter.isRequired());
	        				headerParameter.setMappedVariableName(parameter.getMappedVariableName());
	        				headerParameter.bNew = true;
	        				headerParameter.hasChanged = true;
	        				
							// Add new parameter to parent operation
	        				UrlMappingOperation operation = (UrlMappingOperation) parameter.getParent();
	        				operation.changeTo(headerParameter);
	        				
	        				// Add new parameter in Tree
	        				UrlMappingParameterTreeObject parameterTreeObject = new UrlMappingParameterTreeObject(explorerView.viewer,headerParameter);
	        				treeParent.addChild(parameterTreeObject);
	        				
	        				// Delete old parameter
	        				parameter.delete();
	        				
	        				// Rename new parameter
	        				headerParameter.setName(parameter.getName());
	        				
	        				// Reload in tree
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(headerParameter));
						} else {
							throw new EngineException("You cannot paste to a " + parameter.getParent().getClass().getSimpleName() + " a database object of type " + headerParameter.getClass().getSimpleName());
						}
	        		}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change to Header parameter!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
