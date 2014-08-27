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

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class TransactionExecuteDefaultAction extends MyAbstractAction {

	public TransactionExecuteDefaultAction() {
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
    			if (treeObject != null) {
    				ProjectTreeObject projectTreeObject = treeObject.getProjectTreeObject();
    				
    				Connector connector = null;
    				if (treeObject instanceof ConnectorTreeObject) {
    					ConnectorTreeObject connectorTreeObject = (ConnectorTreeObject)treeObject;
    					connectorTreeObject.openConnectorEditor();
    					connector = connectorTreeObject.getObject();
    				}
    				else {
    					connector = ((Project)projectTreeObject.getObject()).getDefaultConnector();
    					ConnectorTreeObject connectorTreeObject = (ConnectorTreeObject)explorerView.findTreeObjectByUserObject(connector);
    					if (connectorTreeObject != null)connectorTreeObject.openConnectorEditor();
    				}
    				
    				Transaction transaction = connector.getDefaultTransaction();
    				ConnectorEditor connectorEditor = projectTreeObject.getConnectorEditor(connector);
    				if (connectorEditor != null) {
    					getActivePage().activate(connectorEditor);
    					connectorEditor.getDocument(transaction.getName(), false);
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to execute the default transaction!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
