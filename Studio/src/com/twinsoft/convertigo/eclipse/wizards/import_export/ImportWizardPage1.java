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

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.twinsoft.convertigo.eclipse.wizards.new_project.NewProjectWizardComposite1;
import com.twinsoft.convertigo.engine.util.StringUtils;


public class ImportWizardPage1 extends WizardPage {
	private String projectName;
	
	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	protected void setProjectTextValue(String projectName) {
		Control control = getControl();
		if (control != null) {
			((NewProjectWizardComposite1)control).getProjectName().setText(projectName);
		}
	}
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	public ImportWizardPage1(ISelection selection) {
		super("wizardPage");
		setTitle("Import a Convertigo project");
		setDescription("Enter a project name or keep default one.");
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
		if (!StringUtils.isNormalized(projectName)) {
			updateStatus("Project name must be normalized.\nDon't start with number and don't use non ASCII caracters.");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
}
	
