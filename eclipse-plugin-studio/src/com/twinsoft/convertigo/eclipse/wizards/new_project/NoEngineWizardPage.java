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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


class NoEngineWizardPage extends WizardPage {
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	NoEngineWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("Please register to use Convertigo");
		setDescription("Press 'Finish' to launch Convertigo registration");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Label noEngine = new Label(parent, SWT.CENTER);
		noEngine.setText("\n"
				+ "Convertigo Studio isn't completely installed,\n"
				+ "you have to complete the registration before\n"
				+ "starting building your projects.\n\n"
				+ "Please, click 'Finish' to register.");
		setErrorMessage("Press finish to start the registration wizard");
		setPageComplete(true);
		setControl(noEngine);
	}
}

