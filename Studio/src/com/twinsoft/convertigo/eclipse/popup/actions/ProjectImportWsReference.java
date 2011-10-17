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
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceImportDialog;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceImportDialogComposite;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;

public class ProjectImportWsReference extends MyAbstractAction {
	
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
				if ((treeObject != null) && (treeObject instanceof ProjectTreeObject)) {
					ProjectTreeObject projectTreeObject = (ProjectTreeObject)treeObject;
					Project project = projectTreeObject.getObject();
					if (project != null) {
						WsReferenceImportDialog wsReferenceImportDialog = new WsReferenceImportDialog(shell, WsReferenceImportDialogComposite.class, "WSDL Reference import");
						wsReferenceImportDialog.setProject(project);
						wsReferenceImportDialog.open();
			    		if (wsReferenceImportDialog.getReturnCode() != Window.CANCEL) {
			    			HttpConnector httpConnector = wsReferenceImportDialog.getHttpConnector();
			    			if (httpConnector != null) {
			    				// Reload sequence in tree without updating its schema for faster reload
			    				ConvertigoPlugin.logDebug("Reload project: start");
								explorerView.reloadTreeObjectWithoutDynamicUpdate(projectTreeObject);
								ConvertigoPlugin.logDebug("Reload project: end");
								// Select target dbo in tree
								explorerView.objectSelected(new CompositeEvent(httpConnector));
			    			}
			    		}
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
}
