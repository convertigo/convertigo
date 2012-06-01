package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.Wizard;

public class RegistreWizard extends Wizard{
	
	protected RegistrationPage registrationPage;

	public RegistreWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public void addPages() {
		registrationPage = new RegistrationPage();
		addPage(registrationPage);
	}
	
	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}

}
