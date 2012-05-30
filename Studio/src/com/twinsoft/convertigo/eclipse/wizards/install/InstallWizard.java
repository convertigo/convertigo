package com.twinsoft.convertigo.eclipse.wizards.install;

import org.eclipse.jface.wizard.Wizard;

public class InstallWizard extends Wizard {

	protected ChooseWorkspaceLocationPage chooseWorkspaceLocationPage;

	public InstallWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		chooseWorkspaceLocationPage = new ChooseWorkspaceLocationPage();
		addPage(chooseWorkspaceLocationPage);
	}

	@Override
	public boolean performFinish() {
		// Print the result to the console
		System.out.println(chooseWorkspaceLocationPage.getUserWorkspaceLocation());

		return true;
	}

}
