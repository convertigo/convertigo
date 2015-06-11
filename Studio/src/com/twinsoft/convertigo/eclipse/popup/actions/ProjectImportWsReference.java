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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceImportDialog;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceImportDialogComposite;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ReferenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;

public class ProjectImportWsReference extends MyAbstractAction {

	protected boolean updateMode = false;
	
	public ProjectImportWsReference() {
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
					ProjectTreeObject projectTreeObject = null;
					WebServiceReference webServiceReference = null;
					HttpConnector httpConnector = null;
					
					// Create a new  WS reference
					if (treeObject instanceof ProjectTreeObject) {
						projectTreeObject = (ProjectTreeObject)treeObject;
						webServiceReference = new WebServiceReference();
						webServiceReference.bNew = true;
					}
					// Update an existing WS reference
					else if (treeObject instanceof ReferenceTreeObject) {
						/* For further use
						ReferenceTreeObject referenceTreeObject = (ReferenceTreeObject)treeObject;
						webServiceReference = (WebServiceReference) referenceTreeObject.getObject();
						projectTreeObject = referenceTreeObject.getProjectTreeObject();*/
					}
					
					if (webServiceReference != null) {
						WsReferenceImportDialog wsReferenceImportDialog = new WsReferenceImportDialog(shell, WsReferenceImportDialogComposite.class, "Web service reference");
						wsReferenceImportDialog.setProject(projectTreeObject.getObject());
						wsReferenceImportDialog.setReference(webServiceReference);
						
						wsReferenceImportDialog.open();
			    		if (wsReferenceImportDialog.getReturnCode() != Window.CANCEL) {
			    			httpConnector = wsReferenceImportDialog.getHttpConnector();
			    		}
						
						Project project = projectTreeObject.getObject();
						Engine.theApp.schemaManager.clearCache(project.getName());
						
	    				// Reload project in tree 
						explorerView.reloadTreeObject(projectTreeObject);
						
		    			if (httpConnector != null && httpConnector.getParent() != null) {
							explorerView.objectSelected(new CompositeEvent(httpConnector));
		    			}
		    			else if (webServiceReference != null && webServiceReference.getParent() != null) {
		    				if (webServiceReference.hasChanged) projectTreeObject.hasBeenModified(true);
		    				explorerView.objectSelected(new CompositeEvent(webServiceReference));
		    			}
					}
				}
			}
			
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to "+ (updateMode ? "update":"import")+ " from remote WSDL!");
		}
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
