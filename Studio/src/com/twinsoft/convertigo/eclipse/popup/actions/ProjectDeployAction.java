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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ProjectChooseTestCasesDialog;
import com.twinsoft.convertigo.eclipse.dialogs.ProjectDeployDialog;
import com.twinsoft.convertigo.eclipse.dialogs.ProjectDeployDialogComposite;
import com.twinsoft.convertigo.eclipse.dialogs.ProjectVersionUpdateDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;

public class ProjectDeployAction extends MyAbstractAction {

	public ProjectDeployAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	boolean bDeploy = true;
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			ProjectTreeObject projectTreeObject = (ProjectTreeObject)explorerView.getFirstSelectedTreeObject();
            	Project project = projectTreeObject.getObject();
            	
    			ProjectVersionUpdateDialog dlg = new ProjectVersionUpdateDialog(shell, project.getVersion());
            	if (dlg.open() == Window.OK) {
            		project.setVersion(dlg.result);
            		project.hasChanged = true;
            		projectTreeObject.save(false);
            		explorerView.refreshTree();
            	}
    			
    			if (projectTreeObject.getModified()) {
					MessageBox messageBox = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
					messageBox.setMessage("The project \"" + projectTreeObject.getName() + "\" has not been saved.\n Do you want to save it before deployment?");
					int ret = messageBox.open();
					if (ret == SWT.OK) {
	    				projectTreeObject.save(false);
	       				explorerView.refreshTree();
					}
					else
						bDeploy = false;
    			}
    			
    			ProjectChooseTestCasesDialog dlgTC = null;
            	List<TestCase> listTestCasesSelected = new ArrayList<TestCase>();
            	boolean checkTestCases = dlg.isCheckTestCases();
            	
            	if (checkTestCases) {
            		dlgTC = new ProjectChooseTestCasesDialog(shell, project);
					if (dlgTC.open() == Window.OK) {
						listTestCasesSelected = dlgTC.getTestCasesMap();
					}
            	}
    			
    			if (bDeploy) {
	            	ProjectDeployDialog projectDeployDialog = new ProjectDeployDialog(shell, ProjectDeployDialogComposite.class, 
	            			"Deploy a Convertigo project", listTestCasesSelected);
	            	projectDeployDialog.open();
	            	
	        		if (projectDeployDialog.getReturnCode() != Window.CANCEL) { 
	        			
	        		}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to deploy the project!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }        
	}

}
