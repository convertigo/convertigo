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

import java.io.IOException;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceImportDialog;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceImportDialogComposite;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ReferenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

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
					if (treeObject instanceof ProjectTreeObject) {
						projectTreeObject = (ProjectTreeObject)treeObject;
						openWsReferenceImportDialog(shell, explorerView, projectTreeObject, null, treeObject);
					}
					else if (treeObject instanceof ReferenceTreeObject ) {
						ReferenceTreeObject referenceTreeObject = (ReferenceTreeObject)treeObject;
						WebServiceReference webServiceObject = (WebServiceReference) referenceTreeObject.getObject();
						projectTreeObject = referenceTreeObject.getProjectTreeObject();
						openWsReferenceImportDialog(shell, explorerView, projectTreeObject, webServiceObject, treeObject);
					}
				}
			}
			
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to import from remote WSDL!");
		}
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
	private void openWsReferenceImportDialog(Shell shell, ProjectExplorerView explorerView, ProjectTreeObject projectTreeObject, WebServiceReference webServiceReference, TreeObject treeObject) 
			throws EngineException, IOException{
		
		Project project = projectTreeObject.getObject();
		if (project != null) {
			WsReferenceImportDialog wsReferenceImportDialog = new WsReferenceImportDialog(shell, WsReferenceImportDialogComposite.class, "Update reference");
			wsReferenceImportDialog.setProject(project);
			wsReferenceImportDialog.setUpdateMode(updateMode);
			wsReferenceImportDialog.setReference(webServiceReference);
			
			wsReferenceImportDialog.open();
    		if (wsReferenceImportDialog.getReturnCode() != Window.CANCEL) {
    			HttpConnector httpConnector = wsReferenceImportDialog.getHttpConnector();
    			if (httpConnector != null) {
    				// Reload sequence in tree without updating its schema for faster reload
    				ConvertigoPlugin.logDebug("Reload project: start");
					explorerView.reloadTreeObject(projectTreeObject);
					ConvertigoPlugin.logDebug("Reload project: end");
					// Select target dbo in tree
					explorerView.objectSelected(new CompositeEvent(httpConnector));
    			}
    			
    			if (updateMode) {
    			   	MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
    				dialog.setText("SUCCESS");
    				dialog.setMessage("The reference file has been updated with success!");
    				dialog.open();
    				
    				webServiceReference.hasChanged = true;
    				Engine.theApp.schemaManager.clearCache(webServiceReference.getProject().getName());
					explorerView.reloadTreeObject(treeObject);
					explorerView.objectSelected(new CompositeEvent(webServiceReference));
					explorerView.fireTreeObjectPropertyChanged(new TreeObjectEvent(treeObject));
    			}
    		}
		}
	}
}
