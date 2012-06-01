package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.Wizard;

public class SetupWizard extends Wizard {

	protected ChooseWorkspaceLocationPage chooseWorkspaceLocationPage;
	protected ConfigureProxyPage configureProxyPage;
	protected SelectSamplesPage selectSamplesPage;
	protected RegistrationPage registrationPage;
	protected ActivationKeyPage activationKeyPage;

	public SetupWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		chooseWorkspaceLocationPage = new ChooseWorkspaceLocationPage();
		addPage(chooseWorkspaceLocationPage);
		
		configureProxyPage = new ConfigureProxyPage();
		addPage(configureProxyPage);
		
		selectSamplesPage = new SelectSamplesPage();
		addPage(selectSamplesPage);
		
		registrationPage = new RegistrationPage();
		addPage(registrationPage);
		
		activationKeyPage = new ActivationKeyPage();
		addPage(activationKeyPage);
	}

	@Override
	public boolean performFinish() {
		// Print the result to the console
		System.out.println("Workspace location selected :");
		System.out.println(chooseWorkspaceLocationPage.getUserWorkspaceLocation() + "\n");
		System.out.println("Configuration proxy :");
		System.out.println(configureProxyPage.getProxyMode());
		System.out.println(configureProxyPage.getProxyPort());
		System.out.println(configureProxyPage.getProxyHost());
		System.out.println(configureProxyPage.getDoNotApplyProxy());
		System.out.println(configureProxyPage.getProxyUrl());
		System.out.println(configureProxyPage.getProxyMethode());
		System.out.println(configureProxyPage.getProxyUser());
		System.out.println(configureProxyPage.getProxyPassword() + "\n");
		System.out.println("samples selected :");
		System.out.println(selectSamplesPage.getSamples());
		System.out.println("Registration :");
		System.out.println("User name : " + registrationPage.getUserName());
		System.out.println("Password : " + registrationPage.getPassword());
		System.out.println("Mail : " + registrationPage.getMail());
		System.out.println("Certificate key");
		System.out.println("key : " + activationKeyPage.getCertifictateKey());

		return true;
	}
}
