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
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceImportDialog;
import com.twinsoft.convertigo.eclipse.dialogs.WsRestReferenceImportDialogComposite;
import com.twinsoft.convertigo.eclipse.dialogs.WsSoapReferenceImportDialogComposite;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ReferenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;

public class ProjectImportWsReference extends MyAbstractAction {

	public static final int TYPE_SOAP = 1;
	public static final int TYPE_REST = 2;
	
	protected boolean updateMode = false;
	protected int wsType = 2;
	
	protected ProjectImportWsReference() {
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
					RemoteFileReference reference = null;
					HttpConnector httpConnector = null;
					
					// Create a new  WS reference
					if (treeObject instanceof ProjectTreeObject) {
						projectTreeObject = (ProjectTreeObject)treeObject;
						if (wsType == TYPE_SOAP) {
							reference = new WebServiceReference();
							reference.bNew = true;
						}
						if (wsType == TYPE_REST) {
							reference = new RestServiceReference();
							reference.bNew = true;
						}
					}
					// Update an existing WS reference
					else if (treeObject instanceof ReferenceTreeObject) {
						/* For further use
						ReferenceTreeObject referenceTreeObject = (ReferenceTreeObject)treeObject;
						webServiceReference = (WebServiceReference) referenceTreeObject.getObject();
						projectTreeObject = referenceTreeObject.getProjectTreeObject();*/
					}
					
					if (reference != null) {
						WsReferenceImportDialog wsReferenceImportDialog = null;
						if (wsType == TYPE_SOAP) {
							wsReferenceImportDialog = new WsReferenceImportDialog(shell, WsSoapReferenceImportDialogComposite.class, "SOAP Web Service reference");
						}
						if (wsType == TYPE_REST) {
							wsReferenceImportDialog = new WsReferenceImportDialog(shell, WsRestReferenceImportDialogComposite.class, "REST Web Service reference");
						}
						wsReferenceImportDialog.setProject(projectTreeObject.getObject());
						wsReferenceImportDialog.setReference(reference);
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
		    			else if (reference != null && reference.getParent() != null) {
		    				if (reference.hasChanged) projectTreeObject.hasBeenModified(true);
		    				explorerView.objectSelected(new CompositeEvent(reference));
		    			}
					}
				}
			}
			
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to "+ (updateMode ? "update":"import")+ " from remote WS definition!");
		}
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
