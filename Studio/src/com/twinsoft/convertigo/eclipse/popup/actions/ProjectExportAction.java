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

import java.awt.Toolkit;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.CarUtils;

public class ProjectExportAction extends MyAbstractAction {

	public ProjectExportAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
//		int logLevel = Engine.log.logLevel;
//		Engine.log.logLevel = Log.LOGLEVEL_EXCEPTION;
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			ProjectTreeObject projectTreeObject = (ProjectTreeObject)explorerView.getFirstSelectedTreeObject();
    			Project project = (Project) projectTreeObject.getObject();
            	String projectName = project.getName();
    			
                if (projectTreeObject.getModified()) {
                    int response = SWT.YES;
                	MessageBox messageBox = new MessageBox(shell,SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
                	messageBox.setMessage("The project has not been saved. Do you want to save your work now?");
                	response = messageBox.open();
                    if (response == SWT.YES) {
                    	ConvertigoPlugin.logInfo("Saving the project '"+ projectName +"'");
                    	ConvertigoPlugin.projectManager.save(project, true);
                    	while (projectTreeObject.getModified())
                    		projectTreeObject.hasBeenModified(false);
                        ConvertigoPlugin.logInfo("Project '"+ projectName +"' saved!");
                        explorerView.refreshTree();
                    }
                }
    			
    			String projectArchive = projectName + ".car";
    			
            	FileDialog fileDialog = new FileDialog(shell, SWT.PRIMARY_MODAL | SWT.SAVE);
            	fileDialog.setText("Export a project");
            	fileDialog.setFilterExtensions(new String[]{"*.car","*.xml"});
            	fileDialog.setFilterNames(new String[]{"Convertigo archives","Convertigo projects"});
            	fileDialog.setFilterPath(Engine.PROJECTS_PATH);
            	fileDialog.setFileName(projectArchive);
            	
            	String filePath = fileDialog.open();
            	if (filePath != null) {
            		int index = filePath.lastIndexOf(File.separator);
            		String exportDirectoryPath = filePath.substring(0,index);
            		String exportFileName = filePath.substring(index+1);
            		ConvertigoPlugin.logInfo("Export project to file \"" + exportFileName + "\"");
            		
    				String fileName = exportFileName;
    				int idx = fileName.lastIndexOf('.');
    				if (idx != -1 ) {
    					fileName = fileName.substring(0, idx);
    				}

    				if (!fileName.equals(projectName)) {
    					Toolkit.getDefaultToolkit().beep();
    					ConvertigoPlugin.logWarning("Wrong file name (it must be the same as the project)!");
    				}
    				else if (exportFileName.endsWith(".xml")) {
    					Engine.theApp.databaseObjectsManager.exportProject(project);
    				}
    				else if (exportFileName.endsWith(".car")) {
    					CarUtils.makeArchive(exportDirectoryPath, project);
    				}
    				else {
    					Toolkit.getDefaultToolkit().beep();
    					ConvertigoPlugin.logWarning("Wrong file extension!");
    				}
            	}
    	
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to export the project!");
        }
        finally {
//        	Engine.log.logLevel = logLevel;
			shell.setCursor(null);
			waitCursor.dispose();
        }        
	}

}
