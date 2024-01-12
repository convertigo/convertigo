/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;
import com.twinsoft.convertigo.engine.Engine;

class WorkspaceCreationPage extends WizardPage implements SummaryGenerator {

	private Composite container;

	public WorkspaceCreationPage() {
		super("WorkspaceCreationPage");
		setTitle("Convertigo Workspace");
		setDescription("This is the first time Convertigo is launched in this workspace...");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {		
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Label label = new Label(container, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText("A new Convertigo workspace will be created in:\n\n" +
				"\t'" + Engine.PROJECTS_PATH + "'\n\n" +
				"This action will be completed when this wizard finishes.");
		
		setControl(container);
	}
	
	@Override
	public IWizard getWizard() {
		SetupWizard wizard = (SetupWizard) super.getWizard();
		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());
		return wizard;
	}

	public String getSummary() {
		return "Workspace creation in:\n" +
				"\t" + Engine.PROJECTS_PATH + "\n";
	}
}