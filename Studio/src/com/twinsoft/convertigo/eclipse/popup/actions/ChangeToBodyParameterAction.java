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

import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.rest.BodyParameter;
import com.twinsoft.convertigo.beans.rest.PathParameter;
import com.twinsoft.convertigo.beans.rest.PostOperation;
import com.twinsoft.convertigo.beans.rest.PutOperation;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UrlMappingParameterTreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;

public class ChangeToBodyParameterAction extends MyAbstractAction {

	public ChangeToBodyParameterAction() {
	}

	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		
		UrlMappingParameter parameter = (UrlMappingParameter) treeObject.getObject();
		UrlMappingOperation operation = (UrlMappingOperation) parameter.getParent();
		boolean enabled = !(parameter instanceof BodyParameter) && !(parameter instanceof PathParameter) 
				&& (operation instanceof PostOperation || operation instanceof PutOperation);
		action.setEnabled(enabled);
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
	        			// Create new Body parameter
	        			BodyParameter bodyParameter = new BodyParameter();
	        			
	        			if (DatabaseObjectsManager.acceptDatabaseObjects(parameter.getParent(), bodyParameter) ) {
	        				bodyParameter.setComment(parameter.getComment());
	        				bodyParameter.setArray(false);
	        				bodyParameter.setMultiValued(false);
	        				bodyParameter.setRequired(parameter.isRequired());
	        				bodyParameter.setMappedVariableName(parameter.getMappedVariableName());
	        				bodyParameter.bNew = true;
	        				bodyParameter.hasChanged = true;
	        				
							// Add new parameter to parent operation
	        				UrlMappingOperation operation = (UrlMappingOperation) parameter.getParent();
	        				operation.add(bodyParameter);
	        				
	        				// Add new parameter in Tree
	        				UrlMappingParameterTreeObject parameterTreeObject = new UrlMappingParameterTreeObject(explorerView.viewer,bodyParameter);
	        				treeParent.addChild(parameterTreeObject);
	        				
	        				// Delete old parameter
	        				parameter.delete();
	        				
	        				// Rename new parameter
	        				bodyParameter.setName(parameter.getName());
	        				
	        				// Reload in tree
		        			parentTreeObject.hasBeenModified(true);
			                explorerView.reloadTreeObject(parentTreeObject);
			                explorerView.setSelectedTreeObject(explorerView.findTreeObjectByUserObject(bodyParameter));
						} else {
							throw new EngineException("You cannot paste to a " + parameter.getParent().getClass().getSimpleName() + " a database object of type " + bodyParameter.getClass().getSimpleName());
						}
	        		}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to change to Body parameter!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
