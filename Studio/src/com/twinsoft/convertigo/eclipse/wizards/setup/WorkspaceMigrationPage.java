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

public class WorkspaceMigrationPage extends WizardPage  implements SummaryGenerator {

	private Composite container;

	public WorkspaceMigrationPage() {
		super("WorkspaceMigrationPage");
		setTitle("Convertigo Workspace migration");
		setDescription("Older workspace detected.");
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Label label = new Label(container, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText("We detected that the workspace chosen is based on a previous version of Convertigo.\n" +
				"This workspace will be migrated to the new workspace format when this wizard finishes.\n\n" +
				"If you do not want to migrate your workspace, end the wizard and re-launch the studio to choose a different workspace.\n\n\n" +
				"We will migration the new convertigo workspace in\n\n" +
				Engine.PROJECTS_PATH);
		
		setControl(container);
	}
	
	@Override
	public IWizard getWizard() {
		SetupWizard wizard = (SetupWizard) super.getWizard();
		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());
		return wizard;
	}
	
	public String getSummary() {
		return "Workspace migration in:\n" +
				"\t" + Engine.PROJECTS_PATH + "\n";
	}
}