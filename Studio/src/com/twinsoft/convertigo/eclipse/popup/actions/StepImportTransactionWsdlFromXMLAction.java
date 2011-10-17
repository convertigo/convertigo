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

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;

public class StepImportTransactionWsdlFromXMLAction extends MyAbstractAction {

	public StepImportTransactionWsdlFromXMLAction() {
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
    			if ((databaseObject != null) && (databaseObject instanceof TransactionStep)) {
    				TransactionStep transactionStep = (TransactionStep)databaseObject;
    				
    				String projectName = transactionStep.getProjectName();
    				String connectorName = transactionStep.getConnectorName();
    				String transactionName = transactionStep.getTransactionName();
    				
    				Project p = transactionStep.getParentSequence().getLoadedProject(projectName);
    				Connector connector = (connectorName.equals("") ? p.getDefaultConnector():p.getConnectorByName(connectorName));
    				
    				ProjectTreeObject targetProject = null;
                    ConnectorEditor connectorEditor = null;
    				TreeObject object = explorerView.getProjectRootObject(projectName);
    				if (object instanceof ProjectTreeObject)
    					targetProject = (ProjectTreeObject)object;
                    if (targetProject != null)
                    	connectorEditor = targetProject.getConnectorEditor(connector);
                    if (connectorEditor == null) {
                		ConvertigoPlugin.infoMessageBox("Please open target connector first and execute \""+transactionName+"\" transaction.");
                    	return;
	                }
	                
                    Document document = connectorEditor.getLastGeneratedDocument();
                    if (document == null) {
                    	ConvertigoPlugin.infoMessageBox("You should first generate the XML document before trying to extract the XML structure.");
                    	return;
                    }
                    else {
        				transactionStep.importWSDLTypes(document);
        				if (transactionStep.hasChanged) {
        					explorerView.refreshTreeObject(treeObject);
    						StructuredSelection structuredSelection = new StructuredSelection(treeObject);
    						ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged((IWorkbenchPart)explorerView, structuredSelection);
        				}
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to extract transaction xml structure to step!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
