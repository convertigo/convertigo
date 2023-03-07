/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.Project;

public class DeployProjectWizard extends Wizard {

	private ProjectBuildOptionsWizardPage projectBuildOptionsWizardPage = null;
	private ProjectBuildWizardPage projectBuildWizardPage = null;
	private ProjectExportOptionsWizardPage projectExportOptionsWizardPage = null;
	private ProjectDeployOptionsWizardPage projectDeployOptionsWizardPage = null;
	private ProjectDeployResultWizardPage projectDeployResultWizardPage = null;
	
	private Project project = null;
	
	public DeployProjectWizard(Project project) {
		super();
		this.project = project;
		setNeedsProgressMonitor(true);
	}

	private String getUnbuiltMessage() {
		if (project != null) {
			IApplicationComponent app = project.getMobileApplication() != null ? project.getMobileApplication().getApplicationComponent() : null;
			return app != null ? app.getUnbuiltMessage() : null;
		}
		return null;
	}
	
	@Override
	public void addPages() {
		if (getUnbuiltMessage() != null) {
			projectBuildOptionsWizardPage = new ProjectBuildOptionsWizardPage(project);
			addPage(projectBuildOptionsWizardPage);
			
			projectBuildWizardPage = new ProjectBuildWizardPage(project);
			addPage(projectBuildWizardPage);
		}
		
		projectExportOptionsWizardPage = new ProjectExportOptionsWizardPage(project);
		addPage(projectExportOptionsWizardPage);
		
		projectDeployOptionsWizardPage = new ProjectDeployOptionsWizardPage(project);
		addPage(projectDeployOptionsWizardPage);
		
		projectDeployResultWizardPage = new ProjectDeployResultWizardPage(project);
		addPage(projectDeployResultWizardPage);
	}

	protected ProjectExportOptionsWizardPage getProjectExportOptionsWizardPage() {
		return projectExportOptionsWizardPage;
	}
	
	protected ProjectDeployOptionsWizardPage getProjectDeployOptionsWizardPage() {
		return projectDeployOptionsWizardPage;
	}
	
	@Override
	public boolean canFinish() {
		IWizardPage currentPage = this.getContainer().getCurrentPage();
		return currentPage != null && currentPage.equals(projectDeployResultWizardPage) && currentPage.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
