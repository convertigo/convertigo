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

package com.twinsoft.convertigo.eclipse.wizards.import_export;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CarUtils;

public class ExportWizard extends Wizard implements IExportWizard {

	private ExportWizardPage fileChooserPage = null;
	
	public ExportWizard() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		String filePath = fileChooserPage.getFilePath();
		try {
			if (explorerView != null) {
		    	if (filePath != null) {
		    		int index = filePath.lastIndexOf("/");
		    		String exportDirectoryPath = filePath.substring(0,index);
		    		String exportFileName = filePath.substring(index+1);
		    		ConvertigoPlugin.logInfo("Export project to file \"" + exportFileName + "\"");
		    		
		    		String projectName = null;
					String fileName = exportFileName;
					int idx = fileName.lastIndexOf('.');
					if (idx != -1 ) {
						fileName = fileName.substring(0, idx);
						projectName = fileName;
					}
	
					if (projectName != null) {
						Project project = null;
						try {
							project = explorerView.getProject(projectName);
						}
						catch (EngineException e) {
							project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
						}
						
						if (project != null) {
							if (exportFileName.endsWith(".xml")) {
								Engine.theApp.databaseObjectsManager.exportProject(project);
								return true;
							}
							else if (exportFileName.endsWith(".car")) {								
								CarUtils.makeArchive(exportDirectoryPath, project);
								return true;
							}
						}
					}
		    	}
			}
		}
		catch (Exception e) {}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Convertigo project export Wizard");
		setNeedsProgressMonitor(true);
        fileChooserPage = new ExportWizardPage(selection);
	}

	/* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
	@Override
    public void addPages() {
        addPage(fileChooserPage);
    }
}
