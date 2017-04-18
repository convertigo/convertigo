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

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.studio.ActionModel;
import com.twinsoft.convertigo.engine.studio.DatabaseObjectsAction;

public class SetDefaultConnectorAction extends MyAbstractAction {

	public SetDefaultConnectorAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject instanceof ConnectorTreeObject) {
			DatabaseObject dbo = (DatabaseObject) treeObject.getObject();
			ActionModel actionModel = DatabaseObjectsAction.selectionChanged(getClass().getName(), dbo);
			action.setChecked(actionModel.isChecked);
		}
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			ConnectorTreeObject connectorTreeObject = (ConnectorTreeObject)explorerView.getFirstSelectedTreeObject();

    			Connector connector = (Connector)connectorTreeObject.getObject();
                Project project = connector.getProject();
                
                // Report from 4.5: fix #401
                ConnectorTreeObject defaultConnectorTreeObject = null;
                Connector defaultConnector = project.getDefaultConnector();
                if (defaultConnector != null) {
	                defaultConnectorTreeObject = (ConnectorTreeObject)explorerView.findTreeObjectByUserObject(defaultConnector);
                }
                
                project.setDefaultConnector(connector);
                
                if (defaultConnectorTreeObject != null) {
                	defaultConnectorTreeObject.isDefault = false;
                	defaultConnectorTreeObject.hasBeenModified(true);
                }
                connectorTreeObject.isDefault = true;
                connectorTreeObject.hasBeenModified(true);
                
                // Updating the tree
    			explorerView.refreshTreeObject(connectorTreeObject.getParentDatabaseObjectTreeObject());
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to set connector to default one!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
