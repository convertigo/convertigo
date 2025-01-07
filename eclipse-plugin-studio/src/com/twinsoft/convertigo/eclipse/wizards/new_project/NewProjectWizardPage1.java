/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.StringUtils;


class NewProjectWizardPage1 extends WizardPage {
	private String projectName;
	
	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	NewProjectWizardPage1(ISelection selection) {
		super("wizardPage");
		setTitle("New Convertigo project");
		setDescription("This wizard creates a new Convertigo project");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite1(parent, SWT.NULL, new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		initialize();
		setControl(container);
		dialogChanged();
	}
	
	
	private void initialize() {
	}
	
	
	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		projectName = ((NewProjectWizardComposite1)getControl()).getProjectName().getText();
		if (projectName.length() == 0) {
			updateStatus("Please enter project name");
			return;
		}
		boolean bProjectAlreadyExists = projectAlreadyExists(projectName);
		if (bProjectAlreadyExists) {
			updateStatus("This project name already exists !");
			return;
		}
		if (!StringUtils.isNormalized(projectName)) {
			updateStatus("Project name must be normalized.\nDon't start with number and don't use non ASCII caracters.");
			return;
		}
		File templateFile = new File(Engine.TEMPLATES_PATH + "/project/" + projectName + ".car"); 
		if (templateFile.exists()) {
			updateStatus("Project name not authorized.\n Please, choose a project name different from a template's one.");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	private boolean projectAlreadyExists(String projectName) {
		File file = new File(Engine.projectDir(projectName)); //$NON-NLS-1$
		return file.exists();
	}
}
	
