package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class SummaryPage extends WizardPage {
	
	private Composite container;
	
	public SummaryPage () {
		super("SummaryPage");
		setTitle("Setup summary");
		setDescription("Your Convertigo studio is going to be installed with the following parameters.");
	}
	
	private Text summaryText ;

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;
		layout.marginWidth = 8;
		
		summaryText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		summaryText.setLayoutData(new GridData(GridData.FILL_BOTH));

		setControl(container);
		setPageComplete(true);
	}

	protected void updateSummary() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		
		String sSummary = "";
		if (setupWizard.chooseWorkspaceLocationPage != null) {
			sSummary += "User workspace:\n"
				+ " " + setupWizard.chooseWorkspaceLocationPage.getUserWorkspaceLocation() + "\n";
		}
		if (setupWizard.configureProxyPage != null) {
			sSummary += "\nProxy configuration:\n"
				+ "  Mode: " + setupWizard.configureProxyPage.getProxyMode() + "\n"
				+ "  Host: " + setupWizard.configureProxyPage.getProxyHost() + "\n"
				+ "  Port: " + setupWizard.configureProxyPage.getProxyPort() + "\n"
				+ "  Exceptions: " + setupWizard.configureProxyPage.getDoNotApplyProxy() + "\n"
				+ "  Autoconf URL: " + setupWizard.configureProxyPage.getProxyAutoConfUrl() + "\n"
				+ "  Method: " + setupWizard.configureProxyPage.getProxyMethod() + "\n"
				+ "  User: " + setupWizard.configureProxyPage.getProxyUser() + "\n"
				+ "  Password: " + setupWizard.configureProxyPage.getProxyPassword() + "\n";
		}

		summaryText.setText(sSummary);
	}
}
