/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.wizards.import_export;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class ImportWizard extends Wizard implements IImportWizard {

	private IStructuredSelection selection;
	private ImportWizardPage fileChooserPage;
	private ImportWizardPage1 projectNameChooserPage;

	public ImportWizard() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		ProjectUrlParser parser = fileChooserPage.getParser();
		String filePath = fileChooserPage.getFilePath();
		if (parser.isValid()) {
			Job.create("Import project " + parser.getProjectName(), (mon) -> {
				try {
					mon.beginTask("Loading " + parser.getProjectName(), IProgressMonitor.UNKNOWN);
					Engine.theApp.referencedProjectManager.importProject(parser, true); 
				} catch (Exception e) {
					Engine.logStudio.debug("Loading from remote URL failed", e);
				}
				mon.done();
			}).schedule();
			return true;
		}
		
		try {
			if (explorerView != null) {
				if (filePath != null) {
					String defaultProjectName = getDefaultProjectName();
					String targetProjectName = getTargetProjectName();
					boolean newPriorities = !targetProjectName.equals(defaultProjectName);
					explorerView.importProject(filePath, targetProjectName, newPriorities);
				}
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to import project !");
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Convertigo project import Wizard");
		setNeedsProgressMonitor(true);
		this.selection = selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	@Override
	public void addPages() {
		fileChooserPage = new ImportWizardPage();
		addPage(fileChooserPage);
		
		projectNameChooserPage = new ImportWizardPage1(selection);
		addPage(projectNameChooserPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (fileChooserPage.equals(page)) {
			String defaultProjectName = getDefaultProjectName();
			if (defaultProjectName != null) {
				projectNameChooserPage.setProjectTextValue(defaultProjectName);
			}
		}
		return super.getNextPage(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish() {
		return fileChooserPage.getParser().isValid() || getTargetProjectName() != null;
	}

	//Modified by julienda - 13/09/2012
	private String getDefaultProjectName() {
		String filePath = fileChooserPage.getFilePath();
		File file = new File(filePath);
		
		String projectName;
		try {
			projectName = DatabaseObjectsManager.getProjectName(file);
		} catch (EngineException e1) {
			projectName = "";
		}
		
		// Find the filename
		if (projectName != null) {
			return projectName;
		}
		
		// CAR file case - Added by julienda - 08/09/2012
		try {
			return ZipUtils.getProjectName(filePath);
		} catch (IOException e) {
			return projectName;
		}
	}

	private String getTargetProjectName() {
		String projectName = projectNameChooserPage.getProjectName();
		if ((projectName == null) || projectName.equals("")) {
			projectName = getDefaultProjectName();
		}
		return projectName;
	}
}
