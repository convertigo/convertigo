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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

class NewProjectWizardPageSummarySampleProject extends WizardPage {
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	NewProjectWizardPageSummarySampleProject(ISelection selection) {
		super("wizardPage");
		setTitle("New project summary");
		setDescription("This step summarizes all the configuration options");
	}

	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardCompositeSummarySampleProject(parent, SWT.NULL);
		initialize();
		setControl(container);
		setPageComplete(true);
	}
	
	private void initialize() {
	}	
}
	
